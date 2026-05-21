#Requires -Version 5.1
<#
.SYNOPSIS
  Promote dev -> main: merge, bump version, commit, tag, push.
.DESCRIPTION
  Run from the MAIN worktree (C:\HytaleModding\Adventure time).
  Merges dev into main, updates manifest.json Version, commits, tags vX.Y.Z, pushes (if remote exists).
.PARAMETER Version
  New version string for manifest.json (e.g. "0.0.3"). Required.
.PARAMETER Message
  Commit + tag message describing what changed in this release. Required.
.PARAMETER NoPush
  Skip the git push step (just commit and tag locally).
.EXAMPLE
  .\scripts\promote.ps1 -Version 0.0.3 -Message "fix: update for Hytale 2026.04 build"
#>
[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)][string]$Version,
  [Parameter(Mandatory=$true)][string]$Message,
  [switch]$NoPush
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

# Sanity check: are we on main?
$currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
if ($currentBranch -ne 'main') {
  throw "promote.ps1 must run from the 'main' worktree. Currently on '$currentBranch' at $repoRoot."
}

# Sanity check: clean working tree
$dirty = git status --porcelain
if ($dirty) { throw "Working tree is dirty. Commit or stash before promoting:`n$dirty" }

# Sanity check: version format
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
  throw "Version '$Version' must be semver MAJOR.MINOR.PATCH"
}

Write-Host "==> Merging dev into main..." -ForegroundColor Cyan
git merge dev --no-ff -m "chore: merge dev for v$Version"
if ($LASTEXITCODE -ne 0) { throw "Merge failed. Resolve conflicts then re-run." }

Write-Host "==> Bumping manifest.json to $Version..." -ForegroundColor Cyan
$manifestPath = Join-Path $repoRoot 'manifest.json'
$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
$manifest.Version = $Version
# Preserve original key order by writing back with manual JSON formatting (ConvertTo-Json reorders alphabetically in PS 5.1).
$manifest | ConvertTo-Json -Depth 20 | Set-Content -Path $manifestPath -Encoding utf8

git add manifest.json
git commit -m "chore: bump version to $Version"

$tag = "v$Version"
Write-Host "==> Tagging $tag..." -ForegroundColor Cyan
git tag -a $tag -m $Message

if (-not $NoPush) {
  $hasRemote = (git remote) -match '^origin$'
  if ($hasRemote) {
    Write-Host "==> Pushing main + tag to origin..." -ForegroundColor Cyan
    git push origin main
    git push origin $tag
  } else {
    Write-Warning "No 'origin' remote configured — skipping push. Add one with: git remote add origin <url>"
  }
}

Write-Host ""
Write-Host "Promoted dev -> main as $tag" -ForegroundColor Green
Write-Host "Next: .\scripts\publish.ps1   (uploads to CurseForge + GitHub release)"
