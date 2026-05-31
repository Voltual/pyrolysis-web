//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlin.time.ExperimentalTime::class)
package me.voltual.pyrolysis.ui.plaza

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import me.voltual.pyrolysis.AppStore
import me.voltual.pyrolysis.KtorClient
import me.voltual.pyrolysis.feature.store.repository.IAppStoreRepository
import me.voltual.pyrolysis.feature.store.repository.XiaoQuRepository
import me.voltual.pyrolysis.data.unified.UnifiedAppReleaseParams
import me.voltual.apkparser.ApkParser
import me.voltual.apkparser.ApkMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.buffered
import kotlinx.io.write
import kotlinx.io.Source
import kotlin.time.Clock
import kotlin.math.roundToInt


class AppReleaseViewModel(
    private val xiaoQuRepo: XiaoQuRepository
) : ViewModel() {

    private val fileSystem = SystemFileSystem
    
    private val _selectedStore = MutableStateFlow(AppStore.XIAOQU_SPACE)
    val selectedStore = _selectedStore.asStateFlow()
    
    fun onStoreSelected(store: AppStore) {
        _selectedStore.value = store
        clearProcessFeedback()
    }

    private fun getCurrentRepo(): IAppStoreRepository = when (_selectedStore.value) {
        AppStore.XIAOQU_SPACE -> xiaoQuRepo
        else -> xiaoQuRepo
    }

    // --- 通用状态 ---
    val appName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val versionName = mutableStateOf("")
    val versionCode = mutableStateOf(0L)
    val appSize = mutableStateOf("") 
    val localIconFile = mutableStateOf<PlatformFile?>(null)
    val tempIconPath = mutableStateOf<Path?>(null)
    val tempApkPath = mutableStateOf<Path?>(null)
    
    // --- 小趣空间特定状态 ---
    val isUpdateMode = mutableStateOf(false)
    private var appId: Long = 0
    private var appVersionId: Long = 0
    val selectedApkUploadService = mutableStateOf(ApkUploadService.KEYUN)
    val appIntroduce = mutableStateOf("资源介绍【密码:】 ")
    val appExplain = mutableStateOf("适配性能描述 •\n包名：\n版本：")
    val isPay = mutableStateOf(0)
    val payMoney = mutableStateOf("")
    val selectedCategoryIndex = mutableStateOf(0)
    val apkDownloadUrl = mutableStateOf("") 
    val iconUrl = mutableStateOf<String?>(null) 
    val introductionImageUrls = mutableStateListOf<String>() 
    
    val categories = listOf(
        AppCategory(45, 47, "影音阅读"), AppCategory(45, 55, "音乐听歌"),
        AppCategory(45, 61, "休闲娱乐"), AppCategory(45, 58, "文件管理"),
        AppCategory(45, 59, "图像摄影"), AppCategory(45, 53, "输入方式"),
        AppCategory(45, 54, "生活出行"), AppCategory(45, 50, "社交通讯"),
        AppCategory(45, 56, "上网浏览"), AppCategory(45, 60, "其他类型"),
        AppCategory(45, 62, "跑酷竞技"), AppCategory(45, 49, "系统工具"),
        AppCategory(45, 48, "桌面插件"), AppCategory(45, 65, "学习教育")
    ).filter { it.categoryId != null && it.subCategoryId != null }
     .distinctBy { it.subCategoryId }
     .sortedBy { it.categoryName }

    // --- 弦开放平台特定状态 ---
    val appTypeOptions = listOf("手表应用", "手机应用", "大屏应用", "TV应用", "WearOS应用")
    val appTypeId = mutableStateOf(2) 
    val versionTypeOptions = listOf("官方版", "正式版", "测试版", "公测版", "美化版", "破解版", "修改版", "免费版", "定制版", "手表版")
    val appVersionTypeId = mutableStateOf(2) 
    val tagOptions = mutableStateListOf<String>() 
    val selectedTagIndex = mutableStateOf(0)
    val appTags = mutableStateOf(0) 
    val sdkMin = mutableStateOf(21)
    val sdkTarget = mutableStateOf(33)
    val developer = mutableStateOf("")
    val source = mutableStateOf("互联网")
    val describe = mutableStateOf("介绍一下你的应用…")
    val updateLog = mutableStateOf("本次更新的内容…")
    val uploadMessage = mutableStateOf("给审核员的留言…")
    val keyword = mutableStateOf("关键字")
    val isWearOs = mutableStateOf(0)
    val abi = mutableStateOf(0) 
    val screenshotsFiles = mutableStateListOf<PlatformFile>() 
    val tempScreenshotPaths = mutableStateListOf<Path>()

    val isApkUploading = mutableStateOf(false)
    val isIconUploading = mutableStateOf(false)
    val isIntroImagesUploading = mutableStateOf(false)
    val isReleasing = mutableStateOf(false)
    private val _processFeedback = MutableStateFlow<Result<String>?>(null)
    val processFeedback = _processFeedback.asStateFlow()
    
    private val MAX_INTRO_IMAGES_XIAOQU = 3

    fun addScreenshots(files: List<PlatformFile>) {       
        val currentCount = screenshotsFiles.size
        val remainingSlots = MAX_INTRO_IMAGES_XIAOQU - currentCount
        val filesToUpload = files.take(remainingSlots)
        
        screenshotsFiles.addAll(filesToUpload)
        
        viewModelScope.launch(Dispatchers.Default) {
            filesToUpload.forEach { file ->
                val now = Clock.System.now().toEpochMilliseconds()
                val path = fileToTempPath(file, "screenshot_${now}.png")
                path?.let { tempScreenshotPaths.add(it) }
            }
        }
    }
    
    fun removeScreenshot(file: PlatformFile) {
        val index = screenshotsFiles.indexOf(file)
        if (index != -1) {
            screenshotsFiles.removeAt(index)
            if (index < tempScreenshotPaths.size) tempScreenshotPaths.removeAt(index)
        }
    }

    /**
     * 重构后的 APK 解析与上传逻辑，使用 SystemTemporaryDirectory
     */
    fun parseAndUploadApk(file: PlatformFile) {
        viewModelScope.launch(Dispatchers.Default) {
            _processFeedback.value = Result.success("正在读取 APK 文件...")
            
            // 1. 将 PlatformFile 写入系统临时目录
            val now = Clock.System.now().toEpochMilliseconds()
            val apkPath = fileToTempPath(file, "release_${now}.apk")
            if (apkPath == null) {
                _processFeedback.value = Result.failure(Exception("无法创建临时 APK 文件"))
                return@launch
            }
            tempApkPath.value = apkPath

            // 2. 使用纯 Kotlin ApkParser 解析
            val metadata: ApkMetadata = try {
                // 必须在每次使用前重新打开 Source，因为 ApkParser 会消耗它
                val source = fileSystem.source(apkPath).buffered()
                ApkParser(source).parse()
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(Exception("APK 解析失败: ${e.message}"))
                return@launch
            }

            // 3. 计算文件大小
            val sizeInBytes = fileSystem.metadataOrNull(apkPath)?.size ?: 0L
            val sizeMb = (sizeInBytes / 1024.0 / 1024.0 * 100).roundToInt() / 100.0

            // 4. 提取图标
            var iconPath: Path? = null
            metadata.iconPath?.let { internalPath ->
                try {
                    // 再次打开 Source 提取图标字节
                    val apkSourceForIcon = fileSystem.source(apkPath).buffered()
                    val iconBytes = ApkParser.getFileBytes(apkSourceForIcon, internalPath)
                    if (iconBytes != null) {
                        // 同样写入系统临时目录
                        val iconTempPath = Path(SystemTemporaryDirectory, "icon_${now}.png")
                        fileSystem.sink(iconTempPath).buffered().use { it.write(iconBytes) }
                        iconPath = iconTempPath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 5. 更新 UI 状态
            withContext(Dispatchers.Main) {
                appName.value = metadata.label ?: "未知应用"
                packageName.value = metadata.packageName ?: ""
                versionName.value = metadata.versionName ?: "N/A"
                versionCode.value = metadata.versionCode ?: 0L
                appSize.value = sizeMb.toString()
                
                tempIconPath.value = iconPath
                appExplain.value = "适配性能描述 •\n包名：${metadata.packageName}\n版本：${metadata.versionName}"
            }

            // 6. 执行上传逻辑
            if (_selectedStore.value == AppStore.XIAOQU_SPACE) {
                executeXiaoQuUpload(apkPath, iconPath)
            } else {
                _processFeedback.value = Result.success("APK解析完成，准备发布")
            }
        }
    }

    private suspend fun executeXiaoQuUpload(apkPath: Path, iconPath: Path?) {
        val uploadJobs = mutableListOf<kotlinx.coroutines.Job>()

        uploadJobs += viewModelScope.launch(Dispatchers.Default) {
            isApkUploading.value = true
            val serviceType = when (selectedApkUploadService.value) {
                ApkUploadService.KEYUN -> "KEYUN"
                ApkUploadService.WANYUEYUN -> "WANYUEYUN"
            }
            val apkResult = xiaoQuRepo.uploadApk(apkPath, serviceType)
            apkResult.onSuccess { url ->
                apkDownloadUrl.value = url
            }.onFailure { e ->
                withContext(Dispatchers.Main) { _processFeedback.value = Result.failure(e) }
            }
            isApkUploading.value = false
        }

        iconPath?.let { path ->
            uploadJobs += viewModelScope.launch(Dispatchers.Default) {
                isIconUploading.value = true
                val imageResult = xiaoQuRepo.uploadImage(path, "icon")
                imageResult.onSuccess { url ->
                    iconUrl.value = url
                }.onFailure { e ->
                    withContext(Dispatchers.Main) { _processFeedback.value = Result.failure(e) }
                }
                isIconUploading.value = false
            }
        }
        uploadJobs.joinAll()
    }

    fun uploadIntroductionImages(files: List<PlatformFile>) {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) return
        
        viewModelScope.launch(Dispatchers.Default) {
            val currentCount = introductionImageUrls.size
            if (currentCount >= MAX_INTRO_IMAGES_XIAOQU) {
                _processFeedback.value = Result.failure(Exception("最多只能上传 $MAX_INTRO_IMAGES_XIAOQU 张介绍图"))
                return@launch
            }

            isIntroImagesUploading.value = true
            val remainingSlots = MAX_INTRO_IMAGES_XIAOQU - currentCount
            val filesToUpload = files.take(remainingSlots)

            val uploadJobs = filesToUpload.map { file ->
                launch {
                	val now = Clock.System.now().toEpochMilliseconds()
                    val tempFileName = "intro_${now}.jpg"
                    val path = fileToTempPath(file, tempFileName)
                    path?.let {
                        val imageResult = xiaoQuRepo.uploadImage(it, "intro")
                        imageResult.onSuccess { url ->
                            if (introductionImageUrls.size < MAX_INTRO_IMAGES_XIAOQU) {
                                introductionImageUrls.add(url)
                            }
                        }.onFailure { e ->
                            withContext(Dispatchers.Main) { _processFeedback.value = Result.failure(e) }
                        }
                    }
                }
            }
            uploadJobs.joinAll()
            isIntroImagesUploading.value = false
        }
    }

    fun clearProcessFeedback() { _processFeedback.value = null }

    /**
     * 写入系统临时路径
     */
    private suspend fun fileToTempPath(file: PlatformFile, fileName: String): Path? {
        return try {
            val bytes = file.readBytes()
            // 使用 kotlinx.io 提供的 SystemTemporaryDirectory
            val tempPath = Path(SystemTemporaryDirectory, fileName)
            
            fileSystem.sink(tempPath).buffered().use { sink ->
                sink.write(bytes)
            }
            tempPath
        } catch (e: Exception) { 
            e.printStackTrace()
            null 
        }
    }

    fun removeIntroductionImage(url: String) {
        introductionImageUrls.remove(url)
    }

    fun releaseApp(onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            isReleasing.value = true
            _processFeedback.value = Result.success("正在准备发布...")
            
            try {
                val params = UnifiedAppReleaseParams(
                    store = _selectedStore.value,
                    appName = appName.value,
                    packageName = packageName.value,
                    versionName = versionName.value,
                    versionCode = versionCode.value,
                    sizeInMb = appSize.value.toDoubleOrNull() ?: 0.0,
                    iconPath = tempIconPath.value,
                    iconUrl = iconUrl.value,
                    apkPath = tempApkPath.value,
                    apkUrl = apkDownloadUrl.value,
                    introduce = appIntroduce.value,
                    explain = appExplain.value,
                    introImages = introductionImageUrls.toList(),
                    categoryId = categories.getOrNull(selectedCategoryIndex.value)?.categoryId,
                    subCategoryId = categories.getOrNull(selectedCategoryIndex.value)?.subCategoryId,
                    isPay = isPay.value,
                    payMoney = if (isPay.value == 1) payMoney.value else "",
                    isUpdate = isUpdateMode.value,
                    appId = appId,
                    appVersionId = appVersionId,
                    appTypeId = appTypeId.value,
                    appVersionTypeId = appVersionTypeId.value,
                    appTags = appTags.value.toString(),
                    sdkMin = sdkMin.value,
                    sdkTarget = sdkTarget.value,
                    developer = developer.value,
                    source = source.value,
                    describe = describe.value,
                    updateLog = updateLog.value,
                    uploadMessage = uploadMessage.value,
                    keyword = keyword.value,
                    isWearOs = isWearOs.value,
                    abi = abi.value,
                    screenshots = tempScreenshotPaths.toList()
                )
                
                getCurrentRepo().releaseApp(params).onSuccess {
                    _processFeedback.value = Result.success("发布成功！")
                    withContext(Dispatchers.Main) { onSuccess() }
                }.onFailure { e ->
                    _processFeedback.value = Result.failure(e)
                }
            } catch (e: Exception) {
                _processFeedback.value = Result.failure(e)
            } finally {
                isReleasing.value = false
            }
        }
    }

    fun populateFromAppDetail(appDetail: KtorClient.AppDetail) {
        if (_selectedStore.value != AppStore.XIAOQU_SPACE) return
        isUpdateMode.value = true
        appId = appDetail.id
        appVersionId = appDetail.apps_version_id
        appName.value = appDetail.appname
        apkDownloadUrl.value = appDetail.download ?: ""
        packageName.value = appDetail.app_explain?.split("\n")?.find { it.startsWith("包名：") }?.substringAfter("包名：")?.trim() ?: ""
        versionName.value = appDetail.version
        versionCode.value = appDetail.apps_version_id 
        appSize.value = appDetail.app_size.replace("MB", "").trim()
        appIntroduce.value = appDetail.app_introduce?.replace("<br>", "\n") ?: ""
        appExplain.value = appDetail.app_explain ?: ""
        isPay.value = appDetail.is_pay
        payMoney.value = if (appDetail.pay_money > 0) appDetail.pay_money.toString() else ""
        selectedCategoryIndex.value = categories.indexOfFirst {
            it.categoryId == appDetail.category_id && it.subCategoryId == appDetail.sub_category_id
        }.takeIf { it != -1 } ?: 0
        iconUrl.value = appDetail.app_icon
        tempIconPath.value = null
        introductionImageUrls.clear()
        appDetail.app_introduction_image_array?.let { introductionImageUrls.addAll(it) }
    }
    
    fun setAppTypeId(id: Int) { appTypeId.value = id }
    fun setAppVersionTypeId(id: Int) { appVersionTypeId.value = id }
    fun setAppTags(id: Int) { appTags.value = id }
}