package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.VirtualAppEntity
import com.example.data.preferences.GmsOptionMode
import com.example.ui.AtpUiState
import com.example.ui.components.AppPackageIcon
import com.example.ui.components.AtpMetricTile
import com.example.ui.components.AtpModuleCard
import com.example.ui.components.AtpPill
import com.example.ui.components.AtpSectionTitle
import com.example.ui.components.formatBytes

@Composable
fun HomeScreen(
    uiState: AtpUiState,
    loadIcon: suspend (String, String?) -> Bitmap?,
    onOpenImporter: () -> Unit,
    onOpenImportedApps: () -> Unit,
    onOpenEnvSettings: () -> Unit,
    onOpenGmsSettings: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = primaryColor.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.large
                    ),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryColor.copy(alpha = 0.18f),
                                        secondaryColor.copy(alpha = 0.08f),
                                        primaryColor.copy(alpha = 0.02f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(primaryColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_container),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.app_full_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${stringResource(R.string.developer_brand)} · com.atp.vm",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = primaryColor
                                )
                            }
                        }

                        Text(
                            text = "Lightweight Android container workspace for APK and split-APK staging, isolated storage management, and optional Google Services diagnostics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onOpenImporter,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_import_button"),
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = stringResource(R.string.home_import_cta))
                            }

                            OutlinedButton(
                                onClick = onOpenAbout,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_about_button"),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About ATP",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("About")
                            }
                        }
                    }
                }
            }
        }

        // Quick Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AtpMetricTile(
                    label = stringResource(R.string.home_imported_count_label),
                    value = uiState.importedApps.size.toString(),
                    accentColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                AtpMetricTile(
                    label = stringResource(R.string.home_storage_used_label),
                    value = formatBytes(uiState.storageSummary.grandTotalBytes),
                    accentColor = secondaryColor,
                    modifier = Modifier.weight(1f)
                )
                AtpMetricTile(
                    label = stringResource(R.string.home_gms_status_label),
                    value = if (uiState.settings.gmsMode == GmsOptionMode.DISABLED) "Off" else "Check",
                    accentColor = if (uiState.settings.gmsMode == GmsOptionMode.DISABLED) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        tertiaryColor
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Honest Runtime Engine Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = tertiaryColor.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.medium
                    ),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AtpPill(
                            text = "STAGING ACTIVE",
                            containerColor = primaryColor.copy(alpha = 0.2f),
                            contentColor = primaryColor
                        )
                        uiState.diagnostics?.let { diag ->
                            AtpPill(
                                text = diag.supportedAbis.firstOrNull() ?: "ARM",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.home_engine_status_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.home_engine_status_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Recent Imported Containers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtpSectionTitle(
                    title = stringResource(R.string.home_recent_containers),
                    modifier = Modifier.weight(1f)
                )
                if (uiState.importedApps.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOpenImportedApps,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("View All")
                    }
                }
            }
        }

        if (uiState.importedApps.isEmpty()) {
            item {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import_apk),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.home_empty_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.importedApps.take(4), key = { it.packageName }) { app ->
                HomeImportedAppRow(
                    app = app,
                    loadIcon = loadIcon,
                    onClick = onOpenImportedApps
                )
            }
        }

        // Environment Modules Navigation
        item {
            Spacer(modifier = Modifier.height(4.dp))
            AtpSectionTitle(
                title = stringResource(R.string.home_quick_actions),
                subtitle = "Manage container isolation, optional GMS, storage, and licenses"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AtpModuleCard(
                    title = stringResource(R.string.imported_title),
                    subtitle = "${uiState.importedApps.size} staged container(s) · Inspect APKs & data",
                    iconRes = R.drawable.ic_container,
                    accentColor = primaryColor,
                    onClick = onOpenImportedApps,
                    modifier = Modifier.testTag("module_imported_apps")
                )
                AtpModuleCard(
                    title = stringResource(R.string.env_title),
                    subtitle = "ABI support, container isolation & virtual root diagnostics",
                    iconRes = R.drawable.ic_terminal,
                    accentColor = secondaryColor,
                    onClick = onOpenEnvSettings,
                    modifier = Modifier.testTag("module_env_settings")
                )
                AtpModuleCard(
                    title = stringResource(R.string.gms_title),
                    subtitle = "Optional Google Services (${uiState.settings.gmsMode.label})",
                    iconRes = R.drawable.ic_cloud,
                    accentColor = tertiaryColor,
                    onClick = onOpenGmsSettings,
                    modifier = Modifier.testTag("module_gms_settings")
                )
                AtpModuleCard(
                    title = stringResource(R.string.storage_title),
                    subtitle = "${formatBytes(uiState.storageSummary.grandTotalBytes)} used across APKs, data & caches",
                    iconRes = R.drawable.ic_storage,
                    accentColor = primaryColor,
                    onClick = onOpenStorage,
                    modifier = Modifier.testTag("module_storage")
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HomeImportedAppRow(
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
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
