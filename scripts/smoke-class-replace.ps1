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

Write-Host "[smoke-class-replace] Building agent..."
Push-Location $repoRoot
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed with exit code $LASTEXITCODE" }
Pop-Location

$agentJar = Get-AgentJarPath -RepoRoot $repoRoot
$targetSrc = Join-Path $repoRoot "it\target-app\src\com\asm\mja\it\TargetApp.java"
$runRoot = Join-Path $env:TEMP "mja-smoke\class-replace"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$runDir = Join-Path $runRoot $timestamp
$targetClasses = Join-Path $runDir "target-classes"
$patchSrc = Join-Path $runDir "patch-src\TargetApp.java"
$patchClasses = Join-Path $runDir "patch-classes"
$patchJar = Join-Path $runDir "patches.jar"
$invalidClass = Join-Path $runDir "invalid.class"
$traceRoot = Join-Path $runDir "trace"
$agentLogDir = Join-Path $runDir "agent-logs"
$configFile = Join-Path $runDir "config.yaml"
$agentJarLocal = Join-Path $runDir "agent.jar"

New-Item -ItemType Directory -Force -Path $targetClasses, $patchClasses, (Split-Path -Parent $patchSrc), $runDir, $traceRoot, $agentLogDir | Out-Null
Copy-Item -Path $agentJar -Destination $agentJarLocal -Force

Write-Host "[smoke-class-replace] Compiling target app..."
javac -d $targetClasses $targetSrc
if ($LASTEXITCODE -ne 0) { throw "javac target compile failed with exit code $LASTEXITCODE" }

Copy-Item -Path $targetSrc -Destination $patchSrc -Force
$patchedText = (Get-Content -Path $patchSrc -Raw).Replace("return a + b + (appSeed - appSeed);", "return 4242;")
[System.IO.File]::WriteAllText(
    $patchSrc,
    $patchedText,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "[smoke-class-replace] Compiling replacement class and jar..."
javac -d $patchClasses $patchSrc
if ($LASTEXITCODE -ne 0) { throw "javac patch compile failed with exit code $LASTEXITCODE" }
$patchClassFile = Join-Path $patchClasses "com\monarchit\target\TargetApp.class"
& jar cf $patchJar -C $patchClasses com/monarchit/target/TargetApp.class
if ($LASTEXITCODE -ne 0) { throw "jar create failed with exit code $LASTEXITCODE" }
Set-Content -Path $invalidClass -Value "not-a-valid-class-file" -Encoding Ascii

$yaml = @"
mode: instrumenter
instrumentation:
  enabled: true
  configRefreshInterval: 1000
  traceFileLocation: "$(Escape-YamlPath $traceRoot)"
  agentRules:
    - com.monarchit.target.TargetApp@CHANGE::FILE::[$(Escape-YamlPath $patchClassFile)]
    - com.monarchit.target.*@CHANGE::JAR::[$(Escape-YamlPath $patchJar)]
    - com.monarchit.target.MissingClass@CHANGE::FILE::[$(Escape-YamlPath $patchClassFile)]
    - com.monarchit.target.TargetApp@CHANGE::FILE::[$(Escape-YamlPath $invalidClass)]
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
  maxHeapDumps: 1
  emailRecipientList: []
"@
Set-Content -Path $configFile -Value $yaml -Encoding UTF8

$targetStdOut = Join-Path $runDir "target.out.log"
$targetStdErr = Join-Path $runDir "target.err.log"

Write-Host "[smoke-class-replace] Starting target app without agent..."
$proc = Start-Process `
    -FilePath "java" `
    -ArgumentList @("-Xverify:none", "-cp", $targetClasses, "com.monarchit.target.TargetApp") `
    -PassThru `
    -RedirectStandardOutput $targetStdOut `
    -RedirectStandardError $targetStdErr

try {
    Start-Sleep -Seconds 3

    Write-Host "[smoke-class-replace] Attaching agent to PID $($proc.Id)..."
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
    Assert-True ($traceText.Contains("Class replacement requested for com.monarchit.target.TargetApp using FILE source")) "Missing FILE replacement request log"
    Assert-True ($traceText.Contains("Class replacement succeeded for com.monarchit.target.TargetApp")) "Missing class replacement success log"
    Assert-True ($traceText.Contains("Class replacement requested for com.monarchit.target.TargetApp using JAR source")) "Missing JAR replacement request log"
    Assert-True ($traceText.Contains("Class replacement skipped; no loaded class matched pattern com.monarchit.target.MissingClass")) "Missing class-not-loaded warning log"
    Assert-True ($traceText.Contains("Class replacement failed for com.monarchit.target.TargetApp from $(Escape-YamlPath $invalidClass)")) "Missing incompatible replacement failure log"

    $backupFile = Join-Path $traceDir.FullName "backup\com_monarchit_target_TargetApp.class"
    Assert-True (Test-Path $backupFile) "Expected backup class was not created: $backupFile"

    Write-Host "[smoke-class-replace] PASS"
    Write-Host "[smoke-class-replace] Trace: $traceFile"
    Write-Host "[smoke-class-replace] Run dir: $runDir"
}
finally {
    if ($null -ne $proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}
