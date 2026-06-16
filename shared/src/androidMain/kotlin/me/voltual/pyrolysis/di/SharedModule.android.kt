package me.voltual.pyrolysis.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import me.voltual.pyrolysis.core.database.AppDatabase
import me.voltual.pyrolysis.core.proto.createDataStore
import me.voltual.pyrolysis.core.proto.createPreferenceDataStore
import me.voltual.pyrolysis.core.proto.UserCredentials
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    // 1. Android 专属的 Room 数据库构建
    single<AppDatabase> { 
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "pyrolysis_database"
        )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .build()
    }

    // 2. 加密型 UserCredentials DataStore
    single<DataStore<UserCredentials>>(AUTH_STORE_QUALIFIER) {
        createDataStore(get(), androidContext())
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
        THEME_SETTINGS_STORE_QUALIFIER to "theme_settings.preferences_pb",
        PROXY_SETTINGS_STORE_QUALIFIER to "proxy_settings_store.preferences_pb"
    )

    storeFiles.forEach { (qualifier, fileName) ->
        single<DataStore<Preferences>>(qualifier) {
            createPreferenceDataStore(fileName, androidContext())
        }
    }
}