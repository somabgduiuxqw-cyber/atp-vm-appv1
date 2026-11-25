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
import com.example.data.local.VirtualAppEntity
import com.example.data.preferences.GmsOptionMode
import com.example.ui.AtpUiState
import com.example.ui.components.AppPackageIcon
import com.example.ui.components.AtpMetricTile
import com.example.ui.components.AtpModuleCard
import com.example.ui.components.AtpPill
import com.example.ui.components.AtpSectionTitle
import com.example.ui.components.formatBytes
import org.waxmoon.R

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
    val silverPrimary = MaterialTheme.colorScheme.primary
    val lightSilver = MaterialTheme.colorScheme.secondary
    val deepNavy = MaterialTheme.colorScheme.surface
    val darkBlue = MaterialTheme.colorScheme.background

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Metallic Silver & Dark Blue Hero Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = silverPrimary.copy(alpha = 0.45f),
                        shape = MaterialTheme.shapes.large
                    ),
                shape = MaterialTheme.shapes.large,
                color = deepNavy
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        silverPrimary.copy(alpha = 0.16f),
                                        deepNavy.copy(alpha = 0.92f),
                                        darkBlue.copy(alpha = 0.98f)
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
                                    .background(darkBlue)
                                    .border(1.dp, silverPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_container),
                                    contentDescription = null,
                                    tint = lightSilver,
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
                                    text = "${stringResource(R.string.developer_brand)} · Space ${uiState.userSpace}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = silverPrimary
                                )
                            }
                        }

                        Text(
                            text = "MultiApp-powered container workspace for APK & split-APK isolation, multi-space management, and optional Google Services.",
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
                                    containerColor = silverPrimary,
                                    contentColor = darkBlue
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
                                    tint = lightSilver,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("About", color = lightSilver)
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
                    accentColor = lightSilver,
                    modifier = Modifier.weight(1f)
                )
                AtpMetricTile(
                    label = stringResource(R.string.home_storage_used_label),
                    value = formatBytes(uiState.storageSummary.grandTotalBytes),
                    accentColor = silverPrimary,
                    modifier = Modifier.weight(1f)
                )
                AtpMetricTile(
                    label = stringResource(R.string.home_gms_status_label),
                    value = if (uiState.settings.gmsMode == GmsOptionMode.DISABLED) "Off" else "Ready",
                    accentColor = lightSilver,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Engine Status Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                color = deepNavy,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AtpPill(
                            text = if (uiState.diagnostics?.multiAppEngineAvailable == true) {
                                "HACKAPI NATIVE ACTIVE"
                            } else {
                                "CONTAINER STAGING ACTIVE"
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = lightSilver
                        )
                        uiState.diagnostics?.let { diag ->
                            AtpPill(text = diag.supportedAbis.firstOrNull() ?: "ARM")
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

        // Recent Staged Containers
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
                        Text("View All", color = lightSilver)
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
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.medium
                        ),
                    color = deepNavy,
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
                            tint = silverPrimary,
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

        // Environment Modules
        item {
            Spacer(modifier = Modifier.height(4.dp))
            AtpSectionTitle(
                title = stringResource(R.string.home_quick_actions),
                subtitle = "Manage containers, virtual spaces, Google Services, and storage"
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AtpModuleCard(
                    title = stringResource(R.string.imported_title),
                    subtitle = "${uiState.importedApps.size} staged container(s) · Space ${uiState.userSpace}",
                    iconRes = R.drawable.ic_container,
                    accentColor = lightSilver,
                    onClick = onOpenImportedApps,
                    modifier = Modifier.testTag("module_imported_apps")
                )
                AtpModuleCard(
                    title = stringResource(R.string.env_title),
                    subtitle = "ABI architecture, container isolation & virtual root probe",
                    iconRes = R.drawable.ic_terminal,
                    accentColor = silverPrimary,
                    onClick = onOpenEnvSettings,
                    modifier = Modifier.testTag("module_env_settings")
                )
                AtpModuleCard(
                    title = stringResource(R.string.gms_title),
                    subtitle = "Optional Google Services (${uiState.settings.gmsMode.label})",
                    iconRes = R.drawable.ic_cloud,
                    accentColor = lightSilver,
                    onClick = onOpenGmsSettings,
                    modifier = Modifier.testTag("module_gms_settings")
                )
                AtpModuleCard(
                    title = stringResource(R.string.storage_title),
                    subtitle = "${formatBytes(uiState.storageSummary.grandTotalBytes)} across APKs, data & caches",
                    iconRes = R.drawable.ic_storage,
                    accentColor = silverPrimary,
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
                color = MaterialTheme.colorScheme.outlineVariant,
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
                    AtpPill(text = "+${app.splitCount} splits")
                }
            }
        }
    }
}
