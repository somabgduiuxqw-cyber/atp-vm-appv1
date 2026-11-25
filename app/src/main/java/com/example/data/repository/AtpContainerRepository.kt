package com.example.data.repository

import android.accounts.AccountManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.example.data.local.VirtualAppDao
import com.example.data.local.VirtualAppEntity
import com.hack.opensdk.BuildConfig as OpenSdkBuildConfig
import com.hack.opensdk.CmdConstants
import com.hack.opensdk.HackApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

sealed interface VirtualLaunchResult {
    data object Launched : VirtualLaunchResult
    data object AssistPackageRequired : VirtualLaunchResult
    data class EngineUnavailable(val message: String) : VirtualLaunchResult
    data class Failed(val reason: String) : VirtualLaunchResult
}

class AtpContainerRepository(
    private val context: Context,
    private val dao: VirtualAppDao
) {
    private val packageManager: PackageManager = context.packageManager

    val containerRoot: File
        get() = HackApi.getSpaceRoot(0)?.parentFile
            ?: File(context.filesDir, "atp_spaces").apply { if (!exists()) mkdirs() }

    fun observeImportedApps(): Flow<List<VirtualAppEntity>> = dao.observeAllApps()

    fun observeAppsForSpace(userSpace: Int): Flow<List<VirtualAppEntity>> =
        dao.observeAppsForSpace(userSpace)

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

    suspend fun importHostApp(hostApp: HostAppInfo, userSpace: Int = 0): ImportResult =
        withContext(Dispatchers.IO) {
            if (hostApp.baseApkPath.isBlank()) {
                return@withContext ImportResult.Failure("Base APK path is unavailable for ${hostApp.appName}.")
            }

            val installRet = HackApi.installPackageFromHost(hostApp.packageName, userSpace, true)
            if (installRet != CmdConstants.INSTALL_SUCCEEDED &&
                installRet != CmdConstants.INSTALL_FAILED_ALREADY_EXISTS
            ) {
                return@withContext ImportResult.Failure(
                    "Cannot read APK for ${hostApp.appName}. Some OEM/system packages restrict direct file access."
                )
            }

            val spaceRoot = HackApi.getSpaceRoot(userSpace) ?: File(containerRoot, "user_$userSpace")
            val appContainerDir = File(spaceRoot, sanitizePackageDir(hostApp.packageName))
            val apkDir = File(appContainerDir, "apk").apply { mkdirs() }
            val dataDir = File(appContainerDir, "data").apply { mkdirs() }
            val cacheDir = File(appContainerDir, "cache").apply { mkdirs() }

            if (!File(apkDir, "base.apk").exists()) {
                return@withContext ImportResult.Failure(
                    "Staging verification failed: base.apk was not written to container storage."
                )
            }

            saveContainerIcon(hostApp.packageName, appContainerDir)

            val copiedSplits = apkDir.listFiles { f ->
                f.isFile && f.name.endsWith(".apk") && f.name != "base.apk"
            }?.size ?: 0
            val totalCopiedBytes = directorySizeBytes(apkDir)
            val detectedAbi = detectAbiFromApkFiles(apkDir).ifBlank { hostApp.abiTag }

            writeContainerManifest(
                containerDir = appContainerDir,
                packageName = hostApp.packageName,
                appName = hostApp.appName,
                versionName = hostApp.versionName,
                abiTag = detectedAbi,
                splitCount = copiedSplits,
                userSpace = userSpace
            )

            dao.upsertApp(
                VirtualAppEntity(
                    containerKey = VirtualAppEntity.keyFor(userSpace, hostApp.packageName),
                    userSpace = userSpace,
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
        }

    suspend fun importExternalApk(uri: Uri, userSpace: Int = 0): ImportResult =
        withContext(Dispatchers.IO) {
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
                    return@withContext ImportResult.Failure("Cannot import ATP Virtual Environment into itself.")
                }

                val installRet = HackApi.installApkFiles(tempFile.absolutePath, userSpace, true)
                if (installRet != CmdConstants.INSTALL_SUCCEEDED &&
                    installRet != CmdConstants.INSTALL_FAILED_ALREADY_EXISTS
                ) {
                    return@withContext ImportResult.Failure("HackApi failed to stage external APK ($installRet).")
                }

                val label = appInfo?.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                val versionName = archiveInfo.versionName ?: "1.0"
                val versionCode = extractVersionCode(archiveInfo)

                val spaceRoot = HackApi.getSpaceRoot(userSpace) ?: File(containerRoot, "user_$userSpace")
                val appContainerDir = File(spaceRoot, sanitizePackageDir(pkg))
                val apkDir = File(appContainerDir, "apk").apply { mkdirs() }
                val dataDir = File(appContainerDir, "data").apply { mkdirs() }
                val cacheDir = File(appContainerDir, "cache").apply { mkdirs() }

                saveContainerIcon(pkg, appContainerDir, File(apkDir, "base.apk"))

                val detectedAbi = detectAbiFromApkFiles(apkDir)
                writeContainerManifest(
                    containerDir = appContainerDir,
                    packageName = pkg,
                    appName = label,
                    versionName = versionName,
                    abiTag = detectedAbi,
                    splitCount = 0,
                    userSpace = userSpace
                )

                dao.upsertApp(
                    VirtualAppEntity(
                        containerKey = VirtualAppEntity.keyFor(userSpace, pkg),
                        userSpace = userSpace,
                        packageName = pkg,
                        appName = label,
                        versionName = versionName,
                        versionCode = versionCode,
                        abiTag = detectedAbi,
                        splitCount = 0,
                        apkSizeBytes = directorySizeBytes(apkDir),
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

    suspend fun launchVirtualApp(packageName: String, userSpace: Int): VirtualLaunchResult =
        withContext(Dispatchers.IO) {
            val containerEntity = dao.getByPackage(packageName, userSpace)
            val spaceRoot = HackApi.getSpaceRoot(userSpace) ?: File(containerRoot, "user_$userSpace")
            val containerDir = containerEntity?.let { File(it.containerDir) }
                ?: File(spaceRoot, sanitizePackageDir(packageName))
            val stagedBaseApk = File(File(containerDir, "apk"), "base.apk")

            if (!stagedBaseApk.exists() || stagedBaseApk.length() == 0L) {
                return@withContext VirtualLaunchResult.Failed(
                    "Staged APK missing for $packageName in Space $userSpace. Re-import the app first."
                )
            }

            val extras = HackApi.getPackageSetting(packageName, userSpace, 0)
            val needsAssist = extras?.getBoolean(CmdConstants.PKG_SET_REQUEST_ASSISTANT, false) == true
            if (needsAssist && !isAssistPackageInstalled()) {
                return@withContext VirtualLaunchResult.AssistPackageRequired
            }

            val launchIntent = resolveContainerLaunchIntent(packageName, stagedBaseApk)
                ?: return@withContext VirtualLaunchResult.Failed(
                    "No launchable Activity found inside staged APK for $packageName."
                )

            return@withContext when (val code = HackApi.startActivity(launchIntent, userSpace)) {
                CmdConstants.START_SUCCESS -> VirtualLaunchResult.Launched
                CmdConstants.START_ERR_NOT_INSTALLED -> VirtualLaunchResult.Failed(
                    "Container base.apk is missing from Space $userSpace."
                )
                CmdConstants.START_ERR_ENGINE_UNAVAILABLE -> {
                    val diag = HackApi.getRuntimeDiagnosticSummary()
                    VirtualLaunchResult.EngineUnavailable(
                        "Container APK and directories are staged in Space $userSpace (${stagedBaseApk.absolutePath}), " +
                            "but in-container execution cannot start because the closed-source WaxMoon/opensdk runtime binaries " +
                            "(moon.jar and native libhack.so/libmoon.so for armeabi-v7a/arm64-v8a) are not populated in this repository.\n\n" +
                            "Runtime telemetry: $diag"
                    )
                }
                else -> VirtualLaunchResult.Failed("HackApi.startActivity returned code $code")
            }
        }

    private fun resolveContainerLaunchIntent(packageName: String, stagedBaseApk: File): Intent? {
        packageManager.getLaunchIntentForPackage(packageName)?.let { hostIntent ->
            return hostIntent.apply {
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }

        val flags = PackageManager.GET_ACTIVITIES.toLong()
        val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                stagedBaseApk.absolutePath,
                PackageManager.PackageInfoFlags.of(flags)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(stagedBaseApk.absolutePath, PackageManager.GET_ACTIVITIES)
        }

        val exportedActivity = archiveInfo?.activities?.firstOrNull { it.exported }
            ?: archiveInfo?.activities?.firstOrNull()
        return if (exportedActivity != null) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(packageName, exportedActivity.name)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
        }
    }

    fun isAssistPackageInstalled(): Boolean {
        return getPackageInfoCompat(OpenSdkBuildConfig.ASSIST_PACKAGE) != null
    }

    suspend fun stageGoogleServicesFromHost(userSpace: Int): List<String> = withContext(Dispatchers.IO) {
        val googlePkgs = listOf("com.google.android.gms", "com.google.android.gsf", "com.android.vending")
        val results = mutableListOf<String>()
        for (pkg in googlePkgs) {
            val ret = HackApi.installPackageFromHost(pkg, userSpace, false)
            val label = when (ret) {
                CmdConstants.INSTALL_SUCCEEDED -> "staged"
                CmdConstants.INSTALL_FAILED_ALREADY_EXISTS -> "already staged"
                else -> "unavailable on host"
            }
            results.add("$pkg: $label")
        }
        results
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

    suspend fun clearAppCache(packageName: String, userSpace: Int = 0) = withContext(Dispatchers.IO) {
        val app = dao.getByPackage(packageName, userSpace) ?: return@withContext
        File(app.cacheDir).listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun clearAllCaches(apps: List<VirtualAppEntity>) = withContext(Dispatchers.IO) {
        apps.forEach { app ->
            File(app.cacheDir).listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    suspend fun wipeAppData(packageName: String, userSpace: Int = 0) = withContext(Dispatchers.IO) {
        val app = dao.getByPackage(packageName, userSpace) ?: return@withContext
        File(app.dataDir).listFiles()?.forEach { it.deleteRecursively() }
        File(app.cacheDir).listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun removeVirtualApp(packageName: String, userSpace: Int = 0) = withContext(Dispatchers.IO) {
        HackApi.uninstallPackage(packageName, userSpace)
        val app = dao.getByPackage(packageName, userSpace)
        if (app != null) {
            File(app.containerDir).deleteRecursively()
        }
        dao.deleteByPackage(packageName, userSpace)
    }

    suspend fun setPerAppVirtualRoot(packageName: String, userSpace: Int = 0, enabled: Boolean) {
        dao.updateVirtualRootRequested(packageName, userSpace, enabled)
    }

    suspend fun setPerAppGmsBridge(packageName: String, userSpace: Int = 0, enabled: Boolean) {
        dao.updateGmsBridgeRequested(packageName, userSpace, enabled)
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
            multiAppEngineAvailable = HackApi.isNativeRuntimeAvailable(),
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
                "Host Google Play Services (${gmsInfo.versionName ?: "installed"}) detected. Ready for HackApi GMS staging when requested."
            gmsInfo != null ->
                "Host GMS core detected without Play Store. Optional compatibility mode available."
            else ->
                "No Google Play Services detected on host device. ATP Virtual Environment remains fully functional without GMS."
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
            val cachedIcon = containerDir?.let { File(it, "icon.png") }?.takeIf { it.exists() }
            if (cachedIcon != null) {
                BitmapFactory.decodeFile(cachedIcon.absolutePath)?.let { return@withContext it }
            }

            val stagedApk = containerDir?.let { File(it, "apk/base.apk") }?.takeIf { it.exists() }
            val drawable: Drawable? = if (stagedApk != null) {
                loadDrawableFromApk(stagedApk) ?: runCatching {
                    packageManager.getApplicationIcon(packageName)
                }.getOrNull()
            } else {
                runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            }

            drawable?.let { drawableToBitmap(it) }
        }

    private fun saveContainerIcon(packageName: String, containerDir: File, fallbackApk: File? = null) {
        val iconFile = File(containerDir, "icon.png")
        val drawable = fallbackApk?.takeIf { it.exists() }?.let { loadDrawableFromApk(it) }
            ?: runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
            ?: return
        val bitmap = drawableToBitmap(drawable)
        runCatching {
            FileOutputStream(iconFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        }
    }

    private fun loadDrawableFromApk(apkFile: File): Drawable? {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
        }
        return info?.applicationInfo?.apply {
            sourceDir = apkFile.absolutePath
            publicSourceDir = apkFile.absolutePath
        }?.loadIcon(packageManager)
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
        splitCount: Int,
        userSpace: Int
    ) {
        val file = File(containerDir, "container.manifest.txt")
        file.writeText(
            buildString {
                appendLine("ATP_CONTAINER_FORMAT=2")
                appendLine("USER_SPACE=$userSpace")
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
