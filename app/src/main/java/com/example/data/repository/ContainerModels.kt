package com.example.data.repository

data class HostAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val baseApkPath: String,
    val splitApkPaths: List<String>,
    val totalSizeBytes: Long,
    val abiTag: String
)

data class AppStorageBreakdown(
    val packageName: String,
    val appName: String,
    val apkBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = apkBytes + dataBytes + cacheBytes
}

data class StorageSummary(
    val totalApkBytes: Long = 0L,
    val totalDataBytes: Long = 0L,
    val totalCacheBytes: Long = 0L,
    val perApp: List<AppStorageBreakdown> = emptyList()
) {
    val grandTotalBytes: Long get() = totalApkBytes + totalDataBytes + totalCacheBytes
}

data class HostDiagnostics(
    val androidVersion: String,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val supportsArm64: Boolean,
    val supportsArm32: Boolean,
    val suBinaryFoundPath: String?,
    val multiAppEngineAvailable: Boolean,
    val containerRootPath: String
)

data class GmsComponentStatus(
    val gmsInstalled: Boolean,
    val gmsVersion: String?,
    val gsfInstalled: Boolean,
    val playStoreInstalled: Boolean,
    val playStoreVersion: String?,
    val hostAccountsCount: Int?,
    val compatibilitySummary: String
)

data class ContainerVerificationReport(
    val packageName: String,
    val baseApkExists: Boolean,
    val splitApkCountFound: Int,
    val expectedSplitCount: Int,
    val dataDirExists: Boolean,
    val cacheDirExists: Boolean,
    val actualDiskBytes: Long
) {
    val isHealthy: Boolean
        get() = baseApkExists && splitApkCountFound >= expectedSplitCount && dataDirExists && cacheDirExists
}

sealed interface ImportResult {
    data class Success(val appName: String, val packageName: String, val splitCount: Int) : ImportResult
    data class Failure(val reason: String) : ImportResult
}
