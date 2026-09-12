plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "it.pixelbox.cmwatch.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "it.pixelbox.cmwatch.mobile"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
}
