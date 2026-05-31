package me.voltual.pyrolysis.core.proto

import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * 加密型 UserCredentials 序列化器。
 * 仅在边界处与 DataStore 要求的 Okio 进行字节桥接，核心逻辑全为标准 ByteArray。
 */
class UserCredentialsSerializer(
    private val cryptoManager: CryptoManager
) : Serializer<UserCredentials> {
    
    override val defaultValue: UserCredentials
        get() = UserCredentials.getDefaultInstance()

    override suspend fun readFrom(input: BufferedSource): UserCredentials {
        // 1. 直接用 Okio 读取全部原始字节，后续与 Okio 无关
        val encryptedBytes = input.readByteArray()
        if (encryptedBytes.isEmpty()) {
            return defaultValue
        }
        return try {
            // 2. 纯 ByteArray 解密
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            // 3. 纯 kotlinx.serialization 反序列化
            val jsonString = decryptedBytes.decodeToString()
            Json.decodeFromString(UserCredentials.serializer(), jsonString)
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserCredentials, output: BufferedSink) {
        // 1. 纯 kotlinx.serialization 序列化
        val jsonString = Json.encodeToString(UserCredentials.serializer(), t)
        // 2. 纯 ByteArray 加密
        val encryptedBytes = cryptoManager.encrypt(jsonString.encodeToByteArray())
        // 3. 直接写入 Okio 管道
        output.write(encryptedBytes)
    }
}