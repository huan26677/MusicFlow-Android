# MusicFlow academic demo acceptance: one bundled round after changes are made.
# Isolated QA emulator only; no production auth, data sharing, billing, or Git push.
# SAF real DocumentsUI test is separate from in-memory UI regression to identify
# provider-dependent failures clearly.
param([string]$Serial = '')

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'

Push-Location $root
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug `
        :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'DEMO_ACCEPTANCE_BUILD_OR_JVM_FAILED' }
    Write-Output 'DEMO_ACCEPTANCE_BUILD_AND_FULL_JVM=PASS'

    & node scripts/test-admin-web-role-refresh-v1.js
    if ($LASTEXITCODE -ne 0) { throw 'DEMO_ACCEPTANCE_WEB_ADMIN_FAILED' }
    Write-Output 'DEMO_ACCEPTANCE_WEB_ADMIN=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }

        $classes = 'com.example.musicflow.ui.PlaylistQuickAddUiTest,' +
            'com.example.musicflow.ui.PlaylistSearchUiTest,' +
            'com.example.musicflow.ui.ListeningJourneysV1UiTest,' +
            'com.example.musicflow.ui.AccountJourneysV4UiTest,' +
            'com.example.musicflow.ui.AccountHomeV2UiTest,' +
            'com.example.musicflow.ui.AccountTaskUxV3UiTest,' +
            'com.example.musicflow.ui.AccountWorkspacesUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest,' +
            'com.example.musicflow.ui.CreatorApplicationReviewUiTest,' +
            'com.example.musicflow.ui.CreatorDraftStudioUiTest,' +
            'com.example.musicflow.core.playlist.PlaylistRepositoryTest,' +
            'com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest,' +
            'com.example.musicflow.core.playback.OfflineAcceptanceFlowTest,' +
            'com.example.musicflow.core.playback.QueueManagerScreenTest'

        $regression = @(& $adb -s $Serial shell am instrument -w -r `
            -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $regression | Select-Object -Last 90
        $regressionSummary = $regression | Select-String 'OK \((\d+) tests\)' | Select-Object -Last 1
        if ($LASTEXITCODE -ne 0 -or $null -eq $regressionSummary) {
            throw 'DEMO_ACCEPTANCE_ANDROID_REGRESSION_FAILED'
        }
        $regressionCount = [int]$regressionSummary.Matches[0].Groups[1].Value
        if ($regressionCount -lt 44) { throw "ANDROID_REGRESSION_TOO_FEW_TESTS=$regressionCount" }
        Write-Output "DEMO_ACCEPTANCE_ANDROID_REGRESSION=$regressionCount`_PASS"

        # Actual DocumentsUI round-trip, selected fixture WAV, Room write and
        # persisted SAF grant: intentionally not interchangeable with mocks.
        $picker = @(& $adb -s $Serial shell am instrument -w -r `
            -e class com.example.musicflow.ui.PlaylistQuickAddRealSafUiTest `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $picker | Select-Object -Last 85
        if ($LASTEXITCODE -ne 0 -or !($picker -match 'OK \(1 test\)')) {
            throw 'DEMO_ACCEPTANCE_REAL_SAF_PICKER_FAILED'
        }
        Write-Output 'DEMO_ACCEPTANCE_REAL_SAF_PICKER=1_PASS'
    } else {
        Write-Output 'DEMO_ACCEPTANCE_ANDROID=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'DEMO_ACCEPTANCE_V1_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
