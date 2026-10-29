# MusicFlow - Full Product Regression Gate v1 - Part 1

**Date:** 2026-10-29. **Base HEAD:** `23c4835`. **Status:** PARTIAL / GATE FAIL. This is an honest baseline for further fixes, NOT a Full Product acceptance.

## Scope

Part 1 deliberately covers only **build + all JVM tests + unfiltered Android instrumentation baseline** on a single Android 11 / API 30 emulator (`emulator-5554`). Future parts will repair regressions, rerun the gate, and separately address gaps that cannot be tested without real credentials, licensed streaming content, or physical devices.

The user confirmed: offline music already on the phone or downloaded by the user does **not** require MusicFlow's online streaming rights verification. Online content supplied by MusicFlow still requires verified streaming rights. Nothing in this milestone changes that separation.

## Evidence from this session

| Verification | Result |
|---|---|
| `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` | **BUILD SUCCESSFUL** |
| Gradle JVM JUnit XML | **99 total / 99 PASS / 0 FAIL / 0 SKIP**, 15 suites |
| `:app:connectedDebugAndroidTest` (API 30) | **BUILD FAILED (instrumentation assertion failures)** |
| Gradle Android XML `testsuites` | **89 total / 83 PASS / 3 FAIL / 3 SKIP** |
| Direct isolated rerun of MVP player test | **FAIL** again at `ExampleInstrumentedTest.kt:227`, after click **Trước** (Previous); not just a one-time full-suite flake |

The XML `testsuites` root is the authoritative per-case count (`tests=89`); do not rely on Gradle's transient console progress counts. File: `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel_5_API_30(AVD) - 11-_app-.xml`. The XML/report is a build output, not committed to Git. A reproducible script creates the plain-text QA log in `qa-logs/full-product-regression-v1-part1.log`.

### 3 failed Android cases

1. `ExampleInstrumentedTest.mvpLibrarySearchToRealPlayer` - timeout after pressing **Trước**; Player state did not reach expected `MVP Alpha`, index 1, isPlaying=true within 20 seconds. Reproduced in a single-case instrumentation session. **Exact root cause not yet confirmed**, so do not change playback semantics without further diagnosis.
2. `RealPickerSafTest.realOpenMultipleDocumentsGrantsPersistedReadAndPlays` - the expected SAF file did not play; state showed no media item. The test's `selectRealDocument` currently waits for **manual document selection**, not an automated tap.
3. `RealPickerSafTest.realOpenMultipleDocumentsSelectsTwoFilesAndNavigates` - expected 2 MediaItems, got 0. Same manual-selection dependency; the automation must actually select the two documents before asserting playback.

### 3 skipped Android cases

- `SafPermissionRestartTest.cleanupSafDocument`
- `SafPermissionRestartTest.prepareSafDocument`
- `SafPermissionRestartTest.verifySafAfterForceStop`

These skips are **expected** when launching the generic runner without their `musicflow_saf_phase` arguments. The separate **Android Compatibility Matrix v2** ran the SAF phases plus true process restart with PID checks on API 29/30/33/36 and passed. Do not count these three generic-suite skips as completed SAF verification. Similarly, process-restart phase tests that return early without their flags are **not evidence** of actual restart in the generic run.

## Tooling

Added `scripts/run-full-product-regression-v1-part1.ps1`. Default mode checks exactly one matching online emulator, builds app and AndroidTest APK, runs JVM + unfiltered Android instrumentation, reads JUnit XML instead of hardcoded counts, records failed and skipped case names, and exits nonzero unless both groups pass. It does **not** silently turn failing tests into SKIPs.

The `-AnalyzeOnly` option parses existing Gradle XML without rerunning device tests. **Only use with known fresh results**; it does not attest the timestamp or run provenance. In this milestone `-AnalyzeOnly` was used after the direct Gradle commands; its final marker is **`FINAL_RESULT=FAIL`**, as required by the 3 failing cases.

## Next incremental work

- **Part 2A - MVP Previous action:** reproduce with extra Player/MediaSession state logging, distinguish user-interface dispatch from skip engine / media state, fix cause and verify isolated test.
- **Part 2B - Real SAF Picker:** change/manual-test harness so it actually selects the file(s) through DocumentsUI; verify persisted URI grants and playback; run both isolated before rerunning the entire unfiltered Android suite.
- **Part 3:** rerun Build/JVM + full Android API 30 suite; then broader feature QA (Lyrics, Sleep Timer, EQ, large library) and Release Candidate packaging. Keep Google/email real Auth and two-device Cloud E2E near the end, streaming E2E blocked until licensed audio is provided.

**Not a release candidate. Do not mark Full Product Regression Gate PASS until all blocking failures are resolved and the remaining test gaps are explicitly handled.**
