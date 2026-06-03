半成品所以README暂无
下面是一些开发过程当乐子看吧
合并了https://github.com/rushiiMachine/arsc
的代码
合并了
https://github.com/duangsuse-valid-projects/SomeAXML
的代码
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
设置PREFER_PROJECT解决kotlinWasmBinaryenSetup找不到com.github.webassembly.binaryen的问题。
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