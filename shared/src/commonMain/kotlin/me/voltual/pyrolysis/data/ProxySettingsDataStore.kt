package me.voltual.pyrolysis.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.voltual.pyrolysis.DefaultApiBaseUrl
import me.voltual.pyrolysis.DefaultWanyueyunUploadApiBaseUrl
import me.voltual.pyrolysis.getPlatformId

class ProxySettingsDataStore(private val dataStore: DataStore<Preferences>) {
    
    companion object {
        private val USE_CUSTOM_PROXY = booleanPreferencesKey("use_custom_proxy")
        private val CUSTOM_PROXY_URL = stringPreferencesKey("custom_proxy_url")
        private val CUSTOM_WANYUEYUN_URL = stringPreferencesKey("custom_wanyueyun_url")
        
        const val DEFAULT_PROXY = "https://bbq.voltual.cc.cd/"
    }

    // 仅在 Android 上默认开启自定义代理，Web 平台默认关闭
    val useCustomProxy: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_CUSTOM_PROXY] ?: (getPlatformId() == "android")
    }

    val customProxyUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[CUSTOM_PROXY_URL] ?: DEFAULT_PROXY
    }

    val customWanyueyunUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[CUSTOM_WANYUEYUN_URL] ?: DefaultWanyueyunUploadApiBaseUrl
    }

    suspend fun setUseCustomProxy(value: Boolean) {
        dataStore.edit { it[USE_CUSTOM_PROXY] = value }
    }

    suspend fun setCustomProxyUrl(value: String) {
        dataStore.edit { it[CUSTOM_PROXY_URL] = value }
    }

    suspend fun setCustomWanyueyunUrl(value: String) {
        dataStore.edit { it[CUSTOM_WANYUEYUN_URL] = value }
    }
    
    suspend fun resetDefaults() {
        dataStore.edit {
            it.remove(USE_CUSTOM_PROXY)
            it.remove(CUSTOM_PROXY_URL)
            it.remove(CUSTOM_WANYUEYUN_URL)
        }
    }
}