package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.preferences.PreferredAbi
import com.example.ui.AtpUiState
import com.example.ui.components.AtpModuleCard
import com.example.ui.components.AtpPill
import com.example.ui.components.AtpSectionTitle

@Composable
fun EnvironmentSettingsScreen(
    uiState: AtpUiState,
    onSetIsolateAppData: (Boolean) -> Unit,
    onSetIsolateExternalStorage: (Boolean) -> Unit,
    onSetVirtualRootPolicy: (Boolean) -> Unit,
    onSetPreferredAbi: (PreferredAbi) -> Unit,
    onOpenGmsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diag = uiState.diagnostics
    val settings = uiState.settings

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("env_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AtpSectionTitle(
                title = stringResource(R.string.env_arch_section),
                subtitle = "Live hardware ABI & Android compatibility diagnostics"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticRow("Android Version", "Android ${diag?.androidVersion ?: "—"} (API ${diag?.sdkInt ?: "—"})")
                    DiagnosticRow("Supported ABIs", diag?.supportedAbis?.joinToString(", ") ?: "Inspecting…")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtpPill(
                            text = if (diag?.supportsArm64 == true) "ARM64 Supported" else "ARM64 Unavailable",
                            containerColor = if (diag?.supportsArm64 == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (diag?.supportsArm64 == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AtpPill(
                            text = if (diag?.supportsArm32 == true) "ARM32 Supported" else "ARM32 Unavailable",
                            containerColor = if (diag?.supportsArm32 == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (diag?.supportsArm32 == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            AtpSectionTitle(
                title = stringResource(R.string.env_isolation_section),
                subtitle = "Per-container sandbox directory policies"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsSwitchRow(
                        title = "Isolate App Data Directories",
                        subtitle = "Store each imported app's data under files/atp_containers/<pkg>/data",
                        checked = settings.isolateAppData,
                        onCheckedChange = onSetIsolateAppData
                    )
                    SettingsSwitchRow(
                        title = "Isolate External Storage Scope",
                        subtitle = "Keep host media and documents separated from virtual containers",
                        checked = settings.isolateExternalStorage,
                        onCheckedChange = onSetIsolateExternalStorage
                    )
                    diag?.containerRootPath?.let { path ->
                        Text(
                            text = "Root: $path",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            AtpSectionTitle(
                title = "Target ABI Preference",
                subtitle = "Select architecture priority when staging split or universal APKs"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    PreferredAbi.entries.forEach { abi ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSetPreferredAbi(abi) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.preferredAbi == abi,
                                onClick = { onSetPreferredAbi(abi) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(abi.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    abi.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            AtpSectionTitle(
                title = stringResource(R.string.env_root_section),
                subtitle = "Transparent root capability verification"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f), MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AtpPill(
                            text = if (diag?.multiAppEngineAvailable == true) "Engine Active" else "Native Root Hook Absent",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        AtpPill(
                            text = if (diag?.suBinaryFoundPath != null) "Host su: ${diag.suBinaryFoundPath}" else "No Host su Binary"
                        )
                    }
                    Text(
                        text = stringResource(R.string.env_root_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingsSwitchRow(
                        title = "Allow Virtual Root Policy Flag",
                        subtitle = "Stores container preference for future MultiApp root hook integration",
                        checked = settings.virtualRootPolicyEnabled,
                        onCheckedChange = onSetVirtualRootPolicy
                    )
                }
            }
        }

        item {
            AtpModuleCard(
                title = stringResource(R.string.gms_title),
                subtitle = "Current mode: ${settings.gmsMode.label}",
                iconRes = R.drawable.ic_cloud,
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = onOpenGmsSettings,
                modifier = Modifier.testTag("env_open_gms_card")
            )
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
