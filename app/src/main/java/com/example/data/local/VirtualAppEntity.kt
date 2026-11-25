package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_apps")
data class VirtualAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val abiTag: String,
    val splitCount: Int,
    val apkSizeBytes: Long,
    val containerDir: String,
    val dataDir: String,
    val cacheDir: String,
    val importedAt: Long,
    val virtualRootRequested: Boolean = false,
    val gmsBridgeRequested: Boolean = false,
    val sourceType: String = "HOST_PACKAGE"
)
