package com.viciousscan.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.ui.theme.*

@Composable
fun HomeScreen(
    onFolderSelected: (Uri) -> Unit,
    onFileSelected: (Uri, String) -> Unit,
    onShowHistory: () -> Unit
) {
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) onFolderSelected(uri)
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onFileSelected(uri, uri.lastPathSegment ?: "file")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / title
        Text(
            text = "VICIOUS",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 48.sp,
            color = ViciousRed,
            letterSpacing = 8.sp
        )
        Text(
            text = "SCAN",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            fontSize = 28.sp,
            color = ViciousOnSurface,
            letterSpacing = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Analyze your code.\nFind missing permissions & dependencies.",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = ViciousMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onShowHistory,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.viciousscan.app.ui.theme.ViciousRed)
        ) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("SCAN HISTORY", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, letterSpacing = 2.sp)
        }

        // Scan folder button
        Button(
            onClick = { folderLauncher.launch(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ViciousRed)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "SCAN PROJECT FOLDER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan single file button
        OutlinedButton(
            onClick = {
                fileLauncher.launch(
                    arrayOf(
                        "text/plain", "text/x-kotlin", "text/java",
                        "text/xml", "application/octet-stream", "*/*"
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(4.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                // Use explicit border modifier instead
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ViciousOnSurface)
        ) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "SCAN SINGLE FILE",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Supports Kotlin · Java · XML · Gradle\nPython · JS · TS · Swift · Dart · Go · Rust",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = ViciousMuted,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}
