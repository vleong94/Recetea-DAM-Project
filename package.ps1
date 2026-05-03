#Requires -Version 5.1
param(
    [switch]$SkipTests,
    [string]$InstallerType = "app-image"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-OrFail {
    param([string]$Step)
    if ($LASTEXITCODE -ne 0) {
        Write-Error "$Step failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
}

$env:JAVA_HOME = "C:\Users\vleon\.jdks\corretto-24.0.2"
Write-Host "Utilizando JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Cyan

# Step 0: Moditect Patching (PostgreSQL, Okio, etc.)
$bytesJar = "$env:USERPROFILE\.m2\repository\at\favre\lib\bytes\1.5.0\bytes-1.5.0.jar"
$needsBootstrap = $true
if (Test-Path $bytesJar) {
    $modInfo = & "$env:JAVA_HOME\bin\jar.exe" --describe-module --file=$bytesJar 2>&1 | Out-String
    if ($modInfo -match "at\.favre\.lib\.bytes@") { $needsBootstrap = $false }
}
if ($needsBootstrap) {
    Write-Host "`n[0/3] Patching libraries (Moditect)..." -ForegroundColor Yellow
    & .\mvnw.cmd -Dmoditect.skip=false generate-resources
    Invoke-OrFail -Step "Moditect bootstrap"
}

# Step 1: Build
$mvnFlags = if ($SkipTests) { "-DskipTests" } else { "" }
Write-Host "`n[1/3] Building JAR..." -ForegroundColor Cyan
& .\mvnw.cmd clean package $mvnFlags
Invoke-OrFail -Step "Maven build"

# Step 2: jlink - Runtime Image
Write-Host "`n[2/3] Creating custom JRE (jlink)..." -ForegroundColor Cyan
& .\mvnw.cmd javafx:jlink
Invoke-OrFail -Step "javafx:jlink"

$runtimeImage = "target\recetea-runtime"
if (-not (Test-Path "$runtimeImage\bin\java.exe")) {
    Write-Error "Sanity Check Failed: java.exe not found at $runtimeImage\bin\java.exe"
    exit 1
}

# Step 3: jpackage - Portable Bundle
Write-Host "`n[3/3] Generating native portable bundle..." -ForegroundColor Cyan
if (Test-Path "target\installer") { Remove-Item "target\installer" -Recurse -Force }

$jpackageArgs = @(
    "--type", $InstallerType,
    "--name", "Recetea",
    "--runtime-image", $runtimeImage,
    "--module", "com.recetea/com.recetea.Main",
    "--dest", "target\installer",
    "--java-options", "-Denv=prod",
    "--java-options", "--enable-preview",
    # Java 24 native-access policy: javafx.graphics calls restricted JDK methods
    # (Glass / Prism native pipelines). Granting the module up front silences the
    # runtime warning and pre-empts the hard-block planned for a future JDK.
    "--java-options", "--enable-native-access=javafx.graphics",
    # Diagnostic: spawn a Win32 console alongside Recetea.exe so stdout/stderr
    # and SLF4J output are visible. Remove for the final user-facing build.
    "--win-console"
)
& "$env:JAVA_HOME\bin\jpackage.exe" @jpackageArgs
Invoke-OrFail -Step "jpackage"

if ($InstallerType -eq "app-image") {
    $appFolder = "target\installer\Recetea"
    if (-not (Test-Path "$appFolder\Recetea.exe")) {
        Write-Error "Portable EXE missing in $appFolder"
        exit 1
    }
    Write-Host "`n¡SUCCESS! Portable app generated at: $appFolder" -ForegroundColor Green
}
