package com.viciousscan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viciousscan.app.model.ScanReport
import com.viciousscan.app.ui.screens.HistoryScreen
import com.viciousscan.app.ui.screens.HomeScreen
import com.viciousscan.app.ui.screens.PatchPreviewScreen
import com.viciousscan.app.ui.screens.ResultsScreen
import com.viciousscan.app.ui.theme.ViciousRed
import com.viciousscan.app.ui.theme.ViciousScanTheme
import com.viciousscan.app.ui.theme.ViciousSurface
import com.viciousscan.app.viewmodel.HistoryUiState
import com.viciousscan.app.viewmodel.PatchUiState
import com.viciousscan.app.viewmodel.ScanUiState
import com.viciousscan.app.viewmodel.ScanViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ViciousScanTheme { ViciousScanApp() } }
    }
}

@Composable
fun ViciousScanApp(vm: ScanViewModel = viewModel()) {
    val scanState    by vm.scanState.collectAsStateWithLifecycle()
    val patchState   by vm.patchState.collectAsStateWithLifecycle()
    val historyState by vm.historyState.collectAsStateWithLifecycle()
    val snackbar     = remember { SnackbarHostState() }

    LaunchedEffect(patchState) {
        if (patchState is PatchUiState.Error) {
            snackbar.showSnackbar((patchState as PatchUiState.Error).message)
            vm.resetPatch()
        }
        if (patchState is PatchUiState.Done) {
            val d = patchState as PatchUiState.Done
            snackbar.showSnackbar("Patched ${d.successCount} file(s)" +
                if (d.failCount > 0) " — ${d.failCount} failed" else "")
            vm.resetPatch()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = ViciousSurface) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            // History overlay takes priority
            when (val h = historyState) {
                is HistoryUiState.Showing -> {
                    HistoryScreen(
                        entries = h.entries,
                        onEntryClick = { vm.viewHistoryEntry(it) },
                        onDeleteEntry = { vm.deleteHistoryEntry(it) },
                        onClearAll = { vm.clearHistory() },
                        onBack = { vm.hideHistory() }
                    )
                    return@Box
                }
                is HistoryUiState.ViewingEntry -> {
                    // Show results screen with history entry's findings (read-only)
                    val fakeReport = ScanReport(
                        scannedFiles = listOf(h.entry.targetPath),
                        findings = h.entry.findings,
                        scanDurationMs = 0L,
                        projectType = h.entry.projectType
                    )
                    ResultsScreen(
                        report = fakeReport,
                        onAutoPatch = {},
                        onReset = { vm.hideHistory() }
                    )
                    return@Box
                }
                else -> {}
            }

            when (val s = scanState) {
                is ScanUiState.Idle -> HomeScreen(
                    onFolderSelected = { vm.scanFolder(it) },
                    onFileSelected = { uri, name -> vm.scanSingleFile(uri, name) },
                    onShowHistory = { vm.showHistory() }
                )
                is ScanUiState.Scanning -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), color = ViciousRed)
                is ScanUiState.Error -> LaunchedEffect(s) {
                    snackbar.showSnackbar(s.message); vm.resetScan()
                }
                is ScanUiState.Results -> when (val p = patchState) {
                    is PatchUiState.Previewing -> PatchPreviewScreen(
                        previews = p.previews,
                        onConfirm = { vm.applyPatches(it) },
                        onCancel = { vm.resetPatch() }
                    )
                    is PatchUiState.Applying -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center), color = ViciousRed)
                    else -> ResultsScreen(
                        report = s.report,
                        onAutoPatch = { vm.buildPatchPreviews() },
                        onReset = { vm.resetScan() }
                    )
                }
            }
        }
    }
}
