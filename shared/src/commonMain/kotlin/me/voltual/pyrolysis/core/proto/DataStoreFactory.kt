package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * 跨平台 DataStore 工厂函数。
 */
expect fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any? = null
): DataStore<UserCredentials>

/**
 * 跨平台 Preferences DataStore 工厂函数。
 */
expect fun createPreferenceDataStore(
    fileName: String,
    context: Any? = null
): DataStore<Preferences>