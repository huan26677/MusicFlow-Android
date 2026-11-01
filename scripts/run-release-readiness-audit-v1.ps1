# MusicFlow Release Readiness v1 — repeatable, emulator-only / artifact audit.
# A PASS here means QA checks were executed, NOT approval to distribute.
# Never creates a signing key, signs a release with production credentials, or uploads.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$logFile = Join-Path $root "qa-logs\release-readiness-v1.log"
$passed = $false
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null
Set-Content -LiteralPath $logFile -Value "MusicFlow Release Readiness Part 4 — internal audit" -Encoding UTF8

function Log([string]$text) {
    Write-Host $text
    Add-Content -LiteralPath $logFile -Value $text -Encoding UTF8
}
function Native([string]$phase, [string]$binary, [string[]]$arguments) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $lines = @(& $binary @arguments 2>&1 | ForEach-Object { [string]$_ })
        $status = [int]$LASTEXITCODE
    } finally {
        $ErrorActionPreference = $old
    }
    Log "$($phase)_EXIT=$status"
    if ($status -ne 0) {
        $lines | Select-Object -Last 15 | ForEach-Object { Log "$phase $_" }
        throw "$($phase)_FAILED"
    }
    return $lines
}

Push-Location $root
try {
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse HEAD)"
    Log "SCOPE=EMULATOR_AND_ARTIFACT_ONLY"
    Log "PHYSICAL_DEVICE_QA=WAIVED_BY_PROJECT_OWNER"
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $buildTools = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (!$buildTools) { throw "ANDROID_BUILD_TOOLS_NOT_FOUND" }
    $aapt = Join-Path $buildTools.FullName "aapt.exe"
    if (!(Test-Path $aapt)) { throw "AAPT_NOT_FOUND" }
    $buildOutput = Native "GRADLE_ASSEMBLE_BUNDLE_JVM" ".\gradlew.bat" @(
        ":app:assembleRelease", ":app:bundleRelease", ":app:testDebugUnitTest",
        "--console=plain"
    )
    if (!($buildOutput -match "BUILD SUCCESSFUL")) { throw "GRADLE_BUILD_NOT_SUCCESSFUL" }
    Log "GRADLE_RELEASE_APK_AAB_JVM=PASS"

    $reports = @(Get-ChildItem "app\build\test-results\testDebugUnitTest" -Filter "TEST-*.xml" -File)
    if ($reports.Count -eq 0) { throw "MISSING_JVM_XML" }
    $total = 0; $fail = 0; $skip = 0
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $total += [int]$xml.DocumentElement.GetAttribute("tests")
        $fail += [int]$xml.DocumentElement.GetAttribute("failures")
        $fail += [int]$xml.DocumentElement.GetAttribute("errors")
        $skip += [int]$xml.DocumentElement.GetAttribute("skipped")
    }
    Log "JVM_TOTAL=$total"
    Log "JVM_FAIL=$fail"
    Log "JVM_SKIP=$skip"
    if ($total -lt 105 -or $fail -ne 0 -or $skip -ne 0) {
        throw "JVM_QA_GATE_FAILED"
    }
    Log "JVM_QA_GATE=PASS"

    $apk = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"
    $aab = Join-Path $root "app\build\outputs\bundle\release\app-release.aab"
    foreach ($file in @($apk,$aab)) {
        if (!(Test-Path -LiteralPath $file)) { throw "EXPECTED_ARTIFACT_MISSING" }
    }
    $badging = Native "AAPT" $aapt @("dump", "badging", $apk)
    $pkg = $badging | Where-Object {
        $_ -match "^package: name='com.example.musicflow' versionCode='1' versionName='1.0'"
    }
    if (!$pkg -or !($badging -match "sdkVersion:'29'") -or
        !($badging -match "targetSdkVersion:'36'")) {
        throw "UNEXPECTED_PACKAGE_OR_VERSION"
    }
    if ($badging -match '^application-debuggable') { throw "RELEASE_APK_DEBUGGABLE" }
    Log "APK_ID_VERSION_MIN_TARGET=PASS"
    Log "APK_DEBUGGABLE=FALSE"
    Log "APK_UNSIGNED_SHA256=$((Get-FileHash $apk -Algorithm SHA256).Hash)"
    Log "APK_UNSIGNED_BYTES=$((Get-Item $apk).Length)"

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($aab)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName })
        if (!($entries -contains "base/manifest/AndroidManifest.xml") -or
            !($entries -contains "base/dex/classes.dex")) {
            throw "AAB_MISSING_REQUIRED_ENTRIES"
        }
        $signingEntries = @($entries | Where-Object {
            $_ -match '^META-INF/.*\.(RSA|DSA|EC)$'
        })
        if ($signingEntries.Count -ne 0) {
            throw "UNEXPECTED_AAB_SIGNATURE_IN_QA_AUDIT"
        }
    } finally {
        $zip.Dispose()
    }
    Log "AAB_CONTENT_STRUCTURE=PASS"
    Log "AAB_SIGNED=FALSE"
    Log "AAB_UNSIGNED_SHA256=$((Get-FileHash $aab -Algorithm SHA256).Hash)"
    Log "AAB_UNSIGNED_BYTES=$((Get-Item $aab).Length)"

    $source = Get-Content -LiteralPath "app\build.gradle.kts" -Raw
    if ($source -notmatch 'applicationId\s*=\s*"com\.example\.musicflow"' -or
        $source -notmatch 'versionCode\s*=\s*1' -or
        $source -notmatch 'optimization\s*\{\s*enable\s*=\s*false') {
        throw "RELEASE_CONFIG_CHANGED_REVIEW_REQUIRED"
    }
    if ($source -match 'signingConfig\s*=') {
        Log "RELEASE_SIGNING_CONFIG_PRESENT=REVIEW_REQUIRED"
    } else {
        Log "RELEASE_SIGNING_CONFIG_PRESENT=FALSE"
    }
    Log "APPLICATION_ID_FINALIZED=FALSE"
    Log "VERSION_STRATEGY_FINALIZED=FALSE"
    Log "R8_RELEASE_OPTIMIZATION_ENABLED=FALSE"
    Log "STORE_DISTRIBUTION_SELECTED=FALSE"
    Log "PRODUCTION_RELEASE_READY=FALSE"
    $passed = $true
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
} finally {
    Pop-Location
}
Log "FINAL_RESULT=$(if ($passed) { 'PASS' } else { 'FAIL' })"
if (!$passed) { exit 1 }
exit 0
