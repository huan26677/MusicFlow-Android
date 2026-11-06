# Unified MusicFlow account UX V4 QA: run after all feature work is complete.
# Offline/mock Android UI tests, complete JVM tests and Web Admin role regression.
# Never accesses saved passwords, grants live roles or mutates Supabase.
# The optional -Serial must point to a newly created isolated QA emulator.
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
    if ($LASTEXITCODE -ne 0) { throw 'ACCOUNT_JOURNEYS_V4_GRADLE_FAILED' }
    Write-Output 'ACCOUNT_JOURNEYS_V4_FULL_JVM_BUILD=PASS'

    & node scripts/test-admin-web-role-refresh-v1.js
    if ($LASTEXITCODE -ne 0) { throw 'WEB_ADMIN_ROLE_REGRESSION_FAILED' }
    Write-Output 'ACCOUNT_JOURNEYS_V4_WEB_ADMIN=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }

        $classes = 'com.example.musicflow.ui.AccountJourneysV4UiTest,' +
            'com.example.musicflow.ui.AccountHomeV2UiTest,' +
            'com.example.musicflow.ui.AccountTaskUxV3UiTest,' +
            'com.example.musicflow.ui.AccountWorkspacesUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest,' +
            'com.example.musicflow.ui.CreatorApplicationReviewUiTest,' +
            'com.example.musicflow.ui.CreatorDraftStudioUiTest,' +
            'com.example.musicflow.ui.OwnerDemoDashboardUiTest,' +
            'com.example.musicflow.ui.CreatorPrivateAttachmentUiTest'
        $results = @(& $adb -s $Serial shell am instrument -w -r -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $results | Select-Object -Last 85
        $summary = ($results | Select-String 'OK \((\d+) tests\)' | Select-Object -Last 1)
        if ($LASTEXITCODE -ne 0 -or $null -eq $summary) {
            throw 'ACCOUNT_JOURNEYS_V4_INSTRUMENTATION_FAILED'
        }
        $count = [int]$summary.Matches[0].Groups[1].Value
        if ($count -lt 36) { throw "ACCOUNT_JOURNEYS_V4_TEST_COUNT_TOO_LOW=$count" }
        Write-Output "ACCOUNT_JOURNEYS_V4_ANDROID_UI=$count`_PASS"
    } else {
        Write-Output 'ACCOUNT_JOURNEYS_V4_ANDROID_UI=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'ACCOUNT_JOURNEYS_V4_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
