# MusicFlow — Supabase signup/login without email confirmation: end-to-end QA

**Date:** 2026-11-05 (Asia/Ho_Chi_Minh).
**Project:** `vzvxpbwrftlnyvuhfoqn`.
**Git base:** `3854034`, original worktree contained uncommitted local-demo-account work; those edits were preserved, not reset.

## Decision

User requested: **real Supabase Auth user and profile storage, but NO confirmation email**. Offline/local-only demo accounts are NOT the default. The hosted Supabase email-confirmation setting is controlled server-side: Android cannot disable it.

Current observed server settings, via the existing publishable key and read-only `GET /auth/v1/settings`:

- `external.email=true`
- `mailer_autoconfirm=true` — **Confirm email is OFF**.
- `disable_signup=false` — new signups enabled.
- Google OAuth is unrelated and currently disabled.

This status was verified live; **the assistant did not change the server setting**. When running a public-facing service in the future, reevaluate email verification: with auto-confirm enabled, an arbitrary email address can be claimed without proving mailbox ownership.

## APK changes

1. `AccountViewModel.kt`: **Supabase online is selected by default**, not the local-only demo mode. Registering with a server-issued session displays a clear Supabase success message. Missing password and too-short password get focused errors instead of a generic combined form error.
2. `AccountApi.kt`: adds read-only `emailConfirmationRequired()` using Supabase `mailer_autoconfirm`, leaving the remote session and JWT handling unchanged.
3. `AccountScreen.kt`: shows the actual confirmation setting from Supabase on the account screen and a clearly labeled required password hint. Optional local-demo mode, which was already uncommitted in the worktree, remains explicitly separate from Supabase, not the default.
4. `scripts/build-musicflow-demo-apk-v1.ps1`: adds a unique demo APK filename, preserving older binaries and checksum files.

## End-to-end proof on Android emulator API 30

Used a fresh QA AVD `MusicFlow_SupabaseSignup_QA_API30` on port `5558`. Did not modify the preexisting user emulators on ports `5554` and `5556`.

- `:app:testDebugUnitTest :app:assembleRelease`: **BUILD SUCCESSFUL**; JUnit **116 PASS / 0 FAIL / 0 SKIP**.
- A subsequent validation run on the then-current worktree, which also contained additional uncommitted account-form changes, completed **121 JVM PASS / 0 FAIL / 0 SKIP** and release APK build PASS. This later build was **not** used for the above signup E2E test; the tested `dist` APK is uniquely identified by the SHA-256 below.
- QA APK build, release-mode, zipalign, demo debug-key signing, `apksigner verify` and `aapt badging`: **PASS**.
- APK fresh install and MainActivity cold launch: **PASS**.
- Open Account: selected online mode by default. After loading Auth settings, screen states that signup with Supabase needs no confirmation email: **PASS**.
- Fill a random QA email at the reserved `example.com` domain (NOT the user's email) and a random strong password, tap **Đăng ký Free**: **PASS**. UI shows the authenticated **Free** account screen and the server-backed user email.
- Supabase database read-only verification after signup: **1 matching `auth.users` row**, **`email_confirmed_at IS NOT NULL`**, **1 matching `public.account_profiles` row**, no entitlement row (normal Free fallback). These records are real Supabase data, not local-only mock data.
- Tap **Đăng xuất**, enter same QA email/password and **Đăng nhập**: **PASS**.
- Force-stop app and reopen: Supabase token refresh restored the account automatically: **PASS**.

Test secrets were held temporarily only in Git-ignored `app/build/rc-qa`, never in source, logs, or documents. This test creates one **QA-only account on the connected Supabase project**; it is not the user's own account. It can be deleted later in Supabase Dashboard → Authentication → Users. No production email was sent during the test because email confirmation is disabled.

## APK handed off for demo/report

```text
E:\Du_An\MusicFlow\dist\MusicFlow-1.0-Demo-SupabaseSignup-Android.apk
```

- Bytes: **30,262,865**
- SHA-256: `74D5D93294B8B6AAB7283B12835FD53AD0E1AC6BCA179A13F7F17D2ACD795026`
- Version: `1.0 (versionCode=1)`
- Android: minSdk 29, targetSdk 36
- QA/demo signing, **not** production signing/Google Play
- Original APKs in `dist` were not overwritten. `dist/*.apk` are Git ignored.

## If Confirm email ever gets enabled again

Open:

`https://supabase.com/dashboard/project/vzvxpbwrftlnyvuhfoqn/auth/providers`

Authentication → Providers → Email → leave Email provider enabled → **turn OFF Confirm email** → Save. Check again with the Auth settings endpoint or run the newest APK: it displays the current status. The hosted Auth settings change must be made by someone with Supabase project access. There is no need for custom SMTP **while confirmation remains OFF**.

## Manual instructions for the user

1. Install/update `MusicFlow-1.0-Demo-SupabaseSignup-Android.apk` from `dist`, preserving the application data where signing keys match.
2. Account tab → select **Supabase online** (selected by default on a fresh install; don't select Tài khoản Demo).
3. Enter a valid email address and **a password of at least six characters**. Both are mandatory; in the user's screenshot the password field was empty.
4. Tap **Đăng ký Free**; the screen should switch to the server-backed Free account directly. After sign-out, use **Đăng nhập** with the same credentials.
5. Verify account at Supabase Dashboard → Authentication → Users, and profile in Table Editor → `account_profiles`.

**Status: PASS — hosted Supabase registration, login and session restore without confirmation email, on Android API 30 emulator.** Device testing and public release remain outside this school/demo project's agreed scope.
