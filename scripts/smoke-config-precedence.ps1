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

Write-Host "[smoke-config-precedence] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Maven clean package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\config-precedence"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"

New-Item -ItemType Directory -Force -Path $targetClasses, $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-config-precedence] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

$yaml = @"
mode: observer
instrumentation:
  enabled: false
  configRefreshInterval: 1000
  traceFileLocation: "$(Escape-YamlPath $traceRoot)"
observer:
  enabled: true
  printClassLoaderTrace: false
  printJVMSystemProperties: false
  printEnvironmentVariables: false
  metrics:
    exposeHttp: false
    port: 0
    heapUsage: false
    cpuUsage: true
    threadUsage: false
    gcStats: false
    classLoaderStats: false
alerts:
  enabled: false
  maxHeapDumps: 0
  emailRecipientList: []
printJVMHeapUsage: true
printJVMCpuUsage: false
"@
Set-Content -Path $configFile -Value $yaml -Encoding UTF8

$agentArgs = "configFile=$configFile,agentLogFileDir=$agentLogDir,agentLogLevel=INFO,agentJarPath=$agentJarLocal"
$targetStdOut = Join-Path $runDir "target.out.log"
$targetStdErr = Join-Path $runDir "target.err.log"

Write-Host "[smoke-config-precedence] Starting target app with nested+legacy config..."
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
    Assert-True ($traceText.Contains("Current JVM CPU Load")) "Expected CPU monitor trace from nested observer metrics config"
    Assert-True (-not $traceText.Contains("{USED:")) "Heap monitor trace was present even though nested heapUsage=false should override legacy printJVMHeapUsage=true"
    Assert-True (-not $traceText.Contains("ARGS |")) "Unexpected instrumentation marker in observer-only precedence test"

    Write-Host "[smoke-config-precedence] PASS"
    Write-Host "[smoke-config-precedence] Trace: $traceFile"
    Write-Host "[smoke-config-precedence] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
