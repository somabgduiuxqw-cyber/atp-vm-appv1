package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AtpDatabase
import com.example.data.repository.AtpContainerRepository
import com.example.data.repository.HostAppInfo
import com.example.data.repository.ImportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.waxmoon.BuildConfig
import org.waxmoon.R
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AtpDatabase
    private lateinit var repository: AtpContainerRepository
    private lateinit var tempSourceDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AtpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AtpContainerRepository(context, database.virtualAppDao())
        tempSourceDir = File(context.cacheDir, "test_apks").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        database.close()
        tempSourceDir.deleteRecursively()
        repository.containerRoot.deleteRecursively()
    }

    @Test
    fun `verify ATP VM identity and package name`() {
        assertEquals("ATP VM", context.getString(R.string.app_name))
        assertEquals("com.atp.vm", context.packageName)
        assertEquals("com.atp.vm", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `import split APK stages isolated container, verifies integrity, and manages storage`() = runTest {
        val baseApk = File(tempSourceDir, "base.apk").apply { writeBytes(ByteArray(2048) { 1 }) }
        val splitArm = File(tempSourceDir, "split_config.arm64_v8a.apk").apply { writeBytes(ByteArray(1024) { 2 }) }
        val splitLang = File(tempSourceDir, "split_config.en.apk").apply { writeBytes(ByteArray(512) { 3 }) }

        val hostApp = HostAppInfo(
            packageName = "org.test.sample",
            appName = "Sample App",
            versionName = "2.1.0",
            versionCode = 21L,
            isSystemApp = false,
            baseApkPath = baseApk.absolutePath,
            splitApkPaths = listOf(splitArm.absolutePath, splitLang.absolutePath),
            totalSizeBytes = baseApk.length() + splitArm.length() + splitLang.length(),
            abiTag = "arm64-v8a"
        )

        val result = repository.importHostApp(hostApp)
        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(2, success.splitCount)

        val imported = repository.observeImportedApps().first()
        assertEquals(1, imported.size)
        val entity = imported.first()

        // Add isolated data and cache bytes to verify storage management
        File(entity.dataDir, "user.db").writeBytes(ByteArray(400))
        File(entity.cacheDir, "http_cache.bin").writeBytes(ByteArray(300))

        val verification = repository.verifyContainer(entity)
        assertTrue(verification.isHealthy)
        assertEquals(2, verification.splitApkCountFound)

        val storageBefore = repository.computeStorageSummary(imported)
        assertEquals(3584L, storageBefore.totalApkBytes)
        assertEquals(400L, storageBefore.totalDataBytes)
        assertEquals(300L, storageBefore.totalCacheBytes)

        repository.clearAppCache(entity.packageName)
        val storageAfterCache = repository.computeStorageSummary(imported)
        assertEquals(0L, storageAfterCache.totalCacheBytes)
        assertEquals(400L, storageAfterCache.totalDataBytes)

        repository.wipeAppData(entity.packageName)
        val storageAfterWipe = repository.computeStorageSummary(imported)
        assertEquals(0L, storageAfterWipe.totalDataBytes)
        assertEquals(3584L, storageAfterWipe.totalApkBytes)

        val launchResult = repository.launchVirtualApp(entity.packageName, 0)
        assertTrue(launchResult is com.example.data.repository.VirtualLaunchResult.EngineUnavailable)

        repository.removeVirtualApp(entity.packageName, 0)
        assertTrue(repository.observeAppsForSpace(0).first().isEmpty())
        assertFalse(File(entity.containerDir).exists())
    }

    @Test
    fun `host diagnostics accurately reports absence of MultiApp native engine`() = runTest {
        val diag = repository.getHostDiagnostics()
        assertFalse(diag.multiAppEngineAvailable)
        assertTrue(diag.containerRootPath.contains("atp_spaces") || diag.containerRootPath.contains("atp_containers"))
    }
}
