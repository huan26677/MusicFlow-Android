p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()

# A. imports
s = s.replace(
    'import android.view.accessibility.AccessibilityNodeInfo\n',
    'import android.view.accessibility.AccessibilityNodeInfo\nimport android.view.InputDevice\nimport android.view.MotionEvent\n',
    1
)

# B. replace longPressDocumentTitle entirely
old_lp = '''    private fun longPressDocumentTitle(
        node: AccessibilityNodeInfo
    ): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (bounds.width() <= 0 || bounds.height() <= 0) {
            Log.w(
                "MusicFlowRealPickerQA",
                "INVALID_DOCUMENT_TITLE_BOUNDS"
            )
            return false
        }

        val x = bounds.centerX()
        val y = bounds.centerY()

        Log.i(
            "MusicFlowRealPickerQA",
            "LONG_PRESS_DOCUMENT_TITLE x=$x y=$y"
        )

        // NhÃ¡ÂºÂ¥n giÃ¡Â»Â¯ bÃ¡ÂºÂ±ng sÃ¡Â»Â± kiÃ¡Â»â€¡n chÃ¡ÂºÂ¡m Android thÃ¡Â»Â±c tÃ¡ÂºÂ¿.
        // KhÃƒÂ´ng bÃ¡ÂºÂ¥m vÃƒÂ o nÃƒÂºt Preview.
        val command =
            "input touchscreen swipe $x $y $x $y 900"

        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(
                command
            )
        ).use { input ->
            while (input.read() != -1) {
                // Ã„ÂÃ¡Â»Âc hÃ¡ÂºÂ¿t output.
            }
        }

        return true
    }
'''
# Since actual file may have different (mojibake) comments, match by function signature + bounds check loosely.
import re
# Use regex to replace from '    private fun longPressDocumentTitle(' up to the next '    private fun selectRealDocument('
pattern = re.compile(r'    private fun longPressDocumentTitle\(\n        node: AccessibilityNodeInfo\n    \): Boolean \{.*?\n    \}\n\n    private fun selectRealDocument', re.DOTALL)
new_lp_and_click = '''    private fun longPressDocumentTitle(
        node: AccessibilityNodeInfo
    ): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        if (
            bounds.isEmpty ||
            !node.isVisibleToUser
        ) {
            Log.e(
                "MusicFlowRealPickerQA",
                "DOCUMENT_TITLE_NOT_VISIBLE"
            )
            return false
        }

        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()

        val automation = instrumentation.uiAutomation
        val downTime = SystemClock.uptimeMillis()

        fun inject(action: Int): Boolean {
            val event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                action,
                x,
                y,
                0
            )

            event.source = InputDevice.SOURCE_TOUCHSCREEN

            return try {
                automation.injectInputEvent(event, true)
            } finally {
                event.recycle()
            }
        }

        val downOk = inject(MotionEvent.ACTION_DOWN)

        // Äáº£m báº£o vÆ°á»£t ngÆ°á»¡ng long press.
        SystemClock.sleep(1200)

        val upOk = inject(MotionEvent.ACTION_UP)

        val success = downOk && upOk

        Log.i(
            "MusicFlowRealPickerQA",
            "TOUCH_LONG_PRESS " +
                "x=$x y=$y " +
                "down=$downOk up=$upOk " +
                "success=$success"
        )

        return success
    }

    private fun clickDocumentRow(
        titleNode: AccessibilityNodeInfo
    ): Boolean {
        var current = titleNode.parent

        while (current != null) {
            val id = current.viewIdResourceName.orEmpty()
            val description =
                current.contentDescription?.toString().orEmpty()

            if (
                current.isClickable &&
                !id.contains("preview", ignoreCase = true) &&
                !description.startsWith(
                    "Preview the file",
                    ignoreCase = true
                )
            ) {
                Log.i(
                    "MusicFlowRealPickerQA",
                    "DOCUMENT_ROW_ACTION id=$id"
                )

                return current.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            current = current.parent
        }

        Log.w(
            "MusicFlowRealPickerQA",
            "DOCUMENT_ROW_ACTION_NOT_FOUND"
        )

        return false
    }

    private fun selectRealDocument'''
assert pattern.search(s), 'longPress block not found'
s = pattern.sub(new_lp_and_click, s, count=1)

# D. fallback block after AFTER_SELECTION
old_after = '''                SystemClock.sleep(400)
                logPickerUi("AFTER_SELECTION")

                val confirmDeadline ='''
new_after = '''                SystemClock.sleep(400)
                logPickerUi("AFTER_SELECTION")

                // Náº¿u long press khÃ´ng táº¡o nÃºt Open/Select,
                // thá»­ click pháº§n tá»­ cha cá»§a chÃ­nh tÃªn file.
                val selectionRoot =
                    instrumentation.uiAutomation.rootInActiveWindow

                val hasOpenAction = findNode(selectionRoot) {
                    val label = it.trim().lowercase()

                    label == "open" ||
                        label == "select" ||
                        label == "má»Ÿ" ||
                        label == "chá»n"
                } != null

                if (
                    selectionRoot?.packageName?.toString() ==
                        "com.google.android.documentsui" &&
                    !hasOpenAction
                ) {
                    val currentTitle = findDocumentTitleNode(
                        selectionRoot,
                        title
                    )

                    if (currentTitle != null) {
                        val rowClicked = clickDocumentRow(currentTitle)

                        Log.i(
                            "MusicFlowRealPickerQA",
                            "ROW_CLICK_FALLBACK=$rowClicked"
                        )
                    }
                }

                val confirmDeadline ='''
assert old_after in s, 'after block not found'
s = s.replace(old_after, new_after, 1)

# E. isImportantView
import re
old_imp = re.compile(r'            val isImportantView =\n.*?\n', re.DOTALL)
# find current isImportantView block
m = old_imp.search(s)
assert m, 'isImportantView not found'
new_imp = '''            val isImportantView =
                (node.viewIdResourceName ?: "").let { id ->
                    id.contains("option_menu", ignoreCase = true) ||
                    id.contains("button", ignoreCase = true)
                }

'''
s = old_imp.sub(new_imp, s, count=1)

open(p,'w',encoding='utf-8').write(s)
print('v5 done')
