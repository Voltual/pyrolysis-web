// wasmjs/build.gradle.kts
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "pyrolysis-wasm"
        browser()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        
        wasmJsMain.dependencies {
            // 依赖共享的 shared 核心业务模块
            implementation(project(":shared"))
            
            // Compose Multiplatform Web 核心依赖
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            
            // Koin 依赖注入框架核心依赖（用于 main.kt 中的 startKoin）
            implementation(libs.koin.core)
            
            // 协程支持
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}