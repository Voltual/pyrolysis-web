//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.voltual.pyrolysis.Res 
import me.voltual.pyrolysis.core.ui.theme.AppShapes
import me.voltual.pyrolysis.data.UserAgreementDataStore
import org.koin.compose.koinInject
import me.voltual.pyrolysis.core.ui.animation.materialSharedAxisX
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
    shape: Shape = AppShapes.medium,
    onAgreed: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val agreementDataStore: UserAgreementDataStore = koinInject()
    
    var currentAgreementIndex by remember { mutableStateOf(0) }
    val agreementContents = remember { mutableStateMapOf<Int, String>() }
    var animationForward by remember { mutableStateOf(true) }

    // 1. 使用基于纯字符串路径的 KMP 资源项声明
    val agreements = remember {
        listOf(
            AgreementItem("《本项目 用户协议》", "files/useragreement.txt"),
            AgreementItem("《小趣空间用户协议》", "files/xiaoquuseragreement.txt"),
        )
    }

    // 2. 使用 KMP 统一的内部异步方法读取文件，不依赖 Android Context
    LaunchedEffect(Unit) {
        agreements.forEachIndexed { index, item ->
            agreementContents[index] = try {
                // Res.readBytes 是平台无关的挂起函数，由 Compose 运行时在各平台底层自主实现
                val bytes = Res.readBytes(item.resourcePath)
                bytes.decodeToString()
            } catch (e: Exception) {
                "条款内容加载失败，请检查网络或重启应用"
            }
        }
    }

    Dialog(
        // 留空：即使用户点击外面或者按 Android 物理返回键，也绝对无法关闭
        onDismissRequest = { }, 
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false, // 禁用 Android 物理返回键
            dismissOnClickOutside = false // 禁用点击外部关闭
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val mainScrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(mainScrollState)
                    .padding(24.dp)
            ) {
                Text(
                    text = "服务协议与隐私政策",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "请阅读并同意以下条款以继续使用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = currentAgreementIndex,
                    transitionSpec = {
                        materialSharedAxisX(forward = animationForward, slideDistance = 30)
                    },
                    label = "AgreementTransition"
                ) { targetIndex ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = agreements[targetIndex].title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MarkDownText(
                            content = agreementContents[targetIndex] ?: "正在加载...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentAgreementIndex > 0) {
                        FilledTonalButton(
                            onClick = {
                                animationForward = false
                                currentAgreementIndex--
                                scope.launch { mainScrollState.animateScrollTo(0) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("上一个")
                        }
                    } else {
                        // 如果不需要退出，这里可以放一个“不同意”按钮用于展示纯文本提示
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    // 巧妙的小心思：如果用户点击了不同意，把第一页的文本直接改成提示语
                                    agreementContents[0] = """
                                        ### 您需要同意才能使用
                                        
                                        很抱歉，如果您不同意本项目的《用户协议》与《小趣空间用户协议》，应用将无法服务。
                                        
                                        如果您希望退出应用，请直接**关闭此后台程序**或**返回手机桌面**。
                                    """.trimIndent()
                                    mainScrollState.animateScrollTo(0)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("不同意")
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                saveAgreement(agreementDataStore, currentAgreementIndex)
                                if (currentAgreementIndex < agreements.size - 1) {
                                    animationForward = true
                                    currentAgreementIndex++
                                    mainScrollState.animateScrollTo(0)
                                } else {
                                    onAgreed()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (currentAgreementIndex < agreements.size - 1) "同意并继续" else "同意并进入",
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// 移除了 Android 独有的 R 引用，改为通用路径
private data class AgreementItem(val title: String, val resourcePath: String)

// 利用 KMP 分发的 DataStore 异步保存状态
private suspend fun saveAgreement(ds: UserAgreementDataStore, index: Int) {
    when (index) {
        0 -> ds.acceptUserAgreement()
        1 -> ds.acceptXiaoquAgreement()
    }
}