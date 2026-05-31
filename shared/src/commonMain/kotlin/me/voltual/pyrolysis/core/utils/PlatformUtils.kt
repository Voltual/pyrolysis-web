package me.voltual.pyrolysis.util

/**
 * 获取安卓特有的设备定制商和型号信息
 * 如果是非安卓平台，应返回空字符串，避免影响文本排版
 */
expect fun getAndroidDeviceInfo(): String