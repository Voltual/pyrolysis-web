package me.voltual.pyrolysis.ui.settings.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.data.ProxySettingsDataStore

class ProxySettingsViewModel(
    private val proxySettingsDataStore: ProxySettingsDataStore
) : ViewModel() {

    data class UiState(
        val useCustomProxy: Boolean = true,
        val customProxyUrl: String = "",
        val customWanyueyunUrl: String = ""
    )

    val uiState: StateFlow<UiState> = combine(
        proxySettingsDataStore.useCustomProxy,
        proxySettingsDataStore.customProxyUrl,
        proxySettingsDataStore.customWanyueyunUrl
    ) { useProxy, proxyUrl, wanyueyunUrl ->
        UiState(useProxy, proxyUrl, wanyueyunUrl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    fun setUseCustomProxy(value: Boolean) {
        viewModelScope.launch {
            proxySettingsDataStore.setUseCustomProxy(value)
        }
    }

    fun setCustomProxyUrl(value: String) {
        viewModelScope.launch {
            proxySettingsDataStore.setCustomProxyUrl(value)
        }
    }

    fun setCustomWanyueyunUrl(value: String) {
        viewModelScope.launch {
            proxySettingsDataStore.setCustomWanyueyunUrl(value)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            proxySettingsDataStore.resetDefaults()
        }
    }
}