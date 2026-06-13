package com.viciousscan.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.viciousscan.app.model.ScanReport
import com.viciousscan.app.scanner.AutoPatcher
import com.viciousscan.app.scanner.FileReader
import com.viciousscan.app.scanner.ScanEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Scanning : ScanUiState()
    data class Results(val report: ScanReport) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

sealed class PatchUiState {
    object Idle : PatchUiState()
    data class Previewing(val previews: List<AutoPatcher.PatchPreview>) : PatchUiState()
    object Applying : PatchUiState()
    data class Done(val successCount: Int, val failCount: Int) : PatchUiState()
    data class Error(val message: String) : PatchUiState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState

    private val _patchState = MutableStateFlow<PatchUiState>(PatchUiState.Idle)
    val patchState: StateFlow<PatchUiState> = _patchState

    // Keep URIs for auto-patch targets
    private var manifestUri: Uri? = null
    private var gradleUri: Uri? = null

    /** Called when user picks a folder tree URI via SAF. */
    fun scanFolder(treeUri: Uri) {
        _scanState.value = ScanUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val files = FileReader.readTree(ctx, treeUri)
                if (files.isEmpty()) {
                    _scanState.value = ScanUiState.Error(
                        "No supported source files found in the selected folder."
                    )
                    return@launch
                }
                // Cache manifest + gradle URIs for patching
                cacheSpecialUris(ctx, treeUri)
                val report = ScanEngine.scan(files)
                _scanState.value = ScanUiState.Results(report)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Called when user picks a single file URI via SAF. */
    fun scanSingleFile(uri: Uri, displayName: String) {
        _scanState.value = ScanUiState.Scanning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val files = FileReader.readSingleFile(ctx, uri, displayName)
                if (files.isEmpty()) {
                    _scanState.value = ScanUiState.Error(
                        "File type not supported or file is empty."
                    )
                    return@launch
                }
                val report = ScanEngine.scan(files)
                _scanState.value = ScanUiState.Results(report)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Build patch previews for review before writing. */
    fun buildPatchPreviews() {
        val results = (_scanState.value as? ScanUiState.Results) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx = getApplication<Application>()
                val mUri = manifestUri
                val gUri = gradleUri
                val mContent = mUri?.let {
                    ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                }
                val gContent = gUri?.let {
                    ctx.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                }
                val previews = AutoPatcher.buildPreviews(
                    mUri, mContent, gUri, gContent, results.report.findings
                )
                _patchState.value = if (previews.isEmpty())
                    PatchUiState.Error("Nothing to auto-patch — apply changes manually.")
                else
                    PatchUiState.Previewing(previews)
            } catch (e: Exception) {
                _patchState.value = PatchUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Apply the confirmed previews. */
    fun applyPatches(previews: List<AutoPatcher.PatchPreview>) {
        _patchState.value = PatchUiState.Applying
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            var ok = 0; var fail = 0
            for (preview in previews) {
                if (AutoPatcher.writeBack(ctx, preview)) ok++ else fail++
            }
            _patchState.value = PatchUiState.Done(ok, fail)
        }
    }

    fun resetScan() {
        _scanState.value = ScanUiState.Idle
        _patchState.value = PatchUiState.Idle
    }

    fun resetPatch() {
        _patchState.value = PatchUiState.Idle
    }

    // -------------------------------------------------------------------------

    private fun cacheSpecialUris(ctx: android.content.Context, treeUri: Uri) {
        // Walk the tree to find manifest + gradle files
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
                } == true -> {
                    // Prefer the app-level gradle (deeper path)
                    gradleUri = child.uri
                }
            }
        }
    }
}
