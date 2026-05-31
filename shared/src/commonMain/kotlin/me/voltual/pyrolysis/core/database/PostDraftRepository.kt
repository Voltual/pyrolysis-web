//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.pyrolysis.core.database

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import io.ktor.util.encodeBase64
import io.ktor.util.decodeBase64Bytes

/**
 * 表示草稿中的图片。
 * 兼容新选择的本地文件 [LocalFile] 以及从数据库恢复的内存字节数据 [LoadedBytes]。
 */
sealed interface DraftImage {
    val name: String
    suspend fun readBytes(): ByteArray

    data class LocalFile(val file: PlatformFile) : DraftImage {
        override val name: String get() = file.name
        override suspend fun readBytes(): ByteArray = file.readBytes()
    }

    data class LoadedBytes(override val name: String, val bytes: ByteArray) : DraftImage {
        override suspend fun readBytes(): ByteArray = bytes
    }
}

/**
 * 用于数据库持久化的图片数据结构
 */
@Serializable
data class SavedDraftImage(
    val name: String,
    val base64Data: String
)

@Single
class PostDraftRepository(
    private val postDraftDao: PostDraftDao
) {

    val draftFlow: Flow<DraftDto?> = postDraftDao.getDraft().map { entity ->
        entity?.let {
            val images = try {
                // 从存储的 JSON 中解析出图片列表并还原为字节
                val savedImages = Json.decodeFromString(
                    ListSerializer(SavedDraftImage.serializer()),
                    it.imageUris
                )
                savedImages.map { saved ->
                    DraftImage.LoadedBytes(
                        name = saved.name,
                        bytes = saved.base64Data.decodeBase64Bytes()
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }

            DraftDto(
                title = it.title,
                content = it.content,
                imageUris = images,
                imageUrls = it.imageUrls,
                subsectionId = it.subsectionId
            )
        }
    }

    suspend fun saveDraft(draft: DraftDto) {
        // 读取所有图片字节并转为 Base64
        val savedImages = draft.imageUris.map { draftImage ->
            SavedDraftImage(
                name = draftImage.name,
                base64Data = draftImage.readBytes().encodeBase64()
            )
        }
        val imagesJson = Json.encodeToString(
            ListSerializer(SavedDraftImage.serializer()),
            savedImages
        )

        val entity = PostDraft(
            title = draft.title,
            content = draft.content,
            imageUris = imagesJson, // 存入 JSON 字符串
            imageUrls = draft.imageUrls,
            subsectionId = draft.subsectionId
        )
        postDraftDao.save(entity)
    }

    suspend fun clearDraft() {
        postDraftDao.clear()
    }

    data class DraftDto(
        val title: String,
        val content: String,
        val imageUris: List<DraftImage>, // 更改为 List<DraftImage>
        val imageUrls: String,
        val subsectionId: Int
    )
}