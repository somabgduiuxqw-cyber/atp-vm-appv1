package com.example

import com.example.data.repository.AppStorageBreakdown
import com.example.data.repository.ContainerVerificationReport
import com.example.data.repository.StorageSummary
import com.example.ui.components.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.waxmoon.BuildConfig

class ExampleUnitTest {

    @Test
    fun `formatBytes formats bytes, KB, MB, and GB accurately`() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("2 KB", formatBytes(2048L))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", formatBytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun `storage summary totals aggregate APK, data, and cache sizes`() {
        val items = listOf(
            AppStorageBreakdown("com.a", "App A", apkBytes = 1000L, dataBytes = 200L, cacheBytes = 50L),
            AppStorageBreakdown("com.b", "App B", apkBytes = 3000L, dataBytes = 400L, cacheBytes = 150L)
        )
        val summary = StorageSummary(
            totalApkBytes = items.sumOf { it.apkBytes },
            totalDataBytes = items.sumOf { it.dataBytes },
            totalCacheBytes = items.sumOf { it.cacheBytes },
            perApp = items
        )
        assertEquals(4000L, summary.totalApkBytes)
        assertEquals(600L, summary.totalDataBytes)
        assertEquals(200L, summary.totalCacheBytes)
        assertEquals(4800L, summary.grandTotalBytes)
        assertEquals(1250L, items[0].totalBytes)
    }

    @Test
    fun `container verification health checks base APK, splits, data, and cache`() {
        val healthy = ContainerVerificationReport(
            packageName = "org.test",
            baseApkExists = true,
            splitApkCountFound = 2,
            expectedSplitCount = 2,
            dataDirExists = true,
            cacheDirExists = true,
            actualDiskBytes = 4096L
        )
        assertTrue(healthy.isHealthy)

        val missingSplit = healthy.copy(splitApkCountFound = 1)
        assertFalse(missingSplit.isHealthy)

        val missingBase = healthy.copy(baseApkExists = false)
        assertFalse(missingBase.isHealthy)
    }

    @Test
    fun `buildConfig applicationId matches com_atp_vm`() {
        assertEquals("com.atp.vm", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `virtual app entity keys are isolated per userSpace`() {
        val k0 = com.example.data.local.VirtualAppEntity.keyFor(0, "com.example.app")
        val k1 = com.example.data.local.VirtualAppEntity.keyFor(1, "com.example.app")
        assertEquals("0:com.example.app", k0)
        assertEquals("1:com.example.app", k1)
        assertFalse(k0 == k1)
    }

    @Test
    fun `opensdk runtime accurately reports unavailable when native binaries are absent`() {
        assertFalse(com.hack.opensdk.HackRuntime.isEngineReady())
        assertFalse(com.hack.opensdk.HackApi.isNativeRuntimeAvailable())
    }
}
