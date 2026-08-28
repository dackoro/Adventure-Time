#Requires -Version 5.1
<#
.SYNOPSIS
  Builds the distributable .jar (Java plugin) with Gradle.
.DESCRIPTION
  Runs the Gradle wrapper 'clean build' and returns the path of the produced
  jar (build/libs/<archiveBaseName>-<version>.jar). Output dir can be overridden.
.PARAMETER OutputDir
  Where to copy the .jar. Defaults to ./dist next to this script's repo root.
.EXAMPLE
  .\scripts\build-jar.ps1
#>
[CmdletBinding()]
param(
  [string]$OutputDir
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
if (-not $OutputDir) { $OutputDir = Join-Path $repoRoot 'dist' }

$gradlew = Join-Path $repoRoot 'gradlew.bat'
if (-not (Test-Path $gradlew)) { throw "gradlew.bat not found at $gradlew" }

Write-Host "==> Building plugin jar (Gradle clean build)..." -ForegroundColor Cyan
Push-Location $repoRoot
try {
  & $gradlew clean build --no-daemon
  if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit code $LASTEXITCODE)." }
} finally {
  Pop-Location
}

$jar = Get-ChildItem (Join-Path $repoRoot 'build/libs') -Filter '*.jar' | Where-Object { $_.Name -notmatch 'sources|javadoc' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { throw "No jar produced in build/libs" }

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }
$dest = Join-Path $OutputDir $jar.Name
Copy-Item -Path $jar.FullName -Destination $dest -Force

Write-Host "Built: $dest" -ForegroundColor Green
Write-Host ("Size:  {0:N1} KB" -f ((Get-Item $dest).Length / 1KB))
Write-Output $dest