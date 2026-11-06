# Package one INSTALLABLE MusicFlow demo APK for a school/report demonstration.
# Uses the existing local Android DEBUG keystore for QA/demo ONLY.
# Not for Google Play, public distribution, production or sensitive accounts.
# Does not push/upload artifacts, reset Git, or alter emulator/app data.
param(
    [ValidateSet(
        "MusicFlow-1.0-Demo-Android.apk",
        "MusicFlow-1.0-Demo-AuthFeedback-Android.apk",
        "MusicFlow-1.0-Demo-LocalAccount-Android.apk",
        "MusicFlow-1.0-Demo-SupabaseSignup-Android.apk",
        "MusicFlow-1.0-Demo-AuthUX-Android.apk",
        "MusicFlow-1.0-Demo-UXFoundationV3-Android.apk",
        "MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk",
        "MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk",
        "MusicFlow-1.0-Demo-SearchUXV3-Android.apk",
        "MusicFlow-1.0-Demo-RolesMVP-Android.apk",
        "MusicFlow-1.0-Demo-CreatorStudioV1-Android.apk",
        "MusicFlow-1.0-Demo-CreatorPrivateFilesV2-Android.apk",
        "MusicFlow-1.0-Demo-ModeratorReviewV1-Android.apk",
        "MusicFlow-1.0-Demo-OwnerDashboardV1-Android.apk",
        "MusicFlow-1.0-Demo-OwnerReadinessV2-Android.apk",
        "MusicFlow-1.0-Demo-RoleRefreshV1-Android.apk",
        "MusicFlow-1.0-Demo-CreatorReviewV2-Android.apk",
        "MusicFlow-1.0-Demo-AccountWorkspacesV1-Android.apk",
        "MusicFlow-1.0-Demo-AccountHomeV2-Android.apk",
        "MusicFlow-1.0-Demo-AccountUXV3-Android.apk",
        "MusicFlow-1.0-Demo-AccountJourneysV4-Android.apk",
        "MusicFlow-1.0-Demo-ListeningJourneysV1-Android.apk",
        "MusicFlow-1.0-Demo-PlaylistQuickAddV1-Android.apk"
    )]
    [string]$OutputName = "MusicFlow-1.0-Demo-Android.apk"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root "dist"
$temp = Join-Path $root "app\build\rc-qa\demo-apk"
$logName = switch ($OutputName) {
    "MusicFlow-1.0-Demo-Android.apk" { "demo-apk-package-v1.log" }
    "MusicFlow-1.0-Demo-AuthFeedback-Android.apk" {
        "demo-apk-auth-feedback-package-v1.log"
    }
    "MusicFlow-1.0-Demo-LocalAccount-Android.apk" {
        "demo-apk-local-account-package-v1.log"
    }
    "MusicFlow-1.0-Demo-SupabaseSignup-Android.apk" {
        "demo-apk-supabase-signup-package-v1.log"
    }
    "MusicFlow-1.0-Demo-AuthUX-Android.apk" {
        "demo-apk-auth-ux-package-v1.log"
    }
    "MusicFlow-1.0-Demo-UXFoundationV3-Android.apk" {
        "demo-apk-ux-foundation-v3-package.log"
    }
    "MusicFlow-1.0-Demo-PlayerLibraryUXV3-Android.apk" {
        "demo-apk-player-library-ux-v3-package.log"
    }
    "MusicFlow-1.0-Demo-PlaylistUXV3-Android.apk" {
        "demo-apk-playlist-ux-v3-package.log"
    }
    "MusicFlow-1.0-Demo-SearchUXV3-Android.apk" {
        "demo-apk-search-ux-v3-package.log"
    }
    "MusicFlow-1.0-Demo-RolesMVP-Android.apk" {
        "demo-apk-roles-mvp-package.log"
    }
    "MusicFlow-1.0-Demo-CreatorStudioV1-Android.apk" {
        "demo-apk-creator-studio-v1-package.log"
    }
    "MusicFlow-1.0-Demo-CreatorPrivateFilesV2-Android.apk" {
        "demo-apk-creator-private-files-v2-package.log"
    }
    "MusicFlow-1.0-Demo-ModeratorReviewV1-Android.apk" {
        "demo-apk-moderator-review-v1-package.log"
    }
    "MusicFlow-1.0-Demo-OwnerDashboardV1-Android.apk" {
        "demo-apk-owner-dashboard-v1-package.log"
    }
    "MusicFlow-1.0-Demo-OwnerReadinessV2-Android.apk" {
        "demo-apk-owner-readiness-v2-package.log"
    }
    "MusicFlow-1.0-Demo-RoleRefreshV1-Android.apk" {
        "demo-apk-role-refresh-v1-package.log"
    }
    "MusicFlow-1.0-Demo-CreatorReviewV2-Android.apk" {
        "demo-apk-creator-review-v2-package.log"
    }
    "MusicFlow-1.0-Demo-AccountWorkspacesV1-Android.apk" {
        "demo-apk-account-workspaces-v1-package.log"
    }
    "MusicFlow-1.0-Demo-AccountHomeV2-Android.apk" {
        "demo-apk-account-home-v2-package.log"
    }
    "MusicFlow-1.0-Demo-AccountUXV3-Android.apk" {
        "demo-apk-account-ux-v3-package.log"
    }
    "MusicFlow-1.0-Demo-AccountJourneysV4-Android.apk" {
        "demo-apk-account-journeys-v4-package.log"
    }
    "MusicFlow-1.0-Demo-ListeningJourneysV1-Android.apk" {
        "demo-apk-listening-journeys-v1-package.log"
    }
    "MusicFlow-1.0-Demo-PlaylistQuickAddV1-Android.apk" {
        "demo-apk-playlist-quick-add-v1-package.log"
    }
}
$log = Join-Path $root ("qa-logs\" + $logName)
$filename = $OutputName
$destination = Join-Path $dist $filename
$wasSuccessful = $false

New-Item -ItemType Directory -Force -Path (Split-Path $log) | Out-Null
Set-Content -LiteralPath $log -Value "MusicFlow Android demo APK / non-production" -Encoding UTF8
function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}
function Native([string]$name, [string]$exe, [string[]]$arguments) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = @(& $exe @arguments 2>&1 | ForEach-Object { [string]$_ })
        $exitCode = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $previous }
    if ($exitCode -ne 0) {
        $result | Select-Object -Last 18 | ForEach-Object { Log "$name $_" }
        throw "$($name)_EXIT_$exitCode"
    }
    Log "$($name)_EXIT=0"
    return $result
}

Push-Location $root
try {
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse HEAD)"
    Log "ARTIFACT_TYPE=DEMO_SIDELOAD_APK"
    Log "SIGNING=ANDROID_DEBUG_KEY_QA_DEMO_ONLY"
    Log "GOOGLE_PLAY_OR_PRODUCTION_RELEASE=FALSE"
    $buildFile = Get-Content -LiteralPath "app\build.gradle.kts" -Raw -Encoding UTF8
    if ($buildFile -notmatch 'applicationId\s*=\s*"com\.example\.musicflow"' -or
        $buildFile -notmatch 'versionCode\s*=\s*1' -or
        $buildFile -notmatch 'versionName\s*=\s*"1\.0"') {
        throw "APP_ID_OR_VERSION_CHANGED_UPDATE_DEMO_SCRIPT"
    }
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
        $env:ANDROID_SDK_ROOT
    } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $buildTools = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (!$buildTools) { throw "ANDROID_BUILD_TOOLS_MISSING" }
    $align = Join-Path $buildTools.FullName "zipalign.exe"
    $signer = Join-Path $buildTools.FullName "apksigner.bat"
    $aapt = Join-Path $buildTools.FullName "aapt.exe"
    $key = Join-Path $env:USERPROFILE ".android\debug.keystore"
    foreach ($path in @($align,$signer,$aapt,$key)) {
        if (!(Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "DEMO_BUILD_REQUIREMENT_MISSING"
        }
    }

    $build = Native "GRADLE_RELEASE_BUILD" ".\gradlew.bat" @(
        ":app:assembleRelease", "--console=plain"
    )
    if (!($build -match 'BUILD SUCCESSFUL')) { throw "GRADLE_NO_SUCCESS_MARKER" }
    Log "GRADLE_RELEASE_BUILD=PASS"
    $unsigned = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"
    if (!(Test-Path -LiteralPath $unsigned)) { throw "UNSIGNED_RELEASE_NOT_FOUND" }
    New-Item -ItemType Directory -Force -Path $dist,$temp | Out-Null
    $aligned = Join-Path $temp "demo-aligned.apk"
    $signed = Join-Path $temp $filename
    Native "ZIPALIGN" $align @("-f","-p","4",$unsigned,$aligned) | Out-Null
    Native "SIGN_DEMO" $signer @(
        "sign","--ks",$key,"--ks-key-alias","androiddebugkey",
        "--ks-pass","pass:android","--key-pass","pass:android",
        "--out",$signed,$aligned
    ) | Out-Null

    $verify = Native "VERIFY_APK" $signer @("verify","--verbose","--print-certs",$signed)
    if (!($verify -match '^Verifies$') -or
        !($verify -match 'Verified using v3 scheme .*true') -or
        !($verify -match 'CN=Android Debug')) { throw "APK_DEMO_SIGNATURE_INVALID" }
    Log "DEMO_SIGNATURE_VERIFIED=PASS"
    $badge = Native "AAPT" $aapt @("dump","badging",$signed)
    if (!($badge -match "^package: name='com.example.musicflow' versionCode='1' versionName='1.0'") -or
        !($badge -match "sdkVersion:'29'") -or
        !($badge -match "targetSdkVersion:'36'") -or
        $badge -match '^application-debuggable') {
        throw "APK_DEMO_MANIFEST_INVALID"
    }
    Log "PACKAGE=com.example.musicflow"
    Log "VERSION_CODE=1"
    Log "VERSION_NAME=1.0"
    Log "MIN_SDK=29"
    Log "TARGET_SDK=36"
    Log "APK_DEBUGGABLE=FALSE"

    $newHash = (Get-FileHash -LiteralPath $signed -Algorithm SHA256).Hash
    if (Test-Path -LiteralPath $destination) {
        $existingHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
        if ($existingHash -ne $newHash) {
            throw "DIST_DEMO_APK_ALREADY_EXISTS_DIFFERENT_HASH_PRESERVED"
        }
        Log "DIST_EXISTING_IDENTICAL_APK_REUSED=TRUE"
    } else {
        Copy-Item -LiteralPath $signed -Destination $destination -ErrorAction Stop
    }
    $actualHash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
    if ($actualHash -ne $newHash) { throw "DIST_APK_HASH_MISMATCH" }
    $hashLine = "$actualHash  $filename"
    Set-Content -LiteralPath ($destination+".sha256") -Value $hashLine -Encoding ASCII
    Log "APK_DEMO_PATH=$destination"
    Log "APK_DEMO_SIZE_BYTES=$((Get-Item -LiteralPath $destination).Length)"
    Log "APK_DEMO_SHA256=$actualHash"
    Log "DIST_APK_HASH_VERIFIED=PASS"
    Log "PRODUCTION_RELEASE_READY=FALSE"
    $wasSuccessful = $true
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
} finally {
    Pop-Location
}
Log "FINAL_RESULT=$(if ($wasSuccessful) { 'PASS' } else { 'FAIL' })"
if (!$wasSuccessful) { exit 1 }
exit 0
