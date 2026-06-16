package me.voltual.pyrolysis

/**
 * 动态 API 基址提供者。
 * 允许在应用运行时根据用户设置动态更新 API 请求的目标服务器。
 */
object ApiUrlProvider {
    @Volatile
    var apiBaseUrl: String = DefaultApiBaseUrl

    @Volatile
    var wanyueyunUploadApiBaseUrl: String = DefaultWanyueyunUploadApiBaseUrl
}