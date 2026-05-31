// androidMain 中的 PlatformUtils.kt
package me.voltual.pyrolysis.util

import android.os.Build

actual fun getAndroidDeviceInfo(): String {
    val brand = Build.BRAND.uppercase()
    val model = Build.MODEL
    return "\n系统定制商：$brand\n设备型号：$model"
}