//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>
package me.voltual.pyrolysis.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler

@Composable
actual fun PlayerScreen(viewModel: PlayerViewModel, onBack: () -> Unit) {
    val uiState by viewModel.playerUiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator()
            }
            is PlayerUiState.Success -> {
                // 在非 Android 平台，直接使用 UriHandler 打开浏览器播放
                LaunchedEffect(state.videoUrl) {
                    uriHandler.openUri(state.videoUrl)
                    // 自动返回上一页，因为播放已经交给系统浏览器
                    onBack()
                }
                Text("正在尝试在浏览器中打开视频...", color = Color.White)
            }
            is PlayerUiState.Error -> {
                Text("错误: ${state.message}", color = Color.Red)
            }
        }
    }
}