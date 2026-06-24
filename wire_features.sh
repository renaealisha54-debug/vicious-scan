#!/bin/bash
set -e
cd ~/vicious-scan

# ── 1. Update ScanViewModel ───────────────────────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/viewmodel/ScanViewModel.kt').read()

# Add imports if missing
imports_to_add = [
    'import com.viciousscan.app.model.ProjectInfo',
    'import com.viciousscan.app.model.ProjectInfoRepository',
    'import com.viciousscan.app.scanner.ReportGenerator',
    'import android.content.Context',
]
for imp in imports_to_add:
    if imp not in content:
        content = content.replace(
            'import kotlinx.coroutines.Dispatchers',
            imp + '\nimport kotlinx.coroutines.Dispatchers'
        )

# Add ProjectInfo state after HistoryUiState sealed class
if 'projectInfo' not in content:
    content = content.replace(
        'class ScanViewModel(app: Application) : AndroidViewModel(app) {',
        '''sealed class ExportUiState {
    object Idle : ExportUiState()
    data class Done(val content: String, val fileName: String) : ExportUiState()
}

sealed class ProjectInfoUiState {
    object Hidden : ProjectInfoUiState()
    data class Editing(val info: ProjectInfo) : ProjectInfoUiState()
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {'''
    )

    # Add new StateFlows after existing ones
    content = content.replace(
        '    private var manifestUri: Uri? = null',
        '''    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState

    private val _projectInfoState = MutableStateFlow<ProjectInfoUiState>(ProjectInfoUiState.Hidden)
    val projectInfoState: StateFlow<ProjectInfoUiState> = _projectInfoState

    private var manifestUri: Uri? = null'''
    )

    # Add new functions before resetScan
    content = content.replace(
        '    fun resetScan()',
        '''    fun showProjectInfo() {
        val ctx = getApplication<Application>()
        _projectInfoState.value = ProjectInfoUiState.Editing(ProjectInfoRepository.load(ctx))
    }

    fun saveProjectInfo(info: ProjectInfo) {
        val ctx = getApplication<Application>()
        ProjectInfoRepository.save(ctx, info)
        _projectInfoState.value = ProjectInfoUiState.Hidden
    }

    fun hideProjectInfo() { _projectInfoState.value = ProjectInfoUiState.Hidden }

    fun exportAiReadme() {
        val s = _scanState.value as? ScanUiState.Results ?: return
        val ctx = getApplication<Application>()
        val info = ProjectInfoRepository.load(ctx)
        val content = ReportGenerator.generateAiReadme(s.report, info)
        _exportState.value = ExportUiState.Done(content, "ai-brief.md")
    }

    fun exportRawTemplate() {
        val s = _scanState.value as? ScanUiState.Results ?: return
        val ctx = getApplication<Application>()
        val info = ProjectInfoRepository.load(ctx)
        val content = ReportGenerator.generateRawTemplate(s.report, info)
        _exportState.value = ExportUiState.Done(content, "raw-template.md")
    }

    fun saveExportToFile(ctx: Context, content: String, fileName: String): String {
        val file = java.io.File(ctx.getExternalFilesDir(null), fileName)
        file.writeText(content)
        _exportState.value = ExportUiState.Idle
        return file.absolutePath
    }

    fun resetExport() { _exportState.value = ExportUiState.Idle }

    fun resetScan()'''
    )

open('app/src/main/java/com/viciousscan/app/viewmodel/ScanViewModel.kt', 'w').write(content)
print("ScanViewModel.kt updated")
EOF

# ── 2. Rewrite ResultsScreen to show snippets + export buttons ────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/ui/screens/ResultsScreen.kt').read()

# Add missing imports
imports_to_add = [
    'import androidx.compose.foundation.horizontalScroll',
    'import androidx.compose.foundation.rememberScrollState',
    'import androidx.compose.material.icons.filled.FileDownload',
    'import androidx.compose.material.icons.filled.Settings',
    'import androidx.compose.ui.text.style.TextOverflow',
]
for imp in imports_to_add:
    if imp not in content:
        content = content.replace(
            'import androidx.compose.material.icons.filled.*',
            'import androidx.compose.material.icons.filled.*\n' + imp
        )

# Update ResultsScreen signature to add export callbacks
content = content.replace(
    '''fun ResultsScreen(
    report: ScanReport,
    onAutoPatch: () -> Unit,
    onReset: () -> Unit
)''',
    '''fun ResultsScreen(
    report: ScanReport,
    onAutoPatch: () -> Unit,
    onReset: () -> Unit,
    onExportReadme: (() -> Unit)? = null,
    onExportRaw: (() -> Unit)? = null,
    onProjectInfo: (() -> Unit)? = null
)'''
)

# Add export buttons after auto-patch button block
content = content.replace(
    '            item { Spacer(Modifier.height(24.dp)) }',
    '''            // Export buttons
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onExportReadme != null) {
                        OutlinedButton(
                            onClick = onExportReadme,
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("AI README", fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    if (onExportRaw != null) {
                        OutlinedButton(
                            onClick = onExportRaw,
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RAW CODE", fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
                if (onProjectInfo != null) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onProjectInfo,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PROJECT INFO", fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            item { Spacer(Modifier.height(24.dp)) }'''
)

# Update FindingCard to show snippet + source location
content = content.replace(
    '''                if (expanded) {
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
                }''',
    '''                if (expanded) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        finding.reason,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ViciousMuted,
                        lineHeight = 16.sp
                    )
                    // Show source location
                    if (finding.sourceFile != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "📍 ${finding.sourceFile}${if (finding.sourceLine != null) " : line ${finding.sourceLine}" else ""}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = ViciousRed.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Show code snippet
                    finding.codeSnippet?.let { snippet ->
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFF0D1117),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                snippet,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = Color(0xFFE6EDF3),
                                lineHeight = 14.sp
                            )
                        }
                    }
                    // Show fix snippet
                    finding.autoFixSnippet?.let { snippet ->
                        Spacer(Modifier.height(6.dp))
                        Text("FIX:", fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp, color = ViciousGreen, letterSpacing = 2.sp)
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            color = Color(0xFF0A1A0A),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                snippet,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = ViciousGreen,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }'''
)

open('app/src/main/java/com/viciousscan/app/ui/screens/ResultsScreen.kt', 'w').write(content)
print("ResultsScreen.kt updated")
EOF

# ── 3. Update MainActivity to wire everything ────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/MainActivity.kt').read()

# Add imports
new_imports = [
    'import com.viciousscan.app.ui.screens.ProjectInfoScreen',
    'import com.viciousscan.app.viewmodel.ExportUiState',
    'import com.viciousscan.app.viewmodel.ProjectInfoUiState',
]
for imp in new_imports:
    if imp not in content:
        content = content.replace(
            'import com.viciousscan.app.viewmodel.HistoryUiState',
            'import com.viciousscan.app.viewmodel.HistoryUiState\n' + imp
        )

# Add new state observations
if 'exportState' not in content:
    content = content.replace(
        '    val snackbar     = remember { SnackbarHostState() }',
        '''    val exportState      by vm.exportState.collectAsStateWithLifecycle()
    val projectInfoState by vm.projectInfoState.collectAsStateWithLifecycle()
    val snackbar         = remember { SnackbarHostState() }
    val scope            = rememberCoroutineScope()'''
    )

    # Add export handler in LaunchedEffect
    content = content.replace(
        '    LaunchedEffect(patchState) {',
        '''    LaunchedEffect(exportState) {
        if (exportState is ExportUiState.Done) {
            val d = exportState as ExportUiState.Done
            val path = vm.saveExportToFile(scope.let { androidContext }, d.content, d.fileName)
            snackbar.showSnackbar("Saved: $path")
            vm.resetExport()
        }
    }

    LaunchedEffect(patchState) {'''
    )

# Add projectInfo screen routing in the Box
if 'ProjectInfoScreen' not in content:
    content = content.replace(
        '            when (val h = historyState) {',
        '''            // Project info overlay
            if (projectInfoState is ProjectInfoUiState.Editing) {
                val editState = projectInfoState as ProjectInfoUiState.Editing
                ProjectInfoScreen(
                    initial = editState.info,
                    onSave = { vm.saveProjectInfo(it) },
                    onBack = { vm.hideProjectInfo() }
                )
                return@Box
            }

            when (val h = historyState) {'''
    )

# Update ResultsScreen call to pass export callbacks
content = content.replace(
    '''                    else -> ResultsScreen(
                        report = s.report,
                        onAutoPatch = { vm.buildPatchPreviews() },
                        onReset = { vm.resetScan() }
                    )''',
    '''                    else -> ResultsScreen(
                        report = s.report,
                        onAutoPatch = { vm.buildPatchPreviews() },
                        onReset = { vm.resetScan() },
                        onExportReadme = { vm.exportAiReadme() },
                        onExportRaw = { vm.exportRawTemplate() },
                        onProjectInfo = { vm.showProjectInfo() }
                    )'''
    )

open('app/src/main/java/com/viciousscan/app/MainActivity.kt', 'w').write(content)
print("MainActivity.kt updated")
EOF

echo "ALL WIRING DONE"
