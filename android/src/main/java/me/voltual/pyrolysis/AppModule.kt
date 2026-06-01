//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版

package me.voltual.pyrolysis

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import me.voltual.pyrolysis.core.database.FdroidDatabase
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.feature.store.repository.*
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.plaza.AppPageVM
import me.voltual.pyrolysis.ui.plaza.ExploreVM
import me.voltual.pyrolysis.ui.plaza.SearchVM
import me.voltual.pyrolysis.ui.settings.PrefsVM
import me.voltual.pyrolysis.ui.settings.repos.RepoPageVM
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerViewModel
import me.voltual.pyrolysis.di.PAYMENT_STORE_QUALIFIER
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    // =========================================================================
    // 1. Android 独有数据库 FdroidDatabase
    // =========================================================================
    single<FdroidDatabase> { 
        Room.databaseBuilder(
            androidContext(),
            FdroidDatabase::class.java,
            "fdroid_database"
        )
        .setDriver(BundledSQLiteDriver())
        .build()
    }

    single { get<FdroidDatabase>().antiFeatureDao() }
    single { get<FdroidDatabase>().categoryDao() }
    single { get<FdroidDatabase>().downloadStatsDao() }
    single { get<FdroidDatabase>().downloadStatsFileDao() }
    single { get<FdroidDatabase>().downloadedDao() }
    single { get<FdroidDatabase>().exodusInfoDao() }
    single { get<FdroidDatabase>().extrasDao() }
    single { get<FdroidDatabase>().installTaskDao() }
    single { get<FdroidDatabase>().installedDao() }
    single { get<FdroidDatabase>().productDao() }
    single { get<FdroidDatabase>().rbLogDao() }
    single { get<FdroidDatabase>().releaseDao() }
    single { get<FdroidDatabase>().repoCategoryDao() }
    single { get<FdroidDatabase>().repositoryDao() }
    single { get<FdroidDatabase>().trackerDao() }

    // =========================================================================
    // 2. Android 独有业务仓库层
    // =========================================================================
    single { InstallsRepository(get()) }  
    single { ExtrasRepository(get()) }    
    single { DownloadedRepository(get()) }    
    single { InstalledRepository(get(), get()) }
    single { RepositoriesRepository(get(), get(), get()) }
    single { ProductsRepository(get(), get(), get(), get()) }
    single { PrivacyRepository(get(), get(), get(), get(), get()) }

    // =========================================================================
    // 3. Android 独有 ViewModels
    // =========================================================================
    viewModel { AppPageVM(get(), get(), get(), get(), get(), get()) }
    viewModel { SearchVM(get(), get(), get()) }    
    viewModel { ExploreVM(get(), get(), get()) }
    viewModel { RepoPageVM(get(), get()) }
    viewModel { PaymentViewModel(get(), get(PAYMENT_STORE_QUALIFIER)) }
    viewModel { StoreManagerViewModel(androidApplication(), get()) }
    viewModel { PrefsVM(get(), get(), get()) }
}