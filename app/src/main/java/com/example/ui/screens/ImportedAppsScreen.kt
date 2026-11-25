package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.VirtualAppEntity
import com.example.data.repository.ContainerVerificationReport
import com.example.ui.AtpUiState
import com.example.ui.components.AppPackageIcon
import com.example.ui.components.AtpPill
import com.example.ui.components.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportedAppsScreen(
    uiState: AtpUiState,
    loadIcon: suspend (String, String?) -> Bitmap?,
    getHostLaunchIntent: (String) -> Intent?,
    onOpenImporter: () -> Unit,
    onVerifyContainer: (VirtualAppEntity) -> Unit,
    onClearVerification: () -> Unit,
    onWipeData: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onTogglePerAppRoot: (String, Boolean) -> Unit,
    onTogglePerAppGms: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf<String?>(null) }

    val filteredApps = remember(uiState.importedApps, searchQuery) {
        if (searchQuery.isBlank()) uiState.importedApps
        else {
            val q = searchQuery.trim().lowercase()
            uiState.importedApps.filter {
                it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    val selectedApp = remember(uiState.importedApps, selectedPackage) {
        selectedPackage?.let { pkg -> uiState.importedApps.firstOrNull { it.packageName == pkg } }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("imported_apps_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenImporter,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("imported_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.home_import_cta))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.imported_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("imported_search_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredApps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_container),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.imported_empty_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.imported_empty_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onOpenImporter,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(stringResource(R.string.home_import_cta))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        ImportedAppCard(
                            app = app,
                            loadIcon = loadIcon,
                            onClick = {
                                onClearVerification()
                                selectedPackage = app.packageName
                            }
                        )
                    }
                }
            }
        }
    }

    if (selectedApp != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        val hostLaunchIntent = remember(selectedApp.packageName) {
            getHostLaunchIntent(selectedApp.packageName)
        }
        val verification = uiState.verificationReport?.takeIf { it.packageName == selectedApp.packageName }

        ModalBottomSheet(
            onDismissRequest = {
                selectedPackage = null
                onClearVerification()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppPackageIcon(
                        packageName = selectedApp.packageName,
                        appName = selectedApp.appName,
                        containerDir = selectedApp.containerDir,
                        loadBitmap = loadIcon,
                        size = 52.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedApp.appName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${selectedApp.packageName} · v${selectedApp.versionName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AtpPill(text = selectedApp.abiTag)
                    AtpPill(text = formatBytes(selectedApp.apkSizeBytes))
                    if (selectedApp.splitCount > 0) {
                        AtpPill(
                            text = "Base + ${selectedApp.splitCount} splits",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Container Path",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedApp.containerDir,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Runtime Status: Staged on disk. Isolated process virtualization requires the MultiApp native binder runtime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                // Per-container preference switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Request Virtual Root", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Requires MultiApp native root hook (inactive in template)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = selectedApp.virtualRootRequested,
                        onCheckedChange = { onTogglePerAppRoot(selectedApp.packageName, it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Optional GMS Bridge", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Enable Google Services bridge metadata for this container",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = selectedApp.gmsBridgeRequested,
                        onCheckedChange = { onTogglePerAppGms(selectedApp.packageName, it) }
                    )
                }

                if (verification != null) {
                    VerificationReportCard(verification)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onVerifyContainer(selectedApp) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sheet_verify_button")
                    ) {
                        Text(stringResource(R.string.imported_verify_btn))
                    }
                    if (hostLaunchIntent != null) {
                        OutlinedButton(
                            onClick = { context.startActivity(hostLaunchIntent) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(stringResource(R.string.imported_open_host_btn))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onWipeData(selectedApp.packageName) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(stringResource(R.string.imported_wipe_data_btn))
                    }
                    OutlinedButton(
                        onClick = {
                            onRemoveApp(selectedApp.packageName)
                            selectedPackage = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Remove",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationReportCard(report: ContainerVerificationReport) {
    val color = if (report.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), MaterialTheme.shapes.small)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (report.isHealthy) "Container Integrity Verified" else "Container Files Incomplete",
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = "Base APK: ${if (report.baseApkExists) "Present" else "Missing"} · Splits: ${report.splitApkCountFound}/${report.expectedSplitCount} · Disk: ${formatBytes(report.actualDiskBytes)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ImportedAppCard(
    app: VirtualAppEntity,
    loadIcon: suspend (String, String?) -> Bitmap?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
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
                containerDir = app.containerDir,
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
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AtpPill(text = app.abiTag)
                    AtpPill(text = formatBytes(app.apkSizeBytes))
                    if (app.splitCount > 0) {
                        AtpPill(
                            text = "+${app.splitCount} splits",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
