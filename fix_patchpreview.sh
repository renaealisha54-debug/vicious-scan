#!/bin/bash
set -e
cd ~/vicious-scan

python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/ui/screens/PatchPreviewScreen.kt').read()

# Replace the simple diff block with call to ContextualDiff
old_diff = '''            if (showDiff) {
                Spacer(Modifier.height(8.dp))
                // Show a simple added-lines diff
                val addedLines = preview.patchedContent.lines()
                    .filterNot { it in preview.originalContent.lines() }
                Surface(
                    color = Color(0xFF0A1A0A),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(8.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        addedLines.take(20).forEach { line ->
                            Text(
                                "+ $line",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ViciousGreen,
                                lineHeight = 15.sp
                            )
                        }
                        if (addedLines.size > 20) {
                            Text(
                                "... +${addedLines.size - 20} more lines",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ViciousMuted
                            )
                        }
                    }
                }
            }'''

new_diff = '''            if (showDiff) {
                Spacer(Modifier.height(8.dp))
                ContextualDiff(
                    originalContent = preview.originalContent,
                    patchedContent = preview.patchedContent
                )
            }'''

content = content.replace(old_diff, new_diff)

# Append ContextualDiff as a top-level composable at the end of the file
contextual_diff = """
@Composable
private fun ContextualDiff(originalContent: String, patchedContent: String) {
    val originalLines = originalContent.lines()
    val patchedLines = patchedContent.lines()
    val originalSet = originalLines.toSet()

    val insertedIndices = patchedLines.mapIndexedNotNull { i, line ->
        if (line !in originalSet) i else null
    }.toSet()

    val toShow = mutableSetOf<Int>()
    insertedIndices.forEach { idx ->
        for (i in maxOf(0, idx - 3)..minOf(patchedLines.size - 1, idx + 3)) {
            toShow.add(i)
        }
    }

    Surface(
        color = Color(0xFF0D1117),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            var lastShown = -1
            toShow.sorted().forEach { lineIdx ->
                if (lastShown >= 0 && lineIdx > lastShown + 1) {
                    Text(
                        "  \u00b7\u00b7\u00b7",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF484F58)
                    )
                }
                val line = patchedLines[lineIdx]
                val isInserted = lineIdx in insertedIndices
                val lineNum = (lineIdx + 1).toString().padStart(4)
                Row {
                    Text(
                        "$lineNum  ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color(0xFF484F58)
                    )
                    Text(
                        if (isInserted) "+ $line" else "  $line",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = if (isInserted) ViciousGreen else Color(0xFFE6EDF3),
                        lineHeight = 14.sp
                    )
                }
                lastShown = lineIdx
            }
        }
    }
}
"""

content = content.rstrip() + "\n" + contextual_diff

open('app/src/main/java/com/viciousscan/app/ui/screens/PatchPreviewScreen.kt', 'w').write(content)
print("PatchPreviewScreen.kt updated OK")
EOF

echo "DONE"
