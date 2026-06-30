package com.viciousscan.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viciousscan.app.model.*
import com.viciousscan.app.scanner.AutoPatcher
import com.viciousscan.app.scanner.FileReader
import com.viciousscan.app.scanner.ScanEngine
import com.viciousscan.app.model.ProjectInfo
import com.viciousscan.app.model.ProjectInfoRepository
import com.viciousscan.app.scanner.ReportGenerator
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Scanning : ScanUiState()
    data class Results(val report: ScanReport, val targetPath: String = "") : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

sealed class PatchUiState {
    object Idle : PatchUiState()
    data class Previewing(val previews: List<AutoPatcher.PatchPreview>) : PatchUiState()
    object Applying : PatchUiState()
    data class Done(val successCount: Int, val failCount: Int) : PatchUiState()
    data class Error(val message: String) : PatchUiState()
}

sealed class HistoryUiState {
    object Hidden : HistoryUiState()
    data class Showing(val entries: List<ScanHistoryEntry>) : HistoryUiState()
    data class ViewingEntry(val entry: ScanHistoryEntry) : HistoryUiState()
}

sealed class CatalogUiState {
    object Hidden : CatalogUiState()
    object Showing : CatalogUiState()
}

sealed class ExportUiState {
    object Idle : ExportUiState()
    data class Done(val content: String, val fileName: String) : ExportUiState()
}

sealed class ProjectInfoUiState {
    object Hidden : ProjectInfoUiState()
    data class Editing(val info: ProjectInfo) : ProjectInfoUiState()
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _patchState = MutableStateFlow<PatchUiState>(PatchUiState.Idle)
    val patchState: StateFlow<PatchUiState> = _patchState

    private val _historyState = MutableStateFlow<HistoryUiState>(HistoryUiState.Hidden)
    val historyState: StateFlow<HistoryUiState> = _historyState

    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Hidden)
    val catalogState: StateFlow<CatalogUiState> = _catalogState

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState

    private val _projectInfoState = MutableStateFlow<ProjectInfoUiState>(ProjectInfoUiState.Hidden)
    val projectInfoState: StateFlow<ProjectInfoUiState> = _projectInfoState

    private var manifestUri: Uri? = null
    private var gradleUri: Uri? = null

    fun showHistory() {
        val ctx = getApplication<Application>()
        val entries = HistoryRepository.load(ctx)
        _historyState.value = HistoryUiState.Showing(entries)
    }

    fun hideHistory() { _historyState.value = HistoryUiState.Hidden }

    fun viewHistoryEntry(entry: ScanHistoryEntry) {
        _historyState.value = HistoryUiState.ViewingEntry(entry)
    }

    fun deleteHistoryEntry(id: String) {
        val ctx = getApplication<Application>()
        HistoryRepository.delete(ctx, id)
        _historyState.value = HistoryUiState.Showing(HistoryRepository.load(ctx))
    }

    fun clearHistory() {
        val ctx = getApplication<Application>()
        HistoryRepository.clear(ctx)
        _historyState.value = HistoryUiState.Showing(emptyList())
    }

    fun scanFolder(treeUri: Uri) {
        _scanState.value = ScanUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val files = FileReader.readTree(ctx, treeUri)
                if (files.isEmpty()) {
                    _scanState.value = ScanUiState.Error("No supported source files found.")
                    return@launch
                }
                cacheSpecialUris(ctx, treeUri)
                val report = ScanEngine.scan(files)
                val path = treeUri.lastPathSegment ?: treeUri.toString()
                saveToHistory(ctx, report, path)
                _scanState.value = ScanUiState.Results(report, path)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun scanSingleFile(uri: Uri, displayName: String) {
        _scanState.value = ScanUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val files = FileReader.readSingleFile(ctx, uri, displayName)
                if (files.isEmpty()) {
                    _scanState.value = ScanUiState.Error("File type not supported or file is empty.")
                    return@launch
                }
                val report = ScanEngine.scan(files)
                saveToHistory(ctx, report, displayName)
                _scanState.value = ScanUiState.Results(report, displayName)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun saveToHistory(ctx: android.content.Context, report: ScanReport, path: String) {
        val entry = ScanHistoryEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            targetPath = path,
            projectType = report.projectType,
            totalFindings = report.findings.size,
            requiredCount = report.findings.count { it.severity == Severity.REQUIRED },
            recommendedCount = report.findings.count { it.severity == Severity.RECOMMENDED },
            optionalCount = report.findings.count { it.severity == Severity.OPTIONAL },
            findings = report.findings
        )
        HistoryRepository.save(ctx, entry)
    }

    fun buildPatchPreviews() {
        val results = (_scanState.value as? ScanUiState.Results) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val mContent = manifestUri?.let {
                    ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                }
                val gContent = gradleUri?.let {
                    ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                }
                val previews = AutoPatcher.buildPreviews(
                    manifestUri, mContent, gradleUri, gContent, results.report.findings)
                _patchState.value = if (previews.isEmpty())
                    PatchUiState.Error("Nothing to auto-patch.")
                else PatchUiState.Previewing(previews)
            } catch (e: Exception) {
                _patchState.value = PatchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun applyPatches(previews: List<AutoPatcher.PatchPreview>) {
        _patchState.value = PatchUiState.Applying
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            var ok = 0; var fail = 0
            for (p in previews) { if (AutoPatcher.writeBack(ctx, p)) ok++ else fail++ }
            _patchState.value = PatchUiState.Done(ok, fail)
        }
    }

    fun showCatalog() { _catalogState.value = CatalogUiState.Showing }
    fun hideCatalog() { _catalogState.value = CatalogUiState.Hidden }

    fun showProjectInfo() {
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

    fun resetScan() { _scanState.value = ScanUiState.Idle; _patchState.value = PatchUiState.Idle }
    fun resetPatch() { _patchState.value = PatchUiState.Idle }

    private fun cacheSpecialUris(ctx: android.content.Context, treeUri: Uri) {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, treeUri) ?: return
        findSpecialFiles(root)
    }

    private fun findSpecialFiles(dir: androidx.documentfile.provider.DocumentFile) {
        for (child in dir.listFiles()) {
            when {
                child.isDirectory -> findSpecialFiles(child)
                child.name?.lowercase() == "androidmanifest.xml" -> manifestUri = child.uri
                child.name?.let {
                    it.lowercase() == "build.gradle.kts" || it.lowercase() == "build.gradle"
                } == true -> gradleUri = child.uri
            }
        }
    }
}
