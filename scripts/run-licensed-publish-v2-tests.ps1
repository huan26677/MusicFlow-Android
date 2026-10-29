param()
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root "qa-logs\licensed-publish-v2.log"

function Log([string]$line) {
    Write-Host $line
    Add-Content -LiteralPath $log -Value $line -Encoding UTF8
}

function Run-Native([string]$exe, [string[]]$arguments) {
    $before = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = @(& $exe @arguments 2>&1)
        $exit = [int]$LASTEXITCODE
    } finally { $ErrorActionPreference = $before }
    $result | ForEach-Object { Log ([string]$_) }
    return [pscustomobject]@{ Exit = $exit; Output = ($result -join [char]10) }
}

Push-Location $root
try {
    Set-Content -LiteralPath $log -Value "MusicFlow Licensed Content Review & Publish v2" -Encoding UTF8
    Log "DATE=$(Get-Date -Format o)"
    Log "HEAD_BASE=$(git rev-parse --short HEAD)"
    Log "NO_REAL_AUDIO_OR_SECRET_REQUIRED=TRUE"

    $python = Run-Native "python" @(
        "-m", "unittest", "discover", "-s", "scripts",
        "-p", "test_musicflow_licensed_upload.py", "-v"
    )
    Log "PYTHON_TEST_EXIT=$($python.Exit)"
    if ($python.Exit -ne 0 -or $python.Output -notmatch "Ran 17 tests" -or
        $python.Output -notmatch "(?m)^OK$") {
        throw "LICENSED_PUBLISH_PYTHON_FAILED"
    }
    Log "LICENSED_UPLOAD_PYTHON_TESTS=17"

    $compile = Run-Native "python" @(
        "-m", "py_compile", "scripts/musicflow_licensed_upload.py"
    )
    Log "PYTHON_SYNTAX_EXIT=$($compile.Exit)"
    if ($compile.Exit -ne 0) { throw "PYTHON_SYNTAX_FAILED" }

    $build = Run-Native ".\gradlew.bat" @(
        ":app:assembleDebug", ":app:assembleDebugAndroidTest",
        ":app:testDebugUnitTest", "--console=plain"
    )
    Log "ANDROID_BUILD_JVM_EXIT=$($build.Exit)"
    if ($build.Exit -ne 0 -or $build.Output -notmatch "BUILD SUCCESSFUL") {
        throw "ANDROID_BUILD_OR_REGRESSION_FAILED"
    }

    $check = Run-Native "git" @("diff", "--check")
    Log "GIT_DIFF_CHECK_EXIT=$($check.Exit)"
    if ($check.Exit -ne 0) { throw "GIT_DIFF_CHECK_FAILED" }

    # Remote Supabase RPC security and transactional tests are performed
    # separately via the connected Supabase admin tool, never with a secret
    # embedded in the repository or this QA script.
    Log "REMOTE_SQL_REVIEW=SEE_DOCS_Licensed_Content_Review_Publish_v2"
    Log "FINAL_RESULT=PASS"
} catch {
    Log "FAIL_REASON=$($_.Exception.Message)"
    Log "FINAL_RESULT=FAIL"
    exit 1
} finally { Pop-Location }
