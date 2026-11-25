package com.example.data.repository

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.example.data.local.VirtualAppDao
import com.example.data.local.VirtualAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class AtpContainerRepository(
    private val context: Context,
    private val dao: VirtualAppDao
) {
    private val packageManager: PackageManager = context.packageManager

    val containerRoot: File
        get() = File(context.filesDir, "atp_containers").apply { if (!exists()) mkdirs() }

    fun observeImportedApps(): Flow<List<VirtualAppEntity>> = dao.observeAllApps()

    suspend fun scanInstalledHostApps(): List<HostAppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }

        val seenPackages = HashSet<String>()
        val result = ArrayList<HostAppInfo>(resolved.size)

        for (resolveInfo in resolved) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val appInfo = activityInfo.applicationInfo ?: continue
            val pkg = appInfo.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue

            val pkgInfo = getPackageInfoCompat(pkg)
            val versionName = pkgInfo?.versionName ?: "1.0"
            val versionCode = pkgInfo?.let { extractVersionCode(it) } ?: 1L
            val label = resolveInfo.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
                ?: appInfo.loadLabel(packageManager)?.toString()
                ?: pkg
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            val basePath = appInfo.sourceDir.orEmpty()
            val splits = appInfo.splitSourceDirs?.toList().orEmpty()
            val baseSize = if (basePath.isNotEmpty()) File(basePath).takeIf { it.exists() }?.length() ?: 0L else 0L
            val splitSize = splits.sumOf { path -> File(path).takeIf { it.exists() }?.length() ?: 0L }

            result.add(
                HostAppInfo(
                    packageName = pkg,
                    appName = label,
                    versionName = versionName,
                    versionCode = versionCode,
                    isSystemApp = isSystem,
                    baseApkPath = basePath,
                    splitApkPaths = splits,
                    totalSizeBytes = baseSize + splitSize,
                    abiTag = detectAbiFromAppInfo(appInfo)
                )
            )
        }

        result.sortedBy { it.appName.lowercase() }
    }

    suspend fun importHostApp(hostApp: HostAppInfo): ImportResult = withContext(Dispatchers.IO) {
        if (hostApp.baseApkPath.isBlank()) {
            return@withContext ImportResult.Failure("Base APK path is unavailable for ${hostApp.appName}.")
        }

        val sourceBase = File(hostApp.baseApkPath)
        if (!sourceBase.exists() || !sourceBase.canRead()) {
            return@withContext ImportResult.Failure(
                "Cannot read APK for ${hostApp.appName}. Some OEM/system packages restrict direct file access."
            )
        }

        val appContainerDir = File(containerRoot, sanitizePackageDir(hostApp.packageName))
        val apkDir = File(appContainerDir, "apk").apply { mkdirs() }
        val dataDir = File(appContainerDir, "data").apply { mkdirs() }
        val cacheDir = File(appContainerDir, "cache").apply { mkdirs() }

        return@withContext try {
            // Clear stale APKs before copying updated base + splits
            apkDir.listFiles()?.forEach { it.deleteRecursively() }

            val targetBase = File(apkDir, "base.apk")
            sourceBase.inputStream().use { input ->
                targetBase.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            var copiedSplits = 0
            hostApp.splitApkPaths.forEachIndexed { index, splitPath ->
                val splitFile = File(splitPath)
                if (splitFile.exists() && splitFile.canRead()) {
                    val safeName = splitFile.name.takeIf { it.endsWith(".apk") }
                        ?: "split_$index.apk"
                    val targetSplit = File(apkDir, safeName)
                    splitFile.inputStream().use { input ->
                        targetSplit.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    }
                    copiedSplits++
                }
            }

            val totalCopiedBytes = directorySizeBytes(apkDir)
            val detectedAbi = detectAbiFromApkFiles(apkDir).ifBlank { hostApp.abiTag }

            writeContainerManifest(
                containerDir = appContainerDir,
                packageName = hostApp.packageName,
                appName = hostApp.appName,
                versionName = hostApp.versionName,
                abiTag = detectedAbi,
                splitCount = copiedSplits
            )

            dao.upsertApp(
                VirtualAppEntity(
                    packageName = hostApp.packageName,
                    appName = hostApp.appName,
                    versionName = hostApp.versionName,
                    versionCode = hostApp.versionCode,
                    abiTag = detectedAbi,
                    splitCount = copiedSplits,
                    apkSizeBytes = totalCopiedBytes,
                    containerDir = appContainerDir.absolutePath,
                    dataDir = dataDir.absolutePath,
                    cacheDir = cacheDir.absolutePath,
                    importedAt = System.currentTimeMillis(),
                    sourceType = "HOST_PACKAGE"
                )
            )
            ImportResult.Success(hostApp.appName, hostApp.packageName, copiedSplits)
        } catch (t: Throwable) {
            ImportResult.Failure(t.message ?: "Failed to stage ${hostApp.appName}.")
        }
    }

    suspend fun importExternalApk(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "import_probe_${System.currentTimeMillis()}.apk")
        return@withContext try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } ?: return@withContext ImportResult.Failure("Unable to open selected APK stream.")

            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(
                    tempFile.absolutePath,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(tempFile.absolutePath, 0)
            } ?: return@withContext ImportResult.Failure("Selected file is not a valid Android APK package.")

            val appInfo = archiveInfo.applicationInfo?.apply {
                sourceDir = tempFile.absolutePath
                publicSourceDir = tempFile.absolutePath
            }
            val pkg = archiveInfo.packageName
            if (pkg == context.packageName) {
                return@withContext ImportResult.Failure("Cannot import ATP VM into itself.")
            }

            val label = appInfo?.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
            val versionName = archiveInfo.versionName ?: "1.0"
            val versionCode = extractVersionCode(archiveInfo)

            val appContainerDir = File(containerRoot, sanitizePackageDir(pkg))
            val apkDir = File(appContainerDir, "apk").apply { mkdirs() }
            val dataDir = File(appContainerDir, "data").apply { mkdirs() }
            val cacheDir = File(appContainerDir, "cache").apply { mkdirs() }

            apkDir.listFiles()?.forEach { it.deleteRecursively() }
            val targetBase = File(apkDir, "base.apk")
            tempFile.copyTo(targetBase, overwrite = true)

            val detectedAbi = detectAbiFromApkFiles(apkDir)
            writeContainerManifest(
                containerDir = appContainerDir,
                packageName = pkg,
                appName = label,
                versionName = versionName,
                abiTag = detectedAbi,
                splitCount = 0
            )

            dao.upsertApp(
                VirtualAppEntity(
                    packageName = pkg,
                    appName = label,
                    versionName = versionName,
                    versionCode = versionCode,
                    abiTag = detectedAbi,
                    splitCount = 0,
                    apkSizeBytes = targetBase.length(),
                    containerDir = appContainerDir.absolutePath,
                    dataDir = dataDir.absolutePath,
                    cacheDir = cacheDir.absolutePath,
                    importedAt = System.currentTimeMillis(),
                    sourceType = "EXTERNAL_APK"
                )
            )
            ImportResult.Success(label, pkg, 0)
        } catch (t: Throwable) {
            ImportResult.Failure(t.message ?: "Failed to import external APK.")
        } finally {
            tempFile.delete()
        }
    }

    suspend fun verifyContainer(app: VirtualAppEntity): ContainerVerificationReport = withContext(Dispatchers.IO) {
        val container = File(app.containerDir)
        val apkDir = File(container, "apk")
        val baseExists = File(apkDir, "base.apk").exists()
        val splitsFound = apkDir.listFiles { f ->
            f.isFile && f.name.endsWith(".apk") && f.name != "base.apk"
        }?.size ?: 0
        val dataExists = File(app.dataDir).exists()
        val cacheExists = File(app.cacheDir).exists()
        val diskBytes = directorySizeBytes(container)

        ContainerVerificationReport(
            packageName = app.packageName,
            baseApkExists = baseExists,
            splitApkCountFound = splitsFound,
            expectedSplitCount = app.splitCount,
            dataDirExists = dataExists,
            cacheDirExists = cacheExists,
            actualDiskBytes = diskBytes
        )
    }

    suspend fun computeStorageSummary(apps: List<VirtualAppEntity>): StorageSummary = withContext(Dispatchers.IO) {
        val perApp = apps.map { app ->
            val apkBytes = directorySizeBytes(File(app.containerDir, "apk"))
            val dataBytes = directorySizeBytes(File(app.dataDir))
            val cacheBytes = directorySizeBytes(File(app.cacheDir))
            AppStorageBreakdown(
                packageName = app.packageName,
                appName = app.appName,
                apkBytes = apkBytes,
                dataBytes = dataBytes,
                cacheBytes = cacheBytes
            )
        }.sortedByDescending { it.totalBytes }

        StorageSummary(
            totalApkBytes = perApp.sumOf { it.apkBytes },
            totalDataBytes = perApp.sumOf { it.dataBytes },
            totalCacheBytes = perApp.sumOf { it.cacheBytes },
            perApp = perApp
        )
    }

    suspend fun clearAppCache(packageName: String) = withContext(Dispatchers.IO) {
        val app = dao.getByPackage(packageName) ?: return@withContext
        val cacheDir = File(app.cacheDir)
        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun clearAllCaches(apps: List<VirtualAppEntity>) = withContext(Dispatchers.IO) {
        apps.forEach { app ->
            val cacheDir = File(app.cacheDir)
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    suspend fun wipeAppData(packageName: String) = withContext(Dispatchers.IO) {
        val app = dao.getByPackage(packageName) ?: return@withContext
        File(app.dataDir).listFiles()?.forEach { it.deleteRecursively() }
        File(app.cacheDir).listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun removeVirtualApp(packageName: String) = withContext(Dispatchers.IO) {
        val app = dao.getByPackage(packageName)
        if (app != null) {
            File(app.containerDir).deleteRecursively()
        } else {
            File(containerRoot, sanitizePackageDir(packageName)).deleteRecursively()
        }
        dao.deleteByPackage(packageName)
    }

    suspend fun setPerAppVirtualRoot(packageName: String, enabled: Boolean) {
        dao.updateVirtualRootRequested(packageName, enabled)
    }

    suspend fun setPerAppGmsBridge(packageName: String, enabled: Boolean) {
        dao.updateGmsBridgeRequested(packageName, enabled)
    }

    suspend fun getHostDiagnostics(): HostDiagnostics = withContext(Dispatchers.IO) {
        val abis = Build.SUPPORTED_ABIS.toList()
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/su/bin/su"
        )
        val suFound = suPaths.firstOrNull { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
        HostDiagnostics(
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = abis,
            supportsArm64 = abis.any { it.contains("arm64") },
            supportsArm32 = abis.any { it.contains("armeabi") },
            suBinaryFoundPath = suFound,
            multiAppEngineAvailable = false,
            containerRootPath = containerRoot.absolutePath
        )
    }

    suspend fun inspectHostGms(): GmsComponentStatus = withContext(Dispatchers.IO) {
        val gmsInfo = getPackageInfoCompat("com.google.android.gms")
        val gsfInfo = getPackageInfoCompat("com.google.android.gsf")
        val vendingInfo = getPackageInfoCompat("com.android.vending")

        val accountsCount = runCatching {
            AccountManager.get(context).getAccountsByType("com.google").size
        }.getOrNull()

        val summary = when {
            gmsInfo != null && vendingInfo != null ->
                "Host Google Play Services (${gmsInfo.versionName ?: "installed"}) detected. Containerized GMS bridging requires the MultiApp Binder proxy."
            gmsInfo != null ->
                "Host GMS core detected without Play Store. Optional compatibility mode available."
            else ->
                "No Google Play Services detected on host device. ATP VM remains fully functional without GMS."
        }

        GmsComponentStatus(
            gmsInstalled = gmsInfo != null,
            gmsVersion = gmsInfo?.versionName,
            gsfInstalled = gsfInfo != null,
            playStoreInstalled = vendingInfo != null,
            playStoreVersion = vendingInfo?.versionName,
            hostAccountsCount = accountsCount,
            compatibilitySummary = summary
        )
    }

    fun getHostLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    suspend fun loadAppIconBitmap(packageName: String, containerDir: String? = null): Bitmap? =
        withContext(Dispatchers.IO) {
            // Try loading from staged base.apk first, then fallback to host PackageManager
            val stagedApk = containerDir?.let { File(it, "apk/base.apk") }?.takeIf { it.exists() }
            val drawable: Drawable? = if (stagedApk != null) {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageArchiveInfo(
                        stagedApk.absolutePath,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageArchiveInfo(stagedApk.absolutePath, 0)
                }
                info?.applicationInfo?.apply {
                    sourceDir = stagedApk.absolutePath
                    publicSourceDir = stagedApk.absolutePath
                }?.loadIcon(packageManager) ?: runCatching {
                    packageManager.getApplicationIcon(packageName)
                }.getOrNull()
            } else {
                runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            }

            drawable?.let { drawableToBitmap(it) }
        }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceIn(48, 192)
        val height = drawable.intrinsicHeight.coerceIn(48, 192)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getPackageInfoCompat(packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        }.getOrNull()
    }

    private fun extractVersionCode(pkgInfo: PackageInfo): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
    }

    private fun detectAbiFromAppInfo(appInfo: ApplicationInfo): String {
        val nativeDir = appInfo.nativeLibraryDir ?: return "noarch"
        return when {
            nativeDir.contains("arm64") -> "arm64-v8a"
            nativeDir.contains("arm") -> "armeabi-v7a"
            nativeDir.contains("x86_64") -> "x86_64"
            nativeDir.contains("x86") -> "x86"
            else -> "noarch"
        }
    }

    private fun detectAbiFromApkFiles(apkDir: File): String {
        val abis = LinkedHashSet<String>()
        apkDir.listFiles { f -> f.isFile && f.name.endsWith(".apk") }?.forEach { apk ->
            runCatching {
                ZipFile(apk).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name
                        if (name.startsWith("lib/") && name.count { it == '/' } >= 2) {
                            val abi = name.substringAfter("lib/").substringBefore("/")
                            if (abi.isNotBlank()) abis.add(abi)
                        }
                    }
                }
            }
        }
        return when {
            abis.isEmpty() -> "noarch"
            abis.contains("arm64-v8a") && abis.contains("armeabi-v7a") -> "arm64 + arm32"
            abis.size == 1 -> abis.first()
            else -> abis.joinToString(" / ")
        }
    }

    private fun directorySizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun sanitizePackageDir(packageName: String): String {
        return packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun writeContainerManifest(
        containerDir: File,
        packageName: String,
        appName: String,
        versionName: String,
        abiTag: String,
        splitCount: Int
    ) {
        val file = File(containerDir, "container.manifest.txt")
        file.writeText(
            buildString {
                appendLine("ATP_CONTAINER_FORMAT=1")
                appendLine("PACKAGE=$packageName")
                appendLine("NAME=$appName")
                appendLine("VERSION=$versionName")
                appendLine("ABI=$abiTag")
                appendLine("SPLITS=$splitCount")
                appendLine("CREATED_AT=${System.currentTimeMillis()}")
            }
        )
    }
}
