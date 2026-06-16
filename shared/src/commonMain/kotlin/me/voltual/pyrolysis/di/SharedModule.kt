package me.voltual.pyrolysis.di

import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.core.database.*
import me.voltual.pyrolysis.core.proto.CryptoManager
import me.voltual.pyrolysis.core.proto.UserCredentialsSerializer
import me.voltual.pyrolysis.data.*
import me.voltual.pyrolysis.ui.auth.LoginViewModel
import me.voltual.pyrolysis.ui.billing.BillingViewModel
import me.voltual.pyrolysis.ui.plaza.*
import me.voltual.pyrolysis.ui.community.*
import me.voltual.pyrolysis.ui.home.HomeViewModel
import me.voltual.pyrolysis.ui.log.LogViewModel
import me.voltual.pyrolysis.ui.message.MessageViewModel
import me.voltual.pyrolysis.ui.player.PlayerViewModel
import me.voltual.pyrolysis.ui.rank.RankingListViewModel
import me.voltual.pyrolysis.ui.search.SearchViewModel
import me.voltual.pyrolysis.ui.settings.signin.SignInSettingsViewModel
import me.voltual.pyrolysis.ui.settings.update.UpdateSettingsViewModel
import me.voltual.pyrolysis.ui.settings.proxy.ProxySettingsViewModel
import me.voltual.pyrolysis.ui.user.*
import me.voltual.pyrolysis.core.ui.theme.ThemeColorDataStore
import me.voltual.pyrolysis.ui.payment.PaymentViewModel
import me.voltual.pyrolysis.feature.store.repository.XiaoQuRepository
import me.voltual.pyrolysis.feature.store.repository.IAppStoreRepository
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.KtorClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// 限定符定义
val AUTH_STORE_QUALIFIER = named("auth_store")
val DRAFT_STORE_QUALIFIER = named("draft_store")
val PAYMENT_STORE_QUALIFIER = named("payment_store")
val PLAZA_STORE_QUALIFIER = named("plaza_store")
val USER_FILTER_STORE_QUALIFIER = named("user_filter_store")
val USER_AGREEMENT_STORE_QUALIFIER = named("user_agreement_store")
val UPDATE_SETTINGS_STORE_QUALIFIER = named("update_settings_store")
val PROXY_SETTINGS_STORE_QUALIFIER = named("proxy_settings_store")
val STORAGE_SETTINGS_STORE_QUALIFIER = named("storage_settings_store")
val SIGN_IN_SETTINGS_STORE_QUALIFIER = named("sign_in_settings_store")
val SEARCH_HISTORY_STORE_QUALIFIER = named("search_history_store")
val PLAYER_SETTINGS_STORE_QUALIFIER = named("player_settings_store")
val DRAWER_MENU_STORE_QUALIFIER = named("drawer_menu_store")
val DEVICE_INFO_STORE_QUALIFIER = named("device_info_store")
val THEME_SETTINGS_STORE_QUALIFIER = named("theme_settings_store")

val commonModule = module {
    // 加密核心组件
    single { CryptoManager() }
    single { UserCredentialsSerializer(get()) }

    // 数据库 Dao 注册
    single { get<AppDatabase>().logDao() }  
    single { get<AppDatabase>().browseHistoryDao() } 
    single { get<AppDatabase>().postDraftDao() }
    
    single<Map<AppStore, IAppStoreRepository>> {
        mutableMapOf<AppStore, IAppStoreRepository>().apply {
            put(AppStore.XIAOQU_SPACE, get<XiaoQuRepository>())
        }
    }

    // 业务 DataStore 包装类
    single { ThemeColorDataStore(get(THEME_SETTINGS_STORE_QUALIFIER)) }
    single { DeviceNameDataStore(get(DEVICE_INFO_STORE_QUALIFIER)) }
    single { UserFilterDataStore(get(USER_FILTER_STORE_QUALIFIER)) }    
    single { UserAgreementDataStore(get(USER_AGREEMENT_STORE_QUALIFIER)) }    
    single { SearchHistoryDataStore(get(SEARCH_HISTORY_STORE_QUALIFIER)) }
    single { StorageSettingsDataStore(get(STORAGE_SETTINGS_STORE_QUALIFIER)) }
    single { PostDraftDataStore(get(DRAFT_STORE_QUALIFIER)) } 
    single { UpdateSettingsDataStore(get(UPDATE_SETTINGS_STORE_QUALIFIER)) }
    single { ProxySettingsDataStore(get(PROXY_SETTINGS_STORE_QUALIFIER)) }
    single { SignInSettingsDataStore(get(SIGN_IN_SETTINGS_STORE_QUALIFIER)) }
    single { PlayerDataStore(get(PLAYER_SETTINGS_STORE_QUALIFIER)) }
    single { DrawerMenuDataStore(get(DRAWER_MENU_STORE_QUALIFIER)) }

    // 业务仓库层
    single { AuthRepository(get(AUTH_STORE_QUALIFIER)) }
    single { PostDraftRepository(get()) }
    single { BrowseHistoryRepository(get()) }
    single { LogRepository(get()) }
    single { XiaoQuRepository(KtorClient.ApiServiceImpl, get()) }

    // 共享 ViewModels
    viewModel { LoginViewModel(get()) } 
    viewModel { UserProfileViewModel(get(), get()) }
    viewModel { UserListViewModel(get()) }
    viewModel { UserDetailViewModel(get()) }
    viewModel { PaymentViewModel(get(), get(PAYMENT_STORE_QUALIFIER)) }    
    viewModel { SignInSettingsViewModel(get(), get()) }

    viewModel { CommunityViewModel() }
    viewModel { FollowingPostsViewModel(get()) } 
    viewModel { HotPostsViewModel() }
    viewModel { MyLikesViewModel(get()) } 
    viewModel { MyPostsViewModel(get()) }
    viewModel { MyCommentsViewModel(get()) }
    viewModel { MyReviewsViewModel(get()) }
    viewModel { PostCreateViewModel(get(), get(), get()) }
    viewModel { PostDetailViewModel(get(), get()) } 
    viewModel { BrowseHistoryViewModel(get()) }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { PlazaViewModel(get(PLAZA_STORE_QUALIFIER), get()) }
    viewModel { AppDetailComposeViewModel(get()) }
    viewModel { AppReleaseViewModel(get()) }
    viewModel { VersionListViewModel(get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { RankingListViewModel() }

    viewModel { BillingViewModel(get()) }
    viewModel { MessageViewModel(get()) }
    viewModel { PlayerViewModel(get()) }
    viewModel { LogViewModel(get()) }
    viewModel { UpdateSettingsViewModel(get()) }
    viewModel { ProxySettingsViewModel(get()) }
}

expect val platformModule: Module