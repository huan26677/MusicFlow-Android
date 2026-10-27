MusicFlow Fast QA - Compose hierarchy fix (2026-10-28)

1. Extract scripts/fix-real-picker-compose-rule-v1.ps1 into E:\Du_An\MusicFlow\scripts.
2. In PowerShell at E:\Du_An\MusicFlow run:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fix-real-picker-compose-rule-v1.ps1
3. Compile only:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly
4. If compile succeeds, run one isolated Android test:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1
5. Keep Android Emulator visible. When DocumentsUI opens, manually select the
   NEW musicflow_real_picker_*.wav document in Downloads.
6. Commit/test only after checking qa-logs/real-picker-saf-fast-v1.log.

This patch leaves the previously working Fast QA script unchanged. It updates
RealPickerSafTest.kt to use the AndroidComposeTestRule's own MainActivity instance
instead of manually starting another ActivityScenario. It validates all code
anchors first and creates a backup under the Windows TEMP directory.

No Android build/emulator verification was run in this environment.
