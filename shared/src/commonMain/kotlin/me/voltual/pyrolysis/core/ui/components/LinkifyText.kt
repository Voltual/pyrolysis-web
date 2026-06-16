// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
@file:Suppress("DEPRECATION")
package me.voltual.pyrolysis.core.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import io.ktor.http.Url
import me.voltual.pyrolysis.ui.*

/**
 * 内部帖子链接正则 (Kotlin Regex)
 */
private val INTERNAL_POST_LINK_REGEX = Regex("""http://apk\.xiaoqu\.online/post/(\d+)\.html""")

/**
 * B站视频链接正则 (Kotlin Regex)
 */
private val BILI_VIDEO_LINK_REGEX = Regex("""【视频：([a-zA-Z0-9]+)】""")

/**
 * 通用 URL 正则 (Kotlin Regex)
 */
private val GENERAL_URL_REGEX = Regex(
    """(?:(?:https?|ftp)://|www\.)[\w\-_]+(?:\.[\w\-_]+)+(?:[\w\-.,@?^=%&:/~+#]*[\w\-@?^=%&;/~+#])?"""
)

private data class LinkMatch(
    val range: IntRange,
    val text: String,
    val type: LinkType
)

private enum class LinkType {
    POST,
    BILIVIDEO,
    URL
}

/**
 * 自动识别文本中的帖子链接、B站视频标记和普通URL，并使其可点击。
 * - 已移除 java.util.regex 依赖，全面适配 Kotlin Regex。
 * - 使用 Navigation 3 的 LocalNavigator 进行内部导航。
 * - 使用 Ktor Http Url 与 LocalUriHandler 替换 Android 原生 Uri/Intent，全面适配 KMP。
 */
@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current 
    val linkColor = MaterialTheme.colorScheme.primary

    // 初始化跨平台的 B站视频点击处理器
    val biliVideoHandler = rememberBiliVideoHandler(navigator, uriHandler)

    val textStyle = if (style.color == Color.Unspecified) {
        style.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        style
    }
        
    val annotatedString = remember(text, linkColor) {
        val processedText = text.replace("<br>", "\n")
        buildAnnotatedString {
            append(processedText)

            val postMatches = INTERNAL_POST_LINK_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.groups[1]?.value ?: "",
                    type = LinkType.POST
                )
            }

            val biliMatches = BILI_VIDEO_LINK_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.groups[1]?.value ?: "",
                    type = LinkType.BILIVIDEO
                )
            }

            val urlMatches = GENERAL_URL_REGEX.findAll(processedText).map { result ->
                LinkMatch(
                    range = result.range,
                    text = result.value,
                    type = LinkType.URL
                )
            }

            val allMatches = (postMatches + biliMatches + urlMatches)
                .sortedBy { it.range.first }
                .fold(mutableListOf<LinkMatch>()) { acc, current ->
                    if (acc.none { current.range.first in it.range }) {
                        acc.add(current)
                    }
                    acc
                }

            allMatches.forEach { match ->
                addStyle(
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = match.type.name,
                    annotation = match.text,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    SelectionContainer(modifier = modifier) {
        ClickableText(
            text = annotatedString,
            style = textStyle,
            onClick = { offset ->
                // 1. 处理内部帖子链接
                annotatedString.getStringAnnotations(tag = LinkType.POST.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        annotation.item.toLongOrNull()?.let { postId ->
                            navigator.navigate(PostDetail(postId))
                        }
                        return@ClickableText
                    }

                // 2. 处理B站视频链接 (已使用 expect/actual 差异化)
                annotatedString.getStringAnnotations(tag = LinkType.BILIVIDEO.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        biliVideoHandler.handle(annotation.item)
                        return@ClickableText
                    }

                // 3. 处理普通URL
                annotatedString.getStringAnnotations(tag = LinkType.URL.name, start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val urlString = annotation.item.let {
                            if (!it.startsWith("http://") && !it.startsWith("https://")) "http://$it" else it
                        }
                        
                        runCatching {
                            val ktorUrl = Url(urlString)
                            uriHandler.openUri(ktorUrl.toString())
                        }
                    }
            }
        )
    }
}