package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer

actual fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    // 使用官方 1.3.0 内置的 WebLocalStorage
    return DataStoreFactory.create(
        storage = WebLocalStorage(
            serializer = serializer,
            name = "user_credentials_secure"
        )
    )
}

actual fun createPreferenceDataStore(
    fileName: String,
    context: Any?
): DataStore<Preferences> {
    // 同样使用 WebLocalStorage 替代 OkioStorage
    return PreferenceDataStoreFactory.create(
        storage = WebLocalStorage(
            serializer = PreferencesSerializer,
            name = fileName
        )
    )
}