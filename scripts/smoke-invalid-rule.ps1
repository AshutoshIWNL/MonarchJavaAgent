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

Write-Host "[smoke-invalid-rule] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Maven clean package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\invalid-rule"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"

New-Item -ItemType Directory -Force -Path $targetClasses, $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-invalid-rule] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$yaml = @"
shouldInstrument: true
configRefreshInterval: 1000
traceFileLocation: "$(Escape-YamlPath $traceRoot)"
agentRules:
  - com.monarchit.target.TargetApp::hotMethod@INGRESS::NOT_A_REAL_ACTION
printClassLoaderTrace: false
printJVMSystemProperties: false
printEnvironmentVariables: false
printJVMHeapUsage: false
printJVMCpuUsage: false
printJVMThreadUsage: false
printJVMGCStats: false
printJVMClassLoaderStats: false
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

Write-Host "[smoke-invalid-rule] Starting target app with invalid rule config..."
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

$proc.WaitForExit(15000) | Out-Null

Assert-True $proc.HasExited "Process did not exit for invalid rule config"
$stderr = Get-Content -Path $targetStdErr -Raw
$agentLog = ""
$agentLogFile = Join-Path $agentLogDir "monarchAgent.log"
if (Test-Path $agentLogFile) {
    $agentLog = Get-Content -Path $agentLogFile -Raw
}

$failedAsExpected = ($proc.ExitCode -ne 0) -or ($stderr.Length -gt 0) -or ($agentLog.Contains("Exiting Monarch Java Agent"))
Assert-True $failedAsExpected "Invalid rule did not cause expected startup failure"

Write-Host "[smoke-invalid-rule] PASS"
Write-Host "[smoke-invalid-rule] Run dir: $runDir"
