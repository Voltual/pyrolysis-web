// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU Affero 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU Affero 通用公共许可证。
//
// 你应该已经收到了一份 GNU Affero 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import kotlinx.coroutines.launch
import me.voltual.pyrolysis.shared.BuildConfig 
import me.voltual.pyrolysis.core.ui.components.MarkDownText
import me.voltual.pyrolysis.core.ui.icons.drawable.* import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime

private fun openKtorUrlWithHandler(uriHandler: UriHandler, urlString: String) {
    val ktorUrl = Url(urlString)
    uriHandler.openUri(ktorUrl.toString())
}

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState 
) {
    val app_name = "Pyrolysis"
    val uriHandler = LocalUriHandler.current 
    
    // 直接使用 BuildKonfig 的变量
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE
    val scope = rememberCoroutineScope() 
    
    @OptIn(kotlin.time.ExperimentalTime::class)
    val currentYear = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .year
    val copyrightText = if (currentYear > 2025) "2025 - $currentYear" else "2025"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = Fire,
            contentDescription = app_name,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp)),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = app_name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "版本 $versionName (Build $versionCode)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        LicenseCard(
            onLicenseClick = {
                runCatching {
                    openKtorUrlWithHandler(uriHandler, "https://gitee.com/Voltula/bbq/blob/master/LICENSE")
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(message = "无法打开浏览器") }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OpenSourceCard(
            onGiteeClick = {
                runCatching {
                    openKtorUrlWithHandler(uriHandler, "https://gitee.com/Voltula/bbq")
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(message = "无法打开浏览器") }
                }
            },
            onReleasesClick = {
                runCatching {
                    openKtorUrlWithHandler(uriHandler, "https://gitee.com/Voltula/bbq/releases/")
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(message = "无法打开浏览器") }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DeveloperCard()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        AcknowledgmentsCard(uriHandler = uriHandler)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "© $copyrightText Voltula. 保留所有权利。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LicenseCard(onLicenseClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "许可证",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本程序采用 GNU Affero 通用公共许可证第3版 (AGPLv3) 发布。您可以自由地使用、修改和分发本程序，包括在网络服务中提供远程互动，但必须遵守其条款开源相关源码。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClickableTextItem(
                text = "查看项目使用的AGPLv3许可证副本",
                onClick = onLicenseClick
            )
        }
    }
}

@Composable
fun OpenSourceCard(onGiteeClick: () -> Unit, onReleasesClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "开源与下载",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            ClickableTextItem(text = "项目源码 (Gitee)", onClick = onGiteeClick)
            Spacer(modifier = Modifier.height(8.dp))
            ClickableTextItem(text = "下载可安装的APK包 (Releases)", onClick = onReleasesClick)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "提示：如果您不了解如何编译代码，请点击上方\"下载可安装的APK包\"链接，下载后缀为 .apk 的文件进行安装。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "本页面基于 Bilimiao 项目的代码修改而来，受上游组件影响，整体遵循 AGPLv3 协议。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DeveloperCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "开发者信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "开发者(owner): Voltula (Voltual)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "项目目前暂无其他贡献者",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickableTextItem(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = IcArrowRight,
                contentDescription = "打开链接",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AcknowledgmentsCard(uriHandler: UriHandler) {
    val acknowledgmentsMd = """
        本项目的上游与参考项目（或者说是它们的衍生作品）:
        
        * **鸣谢开源项目**:
        * **哔哩终端 (GPLv3)**: [Gitee](https://gitee.com/RobinNotBad/BiliClient)
        * **RikkaHub (AGPLv3)**: [GitHub](https://github.com/rikkahub/rikkahub/)
        * **bilimiao (GPLv3)**: [GitHub](https://github.com/10miaomiao/bilimiao2)
        * **Droid-ify (GPLv3)**: [GitHub](https://github.com/Droid-ify/client)
        * **Neo Store (GPLv3)**: [GitHub](https://github.com/NeoApplications/Neo-Store)
    """.trimIndent()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "鸣谢名单",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            MarkDownText(
                content = acknowledgmentsMd,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClickCitation = { url ->
                    runCatching {
                        openKtorUrlWithHandler(uriHandler, url)
                    }
                }
            )
        }
    }
}