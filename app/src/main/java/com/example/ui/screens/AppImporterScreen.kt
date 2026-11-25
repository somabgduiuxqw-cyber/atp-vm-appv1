package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.HostAppInfo
import com.example.ui.AtpUiState
import com.example.ui.components.AppPackageIcon
import com.example.ui.components.AtpPill
import com.example.ui.components.formatBytes

@Composable
fun AppImporterScreen(
    uiState: AtpUiState,
    loadIcon: suspend (String, String?) -> Bitmap?,
    onScanHostApps: () -> Unit,
    onImportHostApp: (HostAppInfo) -> Unit,
    onImportExternalApk: (android.net.Uri) -> Unit,
    onToggleSystemApps: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportExternalApk(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.hostApps.isEmpty() && !uiState.isScanningHostApps) {
            onScanHostApps()
        }
    }

    val importedPackages = remember(uiState.importedApps) {
        uiState.importedApps.map { it.packageName }.toSet()
    }

    val filteredHostApps = remember(
        uiState.hostApps,
        searchQuery,
        uiState.settings.includeSystemAppsInScanner
    ) {
        val q = searchQuery.trim().lowercase()
        uiState.hostApps.filter { app ->
            val systemMatch = uiState.settings.includeSystemAppsInScanner || !app.isSystemApp
            val queryMatch = q.isEmpty() ||
                app.appName.lowercase().contains(q) ||
                app.packageName.lowercase().contains(q)
            systemMatch && queryMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("app_importer_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.importer_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("importer_search_input")
            )
            IconButton(
                onClick = onScanHostApps,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rescan installed apps")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = uiState.settings.includeSystemAppsInScanner,
                onClick = { onToggleSystemApps(!uiState.settings.includeSystemAppsInScanner) },
                label = { Text(stringResource(R.string.importer_show_system)) }
            )

            OutlinedButton(
                onClick = {
                    apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("importer_pick_apk_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_import_apk),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.importer_pick_apk))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isScanningHostApps) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.importer_scanning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredHostApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.importer_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredHostApps, key = { it.packageName }) { app ->
                    val isImported = importedPackages.contains(app.packageName)
                    val isImporting = uiState.importingPackage == app.packageName
                    HostAppImporterRow(
                        app = app,
                        isImported = isImported,
                        isImporting = isImporting,
                        loadIcon = loadIcon,
                        onImport = { onImportHostApp(app) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HostAppImporterRow(
    app: HostAppInfo,
    isImported: Boolean,
    isImporting: Boolean,
    loadIcon: suspend (String, String?) -> Bitmap?,
    onImport: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppPackageIcon(
                packageName = app.packageName,
                appName = app.appName,
                containerDir = null,
                loadBitmap = loadIcon
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${app.packageName} · v${app.versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtpPill(text = formatBytes(app.totalSizeBytes))
                    AtpPill(text = app.abiTag)
                    if (app.splitApkPaths.isNotEmpty()) {
                        AtpPill(
                            text = "Split x${app.splitApkPaths.size}",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isImporting) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
            } else if (isImported) {
                OutlinedButton(
                    onClick = onImport,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.importer_imported_badge))
                }
            } else {
                Button(
                    onClick = onImport,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.importer_import_btn))
                }
            }
        }
    }
}
