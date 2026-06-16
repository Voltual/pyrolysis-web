@file:OptIn(org.koin.core.annotation.KoinExperimentalAPI::class)

package me.voltual.pyrolysis

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.voltual.pyrolysis.di.*
import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.*
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.core.ui.theme.ThemeManager
import me.voltual.pyrolysis.data.content.Preferences
import me.voltual.pyrolysis.feature.store.repository.privacyModule
import me.voltual.pyrolysis.feature.store.worker.WorkerManager
import me.voltual.pyrolysis.feature.store.worker.workmanagerModule
import me.voltual.pyrolysis.manager.network.downloadClientModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinApplication
import me.voltual.pyrolysis.manager.installer.*
import org.koin.dsl.koinConfiguration
import java.lang.ref.WeakReference

@KoinApplication
class BBQApplication : Application(), KoinStartup {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val wm: WorkerManager by inject()
    val db: FdroidDatabase by inject()
    val installer: AppInstaller by inject() 
    
    val themeStore: ThemeColorDataStore by inject() 

    lateinit var database: AppDatabase
        private set

    private var activityRef: WeakReference<AppCompatActivity> = WeakReference(null)

    override fun onCreate() {
        super.onCreate()
        instance = this

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is AppCompatActivity) {
                    activityRef = WeakReference(activity)
                }
            }
            override fun onActivityPaused(activity: Activity) {
                if (activityRef.get() == activity) {
                    activityRef.clear()
                }
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        Preferences.init(this)
        wm.prune()           
        
        runBlocking {
            ThemeManager.updateCustomColors(themeStore.colorsFlow.first())
        }
    }
    
	@Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")
    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@BBQApplication)
        modules(
                androidAppModule,  
            downloadClientModule,
            workmanagerModule,
            databaseModule,
            privacyModule,
            installerModule,
            commonModule,      // KMP 共享的通用模块
        platformModule,    // KMP 共享的 Android 平台实现
        )
    }

    companion object {
        lateinit var instance: BBQApplication
            private set

        var mainActivity: AppCompatActivity?
            get() = if (::instance.isInitialized) instance.activityRef.get() else null
            set(_) {} 

        val wm: WorkerManager get() = instance.wm
        val db: FdroidDatabase get() = instance.db
        val context: Context get() = instance
        val installer: AppInstaller get() = instance.installer 
        
        val latestSyncs: MutableMap<Long, Long> = mutableMapOf()
    }
}