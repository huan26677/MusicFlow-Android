# MusicFlow Real Picker SAF - Library Permission Dialog v1

Source review: GitHub main dfd8e452. On isolated Android 11 API30 Fast QA, the test clicked Home->Library and then failed trying to find the Compose UI when a permission dialog likely covered the screen.

Extract this archive inside E:\Du_An\MusicFlow (so the new file is in scripts).

1. powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fix-real-picker-library-permission-v1.ps1
2. powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly
3. powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1

Only one Android test runs in Fast QA. Keep the emulator visible, and manually choose the generated musicflow_real_picker_*.wav file if DocumentsUI opens.

Expected diagnostic markers: LIBRARY_FOREGROUND, LIBRARY_PERMISSION_CHECK, PASS libraryPickerButtonClicked. PASS is not claimed without playback + persisted READ assertions and FINAL_RESULT=PASS.

Script is ASCII encoded for Windows PowerShell 5.1 and preserves UTF-8 Kotlin/Markdown content. No production code changes.
