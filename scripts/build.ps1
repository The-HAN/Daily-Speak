[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$toolingRoot = Join-Path (Split-Path -Parent $repoRoot) '.tooling'

$javaHome = Join-Path $toolingRoot 'jdk-24'
$androidSdk = Join-Path $toolingRoot 'android-sdk'
$androidUserHome = Join-Path $toolingRoot 'android-user-home'
$gradleUserHome = Join-Path $toolingRoot 'gradle-user-home'

$requiredDirectories = @(
    $javaHome,
    $androidSdk,
    $androidUserHome,
    $gradleUserHome
)

foreach ($directory in $requiredDirectories) {
    if (-not (Test-Path -LiteralPath $directory)) {
        throw "Missing external tooling directory: $directory"
    }
}

# ANDROID_PREFS_ROOT and ANDROID_USER_HOME must not both be set:
# Android Gradle Plugin 8.11 rejects conflicting preference locations.
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue

$env:JAVA_HOME = $javaHome
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:ANDROID_USER_HOME = $androidUserHome
$env:GRADLE_USER_HOME = $gradleUserHome

if ($null -eq $GradleArgs -or $GradleArgs.Count -eq 0) {
    $GradleArgs = @(':app:assembleDebug', ':app:testDebugUnitTest')
}

Push-Location $repoRoot
try {
    & (Join-Path $repoRoot 'gradlew.bat') @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}