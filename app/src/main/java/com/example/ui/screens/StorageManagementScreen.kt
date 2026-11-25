package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.waxmoon.R
import com.example.data.repository.AppStorageBreakdown
import com.example.ui.AtpUiState
import com.example.ui.components.AtpPill
import com.example.ui.components.AtpSectionTitle
import com.example.ui.components.formatBytes

@Composable
fun StorageManagementScreen(
    uiState: AtpUiState,
    onClearAppCache: (String) -> Unit,
    onWipeAppData: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onClearAllCaches: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = uiState.storageSummary
    val total = summary.grandTotalBytes.coerceAtLeast(1L).toFloat()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("storage_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.large),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Total Container Footprint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatBytes(summary.grandTotalBytes),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Stacked bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (summary.grandTotalBytes > 0L) {
                            val apkWeight = (summary.totalApkBytes / total).coerceAtLeast(0.02f)
                            val dataWeight = (summary.totalDataBytes / total).coerceAtLeast(0.01f)
                            val cacheWeight = (summary.totalCacheBytes / total).coerceAtLeast(0.01f)
                            Box(
                                modifier = Modifier
                                    .weight(apkWeight)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(dataWeight)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(cacheWeight)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AtpPill(
                            text = "APKs: ${formatBytes(summary.totalApkBytes)}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        AtpPill(
                            text = "Data: ${formatBytes(summary.totalDataBytes)}",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        AtpPill(
                            text = "Cache: ${formatBytes(summary.totalCacheBytes)}",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Button(
                        onClick = onClearAllCaches,
                        enabled = summary.perApp.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("storage_clear_all_caches_button")
                    ) {
                        Text(stringResource(R.string.storage_clear_all_caches))
                    }
                }
            }
        }

        item {
            AtpSectionTitle(
                title = "Per-Container Storage",
                subtitle = "Manage staged APKs, isolated app data, and cache directories"
            )
        }

        if (summary.perApp.isEmpty()) {
            item {
                Text(
                    text = "No container storage allocated yet. Import an app to view per-container usage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(summary.perApp, key = { it.packageName }) { item ->
                StorageBreakdownCard(
                    item = item,
                    onClearCache = { onClearAppCache(item.packageName) },
                    onWipeData = { onWipeAppData(item.packageName) },
                    onRemove = { onRemoveApp(item.packageName) }
                )
            }
        }
    }
}

@Composable
private fun StorageBreakdownCard(
    item: AppStorageBreakdown,
    onClearCache: () -> Unit,
    onWipeData: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                AtpPill(text = formatBytes(item.totalBytes))
            }

            Text(
                text = "APKs ${formatBytes(item.apkBytes)} · Data ${formatBytes(item.dataBytes)} · Cache ${formatBytes(item.cacheBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Cache", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onWipeData,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Wipe Data", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
        }
    }
}
