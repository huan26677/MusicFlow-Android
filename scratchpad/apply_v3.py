p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()

start = s.index('    private fun selectRealDocument(title: String): Boolean {')
end_marker = '    @Test\n    fun realOpenMultipleDocumentsGrantsPersistedReadAndPlays()'
end = s.index(end_marker)
old_func = s[start:end]

new_block = '''    private fun longClickNode(
        node: AccessibilityNodeInfo?
    ): Boolean {
        var current = node

        while (current != null) {
            if (
                current.isLongClickable &&
                current.performAction(
                    AccessibilityNodeInfo.ACTION_LONG_CLICK
                )
            ) {
                return true
            }

            current = current.parent
        }

        return false
    }

    private fun logPickerUi(stage: String) {
        val root =
            instrumentation.uiAutomation.rootInActiveWindow

        Log.i(
            "MusicFlowRealPickerQA",
            "$stage ROOT_PACKAGE=${root?.packageName}"
        )

        var recorded = 0

        fun visit(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || recorded >= 70) return

            val text = node.text?.toString().orEmpty()
                .take(100)
            val description =
                node.contentDescription?.toString().orEmpty()
                    .take(100)

            if (text.isNotBlank() || description.isNotBlank()) {
                Log.i(
                    "MusicFlowRealPickerQA",
                    "$stage UI depth=$depth " +
                        "text=$text desc=$description " +
                        "id=${node.viewIdResourceName} " +
                        "class=${node.className} " +
                        "clickable=${node.isClickable} " +
                        "selected=${node.isSelected}"
                )
                recorded++
            }

            for (i in 0 until node.childCount) {
                visit(node.getChild(i), depth + 1)
            }
        }

        visit(root, 0)
    }

    private fun selectRealDocument(title: String): Boolean {
        val deadline =
            SystemClock.uptimeMillis() + 30_000L

        var visitedRecent = false

        while (SystemClock.uptimeMillis() < deadline) {
            val root =
                instrumentation.uiAutomation.rootInActiveWindow

            val file = findNode(root) {
                it.contains(title, ignoreCase = true)
            }

            if (file != null) {
                logPickerUi("BEFORE_SELECTION")

                // OpenMultipleDocuments há»— trá»£ chá»n nhiá»u file.
                // Æ¯u tiÃªn nháº¥n giá»¯ Ä‘á»ƒ vÃ o cháº¿ Ä‘á»™ chá»n.
                val longPressed = longClickNode(file)

                val selected = longPressed || clickNode(file)

                Log.i(
                    "MusicFlowRealPickerQA",
                    "FILE_SELECTION_ACTION " +
                        "longPress=$longPressed success=$selected"
                )

                if (!selected) return false

                SystemClock.sleep(400)
                logPickerUi("AFTER_SELECTION")

                val confirmDeadline =
                    SystemClock.uptimeMillis() + 15_000L

                var confirmClicked = false
                var lastPackage: String? = null

                while (
                    SystemClock.uptimeMillis() < confirmDeadline
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

                    if (!confirmClicked) {
                        val confirmation = findNode(activeRoot) {
                            val label = it.trim().lowercase()

                            label == "open" ||
                                label == "select" ||
                                label == "má»Ÿ" ||
                                label == "chá»n"
                        }

                        if (confirmation != null) {
                            Log.i(
                                "MusicFlowRealPickerQA",
                                "CONFIRM_TARGET " +
                                    "text=${confirmation.text} " +
                                    "id=${confirmation.viewIdResourceName} " +
                                    "class=${confirmation.className}"
                            )

                            confirmClicked = clickNode(confirmation)

                            if (confirmClicked) {
                                Log.i(
                                    "MusicFlowRealPickerQA",
                                    "PICKER_CONFIRM_CLICKED"
                                )
                            }
                        }
                    }

                    SystemClock.sleep(200)
                }

                logPickerUi("PICKER_TIMEOUT")

                Log.w(
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

        logPickerUi("DOCUMENT_NOT_FOUND")
        return false
    }

'''

s = s[:start] + new_block + s[end:]
open(p,'w',encoding='utf-8').write(s)
print('done')
