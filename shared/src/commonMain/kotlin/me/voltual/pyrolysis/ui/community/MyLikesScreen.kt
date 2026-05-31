//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.community

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.voltual.pyrolysis.ui.*
import me.voltual.pyrolysis.ui.community.compose.BaseComposeListScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLikesScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MyLikesViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.loadInitialData()
        }
    }

    BaseComposeListScreen(
        title = "我喜欢的",
        currentRoute = "my_likes",
        posts = posts,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onItemClick = { post -> navigator.navigate(PostDetail(post.postid)) },
        onLoadMore = { viewModel.loadNextPage() },
        onRefresh = { viewModel.refresh() },
        onSearchClick = { navigator.navigate(Search(userId = null, nickname = null)) },
        onCreateClick = { navigator.navigate(CreatePost) },
        snackbarHostState = snackbarHostState,
        historyClick = { navigator.navigate(BrowseHistory) },
        totalPages = totalPages,
        onJumpToPage = { page -> viewModel.jumpToPage(page) },
        onMessageClick = { navigator.navigate(MessageCenter) },
        onNavigate = { route ->
            when {
                route == "community" -> navigator.navigate(Community)
                route == "hot_posts" -> navigator.navigate(HotPosts)
                route == "following_posts" -> navigator.navigate(FollowingPosts)
                route == "my_likes" -> navigator.navigate(MyLikes)
                route.startsWith("my_posts/") -> {
                    val parts = route.removePrefix("my_posts/").split("/")
                    val userIdStr = parts.firstOrNull()
                    val nickname = if (parts.size > 1) parts[1] else "我"
                    userIdStr?.toLongOrNull()?.let { userId ->
                        navigator.navigate(MyPosts(userId, nickname))
                    }
                }
            }
        },
        onBackClick = { navigator.goBack() }
    )
}