import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl 

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

group = "me.voltual.apkparser"
version = "1.0"

kotlin {
	jvm()  

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
	  
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
              	implementation("no.synth:kmp-zip:0.12.1")
			    implementation("no.synth:kmp-zip-kotlinx:0.12.1")
                implementation(project(":arsc"))
                implementation(project(":SomeAXML"))
                implementation(libs.kotlinx.io)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
//                implementation(kotlin("stdlib-jdk17"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(kotlin("reflect"))
            }
        }
        webMain.dependencies {
        }
    }
}