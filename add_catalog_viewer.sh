#!/bin/bash
set -e
cd ~/vicious-scan

# ── 1. PermissionCatalogScreen.kt ─────────────────────────────────────────────
cat > app/src/main/java/com/viciousscan/app/ui/screens/PermissionCatalogScreen.kt << 'EOF'
package com.viciousscan.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viciousscan.app.ui.theme.*

data class CatalogEntry(
    val name: String,
    val snippet: String,
    val category: String,
    val type: CatalogType
)

enum class CatalogType { PERMISSION, DEPENDENCY }

val PERMISSION_CATALOG = listOf(
    // Location
    CatalogEntry("ACCESS_FINE_LOCATION", "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />", "Location", CatalogType.PERMISSION),
    CatalogEntry("ACCESS_COARSE_LOCATION", "<uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />", "Location", CatalogType.PERMISSION),
    CatalogEntry("ACCESS_BACKGROUND_LOCATION", "<uses-permission android:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\" />", "Location", CatalogType.PERMISSION),
    // Camera
    CatalogEntry("CAMERA", "<uses-permission android:name=\"android.permission.CAMERA\" />", "Camera", CatalogType.PERMISSION),
    // Network
    CatalogEntry("INTERNET", "<uses-permission android:name=\"android.permission.INTERNET\" />", "Network", CatalogType.PERMISSION),
    CatalogEntry("ACCESS_NETWORK_STATE", "<uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />", "Network", CatalogType.PERMISSION),
    CatalogEntry("ACCESS_WIFI_STATE", "<uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />", "Network", CatalogType.PERMISSION),
    CatalogEntry("CHANGE_WIFI_STATE", "<uses-permission android:name=\"android.permission.CHANGE_WIFI_STATE\" />", "Network", CatalogType.PERMISSION),
    // Storage
    CatalogEntry("READ_EXTERNAL_STORAGE", "<uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" android:maxSdkVersion=\"32\" />", "Storage", CatalogType.PERMISSION),
    CatalogEntry("WRITE_EXTERNAL_STORAGE", "<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:maxSdkVersion=\"28\" />", "Storage", CatalogType.PERMISSION),
    CatalogEntry("READ_MEDIA_IMAGES", "<uses-permission android:name=\"android.permission.READ_MEDIA_IMAGES\" />", "Storage", CatalogType.PERMISSION),
    CatalogEntry("READ_MEDIA_VIDEO", "<uses-permission android:name=\"android.permission.READ_MEDIA_VIDEO\" />", "Storage", CatalogType.PERMISSION),
    CatalogEntry("READ_MEDIA_AUDIO", "<uses-permission android:name=\"android.permission.READ_MEDIA_AUDIO\" />", "Storage", CatalogType.PERMISSION),
    // Bluetooth
    CatalogEntry("BLUETOOTH_CONNECT", "<uses-permission android:name=\"android.permission.BLUETOOTH_CONNECT\" />", "Bluetooth", CatalogType.PERMISSION),
    CatalogEntry("BLUETOOTH_SCAN", "<uses-permission android:name=\"android.permission.BLUETOOTH_SCAN\" />", "Bluetooth", CatalogType.PERMISSION),
    CatalogEntry("BLUETOOTH_ADVERTISE", "<uses-permission android:name=\"android.permission.BLUETOOTH_ADVERTISE\" />", "Bluetooth", CatalogType.PERMISSION),
    // Notifications
    CatalogEntry("POST_NOTIFICATIONS", "<uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />", "Notifications", CatalogType.PERMISSION),
    CatalogEntry("VIBRATE", "<uses-permission android:name=\"android.permission.VIBRATE\" />", "Notifications", CatalogType.PERMISSION),
    // Microphone
    CatalogEntry("RECORD_AUDIO", "<uses-permission android:name=\"android.permission.RECORD_AUDIO\" />", "Microphone", CatalogType.PERMISSION),
    // Contacts
    CatalogEntry("READ_CONTACTS", "<uses-permission android:name=\"android.permission.READ_CONTACTS\" />", "Contacts", CatalogType.PERMISSION),
    CatalogEntry("WRITE_CONTACTS", "<uses-permission android:name=\"android.permission.WRITE_CONTACTS\" />", "Contacts", CatalogType.PERMISSION),
    // Phone
    CatalogEntry("READ_PHONE_STATE", "<uses-permission android:name=\"android.permission.READ_PHONE_STATE\" />", "Phone", CatalogType.PERMISSION),
    CatalogEntry("CALL_PHONE", "<uses-permission android:name=\"android.permission.CALL_PHONE\" />", "Phone", CatalogType.PERMISSION),
    // Accessibility
    CatalogEntry("BIND_ACCESSIBILITY_SERVICE", "<uses-permission android:name=\"android.permission.BIND_ACCESSIBILITY_SERVICE\" />", "Accessibility", CatalogType.PERMISSION),
    CatalogEntry("FOREGROUND_SERVICE", "<uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />", "Services", CatalogType.PERMISSION),
    CatalogEntry("RECEIVE_BOOT_COMPLETED", "<uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />", "Services", CatalogType.PERMISSION),
    CatalogEntry("WAKE_LOCK", "<uses-permission android:name=\"android.permission.WAKE_LOCK\" />", "Services", CatalogType.PERMISSION),
    // Dependencies
    CatalogEntry("navigation-compose", "implementation(\"androidx.navigation:navigation-compose:2.7.7\")", "Navigation", CatalogType.DEPENDENCY),
    CatalogEntry("lifecycle-viewmodel-compose", "implementation(\"androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4\")", "Lifecycle", CatalogType.DEPENDENCY),
    CatalogEntry("lifecycle-runtime-compose", "implementation(\"androidx.lifecycle:lifecycle-runtime-compose:2.8.4\")", "Lifecycle", CatalogType.DEPENDENCY),
    CatalogEntry("room-runtime + room-ktx", "implementation(\"androidx.room:room-runtime:2.6.1\")\nimplementation(\"androidx.room:room-ktx:2.6.1\")\nksp(\"androidx.room:room-compiler:2.6.1\")", "Database", CatalogType.DEPENDENCY),
    CatalogEntry("datastore-preferences", "implementation(\"androidx.datastore:datastore-preferences:1.1.1\")", "Storage", CatalogType.DEPENDENCY),
    CatalogEntry("hilt-android", "implementation(\"com.google.dagger:hilt-android:2.51.1\")\nksp(\"com.google.dagger:hilt-compiler:2.51.1\")", "DI", CatalogType.DEPENDENCY),
    CatalogEntry("coil-compose", "implementation(\"io.coil-kt:coil-compose:2.6.0\")", "Images", CatalogType.DEPENDENCY),
    CatalogEntry("retrofit", "implementation(\"com.squareup.retrofit2:retrofit:2.11.0\")\nimplementation(\"com.squareup.retrofit2:converter-gson:2.11.0\")", "Network", CatalogType.DEPENDENCY),
    CatalogEntry("okhttp", "implementation(\"com.squareup.okhttp3:okhttp:4.12.0\")", "Network", CatalogType.DEPENDENCY),
    CatalogEntry("kotlinx-coroutines-android", "implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0\")", "Async", CatalogType.DEPENDENCY),
    CatalogEntry("work-runtime-ktx", "implementation(\"androidx.work:work-runtime-ktx:2.9.0\")", "Background", CatalogType.DEPENDENCY),
    CatalogEntry("gson", "implementation(\"com.google.code.gson:gson:2.10.1\")", "Serialization", CatalogType.DEPENDENCY),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCatalogScreen(
    onInject: (List<CatalogEntry>, Uri, String) -> Unit,
    onBack: () -> Unit
) {
    val selected = remember { mutableStateListOf<CatalogEntry>() }
    var targetUri by remember { mutableStateOf<Uri?>(null) }
    var targetName by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<CatalogType?>(null) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            targetUri = uri
            targetName = uri.lastPathSegment?.substringAfterLast('/') ?: "selected file"
        }
    }

    val grouped = PERMISSION_CATALOG
        .filter { filterType == null || it.type == filterType }
        .groupBy { it.category }

    Scaffold(
        containerColor = ViciousSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text("INJECT CATALOG", fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, color = ViciousRed)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = ViciousRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ViciousSurface)
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(color = ViciousCard) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (targetUri != null) {
                            Text("Target: $targetName", fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp, color = ViciousMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { fileLauncher.launch(arrayOf("text/xml", "text/plain", "*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (targetUri == null) "PICK FILE" else "CHANGE FILE",
                                    fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { targetUri?.let { onInject(selected.toList(), it, targetName) } },
                                enabled = targetUri != null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ViciousRed)
                            ) {
                                Text("INJECT ${selected.size}",
                                    fontFamily = FontFamily.Monospace, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filterType == null, { filterType = null }, "ALL")
                    FilterChip(filterType == CatalogType.PERMISSION, { filterType = CatalogType.PERMISSION }, "PERMISSIONS")
                    FilterChip(filterType == CatalogType.DEPENDENCY, { filterType = CatalogType.DEPENDENCY }, "DEPS")
                }
                Spacer(Modifier.height(8.dp))
            }
            grouped.forEach { (category, entries) ->
                item {
                    Text(category.uppercase(), fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = ViciousRed, letterSpacing = 3.sp,
                        modifier = Modifier.padding(vertical = 6.dp))
                }
                items(entries) { entry ->
                    val isSelected = entry in selected
                    Card(
                        onClick = { if (isSelected) selected.remove(entry) else selected.add(entry) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ViciousRed.copy(alpha = 0.15f) else ViciousCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                null, tint = if (isSelected) ViciousRed else ViciousMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                    color = ViciousOnSurface)
                                Text(entry.type.name, fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp, color = ViciousMuted, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ViciousRed else ViciousCard
    ) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontFamily = FontFamily.Monospace, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
            color = if (selected) androidx.compose.ui.graphics.Color.White else ViciousMuted)
    }
}
EOF

echo "PermissionCatalogScreen.kt written"

# ── 2. Add catalog state to ScanViewModel ─────────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/viewmodel/ScanViewModel.kt').read()

if 'CatalogUiState' not in content:
    content = content.replace(
        'sealed class ExportUiState {',
        '''sealed class CatalogUiState {
    object Hidden : CatalogUiState()
    object Showing : CatalogUiState()
}

sealed class ExportUiState {'''
    )

    content = content.replace(
        '    private val _exportState',
        '''    private val _catalogState = MutableStateFlow<CatalogUiState>(CatalogUiState.Hidden)
    val catalogState: StateFlow<CatalogUiState> = _catalogState

    private val _exportState'''
    )

    content = content.replace(
        '    fun showProjectInfo()',
        '''    fun showCatalog() { _catalogState.value = CatalogUiState.Showing }
    fun hideCatalog() { _catalogState.value = CatalogUiState.Hidden }

    fun showProjectInfo()'''
    )

open('app/src/main/java/com/viciousscan/app/viewmodel/ScanViewModel.kt', 'w').write(content)
print("ScanViewModel.kt updated")
EOF

# ── 3. Wire catalog into MainActivity ─────────────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/MainActivity.kt').read()

if 'catalogState' not in content:
    # Add import
    content = content.replace(
        'import com.viciousscan.app.viewmodel.HistoryUiState',
        'import com.viciousscan.app.viewmodel.CatalogUiState\nimport com.viciousscan.app.viewmodel.HistoryUiState'
    )
    content = content.replace(
        'import com.viciousscan.app.ui.screens.ProjectInfoScreen',
        'import com.viciousscan.app.ui.screens.PermissionCatalogScreen\nimport com.viciousscan.app.ui.screens.ProjectInfoScreen'
    )

    # Add state observation
    content = content.replace(
        '    val exportState      by vm.exportState.collectAsStateWithLifecycle()',
        '    val catalogState     by vm.catalogState.collectAsStateWithLifecycle()\n    val exportState      by vm.exportState.collectAsStateWithLifecycle()'
    )

    # Add catalog screen routing before history
    content = content.replace(
        '            // Project info overlay',
        '''            // Catalog overlay
            if (catalogState is CatalogUiState.Showing) {
                PermissionCatalogScreen(
                    onInject = { entries, uri, name ->
                        // TODO: wire injection
                        vm.hideCatalog()
                    },
                    onBack = { vm.hideCatalog() }
                )
                return@Box
            }

            // Project info overlay'''
    )

    # Add onShowCatalog to HomeScreen call
    content = content.replace(
        '                    onShowHistory = { vm.showHistory() }',
        '                    onShowHistory = { vm.showHistory() },\n                    onShowCatalog = { vm.showCatalog() }'
    )

open('app/src/main/java/com/viciousscan/app/MainActivity.kt', 'w').write(content)
print("MainActivity.kt updated")
EOF

# ── 4. Add onShowCatalog to HomeScreen ────────────────────────────────────────
python3 << 'EOF'
content = open('app/src/main/java/com/viciousscan/app/ui/screens/HomeScreen.kt').read()

if 'onShowCatalog' not in content:
    # Add import for grid icon
    content = content.replace(
        'import androidx.compose.material.icons.filled.History',
        'import androidx.compose.material.icons.filled.History\nimport androidx.compose.material.icons.filled.LibraryAdd'
    )

    # Add parameter
    content = content.replace(
        '    onShowHistory: () -> Unit',
        '    onShowHistory: () -> Unit,\n    onShowCatalog: () -> Unit'
    )

    # Add button after history button spacer
    content = content.replace(
        '        Spacer(modifier = Modifier.height(48.dp))\n\n        Text(',
        '''        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onShowCatalog,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ViciousRed)
        ) {
            Icon(Icons.Default.LibraryAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "BROWSE & INJECT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text('''
    )

open('app/src/main/java/com/viciousscan/app/ui/screens/HomeScreen.kt', 'w').write(content)
print("HomeScreen.kt updated")
EOF

echo "ALL DONE"
