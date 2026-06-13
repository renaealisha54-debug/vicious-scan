package com.viciousscan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.scanner.AutoPatcher
import com.viciousscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatchPreviewScreen(
    previews: List<AutoPatcher.PatchPreview>,
    onConfirm: (List<AutoPatcher.PatchPreview>) -> Unit,
    onCancel: () -> Unit
) {
    val selected = remember { mutableStateListOf(*previews.toTypedArray()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PATCH PREVIEW",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = ViciousGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel",
                            tint = ViciousOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ViciousSurface)
            )
        },
        bottomBar = {
            Surface(color = ViciousCard) {
                Button(
                    onClick = { onConfirm(selected.toList()) },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ViciousGreen)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "APPLY ${selected.size} PATCH${if (selected.size != 1) "ES" else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        },
        containerColor = ViciousSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Review changes before writing to disk. Uncheck any file to skip it.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ViciousMuted,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            items(previews) { preview ->
                PatchPreviewCard(
                    preview = preview,
                    checked = selected.contains(preview),
                    onCheckedChange = { checked ->
                        if (checked) selected.add(preview) else selected.remove(preview)
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PatchPreviewCard(
    preview: AutoPatcher.PatchPreview,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var showDiff by remember { mutableStateOf(false) }

    Surface(
        color = ViciousCard,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(checkedColor = ViciousGreen)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        preview.fileName,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ViciousOnSurface
                    )
                    Text(
                        "${preview.appliedFindings.size} change${if (preview.appliedFindings.size != 1) "s" else ""}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ViciousMuted
                    )
                }
                TextButton(onClick = { showDiff = !showDiff }) {
                    Text(
                        if (showDiff) "HIDE" else "DIFF",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ViciousGreen,
                        letterSpacing = 2.sp
                    )
                }
            }

            if (showDiff) {
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
            }
        }
    }
}
