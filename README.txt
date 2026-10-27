MusicFlow - Real Picker SAF accessibility fix v1

GitHub HEAD reviewed: 9231b73aaa02754d24ffb8a5aecb004d856fcff9
Issue: Fast QA compiles and starts exactly one test, but second Compose UI
click cannot find any Compose hierarchy even though foreground is MusicFlow.
The Android permission dialog was not seen.

Unzip the package into E:\Du_An\MusicFlow so the PS1 appears in scripts\.
Run in that project directory:

powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fix-real-picker-accessibility-v1.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1

When DocumentsUI appears, choose the newly created musicflow_real_picker_*.wav
file with your mouse. This is still a MANUAL_ASSISTED QA run.

Do not run Full QA until Fast QA passes and both playback and persisted
READ markers are present. Updated source file is backed up to TEMP.
The patch aborts if expected source content does not match.
