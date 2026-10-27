p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()

old = """            val deadline = SystemClock.uptimeMillis() + 25_000L
            var playsExpected = false
            while (!playsExpected && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync {
                    playsExpected = media.isPlaying &&
                        media.currentMediaItem?.mediaId == uri.toString()
                }
                if (!playsExpected) SystemClock.sleep(150)
            }
            assertTrue(
                "Production picker did not play selected SAF document",
                playsExpected
            )
            Log.i("MusicFlowRealPickerQA", "PASS realPickerPlayback")

            assertTrue(
                "Production callback did not persist READ grant",
                persistedRead(uri)
            )
            Log.i("MusicFlowRealPickerQA", "PASS realPickerPersistedRead")"""

new = """            val deadline =
                SystemClock.uptimeMillis() + 25_000L

            var playsExpected = false
            var actualUri: Uri? = null
            var actualMediaId: String? = null
            var actualTitle: String? = null
            var actualState = -1
            var actualError: String? = null

            while (
                !playsExpected &&
                SystemClock.uptimeMillis() < deadline
            ) {
                instrumentation.runOnMainSync {
                    val item = media.currentMediaItem

                    actualUri =
                        item?.localConfiguration?.uri
                    actualMediaId = item?.mediaId
                    actualTitle =
                        item?.mediaMetadata?.title?.toString()
                    actualState = media.playbackState
                    actualError =
                        media.playerError?.errorCodeName

                    playsExpected =
                        media.isPlaying &&
                        actualTitle == title &&
                        actualUri?.scheme == "content"
                }

                if (!playsExpected) {
                    SystemClock.sleep(150)
                }
            }

            Log.i(
                "MusicFlowRealPickerQA",
                "PICKER_RESULT " +
                    "playing=$playsExpected " +
                    "mediaId=$actualMediaId " +
                    "title=$actualTitle " +
                    "state=$actualState " +
                    "error=$actualError"
            )

            assertTrue(
                "File SAF chưa phát đúng. " +
                    "mediaId=$actualMediaId " +
                    "title=$actualTitle " +
                    "state=$actualState " +
                    "error=$actualError",
                playsExpected
            )

            val selectedUri = checkNotNull(actualUri)

            val header = ByteArray(4)
            val bytesRead =
                context.contentResolver
                    .openInputStream(selectedUri)
                    .use { input ->
                        checkNotNull(input).read(header)
                    }

            assertEquals(
                "Không đọc đủ WAV header",
                4,
                bytesRead
            )

            assertEquals(
                "File đã chọn không phải WAV mong đợi",
                "RIFF",
                String(header, Charsets.US_ASCII)
            )

            Log.i(
                "MusicFlowRealPickerQA",
                "PASS realPickerPlayback"
            )

            assertTrue(
                "Production picker chưa lưu READ permission",
                persistedRead(selectedUri)
            )

            Log.i(
                "MusicFlowRealPickerQA",
                "PASS realPickerPersistedRead"
            )"""

assert old in s, 'old B not found'
s = s.replace(old, new, 1)
open(p,'w',encoding='utf-8').write(s)
print('B part done')
