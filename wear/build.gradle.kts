plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

// google-services.json non è nel repo: il plugin FCM si applica solo se il file c'è.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "it.pixelbox.cmwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.pixelbox.cmwatch"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        buildConfigField("boolean", "FIREBASE", file("google-services.json").exists().toString())
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }

    val keystorePath = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASS")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASS")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Il renderer delle tile cerca le sue risorse (`androidx.wear.protolayout.renderer.R$style`): senza, in CI
        // il test della tile cadeva con NoClassDefFoundError (run 34995385643, 15/09 18:35).
        unitTests.isIncludeAndroidResources = true
        // Traccia intera nel log della CI: Paparazzi gira solo lì (x86_64), e senza questo il NoClassDefFoundError
        // della tile non diceva quale classe mancava (15/09 18:21).
        unitTests.all { it.testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL; showCauses = true; showStackTraces = true } }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.fragment)
    implementation(libs.work.runtime)   // lintVital: registerForActivityResult vuole fragment ≥ 1.3
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.process)
    implementation(libs.concurrent.futures)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    debugImplementation(libs.wear.compose.ui.tooling)
    implementation(libs.protolayout)
    implementation(libs.protolayout.expression)
    implementation(libs.protolayout.material3)
    implementation(libs.tiles)
    implementation(libs.complications.datasource)
    implementation(libs.wear.runtime)
    implementation(libs.wear.ongoing)
    implementation(libs.wear.input)
    implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    // La tile disegnata dal codice nei test Paparazzi, per il README (Franz, 15/09 14:35).
    testImplementation(libs.tiles.renderer)
}
