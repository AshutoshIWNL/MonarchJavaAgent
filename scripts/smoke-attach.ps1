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
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$(Escape-YamlPath $traceRoot)"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ARGS
  - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::STACK
  - com.monarchit.target.TargetApp::profileWork@PROFILE
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("ADD_MARKER");]
  - com.monarchit.target.TargetApp::lineProbe@CODEPOINT($codepointLine)::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace("CODEPOINT_MARKER");]
  - com.monarchit.target.TargetApp::memoryBurst@INGRESS::HEAP
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: true
printJVMCpuUsage: true
printJVMThreadUsage: true
printJVMGCStats: true
printJVMClassLoaderStats: true
exposeMetrics: true
metricsPort: $metricsPort
maxHeapDumps: 1
sendAlertEmails: false
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

    & $javaBin -cp $attachCp com.asm.mja.attach.AgentAttachCLI `
        -agentJar $agentJarLocal `
        -configFile $configFile `
        -args "agentLogFileDir=$agentLogDir,agentLogLevel=INFO" `
        -pid $proc.Id | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "Attach CLI failed with exit code $LASTEXITCODE" }

    Start-Sleep -Seconds 8

    $traceDir = Get-ChildItem -Path $traceRoot -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Assert-True ($null -ne $traceDir) "No trace directory created under $traceRoot after attach"

    $traceFile = Join-Path $traceDir.FullName "agent.trace"
    Assert-True (Test-Path $traceFile) "Trace file not found: $traceFile"

    $traceText = Get-Content -Path $traceFile -Raw
    Assert-True ($traceText.Contains("ARGS |")) "Missing ARGS instrumentation marker after attach"
    Assert-True ($traceText.Contains("RET |")) "Missing RET instrumentation marker after attach"
    Assert-True ($traceText.Contains("STACK")) "Missing STACK instrumentation marker after attach"
    Assert-True ($traceText.Contains("PROFILE | Execution time")) "Missing PROFILE instrumentation marker after attach"
    Assert-True ($traceText.Contains("ADD_MARKER")) "Missing ADD custom code marker after attach"
    Assert-True ($traceText.Contains("CODEPOINT_MARKER")) "Missing CODEPOINT custom code marker after attach"
    Assert-True ($traceText.Contains("HEAP")) "Missing HEAP instrumentation marker after attach"
    Assert-True ($traceText.Contains("Current JVM CPU Load")) "Missing CPU monitor trace after attach"
    Assert-True ($traceText.Contains("GC Stats -")) "Missing GC monitor trace after attach"
    Assert-True ($traceText.Contains("Thread Stats -")) "Missing Thread monitor trace after attach"
    Assert-True ($traceText.Contains("ClassLoader Stats -")) "Missing ClassLoader monitor trace after attach"
    Assert-True ($traceText.Contains("{USED:")) "Missing Heap monitor trace after attach"

    $heapDumps = @(Get-ChildItem -Path $traceDir.FullName -Filter "*.hprof" -File -ErrorAction SilentlyContinue)
    Assert-True ($heapDumps.Count -ge 1) "No heap dump file generated after attach"

    $metrics = Invoke-WebRequest -UseBasicParsing -Uri ("http://127.0.0.1:{0}/metrics" -f $metricsPort)
    Assert-True ($metrics.StatusCode -eq 200) "Metrics endpoint did not return HTTP 200 after attach"
    Assert-True ($metrics.Content.Contains('"agent":"MonarchJavaAgent"')) "Metrics payload missing agent identifier after attach"

    Write-Host "[smoke-attach] PASS"
    Write-Host "[smoke-attach] Trace: $traceFile"
    Write-Host "[smoke-attach] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
