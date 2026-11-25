package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AtpDatabase
import com.example.data.local.VirtualAppEntity
import com.example.data.preferences.EnvironmentPreferences
import com.example.data.preferences.EnvironmentSettings
import com.example.data.preferences.GmsOptionMode
import com.example.data.preferences.PreferredAbi
import com.example.data.repository.AtpContainerRepository
import com.example.data.repository.ContainerVerificationReport
import com.example.data.repository.GmsComponentStatus
import com.example.data.repository.HostAppInfo
import com.example.data.repository.HostDiagnostics
import com.example.data.repository.ImportResult
import com.example.data.repository.StorageSummary
import com.example.data.repository.VirtualLaunchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AtpUiState(
    val userSpace: Int = 0,
    val importedApps: List<VirtualAppEntity> = emptyList(),
    val hostApps: List<HostAppInfo> = emptyList(),
    val isScanningHostApps: Boolean = false,
    val importingPackage: String? = null,
    val storageSummary: StorageSummary = StorageSummary(),
    val settings: EnvironmentSettings = EnvironmentSettings(),
    val diagnostics: HostDiagnostics? = null,
    val gmsStatus: GmsComponentStatus? = null,
    val verificationReport: ContainerVerificationReport? = null,
    val assistPromptPackage: String? = null,
    val launchNotice: String? = null,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AtpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AtpDatabase.getInstance(application)
    private val repository = AtpContainerRepository(application, database.virtualAppDao())
    private val preferences = EnvironmentPreferences(application)

    private val userSpaceFlow = MutableStateFlow(0)
    private val activeSpaceAppsFlow = userSpaceFlow.flatMapLatest { space ->
        repository.observeAppsForSpace(space)
    }
    private val hostAppsFlow = MutableStateFlow<List<HostAppInfo>>(emptyList())
    private val scanningFlow = MutableStateFlow(false)
    private val importingPkgFlow = MutableStateFlow<String?>(null)
    private val storageSummaryFlow = MutableStateFlow(StorageSummary())
    private val diagnosticsFlow = MutableStateFlow<HostDiagnostics?>(null)
    private val gmsStatusFlow = MutableStateFlow<GmsComponentStatus?>(null)
    private val verificationFlow = MutableStateFlow<ContainerVerificationReport?>(null)
    private val assistPromptPkgFlow = MutableStateFlow<String?>(null)
    private val launchNoticeFlow = MutableStateFlow<String?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)

    private val iconCache = HashMap<String, Bitmap?>()

    val uiState: StateFlow<AtpUiState> = combine(
        userSpaceFlow,
        activeSpaceAppsFlow,
        hostAppsFlow,
        scanningFlow,
        importingPkgFlow,
        storageSummaryFlow,
        preferences.settingsFlow,
        diagnosticsFlow,
        gmsStatusFlow,
        verificationFlow,
        assistPromptPkgFlow,
        launchNoticeFlow,
        statusMessageFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AtpUiState(
            userSpace = values[0] as Int,
            importedApps = values[1] as List<VirtualAppEntity>,
            hostApps = values[2] as List<HostAppInfo>,
            isScanningHostApps = values[3] as Boolean,
            importingPackage = values[4] as String?,
            storageSummary = values[5] as StorageSummary,
            settings = values[6] as EnvironmentSettings,
            diagnostics = values[7] as HostDiagnostics?,
            gmsStatus = values[8] as GmsComponentStatus?,
            verificationReport = values[9] as ContainerVerificationReport?,
            assistPromptPackage = values[10] as String?,
            launchNotice = values[11] as String?,
            statusMessage = values[12] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AtpUiState()
    )

    init {
        refreshDiagnosticsAndGms()
        viewModelScope.launch {
            activeSpaceAppsFlow.collect { apps ->
                storageSummaryFlow.value = repository.computeStorageSummary(apps)
            }
        }
    }

    fun incrementUserSpace() {
        userSpaceFlow.value = (userSpaceFlow.value + 1).coerceAtMost(9)
    }

    fun decrementUserSpace() {
        userSpaceFlow.value = (userSpaceFlow.value - 1).coerceAtLeast(0)
    }

    fun refreshDiagnosticsAndGms() {
        viewModelScope.launch {
            diagnosticsFlow.value = repository.getHostDiagnostics()
            gmsStatusFlow.value = repository.inspectHostGms()
        }
    }

    fun scanHostApps() {
        if (scanningFlow.value) return
        viewModelScope.launch {
            scanningFlow.value = true
            hostAppsFlow.value = repository.scanInstalledHostApps()
            scanningFlow.value = false
        }
    }

    fun importHostApp(hostApp: HostAppInfo) {
        viewModelScope.launch {
            importingPkgFlow.value = hostApp.packageName
            val activeSpace = userSpaceFlow.value
            when (val res = repository.importHostApp(hostApp, activeSpace)) {
                is ImportResult.Success -> {
                    val splitNote = if (res.splitCount > 0) " (+${res.splitCount} split APKs)" else ""
                    statusMessageFlow.value = "Staged ${res.appName}$splitNote in Space $activeSpace."
                }
                is ImportResult.Failure -> {
                    statusMessageFlow.value = res.reason
                }
            }
            importingPkgFlow.value = null
            refreshStorage()
        }
    }

    fun importExternalApk(uri: Uri) {
        viewModelScope.launch {
            importingPkgFlow.value = "external_apk"
            val activeSpace = userSpaceFlow.value
            when (val res = repository.importExternalApk(uri, activeSpace)) {
                is ImportResult.Success -> {
                    statusMessageFlow.value = "Imported ${res.appName} (${res.packageName}) in Space $activeSpace."
                }
                is ImportResult.Failure -> {
                    statusMessageFlow.value = res.reason
                }
            }
            importingPkgFlow.value = null
            refreshStorage()
        }
    }

    fun launchVirtualApp(packageName: String) {
        viewModelScope.launch {
            val activeSpace = userSpaceFlow.value
            when (val result = repository.launchVirtualApp(packageName, activeSpace)) {
                VirtualLaunchResult.Launched -> {
                    statusMessageFlow.value = "Launched $packageName in Space $activeSpace."
                }
                VirtualLaunchResult.AssistPackageRequired -> {
                    assistPromptPkgFlow.value = packageName
                }
                is VirtualLaunchResult.EngineUnavailable -> {
                    launchNoticeFlow.value = result.message
                }
                is VirtualLaunchResult.Failed -> {
                    statusMessageFlow.value = result.reason
                }
            }
        }
    }

    fun dismissAssistPrompt() {
        assistPromptPkgFlow.value = null
    }

    fun clearLaunchNotice() {
        launchNoticeFlow.value = null
    }

    fun stageGoogleServicesFromHost() {
        viewModelScope.launch {
            val activeSpace = userSpaceFlow.value
            val results = repository.stageGoogleServicesFromHost(activeSpace)
            statusMessageFlow.value = "GMS Space $activeSpace: ${results.joinToString("; ")}"
            refreshDiagnosticsAndGms()
        }
    }

    fun verifyContainer(app: VirtualAppEntity) {
        viewModelScope.launch {
            verificationFlow.value = repository.verifyContainer(app)
        }
    }

    fun clearVerificationReport() {
        verificationFlow.value = null
    }

    fun clearAppCache(packageName: String) {
        viewModelScope.launch {
            repository.clearAppCache(packageName, userSpaceFlow.value)
            refreshStorage()
            statusMessageFlow.value = "Cleared cache for $packageName."
        }
    }

    fun clearAllCaches() {
        viewModelScope.launch {
            repository.clearAllCaches(uiState.value.importedApps)
            refreshStorage()
            statusMessageFlow.value = "Cleared all container caches in Space ${userSpaceFlow.value}."
        }
    }

    fun wipeAppData(packageName: String) {
        viewModelScope.launch {
            repository.wipeAppData(packageName, userSpaceFlow.value)
            refreshStorage()
            statusMessageFlow.value = "Wiped isolated data for $packageName in Space ${userSpaceFlow.value}."
        }
    }

    fun removeVirtualApp(packageName: String) {
        viewModelScope.launch {
            val activeSpace = userSpaceFlow.value
            repository.removeVirtualApp(packageName, activeSpace)
            iconCache.remove("${activeSpace}:$packageName")
            refreshStorage()
            statusMessageFlow.value = "Removed container $packageName from Space $activeSpace."
        }
    }

    fun setPerAppVirtualRoot(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setPerAppVirtualRoot(packageName, userSpaceFlow.value, enabled)
        }
    }

    fun setPerAppGmsBridge(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setPerAppGmsBridge(packageName, userSpaceFlow.value, enabled)
        }
    }

    fun setIsolateAppData(enabled: Boolean) {
        viewModelScope.launch { preferences.setIsolateAppData(enabled) }
    }

    fun setIsolateExternalStorage(enabled: Boolean) {
        viewModelScope.launch { preferences.setIsolateExternalStorage(enabled) }
    }

    fun setVirtualRootPolicy(enabled: Boolean) {
        viewModelScope.launch { preferences.setVirtualRootPolicy(enabled) }
    }

    fun setPreferredAbi(abi: PreferredAbi) {
        viewModelScope.launch { preferences.setPreferredAbi(abi) }
    }

    fun setGmsMode(mode: GmsOptionMode) {
        viewModelScope.launch { preferences.setGmsMode(mode) }
    }

    fun setIncludeSystemAppsInScanner(enabled: Boolean) {
        viewModelScope.launch { preferences.setIncludeSystemAppsInScanner(enabled) }
    }

    fun consumeStatusMessage() {
        statusMessageFlow.value = null
    }

    fun getHostLaunchIntent(packageName: String) = repository.getHostLaunchIntent(packageName)

    suspend fun loadIconBitmap(packageName: String, containerDir: String? = null): Bitmap? {
        val cacheKey = "${containerDir ?: "host"}:$packageName"
        if (iconCache.containsKey(cacheKey)) return iconCache[cacheKey]
        val loaded = repository.loadAppIconBitmap(packageName, containerDir)
        iconCache[cacheKey] = loaded
        return loaded
    }

    fun refreshStorage() {
        viewModelScope.launch {
            storageSummaryFlow.value = repository.computeStorageSummary(uiState.value.importedApps)
        }
    }
}
