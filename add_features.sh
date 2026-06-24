#!/bin/bash
set -e
cd ~/vicious-scan

# ── 1. Update ScanModels.kt ──────────────────────────────────────────────────
python3 << 'EOF'
path = 'app/src/main/java/com/viciousscan/app/model/ScanModels.kt'
content = open(path).read()
# Add codeSnippet and filePath to ScanFinding if not already there
if 'codeSnippet' not in content:
    content = content.replace(
        'val autoFixSnippet: String? = null   // null = manual-only\n)',
        'val autoFixSnippet: String? = null,   // null = manual-only\n    val codeSnippet: String? = null,      // actual line(s) from source\n    val sourceFile: String? = null,       // file where trigger was found\n    val sourceLine: Int? = null           // line number of trigger\n)'
    )
    open(path, 'w').write(content)
    print("ScanModels.kt updated")
else:
    print("ScanModels.kt already updated")
EOF

# ── 2. Update ScanEngine to track file/line ───────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/scanner/ScanEngine.kt').read()
old_scan = '''        for ((_, content) in files) {
            for (rule in rules) {
                if (rule.trigger in content && seen.add(rule.finding.name)) {
                    findings.add(rule.finding)
                }
            }
        }'''
new_scan = '''        for ((fileName, content) in files) {
            val lines = content.lines()
            for (rule in rules) {
                if (rule.trigger in content && seen.add(rule.finding.name)) {
                    // Find the line number and snippet
                    val lineIndex = lines.indexOfFirst { rule.trigger in it }
                    val snippet = if (lineIndex >= 0) {
                        val start = maxOf(0, lineIndex - 1)
                        val end = minOf(lines.size - 1, lineIndex + 1)
                        lines.subList(start, end + 1).joinToString("\\n")
                    } else null
                    findings.add(rule.finding.copy(
                        sourceFile = fileName,
                        sourceLine = if (lineIndex >= 0) lineIndex + 1 else null,
                        codeSnippet = snippet
                    ))
                }
            }
        }'''
if 'sourceFile' not in content:
    content = content.replace(old_scan, new_scan)
    open('app/src/main/java/com/viciousscan/app/scanner/ScanEngine.kt', 'w').write(content)
    print("ScanEngine.kt updated")
else:
    print("ScanEngine.kt already updated")
EOF

# ── 3. ProjectInfo model + repository ────────────────────────────────────────
cat > app/src/main/java/com/viciousscan/app/model/ProjectInfo.kt << 'EOF'
package com.viciousscan.app.model

import android.content.Context
import org.json.JSONObject
import java.io.File

data class ProjectInfo(
    val developerName: String = "",
    val email: String = "",
    val githubUsername: String = "",
    val repoUrl: String = "",
    val packageName: String = "",
    val appName: String = ""
)

object ProjectInfoRepository {
    private fun file(ctx: Context) = File(ctx.filesDir, "project_info.json")

    fun load(ctx: Context): ProjectInfo {
        val f = file(ctx)
        if (!f.exists()) return ProjectInfo()
        return try {
            val j = JSONObject(f.readText())
            ProjectInfo(
                developerName = j.optString("developerName"),
                email = j.optString("email"),
                githubUsername = j.optString("githubUsername"),
                repoUrl = j.optString("repoUrl"),
                packageName = j.optString("packageName"),
                appName = j.optString("appName")
            )
        } catch (e: Exception) { ProjectInfo() }
    }

    fun save(ctx: Context, info: ProjectInfo) {
        val j = JSONObject().apply {
            put("developerName", info.developerName)
            put("email", info.email)
            put("githubUsername", info.githubUsername)
            put("repoUrl", info.repoUrl)
            put("packageName", info.packageName)
            put("appName", info.appName)
        }
        file(ctx).writeText(j.toString())
    }
}
EOF

# ── 4. ReportGenerator (AI README + Raw Code) ────────────────────────────────
cat > app/src/main/java/com/viciousscan/app/scanner/ReportGenerator.kt << 'EOF'
package com.viciousscan.app.scanner

import com.viciousscan.app.model.*

object ReportGenerator {

    /** Generate an AI-readable markdown README from scan results + project info */
    fun generateAiReadme(report: ScanReport, info: ProjectInfo): String {
        val sb = StringBuilder()
        sb.appendLine("# ${info.appName.ifEmpty { "Android Project" }} — AI Revision Brief")
        sb.appendLine()
        sb.appendLine("## Project Info")
        if (info.developerName.isNotEmpty()) sb.appendLine("- **Developer:** ${info.developerName}")
        if (info.email.isNotEmpty()) sb.appendLine("- **Email:** ${info.email}")
        if (info.githubUsername.isNotEmpty()) sb.appendLine("- **GitHub:** ${info.githubUsername}")
        if (info.repoUrl.isNotEmpty()) sb.appendLine("- **Repo:** ${info.repoUrl}")
        if (info.packageName.isNotEmpty()) sb.appendLine("- **Package:** ${info.packageName}")
        sb.appendLine()
        sb.appendLine("## Scan Summary")
        sb.appendLine("- Files scanned: ${report.scannedFiles.size}")
        sb.appendLine("- Total findings: ${report.findings.size}")
        sb.appendLine("- Project type: ${report.projectType.name}")
        sb.appendLine("- Scan duration: ${report.scanDurationMs}ms")
        sb.appendLine()

        val required = report.findings.filter { it.severity == Severity.REQUIRED }
        val recommended = report.findings.filter { it.severity == Severity.RECOMMENDED }
        val optional = report.findings.filter { it.severity == Severity.OPTIONAL }

        if (required.isNotEmpty()) {
            sb.appendLine("## Required Changes (App will fail without these)")
            required.forEach { f ->
                sb.appendLine()
                sb.appendLine("### ${f.name}")
                sb.appendLine("- **Type:** ${f.type.name.replace('_', ' ')}")
                sb.appendLine("- **Reason:** ${f.reason}")
                if (f.sourceFile != null) {
                    sb.appendLine("- **Found in:** `${f.sourceFile}`${if (f.sourceLine != null) " line ${f.sourceLine}" else ""}")
                }
                if (f.codeSnippet != null) {
                    sb.appendLine("- **Code context:**")
                    sb.appendLine("```")
                    sb.appendLine(f.codeSnippet)
                    sb.appendLine("```")
                }
                if (f.autoFixSnippet != null) {
                    sb.appendLine("- **Fix to apply:**")
                    sb.appendLine("```")
                    sb.appendLine(f.autoFixSnippet)
                    sb.appendLine("```")
                }
            }
            sb.appendLine()
        }

        if (recommended.isNotEmpty()) {
            sb.appendLine("## Recommended Changes")
            recommended.forEach { f ->
                sb.appendLine()
                sb.appendLine("### ${f.name}")
                sb.appendLine("- **Reason:** ${f.reason}")
                if (f.sourceFile != null) sb.appendLine("- **Found in:** `${f.sourceFile}`${if (f.sourceLine != null) " line ${f.sourceLine}" else ""}")
                if (f.autoFixSnippet != null) {
                    sb.appendLine("```")
                    sb.appendLine(f.autoFixSnippet)
                    sb.appendLine("```")
                }
            }
            sb.appendLine()
        }

        if (optional.isNotEmpty()) {
            sb.appendLine("## Optional / Cleanup")
            optional.forEach { f ->
                sb.appendLine("- **${f.name}**: ${f.reason}${if (f.sourceFile != null) " (`${f.sourceFile}`${if (f.sourceLine != null) " L${f.sourceLine}" else ""})" else ""}")
            }
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("*Generated by ViciousScan — https://github.com/${info.githubUsername.ifEmpty { "your-username" }}/${info.repoUrl.substringAfterLast("/").ifEmpty { "your-repo" }}*")
        return sb.toString()
    }

    /** Generate a raw code template with all permissions/dependencies blanked out */
    fun generateRawTemplate(report: ScanReport, info: ProjectInfo): String {
        val sb = StringBuilder()
        sb.appendLine("# Raw Code Template — ${info.appName.ifEmpty { "Project" }}")
        sb.appendLine("# Package: ${info.packageName.ifEmpty { "com.your.package" }}")
        sb.appendLine("# Fill in the blanks marked with TODO")
        sb.appendLine()

        val permissions = report.findings.filter { it.type == FindingType.PERMISSION }
        val dependencies = report.findings.filter { it.type == FindingType.GRADLE_DEPENDENCY }

        if (permissions.isNotEmpty()) {
            sb.appendLine("## AndroidManifest.xml — Permissions to add")
            sb.appendLine("```xml")
            sb.appendLine("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            sb.appendLine("    package=\"${info.packageName.ifEmpty { "TODO: your.package.name" }}\">")
            sb.appendLine()
            permissions.forEach { f ->
                sb.appendLine("    <!-- ${f.reason} -->")
                if (f.name.contains("ACCESSIBILITY")) {
                    sb.appendLine("    <!-- Accessibility sub-options — choose what applies: -->")
                    sb.appendLine("    <!-- Display: TYPE_VIEW_CLICKED, TYPE_VIEW_FOCUSED, TYPE_WINDOW_STATE_CHANGED -->")
                    sb.appendLine("    <!-- Input: TYPE_VIEW_TEXT_CHANGED, TYPE_VIEW_TEXT_SELECTION_CHANGED -->")
                    sb.appendLine("    <!-- Navigation: TYPE_WINDOWS_CHANGED, TYPE_VIEW_SCROLLED -->")
                    sb.appendLine("    <uses-permission android:name=\"${f.name}\" /> <!-- TODO: verify scope needed -->")
                } else {
                    sb.appendLine("    <uses-permission android:name=\"${f.name}\" /> <!-- TODO: confirm needed -->")
                }
                sb.appendLine()
            }
            sb.appendLine("</manifest>")
            sb.appendLine("```")
            sb.appendLine()
        }

        if (dependencies.isNotEmpty()) {
            sb.appendLine("## build.gradle.kts — Dependencies to add")
            sb.appendLine("```kotlin")
            sb.appendLine("dependencies {")
            dependencies.forEach { f ->
                sb.appendLine("    // ${f.reason}")
                if (f.autoFixSnippet != null) {
                    f.autoFixSnippet.lines().forEach { line ->
                        sb.appendLine("    $line // TODO: verify version")
                    }
                } else {
                    sb.appendLine("    // TODO: add ${f.name}")
                }
                sb.appendLine()
            }
            sb.appendLine("}")
            sb.appendLine("```")
            sb.appendLine()
        }

        sb.appendLine("## Files requiring changes")
        report.findings.filter { it.sourceFile != null }.groupBy { it.sourceFile }.forEach { (file, findings) ->
            sb.appendLine()
            sb.appendLine("### `$file`")
            findings.forEach { f ->
                sb.appendLine("- Line ${f.sourceLine ?: "?"}: **${f.name}** — ${f.reason}")
                if (f.codeSnippet != null) {
                    sb.appendLine("  ```")
                    f.codeSnippet.lines().forEach { sb.appendLine("  $it") }
                    sb.appendLine("  ```")
                    sb.appendLine("  → TODO: ${f.autoFixSnippet ?: "fix manually"}")
                }
            }
        }
        return sb.toString()
    }
}
EOF

# ── 5. ProjectInfoScreen.kt ───────────────────────────────────────────────────
cat > app/src/main/java/com/viciousscan/app/ui/screens/ProjectInfoScreen.kt << 'EOF'
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
EOF

echo "ALL FEATURE FILES WRITTEN OK"
