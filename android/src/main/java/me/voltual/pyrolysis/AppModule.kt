//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis

import androidx.room3.*
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.core.database.dao.*
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.core.proto.UserCredentials
import me.voltual.pyrolysis.data.*
import me.voltual.pyrolysis.feature.store.repository.*
import me.voltual.pyrolysis.feature.store.worker.workmanagerModule
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.home.HomeViewModel
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.ui.plaza.*
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.rank.RankingListViewModel
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.settings.PrefsVM
import me.voltual.pyrolysis.ui.settings.repos.RepoPageVM
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsViewModel
import me.voltual.pyrolysis.ui.settings.storage.StoreManagerViewModel
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel
import me.voltual.pyrolysis.ui.user.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// 常量定义，集中管理命名空间限定符
private val AUTH_STORE_QUALIFIER = named("auth_store")
private val DRAFT_STORE_QUALIFIER = named("draft_store")
private val PAYMENT_STORE_QUALIFIER = named("payment_store")
private val PLAZA_STORE_QUALIFIER = named("plaza_store")
private val USER_FILTER_STORE_QUALIFIER = named("user_filter_store")
private val USER_AGREEMENT_STORE_QUALIFIER = named("user_agreement_store")
private val UPDATE_SETTINGS_STORE_QUALIFIER = named("update_settings_store")
private val STORAGE_SETTINGS_STORE_QUALIFIER = named("storage_settings_store")
private val SIGN_IN_SETTINGS_STORE_QUALIFIER = named("sign_in_settings_store")
private val SEARCH_HISTORY_STORE_QUALIFIER = named("search_history_store")
private val PLAYER_SETTINGS_STORE_QUALIFIER = named("player_settings_store")
private val DRAWER_MENU_STORE_QUALIFIER = named("drawer_menu_store")
private val DEVICE_INFO_STORE_QUALIFIER = named("device_info_store")
private val THEME_SETTINGS_STORE_QUALIFIER = named("theme_settings_store")

val appModule = module {

// =========================================================================
// 1. 核心基础设施 & 数据库 (Core Infrastructure & Database)
// =========================================================================

    single<AppDatabase> { 
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "pyrolysis_database"
        )
        .setDriver(BundledSQLiteDriver()) // Room 3 强制要求设置驱动
        .addMigrations(*AppDatabase.ALL_MIGRATIONS)
        .build()
    }
// 2. 所有的 Dao 统一由 Koin 容器管理，它们会自动等待上面的 AppDatabase 构建完成后注入完成
single { get<AppDatabase>().logDao() }  
single { get<AppDatabase>().browseHistoryDao() } 
single { get<AppDatabase>().postDraftDao() }         

    // =========================================================================
    // 2. 底层 DataStore 实例定义 (Platform Specific Instances)
    // =========================================================================
    
    // Preferences DataStores 物理文件定义
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
        THEME_SETTINGS_STORE_QUALIFIER to "theme_settings.preferences_pb" // 新增
    )

    storeFiles.forEach { (qualifier, fileName) ->
        single<DataStore<Preferences>>(qualifier) {
            PreferenceDataStoreFactory.create(
                produceFile = { androidContext().dataStoreFile(fileName) }
            )
        }
    }

    // =========================================================================
    // 3. 业务 DataStore 包装类 (Business Logic Wrappers)
    // =========================================================================
    
    single { ThemeColorDataStore(get(THEME_SETTINGS_STORE_QUALIFIER)) }
    single { DeviceNameDataStore(get(DEVICE_INFO_STORE_QUALIFIER)) }
    single { UserFilterDataStore(get(USER_FILTER_STORE_QUALIFIER)) }    
    single { UserAgreementDataStore(get(USER_AGREEMENT_STORE_QUALIFIER)) }    
    single { SearchHistoryDataStore(get(SEARCH_HISTORY_STORE_QUALIFIER)) }
    single { StorageSettingsDataStore(get(STORAGE_SETTINGS_STORE_QUALIFIER)) }
    single { PostDraftDataStore(get(DRAFT_STORE_QUALIFIER)) } 
    single { UpdateSettingsDataStore(get(UPDATE_SETTINGS_STORE_QUALIFIER)) }
    single { SignInSettingsDataStore(get(SIGN_IN_SETTINGS_STORE_QUALIFIER)) }
    single { PlayerDataStore(get(PLAYER_SETTINGS_STORE_QUALIFIER)) }
    single { DrawerMenuDataStore(get(DRAWER_MENU_STORE_QUALIFIER)) }

    // =========================================================================
    // 4. 业务仓库层 (Repositories)
    // =========================================================================
    
    single { AuthRepository(get(AUTH_STORE_QUALIFIER)) }
    single { PostDraftRepository(get()) }
    single { BrowseHistoryRepository(get()) }
    single { LogRepository(get()) }
    single { InstallsRepository(get()) }  
    single { ExtrasRepository(get()) }    
    single { DownloadedRepository(get()) }    
    single { InstalledRepository(get(), get()) }
    single { RepositoriesRepository(get(), get(), get()) }
    single { ProductsRepository(get(), get(), get(), get()) }
    single { PrivacyRepository(get(), get(), get(), get(), get()) }
    single { XiaoQuRepository(KtorClient.ApiServiceImpl, get()) }
    
    single<Map<AppStore, IAppStoreRepository>> {
        mutableMapOf<AppStore, IAppStoreRepository>().apply {
            put(AppStore.XIAOQU_SPACE, get<XiaoQuRepository>())
        }
    }

    // =========================================================================
    // 5. 表现层 (ViewModels)
    // =========================================================================
    
    viewModel { LoginViewModel(get()) } 
    viewModel { UserProfileViewModel(get(), get()) }
    viewModel { UserListViewModel(get()) }
    viewModel { UserDetailViewModel(get()) }
    viewModel { SignInSettingsViewModel(get(),get()) }

    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(get()) } 
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(get()) } 
    viewModel { MyPostsViewModel(get()) }
    viewModel { MyCommentsViewModel(get()) }
    viewModel { MyReviewsViewModel(get()) }
    viewModel { PostCreateViewModel(get(), get(), get()) }
    viewModel { PostDetailViewModel(get(),get()) } 
    viewModel { BrowseHistoryViewModel(get()) }

    viewModel { HomeViewModel(get(),get()) }
    viewModel { PlazaViewModel(get(PLAZA_STORE_QUALIFIER),get()) }
    viewModel { AppDetailComposeViewModel(get()) }
    viewModel { AppReleaseViewModel(get()) }
    viewModel { VersionListViewModel(get()) }
    viewModel { AppPageVM(get(), get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { SearchVM(get(), get(), get()) }    
    viewModel { ExploreVM(get(), get(), get()) }
    viewModel { RepoPageVM(get(), get()) }
    viewModel { RankingListViewModel() }

    viewModel { BillingViewModel(get()) }
    viewModel { PaymentViewModel(get(), get(PAYMENT_STORE_QUALIFIER)) }
    viewModel { MessageViewModel(get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { LogViewModel(get()) }
    viewModel { StoreManagerViewModel(androidApplication(),get()) }
    viewModel { UpdateSettingsViewModel(get()) }
    viewModel { PrefsVM(get(), get(), get()) }
}