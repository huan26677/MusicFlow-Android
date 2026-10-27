MusicFlow - Fast / Full QA, PowerShell 5.1 compatible (2026-10-28)

WHY THIS UPDATE:
The previous fix-real-picker-and-split-qa-v1.ps1 was saved as UTF-8 without a BOM.
Windows PowerShell 5.1 misread the Vietnamese characters in the script, leading
to a parser error at line 73 before it could change any project file.
The updated .ps1 files contain only ASCII characters and are safe with Windows
PowerShell 5.1 regardless of the ANSI/UTF-8 default encoding.

INSTALL:
1. Extract this archive into E:\Du_An\MusicFlow and overwrite both files in scripts\.
2. Run the repair command from E:\Du_An\MusicFlow:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\fix-real-picker-and-split-qa-v1.ps1

3. Compile only, no Emulator needed:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1 -CompileOnly

4. Real Picker only, Emulator Android 11 API 30 (manual-assisted):
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-fast-v1-tests.ps1
   Select the freshly created musicflow_real_picker_*.wav file in DocumentsUI.
   Fast log: qa-logs\real-picker-saf-fast-v1.log

5. Only after isolated Fast QA PASS, run full regression:
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-real-picker-saf-v1-tests.ps1

NOTES:
- The repair script replaces only the selectRealDocument() region through the
  next @Test marker, preserving the subsequent playback and SAF permission tests.
- A timestamped backup of the original Kotlin file is saved in the Windows TEMP
  directory, outside Git's staging area.
- The repair script appends an English, ASCII-safe entry to the project handoff
  document. Other Vietnamese text is preserved.
- COMPILE_PASS is not functional QA PASS. PASS from manual-assisted QA is not
  equivalent to fully automated DocsUI QA. Run full regression before release.
