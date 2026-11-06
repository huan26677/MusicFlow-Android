# Repeatable Creator application UX and scope regression (demo/report only).
# Only uses local fake RPCs in Android tests; does not mutate Supabase accounts.
# Optional -Serial MUST be an emulator reserved for QA, not a user device.
param([string]$Serial = "")

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'

Push-Location $root
try {
    & .\gradlew.bat :app:testDebugUnitTest `
        --tests 'com.example.musicflow.core.account.CreatorApplicationUiRulesTest' `
        --tests 'com.example.musicflow.core.account.MusicFlowAccessTest' `
        :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'CREATOR_REVIEW_GRADLE_FAILED' }
    Write-Output 'CREATOR_REVIEW_BUILD_AND_JVM=PASS'

    if ($Serial) {
        $devices = @(& $adb devices)
        if (!(($devices | Select-String ('^' + [regex]::Escape($Serial) + '\s+device')))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }
        $classes = 'com.example.musicflow.ui.CreatorApplicationReviewUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest'
        $results = @(& $adb -s $Serial shell am instrument -w -r -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $results | Select-Object -Last 50
        if ($LASTEXITCODE -ne 0 -or !($results -match 'OK \(10 tests\)')) {
            throw 'CREATOR_REVIEW_ANDROID_TEST_FAILED'
        }
        Write-Output 'CREATOR_REVIEW_AND_ROLES_ANDROID_UI=10_PASS'
    } else {
        Write-Output 'CREATOR_REVIEW_ANDROID_UI=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'CREATOR_APPLICATION_REVIEW_V2=PASS'
} finally {
    Pop-Location
}
