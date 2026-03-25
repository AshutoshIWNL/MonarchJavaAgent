Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "[smoke-all] Running startup attach smoke..."
& (Join-Path $scriptDir "smoke-javaagent.ps1")

Write-Host "[smoke-all] Running runtime attach smoke..."
& (Join-Path $scriptDir "smoke-attach.ps1")

Write-Host "[smoke-all] Running class replacement smoke..."
& (Join-Path $scriptDir "smoke-class-replace.ps1")

Write-Host "[smoke-all] PASS"
