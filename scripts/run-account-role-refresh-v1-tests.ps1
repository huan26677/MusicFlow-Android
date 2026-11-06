# MusicFlow demo: selected account role access refresh UI, fail-closed regression.
# Requires an explicitly selected QA emulator for UI. Never starts/stops other devices.
# Never loads a password, access token, or service-role credential.
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
        --tests 'com.example.musicflow.core.account.MusicFlowAccessTest' `
        :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'ROLE_REFRESH_GRADLE_FAILED' }
    Write-Output 'ROLE_REFRESH_GRADLE=PASS'

    & node 'scripts/test-admin-web-role-refresh-v1.js'
    if ($LASTEXITCODE -ne 0) { throw 'ROLE_REFRESH_ADMIN_WEB_TEST_FAILED' }
    Write-Output 'ROLE_REFRESH_ADMIN_WEB=4_PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }
        $output = @(& $adb -s $Serial shell am instrument -w -r -e class `
            'com.example.musicflow.ui.MusicFlowRolesUiTest' `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $output | Select-Object -Last 28
        if ($LASTEXITCODE -ne 0 -or !($output -match 'OK \(7 tests\)')) {
            throw 'ROLE_REFRESH_UI_FAILED'
        }
        Write-Output 'ROLE_REFRESH_ANDROID_UI=7_PASS'
    } else {
        Write-Output 'ROLE_REFRESH_ANDROID_UI=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'ROLE_REFRESH_V1=PASS'
} finally {
    Pop-Location
}
