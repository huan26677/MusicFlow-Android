# Unified listener journey QA: run ONCE after implementation is complete.
# Full JVM + targeted Android Home/Search/Player/Playlist/Queue regression.
# No live Supabase writes, passwords, licensed online media, or repository changes.
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
    if ($LASTEXITCODE -ne 0) { throw 'LISTENING_JOURNEYS_GRADLE_FAILED' }
    Write-Output 'LISTENING_JOURNEYS_FULL_JVM_AND_BUILD=PASS'

    if ($Serial) {
        if (!(@(& $adb devices) -match ('^' + [regex]::Escape($Serial) + '\s+device'))) {
            throw 'SELECTED_QA_DEVICE_NOT_CONNECTED'
        }
        & $adb -s $Serial install -r 'app/build/outputs/apk/debug/app-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'LISTENING_JOURNEYS_APP_INSTALL_FAILED' }
        & $adb -s $Serial install -r 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
        if ($LASTEXITCODE -ne 0) { throw 'LISTENING_JOURNEYS_TEST_INSTALL_FAILED' }

        $classes = 'com.example.musicflow.ui.ListeningJourneysV1UiTest,' +
            'com.example.musicflow.ui.MusicFlowUxFoundationV3Test,' +
            'com.example.musicflow.ui.MusicFlowPlayerLibraryUxV3Test,' +
            'com.example.musicflow.ui.MusicFlowSearchUxV3Test,' +
            'com.example.musicflow.ui.MusicFlowPlaylistPolishV3Test,' +
            'com.example.musicflow.ui.PlaylistSearchUiTest,' +
            'com.example.musicflow.ui.MusicFlowUiPolishTest,' +
            'com.example.musicflow.core.playlist.PlaylistPlaybackOrderTest,' +
            'com.example.musicflow.core.playback.AdvancedSearchRuntimeTest,' +
            'com.example.musicflow.core.playback.QueueManagerScreenTest'
        $result = @(& $adb -s $Serial shell am instrument -w -r -e class $classes `
            'com.example.musicflow.test/androidx.test.runner.AndroidJUnitRunner')
        $result | Select-Object -Last 115
        $summary = $result | Select-String 'OK \((\d+) tests\)' | Select-Object -Last 1
        if ($LASTEXITCODE -ne 0 -or $null -eq $summary) {
            throw 'LISTENING_JOURNEYS_ANDROID_INSTRUMENTATION_FAILED'
        }
        $count = [int]$summary.Matches[0].Groups[1].Value
        if ($count -lt 25) { throw "LISTENING_JOURNEYS_ANDROID_COUNT_TOO_LOW=$count" }
        Write-Output "LISTENING_JOURNEYS_ANDROID_UI=$count`_PASS"
    } else {
        Write-Output 'LISTENING_JOURNEYS_ANDROID_UI=NOT_RUN_NO_QA_SERIAL'
    }
    Write-Output 'LISTENING_JOURNEYS_V1_FINAL_RESULT=PASS'
} finally {
    Pop-Location
}
