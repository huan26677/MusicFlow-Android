p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()
import re

# 1. Remove longClickNode function
pat_longclick = re.compile(r'    private fun longClickNode\(\n        node: AccessibilityNodeInfo\?\n    \): Boolean \{.*?\n    \}\n\n', re.DOTALL)
assert pat_longclick.search(s), 'longClickNode not found'
s = pat_longclick.sub('', s, count=1)

# 2. Remove longPressDocumentTitle + clickDocumentRow
pat_oldhelpers = re.compile(r'    private fun longPressDocumentTitle\(\n        node: AccessibilityNodeInfo\n    \): Boolean \{.*?\n    \}\n\n    private fun clickDocumentRow\(\n        titleNode: AccessibilityNodeInfo\n    \): Boolean \{.*?\n    \}\n\n', re.DOTALL)
assert pat_oldhelpers.search(s), 'old helpers not found'
s = pat_oldhelpers.sub('', s, count=1)

# 3. Replace selectRealDocument entirely with tapDocumentTitle + new selectRealDocument
tap_func = '''    private fun tapDocumentTitle(
        node: AccessibilityNodeInfo
    ): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.isEmpty || !node.isVisibleToUser) {
            Log.e(
                "MusicFlowRealPickerQA",
                "DOCUMENT_TITLE_NOT_VISIBLE"
            )
            return false
        }

        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val downTime = SystemClock.uptimeMillis()

        val down = MotionEvent.obtain(
            downTime,
            downTime,
            MotionEvent.ACTION_DOWN,
            x, y, 0
        )

        val up = MotionEvent.obtain(
            downTime,
            downTime + 100L,
            MotionEvent.ACTION_UP,
            x, y, 0
        )

        down.source = InputDevice.SOURCE_TOUCHSCREEN
        up.source = InputDevice.SOURCE_TOUCHSCREEN

        return try {
            val downOk =
                instrumentation.uiAutomation
                    .injectInputEvent(down, true)

            SystemClock.sleep(100)

            val upOk =
                instrumentation.uiAutomation
                    .injectInputEvent(up, true)

            Log.i(
                "MusicFlowRealPickerQA",
                "TITLE_TAP down=$downOk up=$upOk"
            )

            downOk && upOk
        } finally {
            down.recycle()
            up.recycle()
        }
    }

'''

new_select = '''    private fun selectRealDocument(title: String): Boolean {
        val deadline =
            SystemClock.uptimeMillis() + 25_000L

        var visitedRecent = false

        while (SystemClock.uptimeMillis() < deadline) {
            val root =
                instrumentation.uiAutomation.rootInActiveWindow

            val file = findDocumentTitleNode(root, title)

            if (file != null) {
                logPickerUi("BEFORE_TAP")

                assertTrue(
                    "KhÃ´ng gá»­i Ä‘Æ°á»£c thao tÃ¡c cháº¡m tÃªn file",
                    tapDocumentTitle(file)
                )

                SystemClock.sleep(500)
                logPickerUi("AFTER_TAP")

                var confirmationClicked = false
                var lastPackage: String? = null

                val returnDeadline =
                    SystemClock.uptimeMillis() + 15_000L

                while (
                    SystemClock.uptimeMillis() < returnDeadline
                ) {
                    val activeRoot =
                        instrumentation.uiAutomation
                            .rootInActiveWindow

                    val currentPackage =
                        activeRoot?.packageName?.toString()

                    if (currentPackage != lastPackage) {
                        Log.i(
                            "MusicFlowRealPickerQA",
                            "ACTIVE_WINDOW_PACKAGE=$currentPackage"
                        )
                        lastPackage = currentPackage
                    }

                    if (currentPackage == context.packageName) {
                        Log.i(
                            "MusicFlowRealPickerQA",
                            "PICKER_RETURNED_TO_APP"
                        )
                        return true
                    }

                    if (
                        currentPackage != null &&
                        currentPackage !=
                            "com.google.android.documentsui"
                    ) {
                        Log.e(
                            "MusicFlowRealPickerQA",
                            "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
                        )
                        return false
                    }

                    if (!confirmationClicked) {
                        val confirmation = findNode(activeRoot) {
                            val label = it.trim().lowercase()

                            label == "open" ||
                                label.startsWith("open (") ||
                                label == "select" ||
                                label.startsWith("select (") ||
                                label == "má»Ÿ" ||
                                label == "chá»n"
                        }

                        if (confirmation != null) {
                            confirmationClicked =
                                clickNode(confirmation)

                            Log.i(
                                "MusicFlowRealPickerQA",
                                "PICKER_CONFIRM_CLICKED=" +
                                    confirmationClicked
                            )
                        }
                    }

                    SystemClock.sleep(200)
                }

                logPickerUi("PICKER_TIMEOUT")

                Log.e(
                    "MusicFlowRealPickerQA",
                    "PICKER_NOT_RETURNED_TO_APP"
                )
                return false
            }

            if (!visitedRecent) {
                visitedRecent = true

                clickNode(findNode(root) {
                    it.trim().equals("Recent", true) ||
                        it.trim().equals("Gáº§n Ä‘Ã¢y", true)
                })
            }

            SystemClock.sleep(200)
        }

        Log.e(
            "MusicFlowRealPickerQA",
            "DOCUMENT_TITLE_NOT_FOUND"
        )
        return false
    }
'''

pat_select = re.compile(r'    private fun selectRealDocument\(title: String\): Boolean \{.*?\n    \}\n', re.DOTALL)
assert pat_select.search(s), 'selectRealDocument not found'
s = pat_select.sub(tap_func + new_select, s, count=1)

open(p,'w',encoding='utf-8').write(s)
print('v6 done')
