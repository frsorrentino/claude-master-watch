# App polso — piano di implementazione (fasi 2-5)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** l'app nativa Wear OS `it.pixelbox.cmwatch` che mostra le sessioni Claude Code del PC sul Pixel Watch 5 e risponde alle domande dal polso, sul `Transport` finto finché il relay (fase 1, repo claude-master) non c'è, poi su Firebase.

**Architecture:** C+ del design: modello di dominio = contratto `contract/*.json` decodificato con kotlinx.serialization; un `Repo` che tiene l'ultimo `/state` in Room e lo espone come `StateFlow`; un'interfaccia `Transport` con due implementazioni (`FakeTransport` sulle fixture, `FirebaseTransport` REST+SSE); UI Wear Compose Material 3 con un solo `ViewState` a priorità `offline > domanda > schermata scelta`; tile, complication e notifiche leggono lo stesso `Repo`.

**Tech Stack:** Kotlin 2.3.21 · AGP 8.13.2 · Gradle 9.0.0 · Compose BOM 2026.06.01 · Wear Compose Material 3 1.6.2 (`TransformingLazyColumn`, `ScreenScaffold`, `TimeText`) · ProtoLayout M3 1.4.2 + Tiles 1.6.2 · `watchface-complications-data-source-ktx` 1.3.0 · Room 2.8.5 (KSP 2.3.12) · kotlinx.serialization 1.11.0 · OkHttp 5.4.0 (REST + SSE) · Firebase BoM 34.19.0 (messaging, auth anonima) · DataStore 1.2.1 · `wear-ongoing` 1.1.0 · `wear-input` 1.1.0 · JUnit 4 · Paparazzi 1.3.5 (solo in Actions).

## Global Constraints

- Design approvato: `docs/plans/2026-09-12-app-polso-design.md`. Non si ridiscute; se qualcosa non torna, una frase e una proposta.
- Contratto v1 = `contract/*.json`, letti identici dai test; regole: `state` ≤ 8 KB; `outcome.short` ≤ 60; `outcome.full` ≤ 600; `question.text` intero; `options[].n` da 1 senza buchi; `tier` ∈ low|medium|high; `state` ∈ waiting|busy|idle|awaiting|gone; ordine waiting, busy, idle, gone, poi alfabetico. Cambiare il contratto = messaggio alle sessioni `claude-master` e `master`, mai modifica unilaterale.
- Bus: ogni documento `{"v":1,"enc":"<base64>"}` = AES-256-GCM sul JSON compatto (`separators=(",",":")`, UTF-8), `enc = base64(nonce 12 byte ‖ ciphertext‖tag)`, AAD `claude-master-relay-v1`; chiave = HKDF-SHA256(X25519 shared, info `claude-master-relay-v1`, 32 byte, salt vuoto); `check = HMAC-SHA256(key, code) hex[:16]` (dal piano fase 1 del relay). La chiave vive nel Keystore Android (wrap) e mai in chiaro su disco.
- Package `it.pixelbox.cmwatch`; minSdk 33, compileSdk/targetSdk 36; JDK 17; `org.gradle.jvmargs=-Xmx2048m`, `kotlin.daemon.jvmargs=-Xmx1024m`, un daemon.
- Testi visibili in italiano in `wear/src/main/res/values/strings.xml`, mai cablati nel Kotlin. Icone di stato ❓ ▶ ✓ ✗ come icone (Material Symbols Rounded), stessi significati di Telegram.
- Forma (Franz 12/09): una riga logica = una riga fisica; mai colonne di frammenti; mai «…» nel corpo; un solo bottone pieno (`Button` accent) per schermata, gli altri `FilledTonalButton`; liste `TransformingLazyColumn` + `SurfaceTransformation`; tasto ▶ TTS accanto a ogni testo > `tts.min_chars` (120) o di tipo esito/risposta/domanda.
- Colori: `bg` #000000 · `surface` #121417 · `line` #2A2E35 · `text` #F2F4F7 · `text2` #9AA3B2 · `accent` #4C7DFF (premuto #3457D5) · `waiting` #FFB020 · `busy` #7FA1FF · `idle` #34C759 · `gone` #FF453A · `stale` #6B7280. Tema scuro unico. Nomi e terminale `Roboto Mono` 14 sp; riga di lista 16 sp; domanda 18/15 sp; esito 20/15 sp; minimo 13 sp. Margini 5,2 % su 456 px; schede raggio 20 dp con bordo `line`; pillole 52 dp; bersagli ≥ 48 dp.
- Commit in inglese (`feat(watch): …`, `feat(tile): …`, `test: …`, `docs: …`), file per nome, mai `-A`. Mai keystore, `google-services.json`, chiavi o token nel repo.
- Firebase reale (progetto, RTDB, FCM, `google-services.json`), installazione sul Pixel Watch, screenshot dal polso, approvazione visiva: si chiedono a Franz in una riga e ci si ferma. Mai inventare esiti.

## Vincoli dell'ambiente (verificati il 12/09/2026)

- Macchina aarch64 (Crostini), 6 GB, niente `/dev/kvm`: **nessun emulatore Wear OS può girare qui**. `emulator-5554` in `adb devices` è ARC (Android di ChromeOS, API 33, 1920×1080, telefono), non un orologio. Quindi il «dimostrabile in emulatore» diventa: (a) test unit JVM su contratto, repo e riduttori; (b) installazione dell'APK debug su ARC come prova di fumo (Wear Compose gira anche su un telefono; tile e complication no); (c) screenshot test Paparazzi in GitHub Actions (x86_64; il layoutlib nativo non esiste per linux-aarch64); (d) il Pixel Watch reale via `adb` wireless quando Franz c'è.
- `aapt2` è x86_64: la build locale usa `~/android-sdk/aapt2-qemu/aapt2` tramite `android.aapt2FromMavenOverride` già in `~/.gradle/gradle.properties`. `adb` idem sotto qemu, vedi `scripts/install-watch.sh`.
- Gradle 9.0.0 è già in `~/.gradle/wrapper/dists`; AGP 8.13.2 richiede Gradle ≥ 8.13. Se la combinazione 9.0.0 + 8.13.2 non passa, ripiego: `gradle-8.14.3-bin.zip` nel wrapper, resto invariato.
- Versioni verificate il 12/09/2026 su `dl.google.com/dl/android/maven2` e Maven Central: compose-material3 wear 1.6.2, protolayout 1.4.2, tiles 1.6.2, room 2.8.5, compose-bom 2026.06.01 (Compose 1.11.4), activity-compose 1.13.0, core-ktx 1.18.0, lifecycle 2.10.0, datastore 1.2.1, firebase-bom 34.19.0, google-services 4.5.0, wear-ongoing 1.1.0, wear-input 1.1.0, AGP 8.13.2, Kotlin 2.3.21, KSP 2.3.12, kotlinx-serialization 1.11.0, okhttp 5.4.0, paparazzi 1.3.5. Le ultimissime (Compose 1.12 / BOM 2026.08+, core 1.19, lifecycle 2.11, okhttp 5.5) pretendono compileSdk 37 e AGP 9.1 (`aar-metadata` letti dagli AAR): fuori dal design (SDK 35/36), quindi escluse finché non si passa ad AGP 9.

## Struttura dei file

Tre moduli (Franz via la master, 12/09 12:23): `:core` = tutto ciò che un telefono riuserebbe (contratto, cifratura, Transport finto e Firebase, Room, Repo, preferenze, regole e testi puri); `:wear` = solo presentazione Wear OS (Compose M3, tile, complication, notifiche, TTS, aptica); `:mobile` = app telefono vuota (manifest + Activity segnaposto), non si sviluppa ora. Niente logica in `:wear` che non sia presentazione; i test del contratto vivono in `:core`. I testi puri di `:core` non contengono italiano: ricevono le etichette da `:wear` (`strings.xml`).

```
settings.gradle.kts · build.gradle.kts · gradle.properties · gradle/libs.versions.toml · gradle/wrapper/*
.github/workflows/build-android.yml           test :core + assembleDebug :wear/:mobile; assembleRelease firmato da secret
scripts/install-watch.sh                      adb wireless (pair/connect/install), copia del watchface
core/build.gradle.kts                         com.android.library, namespace it.pixelbox.cmwatch.core; assets = contract/ copiato in build/
core/src/main/kotlin/it/pixelbox/cmwatch/
  contract/Model.kt        data class @Serializable del contratto (State, Session, Question, …)
  contract/ContractJson.kt Json configurato + decode/encode
  contract/Order.kt        ordinamento sessioni, Freshness (PC fermo), durate leggibili
  crypto/Blob.kt           AES-256-GCM seal/open del documento {"v","enc"}
  crypto/Pairing.kt        X25519 + HKDF + check code
  crypto/KeyVault.kt       chiave AES avvolta nel Keystore, salvata in DataStore
  transport/Transport.kt   interfaccia + PairingInfo + TransportException
  transport/FakeTransport.kt   fixture da assets, risposte simulate, commutabile
  transport/FirebaseTransport.kt, transport/Rtdb.kt, transport/AuthToken.kt   REST + SSE su RTDB (fase 3)
  data/Db.kt               Room: StateRow, EventRow, PendingCmdRow + DAO
  data/Store.kt            interfaccia Store (Room) + MemoryStore (test)
  data/Repo.kt             StateFlow<Snapshot>, comandi ottimistici, coda offline, timeout 20 s
  settings/Prefs.kt        DataStore: uid, nome dispositivo, soglia TTS, vibrazioni, account complication, fixture demo
  rules/ViewState.kt       riduttore di priorità offline > domanda > scelta (Screen)
  rules/SessionsText.kt, CardText.kt, QuestionRules.kt, SpeakRules.kt, Wake.kt, NotificationTexts.kt,
        TileTexts.kt, ComplicationTexts.kt, TerminalText.kt, TimelineText.kt, LaunchRules.kt,
        FollowRules.kt, QuotaText.kt, RecapText.kt     funzioni pure, testate, senza risorse Android
core/src/test/kotlin/it/pixelbox/cmwatch/… Fixtures.kt + un file di test per unità (vedi task)
wear/build.gradle.kts                         com.android.application, applicationId it.pixelbox.cmwatch
wear/src/main/AndroidManifest.xml
wear/src/main/res/values/{strings,themes,colors}.xml  testi italiani, tema senza ActionBar
wear/src/main/kotlin/it/pixelbox/cmwatch/wear/
  CmApp.kt                 Application: Db, Transport, Repo, Prefs (DI a mano, niente Hilt)
  MainActivity.kt          AppScaffold + navigazione
  ui/theme/Theme.kt        token colore, ColorScheme, Typography
  ui/Nav.kt                rotte e SwipeDismissableNavHost
  ui/Keyboard.kt           tastiera di sistema (RemoteInput)
  ui/components/StateIcon.kt, AccountDot.kt, SessionRow.kt, SpeakButton.kt, StaleChip.kt, WideButton.kt
  ui/screens/SessionsScreen.kt, SessionScreen.kt, QuestionScreen.kt, PairingScreen.kt, SettingsScreen.kt,
             OutcomeScreen.kt, TerminalScreen.kt, TimelineScreen.kt, LaunchScreen.kt, QuotaScreen.kt,
             RecapScreen.kt, NightScreen.kt
  haptics/Haptics.kt       VibrationEffect per tipo
  tts/Speaker.kt           TextToSpeech di sistema
  push/CmMessagingService.kt, push/Notifier.kt, push/ReplyReceiver.kt
  tile/CmTileService.kt
  complication/CmComplicationService.kt
  follow/FollowOngoing.kt  OngoingActivity «▶ nome 4 m»
wear/src/test/kotlin/it/pixelbox/cmwatch/wear/ScreensSnapshotTest.kt   Paparazzi (solo in Actions)
mobile/build.gradle.kts · mobile/src/main/AndroidManifest.xml · mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt   segnaposto
docs/verifiche/fase-2.md … fase-5.md   checklist dal vivo
```

Il codice della fase 2 è completo qui sotto; le fasi 3-5 hanno file, interfacce, test e codice chiave, e si rifiniscono al «fatto» della fase precedente (design, sezione 6: non si passa oltre senza la prova).

---

## Fase 0 — progetto Gradle e build verde

### Task 1: scheletro Gradle a tre moduli, manifest, Activity vuote, Actions, adb wireless

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew` (copiato dal watchface), `core/build.gradle.kts`, `core/src/main/AndroidManifest.xml`, `wear/build.gradle.kts`, `wear/proguard-rules.pro`, `wear/src/main/AndroidManifest.xml`, `wear/src/main/res/values/strings.xml`, `wear/src/main/res/values/themes.xml`, `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt`, `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt`, `mobile/build.gradle.kts`, `mobile/src/main/AndroidManifest.xml`, `mobile/src/main/res/values/strings.xml`, `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`, `.github/workflows/build-android.yml`, `scripts/install-watch.sh`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/SmokeTest.kt`

**Interfaces:**
- Produces: moduli `:core` (library, namespace `it.pixelbox.cmwatch.core`), `:wear` (app, `applicationId` `it.pixelbox.cmwatch`, dipende da `:core`), `:mobile` (app segnaposto, `applicationId` `it.pixelbox.cmwatch.mobile`, dipende da `:core`); alias del catalogo `libs.*` usati da tutti i task; asset `contract/*.json` di `:core` disponibili nell'APK come `assets/contract/<nome>.json`; comando di verifica di ogni task: `./gradlew :core:testDebugUnitTest :wear:assembleDebug`.

- [ ] **Step 1: wrapper e file radice**

```bash
cd ~/Desktop/workspaces/personali/claude-master-watch
mkdir -p gradle/wrapper core/src/main/kotlin/it/pixelbox/cmwatch core/src/test/kotlin/it/pixelbox/cmwatch wear/src/main/kotlin/it/pixelbox/cmwatch/wear wear/src/main/res/values mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile mobile/src/main/res/values scripts .github/workflows
cp ~/Desktop/workspaces/personali/watchface/gradlew ~/Desktop/workspaces/personali/watchface/gradlew.bat .
cp ~/Desktop/workspaces/personali/watchface/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/
chmod +x gradlew
```

`gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.0.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "claude-master-watch"
include(":core", ":wear", ":mobile")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
kotlin.daemon.jvmargs=-Xmx1024m
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.13.2"
kotlin = "2.3.21"
ksp = "2.3.12"
serialization = "1.11.0"
coroutines = "1.10.2"
coreKtx = "1.18.0"
activityCompose = "1.13.0"
lifecycle = "2.10.0"
composeBom = "2026.06.01"
wearCompose = "1.6.2"
protolayout = "1.4.2"
tiles = "1.6.2"
complications = "1.3.0"
room = "2.8.5"
datastore = "1.2.1"
wearOngoing = "1.1.0"
wearInput = "1.1.0"
firebaseBom = "34.19.0"
googleServices = "4.5.0"
okhttp = "5.4.0"
junit = "4.13.2"

[libraries]
core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material-icons = { module = "androidx.compose.material:material-icons-extended" }
wear-compose-material3 = { module = "androidx.wear.compose:compose-material3", version.ref = "wearCompose" }
wear-compose-foundation = { module = "androidx.wear.compose:compose-foundation", version.ref = "wearCompose" }
wear-compose-navigation = { module = "androidx.wear.compose:compose-navigation", version.ref = "wearCompose" }
wear-compose-ui-tooling = { module = "androidx.wear.compose:compose-ui-tooling", version.ref = "wearCompose" }
protolayout = { module = "androidx.wear.protolayout:protolayout", version.ref = "protolayout" }
protolayout-expression = { module = "androidx.wear.protolayout:protolayout-expression", version.ref = "protolayout" }
protolayout-material3 = { module = "androidx.wear.protolayout:protolayout-material3", version.ref = "protolayout" }
tiles = { module = "androidx.wear.tiles:tiles", version.ref = "tiles" }
complications-datasource = { module = "androidx.wear.watchface:watchface-complications-data-source-ktx", version.ref = "complications" }
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
wear-ongoing = { module = "androidx.wear:wear-ongoing", version.ref = "wearOngoing" }
wear-input = { module = "androidx.wear:wear-input", version.ref = "wearInput" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging" }
firebase-auth = { module = "com.google.firebase:firebase-auth" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-sse = { module = "com.squareup.okhttp3:okhttp-sse", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version.ref = "room" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

Se `kotlinx-coroutines` 1.10.2 non esiste su Maven Central, prendi l'ultima 1.10.x stampata da `curl -s https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/maven-metadata.xml | grep -o "<version>1\.10[^<]*"`.

- [ ] **Step 2: i tre moduli**

`core/build.gradle.kts` (libreria: contratto, cifratura, Transport, Room, Repo, preferenze, regole pure):
```kotlin
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
```

`core/src/main/AndroidManifest.xml`: `<manifest xmlns:android="http://schemas.android.com/apk/res/android" />` con `<uses-permission android:name="android.permission.INTERNET" />`.

`wear/build.gradle.kts` (app Wear OS: solo presentazione):
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    }
    buildFeatures { compose = true }
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
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
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
    implementation(libs.wear.ongoing)
    implementation(libs.wear.input)
    implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
}
```

`mobile/build.gradle.kts` (segnaposto: stesso blocco `android {}` di `:wear` con `applicationId = "it.pixelbox.cmwatch.mobile"`, `namespace = "it.pixelbox.cmwatch.mobile"`, niente firma, `buildFeatures { compose = false }`; dipendenze: `project(":core")`, `libs.core.ktx`, `androidx.appcompat:appcompat:1.7.1`). Manifest con una `MainActivity` (`AppCompatActivity` con un `TextView` «claude-master (telefono): in arrivo») e `LAUNCHER`; `strings.xml` con `app_name` = «Claude Master». `google-services.json` condizionale come in `:wear`.

`wear/proguard-rules.pro`: vuoto con una riga di commento `# minify off in v1`.

`wear/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:name="android.hardware.type.watch" android:required="false" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".CmApp"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="false"
        android:theme="@style/Theme.CmWatch">
        <meta-data android:name="com.google.android.wearable.standalone" android:value="true" />

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:taskAffinity=""
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="cmwatch" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`android:required="false"` sulla feature watch serve solo alla prova di fumo su ARC; sul Pixel Watch non cambia nulla. Icona: `wear/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` adattiva con `@color/ic_bg` (#4C7DFF) e un `ic_fg` vettoriale (cerchio bianco con «?»): tre file piccoli, generati a mano.

`wear/src/main/res/values/strings.xml` (fase 0, si allunga nei task seguenti):
```xml
<resources>
    <string name="app_name">Claude Master</string>
    <string name="sessions_title">Sessioni</string>
    <string name="sessions_empty">Nessuna sessione</string>
</resources>
```

`wear/src/main/res/values/themes.xml`:
```xml
<resources>
    <style name="Theme.CmWatch" parent="android:Theme.DeviceDefault.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
```

`wear/…/MainActivity.kt` (fase 0: una riga di testo, sostituita nel Task 7):
```kotlin
package it.pixelbox.cmwatch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Text(getString(R.string.sessions_title)) } }
    }
}
```

`wear/…/CmApp.kt` (fase 0: vuota, riempita nel Task 5):
```kotlin
package it.pixelbox.cmwatch.wear

import android.app.Application

class CmApp : Application()
```

- [ ] **Step 3: test di fumo**

`core/src/test/kotlin/it/pixelbox/cmwatch/SmokeTest.kt`:
```kotlin
package it.pixelbox.cmwatch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SmokeTest {
    @Test fun contractFixturesAreReachable() {
        assertTrue(File("../contract/state-1-question.json").isFile)
    }
}
```

- [ ] **Step 4: build**

Run: `./gradlew :core:testDebugUnitTest :wear:assembleDebug :mobile:assembleDebug --console=plain 2>&1 | tail -20`
Expected: `BUILD SUCCESSFUL`, `wear/build/outputs/apk/debug/wear-debug.apk` e `mobile/build/outputs/apk/debug/mobile-debug.apk` esistono. Se la risoluzione dei plugin fallisce per Gradle 9 + AGP 8.13: `distributionUrl` → `gradle-8.14.3-bin.zip` e riprova; annota il ripiego in questo file.

- [ ] **Step 5: Actions e adb wireless**

`.github/workflows/build-android.yml`:
```yaml
name: Build Wear OS APK

on:
  push:
    branches: [main, master, feature/**]
    paths: ['core/**', 'wear/**', 'mobile/**', 'gradle/**', 'contract/**', '*.gradle.kts', 'gradle.properties', '.github/workflows/build-android.yml']
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      - uses: android-actions/setup-android@v3
      - uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties', 'gradle/libs.versions.toml', 'core/build.gradle.kts', 'wear/build.gradle.kts') }}
          restore-keys: gradle-
      - name: Unit tests
        run: ./gradlew :core:testDebugUnitTest --console=plain
      - name: Screenshot tests (Paparazzi)
        if: hashFiles('wear/src/test/snapshots/**') != ''
        run: ./gradlew :wear:verifyPaparazziDebug --console=plain
      - name: Debug APK
        run: ./gradlew :wear:assembleDebug :mobile:assembleDebug --console=plain
      - name: Restore keystore
        if: ${{ secrets.RELEASE_KEYSTORE_BASE64 != '' }}
        run: echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d > release.jks
      - name: Release APK
        if: ${{ secrets.RELEASE_KEYSTORE_BASE64 != '' }}
        env:
          KEYSTORE_PATH: ${{ github.workspace }}/release.jks
          KEYSTORE_PASS: ${{ secrets.KEYSTORE_PASS }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASS: ${{ secrets.KEY_PASS }}
        run: ./gradlew :wear:assembleRelease --console=plain -x lint -x test
      - uses: actions/upload-artifact@v4
        with:
          name: cmwatch-apk
          path: |
            wear/build/outputs/apk/debug/*.apk
            wear/build/outputs/apk/release/*.apk
            mobile/build/outputs/apk/debug/*.apk
          retention-days: 30
```

`scripts/install-watch.sh`: copia di `watchface/scripts/install-watch.sh` con `APK` di default = `wear/build/outputs/apk/debug/wear-debug.apk` (niente `gh release`), uso `./scripts/install-watch.sh pair IP:PORTA CODICE` e `./scripts/install-watch.sh IP:PORTA [apk]`.

- [ ] **Step 6: commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat core/build.gradle.kts core/src/main/AndroidManifest.xml core/src/test/kotlin/it/pixelbox/cmwatch/SmokeTest.kt wear/build.gradle.kts wear/proguard-rules.pro wear/src/main/AndroidManifest.xml wear/src/main/res/values/strings.xml wear/src/main/res/values/themes.xml wear/src/main/res/values/colors.xml wear/src/main/res/mipmap-anydpi-v26/ic_launcher.xml wear/src/main/res/drawable/ic_launcher_fg.xml wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt mobile/build.gradle.kts mobile/src/main/AndroidManifest.xml mobile/src/main/res/values/strings.xml mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt .github/workflows/build-android.yml scripts/install-watch.sh
git commit -m "build: three-module Gradle project — :core (contract, crypto, transport, Room), :wear (Wear OS app), :mobile (placeholder); AGP 8.13, Kotlin 2.3, contract fixtures as assets, Actions with keystore from secret, adb wireless script"
```

---

## Fase 2 — pairing, Room, Sessioni, Scheda, Domanda con `answer`

### Task 2: modello del contratto e test sulle fixture

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt`, `contract/ContractJson.kt`, `contract/Order.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/contract/ContractTest.kt`, `contract/OrderTest.kt`, `core/src/test/kotlin/it/pixelbox/cmwatch/Fixtures.kt`

**Interfaces:**
- Produces: `State`, `Session`, `SessionState`, `Question`, `QuestionKind`, `Tier`, `Option`, `Outcome`, `QuotaAccount`, `Project`, `Night`, `Recap`, `RecapItem`, `Event`, `EventKind`, `Cmd`, `CmdOp`, `CmdResult`; `ContractJson.json: Json`, `ContractJson.decodeState(String): State`, `decodeEvents(String): List<Event>`, `encode(Cmd): String`, `decodeResult(String): CmdResult`; `Order.sessions(List<Session>): List<Session>`; `Freshness.of(stateTs: Long, now: Long): Freshness` (`Fresh`, `Stale(minutes)`), `Freshness.STALE_AFTER_S = 180`; `Durations.since(from: Long, now: Long): String` («2 m», «1 h 05», «3 g»).

- [ ] **Step 1: test che leggono le fixture**

`Fixtures.kt`:
```kotlin
package it.pixelbox.cmwatch

import java.io.File

object Fixtures {
    private val dir = File("../contract")
    fun read(name: String): String = File(dir, name).readText()
    val stateQuestion get() = read("state-1-question.json")
    val stateIdle get() = read("state-2-idle.json")
    val stateStale get() = read("state-3-stale.json")
    val events get() = read("events-sample.json")
    val cmdResult get() = read("cmd-result-sample.json")
}
```

`ContractTest.kt`:
```kotlin
package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    private val all = listOf(Fixtures.stateQuestion, Fixtures.stateIdle, Fixtures.stateStale)

    @Test fun stateOneDecodesEveryField() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(1, s.v); assertEquals("crostini-demo", s.host); assertEquals(4, s.sessions.size)
        val led = s.sessions[0]
        assertEquals("ledger-api", led.name); assertEquals(SessionState.WAITING, led.state)
        assertTrue(led.followed); assertEquals("Wait for the go", led.next)
        val q = led.question!!
        assertEquals(QuestionKind.ASK, q.kind); assertEquals(Tier.MEDIUM, q.tier)
        assertEquals(listOf(1, 2), q.options.map { it.n }); assertEquals("yes", q.options[0].label)
        val atlas = s.sessions[1]
        assertEquals("Bash pytest -q tests", atlas.tool); assertEquals(1789210300L, atlas.outcome!!.at)
        assertNull(s.quota.getValue("agenzia").h5); assertTrue(s.quota.getValue("agenzia").stale)
        assertEquals(11, s.quota.getValue("personale").h5)
        assertEquals(3, s.projects.size); assertEquals(2, s.night.queued); assertNull(s.night.running)
        assertEquals("2026-09-12", s.recap.date); assertEquals(2, s.recap.items.size)
    }

    @Test fun staleFixtureHasNoSessions() {
        val s = ContractJson.decodeState(Fixtures.stateStale)
        assertTrue(s.sessions.isEmpty()); assertEquals(1789200000L, s.ts)
    }

    @Test fun rulesHoldOnEveryFixture() {
        for (raw in all) {
            assertTrue("state ≤ 8 KB", raw.toByteArray().size <= 8 * 1024)
            val s = ContractJson.decodeState(raw)
            for (ses in s.sessions) {
                ses.outcome?.let { assertTrue(it.short.length <= 60); assertTrue(it.full.length <= 600) }
                ses.question?.let { q ->
                    assertFalse(q.text.endsWith("…")); assertTrue(q.text.isNotBlank())
                    assertEquals((1..q.options.size).toList(), q.options.map { it.n })
                }
            }
            assertEquals(Order.sessions(s.sessions), s.sessions)
        }
    }

    @Test fun roundTripKeepsEveryValue() {
        for (raw in all) {
            val s = ContractJson.decodeState(raw)
            val again = ContractJson.decodeState(ContractJson.json.encodeToString(State.serializer(), s))
            assertEquals(s, again)
        }
    }

    @Test fun eventsDecodeOnePerKind() {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        assertEquals(EventKind.entries.size - 1, ev.map { it.kind }.toSet().size) // manca «resumed» nella fixture
        assertEquals("q-1789210500-1", ev.first { it.kind == EventKind.QUESTION }.ref)
        assertNull(ev.first { it.kind == EventKind.QUOTA }.session)
    }

    @Test fun cmdAndResultDecodeAndEncode() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        assertEquals(CmdOp.entries.size, cmds.map { it.op }.toSet().size)
        assertEquals(7, results.size); assertEquals(2, results.count { !it.ok })
        val enc = ContractJson.encode(cmds[0])
        assertTrue(enc.contains("\"op\":\"answer\"")); assertTrue(enc.contains("\"arg\":\"1\""))
        assertEquals(cmds[0], ContractJson.json.decodeFromString(Cmd.serializer(), enc))
        assertEquals(results[3], ContractJson.decodeResult(ContractJson.json.encodeToString(CmdResult.serializer(), results[3])))
    }

    @Test fun unknownKeysAreIgnored() {
        val s = ContractJson.decodeState(Fixtures.stateIdle.replaceFirst("\"host\"", "\"extra\": 1, \"host\""))
        assertEquals("crostini-demo", s.host)
    }
}
```

`OrderTest.kt`:
```kotlin
package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderTest {
    @Test fun shuffledSessionsComeBackInContractOrder() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val shuffled = s.sessions.reversed()
        assertEquals(listOf("ledger-api", "atlas-shop", "field-notes", "orbit-docs"), Order.sessions(shuffled).map { it.name })
    }

    @Test fun sameStateSortsByName() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val a = s.sessions[2].copy(name = "zeta"); val b = s.sessions[2].copy(name = "alpha")
        assertEquals(listOf("alpha", "zeta"), Order.sessions(listOf(a, b)).map { it.name })
    }

    @Test fun freshnessThreeMinutes() {
        assertEquals(Freshness.Fresh, Freshness.of(1000, 1000 + 179))
        assertEquals(Freshness.Stale(3), Freshness.of(1000, 1000 + 180))
        assertEquals(Freshness.Stale(65), Freshness.of(1000, 1000 + 65 * 60 + 5))
    }

    @Test fun durations() {
        assertEquals("0 m", Durations.since(100, 130))
        assertEquals("2 m", Durations.since(100, 100 + 150))
        assertEquals("1 h 05", Durations.since(0, 65 * 60))
        assertEquals("3 g", Durations.since(0, 3 * 86400 + 100))
    }
}
```

- [ ] **Step 2: rosso**

Run: `./gradlew :core:testDebugUnitTest --console=plain 2>&1 | grep -E "error:|FAILED|BUILD" | head`
Expected: errori di compilazione (`ContractJson`, `Order` non definiti).

- [ ] **Step 3: implementazione**

`Model.kt`:
```kotlin
package it.pixelbox.cmwatch.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SessionState {
    @SerialName("waiting") WAITING, @SerialName("busy") BUSY, @SerialName("idle") IDLE,
    @SerialName("awaiting") AWAITING, @SerialName("gone") GONE
}

@Serializable
enum class QuestionKind { @SerialName("permission") PERMISSION, @SerialName("ask") ASK, @SerialName("plan") PLAN }

@Serializable
enum class Tier { @SerialName("low") LOW, @SerialName("medium") MEDIUM, @SerialName("high") HIGH }

@Serializable
enum class EventKind {
    @SerialName("question") QUESTION, @SerialName("answered") ANSWERED, @SerialName("outcome") OUTCOME,
    @SerialName("gone") GONE, @SerialName("launched") LAUNCHED, @SerialName("quota") QUOTA, @SerialName("resumed") RESUMED
}

@Serializable
enum class CmdOp {
    @SerialName("answer") ANSWER, @SerialName("prompt") PROMPT, @SerialName("launch") LAUNCH,
    @SerialName("follow") FOLLOW, @SerialName("unfollow") UNFOLLOW, @SerialName("resume") RESUME,
    @SerialName("screen") SCREEN, @SerialName("allow_all") ALLOW_ALL
}

@Serializable data class Option(val n: Int, val label: String)

@Serializable data class Question(
    val id: String, val kind: QuestionKind, val text: String, val options: List<Option>,
    val tier: Tier, @SerialName("asked_at") val askedAt: Long,
)

@Serializable data class Outcome(val short: String, val full: String, val at: Long)

@Serializable data class Session(
    val id: String, val name: String, val account: String, val project: String,
    val state: SessionState, val since: Long,
    @SerialName("turn_started") val turnStarted: Long? = null,
    val tool: String? = null, val link: String = "",
    val attached: Boolean = false, val followed: Boolean = false,
    val question: Question? = null, val outcome: Outcome? = null, val next: String? = null,
)

@Serializable data class QuotaAccount(
    val h5: Int? = null, val w7: Int? = null,
    @SerialName("reset_w7") val resetW7: Long? = null, val stale: Boolean = false,
)

@Serializable data class Project(val path: String, val name: String, val account: String)
@Serializable data class Night(val queued: Int = 0, val running: String? = null)
@Serializable data class RecapItem(val project: String, val done: String, val next: String? = null)
@Serializable data class Recap(val date: String = "", val items: List<RecapItem> = emptyList())

@Serializable data class State(
    val v: Int, val ts: Long, val host: String,
    val sessions: List<Session> = emptyList(),
    val quota: Map<String, QuotaAccount> = emptyMap(),
    val projects: List<Project> = emptyList(),
    val night: Night = Night(), val recap: Recap = Recap(),
)

@Serializable data class Event(
    val key: String, val kind: EventKind, val session: String? = null, val account: String? = null,
    val ts: Long, val title: String, val body: String = "", val ref: String? = null,
)

@Serializable data class Cmd(
    val id: String, val op: CmdOp, val session: String? = null, val arg: String? = null,
    val issued: Long, val by: String,
)

@Serializable data class CmdResult(val id: String, val ok: Boolean, val text: String, val at: Long)
```

`ContractJson.kt`:
```kotlin
package it.pixelbox.cmwatch.contract

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ContractJson {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }
    fun decodeState(raw: String): State = json.decodeFromString(State.serializer(), raw)
    fun decodeEvents(raw: String): List<Event> = json.decodeFromString(ListSerializer(Event.serializer()), raw)
    fun encode(cmd: Cmd): String = json.encodeToString(Cmd.serializer(), cmd)
    fun decodeResult(raw: String): CmdResult = json.decodeFromString(CmdResult.serializer(), raw)
}
```

`Order.kt`:
```kotlin
package it.pixelbox.cmwatch.contract

object Order {
    // waiting, busy, idle, gone, poi alfabetico (contract/README.md); awaiting sta con idle.
    private fun rank(s: SessionState) = when (s) {
        SessionState.WAITING -> 0; SessionState.BUSY -> 1; SessionState.IDLE -> 2
        SessionState.AWAITING -> 2; SessionState.GONE -> 3
    }
    fun sessions(list: List<Session>): List<Session> =
        list.sortedWith(compareBy<Session> { rank(it.state) }.thenBy { it.name.lowercase() })
}

sealed class Freshness {
    data object Fresh : Freshness()
    data class Stale(val minutes: Int) : Freshness()
    companion object {
        const val STALE_AFTER_S = 180L
        fun of(stateTs: Long, now: Long): Freshness {
            val age = now - stateTs
            return if (age < STALE_AFTER_S) Fresh else Stale((age / 60).toInt())
        }
    }
}

object Durations {
    fun since(from: Long, now: Long): String {
        val s = (now - from).coerceAtLeast(0)
        return when {
            s < 3600 -> "${s / 60} m"
            s < 86400 -> "%d h %02d".format(s / 3600, (s % 3600) / 60)
            else -> "${s / 86400} g"
        }
    }
}
```

- [ ] **Step 4: verde**

Run: `./gradlew :core:testDebugUnitTest --console=plain 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`. Report in `core/build/reports/tests/testDebugUnitTest/index.html`; per i numeri: `grep -o 'tests="[0-9]*"\|failures="[0-9]*"' core/build/test-results/testDebugUnitTest/*.xml`.

- [ ] **Step 5: commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt core/src/main/kotlin/it/pixelbox/cmwatch/contract/ContractJson.kt core/src/main/kotlin/it/pixelbox/cmwatch/contract/Order.kt core/src/test/kotlin/it/pixelbox/cmwatch/Fixtures.kt core/src/test/kotlin/it/pixelbox/cmwatch/contract/ContractTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/contract/OrderTest.kt
git commit -m "feat(watch): contract v1 model — state, events, cmd/result decoded from the shared fixtures; session order, freshness, durations"
```

### Task 3: cifratura del blob e chiavi di pairing

**Files:**
- Create: `crypto/Blob.kt`, `crypto/Pairing.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/crypto/BlobTest.kt`, `crypto/PairingTest.kt`

**Interfaces:**
- Produces: `Blob.seal(plain: String, key: ByteArray): String` (JSON `{"v":1,"enc":…}`), `Blob.open(doc: String, key: ByteArray): String` (lancia `BlobException` su chiave errata o `v` diverso), `Blob.AAD`; `Pairing.newKeyPair(): KeyPair` (X25519), `Pairing.publicB64(KeyPair): String`, `Pairing.sharedKey(priv: PrivateKey, peerPubB64: String): ByteArray` (32 byte), `Pairing.checkCode(key: ByteArray, code: String): String` (16 hex).

- [ ] **Step 1: test**

`BlobTest.kt`:
```kotlin
package it.pixelbox.cmwatch.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class BlobTest {
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Test fun sealThenOpen() {
        val doc = Blob.seal("""{"a":1,"è":"sì"}""", key)
        val o = Json.parseToJsonElement(doc).jsonObject
        assertEquals(1, o.getValue("v").jsonPrimitive.content.toInt())
        assertTrue(o.getValue("enc").jsonPrimitive.content.matches(Regex("[A-Za-z0-9+/=]+")))
        assertEquals("""{"a":1,"è":"sì"}""", Blob.open(doc, key))
    }

    @Test fun twoSealsDiffer() { assertNotEquals(Blob.seal("x", key), Blob.seal("x", key)) }

    @Test(expected = BlobException::class) fun wrongKeyFails() {
        val other = ByteArray(32).also { SecureRandom().nextBytes(it) }
        Blob.open(Blob.seal("x", key), other)
    }

    @Test(expected = BlobException::class) fun wrongVersionFails() {
        Blob.open("""{"v":2,"enc":"AAAA"}""", key)
    }

    @Test fun pythonLayoutIsNonceThenCiphertext() {
        // Stesso layout di cm-relay-crypto.py: base64(nonce12 ‖ ct‖tag), AAD claude-master-relay-v1.
        val doc = Blob.seal("hi", key)
        val enc = java.util.Base64.getDecoder().decode(Json.parseToJsonElement(doc).jsonObject.getValue("enc").jsonPrimitive.content)
        assertEquals(12 + 2 + 16, enc.size)
    }
}
```

`PairingTest.kt`:
```kotlin
package it.pixelbox.cmwatch.crypto

import org.junit.Assert.*
import org.junit.Test

class PairingTest {
    @Test fun sharedKeyMatchesBothWays() {
        val a = Pairing.newKeyPair(); val b = Pairing.newKeyPair()
        val ab = Pairing.sharedKey(a.private, Pairing.publicB64(b))
        val ba = Pairing.sharedKey(b.private, Pairing.publicB64(a))
        assertArrayEquals(ab, ba); assertEquals(32, ab.size)
        assertFalse(ab.contentEquals(Pairing.sharedKey(a.private, Pairing.publicB64(a))))
    }

    @Test fun publicKeyIsRaw32Bytes() {
        assertEquals(32, java.util.Base64.getDecoder().decode(Pairing.publicB64(Pairing.newKeyPair())).size)
    }

    @Test fun checkCodeIs16HexAndKeyBound() {
        val k1 = ByteArray(32) { 1 }; val k2 = ByteArray(32) { 2 }
        val c = Pairing.checkCode(k1, "123456")
        assertTrue(c.matches(Regex("[0-9a-f]{16}")))
        assertNotEquals(c, Pairing.checkCode(k2, "123456"))
        assertEquals("f3d4b8a1e6c2f9d0".length, c.length)
    }
}
```

- [ ] **Step 2: rosso** — Run: `./gradlew :core:testDebugUnitTest --console=plain 2>&1 | grep -c "error:"` → > 0.

- [ ] **Step 3: implementazione**

`Blob.kt`:
```kotlin
package it.pixelbox.cmwatch.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BlobException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

object Blob {
    const val VERSION = 1
    val AAD: ByteArray = "claude-master-relay-v1".toByteArray()
    private const val NONCE = 12
    private val rnd = SecureRandom()

    fun seal(plain: String, key: ByteArray): String {
        val nonce = ByteArray(NONCE).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(AAD)
        val ct = c.doFinal(plain.toByteArray())
        val enc = Base64.getEncoder().encodeToString(nonce + ct)
        return Json.encodeToString(JsonObject.serializer(), JsonObject(mapOf("v" to JsonPrimitive(VERSION), "enc" to JsonPrimitive(enc))))
    }

    fun open(doc: String, key: ByteArray): String {
        val o = runCatching { Json.parseToJsonElement(doc).jsonObject }.getOrElse { throw BlobException("not a document", it) }
        if (o["v"]?.jsonPrimitive?.content?.toIntOrNull() != VERSION) throw BlobException("unsupported version")
        val raw = runCatching { Base64.getDecoder().decode(o.getValue("enc").jsonPrimitive.content) }
            .getOrElse { throw BlobException("bad base64", it) }
        if (raw.size < NONCE + 16) throw BlobException("too short")
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, raw, 0, NONCE))
            c.updateAAD(AAD)
            String(c.doFinal(raw, NONCE, raw.size - NONCE))
        } catch (e: Exception) { throw BlobException("cannot open", e) }
    }
}
```

`Pairing.kt` (X25519 via JCA `XDH`, presente su Android 33+ e su JDK 17; HKDF a mano con HMAC-SHA256):
```kotlin
package it.pixelbox.cmwatch.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPublicKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.math.BigInteger
import java.security.interfaces.XECPublicKey

object Pairing {
    private const val INFO = "claude-master-relay-v1"

    fun newKeyPair(): KeyPair = KeyPairGenerator.getInstance("XDH").apply { initialize(NamedParameterSpec.X25519) }.generateKeyPair()

    /** Chiave pubblica grezza (32 byte little-endian, come `public_bytes(Raw)` in Python). */
    fun publicB64(kp: KeyPair): String {
        val u = (kp.public as XECPublicKey).u
        return Base64.getEncoder().encodeToString(uToRaw(u))
    }

    fun sharedKey(priv: PrivateKey, peerPubB64: String): ByteArray {
        val raw = Base64.getDecoder().decode(peerPubB64)
        val u = BigInteger(1, raw.reversedArray())
        val pub = KeyFactory.getInstance("XDH").generatePublic(XECPublicKeySpec(NamedParameterSpec.X25519, u))
        val ka = KeyAgreement.getInstance("XDH").apply { init(priv); doPhase(pub, true) }
        return hkdf(ka.generateSecret(), INFO.toByteArray(), 32)
    }

    fun checkCode(key: ByteArray, code: String): String =
        hmac(key, code.toByteArray()).joinToString("") { "%02x".format(it) }.substring(0, 16)

    private fun uToRaw(u: BigInteger): ByteArray {
        val be = u.toByteArray().let { if (it.size > 32 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        val out = ByteArray(32)
        be.copyInto(out, 32 - be.size)
        return out.reversedArray()
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    /** HKDF-SHA256 (RFC 5869), salt vuoto. */
    private fun hkdf(ikm: ByteArray, info: ByteArray, len: Int): ByteArray {
        val prk = hmac(ByteArray(32), ikm)
        var t = ByteArray(0); val out = ArrayList<Byte>(len); var i = 1
        while (out.size < len) { t = hmac(prk, t + info + byteArrayOf(i.toByte())); out.addAll(t.toList()); i++ }
        return out.take(len).toByteArray()
    }
}
```

- [ ] **Step 4: verde** — `./gradlew :core:testDebugUnitTest --console=plain 2>&1 | tail -3` → `BUILD SUCCESSFUL`.

- [ ] **Step 5: commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/crypto/Blob.kt core/src/main/kotlin/it/pixelbox/cmwatch/crypto/Pairing.kt core/src/test/kotlin/it/pixelbox/cmwatch/crypto/BlobTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/crypto/PairingTest.kt
git commit -m "feat(watch): E2E blob — AES-256-GCM {v,enc} documents, X25519 + HKDF pairing key, check code (same layout as cm-relay-crypto.py)"
```

Nota per la sessione `claude-master`: il layout scelto (nonce ‖ ct, AAD `claude-master-relay-v1`, HKDF salt vuoto, chiave pubblica X25519 grezza 32 byte in base64) va confermato con un vettore di prova quando `cm-relay-crypto.py` esiste: un `enc` prodotto dal Python decifrato da `BlobTest`. Messaggio da mandare a fine fase 2.

### Task 4: `Transport` e `FakeTransport` sulle fixture

**Files:**
- Create: `transport/Transport.kt`, `transport/FakeTransport.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/transport/FakeTransportTest.kt`

**Interfaces:**
- Produces:
```kotlin
interface Transport {
    val state: Flow<State>                 // ogni cambiamento di /state, dopo la decifratura
    val events: Flow<List<Event>>          // /events ordinati per ts decrescente
    suspend fun fetchState(): State        // un GET (sveglia FCM)
    suspend fun send(cmd: Cmd): CmdResult  // attende /result; TransportException.Timeout dopo 20 s
    suspend fun pair(code: String, deviceName: String): PairingInfo
}
data class PairingInfo(val uid: String, val host: String)
sealed class TransportException(msg: String) : Exception(msg) { class Timeout(id: String); class NotPaired; class Network(msg: String) }
class FakeTransport(private val load: (String) -> String, private val now: () -> Long = { System.currentTimeMillis() / 1000 }) : Transport {
    fun useFixture(name: String)   // "state-1-question" | "state-2-idle" | "state-3-stale"
}
```
- Consumes: `ContractJson`, modello del Task 2.

- [ ] **Step 1: test**

```kotlin
package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeTransportTest {
    private val clock = 1789210800L + 30
    private fun t() = FakeTransport(load = { Fixtures.read("$it.json") }, now = { clock })

    @Test fun startsOnQuestionFixtureRebasedToNow() = runTest {
        val s = t().state.first()
        assertEquals(4, s.sessions.size)
        assertEquals(clock, s.ts)                               // ts riportato a «adesso»
        assertEquals(1789210500L + 30, s.sessions[0].question!!.askedAt)   // stessi scarti relativi
    }

    @Test fun staleFixtureKeepsOldTs() = runTest {
        val tr = t(); tr.useFixture("state-3-stale")
        assertEquals(1789200000L, tr.state.first().ts)
    }

    @Test fun answerRemovesTheQuestionAndReports() = runTest {
        val tr = t()
        val r = tr.send(Cmd("u1", CmdOp.ANSWER, "ledger-api", "1", clock, "test"))
        assertTrue(r.ok); assertEquals("answered 1. yes", r.text)
        val s = tr.state.first()
        val led = s.sessions.first { it.name == "ledger-api" }
        assertNull(led.question); assertEquals(SessionState.BUSY, led.state)
        assertEquals("ledger-api", s.sessions[1].name)          // ordine: atlas-shop (busy) prima per alfabeto
    }

    @Test fun answerOnUnknownSessionFails() = runTest {
        val r = t().send(Cmd("u2", CmdOp.ANSWER, "nope", "1", clock, "test"))
        assertFalse(r.ok)
    }

    @Test fun promptAndScreenAndFollow() = runTest {
        val tr = t()
        assertEquals("delivered", tr.send(Cmd("u3", CmdOp.PROMPT, "atlas-shop", "ciao", clock, "test")).text)
        assertTrue(tr.send(Cmd("u4", CmdOp.SCREEN, "atlas-shop", null, clock, "test")).text.lines().size in 1..30)
        tr.send(Cmd("u5", CmdOp.FOLLOW, "atlas-shop", null, clock, "test"))
        assertTrue(tr.state.first().sessions.first { it.name == "atlas-shop" }.followed)
        assertFalse(tr.state.first().sessions.first { it.name == "ledger-api" }.followed)
    }

    @Test fun duplicateIdReturnsSameResult() = runTest {
        val tr = t(); val c = Cmd("dup", CmdOp.ANSWER, "ledger-api", "2", clock, "test")
        assertEquals(tr.send(c), tr.send(c))
    }

    @Test fun eventsComeFromFixtureNewestFirst() = runTest {
        val ev = t().events.first()
        assertEquals(6, ev.size); assertTrue(ev[0].ts >= ev[1].ts)
    }

    @Test fun pairAcceptsAnySixDigits() = runTest {
        assertEquals("crostini-demo", t().pair("123456", "watch").host)
        assertThrows(TransportException.Network::class.java) { kotlinx.coroutines.runBlocking { t().pair("12", "watch") } }
    }
}
```

- [ ] **Step 2: rosso** — compila? No: `Transport` assente.

- [ ] **Step 3: implementazione**

`Transport.kt`:
```kotlin
package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.Flow

data class PairingInfo(val uid: String, val host: String)

sealed class TransportException(msg: String) : Exception(msg) {
    class Timeout(id: String) : TransportException("no result for $id")
    class NotPaired : TransportException("not paired")
    class Network(msg: String) : TransportException(msg)
}

interface Transport {
    val state: Flow<State>
    val events: Flow<List<Event>>
    suspend fun fetchState(): State
    suspend fun send(cmd: Cmd): CmdResult
    suspend fun pair(code: String, deviceName: String): PairingInfo
    companion object { const val RESULT_TIMEOUT_MS = 20_000L }
}
```

`FakeTransport.kt`:
```kotlin
package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Legge le fixture del contratto e simula il PC. Gli scarti temporali delle fixture 1 e 2 vengono riportati a «adesso». */
class FakeTransport(
    private val load: (String) -> String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
) : Transport {
    private val current = MutableStateFlow(rebase(ContractJson.decodeState(load("state-1-question"))))
    private val eventList = ContractJson.decodeEvents(load("events-sample")).sortedByDescending { it.ts }
    private val results = HashMap<String, CmdResult>()

    override val state: Flow<State> get() = current
    override val events: Flow<List<Event>> = MutableStateFlow(eventList)

    fun useFixture(name: String) {
        val s = ContractJson.decodeState(load(name))
        current.value = if (name.endsWith("stale")) s else rebase(s)
    }

    private fun rebase(s: State): State {
        val d = now() - s.ts
        fun sh(t: Long?) = t?.plus(d)
        return s.copy(ts = s.ts + d, sessions = s.sessions.map { ses ->
            ses.copy(since = ses.since + d, turnStarted = sh(ses.turnStarted),
                question = ses.question?.let { it.copy(askedAt = it.askedAt + d) },
                outcome = ses.outcome?.let { it.copy(at = it.at + d) })
        })
    }

    override suspend fun fetchState(): State = current.value

    override suspend fun send(cmd: Cmd): CmdResult {
        results[cmd.id]?.let { return it }
        val s = current.value
        val ses = s.sessions.firstOrNull { it.name == cmd.session }
        fun ok(text: String) = CmdResult(cmd.id, true, text, now())
        fun ko(text: String) = CmdResult(cmd.id, false, text, now())
        val r = when (cmd.op) {
            CmdOp.ANSWER -> {
                val q = ses?.question
                val opt = q?.options?.firstOrNull { it.n.toString() == cmd.arg }
                when {
                    ses == null -> ko("no session ${cmd.session}")
                    q == null -> ko("${ses.name} has no question")
                    else -> { replace(ses.copy(question = null, state = SessionState.BUSY, since = now())); ok("answered ${cmd.arg}. ${opt?.label ?: cmd.arg}") }
                }
            }
            CmdOp.PROMPT -> if (ses == null) ko("no session ${cmd.session}") else { replace(ses.copy(state = SessionState.BUSY, question = null, turnStarted = now())); ok("delivered") }
            CmdOp.LAUNCH -> s.projects.firstOrNull { it.path == cmd.arg }?.let { ok("launched ${it.name} (${it.account})") } ?: ko("unknown project")
            CmdOp.FOLLOW -> if (ses == null) ko("no session") else { current.value = s.copy(sessions = s.sessions.map { it.copy(followed = it.name == ses.name) }); ok("following ${ses.name}") }
            CmdOp.UNFOLLOW -> { current.value = s.copy(sessions = s.sessions.map { it.copy(followed = false) }); ok("unfollowed") }
            CmdOp.RESUME -> if (ses?.state == SessionState.GONE) ko("${ses.name} is gone: use launch") else ok("resumed")
            CmdOp.SCREEN -> if (ses == null) ko("no session") else ok("$ pytest -q tests\n42 passed in 3.1s\nEdit app/admin.py\nRead app/seed.py")
            CmdOp.ALLOW_ALL -> ko("no «don't ask again» option on this question")
        }
        results[cmd.id] = r
        return r
    }

    private fun replace(ses: Session) {
        val s = current.value
        current.value = s.copy(ts = now(), sessions = Order.sessions(s.sessions.map { if (it.id == ses.id) ses else it }))
    }

    override suspend fun pair(code: String, deviceName: String): PairingInfo {
        if (!code.matches(Regex("\\d{6}"))) throw TransportException.Network("bad code")
        return PairingInfo(uid = "fake-$deviceName", host = current.value.host)
    }
}
```

- [ ] **Step 4: verde** — `./gradlew :core:testDebugUnitTest --console=plain 2>&1 | tail -3`.

- [ ] **Step 5: commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/transport/Transport.kt core/src/main/kotlin/it/pixelbox/cmwatch/transport/FakeTransport.kt core/src/test/kotlin/it/pixelbox/cmwatch/transport/FakeTransportTest.kt
git commit -m "feat(watch): Transport interface and FakeTransport on the contract fixtures — answer/prompt/follow/screen simulated, times rebased to now"
```

### Task 5: Room, `Store`, `Repo` (stato, comandi ottimistici, coda offline)

**Files:**
- Create: `data/Db.kt`, `data/Store.kt`, `data/Repo.kt`, `settings/Prefs.kt`; Modify: `CmApp.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/data/RepoTest.kt`

**Interfaces:**
- Produces:
```kotlin
interface Store {                                  // Room dietro; MemoryStore nei test
    suspend fun loadState(): Pair<State, Long>?    // stato, receivedAt
    suspend fun saveState(s: State, receivedAt: Long)
    suspend fun loadEvents(): List<Event>; suspend fun saveEvents(ev: List<Event>); suspend fun pruneEvents(olderThan: Long)
    suspend fun loadPending(): List<Cmd>; suspend fun savePending(c: List<Cmd>)
}
data class Snapshot(val state: State?, val freshness: Freshness, val pending: List<Pending>, val lastResult: Outcome? = null)
data class Pending(val cmd: Cmd, val status: PendingStatus)        // SENDING | QUEUED | FAILED
enum class PendingStatus { SENDING, QUEUED, FAILED }
class Repo(store: Store, transport: Transport, scope: CoroutineScope, now: () -> Long, online: () -> Boolean, by: String) {
    val snapshot: StateFlow<Snapshot>
    val events: StateFlow<List<Event>>
    val results: SharedFlow<CmdResult>            // ogni /result arrivato, per aptica e toast
    suspend fun answer(session: String, n: Int): String        // ritorna l'id del comando
    suspend fun prompt(session: String, text: String): String
    suspend fun command(op: CmdOp, session: String?, arg: String?): String
    suspend fun retry(id: String); suspend fun flushQueue(); suspend fun refresh()
    fun start()                                   // collega transport.state → store → snapshot
}
```
- Consumes: `Transport`, `Store`, `Freshness`, `Order`.

- [ ] **Step 1: test** (`RepoTest.kt`, con `MemoryStore` definito nel test e un `Transport` finto controllabile):

```kotlin
package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class RepoTest {
    private class Slow(val delayMs: Long, base: FakeTransport) : Transport by base {
        var fail = false
        override suspend fun send(cmd: Cmd): CmdResult { delay(delayMs); if (fail) throw TransportException.Network("down"); return CmdResult(cmd.id, true, "answered ${cmd.arg}. ok", 0) }
    }
    private var clock = 1789210800L
    private var online = true
    private fun fake() = FakeTransport({ Fixtures.read("$it.json") }, { clock })

    @Test fun opensFromStoreThenFollowsTransport() = runTest {
        val store = MemoryStore()
        store.saveState(ContractJson.decodeState(Fixtures.stateIdle), clock - 10)
        val repo = Repo(store, fake(), backgroundScope, { clock }, { online }, "test")
        assertEquals(1, repo.snapshot.value.state!!.sessions.size)     // da Room, subito
        repo.start(); advanceUntilIdle()
        assertEquals(4, repo.snapshot.value.state!!.sessions.size)     // poi dal transport
        assertEquals(Freshness.Fresh, repo.snapshot.value.freshness)
        assertEquals(4, store.loadState()!!.first.sessions.size)
    }

    @Test fun staleStateIsFlagged() = runTest {
        val tr = fake(); tr.useFixture("state-3-stale")
        val repo = Repo(MemoryStore(), tr, backgroundScope, { clock }, { online }, "test"); repo.start(); advanceUntilIdle()
        assertTrue(repo.snapshot.value.freshness is Freshness.Stale)
    }

    @Test fun answerIsOptimisticThenConfirmed() = runTest {
        val repo = Repo(MemoryStore(), fake(), backgroundScope, { clock }, { online }, "test"); repo.start(); advanceUntilIdle()
        val got = mutableListOf<CmdResult>(); backgroundScope.launch { repo.results.collect { got += it } }
        val id = repo.answer("ledger-api", 1); advanceUntilIdle()
        assertTrue(repo.snapshot.value.pending.isEmpty())
        assertEquals(1, got.size); assertEquals(id, got[0].id); assertTrue(got[0].ok)
        assertNull(repo.snapshot.value.state!!.sessions.first { it.name == "ledger-api" }.question)
    }

    @Test fun noResultWithin20sBecomesFailedAndRetryIsSafe() = runTest {
        val slow = Slow(25_000, fake())
        val repo = Repo(MemoryStore(), slow, backgroundScope, { clock }, { online }, "test"); repo.start(); advanceUntilIdle()
        val id = repo.answer("ledger-api", 1)
        advanceTimeBy(21_000)
        assertEquals(PendingStatus.FAILED, repo.snapshot.value.pending.single().status)
        slow.delayMs.let {}; repo.retry(id); advanceUntilIdle()
        assertTrue(repo.snapshot.value.pending.isEmpty())
    }

    @Test fun offlineQueuesAtMostTenAndDropsAfterTenMinutes() = runTest {
        online = false
        val store = MemoryStore()
        val repo = Repo(store, fake(), backgroundScope, { clock }, { online }, "test"); repo.start(); advanceUntilIdle()
        repeat(12) { repo.prompt("atlas-shop", "m$it") }
        assertEquals(10, repo.snapshot.value.pending.size)
        assertTrue(repo.snapshot.value.pending.all { it.status == PendingStatus.QUEUED })
        assertEquals(10, store.loadPending().size)
        clock += 11 * 60; online = true; repo.flushQueue(); advanceUntilIdle()
        assertTrue(repo.snapshot.value.pending.isEmpty())                 // scartati con avviso, non inviati
        online = false; repo.prompt("atlas-shop", "fresh"); online = true; repo.flushQueue(); advanceUntilIdle()
        assertTrue(repo.snapshot.value.pending.isEmpty())                 // inviato
    }
}

class MemoryStore : Store {
    private var state: Pair<State, Long>? = null
    private var events: List<Event> = emptyList()
    private var pending: List<Cmd> = emptyList()
    override suspend fun loadState() = state
    override suspend fun saveState(s: State, receivedAt: Long) { state = s to receivedAt }
    override suspend fun loadEvents() = events
    override suspend fun saveEvents(ev: List<Event>) { events = (ev + events).distinctBy { it.key }.sortedByDescending { it.ts } }
    override suspend fun pruneEvents(olderThan: Long) { events = events.filter { it.ts >= olderThan } }
    override suspend fun loadPending() = pending
    override suspend fun savePending(c: List<Cmd>) { pending = c }
}
```

Nel test `noResultWithin20s…` il `retry` deve riuscire: `Slow.delayMs` è `val`; rendilo `var` e nel test imposta `slow.delayMs = 10` prima di `repo.retry(id)`.

- [ ] **Step 2: rosso**.

- [ ] **Step 3: implementazione**

`Db.kt`:
```kotlin
package it.pixelbox.cmwatch.data

import android.content.Context
import androidx.room.*
import it.pixelbox.cmwatch.contract.*

@Entity(tableName = "state") data class StateRow(@PrimaryKey val id: Int = 1, val json: String, val receivedAt: Long)
@Entity(tableName = "events") data class EventRow(@PrimaryKey val key: String, val ts: Long, val session: String?, val json: String)
@Entity(tableName = "pending") data class PendingRow(@PrimaryKey val id: String, val issued: Long, val json: String)

@Dao interface CmDao {
    @Query("SELECT * FROM state WHERE id = 1") suspend fun state(): StateRow?
    @Upsert suspend fun putState(row: StateRow)
    @Query("SELECT * FROM events ORDER BY ts DESC") suspend fun events(): List<EventRow>
    @Upsert suspend fun putEvents(rows: List<EventRow>)
    @Query("DELETE FROM events WHERE ts < :olderThan") suspend fun pruneEvents(olderThan: Long)
    @Query("SELECT * FROM pending ORDER BY issued") suspend fun pending(): List<PendingRow>
    @Query("DELETE FROM pending") suspend fun clearPending()
    @Insert suspend fun putPending(rows: List<PendingRow>)
}

@Database(entities = [StateRow::class, EventRow::class, PendingRow::class], version = 1, exportSchema = true)
abstract class Db : RoomDatabase() {
    abstract fun dao(): CmDao
    companion object {
        fun open(ctx: Context): Db = Room.databaseBuilder(ctx, Db::class.java, "cmwatch.db").fallbackToDestructiveMigration(true).build()
    }
}

class RoomStore(private val dao: CmDao) : Store {
    override suspend fun loadState() = dao.state()?.let { ContractJson.decodeState(it.json) to it.receivedAt }
    override suspend fun saveState(s: State, receivedAt: Long) = dao.putState(StateRow(1, ContractJson.json.encodeToString(State.serializer(), s), receivedAt))
    override suspend fun loadEvents() = dao.events().map { ContractJson.json.decodeFromString(Event.serializer(), it.json) }
    override suspend fun saveEvents(ev: List<Event>) = dao.putEvents(ev.map { EventRow(it.key, it.ts, it.session, ContractJson.json.encodeToString(Event.serializer(), it)) })
    override suspend fun pruneEvents(olderThan: Long) = dao.pruneEvents(olderThan)
    override suspend fun loadPending() = dao.pending().map { ContractJson.json.decodeFromString(Cmd.serializer(), it.json) }
    override suspend fun savePending(c: List<Cmd>) { dao.clearPending(); dao.putPending(c.map { PendingRow(it.id, it.issued, ContractJson.encode(it)) }) }
}
```

`Store.kt`: l'interfaccia `Store` come nel blocco Interfaces.

`Repo.kt`:
```kotlin
package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.Transport
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

enum class PendingStatus { SENDING, QUEUED, FAILED }
data class Pending(val cmd: Cmd, val status: PendingStatus)
data class Snapshot(val state: State?, val freshness: Freshness, val pending: List<Pending> = emptyList())

class Repo(
    private val store: Store,
    private val transport: Transport,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val online: () -> Boolean,
    private val by: String,
) {
    private val _snapshot = MutableStateFlow(Snapshot(null, Freshness.Stale(0)))
    val snapshot: StateFlow<Snapshot> = _snapshot
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events
    private val _results = MutableSharedFlow<CmdResult>(extraBufferCapacity = 16)
    val results: SharedFlow<CmdResult> = _results
    private val _notices = MutableSharedFlow<Notice>(extraBufferCapacity = 16)
    val notices: SharedFlow<Notice> = _notices
    private val jobs = HashMap<String, Job>()

    init {
        // Apertura immediata da Room: l'ultimo stato è leggibile anche senza rete.
        runBlocking {
            store.loadState()?.let { (s, _) -> _snapshot.value = Snapshot(s, Freshness.of(s.ts, now())) }
            _events.value = store.loadEvents()
            _snapshot.update { it.copy(pending = store.loadPending().map { c -> Pending(c, PendingStatus.QUEUED) }) }
        }
    }

    fun start() {
        scope.launch { transport.state.collect { s -> accept(s) } }
        scope.launch { transport.events.collect { ev -> store.saveEvents(ev); store.pruneEvents(now() - 30L * 86400); _events.value = store.loadEvents() } }
        scope.launch { while (isActive) { delay(30_000); _snapshot.update { it.copy(freshness = it.state?.let { s -> Freshness.of(s.ts, now()) } ?: Freshness.Stale(0)) } } }
    }

    private suspend fun accept(s: State) {
        val ordered = s.copy(sessions = Order.sessions(s.sessions))
        store.saveState(ordered, now())
        _snapshot.update { it.copy(state = ordered, freshness = Freshness.of(ordered.ts, now())) }
    }

    suspend fun refresh() = runCatching { accept(transport.fetchState()) }.isSuccess

    suspend fun answer(session: String, n: Int) = command(CmdOp.ANSWER, session, n.toString())
    suspend fun prompt(session: String, text: String) = command(CmdOp.PROMPT, session, text)

    suspend fun command(op: CmdOp, session: String?, arg: String?): String {
        val cmd = Cmd(UUID.randomUUID().toString(), op, session, arg, now(), by)
        if (!online()) { enqueue(cmd); return cmd.id }
        dispatch(cmd)
        return cmd.id
    }

    private suspend fun enqueue(cmd: Cmd) {
        val queued = _snapshot.value.pending.filter { it.status == PendingStatus.QUEUED }.map { it.cmd }
        if (queued.size >= MAX_QUEUE) { _notices.tryEmit(Notice.QueueFull); return }
        _snapshot.update { it.copy(pending = it.pending + Pending(cmd, PendingStatus.QUEUED)) }
        store.savePending(queued + cmd)
    }

    private fun dispatch(cmd: Cmd) {
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id } + Pending(cmd, PendingStatus.SENDING)) }
        if (cmd.op == CmdOp.ANSWER || cmd.op == CmdOp.PROMPT) optimistic(cmd)
        jobs[cmd.id] = scope.launch {
            val r = try {
                withTimeout(Transport.RESULT_TIMEOUT_MS) { transport.send(cmd) }
            } catch (e: TimeoutCancellationException) { null } catch (e: TransportException) { null }
            if (r == null) {
                _snapshot.update { it.copy(pending = it.pending.map { p -> if (p.cmd.id == cmd.id) p.copy(status = PendingStatus.FAILED) else p }) }
            } else {
                _snapshot.update { it.copy(pending = it.pending.filter { p -> p.cmd.id != cmd.id }) }
                _results.emit(r)
            }
        }
    }

    /** La domanda sparisce subito dallo schermo; la verità torna con /state. */
    private fun optimistic(cmd: Cmd) {
        _snapshot.update { snap ->
            val s = snap.state ?: return@update snap
            snap.copy(state = s.copy(sessions = s.sessions.map { if (it.name == cmd.session) it.copy(question = null, state = SessionState.BUSY) else it }))
        }
    }

    suspend fun retry(id: String) {
        val p = _snapshot.value.pending.firstOrNull { it.cmd.id == id } ?: return
        dispatch(p.cmd)   // stesso uuid: il PC ignora i duplicati
    }

    suspend fun flushQueue() {
        if (!online()) return
        val queued = _snapshot.value.pending.filter { it.status == PendingStatus.QUEUED }.map { it.cmd }
        store.savePending(emptyList())
        _snapshot.update { it.copy(pending = it.pending.filter { p -> p.status != PendingStatus.QUEUED }) }
        val (fresh, old) = queued.partition { now() - it.issued <= MAX_QUEUE_AGE_S }
        if (old.isNotEmpty()) _notices.tryEmit(Notice.Dropped(old.size))
        fresh.forEach { dispatch(it) }
    }

    sealed class Notice { data object QueueFull : Notice(); data class Dropped(val n: Int) : Notice() }
    companion object { const val MAX_QUEUE = 10; const val MAX_QUEUE_AGE_S = 600L }
}
```

`Prefs.kt` (DataStore Preferences): chiavi `uid`, `deviceName` (default «watch-pixel5»), `host`, `ttsMinChars` (120), `hapticsQuestion|Outcome|Gone` (true), `complicationAccount` («personale»), `demoFixture` («state-1-question»), `paired: Boolean`. API: `class Prefs(ctx)` con `val flow: Flow<Settings>` e `suspend fun update(block: (Settings) -> Settings)`.

`CmApp.kt`:
```kotlin
package it.pixelbox.cmwatch

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import it.pixelbox.cmwatch.data.*
import it.pixelbox.cmwatch.settings.Prefs
import it.pixelbox.cmwatch.transport.FakeTransport
import it.pixelbox.cmwatch.transport.Transport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CmApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs; lateinit var transport: Transport; lateinit var repo: Repo

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        transport = FakeTransport(load = { assets.open("contract/$it.json").bufferedReader().readText() })
        val store = RoomStore(Db.open(this).dao())
        repo = Repo(store, transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, "watch-pixel5")
        repo.start()
    }

    fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

`Repo.init` con `runBlocking` su Room: accettabile perché la riga è una (≤ 8 KB) e l'apertura da Room «subito» è nel design; se al primo avvio su orologio la finestra supera 100 ms, spostare in `start()` con un `LaunchedEffect` di attesa.

- [ ] **Step 4: verde** — `./gradlew :core:testDebugUnitTest :wear:assembleDebug --console=plain 2>&1 | tail -3`.

- [ ] **Step 5: commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/data/Db.kt core/src/main/kotlin/it/pixelbox/cmwatch/data/Store.kt core/src/main/kotlin/it/pixelbox/cmwatch/data/Repo.kt core/src/main/kotlin/it/pixelbox/cmwatch/settings/Prefs.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt core/src/test/kotlin/it/pixelbox/cmwatch/data/RepoTest.kt core/schemas
git commit -m "feat(watch): Room store and Repo — last state readable offline, optimistic answer, 20 s result timeout with retry, offline queue (10 commands, 10 minutes)"
```

### Task 6: tema, componenti, schermata Sessioni

**Files:**
- Create: `:core` `rules/SessionsText.kt`; `:wear` `ui/theme/Theme.kt`, `ui/components/StateIcon.kt`, `ui/components/AccountDot.kt`, `ui/components/SessionRow.kt`, `ui/components/StaleChip.kt`, `ui/components/WideButton.kt`, `ui/screens/SessionsScreen.kt`; Modify: `res/values/strings.xml`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/SessionsTextTest.kt`

**Interfaces:**
- Produces: `CmTheme { content }`, `CmColors` (`bg, surface, line, text, text2, accent, accentPressed, waiting, busy, idle, gone, stale`), `stateColor(SessionState, fresh: Boolean): Color`; `StateIcon(state, modifier)`; `AccountDot(account)`; `SessionRow(session, now, onClick, transformation)`; `StaleChip(minutes)`; `WideButton(text, onClick, primary: Boolean, enabled, onLongClick)`; `SessionsScreen(snapshot, now, onOpen: (String) -> Unit, onSettings)`; `SessionsText.row(session, now): String` («ledger-api · 2 m»: icona e pallino sono composable) e `SessionsText.header(list, sessionsLabel)` — in `:core`, senza italiano.

- [ ] **Step 1: test del testo di riga** (il resto della schermata si verifica con Paparazzi nel Task 10 e dal vivo):

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionsTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = 1789210800L

    @Test fun waitingShowsAgeOfTheQuestion() = assertEquals("ledger-api · 5 m", SessionsText.row(s.sessions[0], now))
    @Test fun busyShowsTurnAge() = assertEquals("atlas-shop · 1 m", SessionsText.row(s.sessions[1], now))
    @Test fun idleShowsSinceAge() = assertEquals("field-notes · 1 g", SessionsText.row(s.sessions[2], now))
    @Test fun goneShowsNoAge() = assertEquals("orbit-docs", SessionsText.row(s.sessions[3], now))
    @Test fun headerCounts() = assertEquals("4 sessioni · 1 ❓ · 1 ✗", SessionsText.header(s.sessions, "sessioni"))
}
```

- [ ] **Step 2: rosso**.

- [ ] **Step 3: implementazione**

`SessionsText.kt`:
```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*

object SessionsText {
    fun row(s: Session, now: Long): String {
        val from = when (s.state) {
            SessionState.WAITING -> s.question?.askedAt ?: s.since
            SessionState.BUSY -> s.turnStarted ?: s.since
            SessionState.GONE -> return s.name
            else -> s.since
        }
        return "${s.name} · ${Durations.since(from, now)}"
    }
    fun header(list: List<Session>, sessionsLabel: String): String {
        val q = list.count { it.question != null }; val g = list.count { it.state == SessionState.GONE }
        val parts = mutableListOf("${list.size} $sessionsLabel")
        if (q > 0) parts += "$q ❓"; if (g > 0) parts += "$g ✗"
        return parts.joinToString(" · ")
    }
}
```

`Theme.kt`:
```kotlin
package it.pixelbox.cmwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import it.pixelbox.cmwatch.contract.SessionState

object CmColors {
    val bg = Color(0xFF000000); val surface = Color(0xFF121417); val line = Color(0xFF2A2E35)
    val text = Color(0xFFF2F4F7); val text2 = Color(0xFF9AA3B2)
    val accent = Color(0xFF4C7DFF); val accentPressed = Color(0xFF3457D5)
    val waiting = Color(0xFFFFB020); val busy = Color(0xFF7FA1FF); val idle = Color(0xFF34C759)
    val gone = Color(0xFFFF453A); val stale = Color(0xFF6B7280)
}

fun stateColor(s: SessionState, fresh: Boolean = true): Color = if (!fresh) CmColors.stale else when (s) {
    SessionState.WAITING -> CmColors.waiting; SessionState.BUSY -> CmColors.busy
    SessionState.IDLE, SessionState.AWAITING -> CmColors.idle; SessionState.GONE -> CmColors.gone
}

private val scheme = ColorScheme(
    primary = CmColors.accent, onPrimary = CmColors.text, primaryContainer = CmColors.accent, onPrimaryContainer = CmColors.text,
    secondary = CmColors.text2, onSecondary = CmColors.bg,
    tertiary = CmColors.busy, onTertiary = CmColors.bg,
    background = CmColors.bg, onBackground = CmColors.text,
    surfaceContainerLow = CmColors.surface, surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surface,
    onSurface = CmColors.text, onSurfaceVariant = CmColors.text2, outline = CmColors.line, outlineVariant = CmColors.line,
    error = CmColors.gone, onError = CmColors.text, errorContainer = CmColors.gone, onErrorContainer = CmColors.text,
)

val Mono = FontFamily.Monospace
val typography = Typography(
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp), bodyMedium = TextStyle(fontSize = 15.sp), bodySmall = TextStyle(fontSize = 13.sp),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, fontFamily = Mono),
    displaySmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
)

@Composable fun CmTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, typography = typography, content = content)
```

I nomi dei campi di `ColorScheme` e `Typography` vanno controllati contro `compose-material3` 1.6.2 alla prima compilazione (sono quelli dell'API 1.5/1.6: `surfaceContainerLow/High`, `outlineVariant`, `arcLarge…`); se uno manca, toglilo: i default valgono.

`StateIcon.kt`: `Icon(imageVector = when(state) { WAITING -> Icons.Rounded.Help; BUSY -> Icons.Rounded.PlayArrow; IDLE/AWAITING -> Icons.Rounded.Check; GONE -> Icons.Rounded.Close }, contentDescription = stringResource(...), tint = stateColor(state, fresh))`, 20 dp.

`AccountDot.kt`: `Box(Modifier.size(10.dp).background(if (account == "agenzia") Color(0xFFE53935) else Color(0xFF43A047), CircleShape))` (🔴 agenzia, 🟢 personale come Telegram).

`SessionRow.kt` — riga a tutta larghezza, una riga logica = una riga fisica:
```kotlin
@Composable
fun SessionRow(s: Session, now: Long, fresh: Boolean, onClick: () -> Unit, transformation: SurfaceTransformation?, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick, modifier = modifier.fillMaxWidth(), transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text),
        border = BorderStroke(1.dp, CmColors.line),
        icon = { StateIcon(s.state, fresh) },
        label = {
            AccountDot(s.account); Spacer(Modifier.width(6.dp))
            Text(SessionsText.row(s, now), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
        },
        secondaryLabel = s.question?.let { { Text(it.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = CmColors.text2) } },
    )
}
```

`WideButton.kt` (una pillola 52 dp a tutta larghezza; `primary` = l'unico bottone pieno accent della schermata):
```kotlin
@Composable
fun WideButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true, onLongClick: (() -> Unit)? = null, transformation: SurfaceTransformation? = null) {
    val m = modifier.fillMaxWidth().heightIn(min = 52.dp)
    if (primary) Button(onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
        colors = ButtonDefaults.buttonColors(containerColor = CmColors.accent, contentColor = CmColors.text)) { Text(text, maxLines = 2) }
    else FilledTonalButton(onClick = onClick, onLongClick = onLongClick, enabled = enabled, modifier = m, transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text), border = BorderStroke(1.dp, CmColors.line)) { Text(text, maxLines = 2) }
}
```

`StaleChip.kt`: `Text(stringResource(R.string.pc_stale, minutes), color = CmColors.stale, style = bodySmall)` dentro una `Box` con bordo `line` raggio 12 dp.

`SessionsScreen.kt`:
```kotlin
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val sessions = snapshot.state?.sessions.orEmpty()
    val fresh = snapshot.freshness is Freshness.Fresh
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.sessions_title)) } }
            (snapshot.freshness as? Freshness.Stale)?.let { st -> item { StaleChip(st.minutes, Modifier.transformedHeight(this, spec)) } }
            if (sessions.isEmpty()) item { Text(stringResource(R.string.sessions_empty), color = CmColors.text2, modifier = Modifier.transformedHeight(this, spec)) }
            items(sessions, key = { it.id }) { s ->
                SessionRow(s, now, fresh, onClick = { onOpen(s.name) }, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
            item { WideButton(stringResource(R.string.settings_title), onClick = onSettings, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
        }
    }
}
```

`strings.xml` aggiunte: `pc_stale` = «PC fermo da %1$d min», `settings_title` = «Impostazioni», `state_waiting` = «In attesa di risposta», `state_busy` = «Lavora», `state_idle` = «Ferma», `state_gone` = «Sparita».

- [ ] **Step 4: verde + build** — `./gradlew :core:testDebugUnitTest :wear:assembleDebug --console=plain 2>&1 | tail -3`.

- [ ] **Step 5: commit** — `feat(watch): theme tokens, state icons, session rows, Sessions screen on TransformingLazyColumn`.

### Task 7: navigazione, `ViewState`, Scheda

**Files:**
- Create: `:core` `rules/ViewState.kt`, `rules/CardText.kt`; `:wear` `ui/Nav.kt`, `ui/screens/SessionScreen.kt`; Modify: `MainActivity.kt`, `strings.xml`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/ViewStateTest.kt`, `rules/CardTextTest.kt`

**Interfaces:**
- Produces: `sealed class Screen { Sessions; Session(name); Question(name); Settings; Pairing; Outcome(name); … }`; `ViewState.reduce(snapshot, paired: Boolean, chosen: Screen, dismissedQuestionIds: Set<String>): Screen` — regole: non accoppiato → `Pairing`; domanda aperta non ancora vista → `Question(name)` (la prima nell'ordine del contratto); altrimenti `chosen`; la freschezza non cambia schermata ma i comandi si disabilitano (`Snapshot.freshness`); `CardText.header(session, now)` («ledger-api · 🔴 agenzia · In attesa di risposta · 5 m»), `CardText.next(session)` («→ Wait for the go»).

- [ ] **Step 1: test**

```kotlin
class ViewStateTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val snap = Snapshot(s, Freshness.Fresh)
    @Test fun unpairedGoesToPairing() = assertEquals(Screen.Pairing, ViewState.reduce(snap, paired = false, chosen = Screen.Sessions, seen = emptySet()))
    @Test fun openQuestionWins() = assertEquals(Screen.Question("ledger-api"), ViewState.reduce(snap, true, Screen.Sessions, emptySet()))
    @Test fun seenQuestionDoesNotReopen() = assertEquals(Screen.Sessions, ViewState.reduce(snap, true, Screen.Sessions, setOf("q-1789210500-1")))
    @Test fun answeredElsewhereClosesQuestion() {
        val idle = Snapshot(ContractJson.decodeState(Fixtures.stateIdle), Freshness.Fresh)
        assertEquals(Screen.Sessions, ViewState.reduce(idle, true, Screen.Question("ledger-api"), emptySet()))
    }
    @Test fun staleKeepsScreen() = assertEquals(Screen.Session("atlas-shop"), ViewState.reduce(snap.copy(freshness = Freshness.Stale(5)), true, Screen.Session("atlas-shop"), setOf("q-1789210500-1")))
}
class CardTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    @Test fun header() = assertEquals("ledger-api · agenzia · In attesa di risposta · 5 m", CardText.header(s.sessions[0], 1789210800, mapOf(SessionState.WAITING to "In attesa di risposta")))
    @Test fun next() = assertEquals("→ Wait for the go", CardText.next(s.sessions[0]))
    @Test fun busyShowsTool() = assertEquals("atlas-shop · personale · Lavora · 1 m · Bash pytest -q tests", CardText.header(s.sessions[1], 1789210800, mapOf(SessionState.BUSY to "Lavora")))
}
```

- [ ] **Step 2: rosso**. **Step 3: implementazione**

`ViewState.kt`:
```kotlin
sealed class Screen {
    data object Sessions : Screen(); data class Session(val name: String) : Screen(); data class Question(val name: String) : Screen()
    data object Settings : Screen(); data object Pairing : Screen(); data class Outcome(val name: String) : Screen()
    data class Terminal(val name: String) : Screen(); data object Timeline : Screen(); data object Launch : Screen()
    data object Quota : Screen(); data object Recap : Screen(); data object Night : Screen()
}
object ViewState {
    fun reduce(snap: Snapshot, paired: Boolean, chosen: Screen, seen: Set<String>): Screen {
        if (!paired) return Screen.Pairing
        val sessions = snap.state?.sessions.orEmpty()
        val open = sessions.firstOrNull { it.question != null && it.question.id !in seen }
        if (open != null) return Screen.Question(open.name)
        if (chosen is Screen.Question && sessions.none { it.name == chosen.name && it.question != null }) return Screen.Sessions
        return chosen
    }
}
```

`CardText.kt`:
```kotlin
object CardText {
    fun header(s: Session, now: Long, stateLabels: Map<SessionState, String>): String {
        val parts = mutableListOf(s.name, s.account, stateLabels[s.state] ?: s.state.name.lowercase())
        if (s.state != SessionState.GONE) parts += Durations.since(when (s.state) { SessionState.WAITING -> s.question?.askedAt ?: s.since; SessionState.BUSY -> s.turnStarted ?: s.since; else -> s.since }, now)
        s.tool?.takeIf { s.state == SessionState.BUSY }?.let { parts += it }
        return parts.joinToString(" · ")
    }
    fun next(s: Session): String? = s.next?.let { "→ $it" }
}
```

`Nav.kt`: `SwipeDismissableNavHost` con rotte `sessions`, `session/{name}`, `question/{name}`, `settings`, `pairing`, `outcome/{name}`, `terminal/{name}`, `timeline`, `launch`, `quota`, `recap`, `night`; funzione `Screen.route()` e `navigateTo(Screen)`. Un `LaunchedEffect(snapshot, paired)` in `MainActivity` chiama `ViewState.reduce` e naviga se la schermata calcolata differisce da quella corrente; le domande viste stanno in `rememberSaveable` (`seen`), aggiunte quando l'utente esce dalla Domanda senza rispondere.

`SessionScreen.kt` (Scheda): `ScreenScaffold` + `TransformingLazyColumn`: riga `CardText.header` (mono per il nome), riga `→ prossimo` se c'è, esito `short` con `SpeakButton` (Task 16: finché non c'è, il tasto ▶ è un `IconButton` che chiama `Speaker`, aggiunto in fase 4 — in fase 2 si mostra solo il testo), bottoni: **Rispondi** (primary, solo se `question != null`) → `Question(name)`; **Scrivi** → tastiera di sistema (Task 8) → `repo.prompt`; **Terminale** → `Terminal(name)` (fase 4, in fase 2 disabilitato); **Segui** → `repo.command(FOLLOW/UNFOLLOW)`. Tutti disabilitati con `Freshness.Stale`. Se la sessione non è più nello stato: testo «Sessione non più presente» e bottone **Sessioni**.

`MainActivity.kt`: `setContent { CmTheme { AppScaffold(timeText = { TimeText() }) { Nav(app.repo, app.prefs, …) } } }`, `now` da un `produceState` che avanza ogni 30 s; deep link `cmwatch://session/<name>` e `cmwatch://question/<name>` letti da `intent.data` (per tile e notifiche in fase 3).

`strings.xml` aggiunte: `card_reply` = «Rispondi», `card_write` = «Scrivi», `card_terminal` = «Terminale», `card_follow` = «Segui», `card_unfollow` = «Smetti di seguire», `card_missing` = «Sessione non più presente», `card_next_prefix` = «→», `write_hint` = «Messaggio per %1$s».

- [ ] **Step 4: verde + build**. **Step 5: commit** — `feat(watch): navigation with the single ViewState (offline > question > chosen), session card with outcome and next`.

### Task 8: schermata Domanda con `answer`, tier high, testo libero, aptica

**Files:**
- Create: `:core` `rules/QuestionRules.kt`; `:wear` `ui/screens/QuestionScreen.kt`, `haptics/Haptics.kt`, `ui/Keyboard.kt`; Modify: `Nav.kt`, `strings.xml`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/QuestionRulesTest.kt`

**Interfaces:**
- Produces: `QuestionScreen(session, enabled, onAnswer: (Int) -> Unit, onFreeText: (String) -> Unit, onBack)`; `QuestionRules.needsLongPress(tier) = tier == HIGH`, `QuestionRules.allowAllVisible(question) = tier != HIGH && kind == PERMISSION`; `Haptics.play(ctx, Haptics.Kind)` con `Kind { QUESTION(2×60 ms), OUTCOME(40), GONE(200), SENT(tick), CONFIRMED(2 tick), ERROR(3 colpi) }`; `Keyboard.launch(launcher: ActivityResultLauncher<Intent>, label: String)` + `Keyboard.result(Intent?): String?` via `RemoteInputIntentHelper` (`wear-input`).

- [ ] **Step 1: test**

```kotlin
class QuestionRulesTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion).sessions[0].question!!
    @Test fun highNeedsLongPress() { assertTrue(QuestionRules.needsLongPress(Tier.HIGH)); assertFalse(QuestionRules.needsLongPress(Tier.MEDIUM)) }
    @Test fun allowAllOnlyForPermissionsBelowHigh() {
        assertFalse(QuestionRules.allowAllVisible(q))                                     // kind = ask
        assertTrue(QuestionRules.allowAllVisible(q.copy(kind = QuestionKind.PERMISSION)))
        assertFalse(QuestionRules.allowAllVisible(q.copy(kind = QuestionKind.PERMISSION, tier = Tier.HIGH)))
    }
    @Test fun optionLabelKeepsNumber() = assertEquals("1 · yes", QuestionRules.optionLabel(q.options[0]))
}
```

- [ ] **Step 2: rosso**. **Step 3: implementazione**

`QuestionScreen.kt`: schermo intero, `ScreenScaffold` + `TransformingLazyColumn`: riga «❓ nome» (mono), testo intero della domanda a 18 sp (mai troncato, niente `maxLines`), poi un `WideButton` per opzione, uno sotto l'altro, `primary = (index == 0)`; con `tier == HIGH`: `onClick` mostra un `Text` «Tieni premuto per confermare» e `onLongClick` invia; `tier == MEDIUM`: bottone della prima opzione color ambra pieno (`Button` con `containerColor = CmColors.waiting`), `HIGH` rosso pieno; `LOW` neutro. In fondo: **Scrivi** (tastiera di sistema) e, se `QuestionRules.allowAllVisible`, **Consenti sempre** → `repo.command(ALLOW_ALL, name, null)`. Con `enabled = false` (PC fermo) i bottoni sono disabilitati e in cima c'è `StaleChip`. Al montaggio: `Haptics.play(QUESTION)` una volta per `question.id` (memorizzato in `rememberSaveable`). All'invio: `Haptics.SENT`, poi al `repo.results` con quell'id `CONFIRMED` o `ERROR`, e ritorno a `Sessions`. Se la domanda sparisce dallo stato mentre è aperta (risposta altrove): `Text(stringResource(R.string.question_answered_elsewhere))` per 1,5 s, `Haptics.OUTCOME`, poi indietro (regola del design, sezione 5).

`Haptics.kt`:
```kotlin
object Haptics {
    enum class Kind { QUESTION, OUTCOME, GONE, SENT, CONFIRMED, ERROR }
    fun play(ctx: Context, kind: Kind) {
        val v = ctx.getSystemService(Vibrator::class.java) ?: return
        val eff = when (kind) {
            Kind.QUESTION -> VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1)
            Kind.OUTCOME -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.GONE -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            Kind.SENT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Kind.CONFIRMED -> VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1)
            Kind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50, 60, 50), -1)
        }
        v.vibrate(eff)
    }
}
```

`Keyboard.kt`:
```kotlin
object Keyboard {
    const val KEY = "text"
    fun intent(label: String): Intent = RemoteInputIntentHelper.createActionRemoteInputIntent().also {
        RemoteInputIntentHelper.putRemoteInputsExtra(it, listOf(RemoteInput.Builder(KEY).setLabel(label).build()))
    }
    fun result(data: Intent?): String? = data?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(KEY)?.toString() }?.takeIf { it.isNotBlank() }
}
```
(`RemoteInput` è `android.app.RemoteInput`; `RemoteInputIntentHelper` da `androidx.wear.input`.) Il chiamante usa `rememberLauncherForActivityResult(StartActivityForResult()) { Keyboard.result(it.data)?.let(onFreeText) }`.

`strings.xml`: `question_hold` = «Tieni premuto per confermare», `question_write` = «Scrivi», `question_allow_all` = «Consenti sempre», `question_answered_elsewhere` = «Già risposta da un altro canale», `question_sent` = «Inviato», `question_not_delivered` = «Non consegnato», `question_retry` = «Riprova».

- [ ] **Step 4: verde + build**. **Step 5: commit** — `feat(watch): full-screen Question — whole text, one wide button per option, long press on tier high, free text via the system keyboard, haptics per kind`.

### Task 9: Pairing e Impostazioni (con selettore di fixture per la demo)

**Files:**
- Create: `ui/screens/PairingScreen.kt`, `ui/screens/SettingsScreen.kt`, `crypto/KeyVault.kt`; Modify: `Prefs.kt`, `Nav.kt`, `strings.xml`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/crypto/KeyVaultTest.kt` (solo la parte pura: wrap/unwrap con una chiave AES data, senza Keystore)

**Interfaces:**
- Produces: `KeyVault.wrap(key: ByteArray, kek: SecretKey): String` / `unwrap(b64: String, kek: SecretKey): ByteArray` (AES-GCM, stesso `Blob` con chiave del Keystore alias `cmwatch-kek`, `KeyGenParameterSpec` `PURPOSE_ENCRYPT|DECRYPT`, GCM, 256 bit, `setUserAuthenticationRequired(false)`); `KeyVault.store(ctx, key)` / `load(ctx): ByteArray?` che passano da `Prefs.wrappedKey`; `PairingScreen(onPair: suspend (String) -> Result<PairingInfo>)`: bottone **Inserisci codice** (tastiera di sistema numerica: `RemoteInput` con `setAllowFreeFormInput(true)`), invio, esito «Accoppiato con <host>» e vibrazione `CONFIRMED`, errore in rosso con **Riprova**; `SettingsScreen(settings, onChange, onRepair)`: righe «Soglia lettura vocale 120» (±20 con due `TextButton` sulla stessa riga: sono icone, non frammenti), «Vibrazione domanda/esito/sparita» (`SwitchButton`), «Account complication» (`RadioButton` personale/agenzia), «Nuovo pairing», e — solo in build debug — «Demo: fixture 1/2/3» che chiama `FakeTransport.useFixture`.

- [ ] **Step 1: test** `KeyVaultTest`: `wrap` poi `unwrap` con `SecretKeySpec` casuale restituisce la chiave; `unwrap` con altra kek lancia `BlobException`.
- [ ] **Step 2-3: rosso, implementazione.** Nel pairing finto: `transport.pair(code, deviceName)` → `Prefs.update { paired = true, uid, host }`, chiave = `Pairing.sharedKey(kp.private, pcPub)` solo con Firebase (fase 3); col finto si salva una chiave casuale nel vault per esercitare il percorso.
- [ ] **Step 4: verde + build.** **Step 5: commit** — `feat(watch): pairing screen with the six-digit code, key wrapped in the Keystore, settings (TTS threshold, haptics, complication account, demo fixtures)`.

### Task 10: prova di fumo, screenshot test, checklist fase 2

**Files:**
- Create: `docs/verifiche/fase-2.md`, `wear/src/test/kotlin/it/pixelbox/cmwatch/wear/ScreensSnapshotTest.kt` (Paparazzi), Modify: `wear/build.gradle.kts` (plugin `app.cash.paparazzi` 1.3.5 nel catalogo, `alias(libs.plugins.paparazzi)`), `.github/workflows/build-android.yml` (già pronto).

- [ ] **Step 1: prova di fumo su ARC** — `./gradlew :wear:assembleDebug` poi `adb -s emulator-5554 install -r wear/build/outputs/apk/debug/wear-debug.apk && adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/it.pixelbox.cmwatch.wear.MainActivity`; `adb -s emulator-5554 exec-out screencap -p > docs/screenshots/fase2-sessioni-arc.png`; il log: `adb logcat -d | grep -E "cmwatch|AndroidRuntime" | tail`. Atteso: nessun crash; lista con quattro righe; tap su ledger-api → Domanda; tap «1 · yes» → torna alla lista con ledger-api ▶. Se ARC rifiuta l'APK (feature watch), lo si dice e si passa al punto 3.
- [ ] **Step 2: Paparazzi** — `ScreensSnapshotTest` con `@get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = 456, screenHeight = 456, density = Density.XHIGH))` e quattro `@Test` (`sessions`, `card`, `question`, `stale`) che chiamano `paparazzi.snapshot { CmTheme { …Screen(Snapshot(fixture)) } }`. In locale su aarch64 Paparazzi non parte (layoutlib nativo assente): `./gradlew :wear:recordPaparazziDebug` si lancia in Actions con `workflow_dispatch` e le immagini si scaricano come artifact e si committano in `wear/src/test/snapshots/`. Se il plugin 1.3.5 non compila con AGP 8.13, si prova `2.0.0-alpha05`; se nemmeno, il task resta aperto e lo si scrive qui.
- [ ] **Step 3: checklist** `docs/verifiche/fase-2.md`: 1) l'app apre da Room senza rete; 2) lista in ordine ❓ ▶ ✓ ✗; 3) Scheda con esito e → prossimo; 4) Domanda a schermo intero con bottoni larghi; 5) risposta «1» → domanda sparita, vibrazione; 6) fixture 3 → chip «PC fermo», comandi disabilitati; 7) tastiera di sistema per «Scrivi»; 8) pairing con codice 6 cifre. Le voci «dal vivo sul Pixel Watch» e «una domanda reale risposta dal polso, registrata nel ledger» richiedono il relay (fase 1) e Franz: si segnano come «in attesa».
- [ ] **Step 4: commit** — `test: Paparazzi snapshots of the four phase-2 screens (recorded in Actions); docs: phase-2 live checklist`.
- [ ] **Step 5: messaggio** alla sessione `claude-master` (SendMessage): layout crypto da confermare con un vettore, e `awaiting` nell'ordine; alla `master`: esito fase 2, cosa serve a Franz (Firebase, orologio).

---

## Fase 3 — complication, tile, notifiche, aptica, FCM, Firebase

Prerequisito che ferma: progetto Firebase (RTDB + FCM, app Android `it.pixelbox.cmwatch`, `google-services.json` in `wear/` (e in `mobile/` quando servirà), mai committato) e il relay reale della fase 1. Fino ad allora `FirebaseTransport` si prova solo su `MockWebServer`.

### Task 11: `FirebaseTransport` (REST + SSE + auth anonima)

**Files:** `transport/FirebaseTransport.kt`, `transport/Rtdb.kt` (client OkHttp: `get(path)`, `put(path, json)`, `delete(path)`, `stream(path): Flow<String>` SSE con `okhttp-sse`, `?auth=<idToken>`), `transport/AuthToken.kt` (`FirebaseAuth.signInAnonymously()` → `getIdToken`), test `transport/FirebaseTransportTest.kt` con `MockWebServer`.

**Interfaces:** `FirebaseTransport(baseUrl, token: suspend () -> String, key: suspend () -> ByteArray?, uid: () -> String?, keyPair: () -> KeyPair)`. `state` = SSE su `/state.json`: eventi `put`/`patch` con `data` → `Blob.open` → `decodeState`; riconnessione con backoff 1-2-5-15-30 s; `events` = GET `/events.json?orderBy="$key"&limitToLast=200` a ogni apertura + SSE; `send`: PUT `/cmd/<uuid>.json` = `Blob.seal(encode(cmd))`, poi SSE (o poll ogni 1 s) su `/result/<uuid>.json` fino a 20 s → `decodeResult(Blob.open(...))`; `pair(code, name)`: GET `/pair/<code>.json` → `{pc_pub, host, exp}` (in chiaro), chiave = `Pairing.sharedKey(priv, pc_pub)`, PUT `/pair/<code>/watch.json` = `{watch_pub, uid, name, check}`, poll `/allowed/<uid>.json` fino a `true` (30 s) → `PairingInfo(uid, host)`; poi la chiave va in `KeyVault`.

Test: R1 stato — server risponde a `/state.json` con SSE `event: put\ndata: {"path":"/","data":{"v":1,"enc":"…"}}` cifrato con la chiave del test → `state.first()` uguale alla fixture; R2 `send` — PUT registrato, poi `/result/<id>.json` = blob → `CmdResult`; R3 timeout — nessun result → `TransportException.Timeout` dopo 20 s (clock virtuale); R4 pairing — sequenza di 3 richieste e chiave uguale a quella calcolata dal test con la coppia «PC» finta; R5 chiave sbagliata → `BlobException` incapsulata in `TransportException.Network`.

Commit: `feat(watch): FirebaseTransport — RTDB REST + SSE behind the Transport interface, anonymous auth, pairing over /pair/<code>`.

### Task 12: FCM e sveglia

**Files:** `push/CmMessagingService.kt` (`FirebaseMessagingService`: `onMessageReceived(data{kind, session})` → `repo.refresh()` con `goAsync`-like `runBlocking(timeout 8 s)` → `Notifier.show(...)` → `TileService.getUpdater(ctx).requestUpdate(CmTileService::class.java)` + `ComplicationDataSourceUpdateRequester` per i tre tipi; `onNewToken` → `subscribeToTopic(prefs.fcmTopic)`); manifest: service con `com.google.firebase.MESSAGING_EVENT`; `CmApp` (`:wear`): sceglie `FirebaseTransport` se `paired && keyVault.load != null && BuildConfig.FIREBASE` (= `google-services.json` presente, `buildConfigField` in `wear/build.gradle.kts`, `buildFeatures { buildConfig = true }`), altrimenti `FakeTransport`; la scelta è una funzione pura `TransportChoice.pick(paired, hasKey, firebase)` in `:core`.

Test: `push/WakeTest.kt` sulla funzione pura `Wake.plan(prev: State?, cur: State, followed: String?): List<WakeAction>` (`Notify(kind, session)`, `RefreshTile`, `RefreshComplications`): da idle a question → `Notify(QUESTION)`; outcome nuovo sulla seguita → `Notify(OUTCOME)`; sessione sparita → `Notify(GONE)`; quota ≥ 95 → `Notify(QUOTA)`; sempre `RefreshTile`, `RefreshComplications`.

Commit: `feat(watch): FCM wake — one GET, Room, notifications, tile and complication refresh`.

### Task 13: notifiche e testi

**Files:** `push/Notifier.kt`, `push/NotificationTexts.kt`, `push/ReplyReceiver.kt` (BroadcastReceiver per le azioni «1», «2» e `RemoteInput` «Rispondi» → `repo.answer`/`repo.prompt`), `res/values/strings.xml`.

Regole (design, sezione 2): canale `questions` (importanza alta, vibrazione doppia), `outcomes` (default, 40 ms), `gone` (default, 200 ms), `quota` (default). Domanda: titolo «❓ nome», testo intero (`BigTextStyle`), azioni = prime due opzioni + **Apri** (deep link `cmwatch://question/<name>`) + `RemoteInput` «Rispondi»; esito seguito: «✓ nome» + `short`; sparita: «✗ nome»; quota: «⚠ 95 % personale, reset 13:10». Una notifica per sessione (`notificationId = name.hashCode()`), sostituita. Niente notifica con app in primo piano (`ProcessLifecycleOwner`). Test: `NotificationTextsTest` — titolo/corpo/azioni per ogni tipo dalle fixture (`question` → `["1 · yes", "2 · no", "Apri"]`; `quota` con `resetW7` → «reset 13:10» in ora locale del test con `ZoneId` fissa).

Commit: `feat(watch): local notifications from FCM — question with the first two options and dictated reply, outcome, gone, quota threshold; one per session`.

### Task 14: complication (tre tipi)

**Files:** `complication/CmComplicationService.kt` (`SuspendingComplicationDataSourceService`), `complication/ComplicationTexts.kt`, manifest (`android.support.wearable.complications.SUPPORTED_TYPES` = `SHORT_TEXT,RANGED_VALUE,LONG_TEXT`, `UPDATE_PERIOD_SECONDS` = 0: aggiornamento solo su richiesta), `res/drawable/ic_complication.xml`.

`ComplicationTexts.short(state): String` («1?» se domande, altrimenti «▶3» se busy, altrimenti «✓»; «PC» con stale), `long(state)` («❓ ledger-api · Deploy now?» oppure «▶ 2 · ✓ 3»), `ranged(state, account): Triple<Float, Float, String>` (h5 %, 100, «36 %»); tap: `PendingIntent` su `cmwatch://question/<name>` se domanda, altrimenti `cmwatch://sessions`; ranged → `cmwatch://quota`. `getPreviewData` dalla fixture 1. Test: `ComplicationTextsTest` sulle tre fixture.

Commit: `feat(complication): short text, ranged quota ring, long text — one source, refresh on every FCM`.

### Task 15: tile ProtoLayout M3

**Files:** `tile/CmTileService.kt` (`TileService`, `onTileRequest` → `materialScope(ctx, deviceParameters) { primaryLayout(titleSlot = text(header), mainSlot = …, bottomSlot = buttonGroup(...)) }`), `tile/TileTexts.kt`, manifest (`androidx.wear.tiles.action.BIND_TILE_PROVIDER`, preview `@drawable/tile_preview`), `res/values/strings.xml`.

`TileTexts.header(state)` («5 sessioni · 1? · 1✗»), `TileTexts.line(state)` = la sessione ferma con la domanda su una riga intera, altrimenti la seguita o la più recente («▶ atlas-shop · 1 m»); stale → «PC fermo da N min». Bottoni: con domanda **Apri** (`LaunchAction` su `cmwatch://question/<name>`) · **Sessioni**; senza **Sessioni** · **Quota**. `freshnessIntervalMillis` = 30 s con domande, 15 min senza. Test: `TileTextsTest` sulle tre fixture.

Commit: `feat(tile): counts, the stuck session on one whole line, Apri / Sessioni or Sessioni / Quota, adaptive freshness`.

**«Fatto» della fase 3** (design): domanda vista sul quadrante senza aprire l'app — richiede Firebase reale, relay attivo e il Pixel Watch: si chiede a Franz e ci si ferma. `docs/verifiche/fase-3.md`.

---

## Fase 4 — Esito con TTS, Terminale, Timeline, Lancia, Segui, Quota, Recap, Notte, Impostazioni

### Task 16: Esito e TTS
**Files:** `tts/Speaker.kt` (`TextToSpeech` di sistema, `Locale.ITALIAN` se disponibile altrimenti default; `speak(text)`, `stop()`, `release()`; stato `speaking: StateFlow<Boolean>`), `ui/components/SpeakButton.kt` (`IconButton` ▶/■ da 48 dp accanto al testo), `ui/screens/OutcomeScreen.kt` (`short` a 20 sp, `full` a 15 sp, ▶, bottone **Leggi tutto** → `repo.command(SCREEN, name, null)` e mostra il testo del `/result`), `ui/SpeakRules.kt`. Test: `SpeakRulesTest` — `SpeakRules.showButton(text, kind, minChars)`: vero sopra `minChars` o per `OUTCOME|ANSWER|QUESTION`, falso altrimenti. Commit: `feat(watch): outcome screen and system TTS with the ▶ key next to every long or outcome text`.

### Task 17: Terminale
**Files:** `ui/screens/TerminalScreen.kt` (`repo.command(SCREEN)`; 30 righe mono 14 sp, una riga per tool, `horizontalScroll` per non spezzare; **Aggiorna**). Test: `TerminalTextTest` — `TerminalText.lines(result.text)` ≤ 30, righe intere. Commit: `feat(watch): terminal tail — 30 mono lines, one per tool`.

### Task 18: Timeline
**Files:** `ui/screens/TimelineScreen.kt` (eventi da `repo.events` raggruppati per giorno con `ListHeader`, filtro sessione con un `FilledTonalButton` a scelta ciclica), `ui/TimelineText.kt`. Test: `TimelineTextTest` — `TimelineText.groups(events, zone)` → giorni in ordine decrescente, titolo «12 set»; riga «12:15 · ❓ ledger-api · Deploy now?». Commit: `feat(watch): timeline of /events by day with a session filter, 30 days in Room`.

### Task 19: Lancia
**Files:** `ui/screens/LaunchScreen.kt` (`projects` con `AccountDot`, tap → **Lancia** primary con conferma sulla stessa schermata → `repo.command(LAUNCH, null, path)`; mai percorsi liberi). Test: `LaunchRulesTest` — `LaunchRules.allowed(path, projects)` vero solo se `path` è in `projects`. Commit: `feat(watch): launch a published project from the wrist`.

### Task 20: Segui con `OngoingActivity`
**Files:** `follow/FollowOngoing.kt` (`OngoingActivity.Builder` su una notifica silenziosa del canale `follow`, `Status` «▶ ledger-api 4 m» con `TimerPart`, `touchIntent` → `cmwatch://session/<name>`; si mostra quando `state.sessions.any { followed && state == BUSY }`, si toglie altrimenti), chiamato da `CmMessagingService` e da `Repo` (osservatore in `CmApp`). Test: `FollowRulesTest` — `FollowRules.ongoing(state): Session?`. Commit: `feat(watch): follow — ongoing activity on the watch face while the followed session works`.

### Task 21: Quota
**Files:** `ui/screens/QuotaScreen.kt` (due `CircularProgressIndicator` a tutta larghezza uno sotto l'altro: h5 con etichetta «personale · 11 %», poi w7 «settimana 36 % · reset gio 15:20»; `stale` → grigio con «dato vecchio»), `ui/QuotaText.kt`. Test: `QuotaTextTest` (h5 null → «—»; reset in ora locale con `ZoneId` fissa). Commit: `feat(watch): quota rings for both accounts with reset time`.

### Task 22: Recap e Notte
**Files:** `ui/screens/RecapScreen.kt` (voci `project · done`, riga `→ next`), `ui/screens/NightScreen.kt` («In coda: 2 · In corso: —»). Test: `RecapTextTest`. Commit: `feat(watch): day recap and night queue screens`.

### Task 23: Impostazioni complete, menu della lista
**Files:** `SettingsScreen.kt` (già dal Task 9: aggiunge «Timeline», «Lancia», «Quota», «Recap», «Notte» come voci in fondo alla lista Sessioni, non nelle impostazioni), `Nav.kt`. `docs/verifiche/fase-4.md` con le 10 voci dal vivo: esito letto a voce; terminale 30 righe; timeline per giorno; lancio di un progetto; segui con ongoing activity; quota; recap; notte; soglia TTS cambiata e rispettata; vibrazioni per tipo. Commit: `feat(watch): list menu to timeline, launch, quota, recap, night; docs: phase-4 live checklist`.

---

## Fase 5 — rifinitura visiva sul Pixel Watch

### Task 24: larghezze, ambient, movimento, screenshot delle 8 schermate
- Larghezze: ogni riga logica su una riga fisica a 456 px con margini 5,2 % (`ScreenScaffoldDefaults.contentPadding` sostituito da `PaddingValues(horizontal = 24.dp)`), ellissi solo sul nome; verifica con gli screenshot Paparazzi a 456×456 e dal vivo.
- Ambient: `AmbientLifecycleObserver` (`androidx.wear:wear`?? no: `androidx.wear.compose.foundation` non lo ha; usare `androidx.wear:wear` `AmbientLifecycleObserver`) → in ambient la lista mostra solo icona + nome, sfondo nero, niente colori pieni.
- Movimento: `MotionScheme` di M3 con molla damping 0,8; respiro 3 s (`rememberInfiniteTransition`) dell'icona ▶ della seguita; disattivato con `Settings.Global.ANIMATOR_DURATION_SCALE == 0`.
- Screenshot: `docs/screenshots/fase5-<schermata>.png` per Sessioni, Scheda, Domanda, Esito, Terminale, Timeline, Quota, Impostazioni — dal polso, con Franz; approvazione visiva = «fatto» della fase 5.
- `docs/verifiche/fase-5.md`. Commit: `feat(watch): widths on the round screen, ambient mode, motion; docs: phase-5 screenshots`.

---

## Auto-verifica del piano

- Moduli: le regole pure (`rules/*`, `Wake`, `NotificationTexts`, `TileTexts`, `ComplicationTexts`, `SpeakRules`, …) e i loro test stanno in `:core`; i servizi Android (tile, complication, FCM, notifiche, TTS, aptica, ongoing) e le schermate in `:wear`.
- Copertura del design: sezione 1 (flusso, cifratura, `/state`, `/events`, `/cmd`, `/result`, freschezza, coda) → Task 2-5, 11-12; sezione 2 (complication, tile, notifiche, app: Sessioni, Scheda, Domanda, Esito, Terminale, Timeline, Lancia, Quota, Recap, Notte, Impostazioni, Segui, Ambient) → Task 6-9, 13-24; sezione 3 (colori, tipografia, layout, movimento, aptica) → Task 6, 8, 24; sezione 5 (PC fermo, offline, non consegnato, risposta altrove, due orologi via `by`, chiave persa → nuovo pairing, `launch` solo su `projects`, `allow_all` non per high, pressione lunga) → Task 5, 7, 8, 9, 11, 19; sezione 6 (build, test, Actions, adb, fasi, checklist) → Task 1, 10, 15, 23, 24. `mode`, telefono companion, Play Store: fuori scope, come nel design.
- Tipi coerenti: `Snapshot(state, freshness, pending)`, `Repo.answer/prompt/command/retry/flushQueue/refresh/start`, `Transport.state/events/fetchState/send/pair`, `Screen.*`, `ViewState.reduce(snap, paired, chosen, seen)`, `Freshness.of(ts, now)`, `Durations.since(from, now)`, `Blob.seal/open`, `Pairing.newKeyPair/publicB64/sharedKey/checkCode` usati con gli stessi nomi in tutti i task.
- Assunzioni dichiarate: `awaiting` ordinato con `idle`; HKDF con salt vuoto e chiave pubblica X25519 grezza; entrambe da confermare con la sessione `claude-master` (Task 10, Step 5).
