#Requires -Version 5.1
<#
.SYNOPSIS
  Publish the current main version to CurseForge + GitHub release.
.DESCRIPTION
  Run AFTER promote.ps1. Builds the zip, uploads to CurseForge via Upload API,
  and optionally creates a GitHub release with the zip attached.
  Reads config from .cf-config.json (untracked) at repo root:
    {
      "ProjectId": 123456,
      "ApiBaseUrl": "https://hytale.curseforge.com/api"
    }
  Reads token from env var CURSEFORGE_API_TOKEN (or .cf-token file at repo root, gitignored).
.PARAMETER ReleaseType
  release | beta | alpha. Default: release
.PARAMETER ChangelogFile
  Path to a markdown file with the release notes. Default: derive from latest git tag message.
.PARAMETER SkipCurseForge
  Only build zip + GitHub release, skip CF upload.
.PARAMETER SkipGitHub
  Only build zip + CF upload, skip GitHub release.
.EXAMPLE
  $env:CURSEFORGE_API_TOKEN = "..."; .\scripts\publish.ps1
#>
[CmdletBinding()]
param(
  [ValidateSet('release','beta','alpha')][string]$ReleaseType = 'release',
  [string]$ChangelogFile,
  [switch]$SkipCurseForge,
  [switch]$SkipGitHub
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

$currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
if ($currentBranch -ne 'main') { throw "publish.ps1 must run from 'main'. Currently on '$currentBranch'." }

$manifest = Get-Content (Join-Path $repoRoot 'manifest.json') -Raw | ConvertFrom-Json
$version = $manifest.Version
$serverVersion = $manifest.ServerVersion
$tag = "v$version"
Write-Host "Publishing $($manifest.Name) $tag (Hytale build: $serverVersion)" -ForegroundColor Cyan

# Resolve changelog
$changelog = ''
if ($ChangelogFile -and (Test-Path $ChangelogFile)) {
  $changelog = Get-Content $ChangelogFile -Raw
} else {
  $changelog = (git tag -l --format='%(contents)' $tag) -join "`n"
  if (-not $changelog) { $changelog = "Release $tag" }
}

# Build zip
Write-Host "==> Building zip..." -ForegroundColor Cyan
$zipPath = & (Join-Path $PSScriptRoot 'build-zip.ps1') | Select-Object -Last 1
if (-not (Test-Path $zipPath)) { throw "Zip build failed." }

# --- CurseForge upload ---
if (-not $SkipCurseForge) {
  $cfgPath = Join-Path $repoRoot '.cf-config.json'
  if (-not (Test-Path $cfgPath)) {
    throw ".cf-config.json missing. Create it with: { `"ProjectId`": <id>, `"ApiBaseUrl`": `"https://hytale.curseforge.com/api`" }"
  }
  $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
  $token = $env:CURSEFORGE_API_TOKEN
  if (-not $token) {
    $tokenFile = Join-Path $repoRoot '.cf-token'
    if (Test-Path $tokenFile) { $token = (Get-Content $tokenFile -Raw).Trim() }
  }
  if (-not $token) { throw "Set CURSEFORGE_API_TOKEN env var or create .cf-token file." }

  Write-Host "==> Resolving Hytale gameVersion id for '$serverVersion'..." -ForegroundColor Cyan
  $versions = Invoke-RestMethod -Uri "$($cfg.ApiBaseUrl)/game/versions" -Headers @{ 'X-Api-Token' = $token }
  $match = $versions | Where-Object { $_.name -eq $serverVersion -or $_.slug -eq ($serverVersion -replace '\.','-') }
  if (-not $match) {
    Write-Warning "No exact match for '$serverVersion'. Available recent versions:"
    $versions | Select-Object -First 10 | ForEach-Object { Write-Host ("  id={0}  name={1}" -f $_.id, $_.name) }
    throw "Pick the right id manually and rerun with -GameVersionId, or update ServerVersion in manifest."
  }
  $gameVersionId = $match.id
  Write-Host "    gameVersionId=$gameVersionId"

  Write-Host "==> Uploading to CurseForge project $($cfg.ProjectId)..." -ForegroundColor Cyan
  $metadata = @{
    changelog     = $changelog
    changelogType = 'markdown'
    displayName   = "$($manifest.Name) $tag"
    gameVersions  = @($gameVersionId)
    releaseType   = $ReleaseType
  } | ConvertTo-Json -Depth 5 -Compress

  $form = @{
    metadata = $metadata
    file     = Get-Item $zipPath
  }
  $resp = Invoke-RestMethod -Method Post `
    -Uri "$($cfg.ApiBaseUrl)/projects/$($cfg.ProjectId)/upload-file" `
    -Headers @{ 'X-Api-Token' = $token } `
    -Form $form
  Write-Host "    CurseForge fileId: $($resp.id)" -ForegroundColor Green
}

# --- GitHub release ---
if (-not $SkipGitHub) {
  $gh = Get-Command gh -ErrorAction SilentlyContinue
  if (-not $gh) {
    Write-Warning "gh CLI not installed — skipping GitHub release. Install: winget install --id GitHub.cli"
  } else {
    Write-Host "==> Creating GitHub release $tag..." -ForegroundColor Cyan
    $notesFile = New-TemporaryFile
    Set-Content -Path $notesFile -Value $changelog -Encoding utf8
    try {
      gh release create $tag $zipPath --title "$($manifest.Name) $tag" --notes-file $notesFile
    } finally {
      Remove-Item $notesFile -ErrorAction SilentlyContinue
    }
  }
}

Write-Host ""
Write-Host "Published $tag" -ForegroundColor Green
