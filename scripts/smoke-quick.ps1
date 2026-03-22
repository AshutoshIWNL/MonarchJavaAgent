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

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

Write-Host "[smoke-quick] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Maven clean package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\quick"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"

New-Item -ItemType Directory -Force -Path $targetClasses, $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-quick] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$yaml = @"
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$(Escape-YamlPath $traceRoot)"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::ARGS
  - com.monarchit.target.TargetApp::hotMethod@EGRESS::RET
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: true
printJVMCpuUsage: true
printJVMThreadUsage: true
printJVMGCStats: true
printJVMClassLoaderStats: true
exposeMetrics: false
metricsPort: 0
maxHeapDumps: 0
sendAlertEmails: false
emailRecipientList: []
"@
Set-Content -Path $configFile -Value $yaml -Encoding UTF8

$agentArgs = "configFile=$configFile,agentLogFileDir=$agentLogDir,agentLogLevel=INFO,agentJarPath=$agentJarLocal"
$targetStdOut = Join-Path $runDir "target.out.log"
$targetStdErr = Join-Path $runDir "target.err.log"

Write-Host "[smoke-quick] Starting target app with -javaagent..."
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

    $traceText = Get-Content -Path $traceFile -Raw
    Assert-True ($traceText.Contains("ARGS |")) "Missing ARGS instrumentation marker"
    Assert-True ($traceText.Contains("RET |")) "Missing RET instrumentation marker"
    Assert-True ($traceText.Contains("Current JVM CPU Load")) "Missing CPU monitor trace"
    Assert-True ($traceText.Contains("GC Stats -")) "Missing GC monitor trace"
    Assert-True ($traceText.Contains("Thread Stats -")) "Missing Thread monitor trace"
    Assert-True ($traceText.Contains("ClassLoader Stats -")) "Missing ClassLoader monitor trace"
    Assert-True ($traceText.Contains("{USED:")) "Missing Heap monitor trace"

    Write-Host "[smoke-quick] PASS"
    Write-Host "[smoke-quick] Trace: $traceFile"
    Write-Host "[smoke-quick] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
