#Requires -Version 5.1
#Requires -RunAsAdministrator
<#
.SYNOPSIS
  Replace mod folders in %APPDATA%\Hytale\UserData\Mods with symlinks to this repo's worktrees.
.DESCRIPTION
  After this runs, editing JSON in C:\HytaleModding\Adventure time(-dev) is reflected in Hytale
  immediately — no copy step.
  MUST be run as Administrator (Windows requires elevation for SymbolicLink creation).
#>
[CmdletBinding()]
param(
  [string]$ModsDir = (Join-Path $env:APPDATA 'Hytale\UserData\Mods'),
  [string]$ProdSrc = 'C:\HytaleModding\Adventure time',
  [string]$DevSrc  = 'C:\HytaleModding\Adventure time-dev'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $ModsDir)) { throw "Mods dir not found: $ModsDir (launch Hytale once first)" }

function Link-One {
  param([string]$Source, [string]$LinkName)
  $linkPath = Join-Path $ModsDir $LinkName
  if (Test-Path $linkPath) {
    $existing = Get-Item $linkPath -Force
    if ($existing.LinkType -eq 'SymbolicLink') {
      Write-Host "  $LinkName already a symlink -> $($existing.Target)" -ForegroundColor DarkGray
      return
    }
    Write-Host "  Backing up existing $LinkName -> $LinkName.bak" -ForegroundColor Yellow
    if (Test-Path "$linkPath.bak") { Remove-Item "$linkPath.bak" -Recurse -Force }
    Move-Item $linkPath "$linkPath.bak"
  }
  New-Item -ItemType SymbolicLink -Path $linkPath -Target $Source | Out-Null
  Write-Host "  Linked $LinkName -> $Source" -ForegroundColor Green
}

Write-Host "Installing symlinks in $ModsDir" -ForegroundColor Cyan
Link-One -Source $ProdSrc -LinkName 'Adventure time'
Link-One -Source $DevSrc  -LinkName 'Adventure time DEV'
Write-Host "Done. Restart Hytale to see both mods in the list." -ForegroundColor Green
