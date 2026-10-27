p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()

old1 = """            if (clickNode(file)) {
                // Single-item ACTION_OPEN_DOCUMENT may return directly.
                // On multi-select layouts an Open/Select confirmation is shown.
                clickNamedAction(2_000L) { label ->
                    label.trim().equals("Open", true) ||
                        label.trim().equals("Select", true) ||
                        label.trim().equals("Mở", true) ||
                        label.trim().equals("Chọn", true)
                }
                return true
            }"""

new1 = """            if (file != null && clickNode(file)) {
                Log.i(
                    "MusicFlowRealPickerQA",
                    "DOCUMENT_ROW_CLICKED"
                )

                val confirmDeadline =
                    SystemClock.uptimeMillis() + 12_000L

                var confirmationClicked = false

                while (
                    SystemClock.uptimeMillis() < confirmDeadline
                ) {
                    val activeRoot =
                        instrumentation.uiAutomation
                            .rootInActiveWindow

                    val activePackage =
                        activeRoot?.packageName?.toString()

                    if (activePackage == context.packageName) {
                        Log.i(
                            "MusicFlowRealPickerQA",
                            "PICKER_RETURNED_TO_APP"
                        )
                        return true
                    }

                    if (!confirmationClicked) {
                        val confirmation = findNode(activeRoot) {
                            val label = it.trim().lowercase()

                            label == "open" ||
                                label == "select" ||
                                label == "mở" ||
                                label == "chọn" ||
                                label.startsWith("open (") ||
                                label.startsWith("select (") ||
                                label.startsWith("open ") ||
                                label.startsWith("select ")
                        }

                        if (confirmation != null) {
                            confirmationClicked =
                                clickNode(confirmation)

                            if (confirmationClicked) {
                                Log.i(
                                    "MusicFlowRealPickerQA",
                                    "PICKER_CONFIRM_CLICKED"
                                )
                            }
                        }
                    }

                    SystemClock.sleep(200)
                }

                Log.w(
                    "MusicFlowRealPickerQA",
                    "PICKER_NOT_RETURNED_TO_APP"
                )

                return false
            }"""

assert old1 in s, 'old1 not found'
s = s.replace(old1, new1, 1)

old2 = """            val root = instrumentation.uiAutomation.rootInActiveWindow
            val file = findNode(root) {
                it.contains(title, ignoreCase = true)
            }"""
new2 = """            val root =
                instrumentation.uiAutomation.rootInActiveWindow

            val file = findNode(root) {
                it.contains(title, ignoreCase = true)
            }"""
assert old2 in s, 'old2 not found'
s = s.replace(old2, new2, 1)

old3 = """            if (!visitedRecent &&
                SystemClock.uptimeMillis() + 22_000L < deadline
            ) {"""
new3 = """            if (!visitedRecent) {"""
assert old3 in s, 'old3 not found'
s = s.replace(old3, new3, 1)

old4 = """            SystemClock.sleep(200)
        }
        return false
    }

    @Test"""
new4 = """            SystemClock.sleep(200)
        }

        Log.w(
            "MusicFlowRealPickerQA",
            "DOCUMENT_NOT_FOUND_IN_PICKER"
        )

        return false
    }

    @Test"""
assert old4 in s, 'old4 not found'
s = s.replace(old4, new4, 1)

open(p,'w',encoding='utf-8').write(s)
print('select part done')
