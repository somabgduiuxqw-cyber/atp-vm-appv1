package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atpEnvDataStore: DataStore<Preferences> by preferencesDataStore(name = "atp_env_prefs")

enum class PreferredAbi(val label: String, val detail: String) {
    AUTO("Auto (Host Native)", "Matches host device ABI priority"),
    ARM64("ARM64-v8a", "Prefer 64-bit native libraries"),
    ARM32("ARMv7 (armeabi-v7a)", "Prefer 32-bit compatibility libraries")
}

enum class GmsOptionMode(val label: String, val summary: String) {
    DISABLED("Disabled (Lightweight)", "Zero Google Services footprint; recommended for minimal size"),
    HOST_INSPECTION("Host GMS Compatibility", "Inspect host Google Play Services without bundling proprietary blobs"),
    OPEN_COMPAT_INFO("Open Compatibility Guide", "View microG / open-source GMS container compatibility notes")
}

data class EnvironmentSettings(
    val isolateAppData: Boolean = true,
    val isolateExternalStorage: Boolean = true,
    val virtualRootPolicyEnabled: Boolean = false,
    val preferredAbi: PreferredAbi = PreferredAbi.AUTO,
    val gmsMode: GmsOptionMode = GmsOptionMode.DISABLED,
    val includeSystemAppsInScanner: Boolean = false
)

class EnvironmentPreferences(private val context: Context) {

    private object Keys {
        val ISOLATE_APP_DATA = booleanPreferencesKey("isolate_app_data")
        val ISOLATE_EXTERNAL_STORAGE = booleanPreferencesKey("isolate_external_storage")
        val VIRTUAL_ROOT_POLICY = booleanPreferencesKey("virtual_root_policy")
        val PREFERRED_ABI = stringPreferencesKey("preferred_abi")
        val GMS_MODE = stringPreferencesKey("gms_mode")
        val INCLUDE_SYSTEM_APPS = booleanPreferencesKey("include_system_apps")
    }

    val settingsFlow: Flow<EnvironmentSettings> = context.atpEnvDataStore.data.map { prefs ->
        EnvironmentSettings(
            isolateAppData = prefs[Keys.ISOLATE_APP_DATA] ?: true,
            isolateExternalStorage = prefs[Keys.ISOLATE_EXTERNAL_STORAGE] ?: true,
            virtualRootPolicyEnabled = prefs[Keys.VIRTUAL_ROOT_POLICY] ?: false,
            preferredAbi = prefs[Keys.PREFERRED_ABI]?.let { name ->
                PreferredAbi.entries.firstOrNull { it.name == name }
            } ?: PreferredAbi.AUTO,
            gmsMode = prefs[Keys.GMS_MODE]?.let { name ->
                GmsOptionMode.entries.firstOrNull { it.name == name }
            } ?: GmsOptionMode.DISABLED,
            includeSystemAppsInScanner = prefs[Keys.INCLUDE_SYSTEM_APPS] ?: false
        )
    }

    suspend fun setIsolateAppData(enabled: Boolean) {
        context.atpEnvDataStore.edit { it[Keys.ISOLATE_APP_DATA] = enabled }
    }

    suspend fun setIsolateExternalStorage(enabled: Boolean) {
        context.atpEnvDataStore.edit { it[Keys.ISOLATE_EXTERNAL_STORAGE] = enabled }
    }

    suspend fun setVirtualRootPolicy(enabled: Boolean) {
        context.atpEnvDataStore.edit { it[Keys.VIRTUAL_ROOT_POLICY] = enabled }
    }

    suspend fun setPreferredAbi(abi: PreferredAbi) {
        context.atpEnvDataStore.edit { it[Keys.PREFERRED_ABI] = abi.name }
    }

    suspend fun setGmsMode(mode: GmsOptionMode) {
        context.atpEnvDataStore.edit { it[Keys.GMS_MODE] = mode.name }
    }

    suspend fun setIncludeSystemAppsInScanner(enabled: Boolean) {
        context.atpEnvDataStore.edit { it[Keys.INCLUDE_SYSTEM_APPS] = enabled }
    }
}
