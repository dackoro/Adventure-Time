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
    throw ".cf-config.json missing. Copy from .cf-config.example.json and fill in ProjectId."
  }
  $cfg = Get-Content $cfgPath -Raw | ConvertFrom-Json
  $token = $env:CURSEFORGE_API_TOKEN
  if (-not $token) {
    $tokenFile = Join-Path $repoRoot '.cf-token'
    if (Test-Path $tokenFile) { $token = (Get-Content $tokenFile -Raw).Trim() }
  }
  if (-not $token) { throw "Set CURSEFORGE_API_TOKEN env var or create .cf-token file." }

  Write-Host "==> Uploading to CurseForge project $($cfg.ProjectId)..." -ForegroundColor Cyan
  # NOTE: Hytale uploads must OMIT gameVersions entirely. The legacy API doc
  # marks it required, but the Hytale backend rejects every gameVersion ID with
  # "belongs to an invalid game". Compat is controlled by ServerVersion in manifest.json.
  $metadata = @{
    changelog     = $changelog
    changelogType = 'markdown'
    displayName   = "$($manifest.Name) $tag"
    releaseType   = $ReleaseType
  } | ConvertTo-Json -Depth 5 -Compress

  # PS 5.1 has no Invoke-RestMethod -Form; build multipart/form-data manually.
  $boundary = "----PSBoundary$([guid]::NewGuid().ToString('N'))"
  $LF = "`r`n"
  $enc = [System.Text.Encoding]::UTF8
  $ms = New-Object System.IO.MemoryStream
  function Add-Bytes([System.IO.Stream]$s, [byte[]]$b) { $s.Write($b, 0, $b.Length) }
  Add-Bytes $ms $enc.GetBytes("--$boundary$LF")
  Add-Bytes $ms $enc.GetBytes("Content-Disposition: form-data; name=`"metadata`"$LF$LF$metadata$LF")
  Add-Bytes $ms $enc.GetBytes("--$boundary$LF")
  $fileName = [System.IO.Path]::GetFileName($zipPath)
  Add-Bytes $ms $enc.GetBytes("Content-Disposition: form-data; name=`"file`"; filename=`"$fileName`"$LF")
  Add-Bytes $ms $enc.GetBytes("Content-Type: application/zip$LF$LF")
  Add-Bytes $ms ([System.IO.File]::ReadAllBytes($zipPath))
  Add-Bytes $ms $enc.GetBytes("$LF--$boundary--$LF")
  $body = $ms.ToArray(); $ms.Dispose()

  $uploadUri = "$($cfg.ApiBaseUrl)/projects/$($cfg.ProjectId)/upload-file"
  $resp = Invoke-RestMethod -Method Post -Uri $uploadUri -Headers @{ 'X-Api-Token' = $token } -ContentType "multipart/form-data; boundary=$boundary" -Body $body
  Write-Host "    CurseForge fileId: $($resp.id)" -ForegroundColor Green
}

# --- GitHub release ---
if (-not $SkipGitHub) {
  $gh = Get-Command gh -ErrorAction SilentlyContinue
  if (-not $gh) {
    Write-Warning "gh CLI not installed - skipping GitHub release. Install: winget install --id GitHub.cli"
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
