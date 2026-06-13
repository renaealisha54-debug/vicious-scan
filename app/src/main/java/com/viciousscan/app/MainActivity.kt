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
import com.viciousscan.app.ui.screens.HomeScreen
import com.viciousscan.app.ui.screens.PatchPreviewScreen
import com.viciousscan.app.ui.screens.ResultsScreen
import com.viciousscan.app.ui.theme.ViciousRed
import com.viciousscan.app.ui.theme.ViciousScanTheme
import com.viciousscan.app.ui.theme.ViciousSurface
import com.viciousscan.app.viewmodel.PatchUiState
import com.viciousscan.app.viewmodel.ScanUiState
import com.viciousscan.app.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViciousScanTheme {
                ViciousScanApp()
            }
        }
    }
}

@Composable
fun ViciousScanApp(vm: ScanViewModel = viewModel()) {
    val scanState  by vm.scanState.collectAsStateWithLifecycle()
    val patchState by vm.patchState.collectAsStateWithLifecycle()
    val snackbar   = remember { SnackbarHostState() }
    val scope      = rememberCoroutineScope()

    // Show error snackbars
    LaunchedEffect(patchState) {
        if (patchState is PatchUiState.Error) {
            snackbar.showSnackbar((patchState as PatchUiState.Error).message)
            vm.resetPatch()
        }
        if (patchState is PatchUiState.Done) {
            val d = patchState as PatchUiState.Done
            snackbar.showSnackbar(
                "Patched ${d.successCount} file(s)" +
                if (d.failCount > 0) " — ${d.failCount} failed (check write permissions)" else ""
            )
            vm.resetPatch()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = ViciousSurface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = scanState) {
                is ScanUiState.Idle -> {
                    HomeScreen(
                        onFolderSelected = { vm.scanFolder(it) },
                        onFileSelected   = { uri, name -> vm.scanSingleFile(uri, name) }
                    )
                }

                is ScanUiState.Scanning -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ViciousRed
                    )
                }

                is ScanUiState.Error -> {
                    LaunchedEffect(s) {
                        snackbar.showSnackbar(s.message)
                        vm.resetScan()
                    }
                }

                is ScanUiState.Results -> {
                    when (val p = patchState) {
                        is PatchUiState.Previewing -> {
                            PatchPreviewScreen(
                                previews  = p.previews,
                                onConfirm = { vm.applyPatches(it) },
                                onCancel  = { vm.resetPatch() }
                            )
                        }

                        is PatchUiState.Applying -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = ViciousRed
                            )
                        }

                        else -> {
                            ResultsScreen(
                                report      = s.report,
                                onAutoPatch = { vm.buildPatchPreviews() },
                                onReset     = { vm.resetScan() }
                            )
                        }
                    }
                }
            }
        }
    }
}
