plugins {
    id("com.android.application")
    kotlin("android")

    // Compose plugins
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.barter.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.barter.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }

    // ✅ Make Java & Kotlin target the same JVM (17)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    // ✅ Kotlin JVM target = 17 (matches compileOptions above)
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.ui)
    implementation(compose.material3)
    implementation("androidx.activity:activity-compose:1.9.3")

    // FileKit — needed for FileKit.init(this) in MainActivity
    implementation("io.github.vinceglb:filekit-dialogs:0.12.0")

    // Koin — needed for platform DI module in MainActivity
    implementation("io.insert-koin:koin-core:4.1.1")
}
