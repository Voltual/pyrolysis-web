package me.voltual.pyrolysis.di

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import me.voltual.pyrolysis.core.database.AppDatabase
import androidx.room3.RoomDatabase
import me.voltual.pyrolysis.core.proto.createDataStore
import me.voltual.pyrolysis.core.proto.createPreferenceDataStore
import me.voltual.pyrolysis.core.proto.UserCredentials
import org.koin.core.module.Module
import org.koin.dsl.module

private fun createWasmWorker(): Worker = 
    js("new Worker('sqlite.worker.js', { type: 'module' })")

actual val platformModule: Module = module {
    // 1. WasmJS 专属的 Room 数据库构建
    single<AppDatabase> { 
        val worker = createWasmWorker()

        Room.databaseBuilder<AppDatabase>(
            name = "pyrolysis_database"
        )
        .setDriver(WebWorkerSQLiteDriver(worker)) // 传入打上 module 标签的 worker
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    // 2. 加密型 UserCredentials DataStore
    single<DataStore<UserCredentials>>(AUTH_STORE_QUALIFIER) {
        createDataStore(get(), null)
    }

    // 3. 13 个 Preferences DataStore
    val storeFiles = mapOf(
        DRAFT_STORE_QUALIFIER to "post_drafts.preferences_pb",
        PAYMENT_STORE_QUALIFIER to "payment_requests.preferences_pb",
        PLAZA_STORE_QUALIFIER to "plaza_preferences.preferences_pb",
        USER_FILTER_STORE_QUALIFIER to "user_filter.preferences_pb",
        USER_AGREEMENT_STORE_QUALIFIER to "user_agreement_prefs.preferences_pb",
        UPDATE_SETTINGS_STORE_QUALIFIER to "update_settings.preferences_pb",
        STORAGE_SETTINGS_STORE_QUALIFIER to "storage_settings.preferences_pb",
        SIGN_IN_SETTINGS_STORE_QUALIFIER to "sign_in_settings.preferences_pb",
        SEARCH_HISTORY_STORE_QUALIFIER to "search_history.preferences_pb",
        PLAYER_SETTINGS_STORE_QUALIFIER to "player_settings.preferences_pb",
        DRAWER_MENU_STORE_QUALIFIER to "settings.preferences_pb",
        DEVICE_INFO_STORE_QUALIFIER to "device_info.preferences_pb",
        THEME_SETTINGS_STORE_QUALIFIER to "theme_settings.preferences_pb"
    )

    storeFiles.forEach { (qualifier, fileName) ->
        single<DataStore<Preferences>>(qualifier) {
            createPreferenceDataStore(fileName, null)
        }
    }
}