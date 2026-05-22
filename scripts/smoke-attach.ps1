Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw "ASSERTION FAILED: $Message"
    }
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [int]$TimeoutSeconds,
        [int]$PollIntervalMs,
        [string]$TimeoutMessage
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $Condition) {
            return
        }
        Start-Sleep -Milliseconds $PollIntervalMs
    }
    throw "READINESS TIMEOUT: $TimeoutMessage"
}

function Escape-YamlPath {
    param([string]$PathValue)
    return $PathValue.Replace("\", "\\")
}

function Get-AgentJarPath {
    param([string]$RepoRoot)
    $candidates = @(Get-ChildItem -Path (Join-Path $RepoRoot "target") -Filter "*.jar" -File |
        Where-Object { $_.Name -notlike "original-*" } |
        Sort-Object LastWriteTime -Descending)
    if ($candidates.Count -eq 0) {
        throw "No built agent jar found under target/"
    }
    return $candidates[0].FullName
}

function Get-CodepointLine {
    param([string]$SourceFile)
    $match = Select-String -Path $SourceFile -Pattern "CODEPOINT_TARGET"
    Assert-True ($null -ne $match) "Could not find CODEPOINT_TARGET marker in target app source"
    return $match.LineNumber
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

Write-Host "[smoke-attach] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\attach"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"
$metricsPort = 9100

New-Item -ItemType Directory -Force -Path $targetClasses, $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-attach] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$codepointLine = Get-CodepointLine -SourceFile $targetSrc

$yaml = @"
mode: hybrid
instrumentation:
  enabled: true
  configRefreshInterval: 1000
  traceFileLocation: "$(Escape-YamlPath $traceRoot)"
  agentRules:
    - com.monarchit.target.TargetApp::TargetApp@INGRESS::ARGS
    - com.monarchit.target.TargetApp::hotMethod@INGRESS::ARGS
    - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
    - com.monarchit.target.TargetApp::hotMethod@INGRESS::STACK
    - com.monarchit.target.TargetApp::filteredStackMethod@INGRESS::STACK::[com.monarchit.target.TargetApp.main]
    - com.monarchit.target.TargetApp::profileWork@PROFILE
    - com.monarchit.target.TargetApp::hotMethod@INGRESS::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("ADD_MARKER");]
    - com.monarchit.target.TargetApp::lineProbe@CODEPOINT($codepointLine)::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("CODEPOINT_MARKER");]
    - com.monarchit.target.TargetApp::memoryBurst@INGRESS::HEAP
observer:
  enabled: true
  printClassLoaderTrace: false
  printJVMSystemProperties: false
  printEnvironmentVariables: false
  metrics:
    exposeHttp: true
    port: $metricsPort
    heapUsage: true
    cpuUsage: true
    threadUsage: true
    gcStats: true
    classLoaderStats: true
alerts:
  enabled: false
  maxHeapDumps: 1
  emailRecipientList: []
"@
Set-Content -Path $configFile -Value $yaml -Encoding UTF8

$targetStdOut = Join-Path $runDir "target.out.log"
$targetStdErr = Join-Path $runDir "target.err.log"

Write-Host "[smoke-attach] Starting target app without agent..."
$proc = Start-Process `
    -FilePath "java" `
    -ArgumentList @("-Xverify:none", "-cp", $targetClasses, "com.monarchit.target.TargetApp") `
    -PassThru `
    -RedirectStandardOutput $targetStdOut `
    -RedirectStandardError $targetStdErr

try {
    Wait-Until -TimeoutSeconds 20 -PollIntervalMs 250 -TimeoutMessage "Target app did not stay alive long enough before attach." -Condition {
        return (-not $proc.HasExited)
    }
    Start-Sleep -Seconds 3
    $preAttachTraceDirs = Get-ChildItem -Path $traceRoot -Directory -ErrorAction SilentlyContinue
    Assert-True (($null -eq $preAttachTraceDirs) -or ($preAttachTraceDirs.Count -eq 0)) "Trace output already exists before attach"

Write-Host "[smoke-attach] Attaching agent to PID $($proc.Id)..."
    if (-not $env:JAVA_HOME) {
        throw "JAVA_HOME is not set. Set JAVA_HOME to a JDK installation for attach mode."
    }
    $javaBin = Join-Path $env:JAVA_HOME "bin\\java"
    $toolsJar = Join-Path $env:JAVA_HOME "lib\\tools.jar"
    $attachCp = $agentJarLocal
    if (Test-Path $toolsJar) {
        $attachCp = "$agentJarLocal;$toolsJar"
    }

    Write-Host "[smoke-attach] Preflight attach to PID $($proc.Id)..."
    & $javaBin -cp $attachCp com.asm.mja.attach.AgentAttachCLI `
        -agentJar $agentJarLocal `
        -configFile $configFile `
        -args "preflight=true,agentLogFileDir=$agentLogDir,agentLogLevel=INFO" `
        -pid $proc.Id | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Preflight attach CLI failed with exit code $LASTEXITCODE" }

    Start-Sleep -Seconds 2
    $postPreflightTraceDirs = Get-ChildItem -Path $traceRoot -Directory -ErrorAction SilentlyContinue
    Assert-True (($null -eq $postPreflightTraceDirs) -or ($postPreflightTraceDirs.Count -eq 0)) "Trace output exists after preflight attach; expected no instrumentation side effects"

    Write-Host "[smoke-attach] Full attach to PID $($proc.Id)..."
    & $javaBin -cp $attachCp com.asm.mja.attach.AgentAttachCLI `
        -agentJar $agentJarLocal `
        -configFile $configFile `
        -args "agentLogFileDir=$agentLogDir,agentLogLevel=INFO" `
        -pid $proc.Id | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Attach CLI failed with exit code $LASTEXITCODE" }

    Wait-Until -TimeoutSeconds 40 -PollIntervalMs 500 -TimeoutMessage "No trace directory created under $traceRoot after attach." -Condition {
        $dirs = @(Get-ChildItem -Path $traceRoot -Directory -ErrorAction SilentlyContinue)
        return $dirs.Count -gt 0
    }
    $traceDir = Get-ChildItem -Path $traceRoot -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Assert-True ($null -ne $traceDir) "No trace directory created under $traceRoot after attach"

    $traceFile = Join-Path $traceDir.FullName "agent.trace"
    Wait-Until -TimeoutSeconds 40 -PollIntervalMs 500 -TimeoutMessage "Trace file not found: $traceFile" -Condition {
        return Test-Path $traceFile
    }

    Wait-Until -TimeoutSeconds 90 -PollIntervalMs 1000 -TimeoutMessage "Trace markers did not appear in time: ARGS/RET/STACK/PROFILE/ADD/CODEPOINT/HEAP and monitor traces." -Condition {
        $traceText = Get-Content -Path $traceFile -Raw
        return $traceText.Contains("ARGS |") `
            -and $traceText.Contains("{com.monarchit.target.TargetApp.TargetApp} | INGRESS | ARGS") `
            -and $traceText.Contains("RET |") `
            -and $traceText.Contains("STACK") `
            -and $traceText.Contains("{com.monarchit.target.TargetApp.filteredStackMethod} | INGRESS | STACK") `
            -and $traceText.Contains("PROFILE | Execution time") `
            -and $traceText.Contains("ADD_MARKER") `
            -and $traceText.Contains("CODEPOINT_MARKER") `
            -and $traceText.Contains("HEAP") `
            -and $traceText.Contains("Current JVM CPU Load") `
            -and $traceText.Contains("GC Stats -") `
            -and $traceText.Contains("Thread Stats -") `
            -and $traceText.Contains("ClassLoader Stats -") `
            -and $traceText.Contains("{USED:")
    }
    $traceText = Get-Content -Path $traceFile -Raw

    Wait-Until -TimeoutSeconds 60 -PollIntervalMs 1000 -TimeoutMessage "No heap dump file generated after attach." -Condition {
        $heapDumps = @(Get-ChildItem -Path $traceDir.FullName -Filter "*.hprof" -File -ErrorAction SilentlyContinue)
        return $heapDumps.Count -ge 1
    }

    Wait-Until -TimeoutSeconds 45 -PollIntervalMs 1000 -TimeoutMessage "Metrics endpoint did not become reachable after attach." -Condition {
        try {
            $null = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:{0}/metrics" -f $metricsPort) -TimeoutSec 3
            return $true
        } catch {
            return $false
        }
    }

    $metrics = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:{0}/metrics" -f $metricsPort) -TimeoutSec 5
    Assert-True ($metrics.StatusCode -eq 200) "Metrics endpoint did not return HTTP 200 after attach"
    Assert-True ($metrics.Headers["Content-Type"].Contains("text/plain")) "Metrics endpoint did not return Prometheus text content type after attach"
    Assert-True ($metrics.Content.Contains("monarch_agent_info{agent=""MonarchJavaAgent""} 1.0")) "Prometheus payload missing monarch_agent_info after attach"

    $openMetrics = Invoke-WebRequest -UseBasicParsing `
        -Headers @{ "Accept" = "application/openmetrics-text; version=1.0.0" } `
        -Uri ("http://127.0.0.1:{0}/metrics" -f $metricsPort)
    Assert-True ($openMetrics.StatusCode -eq 200) "OpenMetrics request did not return HTTP 200 after attach"
    Assert-True ($openMetrics.Headers["Content-Type"].Contains("application/openmetrics-text")) "OpenMetrics content type not returned after attach"
    $openMetricsBody = & curl.exe -fsS -H "Accept: application/openmetrics-text; version=1.0.0" ("http://127.0.0.1:{0}/metrics" -f $metricsPort)
    Assert-True ($openMetricsBody.Contains("# EOF")) "OpenMetrics payload missing EOF marker after attach"

    $metricsJson = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:{0}/metrics.json" -f $metricsPort)
    Assert-True ($metricsJson.StatusCode -eq 200) "Metrics JSON endpoint did not return HTTP 200 after attach"
    Assert-True ($metricsJson.Content.Contains('"agent":"MonarchJavaAgent"')) "Metrics JSON payload missing agent identifier after attach"

    Write-Host "[smoke-attach] PASS"
    Write-Host "[smoke-attach] Trace: $traceFile"
    Write-Host "[smoke-attach] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
