//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.voltual.pyrolysis.core.proto.UserCredentials

/**
 * 持久化且高度安全的 AuthRepository。
 * 数据通过 DataStore 存储在磁盘（Android）或 LocalStorage（WasmJS）中，且全程通过 AES-GCM + PBKDF2 加密。
 */
class AuthRepository(
    private val dataStore: DataStore<UserCredentials>
) {

    // --- 1. 读取逻辑 ---

    // 暴露为只读 Flow，UI 订阅它时就和订阅真正的 DataStore 一模一样
    val credentials: Flow<UserCredentials> = dataStore.data
    
    val userId: Flow<Long> = credentials.map { it.userId }

    val deviceId: Flow<String> = credentials.map { it.deviceId.ifEmpty { generateDeviceId() } }

    // --- 2. 保存逻辑 ---

    suspend fun saveCredentials(
        username: String,
        password: String,
        token: String,
        userId: Long
    ) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setUsername(username)
                .setPassword(password)
                .setToken(token)
                .setUserId(userId)
                .setDeviceId(current.deviceId.ifEmpty { generateDeviceId() })
                .build()
        }
    }

    // --- 3. 清理逻辑 ---

    suspend fun clearCredentials() {
        dataStore.updateData {
            UserCredentials.getDefaultInstance()
        }
    }

    private fun generateDeviceId(): String = (1..15).map { (0..9).random() }.joinToString("")
}