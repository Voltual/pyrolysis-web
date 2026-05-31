//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版...

package me.voltual.pyrolysis.core.proto

import kotlinx.serialization.Serializable

/**
 * 纯 Kotlin 伪造的 UserCredentials 实体类。
 * 摆脱了 Google Protobuf 对 JVM 的依赖，确保 KMP 全平台（iOS/Desktop/Wasm）顺利编译。
 * 支持 kotlinx.serialization 序列化。
 */
@Serializable
class UserCredentials internal constructor(
    val username: String,
    val password: String,
    val token: String,
    val userId: Long,
    val deviceId: String,
    val sineMarketToken: String,
    val sineOpenMarketToken: String,
    val lingMarketToken: String
) {
    // 模拟 Protobuf 的 toBuilder()
    fun toBuilder(): Builder = Builder(this)

    // 模拟 Protobuf 的 Builder 内部类
    class Builder {
        private var username = ""
        private var password = ""
        private var token = ""
        private var userId = 0L
        private var deviceId = ""
        private var sineMarketToken = ""
        private var sineOpenMarketToken = ""
        private var lingMarketToken = ""

        constructor()
        
        constructor(credentials: UserCredentials) {
            this.username = credentials.username
            this.password = credentials.password
            this.token = credentials.token
            this.userId = credentials.userId
            this.deviceId = credentials.deviceId
            this.sineMarketToken = credentials.sineMarketToken
            this.sineOpenMarketToken = credentials.sineOpenMarketToken
            this.lingMarketToken = credentials.lingMarketToken
        }

        fun setUsername(value: String) = apply { this.username = value }
        fun setPassword(value: String) = apply { this.password = value }
        fun setToken(value: String) = apply { this.token = value }
        fun setUserId(value: Long) = apply { this.userId = value }
        fun setDeviceId(value: String) = apply { this.deviceId = value }
        fun setSineMarketToken(value: String) = apply { this.sineMarketToken = value }
        fun setSineOpenMarketToken(value: String) = apply { this.sineOpenMarketToken = value }
        fun setLingMarketToken(value: String) = apply { this.lingMarketToken = value }

        fun build(): UserCredentials = UserCredentials(
            username = username,
            password = password,
            token = token,
            userId = userId,
            deviceId = deviceId,
            sineMarketToken = sineMarketToken,
            sineOpenMarketToken = sineOpenMarketToken,
            lingMarketToken = lingMarketToken
        )
    }

    companion object {
        private val DEFAULT_INSTANCE = UserCredentials("", "", "", 0L, "", "", "", "")
        
        // 模拟 Protobuf 的 getDefaultInstance()
        fun getDefaultInstance(): UserCredentials = DEFAULT_INSTANCE
    }
}