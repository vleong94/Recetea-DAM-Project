#Requires -Version 5.1
<#
.SYNOPSIS
    Builds an optimised Windows installer for Recetea using jlink + jpackage.

.DESCRIPTION
    1. Runs the full Maven build (compile + test + package).
    2. Invokes 'mvn javafx:jlink' to produce a stripped JRE image under
       target/recetea-runtime/ (~30-50 MB depending on the JDK installation).
    3. Invokes 'mvn javafx:jpackage' to wrap the image in a Windows .msi installer
       under target/installer/.

.PREREQUISITES
    - Amazon Corretto 24 (or any JDK 24) set as JAVA_HOME.
    - WiX Toolset 3.x on PATH  →  https://wixtoolset.org  (required for .msi output).
    - PostgreSQL must be reachable at localhost:5432 for the integration tests.
      Skip tests with  -SkipTests  if no DB is available during packaging.

.PARAMETER SkipTests
    Pass -SkipTests to skip the Surefire test phase.

.EXAMPLE
    .\package.ps1
    .\package.ps1 -SkipTests
#>
param(
    [switch]$SkipTests
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ── Detect JAVA_HOME ──────────────────────────────────────────────────────────
if (-not $env:JAVA_HOME) {
    $corretto = "C:\Users\$env:USERNAME\.jdks\corretto-24.0.2"
    if (Test-Path $corretto) {
        $env:JAVA_HOME = $corretto
    } else {
        Write-Error "JAVA_HOME is not set and Corretto 24 was not found at $corretto."
    }
}
Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan

# ── Step 1: Build ─────────────────────────────────────────────────────────────
$mvnFlags = if ($SkipTests) { "-DskipTests" } else { "" }
Write-Host "`n[1/3] Building project..." -ForegroundColor Cyan
& .\mvnw.cmd clean package $mvnFlags
if ($LASTEXITCODE -ne 0) { Write-Error "Maven build failed (exit $LASTEXITCODE)." }

# ── Step 2: jlink — optimised JRE image ───────────────────────────────────────
Write-Host "`n[2/3] Creating jlink runtime image..." -ForegroundColor Cyan
& .\mvnw.cmd javafx:jlink
if ($LASTEXITCODE -ne 0) { Write-Error "javafx:jlink failed (exit $LASTEXITCODE)." }

$imageSize = "{0:N1} MB" -f ((Get-ChildItem "target\recetea-runtime" -Recurse | Measure-Object -Property Length -Sum).Sum / 1MB)
Write-Host "    Runtime image size: $imageSize" -ForegroundColor Green

# ── Step 3: jpackage — Windows installer (.msi) ────────────────────────────────
Write-Host "`n[3/3] Packaging installer (requires WiX Toolset on PATH)..." -ForegroundColor Cyan
& .\mvnw.cmd javafx:jpackage
if ($LASTEXITCODE -ne 0) { Write-Error "javafx:jpackage failed (exit $LASTEXITCODE)." }

Write-Host "`nDone. Installer written to target\installer\" -ForegroundColor Green
Get-ChildItem "target\installer" -Filter "*.msi" | ForEach-Object {
    $size = "{0:N1} MB" -f ($_.Length / 1MB)
    Write-Host "    $($_.Name)  ($size)" -ForegroundColor Green
}
