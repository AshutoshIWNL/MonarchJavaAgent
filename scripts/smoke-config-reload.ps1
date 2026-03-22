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

function Write-Config {
    param(
        [string]$ConfigFile,
        [string]$TraceRoot,
        [bool]$EnableAddRule
    )
    $addRule = ""
    if ($EnableAddRule) {
        $addRule = "    - com.monarchit.target.TargetApp::hotMethod@INGRESS::ADD::[com.asm.mja.logging.TraceFileLogger.getInstance().trace(""RELOAD_MARKER_ON"");]"
    }

    $yaml = @"
mode: instrumenter
instrumentation:
  enabled: true
  configRefreshInterval: 1000
  traceFileLocation: "$(Escape-YamlPath $TraceRoot)"
  agentRules:
    - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
$addRule
observer:
  enabled: false
  printClassLoaderTrace: false
  printJVMSystemProperties: false
  printEnvironmentVariables: false
  metrics:
    exposeHttp: false
    port: 0
    heapUsage: false
    cpuUsage: false
    threadUsage: false
    gcStats: false
    classLoaderStats: false
alerts:
  enabled: false
  maxHeapDumps: 0
  emailRecipientList: []
"@
    Set-Content -Path $ConfigFile -Value $yaml -Encoding UTF8
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

Write-Host "[smoke-config-reload] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Maven clean package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\config-reload"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"

New-Item -ItemType Directory -Force -Path $targetClasses, $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-config-reload] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Config -ConfigFile $configFile -TraceRoot $traceRoot -EnableAddRule $true

$agentArgs = "configFile=$configFile,agentLogFileDir=$agentLogDir,agentLogLevel=INFO,agentJarPath=$agentJarLocal"
$targetStdOut = Join-Path $runDir "target.out.log"
$targetStdErr = Join-Path $runDir "target.err.log"

Write-Host "[smoke-config-reload] Starting target app with initial ADD rule..."
$proc = Start-Process `
    -FilePath "java" `
    -ArgumentList @(
        "-Xverify:none",
        "-javaagent:`"$agentJarLocal`"=$agentArgs",
        "-cp", $targetClasses,
        "com.monarchit.target.TargetApp"
    ) `
    -PassThru `
    -RedirectStandardOutput $targetStdOut `
    -RedirectStandardError $targetStdErr

try {
    Start-Sleep -Seconds 8
    $traceDir = Get-ChildItem -Path $traceRoot -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    Assert-True ($null -ne $traceDir) "No trace directory created under $traceRoot"
    $traceFile = Join-Path $traceDir.FullName "agent.trace"
    Assert-True (Test-Path $traceFile) "Trace file not found: $traceFile"

    $before = Get-Content -Path $traceFile -Raw
    Assert-True ($before.Contains("RELOAD_MARKER_ON")) "Expected RELOAD_MARKER_ON before config change"

    $beforeLength = $before.Length

    Write-Host "[smoke-config-reload] Updating config to remove ADD rule..."
    Write-Config -ConfigFile $configFile -TraceRoot $traceRoot -EnableAddRule $false

    $postReload = ""
    $foundReloadEvent = $false
    $foundRet = $false
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        $now = Get-Content -Path $traceFile -Raw
        if ($now.Length -gt $beforeLength) {
            $tail = $now.Substring($beforeLength)
            $reloadIdx = $tail.LastIndexOf("Configuration file has been modified")
            if ($reloadIdx -ge 0) {
                $foundReloadEvent = $true
                $postReload = $tail.Substring($reloadIdx)
                if ($postReload.Contains("RET |")) {
                    $foundRet = $true
                    break
                }
            }
        }
    }

    Assert-True $foundReloadEvent "Expected config reload event in trace after file update"
    Assert-True $foundRet "Expected ongoing RET activity after config reload"
    Assert-True (-not $postReload.Contains("RELOAD_MARKER_ON")) "RELOAD_MARKER_ON still present after removing ADD rule"

    Write-Host "[smoke-config-reload] PASS"
    Write-Host "[smoke-config-reload] Trace: $traceFile"
    Write-Host "[smoke-config-reload] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
