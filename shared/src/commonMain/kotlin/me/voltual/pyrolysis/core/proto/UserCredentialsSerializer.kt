package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * 加密型 UserCredentials 序列化器。
 * 遵循 KMP 规范实现 OkioSerializer 接口。
 */
class UserCredentialsSerializer(
    private val cryptoManager: CryptoManager
) : OkioSerializer<UserCredentials> {
    
    override val defaultValue: UserCredentials
        get() = UserCredentials.getDefaultInstance()

    override suspend fun readFrom(source: BufferedSource): UserCredentials {
        val encryptedBytes = source.readByteArray()
        if (encryptedBytes.isEmpty()) {
            return defaultValue
        }
        return try {
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            val jsonString = decryptedBytes.decodeToString()
            Json.decodeFromString(UserCredentials.serializer(), jsonString)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserCredentials, sink: BufferedSink) {
        val jsonString = Json.encodeToString(UserCredentials.serializer(), t)
        val encryptedBytes = cryptoManager.encrypt(jsonString.encodeToByteArray())
        sink.write(encryptedBytes)
    }
}