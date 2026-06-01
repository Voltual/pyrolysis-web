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
                implementation(project(":shared"))
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
        }
    }
}