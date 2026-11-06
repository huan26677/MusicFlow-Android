# Targeted, non-production MusicFlow Owner Demo Readiness v2 checks.
# No passwords, owner login, or Supabase mutations. Never starts an emulator.
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
        --tests 'com.example.musicflow.core.account.OwnerDemoSnapshotTest' `
        :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'OWNER_DEMO_GRADLE_FAILED' }
    Write-Output 'OWNER_DEMO_GRADLE=PASS'

    $html = Get-Content 'admin-web/index.html' -Raw -Encoding UTF8
    $javascript = [regex]::Match($html, '(?s)<script>(.*?)</script>')
    if (!$javascript.Success) { throw 'ADMIN_WEB_SCRIPT_MISSING' }
    $temp = Join-Path $env:TEMP 'musicflow-owner-readiness-v2-syntax.js'
    try {
        [IO.File]::WriteAllText($temp, $javascript.Groups[1].Value,
            [Text.UTF8Encoding]::new($false))
        & node --check $temp
        if ($LASTEXITCODE -ne 0) { throw 'ADMIN_WEB_JS_SYNTAX_FAILED' }
    } finally {
        Remove-Item $temp -ErrorAction SilentlyContinue
    }
    Write-Output 'ADMIN_WEB_JS_SYNTAX=PASS'

    if ($Serial) {
        # The caller must explicitly select their QA device. Never use the default emulator.
        $devices = @(& $adb devices)
        if (!(($devices | Select-String ('^' + [regex]::Escape($Serial) + '\s+device')))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'QA_TEST_INSTALL_FAILED' }
        $classes = 'com.example.musicflow.ui.OwnerDemoDashboardUiTest,' +
            'com.example.musicflow.ui.MusicFlowRolesUiTest'
        $result = @(& $adb -s $Serial shell am instrument -w -r -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $result | Select-Object -Last 30
        # New targeted role tests may be added later: keep the v2 baseline (8)
        # as the minimum rather than making this script fail on a larger suite.
        $completed = [regex]::Match(($result -join "`n"), 'OK \((\d+) tests\)')
        if ($LASTEXITCODE -ne 0 -or !$completed.Success -or
            [int]$completed.Groups[1].Value -lt 8) {
            throw 'QA_OWNER_AND_ROLE_UI_FAILED'
        }
        Write-Output ('QA_OWNER_AND_ROLE_UI=' + $completed.Groups[1].Value + '_PASS')
    } else {
        Write-Output 'QA_OWNER_AND_ROLE_UI=NOT_RUN_NO_SERIAL'
    }
    Write-Output 'OWNER_DEMO_READINESS_V2=PASS'
} finally {
    Pop-Location
}
