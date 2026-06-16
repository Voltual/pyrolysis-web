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
        useEsModules()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting
        
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            
            // 修复：补齐 Compose 资源加载核心依赖，使 Wasm 壳工程能解析 FontResource
            implementation(libs.components.resources)
            implementation(libs.coil.compose.wasm)
            implementation(libs.coil.network.ktor.wasm)                
            
            implementation(libs.koin.core)
            implementation(libs.compose.navigation3)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}