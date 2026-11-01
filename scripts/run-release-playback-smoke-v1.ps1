param(
    [string]$DeviceId = "emulator-5554",
    [int]$ExpectedApi = 30,
    [ValidateSet("wav", "codec")][string]$FixtureMode = "wav",
    [string]$DemoApkPath = ""
)

# Release-mode, black-box playback test. Only synthetic WAVs in a unique QA folder.
# Refuses to replace any pre-existing MusicFlow installation.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
# Preserve Part 2's committed API 30 log; collect separate evidence for API 29/33/36.
$logName = if ($DemoApkPath -and $FixtureMode -eq "codec") {
    "demo-apk-smoke-codec-api$ExpectedApi-v1.log"
} elseif ($DemoApkPath -and $FixtureMode -eq "wav") {
    "demo-apk-smoke-wav-api$ExpectedApi-v1.log"
} elseif ($FixtureMode -eq "codec") {
    "release-codec-smoke-api$ExpectedApi-v1.log"
} elseif ($ExpectedApi -eq 30) {
    "release-playback-smoke-v1.log"
} else {
    "release-playback-smoke-api$ExpectedApi-v1.log"
}
$log = Join-Path $root ("qa-logs\" + $logName)
$diagnostic = Join-Path $root (
    "qa-logs\release-playback-" + $FixtureMode + "-api" +
    $ExpectedApi + "-diagnostics-v1.log"
)
$work = Join-Path $root "app\build\rc-qa\release-playback"
$pkg = "com.example.musicflow"
$stamp = Get-Date -Format "yyyyMMdd_HHmmss_fffffff"
$folder = "/sdcard/Music/MusicFlow_RC2_$stamp"
$uiDump = "/sdcard/musicflow_rc2_$stamp.xml"
$trackPrefix = if ($FixtureMode -eq "codec") {
    "AAA_MusicFlow_CODEC_$stamp"
} else {
    "AAA_MusicFlow_RC2_$stamp"
}
$tracks = @("${trackPrefix}_A", "${trackPrefix}_B")
$extensions = if ($FixtureMode -eq "codec") { @("mp3", "flac") } else { @("wav", "wav") }
$ids = @()
$files = @()
$installed = $false
$folderCreated = $false
$passed = $false
$preexistingApp = $false
New-Item -ItemType Directory -Force -Path (Split-Path $log),$work | Out-Null
Set-Content -LiteralPath $log -Value "MusicFlow RC2 release-mode playback QA" -Encoding UTF8

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}
function Run([string]$phase, [string]$exe, [string[]]$argv, [switch]$Quiet) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $out = @(& $exe @argv 2>&1 | ForEach-Object { [string]$_ })
        $status = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    if (!$Quiet) { $out | ForEach-Object { Log "$phase $_" } }
    if ($status -ne 0) { throw ($phase + "_EXIT_" + $status) }
    return $out
}
function Adb([string]$phase, [string[]]$argv, [switch]$Quiet) {
    return @(Run $phase $adb (@("-s",$DeviceId) + $argv) -Quiet:$Quiet)
}
function IsInstalled {
    $old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
    try {
        $out = @(& $adb -s $DeviceId shell pm path $pkg 2>&1)
        $status = $LASTEXITCODE
    } finally { $ErrorActionPreference = $old }
    return ($status -eq 0 -and ($out -join "") -match 'package:')
}
function MakeWav([string]$path) {
    $samples = 8000 * 40
    $stream = [System.IO.File]::Create($path)
    try {
        $w = New-Object System.IO.BinaryWriter($stream)
        $w.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
        $w.Write([int](36+$samples*2))
        $w.Write([System.Text.Encoding]::ASCII.GetBytes("WAVEfmt "))
        $w.Write([int]16);$w.Write([int16]1);$w.Write([int16]1)
        $w.Write([int]8000);$w.Write([int]16000);$w.Write([int16]2);$w.Write([int16]16)
        $w.Write([System.Text.Encoding]::ASCII.GetBytes("data"))
        $w.Write([int]($samples*2))
        $w.Write((New-Object byte[] ($samples*2)))
        $w.Flush()
    } finally { $stream.Dispose() }
    if ((Get-Item $path).Length -ne 640044) { throw "INVALID_WAV_FIXTURE" }
}
function Rows {
    return @(Adb "ROWS" @("shell","content","query","--uri",
        "content://media/external/audio/media","--projection",
        "_id:_display_name:is_pending:is_music:duration") -Quiet)
}
function FindId([string]$name, [string]$extension) {
    for ($i=0;$i -lt 25;$i++) {
        $row = Rows | Where-Object {
            $_ -match ("_display_name=" + [regex]::Escape($name+"."+$extension) + "(?:,|$)")
        } | Select-Object -First 1
        if ($row -and $row -match '_id=(\d+)') { return $Matches[1] }
        Start-Sleep -Milliseconds 400
    }
    throw ("QA_AUDIO_NOT_INDEXED_" + $name)
}
function UiTree {
    Adb "DUMP" @("shell","uiautomator","dump",$uiDump) -Quiet | Out-Null
    $raw = (Adb "XML" @("shell","cat",$uiDump) -Quiet) -join ""
    return [xml]$raw
}
function TapLabel([string]$label, [switch]$Scroll) {
    for ($i=0;$i -lt 13;$i++) {
        $xml = UiTree
        $node = $xml.SelectNodes("//node") | Where-Object {
            $_.GetAttribute("text") -eq $label
        } | Select-Object -First 1
        if ($node) {
            $m = [regex]::Match($node.GetAttribute("bounds"),
                '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$')
            if ($m.Success) {
                $x = [int](([int]$m.Groups[1].Value+[int]$m.Groups[3].Value)/2)
                $y = [int](([int]$m.Groups[2].Value+[int]$m.Groups[4].Value)/2)
                if ($x -gt 0 -and $y -gt 0) {
                    Adb "TAP" @("shell","input","tap","$x","$y") -Quiet | Out-Null
                    Log ("UI_TAPPED=" + $label)
                    return
                }
            }
        }
        if ($Scroll -and $i -gt 2 -and $i % 3 -eq 0) {
            Adb "SWIPE" @("shell","input","swipe","530","1640","530","960","350") -Quiet | Out-Null
        }
        Start-Sleep -Milliseconds 300
    }
    throw ("UI_LABEL_MISSING_" + $label)
}
function AssertSession([string]$title, [int]$state) {
    # Android 16 / API 36 prints PLAYING(3), PAUSED(2) in dumpsys,
    # whereas API 29/30/33 prints the numeric state (3, 2).
    $statePattern = switch ($state) {
        3 { '(?:PLAYING\(3\)|3)' }
        2 { '(?:PAUSED\(2\)|2)' }
        default { [regex]::Escape([string]$state) }
    }
    for ($i=0;$i -lt 16;$i++) {
        $dump = (Adb "MEDIA_SESSION" @("shell","dumpsys","media_session") -Quiet) -join [Environment]::NewLine
        $pattern = '(?s)package=com\.example\.musicflow.*?state=PlaybackState \{state=' +
            $statePattern + ',.*?metadata: size=\d+, description=' + [regex]::Escape($title) + ','
        if ($dump -match $pattern) {
            Log ("MEDIASESSION_STATE_" + $state + "=" + $title)
            return
        }
        Start-Sleep -Milliseconds 300
    }
    throw ("MEDIASESSION_NOT_EXPECTED_STATE_" + $state)
}

Push-Location $root
try {
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse HEAD)"
    Log "DEVICE=$DeviceId EXPECTED_API=$ExpectedApi"
    Log "SIGNING=DEBUG_KEY_QA_ONLY"
    Log "FIXTURE_MODE=$FixtureMode"
    Log "PREBUILT_DEMO_APK=$(if ($DemoApkPath) { 'TRUE' } else { 'FALSE' })"
    $sdk = if($env:ANDROID_HOME){$env:ANDROID_HOME}elseif($env:ANDROID_SDK_ROOT){
        $env:ANDROID_SDK_ROOT
    }else{Join-Path $env:LOCALAPPDATA "Android\Sdk"}
    $adb = Join-Path $sdk "platform-tools\adb.exe"
    $bt = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    $zip = Join-Path $bt.FullName "zipalign.exe"
    $signer = Join-Path $bt.FullName "apksigner.bat"
    $keystore = Join-Path $env:USERPROFILE ".android\debug.keystore"
    foreach($tool in @($adb,$zip,$signer,$keystore)) {
        if (!(Test-Path -LiteralPath $tool)) { throw "MISSING_QA_TOOL" }
    }
    $api = (Adb "API" @("shell","getprop","ro.build.version.sdk") -Quiet) -join ""
    $boot = (Adb "BOOT" @("shell","getprop","sys.boot_completed") -Quiet) -join ""
    if ($api.Trim() -ne "$ExpectedApi" -or $boot.Trim() -ne "1") {
        throw "EMULATOR_NOT_BOOTED_OR_WRONG_API"
    }
    if (IsInstalled) {
        $preexistingApp = $true
        throw "PREEXISTING_MUSICFLOW_DO_NOT_UNINSTALL"
    }
    if ($DemoApkPath) {
        $apk = (Resolve-Path -LiteralPath $DemoApkPath -ErrorAction Stop).Path
        $expectedFolder = [System.IO.Path]::GetFullPath((Join-Path $root "dist")) +
            [System.IO.Path]::DirectorySeparatorChar
        if (!$apk.StartsWith($expectedFolder,[System.StringComparison]::OrdinalIgnoreCase) -or
            [System.IO.Path]::GetFileName($apk) -ne "MusicFlow-1.0-Demo-Android.apk") {
            throw "DEMO_APK_MUST_BE_FROM_LOCAL_DIST"
        }
        Log "QA_USING_PREBUILT_DEMO_APK=TRUE"
    } else {
        Run "BUILD_RELEASE" ".\gradlew.bat" @(":app:assembleRelease","--console=plain") -Quiet | Out-Null
        Log "BUILD_RELEASE=PASS"
        $unsigned = Join-Path $root "app\build\outputs\apk\release\app-release-unsigned.apk"
        $aligned = Join-Path $work "rc2-aligned.apk"
        $apk = Join-Path $work "MusicFlow-1.0-RC2-QA.apk"
        Run "ALIGN" $zip @("-f","-p","4",$unsigned,$aligned) -Quiet | Out-Null
        Run "SIGN" $signer @("sign","--ks",$keystore,"--ks-key-alias","androiddebugkey",
            "--ks-pass","pass:android","--key-pass","pass:android",
            "--out",$apk,$aligned) -Quiet | Out-Null
    }
    $verify = Run "VERIFY" $signer @("verify","--verbose","--print-certs",$apk) -Quiet
    if (!($verify -match '^Verifies$') -or
        !($verify -match 'Verified using v3 scheme .*true') -or
        !($verify -match 'CN=Android Debug')) { throw "QA_SIGNATURE_FAILED" }
    Log "QA_APK_SIGNATURE=PASS"
    Log "QA_APK_SHA256=$((Get-FileHash $apk -Algorithm SHA256).Hash)"
    if ($FixtureMode -eq "codec") {
        $generator = Join-Path $root "scripts\generate-release-codec-fixtures.py"
        Run "CODEC_GENERATION" "python" @(
            $generator, "--output", $work, "--prefix", $trackPrefix
        ) | Out-Null
        Log "CODEC_MP3_FLAC_FIXTURES=PASS"
    }
    Adb "MKDIR" @("shell","mkdir",$folder) -Quiet | Out-Null
    $folderCreated = $true
    for ($index=0; $index -lt $tracks.Count; $index++) {
        $name = $tracks[$index]
        $extension = $extensions[$index]
        $local = Join-Path $work ($name+"."+$extension)
        if ($FixtureMode -eq "wav") { MakeWav $local }
        $remote = "$folder/$name.$extension"
        Adb "PUSH" @("push",$local,$remote) -Quiet | Out-Null
        $files += $remote
        Adb "SCAN" @("shell","am","broadcast","-a",
            "android.intent.action.MEDIA_SCANNER_SCAN_FILE","-d","file://$remote") -Quiet | Out-Null
        $id = FindId $name $extension
        $ids += $id
        # ADB push may mark a newly created media row as pending on API 30.
        # Only touch the two exact rows that this QA run just created.
        $publishArgs=@("shell","content","update","--uri",
            "content://media/external/audio/media/$id",
            "--bind","is_pending:i:0","--bind","is_music:i:1")
        if ($FixtureMode -eq "wav") {
            $publishArgs += @("--bind","duration:l:40000")
        }
        Adb "PUBLISH" $publishArgs -Quiet | Out-Null
    }
    $rows = Rows
    for ($index=0; $index -lt $tracks.Count; $index++) {
        $name=$tracks[$index]
        $ext=$extensions[$index]
        $matched = @($rows | Where-Object {
            $_ -match ("_display_name=" + [regex]::Escape("$name.$ext") + ", is_pending=0, is_music=1, duration=(\d+)")
        })
        if ($matched.Count -ne 1) { throw "QA_AUDIO_NOT_PUBLISHED_$ext" }
        if ($FixtureMode -eq "codec") {
            if ($matched[0] -notmatch 'duration=(\d+)' -or
                [int]$Matches[1] -lt 38000 -or [int]$Matches[1] -gt 43000) {
                throw "CODEC_DURATION_NOT_PARSED_$ext"
            }
        } elseif ($matched[0] -notmatch 'duration=40000') {
            throw "WAV_DURATION_INVALID"
        }
    }
    if ($FixtureMode -eq "codec") {
        $metadata = Adb "CODEC_METADATA" @("shell","content","query","--uri",
            "content://media/external/audio/media","--projection",
            "_display_name:title:artist:album:mime_type:duration") -Quiet
        foreach ($name in $tracks) {
            $line = @($metadata | Where-Object {
                $_ -match ([regex]::Escape("title=$name") + "(?:,|$)")
            }) | Select-Object -First 1
            if (!$line -or $line -notmatch 'artist=MusicFlow QA Artist' -or
                $line -notmatch 'album=MusicFlow QA Album') {
                throw "CODEC_METADATA_TAGS_NOT_FOUND_$name"
            }
        }
        Log "QA_MEDIASTORE_MP3_FLAC_METADATA_2_OF_2=PASS"
    } else {
        Log "QA_MEDIASTORE_WAV_2_OF_2=PASS"
    }
    Adb "INSTALL" @("install",$apk) -Quiet | Out-Null
    $installed = $true
    $permission = if($ExpectedApi -ge 33) {
        "android.permission.READ_MEDIA_AUDIO"
    }else{"android.permission.READ_EXTERNAL_STORAGE"}
    Adb "PERMISSION" @("shell","pm","grant",$pkg,$permission) -Quiet | Out-Null
    $launch = Adb "LAUNCH" @("shell","am","start","-W","-n",
        "$pkg/.MainActivity") -Quiet
    if(!($launch -match 'Status: ok')) { throw "RELEASE_APP_LAUNCH_FAILED" }
    TapLabel "Mở thư viện thiết bị"
    TapLabel $tracks[0] -Scroll
    AssertSession $tracks[0] 3
    Log "RELEASE_OFFLINE_PLAY=PASS"
    if ($FixtureMode -eq "codec") {
        Log "RELEASE_MP3_PLAY=PASS"
    }
    Adb "PAUSE" @("shell","input","keyevent","127") -Quiet | Out-Null
    AssertSession $tracks[0] 2
    Log "RELEASE_PAUSE=PASS"
    Adb "RESUME" @("shell","input","keyevent","126") -Quiet | Out-Null
    AssertSession $tracks[0] 3
    Log "RELEASE_RESUME=PASS"
    Adb "NEXT" @("shell","input","keyevent","87") -Quiet | Out-Null
    AssertSession $tracks[1] 3
    Log "RELEASE_NEXT=PASS"
    if ($FixtureMode -eq "codec") { Log "RELEASE_FLAC_PLAY=PASS" }
    Adb "HOME" @("shell","input","keyevent","3") -Quiet | Out-Null
    AssertSession $tracks[1] 3
    Log "RELEASE_BACKGROUND=PASS"
    $notifications = (Adb "NOTIFICATIONS" @("shell","dumpsys",
        "notification","--noredact") -Quiet) -join [Environment]::NewLine
    if(!($notifications -match 'NotificationRecord\([^\n]*pkg=com\.example\.musicflow') -or
       !($notifications -match 'android\.app\.Notification\$MediaStyle')){
        throw "MEDIA_NOTIFICATION_MISSING"
    }
    Log "RELEASE_MEDIA_NOTIFICATION=PASS"
    $passed = $true
}catch{
    Log ("FAIL_REASON=" + $_.Exception.Message)
    if ($adb -and (Test-Path -LiteralPath $adb)) {
        try {
            $sessionDump = @(& $adb -s $DeviceId shell dumpsys media_session 2>&1)
            $appDump = @(& $adb -s $DeviceId shell dumpsys activity activities 2>&1)
            $recentLogs = @(& $adb -s $DeviceId logcat -d -t 2500 2>&1)
            $importantLogs = @($recentLogs | Where-Object {
                $_ -match 'com\.example\.musicflow|AndroidRuntime|FATAL EXCEPTION|ForegroundService|MediaSession|ExoPlayer|MediaCodec|CCodec|PlayerImpl|PlaybackService|SecurityException'
            } | Select-Object -Last 150)
            @(
                ("DATE=" + (Get-Date -Format o)),
                ("FAIL_REASON=" + $_.Exception.Message),
                "--- DUMPSYS MEDIA_SESSION ---"
            ) + $sessionDump + @("--- DUMPSYS ACTIVITY ---") +
                @($appDump | Select-String 'ResumedActivity|mResumedActivity|com.example.musicflow' -Context 1,1 | ForEach-Object {$_}) +
                @("--- LOGCAT FILTERED ---") + $importantLogs |
                Set-Content -LiteralPath $diagnostic -Encoding UTF8
            Log ("FAIL_DIAGNOSTICS=" + $diagnostic)
        } catch {
            Log ("DIAGNOSTICS_CAPTURE_FAILED=" + $_.Exception.Message)
        }
    }
}finally{
    if($adb -and (Test-Path -LiteralPath $adb)){
        if($installed){
            & $adb -s $DeviceId shell am force-stop $pkg 2>&1 | Out-Null
            & $adb -s $DeviceId uninstall $pkg 2>&1 | Out-Null
        }
        foreach($id in $ids){
            & $adb -s $DeviceId shell content delete --uri (
                "content://media/external/audio/media/$id"
            ) 2>&1 | Out-Null
        }
        foreach($file in $files){
            & $adb -s $DeviceId shell rm -f $file 2>&1 | Out-Null
        }
        if($folderCreated){
            & $adb -s $DeviceId shell rmdir $folder 2>&1 | Out-Null
        }
        & $adb -s $DeviceId shell rm -f $uiDump 2>&1 | Out-Null
        $left = Rows | Where-Object { $_ -match [regex]::Escape($stamp) }
        if ($preexistingApp -and !$installed -and $ids.Count -eq 0 -and
            $files.Count -eq 0 -and !$folderCreated) {
            Log "QA_CLEANUP=NOT_NEEDED_PREEXISTING_APP_PRESERVED"
        } elseif (!(IsInstalled) -and @($left).Count -eq 0) {
            Log "QA_CLEANUP=PASS"
        } else {
            Log "QA_CLEANUP=FAIL"
            $passed = $false
        }
    }
    Pop-Location
}
Log "FINAL_RESULT=$(if($passed){'PASS'}else{'FAIL'})"
if(!$passed){exit 1}
exit 0
