package com.viciousscan.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.model.ProjectInfo
import com.viciousscan.app.ui.theme.ViciousRed
import com.viciousscan.app.ui.theme.ViciousSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectInfoScreen(
    initial: ProjectInfo,
    onSave: (ProjectInfo) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(initial.developerName) }
    var email by remember { mutableStateOf(initial.email) }
    var github by remember { mutableStateOf(initial.githubUsername) }
    var repo by remember { mutableStateOf(initial.repoUrl) }
    var pkg by remember { mutableStateOf(initial.packageName) }
    var appName by remember { mutableStateOf(initial.appName) }

    Scaffold(
        containerColor = ViciousSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text("PROJECT INFO", fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, color = ViciousRed)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ViciousRed)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(ProjectInfo(name, email, github, repo, pkg, appName))
                    }) {
                        Text("SAVE", fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, color = ViciousRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ViciousSurface)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Used in AI README exports and raw code templates.",
                fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))
            InfoField("App Name", appName) { appName = it }
            InfoField("Package Name", pkg) { pkg = it }
            InfoField("Developer Name", name) { name = it }
            InfoField("Email", email) { email = it }
            InfoField("GitHub Username", github) { github = it }
            InfoField("Repo URL", repo) { repo = it }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSave(ProjectInfo(name, email, github, repo, pkg, appName)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ViciousRed)
            ) {
                Text("SAVE PROJECT INFO", fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun InfoField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    )
}
