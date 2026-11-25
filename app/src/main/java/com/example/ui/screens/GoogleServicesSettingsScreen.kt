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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.data.preferences.GmsOptionMode
import com.example.ui.AtpUiState
import com.example.ui.components.AtpPill
import com.example.ui.components.AtpSectionTitle
import org.waxmoon.R

@Composable
fun GoogleServicesSettingsScreen(
    uiState: AtpUiState,
    onSetGmsMode: (GmsOptionMode) -> Unit,
    onRefreshGmsCheck: () -> Unit,
    onStageHostGms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gms = uiState.gmsStatus
    val selectedMode = uiState.settings.gmsMode

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("gms_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AtpPill(text = "OPTIONAL · ZERO PROPRIETARY BLOBS BUNDLED")
                    Text(
                        text = stringResource(R.string.gms_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.gms_optional_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AtpSectionTitle(
                title = "Google Services Mode",
                subtitle = "Keep disabled for minimal footprint and zero background overhead"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    GmsOptionMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onSetGmsMode(mode) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = { onSetGmsMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(mode.label, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    mode.summary,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtpSectionTitle(
                    title = "Host GMS Detection",
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onRefreshGmsCheck,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Recheck")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GmsStatusLine(
                        "Google Play Services (GMS)",
                        if (gms?.gmsInstalled == true) "Installed (${gms.gmsVersion ?: "unknown"})" else "Not Detected"
                    )
                    GmsStatusLine(
                        "Services Framework (GSF)",
                        if (gms?.gsfInstalled == true) "Installed" else "Not Detected"
                    )
                    GmsStatusLine(
                        "Google Play Store",
                        if (gms?.playStoreInstalled == true) "Installed (${gms.playStoreVersion ?: "unknown"})" else "Not Detected"
                    )
                    GmsStatusLine(
                        "Host Google Accounts",
                        gms?.hostAccountsCount?.toString() ?: "Unavailable"
                    )
                    Text(
                        text = gms?.compatibilitySummary ?: "Checking host packages…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (gms?.gmsInstalled == true) {
                        Button(
                            onClick = onStageHostGms,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("gms_stage_host_button")
                        ) {
                            Text("Stage Host GMS into Space ${uiState.userSpace}")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Compatibility & Licensing Notice",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• ATP Virtual Environment never bundles unauthorized Google proprietary APKs.\n• Staging host GMS packages uses MultiApp HackApi.installPackageFromHost.\n• In-container Google Sign-In requires the native WaxMoon/opensdk Binder proxy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GmsStatusLine(label: String, status: String) {
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
            text = status,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
