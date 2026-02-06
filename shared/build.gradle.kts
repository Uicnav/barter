import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")

    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")

    // Android-KMP plugin (în loc de com.android.library)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "com.barter.shared"
        compileSdk = 35
        minSdk = 24
        // Important: we intentionally DO NOT set jvmTarget here to avoid DSL incompatibilities.
        // We'll align JVM targets later after the project builds.
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Keep Compose accessors for now (warnings are OK)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                implementation("io.insert-koin:koin-core:4.1.1")

                implementation("io.ktor:ktor-client-core:3.4.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.4.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
                implementation("io.ktor:ktor-client-mock:3.4.0")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // Coil 3 — KMP image loading
                implementation("io.coil-kt.coil3:coil-compose:3.1.0")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.1.0")

                // FileKit — cross-platform file/image picker
                implementation("io.github.vinceglb:filekit-dialogs-compose:0.12.0")
            }
        }

        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation("androidx.core:core-ktx:1.15.0")
                implementation("io.ktor:ktor-client-okhttp:3.4.0")
                implementation("com.google.android.gms:play-services-location:21.3.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:3.4.0")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.4.0")
            }
        }
    }
}
