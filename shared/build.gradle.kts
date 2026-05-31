// shared/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl 

plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("com.github.gmazzo.buildconfig") version "5.3.0"
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.kotlin.serialization)
}

buildConfig {
    useKotlinOutput()
    packageName("me.voltual.pyrolysis")
    
    // 直接在这里写死，简单粗暴且完全可行
    buildConfigField("VERSION_NAME", "22.1")
    buildConfigField("VERSION_CODE", 511) 
}

kotlin {
    // AGP 9.0 KMP 库专用 Android 配置块
    android {
        namespace = "me.voltual.pyrolysis.shared"
        compileSdk = 37
        minSdk = 24 // 直接设置，不需要 defaultConfig
        
        // 开启 Android 资源支持（如果以后要放图片、字符串到 shared）
        androidResources {
            enable = true
        }

        // 替代旧的 compileOptions 和 kotlinOptions
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)              
                implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
                implementation(libs.jetbrains.material3.adaptiveNavigation3)
                implementation(libs.compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.markdown)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
                implementation(libs.room3.runtime)
                implementation(libs.sqlite)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.io)
                implementation(project(":ApkParser"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.components.resources)
                implementation(libs.kotlinx.datetime)
                implementation(libs.koin.core)
                implementation(libs.koin.annotations)
                implementation(libs.material.icons.core)
                implementation(libs.material.icons.extended)
                
                // FileKit
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)
                
                // DataStore library (这里的datastore一定要+"-core"并且得是1.3.0-alpha01之后才支持wasm+js)
                implementation("androidx.datastore:datastore-core:1.3.0-alpha09")
                // The Preferences DataStore library
                implementation("androidx.datastore:datastore-preferences-core:1.3.0-alpha09")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ijkplayer)
                implementation(libs.koin.android.compose)
                implementation(libs.sqlite.bundled)
                implementation(project(":DanmakuFlameMaster"))
                implementation(libs.compose.adaptive)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }

        wasmJsMain.dependencies {
            implementation(libs.navigation3.browser) 
            implementation(libs.sqlite.web)
            implementation(libs.kotlinx.io)             
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "me.voltual.pyrolysis"
    generateResClass = always
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // 💡 关键修改：彻底删除了引发错误的 kspCommonMainMetadata 这一行
    // 仅为具体的目标平台单独配置 Room KSP 编译器
    add("kspAndroid", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}