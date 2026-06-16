瞎写的
合并了https://github.com/rushiiMachine/arsc
的代码
合并了
https://github.com/duangsuse-valid-projects/SomeAXML
的代码
（这两个项目的io被改成kotlinx.io作为项目的模块用于在KMP中解析apk信息）
吸取了
https://slack-chats.kotlinlang.org/t/32605455/how-can-i-configure-kmp-to-stop-downloading-the-nodejs-and-y
的教训添加了
allprojects {
    project.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().download = false
    }
    project.plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        project.the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().download = false
    }
    rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        project.the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().download = false
    }
}
（这倒不是什么网络问题，是因为插件自动下载就算网络好的也找不到下载失败）
设置PREFER_PROJECT解决kotlinWasmBinaryenSetup找不到com.github.webassembly.binaryen的问题。
（如果是RepositoriesMode.PREFER_SETTINGS，Gradle只能在项目配置的什么Maven中心仓库找结果到处找不到，还不如设置成PREFER_PROJECT让插件自己随便找）
在gradle.properties中kotlin.js.yarn=false，让org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin'不要自动下载Yarn啊（我根本没有用）
agp升级到9.0:
The 'org.jetbrains.kotlin.android' plugin in project ':android' is no longer required for Kotlin support since AGP 9.0.
64
Solution: Remove both `android.builtInKotlin=true` and `android.newDsl=false` from `gradle.properties`, then migrate to built-in Kotlin.
翻译过来是:
你用了 AGP 9.0，老子不需要这个插件了。请立刻把 android.builtInKotlin 删掉，转用内置 Kotlin。

KSP is not compatible with Android Gradle Plugin's built-in Kotlin. Please disable by adding android.builtInKotlin=false to gradle.properties and apply kotlin("android") plugin
翻译:
KSP不兼容，请去 gradle.properties 加上 android.builtInKotlin=false 并引入 kotlin.android 插件。

然后你就会陷入死循环

(✘_✘)毁灭吧世界
这是我发的帖子。不过真正的解决方案是提升ksp版本🙄，所以后来我成功升级到agp9了。但是光看报错日志确实一头雾水

一些androidx库，实际JetBrains都会在后续提供跨平台方案比如
material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version = "1.7.3" }
material-icons-core = { module = "org.jetbrains.compose.material:material-icons-core", version = "1.7.3" }
jetbrains-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "multiplatform-nav3-ui" }

jetbrains-material3-adaptiveNavigation3 = { module = "org.jetbrains.compose.material3.adaptive:adaptive-navigation3", version.ref = "compose-multiplatform-adaptive" }
jetbrains-lifecycle-viewmodelNavigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "compose-multiplatform-lifecycle" }


eruda是个好东西🙃

wasmjs的字体加载参考了kotlinconf
用preloadFont在ComposeViewport中进行非阻塞的字体预加载

配置了coi-serviceworker.js使room3正常工作不会触发SQLiteException: ut.oo1.OpfsDb is not a constructor
加入了sqlite3-opfs-async-proxy.js使room3正常工作
npx esbuild worker.js --bundle --minify --format=esm --outfile=sqlite.worker.js
然后private fun createWasmWorker(): Worker = 
    js("new Worker('sqlite.worker.js', { type: 'module' })")
    接着.setDriver(WebWorkerSQLiteDriver(worker)) // 传入打上 module 标签的 worker
    
    必须在wasmjs目标配置useEsModules()！
wasmJs {
        browser()
        useEsModules()
    }
    不然浏览器不认识sqlite.worker.js
    
    
尝试参考了https://slack-chats.kotlinlang.org/t/29888854/anyone-here-successfully-use-coil-image-library-with-wasmjs-
并参考https://github.com/coil-kt/coil/blob/main/samples/shared/src/wasmJsMain/kotlin/sample/common/imageLoader.wasmJs.kt
参考kotlinconf实现coil拦截器，不要像kotlinconf用import coil3.SingletonImageLoader
一定要用import coil3.compose.setSingletonImageLoaderFactory来初始化啊啊啊啊啊！
正确修复wasmjs的coil加载图片    

啊啊啊啊。就算是代理，coil本身没有那么聪明coil只认绝对url，不会识别相对url路径!!

Platform.kt的实现参考了kotlinconf