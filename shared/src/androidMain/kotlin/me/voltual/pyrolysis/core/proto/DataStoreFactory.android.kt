package me.voltual.pyrolysis.core.proto

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.dataStoreFile
import okio.FileSystem
import okio.Path.Companion.toPath

actual fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any?
): DataStore<UserCredentials> {
    val appContext = context as? Context ?: throw IllegalArgumentException("Android 平台需要传入 Context 参数")
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = serializer,
            producePath = {
                appContext.filesDir.resolve("user_credentials.preferences_pb").absolutePath.toPath()
            }
        )
    )
}

actual fun createPreferenceDataStore(
    fileName: String,
    context: Any?
): DataStore<Preferences> {
    val appContext = context as? Context ?: throw IllegalArgumentException("Android 平台需要传入 Context 参数")
    return PreferenceDataStoreFactory.create(
        produceFile = { appContext.dataStoreFile(fileName) }
    )
}