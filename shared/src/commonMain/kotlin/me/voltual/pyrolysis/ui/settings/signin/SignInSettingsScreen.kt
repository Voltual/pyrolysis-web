//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.settings.signin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.voltual.pyrolysis.core.ui.theme.BBQButton
import me.voltual.pyrolysis.core.ui.theme.SwitchWithText
import me.voltual.pyrolysis.core.ui.theme.BBQSnackbarHost
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInSettingsScreen(
    viewModel: SignInSettingsViewModel = koinViewModel(), // 使用 koinViewModel 自动注入
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val autoSignIn by viewModel.autoSignIn.collectAsState(initial = false)
    val signInState by viewModel.signInState.collectAsState()
    
    // 监听签到状态变化，显示Snackbar
    LaunchedEffect(signInState) {
        when (val state = signInState) {
            is SignInState.Success -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is SignInState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            is SignInState.Info -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 自动签到开关
            SwitchWithText(
                text = "开启自动签到",
                checked = autoSignIn,
                onCheckedChange = { checked ->
                    scope.launch {
                        viewModel.setAutoSignIn(checked)
                        // 如果开启自动签到，立即执行一次签到
                        if (checked) {
                            viewModel.signIn() // 干净的调用
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 签到按钮
                BBQButton(
                    onClick = {
                        viewModel.signIn() // 干净的调用
                    },
                    enabled = signInState !is SignInState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    text = {
                        if (signInState is SignInState.Loading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "立即签到",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                )
            }
        }
        
        // 显示Snackbar
        BBQSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}