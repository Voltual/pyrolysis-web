//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.pyrolysis.ui.community.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.core.ui.theme.BBQDropdownMenu
import me.voltual.pyrolysis.AuthRepository
import me.voltual.pyrolysis.core.ui.icons.drawable.IcMenuMessage // 导入消息图标
import me.voltual.pyrolysis.core.ui.icons.drawable.KakaoPage // 导入跳页图标
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.voltual.pyrolysis.core.ui.theme.BBQPullRefreshIndicator
import me.voltual.pyrolysis.ui.LocalTopAppBarController
import me.voltual.pyrolysis.ui.TopAppBarAction
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseComposeListScreen(
    title: String = "",
    currentRoute: String = "",
    posts: List<KtorClient.Post>,
    isLoading: Boolean,
    errorMessage: String,
    onItemClick: (KtorClient.Post) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    historyClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    totalPages: Int = 1,
    onJumpToPage: (Int) -> Unit = {},
    onNavigate: (String) -> Unit,
    onBackClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showJumpDialog by remember { mutableStateOf(false) }
    var inputPage by remember { mutableStateOf("") }
    var menuExpanded by mutableStateOf(false)
    val scope = rememberCoroutineScope()
    
    val authRepository: AuthRepository = koinInject()
    
    val topAppBarController = LocalTopAppBarController.current

    LaunchedEffect(title, menuExpanded, totalPages, currentRoute) {
        topAppBarController.titleContent = {
            val titleScrollState = rememberScrollState()
            Box(modifier = Modifier.wrapContentSize()) {
                Row(
                    modifier = Modifier
                        .wrapContentHeight()
                        .horizontalScroll(titleScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { menuExpanded = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = title, 
                            maxLines = 1,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                BBQDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    if (currentRoute != "community") {
                        DropdownMenuItem(text = { Text("社区") }, onClick = { menuExpanded = false; onNavigate("community") })
                    }
                    if (currentRoute != "hot_posts") {
                        DropdownMenuItem(text = { Text("热点") }, onClick = { menuExpanded = false; onNavigate("hot_posts") })
                    }
                    if (currentRoute != "following_posts") {
                        DropdownMenuItem(text = { Text("关注的人") }, onClick = { menuExpanded = false; onNavigate("following_posts") })
                    }
                    if (currentRoute != "my_likes") {
                        DropdownMenuItem(text = { Text("我喜欢的") }, onClick = { menuExpanded = false; onNavigate("my_likes") })
                    }
                    
                    if (!currentRoute.startsWith("my_posts")) {
                        DropdownMenuItem(
                            text = { Text("我的帖子") },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    val currentUserId = authRepository.userId.first()
                                    if (currentUserId > 0) onNavigate("my_posts/$currentUserId")
                                    else snackbarHostState.showSnackbar("请先登录")
                                }
                            }
                        )
                    }
                }
            }
        }

        topAppBarController.updateActions(
            listOf(
                TopAppBarAction(
                    icon = { tint -> Icon(imageVector = IcMenuMessage, "消息", tint = tint) },
                    description = "消息中心",
                    onClick = onMessageClick
                ),
                TopAppBarAction(
                    icon = { tint -> Icon(imageVector = KakaoPage, "跳页", tint = tint) },
                    description = "跳页",
                    onClick = { showJumpDialog = true; inputPage = "" }
                )
            )
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isLoading, errorMessage, posts) {
        if (!isLoading && isRefreshing) {
             isRefreshing = false
        }
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("从哪页开始呢~_~") },
            text = {
                Column {
                    Text("总页数: $totalPages", color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = inputPage,
                        onValueChange = { inputPage = it },
                        label = { Text("输入页码 (1-$totalPages)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val page = inputPage.toIntOrNull() ?: 1
                    if (page in 1..totalPages) onJumpToPage(page)
                    showJumpDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) { Text("取消") }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        },
        state = pullRefreshState,
        indicator = {
            BBQPullRefreshIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = modifier.fillMaxSize() 
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                errorMessage.isNotEmpty() -> ErrorView(errorMessage, onRefresh, Modifier.fillMaxSize())
                isLoading && posts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                posts.isEmpty() -> EmptyView(onRefresh, Modifier.fillMaxSize())
                else -> {
                    PostListComposable(
                        posts = posts,
                        isLoading = isLoading,
                        onItemClick = onItemClick,
                        onLoadMore = onLoadMore,
                        modifier = Modifier.fillMaxSize(),
                        isRefreshing = isRefreshing
                    )
                }
            }
            if (isLoading && posts.isNotEmpty() && !isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        } 
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
         Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = TextStyle(color = MaterialTheme.colorScheme.error), modifier = Modifier.padding(16.dp))
            Button(onClick = onRetry) { Text(text = "重试") }
        }
    }
}

@Composable
private fun EmptyView(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "暂无内容", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) { Text(text = "刷新") }
        }
    }
}