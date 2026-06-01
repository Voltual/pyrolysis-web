//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import android.content.Context
import android.content.Intent
import android.app.ActivityOptions
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.data.UserAgreementDataStore
import me.voltual.pyrolysis.core.database.LogEntry
import me.voltual.pyrolysis.core.database.LogDao
import me.voltual.pyrolysis.ui.*

// 导入 Android 专属界面，并使用别名避免与 NavKey 命名冲突
import me.voltual.pyrolysis.ui.plaza.AppPage as AppPageScreen
import me.voltual.pyrolysis.ui.plaza.ExplorePage
import me.voltual.pyrolysis.ui.plaza.SearchPage as SearchPageScreen
import me.voltual.pyrolysis.ui.plaza.SortFilterSheet as SortFilterSheetScreen
import me.voltual.pyrolysis.ui.settings.repos.PrefsReposPage as PrefsReposPageScreen
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerScreen

import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val agreementDataStore: UserAgreementDataStore by inject()
    private val authRepository: AuthRepository by inject()    
    
    companion object {
        private const val TAG = "NeoActivity"
        const val ACTION_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.action.UPDATES"
        const val ACTION_INSTALL = "${BuildConfig.APPLICATION_ID}.intent.action.INSTALL"
        const val EXTRA_UPDATES = "${BuildConfig.APPLICATION_ID}.intent.extra.UPDATES"
        const val EXTRA_CACHE_FILE_NAME = "${BuildConfig.APPLICATION_ID}.intent.extra.CACHE_FILE_NAME"
    }
    
    fun launchLockPrompt(action: () -> Unit) {
        // TODO: 待重新实现生物识别逻辑
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_BBQ_Main)
        super.onCreate(savedInstanceState)

        setContent {
            // 调用共享的 PyrolysisApp，并注入 Android 专属界面的渲染逻辑
            PyrolysisApp(
                agreementDataStore = agreementDataStore,
                platformEntryProvider = { key, navigator ->
                    when (key) {
                        is PrefsReposPage -> {
                            { PrefsReposPageScreen() }
                        }
                        is StoreManager -> {
                            { StoreManagerScreen() }
                        }
                        is AppPage -> {
                            { AppPageScreen(packageName = key.packageName, onDismiss = { navigator.goBack() }) }
                        }
                        is SearchPage -> {
                            { SearchPageScreen(onDismiss = { navigator.goBack() }) }
                        }
                        is Explore -> {
                            { ExplorePage() }
                        }
                        is SortFilterSheet -> {
                            { SortFilterSheetScreen(onDismiss = { navigator.goBack() }) }
                        }
                        else -> null
                    }
                }
            )
        }

        lifecycleScope.launch {
            delay(10000)
            val userCredentials = authRepository.credentials.first()
            if (userCredentials.token.isNotEmpty()) {
                startHeartbeatService(this@MainActivity, userCredentials.token)
            }
        }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val crashReport = getCrashReport(throwable)
            val logDao: LogDao by inject()
            CoroutineScope(Dispatchers.IO).launch {
                val logEntry = LogEntry(
                    type = "CRASH",
                    requestBody = "MainActivity 崩溃",
                    responseBody = crashReport,
                    status = "FAILURE"
                )
                logDao.insert(logEntry)
            }.invokeOnCompletion {
                CrashLogActivity.start(BBQApplication.instance, crashReport)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private fun getCrashReport(throwable: Throwable): String {
        val stackTrace = throwable.stackTraceToString()
        val deviceInfo = """
            设备型号: ${android.os.Build.MODEL}
            Android 版本: ${android.os.Build.VERSION.RELEASE}
            App 版本: ${BuildConfig.VERSION_NAME}
        """.trimIndent()
        return """
            崩溃信息: ${throwable.message}
            
            设备信息:
            $deviceInfo
            
            堆栈跟踪:
            $stackTrace
        """.trimIndent()
    }
}

fun startHeartbeatService(context: Context, token: String) {
    Intent(context, HeartbeatService::class.java).apply {
        putExtra("TOKEN", token)
        context.startService(this)
    }
}