package com.viciousscan.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.model.ScanHistoryEntry
import com.viciousscan.app.ui.theme.ViciousRed
import com.viciousscan.app.ui.theme.ViciousSurface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    entries: List<ScanHistoryEntry>,
    onEntryClick: (ScanHistoryEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Delete all scan history? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text("Clear All", color = ViciousRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
    Scaffold(
        containerColor = ViciousSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text("SCAN HISTORY", fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, color = ViciousRed)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ViciousRed)
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "Clear all", tint = ViciousRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ViciousSurface)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No scan history yet.", fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryCard(entry, onClick = { onEntryClick(entry) },
                        onDelete = { onDeleteEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: ScanHistoryEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM dd yyyy  HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.targetPath.substringAfterLast("/"),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(fmt.format(Date(entry.timestamp)), fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SeverityChip("REQ ${entry.requiredCount}", ViciousRed)
                    SeverityChip("REC ${entry.recommendedCount}", MaterialTheme.colorScheme.primary)
                    SeverityChip("OPT ${entry.optionalCount}",
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = ViciousRed.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun SeverityChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.15f)) {
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = color)
    }
}
