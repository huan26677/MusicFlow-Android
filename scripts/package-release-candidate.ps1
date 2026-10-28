param(
    [switch]$Signed,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist"
$buildApk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
$buildUnsigned = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"

Push-Location $root
try {
    # No private keys, passwords or keystore contents are logged.
    $commit = (& git rev-parse HEAD | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or !$commit) { throw "GIT_HEAD_UNAVAILABLE" }

    $required = @(
        "MUSICFLOW_KEYSTORE_PATH",
        "MUSICFLOW_STORE_PASSWORD",
        "MUSICFLOW_KEY_ALIAS",
        "MUSICFLOW_KEY_PASSWORD"
    )
    $set = @($required | Where-Object {
        ![string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_))
    }).Count
    if ($Signed -and $set -ne $required.Count) {
        throw "SIGNED_RELEASE_REQUIRES_ALL_FOUR_MUSICFLOW_SIGNING_VARIABLES"
    }
    if (!$Signed -and $set -gt 0) {
        throw "SIGNING_VARIABLES_PRESENT: use -Signed or unset variables"
    }

    if (!$SkipBuild) {
        & .\gradlew.bat :app:assembleRelease --console=plain
        if ($LASTEXITCODE -ne 0) { throw "ASSEMBLE_RELEASE_FAILED" }
    }

    $source = if ($Signed) { $buildApk } else { $buildUnsigned }
    if (!(Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "EXPECTED_RELEASE_APK_NOT_FOUND: $source"
    }
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
    $mode = if ($Signed) { "signed" } else { "unsigned" }
    $name = "musicflow-rc-$($commit.Substring(0,8))-$mode.apk"
    $target = Join-Path $dist $name
    Copy-Item -LiteralPath $source -Destination $target -Force
    $sha256 = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash

    $metadata = @(
        "MusicFlow release candidate bundle"
        "COMMIT=$commit"
        "BUILD_TYPE=release"
        "SIGNING=$mode"
        "APK=$name"
        "SHA256=$sha256"
        "DATE=$(Get-Date -Format o)"
        "TEST_STATUS=NOT_RUN"
        "RELEASE_APPROVAL=PENDING"
    )
    $report = Join-Path $dist "musicflow-rc-$($commit.Substring(0,8))-$mode.txt"
    $metadata | Set-Content -LiteralPath $report -Encoding UTF8
    $metadata | ForEach-Object { Write-Host $_ }
    Write-Host "REPORT=$report"
} finally {
    Pop-Location
}
