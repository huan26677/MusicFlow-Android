p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()

# A. imports
s = s.replace(
    'import android.content.Intent\n',
    'import android.content.Intent\nimport android.graphics.Rect\n',
    1
)
s = s.replace(
    'import android.net.Uri\n',
    'import android.net.Uri\nimport android.os.ParcelFileDescriptor\n',
    1
)

# B. add helpers before selectRealDocument
anchor = '    private fun selectRealDocument(title: String): Boolean {'
helpers = '''    private fun findDocumentTitleNode(
        node: AccessibilityNodeInfo?,
        title: String
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        if (
            node.viewIdResourceName == "android:id/title" &&
            node.text?.toString() == title
        ) {
            return node
        }

        for (i in 0 until node.childCount) {
            findDocumentTitleNode(
                node.getChild(i),
                title
            )?.let { return it }
        }

        return null
    }

    private fun longPressDocumentTitle(
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

        // Nháº¥n giá»¯ báº±ng sá»± kiá»‡n cháº¡m Android thá»±c táº¿.
        // KhÃ´ng báº¥m vÃ o nÃºt Preview.
        val command =
            "input touchscreen swipe $x $y $x $y 900"

        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(
                command
            )
        ).use { input ->
            while (input.read() != -1) {
                // Äá»c háº¿t output.
            }
        }

        return true
    }

'''
assert anchor in s
s = s.replace(anchor, helpers + anchor, 1)

# C. replace file find
old_find = '''            val file = findNode(root) {
                it.contains(title, ignoreCase = true)
            }'''
new_find = '''            val file = findDocumentTitleNode(root, title)'''
assert old_find in s
s = s.replace(old_find, new_find, 1)

# D. replace selection action
old_sel = '''                val longPressed = longClickNode(file)

                val selected = longPressed || clickNode(file)'''
new_sel = '''                val longPressed = longClickNode(file)

                val selected = longPressed ||
                    longPressDocumentTitle(file)'''
assert old_sel in s
s = s.replace(old_sel, new_sel, 1)

# E. unexpected package check
old_pkg = '''                    val currentPackage =
                        activeRoot?.packageName?.toString()

                    if (currentPackage != lastPackage) {'''
new_pkg = '''                    val currentPackage =
                        activeRoot?.packageName?.toString()

                    if (
                        currentPackage != null &&
                        currentPackage != context.packageName &&
                        currentPackage != "com.google.android.documentsui"
                    ) {
                        Log.e(
                            "MusicFlowRealPickerQA",
                            "UNEXPECTED_PICKER_PACKAGE=$currentPackage"
                        )

                        return false
                    }

                    if (currentPackage != lastPackage) {'''
assert old_pkg in s
s = s.replace(old_pkg, new_pkg, 1)

# Limit logPickerUi to QA file and action buttons
old_log_cond = '''            if (text.isNotBlank() || description.isNotBlank()) {'''
new_log_cond = '''            val actionLabels = setOf(
                "open", "select", "má»Ÿ", "chá»n",
                "recent", "gáº§n Ä‘Ã¢y"
            )

            val isQaFile =
                text.startsWith("musicflow_real_picker_")
            val isActionLabel =
                text.lowercase() in actionLabels
            val isImportantView =
                (node.viewIdResourceName ?: "")
                    .contains("title", ignoreCase = true) ||
                    (node.viewIdResourceName ?: "")
                        .contains("button", ignoreCase = true)

            if (
                (text.isNotBlank() || description.isNotBlank()) &&
                (isQaFile || isActionLabel || isImportantView)
            ) {
'''
assert old_log_cond in s
s = s.replace(old_log_cond, new_log_cond, 1)

open(p,'w',encoding='utf-8').write(s)
print('test edits done')
