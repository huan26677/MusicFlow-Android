# Visual Identity v1 consolidated QA. Run only AFTER all UI code changes.
# Creates NO account, cloud, payment or GitHub mutations; selected Android
# instrumentation must target an isolated QA emulator serial.
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
    if ($LASTEXITCODE -ne 0) { throw 'OCEAN_FLOW_BUILD_OR_JVM_FAILED' }
    Write-Output 'OCEAN_FLOW_FULL_JVM_AND_BUILD=PASS'

    & node scripts/test-admin-web-role-refresh-v1.js
    if ($LASTEXITCODE -ne 0) { throw 'OCEAN_FLOW_WEB_ADMIN_FAILED' }
    Write-Output 'OCEAN_FLOW_WEB_ADMIN=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'OCEAN_FLOW_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'OCEAN_FLOW_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'OCEAN_FLOW_TEST_INSTALL_FAILED' }

        $classes = 'com.example.musicflow.ui.OceanFlowVisualUiTest,' +
            'com.example.musicflow.ui.DemoCenterV1UiTest,' +
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
        $regression | Select-Object -Last 100
        $summary = $regression | Select-String 'OK \((\d+) tests\)' | Select-Object -Last 1
        if ($LASTEXITCODE -ne 0 -or $null -eq $summary) {
            throw 'OCEAN_FLOW_ANDROID_REGRESSION_FAILED'
        }
        $count = [int]$summary.Matches[0].Groups[1].Value
        if ($count -lt 77) { throw "OCEAN_FLOW_ANDROID_TOO_FEW_TESTS=$count" }
        Write-Output "OCEAN_FLOW_ANDROID_REGRESSION=$count`_PASS"

        $picker = @(& $adb -s $Serial shell am instrument -w -r `
            -e class com.example.musicflow.ui.PlaylistQuickAddRealSafUiTest `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $picker | Select-Object -Last 50
        if ($LASTEXITCODE -ne 0 -or !($picker -match 'OK \(1 test\)')) {
            throw 'OCEAN_FLOW_REAL_DOCUMENTS_UI_FAILED'
        }
        Write-Output 'OCEAN_FLOW_REAL_SAF=1_PASS'
    } else {
        Write-Output 'OCEAN_FLOW_ANDROID=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'OCEAN_FLOW_V1_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
