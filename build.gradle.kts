plugins {
    // Kotlin
    kotlin("multiplatform") version "2.2.0" apply false
    kotlin("plugin.serialization") version "2.2.0" apply false
    kotlin("android") version "2.2.0" apply false

    // Required since Kotlin 2.0 when Compose is enabled
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false

    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.10.0" apply false

    // Android app module
    id("com.android.application") version "8.13.2" apply false

    // ✅ Android-KMP library plugin for KMP shared module (replaces com.android.library)
    id("com.android.kotlin.multiplatform.library") version "8.13.2" apply false
}
