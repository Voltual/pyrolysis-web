package me.voltual.pyrolysis.core.proto

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.random.CryptographyRandom

/**
 * 跨平台加解密管理器，基于 cryptography-kotlin 库。
 * 采用 PBKDF2 派生 256 位密钥，并以 AES-GCM 模式进行高强度对称加密。
 */
class CryptoManager {
    private val provider = CryptographyProvider.Default
    
    // 用于本地存储加密的固定派生源密码
    private val passwordBytes = "pyrolysis-secure-datastore-secret-key".encodeToByteArray()

    /**
     * 加密数据，输出格式为: [16字节Salt] + [AES-GCM密文(包含IV和Tag)]
     */
    suspend fun encrypt(plaintext: ByteArray): ByteArray {
        val salt = CryptographyRandom.nextBytes(16)
        val derivedKeyBytes = provider.get(PBKDF2).secretDerivation(
            digest = SHA256,
            iterations = 100_000,
            outputSize = 256.bits,
            salt = salt
        ).deriveSecretToByteArray(passwordBytes)

        val aesGcm = provider.get(AES.GCM)
        val aesKey = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes)
        val ciphertext = aesKey.cipher().encrypt(plaintext)

        return salt + ciphertext
    }

    /**
     * 解密数据，输入格式为: [16字节Salt] + [AES-GCM密文(包含IV和Tag)]
     */
    suspend fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.size < 16) {
            throw IllegalArgumentException("密文长度不足以读取Salt")
        }
        val salt = encryptedData.copyOfRange(0, 16)
        val ciphertext = encryptedData.copyOfRange(16, encryptedData.size)

        val derivedKeyBytes = provider.get(PBKDF2).secretDerivation(
            digest = SHA256,
            iterations = 100_000,
            outputSize = 256.bits,
            salt = salt
        ).deriveSecretToByteArray(passwordBytes)

        val aesGcm = provider.get(AES.GCM)
        val aesKey = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, derivedKeyBytes)
        return aesKey.cipher().decrypt(ciphertext)
    }
}