package me.voltual.pyrolysis

import kotlin.jvm.Volatile

actual object ApiUrlProvider {
    @Volatile
    private var _apiBaseUrl: String = DefaultApiBaseUrl

    @Volatile
    private var _wanyueyunUploadApiBaseUrl: String = DefaultWanyueyunUploadApiBaseUrl

    actual var apiBaseUrl: String
        get() = _apiBaseUrl
        set(value) {
            _apiBaseUrl = value
        }

    actual var wanyueyunUploadApiBaseUrl: String
        get() = _wanyueyunUploadApiBaseUrl
        set(value) {
            _wanyueyunUploadApiBaseUrl = value
        }
}