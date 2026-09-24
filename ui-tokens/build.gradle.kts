plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// I colori comuni a orologio e telefono (design 24/09, «Aspetto e movimento»): una sola fonte, così non divergono.
android {
    namespace = "it.pixelbox.cmwatch.ui.tokens"
    compileSdk = 36
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui.graphics)
}
