# MusicFlow Account UX v3: role task guidance, empty/error feedback and large-text UX.
# Local fixtures only; no passwords or Supabase mutations.
param([string]$Serial = '')

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adb = Join-Path $sdk 'platform-tools\adb.exe'

Push-Location $root
try {
    & .\gradlew.bat :app:testDebugUnitTest `
        --tests 'com.example.musicflow.core.account.AccountTaskGuideTest' `
        --tests 'com.example.musicflow.core.account.AccountHomePlanTest' `
        --tests 'com.example.musicflow.core.account.AccountWorkspacePlanTest' `
        --tests 'com.example.musicflow.core.account.MusicFlowAccessTest' `
        --tests 'com.example.musicflow.core.account.AccountTierTest' `
        :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'ACCOUNT_UX_V3_GRADLE_FAILED' }
    Write-Output 'ACCOUNT_UX_V3_BUILD_AND_JVM=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }
        $classes = 'com.example.musicflow.ui.AccountTaskUxV3UiTest,' +
            'com.example.musicflow.ui.AccountHomeV2UiTest,' +
            'com.example.musicflow.ui.AccountWorkspacesUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest,' +
            'com.example.musicflow.ui.CreatorApplicationReviewUiTest,' +
            'com.example.musicflow.ui.CreatorDraftStudioUiTest'
        $results = @(& $adb -s $Serial shell am instrument -w -r -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $results | Select-Object -Last 70
        if ($LASTEXITCODE -ne 0 -or !($results -match 'OK \(28 tests\)')) {
            throw 'ACCOUNT_UX_V3_ANDROID_UI_FAILED'
        }
        Write-Output 'ACCOUNT_UX_V3_ANDROID_UI=28_PASS'
    } else {
        Write-Output 'ACCOUNT_UX_V3_ANDROID_UI=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'ACCOUNT_UX_V3_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
