plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "it.pixelbox.cmwatch.core"
    compileSdk = 36
    defaultConfig { minSdk = 33 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    testOptions { unitTests.isReturnDefaultValues = true }
}

room { schemaDirectory("$projectDir/schemas") }

// Le fixture del contratto entrano nell'APK come assets/contract/*.json (usate dal FakeTransport).
val contractAssets = layout.buildDirectory.dir("generated/contractAssets")
val copyContract by tasks.registering(Copy::class) {
    from(rootProject.file("contract")) { include("*.json") }
    into(contractAssets.map { it.dir("contract") })
}
android.sourceSets["main"].assets.srcDir(contractAssets)
tasks.named("preBuild") { dependsOn(copyContract) }

// I test leggono le fixture da ../contract mentre girano: senza questo input, una modifica alle sole fixture lasciava il
// task «aggiornato», i test non ripartivano e restavano i risultati vecchi (un verde falso il 14/09 alle 23:15).
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("contract")).withPropertyName("contractFixtures").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
