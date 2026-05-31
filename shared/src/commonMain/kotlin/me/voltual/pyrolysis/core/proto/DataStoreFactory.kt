package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.DataStore

/**
 * 跨平台 DataStore 工厂函数。
 * @param serializer 自定义的加密序列化器。
 * @param context 平台特有的上下文（如 Android 的 Context，Wasm 端可传 null）。
 */
expect fun createDataStore(
    serializer: UserCredentialsSerializer,
    context: Any? = null
): DataStore<UserCredentials>