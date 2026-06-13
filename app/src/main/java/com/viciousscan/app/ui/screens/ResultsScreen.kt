package com.viciousscan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.model.*
import com.viciousscan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    report: ScanReport,
    onAutoPatch: () -> Unit,
    onReset: () -> Unit
) {
    val required     = report.findings.filter { it.severity == Severity.REQUIRED }
    val recommended  = report.findings.filter { it.severity == Severity.RECOMMENDED }
    val optional     = report.findings.filter { it.severity == Severity.OPTIONAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SCAN RESULTS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = ViciousRed
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = ViciousOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ViciousSurface
                )
            )
        },
        containerColor = ViciousSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats header
            item {
                Spacer(Modifier.height(8.dp))
                ScanSummaryCard(report)
                Spacer(Modifier.height(8.dp))
            }

            // Auto-patch button (only when findings have fix snippets)
            val patchable = report.findings.count { it.autoFixSnippet != null }
            if (patchable > 0) {
                item {
                    Button(
                        onClick = onAutoPatch,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ViciousGreen)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null,
                            tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AUTO-PATCH $patchable ITEM${if (patchable != 1) "S" else ""}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (report.findings.isEmpty()) {
                item {
                    EmptyState()
                }
            }

            // REQUIRED
            if (required.isNotEmpty()) {
                item { SectionHeader("REQUIRED", ViciousOrange, required.size) }
                items(required) { FindingCard(it) }
            }

            // RECOMMENDED
            if (recommended.isNotEmpty()) {
                item { SectionHeader("RECOMMENDED", ViciousYellow, recommended.size) }
                items(recommended) { FindingCard(it) }
            }

            // OPTIONAL
            if (optional.isNotEmpty()) {
                item { SectionHeader("OPTIONAL", ViciousMuted, optional.size) }
                items(optional) { FindingCard(it) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ScanSummaryCard(report: ScanReport) {
    Surface(
        color = ViciousCard,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(report.scannedFiles.size.toString(), "FILES")
            StatItem(report.findings.size.toString(), "FINDINGS")
            StatItem("${report.scanDurationMs}ms", "TIME")
            StatItem(report.projectType.name, "TYPE")
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = ViciousRed
        )
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = ViciousMuted,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun SectionHeader(label: String, color: Color, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(16.dp)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$label  ($count)",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = color,
            letterSpacing = 3.sp
        )
    }
}

@Composable
private fun FindingCard(finding: ScanFinding) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = ViciousCard,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (finding.severity) {
                    Severity.REQUIRED    -> ViciousOrange.copy(alpha = 0.5f)
                    Severity.RECOMMENDED -> ViciousYellow.copy(alpha = 0.3f)
                    Severity.OPTIONAL    -> ViciousBorder
                },
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        finding.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ViciousOnSurface
                    )
                    Text(
                        finding.type.name.replace('_', ' '),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = ViciousMuted,
                        letterSpacing = 1.sp
                    )
                }
                if (finding.autoFixSnippet != null) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = "Auto-patchable",
                        tint = ViciousGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = ViciousMuted
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    finding.reason,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ViciousMuted,
                    lineHeight = 16.sp
                )
                finding.autoFixSnippet?.let { snippet ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF111111),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            snippet,
                            modifier = Modifier.padding(8.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = ViciousGreen,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ViciousGreen,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "NO ISSUES FOUND",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = ViciousGreen,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Your code looks clean.",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = ViciousMuted
        )
    }
}
