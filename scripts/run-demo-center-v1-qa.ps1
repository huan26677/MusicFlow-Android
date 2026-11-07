# One-batch academic Demo Center v1 QA: nine UI-only personas, local demo login,
# simulated payment, Supabase role fail-closed, offline playback and real SAF.
# Run only after all implementation is done, on an isolated QA AVD if provided.
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
    if ($LASTEXITCODE -ne 0) { throw 'DEMO_CENTER_BUILD_OR_JVM_FAILED' }
    Write-Output 'DEMO_CENTER_FULL_JVM_AND_BUILD=PASS'

    & node scripts/test-admin-web-role-refresh-v1.js
    if ($LASTEXITCODE -ne 0) { throw 'DEMO_CENTER_WEB_ADMIN_FAILED' }
    Write-Output 'DEMO_CENTER_WEB_ADMIN=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'DEMO_CENTER_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'DEMO_CENTER_TEST_INSTALL_FAILED' }

        $classes = 'com.example.musicflow.ui.DemoCenterV1UiTest,' +
            'com.example.musicflow.ui.DemoBillingUiTest,' +
            'com.example.musicflow.ui.AccountJourneysV4UiTest,' +
            'com.example.musicflow.ui.AccountHomeV2UiTest,' +
            'com.example.musicflow.ui.AccountTaskUxV3UiTest,' +
            'com.example.musicflow.ui.AccountWorkspacesUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest,' +
            'com.example.musicflow.ui.CreatorApplicationReviewUiTest,' +
            'com.example.musicflow.ui.CreatorDraftStudioUiTest,' +
            'com.example.musicflow.ui.PlaylistQuickAddUiTest,' +
            'com.example.musicflow.ui.PlaylistSearchUiTest,' +
            'com.example.musicflow.ui.ListeningJourneysV1UiTest,' +
            'com.example.musicflow.ui.MusicFlowUiPolishTest,' +
            'com.example.musicflow.core.playlist.PlaylistRepositoryTest,' +
            'com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest,' +
            'com.example.musicflow.core.playback.OfflineAcceptanceFlowTest,' +
            'com.example.musicflow.core.playback.QueueManagerScreenTest'

        $regression = @(& $adb -s $Serial shell am instrument -w -r `
            -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $regression | Select-Object -Last 108
        $summary = $regression | Select-String 'OK \((\d+) tests\)' | Select-Object -Last 1
        if ($LASTEXITCODE -ne 0 -or $null -eq $summary) {
            throw 'DEMO_CENTER_ANDROID_REGRESSION_FAILED'
        }
        $count = [int]$summary.Matches[0].Groups[1].Value
        if ($count -lt 70) { throw "DEMO_CENTER_ANDROID_TEST_COUNT_TOO_LOW=$count" }
        Write-Output "DEMO_CENTER_ANDROID_REGRESSION=$count`_PASS"

        $picker = @(& $adb -s $Serial shell am instrument -w -r `
            -e class com.example.musicflow.ui.PlaylistQuickAddRealSafUiTest `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $picker | Select-Object -Last 76
        if ($LASTEXITCODE -ne 0 -or !($picker -match 'OK \(1 test\)')) {
            throw 'DEMO_CENTER_REAL_SAF_FAILED'
        }
        Write-Output 'DEMO_CENTER_REAL_SAF=1_PASS'
    } else {
        Write-Output 'DEMO_CENTER_ANDROID=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'DEMO_CENTER_V1_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
