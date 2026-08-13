#Requires -Version 5.1
<#
.SYNOPSIS
  Builds the distributable .zip for CurseForge from the current worktree.
.DESCRIPTION
  Reads manifest.json, zips the mod content (excluding .git, scripts, dist, .gitignore-d files)
  and outputs dist/<Name>-<Version>.zip with manifest.json at the zip root (what Hytale expects).
.PARAMETER OutputDir
  Where to put the .zip. Defaults to ./dist next to this script's repo root.
#>
[CmdletBinding()]
param(
  [string]$OutputDir
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
if (-not $OutputDir) { $OutputDir = Join-Path $repoRoot 'dist' }

$manifest = Get-Content (Join-Path $repoRoot 'manifest.json') -Raw | ConvertFrom-Json
$safeName = ($manifest.Name -replace '[^A-Za-z0-9._-]', '_')
$zipName = "{0}-{1}.zip" -f $safeName, $manifest.Version
$zipPath = Join-Path $OutputDir $zipName

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

# Staging dir so the zip root contains manifest.json (not the parent folder).
$staging = Join-Path $env:TEMP "hytale-mod-build-$([guid]::NewGuid().ToString('N'))"
New-Item -ItemType Directory -Path $staging | Out-Null
try {
  $exclude = @('.git', 'scripts', 'dist', '.gitignore', '.gitattributes', '.claude', 'CLAUDE.md', 'AGENTS.md', '.cf-token', '.cf-config.json', '.cf-config.example.json')
  Get-ChildItem -Path $repoRoot -Force | Where-Object { $exclude -notcontains $_.Name } | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $staging -Recurse -Force
  }
  Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $zipPath -CompressionLevel Optimal
} finally {
  Remove-Item $staging -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Built: $zipPath" -ForegroundColor Green
Write-Host ("Size:  {0:N1} KB" -f ((Get-Item $zipPath).Length / 1KB))
Write-Output $zipPath
