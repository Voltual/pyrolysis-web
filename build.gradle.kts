// 根目录 build.gradle.kts
plugins {
    id("base") 
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

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
    
    configurations.all {
        resolutionStrategy {
            eachDependency {
                // 如果发现有第三方库（比如 Coil）偷偷引进了旧版 skiko
                if (requested.group == "org.jetbrains.skiko") {
                    // 强行把它转为框架目前正在使用的稳定版
                    useVersion("0.148.1")
                }
            }
        }
    }
}
tasks.named<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}