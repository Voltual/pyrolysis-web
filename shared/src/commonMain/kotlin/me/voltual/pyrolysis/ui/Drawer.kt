//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import me.voltual.pyrolysis.core.ui.icons.Phosphor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import me.voltual.pyrolysis.core.ui.icons.phosphor.*
import me.voltual.pyrolysis.core.ui.icons.drawable.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey        
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.AuthRepository 
import me.voltual.pyrolysis.data.DrawerMenuDataStore
import me.voltual.pyrolysis.getPlatformId
import org.koin.compose.koinInject 

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class Remote(val url: String) : IconSource()
}

data class DrawerItem(
    val id: String, 
    val label: String,
    val icon: IconSource, 
    val route: AppDestination
)

@Composable
fun DrawerHeader(modifier: Modifier = Modifier, backgroundUri: String?) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        if (backgroundUri != null) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = "Drawer Header Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NavigationDrawerItems(
    navigator: Navigator,
    currentTopLevelRoute: NavKey?,           
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    val authRepository: AuthRepository = koinInject()
    val platformId = remember { getPlatformId() }

    val allDrawerItems = remember(platformId) {
        val items = mutableListOf(
            DrawerItem("home", "首页", IconSource.Vector(IcMenuHome), Home),
            DrawerItem("resources", "资源广场", IconSource.Vector(IcMenuApps), ResourcePlaza(isMyResource = false)),
            DrawerItem("community", "交流社区", IconSource.Vector(IcMenuCommunity), Community),
            DrawerItem("messages", "消息中心", IconSource.Vector(IcMenuMessage), MessageCenter),
            DrawerItem("ranking_list", "天梯竞赛", IconSource.Vector(IcMenuRanking), RankingList),
            DrawerItem("release_app", "发布应用", IconSource.Vector(Bg), CreateAppRelease),
            DrawerItem("bot_logs", "日志", IconSource.Vector(WorkLog), LogViewer),
            DrawerItem("update_settings", "更新设置", IconSource.Vector(Asusupdate), UpdateSettings)
        )

        // 仅在 Android 平台下将仓库相关、存储管理、网络代理加入侧边栏
        if (platformId == "android") {
            items.add(DrawerItem("repos_search", "仓库搜索", IconSource.Vector(Phosphor.MagnifyingGlass), SearchPage))
            items.add(DrawerItem("prefsrepos", "仓库管理", IconSource.Vector(Phosphor.Graph), PrefsReposPage))
            items.add(DrawerItem("store_manager", "存储管理", IconSource.Vector(Appbackuprestore), StoreManager))
            items.add(DrawerItem("explore", "仓库探索", IconSource.Vector(Phosphor.Compass), Explore))
            items.add(DrawerItem("proxy_settings", "网络代理", IconSource.Vector(Phosphor.GlobeSimple), ProxySettings))
        }

        items.addAll(
            listOf(
                DrawerItem("settings", "主题设置", IconSource.Vector(IcMenuSettings), ThemeCustomize),
                DrawerItem("signin_settings", "签到设置", IconSource.Vector(SignIn), SignInSettings),
                DrawerItem("login", "登录账号", IconSource.Vector(IcMenuLogin), Login),
                DrawerItem("logout", "退出登录", IconSource.Vector(IcMenuLogout), Home)
            )
        )
        items
    }
    val allItemsMap = remember(allDrawerItems) { allDrawerItems.associateBy { it.id } }

    var orderedItems by remember { mutableStateOf<List<DrawerItem>>(emptyList()) }
    var draggedItem by remember { mutableStateOf<DrawerItem?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeight by remember { mutableStateOf(0) }
    val drawerMenuDataStore: DrawerMenuDataStore = koinInject()

    var selectedItemId by remember { mutableStateOf("home") }

    LaunchedEffect(allDrawerItems) {
        val savedOrder = drawerMenuDataStore.loadMenuOrder().first()
        orderedItems = if (savedOrder.isEmpty()) {
            allDrawerItems
        } else {
            val ordered = savedOrder.mapNotNull { allItemsMap[it] }
            val newItems = allDrawerItems.filter { it.id !in savedOrder }
            ordered + newItems
        }
    }

    LaunchedEffect(currentTopLevelRoute, orderedItems) {
        currentTopLevelRoute?.let { currentRoute ->
            val matchedItem = orderedItems.find { it.route == currentRoute && it.id != "logout" }
            if (matchedItem != null && matchedItem.id != selectedItemId) {
                selectedItemId = matchedItem.id
            }
        }
    }

    val placeholderIndex by remember(draggedItem, dragOffsetY) {
        derivedStateOf {
            draggedItem?.let {
                val initialIndex = orderedItems.indexOf(it)
                val displacement = (dragOffsetY / itemHeight).toInt()
                (initialIndex + displacement).coerceIn(0, orderedItems.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(orderedItems, key = { it.id }) { item ->
                val isBeingDragged = item.id == draggedItem?.id
                val index = orderedItems.indexOf(item)
                val showPlaceholder = placeholderIndex == index && placeholderIndex != orderedItems.indexOf(draggedItem)

                if (showPlaceholder) {
                    if (placeholderIndex!! > orderedItems.indexOf(draggedItem)) {
                        ItemContent(
                            item = item,
                            selectedItemId = selectedItemId,
                            onItemClick = { selectedItemId = it },
                            isDragged = false,
                            scope = scope,
                            drawerState = drawerState,
                            navigator = navigator,
                            authRepository = authRepository
                        )
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                    } else {
                        PlaceholderItem(modifier = Modifier.onSizeChanged { itemHeight = it.height })
                        ItemContent(
                            item = item,
                            selectedItemId = selectedItemId,
                            onItemClick = { selectedItemId = it },
                            isDragged = false,
                            scope = scope,
                            drawerState = drawerState,
                            navigator = navigator,
                            authRepository = authRepository
                        )
                    }
                } else {
                    ItemContent(
                        item = item,
                        selectedItemId = selectedItemId,
                        onItemClick = { selectedItemId = it },
                        isDragged = isBeingDragged,
                        scope = scope,
                        drawerState = drawerState,
                        navigator = navigator,
                        authRepository = authRepository,
                        modifier = Modifier
                            .onSizeChanged { itemHeight = it.height }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggedItem = item },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                    },
                                    onDragEnd = {
                                        placeholderIndex?.let { toIndex ->
                                            val fromIndex = orderedItems.indexOf(draggedItem!!)
                                            if (fromIndex != toIndex) {
                                                val newList = orderedItems.toMutableList().apply {
                                                    add(toIndex, removeAt(fromIndex))
                                                }
                                                orderedItems = newList
                                                scope.launch {
                                                    drawerMenuDataStore.saveMenuOrder(newList.map { it.id })
                                                }
                                            }
                                        }
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItem = null
                                        dragOffsetY = 0f
                                    }
                                )
                            }
                    )
                }
            }
        }

        draggedItem?.let { item ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = dragOffsetY
                        shadowElevation = 8f
                    }
                    .padding(horizontal = 12.dp)
            ) {
                ItemContent(
                    item = item,
                    selectedItemId = selectedItemId,
                    onItemClick = { selectedItemId = it },
                    isDragged = false,
                    scope = scope,
                    drawerState = drawerState,
                    navigator = navigator,
                    authRepository = authRepository
                )
            }
        }
    }
}

@Composable
private fun ItemContent(
    item: DrawerItem,
    selectedItemId: String,
    onItemClick: (String) -> Unit,
    isDragged: Boolean,
    scope: CoroutineScope,
    drawerState: DrawerState,
    navigator: Navigator,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedItemId == item.id

    NavigationDrawerItem(
        label = { Text(item.label) },
        icon = {
            val iconModifier = Modifier.size(24.dp)
            when (val source = item.icon) {
                is IconSource.Vector -> Icon(source.imageVector, null, modifier = iconModifier)
                is IconSource.Remote -> AsyncImage(
                    model = source.url,
                    contentDescription = null,
                    modifier = iconModifier
                )
            }
        },
        selected = isSelected,
        onClick = {
            onItemClick(item.id)
            
            scope.launch { drawerState.close() }
            when (item.id) {
                "logout" -> {
                    scope.launch {
                        authRepository.clearCredentials()
                        navigator.logoutAndReset()   
                    }
                }
                "login" -> {
                    navigator.navigate(Login)
                }
                else -> {
                    navigator.navigate(item.route)
                }
            }
        },
        modifier = modifier
            .padding(vertical = 4.dp)
            .graphicsLayer { alpha = if (isDragged) 0f else 1f },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun PlaceholderItem(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {}
}