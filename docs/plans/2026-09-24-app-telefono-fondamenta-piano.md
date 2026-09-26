# App del telefono e accoppiamento senza build: piano d'implementazione (fondamenta)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** su un progetto Firebase nuovo, telefono e orologio si accoppiano con il QR di `relay pair`, senza Gradle e senza console Firebase; un push sveglia l'orologio e un comando dall'orologio va a buon fine.

**Architecture:** l'orologio avvia Firebase a runtime da una configurazione salvata (dal telefono) o, in mancanza, da quella dentro la build. Il telefono legge il QR, fa l'accesso anonimo, chiede all'orologio uid e chiave temporanea sul canale di Wear OS, scrive la risposta in `/pair/<id>` con i due uid, riceve la conferma del PC e passa K all'orologio cifrata per lui. La logica pura (QR, configurazione, cifratura del passaggio, risposta al PC, lato orologio) sta in `core` ed è testata sulla JVM; il controller del telefono è testato con dispositivi finti; il resto è colla Android sottile.

**Tech Stack:** Kotlin 2.3.21, AGP 8.13.2, JDK 17, Compose (BOM 2026.06.01, Material 3 1.4.0 sul telefono, Wear Compose Material 3 sull'orologio), kotlinx.serialization, OkHttp + MockWebServer, Firebase BOM 34.19.0 (Auth, Messaging), `play-services-wearable` 20.0.1, `play-services-code-scanner` 16.1.0, `androidx.wear:wear-remote-interactions` 1.2.0, `kotlinx-coroutines-play-services` 1.10.2, Paparazzi 2.0.0-alpha02.

**Spec:** `docs/plans/2026-09-24-app-telefono-fondamenta-design.md` (approvata da Franz il 24/09).

## Global Constraints

- minSdk 33, compileSdk e targetSdk 36, JDK 17, per `core`, `wear`, `mobile`, `ui-tokens`.
- applicationId `it.pixelbox.cmwatch` sia per `wear` sia per `mobile`; le due app si firmano con la stessa chiave (debug: il keystore di debug di questa macchina; release: la chiave del watchface, variabili `KEYSTORE_*`).
- Contratto 1.15, `v` resta 1. Le fixture `contract/pair-qr.json` e `contract/pair-response.json` sono identiche byte per byte in claude-master (`tests/fixtures/relay/`).
- Testi visibili in `res/values/strings.xml` (italiano) e `res/values-en/strings.xml`; mai cablati nel Kotlin; mai «…» nel corpo dei testi; una riga logica su una riga; un solo bottone pieno per schermata.
- Tema solo scuro, colori da `it.pixelbox.cmwatch.ui.tokens.CmColors`, niente colori dinamici.
- Movimento: 200-350 ms, `CubicBezierEasing(0.3f, 0f, 0.2f, 1f)`, niente animazioni infinite tranne il respiro di ciò che lavora; con «riduci animazioni» lo stato finale subito.
- **Mai `claude-master relay pair` con la configurazione principale mentre un dispositivo di prova risponde**: un accoppiamento nuovo riscrive `/allowed` e scollega l'orologio di Franz. Le prove d'accoppiamento usano `CLAUDE_MASTER_CONFIG=<configurazione di prova>`.
- Sul polso di Franz non si installa nessuna build di questo piano senza il suo ok. Mai un APK di GitHub Actions sull'orologio.
- Build sulla VM da 6 GB: `GW="./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process"`; prima di ogni build `ps -eo pid,comm,args | awk '$2=="java"'` e `kill` per PID dei JVM rimasti. `adb` è sempre `/usr/bin/adb`.
- Paparazzi gira solo in GitHub Actions: push solo su `origin` (mai `archivio`), ogni push con l'ok di Franz (cosa esce e dove); `gh workflow run` solo per `record=true`.
- Commit in inglese (`feat(watch): …`, `feat(phone): …`, `feat(core): …`, `test: …`, `docs: …`); `git add` solo di file per nome; mai `google-services.json`, keystore, chiavi o token.
- claude-master non si modifica da qui: una richiesta per volta, mai durante una sua release.
- Si lavora sul ramo `feature/fondamenta` (da `master`); lo spike su `spike/fondamenta`, mai pushato, cancellato alla fine.

## Review Focus

1. **Un QR che non è nostro** (un link, un testo tagliato, una versione futura): «codice non valido», nessuna chiamata di rete, niente salvato. Test: `PairQrTest.rejectsWhatIsNotOurs` (Task 3), `PairingControllerTest.notOurQrStopsBeforeTheNetwork` (Task 13).
2. **Un QR scaduto o già usato:** errore subito, non dopo 30 s d'attesa. Test: `PhonePairerTest.expiredCodeFailsWithoutNetwork` e `usedOrForeignCodeIsUnknownRightAway` (Task 6), `PairingControllerTest.expiredQr` (Task 13).
3. **L'orologio si scollega fra la conferma del PC e la consegna di K:** accoppiamento fatto, orologio «in attesa», K consegnata al ricollegamento, mai persa. Test: `PairingControllerTest.watchGoneAfterThePcKeepsTheKeyForLater` (Task 13).
4. **Orologio con i dati cancellati** (uid anonimo nuovo, non in `/allowed`): nessuna K consegnata, «rifai l'accoppiamento». Test: `PairingControllerTest.aWatchWithANewUidIsNotGivenTheKey` (Task 13).
5. **La release di Franz aggiornata sopra** (configurazione dentro la build): resta accoppiata e accetta ancora il codice a 6 cifre. Test: `FirebaseBootTest.storedWinsOverBundled` (Task 7), prova in emulatore (Task 9), checklist (Task 18).

---

### Task 1: Spike A e B, Firebase a runtime sull'emulatore Wear

Codice da buttare, sul ramo `spike/fondamenta`. Non serve Franz. Esito: le due domande dello spike con una risposta scritta in fondo a questo piano.

**Files (tutti temporanei):**
- Modify: `wear/src/main/AndroidManifest.xml`
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt`
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt`
- Sposta fuori dal repo, poi rimetti: `wear/google-services.json`

- [ ] **Step 1: Ramo, emulatore, memoria**

```bash
git switch -c spike/fondamenta
/usr/bin/adb devices            # atteso: emulator-5554  device
free -m                         # almeno 2000 MB liberi, altrimenti chiudi quello che pesa e riprova
SCR=/tmp/claude-1000/-home-franz-Desktop-workspaces-personali-claude-master-watch/38a17d8c-f025-47b5-9fd2-0f177f725141/scratchpad
mv wear/google-services.json $SCR/spike-gs.json
```

Se `adb devices` non mostra l'emulatore, avvialo (`~/android-sdk/emulator/emulator -list-avds`, poi `-avd <nome> -no-window &`) e riprova.

- [ ] **Step 2: Avvio automatico di Firebase spento**

In `wear/src/main/AndroidManifest.xml` aggiungi `xmlns:tools="http://schemas.android.com/tools"` al tag `<manifest>` e, dentro `<application>`:

```xml
        <provider
            android:name="com.google.firebase.provider.FirebaseInitProvider"
            android:authorities="${applicationId}.firebaseinitprovider"
            tools:node="remove" />
```

- [ ] **Step 3: Configurazione iniettata via adb, salvata, processo chiuso**

In `MainActivity.onCreate`, come prima istruzione dopo `super.onCreate(savedInstanceState)`:

```kotlin
        intent?.getStringExtra("spike_fb64")?.let { b64 ->
            val json = String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT))
            getSharedPreferences("spike", MODE_PRIVATE).edit().putString("fb", json).commit()
            android.util.Log.i("cmwatch", "spike: config salvata, chiudo il processo")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
```

In `CmApp.onCreate`, subito dopo `super.onCreate()`:

```kotlin
        getSharedPreferences("spike", MODE_PRIVATE).getString("fb", null)?.let { j ->
            val o = org.json.JSONObject(j)
            val opts = com.google.firebase.FirebaseOptions.Builder()
                .setApiKey(o.getString("k")).setApplicationId(o.getString("a")).setProjectId(o.getString("p"))
                .setDatabaseUrl(o.getString("d")).setGcmSenderId(o.getString("a").split(":")[1]).build()
            com.google.firebase.FirebaseApp.initializeApp(this, opts)
            android.util.Log.i("cmwatch", "spike: firebase ${com.google.firebase.FirebaseApp.getInstance().options.databaseUrl}")
            scope.launch {
                val t = FirebaseAuthToken.token()
                android.util.Log.i("cmwatch", "spike: token ${t != null} uid ${FirebaseAuthToken.uid()}")
                FirebaseMessaging.getInstance().subscribeToTopic(o.getString("t"))
                    .addOnCompleteListener { r -> android.util.Log.i("cmwatch", "spike: topic ${r.isSuccessful}") }
            }
        }
```

`CmMessagingService.onMessageReceived` scrive già `fcm message: …` nel log.

- [ ] **Step 4: Build senza `google-services.json` e installazione sull'emulatore**

```bash
ps -eo pid,comm,args | awk '$2=="java"'     # kill per PID dei rimasti
$GW :wear:assembleDebug
/usr/bin/adb -s emulator-5554 install -r wear/build/outputs/apk/debug/wear-debug.apk \
  || { /usr/bin/adb -s emulator-5554 uninstall it.pixelbox.cmwatch; /usr/bin/adb -s emulator-5554 install wear/build/outputs/apk/debug/wear-debug.apk; }
```

Atteso: `Success`. Il plugin google-services non si applica perché il file non c'è.

- [ ] **Step 5: Spike A: configurazione del progetto di Franz a runtime**

```bash
B64=$(python3 - "$SCR/spike-gs.json" <<'EOF'
import base64, json, sys
g = json.load(open(sys.argv[1]))
c = [c for c in g["client"] if c["client_info"]["android_client_info"]["package_name"] == "it.pixelbox.cmwatch"][0]
cfg = {"k": c["api_key"][0]["current_key"], "p": g["project_info"]["project_id"],
       "a": c["client_info"]["mobilesdk_app_id"], "d": g["project_info"]["firebase_url"], "t": "watch"}
print(base64.b64encode(json.dumps(cfg, separators=(",", ":")).encode()).decode())
EOF
)
/usr/bin/adb -s emulator-5554 logcat -c
/usr/bin/adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/.wear.MainActivity --es spike_fb64 "$B64"
sleep 3
/usr/bin/adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/.wear.MainActivity
sleep 15
/usr/bin/adb -s emulator-5554 logcat -d -s cmwatch | grep spike
```

Atteso: `spike: firebase https://…`, `spike: token true uid <28 caratteri>`, `spike: topic true`.

- [ ] **Step 6: Spike A: un push arriva**

Un messaggio sul topic, mandato con le funzioni del relay (arriva anche all'orologio di Franz: è una sveglia normale, al massimo una GET di `/state`):

```bash
python3 - <<'EOF'
import importlib.util, sys
p = "~/…/claude-master/claude-master/scripts/cm-relay.py"
s = importlib.util.spec_from_file_location("cmrelay", p); m = importlib.util.module_from_spec(s)
sys.argv = ["cm-relay"]; s.loader.exec_module(m)
print(m.fcm_send({"kind": "spike", "session": "spike"}))
EOF
sleep 20
/usr/bin/adb -s emulator-5554 logcat -d -s cmwatch | grep "fcm message"
```

Atteso: `True`, poi `fcm message: {kind=spike, session=spike}`. La GET di `/state` che segue fallisce con HTTP 401 (l'emulatore non è in `/allowed`): è previsto.

- [ ] **Step 7: Spike B: configurazione diversa, riavvio del processo**

```bash
B64B=$(echo "$B64" | base64 -d | python3 -c 'import sys,json,base64; c=json.load(sys.stdin); c["d"]=c["d"].rstrip("/")+"/"; print(base64.b64encode(json.dumps(c,separators=(",",":")).encode()).decode())')
/usr/bin/adb -s emulator-5554 logcat -c
/usr/bin/adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/.wear.MainActivity --es spike_fb64 "$B64B"
sleep 3
/usr/bin/adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/.wear.MainActivity
sleep 10
/usr/bin/adb -s emulator-5554 logcat -d -s cmwatch | grep "spike: firebase"
```

Atteso: `spike: firebase` con l'URL che finisce in `/`. Il processo nuovo si avvia con la configurazione nuova: il riavvio basta, non serve riavviare Firebase dentro il processo.

- [ ] **Step 8: Rimetti il file, annota, lascia il ramo**

```bash
mv $SCR/spike-gs.json wear/google-services.json && ls -l wear/google-services.json
git checkout -- wear/src/main/AndroidManifest.xml wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt
git status --short     # atteso: nessuna modifica rimasta in wear/
git switch master
```

Le modifiche dello spike non si committano mai: si buttano con `git checkout`, e il ramo `spike/fondamenta` resta vuoto.

In fondo a questo piano aggiungi la sezione `## Risultati dello spike` con: esito di A (accesso anonimo, topic, messaggio FCM ricevuto: sì o no, con le righe di log), esito di B, e la decisione. Se A fallisce, **fermati**: la configurazione a runtime della specifica non regge, torna da Franz prima di qualunque altro task.

```bash
git add docs/plans/2026-09-24-app-telefono-fondamenta-piano.md
git commit -m "docs: phase-1 spike results, Firebase configured at runtime on Wear"
```

---

### Task 2: Spike C, canale telefono-orologio sui dispositivi di Franz

Serve Franz: debug wireless acceso su telefono e orologio. Il pacchetto dello spike è `it.pixelbox.cmwatch.spike`, un'app diversa: la release sul polso non si tocca. Coppia di emulatori scartata: sul telefono emulato il Wear OS app vuole Play con un account Google, e la VM ha 6 GB.

**Files (tutti temporanei, sul ramo `spike/fondamenta`):**
- Modify: `wear/build.gradle.kts`, `mobile/build.gradle.kts` (suffisso `.spike` sul debug, `play-services-wearable`)
- Create: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/SpikeService.kt`
- Create: `wear/src/main/res/values/wear.xml`
- Modify: `wear/src/main/AndroidManifest.xml`, `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`

- [ ] **Step 1: Chiedi a Franz**

Una riga: «Per lo spike C mi servono telefono e orologio con il debug wireless acceso: installo un'app di prova (`it.pixelbox.cmwatch.spike`) su tutti e due, la tua release resta com'è, poi la tolgo.» Aspetta il suo ok e gli indirizzi. Collega come da memoria `watch-install-release-signing` (server adb 36 sotto qemu, `adb pair` per il telefono se serve).

- [ ] **Step 2: Suffisso e dipendenza**

`git switch spike/fondamenta`. In `wear/build.gradle.kts` e in `mobile/build.gradle.kts`, dentro `android { buildTypes { … } }`:

```kotlin
        debug { applicationIdSuffix = ".spike" }
```

In tutti e due i `dependencies { … }`:

```kotlin
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
```

- [ ] **Step 3: Orologio che risponde**

`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/SpikeService.kt`:

```kotlin
package it.pixelbox.cmwatch.wear

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.WearableListenerService

class SpikeService : WearableListenerService() {
    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray> {
        android.util.Log.i("cmwatch", "spike: request $path from $nodeId")
        return Tasks.forResult("pong:${String(request)}".toByteArray())
    }
}
```

In `wear/src/main/AndroidManifest.xml`, dentro `<application>`:

```xml
        <service android:name=".wear.SpikeService" android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.REQUEST_RECEIVED" />
                <data android:scheme="wear" android:host="*" android:pathPrefix="/spike" />
            </intent-filter>
        </service>
```

`wear/src/main/res/values/wear.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@array/android_wear_capabilities">
    <string-array name="android_wear_capabilities" translatable="false">
        <item>cmwatch_wear</item>
    </string-array>
</resources>
```

- [ ] **Step 4: Telefono che chiede**

In `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`, in fondo a `onCreate`:

```kotlin
        Thread {
            runCatching {
                val t0 = System.currentTimeMillis()
                val nodes = com.google.android.gms.tasks.Tasks.await(
                    com.google.android.gms.wearable.Wearable.getCapabilityClient(this)
                        .getCapability("cmwatch_wear", com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)).nodes
                android.util.Log.i("cmwatch", "spike: nodes ${nodes.map { it.displayName }}")
                val r = com.google.android.gms.tasks.Tasks.await(
                    com.google.android.gms.wearable.Wearable.getMessageClient(this).sendRequest(nodes.first().id, "/spike/ping", "hi".toByteArray()))
                android.util.Log.i("cmwatch", "spike: reply ${String(r)} in ${System.currentTimeMillis() - t0} ms")
            }.onFailure { android.util.Log.w("cmwatch", "spike: failed", it) }
        }.start()
```

- [ ] **Step 5: Build, installazione, prova**

```bash
$GW :wear:assembleDebug :mobile:assembleDebug
timeout 900 /usr/bin/adb -s <OROLOGIO> install -r wear/build/outputs/apk/debug/wear-debug.apk
timeout 900 /usr/bin/adb -s <TELEFONO> install -r mobile/build/outputs/apk/debug/mobile-debug.apk
/usr/bin/adb -s <TELEFONO> logcat -c
/usr/bin/adb -s <TELEFONO> shell am start -n it.pixelbox.cmwatch.spike/it.pixelbox.cmwatch.mobile.MainActivity
sleep 10
/usr/bin/adb -s <TELEFONO> logcat -d -s cmwatch | grep spike
/usr/bin/adb -s <OROLOGIO> logcat -d -s cmwatch | grep spike
```

Atteso: sul telefono `spike: nodes [<nome dell'orologio>]` e `spike: reply pong:hi in <ms> ms`; sull'orologio `spike: request /spike/ping`. L'orologio risponde anche con l'app spenta: il servizio si avvia da sé.

- [ ] **Step 6: Pulizia e annotazione**

```bash
/usr/bin/adb -s <OROLOGIO> uninstall it.pixelbox.cmwatch.spike
/usr/bin/adb -s <TELEFONO> uninstall it.pixelbox.cmwatch.spike
git checkout -- wear/build.gradle.kts mobile/build.gradle.kts wear/src/main/AndroidManifest.xml mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt
rm wear/src/main/kotlin/it/pixelbox/cmwatch/wear/SpikeService.kt wear/src/main/res/values/wear.xml
git status --short     # atteso: niente dello spike
git switch master && git branch -D spike/fondamenta
```

Aggiungi l'esito di C a `## Risultati dello spike` (nodi trovati, risposta, tempo). Se C fallisce, **fermati** e torna da Franz: il flusso della specifica si regge su questo canale.

```bash
git add docs/plans/2026-09-24-app-telefono-fondamenta-piano.md
git commit -m "docs: phase-2 spike results, phone-to-watch request channel"
git switch -c feature/fondamenta
```

---

### Task 3: Contratto 1.15, configurazione Firebase e contenuto del QR

**Files:**
- Create: `contract/pair-qr.json`, `contract/pair-response.json`
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfig.kt`
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairQr.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfigTest.kt`, `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairQrTest.kt`

**Interfaces:**
- Produces: `FirebaseConfig(apiKey, projectId, appId, databaseUrl, topic = "watch")` con `senderId`, `sameProject(o)`, `toJson()`, `compact(): QrFirebase`, `FirebaseConfig.fromJson(String?)`, `FirebaseConfig.fromResources(Map<String, String?>)`, `FirebaseConfig.RESOURCE_NAMES`; `QrFirebase(k, p, a, d, t)` con `config()`; `PairQr(v, i, c, h, e, f: QrFirebase)` con `expired(nowSec)` e `PairQr.parse(text): PairQr?`.

I vettori sono quelli già condivisi con il relay (`PairingTest.vectorsFromCmRelayCryptoPy`): chiave privata del PC = scalare 0..31 (`c` del QR), del telefono = 32..63. K = `dd9f775d…8b5c`; i due controlli sono calcolati con `hmac`/`hashlib` di Python.

- [ ] **Step 1: Le due fixture**

`contract/pair-qr.json`:

```json
{
  "v": 1,
  "i": "q3Vb2mXz0rT8yKp1LwN4sA",
  "c": "j0DFrbaPJWJK5bIU6nZ6bslNgp09e14a0bpvPiE4KF8=",
  "h": "penguin",
  "e": 1789211100,
  "f": {
    "k": "AIzaSyD-example-key-000000000000000000",
    "p": "cmwatch-demo",
    "a": "1:123456789012:android:0a1b2c3d4e5f6a7b8c9d0e",
    "d": "https://cmwatch-demo-default-rtdb.europe-west1.firebasedatabase.app",
    "t": "watch"
  }
}
```

`contract/pair-response.json`:

```json
{
  "watch": {
    "watch_pub": "NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ=",
    "uid": "phoneUid00000000000000000000",
    "name": "Pixel 9",
    "check": "f47e1dedf0678005",
    "uids": ["phoneUid00000000000000000000", "watchUid0000000000000000000"],
    "names": {"phoneUid00000000000000000000": "Pixel 9", "watchUid0000000000000000000": "Pixel Watch 5"}
  },
  "ok": {"host": "penguin", "check": "45b2b3a0ed376e3d"}
}
```

- [ ] **Step 2: Test che falliscono**

`core/src/test/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfigTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class FirebaseConfigTest {
    private val cfg = FirebaseConfig("k", "p", "1:42:android:ab", "https://p.firebaseio.com")

    @Test fun roundTripsAndDefaultsTheTopic() {
        assertEquals(cfg, FirebaseConfig.fromJson(cfg.toJson()))
        assertEquals("watch", cfg.topic)
        assertNull(FirebaseConfig.fromJson("{"))
        assertNull(FirebaseConfig.fromJson(null))
    }

    @Test fun senderIdComesFromTheAppId() {
        assertEquals("42", cfg.senderId)
        assertEquals("", cfg.copy(appId = "x").senderId)
    }

    @Test fun sameProjectIgnoresTheTopic() {
        assertTrue(cfg.sameProject(cfg.copy(topic = "other")))
        assertFalse(cfg.sameProject(cfg.copy(databaseUrl = "https://q.firebaseio.com")))
    }

    @Test fun fromGoogleServicesResources() {
        val v = mapOf("google_api_key" to "k", "project_id" to "p", "google_app_id" to "1:42:android:ab", "firebase_database_url" to "https://p.firebaseio.com")
        assertEquals(cfg, FirebaseConfig.fromResources(v))
        assertNull(FirebaseConfig.fromResources(v - "firebase_database_url"))
        assertNull(FirebaseConfig.fromResources(v + ("project_id" to "")))
    }

    @Test fun compactAndBack() = assertEquals(cfg, cfg.compact().config())
}
```

`core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairQrTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PairQrTest {
    private val text = Fixtures.read("pair-qr.json")

    @Test fun readsTheFixture() {
        val q = PairQr.parse(text)!!
        assertEquals("q3Vb2mXz0rT8yKp1LwN4sA", q.i)
        assertEquals("penguin", q.h)
        assertEquals(1789211100L, q.e)
        val cfg = q.f.config()
        assertEquals("cmwatch-demo", cfg.projectId)
        assertEquals("123456789012", cfg.senderId)
        assertEquals("watch", cfg.topic)
    }

    /** `relay pair --text` stampa lo stesso JSON su una riga. */
    @Test fun oneLineFormAsPrinted() = assertNotNull(PairQr.parse(Json.parseToJsonElement(text).toString()))

    @Test fun rejectsWhatIsNotOurs() {
        assertNull(PairQr.parse("https://example.com"))
        assertNull(PairQr.parse(""))
        assertNull(PairQr.parse(text.replace("\"v\": 1", "\"v\": 2")))
        assertNull(PairQr.parse(text.replace("q3Vb2mXz0rT8yKp1LwN4sA", "123456")))
        assertNull(PairQr.parse(text.replace("https://", "http://")))
        assertNull(PairQr.parse(text.replace("j0DFrbaPJWJK5bIU6nZ6bslNgp09e14a0bpvPiE4KF8=", "abc=")))
        assertNull(PairQr.parse(text.dropLast(20)))
    }

    @Test fun expiry() {
        val q = PairQr.parse(text)!!
        assertFalse(q.expired(1789211100L))
        assertTrue(q.expired(1789211101L))
    }
}
```

- [ ] **Step 3: Verifica che falliscano**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.*'`
Expected: FAIL, compilazione: `Unresolved reference 'FirebaseConfig'` e `'PairQr'`.

- [ ] **Step 4: Implementazione**

`core/src/main/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfig.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** La configurazione Firebase del progetto dell'utente: arriva dal QR (telefono) o da `hello` (orologio). Nessun segreto. */
@Serializable
data class FirebaseConfig(
    val apiKey: String,
    val projectId: String,
    val appId: String,
    val databaseUrl: String,
    val topic: String = DEFAULT_TOPIC,
) {
    /** Il numero del progetto, cioè il mittente FCM, sta dentro l'id dell'app: `1:<numero>:android:<hash>`. */
    val senderId: String get() = appId.split(':').getOrElse(1) { "" }

    /** Stesso progetto Firebase: il topic da solo non chiede di riavviare Firebase. */
    fun sameProject(o: FirebaseConfig): Boolean =
        apiKey == o.apiKey && projectId == o.projectId && appId == o.appId && databaseUrl == o.databaseUrl

    fun toJson(): String = json.encodeToString(serializer(), this)

    fun compact(): QrFirebase = QrFirebase(k = apiKey, p = projectId, a = appId, d = databaseUrl, t = topic)

    companion object {
        const val DEFAULT_TOPIC = "watch"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(s: String?): FirebaseConfig? = s?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }

        /** I nomi delle risorse che il plugin google-services genera nell'app. */
        val RESOURCE_NAMES = listOf("google_api_key", "project_id", "google_app_id", "firebase_database_url")

        /** Configurazione dai valori di google-services (build con il file dentro); null se ne manca uno. */
        fun fromResources(v: Map<String, String?>): FirebaseConfig? {
            val (k, p, a, d) = RESOURCE_NAMES.map { v[it]?.takeIf(String::isNotBlank) ?: return null }
            return FirebaseConfig(apiKey = k, projectId = p, appId = a, databaseUrl = d)
        }
    }
}

/** La stessa configurazione con le chiavi brevi del QR e di `hello` (design 24/09). */
@Serializable
data class QrFirebase(val k: String, val p: String, val a: String, val d: String, val t: String) {
    fun config(): FirebaseConfig = FirebaseConfig(apiKey = k, projectId = p, appId = a, databaseUrl = d, topic = t)
}
```

`core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairQr.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Il contenuto del QR di `claude-master relay pair` (contratto 1.15, `contract/pair-qr.json`). */
@Serializable
data class PairQr(val v: Int, val i: String, val c: String, val h: String, val e: Long, val f: QrFirebase) {
    fun expired(nowSec: Long): Boolean = nowSec > e

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val ID = Regex("[A-Za-z0-9_-]{22}")

        /** Testo del QR o di «Incolla il codice»; null se non è un codice di claude-master (altro QR, testo tagliato, versione futura). */
        fun parse(text: String): PairQr? {
            val q = runCatching { json.decodeFromString(serializer(), text.trim()) }.getOrNull() ?: return null
            val pubOk = runCatching { Pairing.rawFromB64(q.c).size == 32 }.getOrDefault(false)
            val fbOk = listOf(q.f.k, q.f.p, q.f.a, q.f.t).none(String::isBlank) && q.f.d.startsWith("https://")
            return q.takeIf { it.v == 1 && ID.matches(it.i) && pubOk && fbOk }
        }
    }
}
```

- [ ] **Step 5: Verifica che passino**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.*'`
Expected: PASS (9 test).

- [ ] **Step 6: Commit**

```bash
git add contract/pair-qr.json contract/pair-response.json core/src/main/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfig.kt core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairQr.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/FirebaseConfigTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairQrTest.kt
git commit -m "feat(core): contract 1.15 pairing fixtures, Firebase config and QR payload"
```

---

### Task 4: Richiesta R1 a claude-master

Nessun codice. Non blocca i task seguenti: il lavoro dell'app va avanti sulle fixture.

- [ ] **Step 1: Controlla che claude-master non sia in release**

```bash
claude-master sessions | grep -i claude-master
tmux capture-pane -p -t claude-master -S -30 | grep -i -E "release|rilascio" | tail -3
```

Se è in release, aspetta la fine (`SendMessage` con `notify_when_idle: true`).

- [ ] **Step 2: Manda la richiesta**

Con `SendMessage` (nome da `ListAgents`) o `claude-master talk claude-master "…"`. `<SHA>` è il commit del Task 3.

```
Richiesta da claude-master-watch (una sola; design approvato da Franz il 24/09, docs/plans/2026-09-24-app-telefono-fondamenta-design.md in claude-master-watch, sezione «Richieste al relay», R1): accoppiamento dal telefono con un QR. Contratto 1.15, `v` resta 1.
1) `relay pair` disegna nel terminale un QR (mezzi blocchi, margine di 4 moduli, correzione M) con il JSON su una riga di contract/pair-qr.json: v=1; i = 16 byte casuali in base64url senza padding (22 caratteri); c = pc_pub; h = host; e = exp; f = {k chiave API, p id del progetto, a id dell'app Android, d URL del database, t topic FCM}. Generatore QR in Python incluso nel plugin (per esempio qrcodegen di Project Nayuki, MIT): nessuna dipendenza nuova. Sotto il QR, il codice a 6 cifre come oggi. `--text` stampa solo il JSON.
2) Stesso documento {pc_pub, host, exp} in /pair/<code> e in /pair/<id>, stessa coppia di chiavi; il relay interroga tutti e due; vince la prima risposta valida e l'altro nodo si cancella; scadenza e tentativi in comune. I controlli HMAC usano la stringa del nodo: checkCode(K, id) e checkCode(K, id + ":pc").
3) In /pair/<…>/watch campi opzionali `uids` (lista, al massimo 4) e `names` (uid → nome). /allowed riceve tutti gli uid, devices.json li registra con il nome. Senza `uids`, tutto come oggi.
4) Dati dell'app Firebase: `relay.firebase_app` = {api_key, project_id, app_id}, oppure `relay.google_services` = percorso di un google-services.json (project_info.project_id, project_info.firebase_url, e il client con package_name it.pixelbox.cmwatch: client_info.mobilesdk_app_id, api_key[0].current_key). Senza questi dati `relay pair` non mostra il QR, lo dice, e il codice a 6 cifre funziona; doctor mostra un WARN.
5) Prove isolate: pair, push e serve devono funzionare con CLAUDE_MASTER_CONFIG che punta a una configurazione di prova (relay.dir, service_account, firebase_url suoi), senza toccare chiave, devices.json, /allowed e crontab della configurazione principale. Serve perché le prove dell'app non scolleghino l'orologio di Franz.
6) Fixture: copia byte per byte contract/pair-qr.json e contract/pair-response.json di claude-master-watch (commit <SHA>) in tests/fixtures/relay/. Vettori: chiave privata del PC = scalare 0..31, del telefono = 32..63, come i vettori di pairing già condivisi. Il test Python legge pair-qr.json, verifica che il check di pair-response.json → watch sia valido per l'id della fixture e produce esattamente pair-response.json → ok.
Quando è fatto, scrivimi commit e versione del plugin; la sezione del README del contratto la scrivo io.
```

- [ ] **Step 3: Aspetta la risposta senza chiedere a vuoto**

`SendMessage` con `notify_when_idle: true` verso claude-master, oppure `claude-master wait claude-master`. Il seguito è il Task 16.

---

### Task 5: Cifratura del passaggio e messaggi fra telefono e orologio

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/crypto/Pairing.kt` (`sharedKey` con `info`, `publicFromRaw`)
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/Handoff.kt`
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessages.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/crypto/PairingTest.kt` (due test in più), `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffTest.kt`, `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessagesTest.kt`

**Interfaces:**
- Consumes: `QrFirebase` (Task 3).
- Produces: `Pairing.sharedKey(priv, peerPubB64, info: String = INFO)`, `Pairing.publicFromRaw(raw): PublicKey`; `Handoff.INFO`, `Handoff.seal(key, phoneEph: PrivateKey, watchEphPubB64, watchUid, nonce = random): String`, `Handoff.open(box, watchEph: PrivateKey, phoneEphPubB64, watchUid): ByteArray`; `HandoffException(code)`; `HelloRequest(v = 1, f: QrFirebase)`, `HelloResponse(v, uid?, name?, eph?, restart = false, error?)`, `KeyRequest(v, host, eph, box)`, `KeyResponse(v, ok = false, error?)`; `HandoffMessages.HELLO`, `.KEY`, `.ERR_AUTH`, `.ERR_NO_SESSION`, `.ERR_DECRYPT`, `.ERR_STORE`, `.ERR_BAD_REQUEST`, `.encode(serializer, value): ByteArray`, `.decode(serializer, bytes): T?`.

Vettore fisso, calcolato con `cryptography` di Python: eph del telefono = scalare 64..95, eph dell'orologio = 96..127, nonce = byte 0..11, K = `dd9f…8b5c`, aad = `watchUid0000000000000000000`.

- [ ] **Step 1: Test che falliscono**

In `PairingTest.kt`, dentro la classe:

```kotlin
    @Test fun infoSeparatesTheUses() {
        val a = Pairing.privateFromRaw((0 until 32).map { it.toByte() }.toByteArray())
        val bPub = "NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ="
        assertFalse(Pairing.sharedKey(a, bPub).contentEquals(Pairing.sharedKey(a, bPub, "cmwatch-handoff-v1")))
    }

    @Test fun publicFromRawRoundTrips() {
        val b64 = "NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ="
        assertEquals(b64, Pairing.publicB64(java.security.KeyPair(Pairing.publicFromRaw(Pairing.rawFromB64(b64)), null)))
    }
```

`core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import org.junit.Assert.*
import org.junit.Test

class HandoffTest {
    private fun priv(from: Int) = Pairing.privateFromRaw((from until from + 32).map { it.toByte() }.toByteArray())
    private val key = "dd9f775d5fbdd918e727cb41c05452189759ccc0d87798791eff22474e278b5c".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val phonePub = "eaYx7t4b+cmPEgMs3q3Q56B5OY/HhriMyEbsia+FpRo="
    private val watchPub = "Z13VdO13iTELPS52gfN5C0ZsdzsVIf7PNld5WDcepS8="
    private val uid = "watchUid0000000000000000000"
    private val box = "AAECAwQFBgcICQoLtCFqAvmVEVAwdkhHgr49Nf8yUE0cy5x6KEOrUY36BNwwg8O/d+QUEK3eSLm+02i2"

    @Test fun fixedVectorFromPythonCryptography() {
        assertEquals(box, Handoff.seal(key, priv(64), watchPub, uid, nonce = ByteArray(12) { it.toByte() }))
        assertArrayEquals(key, Handoff.open(box, priv(96), phonePub, uid))
    }

    @Test fun roundTripWithFreshKeys() {
        val p = Pairing.newKeyPair(); val w = Pairing.newKeyPair()
        val b = Handoff.seal(key, p.private, Pairing.publicB64(w), uid)
        assertArrayEquals(key, Handoff.open(b, w.private, Pairing.publicB64(p), uid))
    }

    @Test fun theWatchUidIsBoundIn() {
        val e = assertThrows(HandoffException::class.java) { Handoff.open(box, priv(96), phonePub, "anotherUid") }
        assertEquals(HandoffMessages.ERR_DECRYPT, e.code)
    }

    @Test fun garbageIsADecryptError() {
        val e = assertThrows(HandoffException::class.java) { Handoff.open("AAA", priv(96), phonePub, uid) }
        assertEquals(HandoffMessages.ERR_DECRYPT, e.code)
    }
}
```

`core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessagesTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class HandoffMessagesTest {
    @Test fun helloCarriesTheCompactConfig() {
        val req = HelloRequest(f = QrFirebase("k", "p", "a", "https://d", "watch"))
        val s = String(HandoffMessages.encode(HelloRequest.serializer(), req))
        assertEquals("""{"v":1,"f":{"k":"k","p":"p","a":"a","d":"https://d","t":"watch"}}""", s)
        assertEquals(req, HandoffMessages.decode(HelloRequest.serializer(), s.toByteArray()))
    }

    @Test fun repliesOmitNulls() {
        assertEquals("""{"v":1,"uid":"u","name":"n","eph":"e","restart":false}""",
            String(HandoffMessages.encode(HelloResponse.serializer(), HelloResponse(uid = "u", name = "n", eph = "e"))))
        assertEquals("""{"v":1,"restart":true}""", String(HandoffMessages.encode(HelloResponse.serializer(), HelloResponse(restart = true))))
        assertEquals("""{"v":1,"ok":false,"error":"decrypt"}""", String(HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = "decrypt"))))
    }

    @Test fun garbageDecodesToNull() = assertNull(HandoffMessages.decode(KeyRequest.serializer(), "nope".toByteArray()))
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.*' --tests 'it.pixelbox.cmwatch.crypto.PairingTest'`
Expected: FAIL, compilazione: `Unresolved reference 'Handoff'`, `'publicFromRaw'`, e `sharedKey` con tre argomenti.

- [ ] **Step 3: Implementazione**

In `Pairing.kt` sostituisci `sharedKey` con queste due funzioni (aggiungi `import java.security.PublicKey` se manca):

```kotlin
    /** `info` separa gli usi: il relay (default) e il passaggio telefono → orologio (`Handoff.INFO`). */
    fun sharedKey(priv: PrivateKey, peerPubB64: String, info: String = INFO): ByteArray {
        val ka = KeyAgreement.getInstance("XDH").apply { init(priv); doPhase(publicFromRaw(rawFromB64(peerPubB64)), true) }
        return hkdf(ka.generateSecret(), info.toByteArray(), 32)
    }

    /** Chiave pubblica X25519 da 32 byte grezzi; su Android Conscrypt non sempre accetta lo SPKI, c'è il ripiego. */
    fun publicFromRaw(raw: ByteArray): PublicKey {
        require(raw.size == 32) { "peer public key must be 32 bytes" }
        val kf = KeyFactory.getInstance("XDH")
        return runCatching { kf.generatePublic(X509EncodedKeySpec(SPKI_PREFIX + raw)) }
            .getOrElse { kf.generatePublic(XECPublicKeySpec(NamedParameterSpec.X25519, BigInteger(1, raw.reversedArray()))) }
    }
```

`core/src/main/kotlin/it/pixelbox/cmwatch/pairing/Handoff.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import java.security.PrivateKey
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class HandoffException(val code: String, cause: Throwable? = null) : Exception(code, cause)

/**
 * K dal telefono all'orologio (design 24/09, «Sicurezza»): kw = HKDF-SHA256(X25519(eph del telefono, eph dell'orologio),
 * info "cmwatch-handoff-v1"); box = base64(nonce di 12 byte ‖ AES-256-GCM(kw, K, aad = uid dell'orologio)). Il canale di
 * Wear OS può passare dal cloud Google: così anche lì viaggiano solo dati cifrati.
 */
object Handoff {
    const val INFO = "cmwatch-handoff-v1"
    private const val NONCE = 12
    private val rnd = SecureRandom()

    fun seal(
        key: ByteArray, phoneEph: PrivateKey, watchEphPubB64: String, watchUid: String,
        nonce: ByteArray = ByteArray(NONCE).also { rnd.nextBytes(it) },
    ): String {
        val c = cipher(Cipher.ENCRYPT_MODE, Pairing.sharedKey(phoneEph, watchEphPubB64, INFO), nonce, watchUid)
        return Base64.getEncoder().encodeToString(nonce + c.doFinal(key))
    }

    fun open(box: String, watchEph: PrivateKey, phoneEphPubB64: String, watchUid: String): ByteArray = try {
        val raw = Base64.getDecoder().decode(box)
        val c = cipher(Cipher.DECRYPT_MODE, Pairing.sharedKey(watchEph, phoneEphPubB64, INFO), raw.copyOfRange(0, NONCE), watchUid)
        c.doFinal(raw, NONCE, raw.size - NONCE).also { require(it.size == 32) { "key must be 32 bytes" } }
    } catch (e: Exception) {
        throw HandoffException(HandoffMessages.ERR_DECRYPT, e)
    }

    private fun cipher(mode: Int, kw: ByteArray, nonce: ByteArray, aad: String): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(kw, "AES"), GCMParameterSpec(128, nonce))
            updateAAD(aad.toByteArray())
        }
}
```

`core/src/main/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessages.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Richieste e risposte fra telefono e orologio sul canale di Wear OS (design 24/09, «Messaggi fra telefono e orologio»). */
@Serializable data class HelloRequest(val v: Int = 1, val f: QrFirebase)

@Serializable data class HelloResponse(
    val v: Int = 1, val uid: String? = null, val name: String? = null, val eph: String? = null,
    val restart: Boolean = false, val error: String? = null,
)

@Serializable data class KeyRequest(val v: Int = 1, val host: String, val eph: String, val box: String)

@Serializable data class KeyResponse(val v: Int = 1, val ok: Boolean = false, val error: String? = null)

object HandoffMessages {
    const val HELLO = "/cmwatch/pair/hello"
    const val KEY = "/cmwatch/pair/key"
    const val ERR_AUTH = "auth"
    const val ERR_NO_SESSION = "no_session"
    const val ERR_DECRYPT = "decrypt"
    const val ERR_STORE = "store"
    const val ERR_BAD_REQUEST = "bad_request"

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    fun <T> encode(s: KSerializer<T>, v: T): ByteArray = json.encodeToString(s, v).toByteArray()
    fun <T> decode(s: KSerializer<T>, b: ByteArray): T? = runCatching { json.decodeFromString(s, String(b)) }.getOrNull()
}
```

- [ ] **Step 4: Verifica che passino**

Run: `$GW :core:testDebugUnitTest`
Expected: PASS, compresi i vettori del relay già esistenti (`vectorsFromCmRelayCryptoPy`).

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/crypto/Pairing.kt core/src/main/kotlin/it/pixelbox/cmwatch/pairing/Handoff.kt core/src/main/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessages.kt core/src/test/kotlin/it/pixelbox/cmwatch/crypto/PairingTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/HandoffMessagesTest.kt
git commit -m "feat(core): phone-to-watch key handoff, sealed for the watch, and its messages"
```

---

### Task 6: La risposta del telefono al PC (`PhonePairer`)

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PhonePairer.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PhonePairerTest.kt`

**Interfaces:**
- Consumes: `PairQr` (Task 3), `Pairing.sharedKey`, `Pairing.publicFromRaw` (Task 5), `Rtdb` (esistente).
- Produces: `WatchPeer(uid, name)`, `PhonePairResult(key: ByteArray, host: String, uids: List<String>)`, `PairError` (`Expired`, `Unknown`, `NoConfirm`, `BadConfirm`, `Network(msg)`), `PhonePairer(rtdb, deviceKeyPair, now, pollMs, timeoutMs).pair(qr, phoneUid, phoneName, watch: WatchPeer?): PhonePairResult`, `PhonePairer.response(...)`.

- [ ] **Step 1: Test che falliscono**

`core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PhonePairerTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.transport.Rtdb
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

class PhonePairerTest {
    private val server = MockWebServer()
    private val store = HashMap<String, String>()
    private val qr = PairQr.parse(Fixtures.read("pair-qr.json"))!!
    private val exchange = Json.parseToJsonElement(Fixtures.read("pair-response.json")).jsonObject
    private fun priv(from: Int) = Pairing.privateFromRaw((from until from + 32).map { it.toByte() }.toByteArray())
    private val phoneKeys = KeyPair(Pairing.publicFromRaw(Pairing.rawFromB64("NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ=")), priv(32))
    private val watch = WatchPeer("watchUid0000000000000000000", "Pixel Watch 5")
    private var pcAnswers = true

    @Before fun up() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl!!.encodedPath.removePrefix("/").removeSuffix(".json")
                return when (request.method) {
                    "GET" -> MockResponse().setBody(store[path] ?: "null")
                    "PUT" -> {
                        val body = request.body.readUtf8()
                        store[path] = body
                        // Il relay finto: verifica il check con la chiave privata del PC (scalare 0..31) e conferma.
                        if (pcAnswers && path.endsWith("/watch")) {
                            val w = Json.parseToJsonElement(body).jsonObject
                            val k = Pairing.sharedKey(priv(0), w["watch_pub"]!!.jsonPrimitive.content)
                            if (Pairing.checkCode(k, qr.i) == w["check"]!!.jsonPrimitive.content)
                                store["pair/${qr.i}/ok"] = buildJsonObject { put("host", "penguin"); put("check", Pairing.checkCode(k, "${qr.i}:pc")) }.toString()
                        }
                        MockResponse().setBody(body)
                    }
                    else -> MockResponse().setResponseCode(405)
                }
            }
        }
        server.start()
        store["pair/${qr.i}"] = """{"pc_pub":"${qr.c}","host":"penguin","exp":${qr.e}}"""
    }

    @After fun down() = server.shutdown()

    private fun pairer(now: Long = qr.e - 60, timeout: Long = 2_000) = PhonePairer(
        Rtdb(server.url("/").toString().removeSuffix("/"), token = { "t0k" }), { phoneKeys }, { now }, pollMs = 10, timeoutMs = timeout,
    )

    @Test fun writesExactlyTheContractAnswerAndGetsTheKey() {
        val r = runBlocking { pairer().pair(qr, "phoneUid00000000000000000000", "Pixel 9", watch) }
        assertEquals(exchange["watch"], Json.parseToJsonElement(store["pair/${qr.i}/watch"]!!))
        assertEquals(exchange["ok"], Json.parseToJsonElement(store["pair/${qr.i}/ok"]!!))
        assertEquals("dd9f775d5fbdd918e727cb41c05452189759ccc0d87798791eff22474e278b5c", r.key.joinToString("") { "%02x".format(it) })
        assertEquals(listOf("phoneUid00000000000000000000", "watchUid0000000000000000000"), r.uids)
        assertEquals("penguin", r.host)
    }

    @Test fun phoneAloneHasOneUid() {
        val r = runBlocking { pairer().pair(qr, "phoneUid00000000000000000000", "Pixel 9", null) }
        assertEquals(listOf("phoneUid00000000000000000000"), r.uids)
        assertEquals(1, Json.parseToJsonElement(store["pair/${qr.i}/watch"]!!).jsonObject["uids"]!!.jsonArray.size)
    }

    @Test fun expiredCodeFailsWithoutNetwork() {
        assertThrows(PairError.Expired::class.java) { runBlocking { pairer(now = qr.e + 1).pair(qr, "p", "Pixel 9", watch) } }
        assertEquals(0, server.requestCount)
    }

    @Test fun usedOrForeignCodeIsUnknownRightAway() {
        store.remove("pair/${qr.i}")
        assertThrows(PairError.Unknown::class.java) { runBlocking { pairer().pair(qr, "p", "Pixel 9", watch) } }
        assertNull(store["pair/${qr.i}/watch"])
    }

    @Test fun silentPcIsNoConfirm() {
        pcAnswers = false
        assertThrows(PairError.NoConfirm::class.java) { runBlocking { pairer(timeout = 100).pair(qr, "p", "Pixel 9", watch) } }
    }

    @Test fun forgedConfirmIsRejected() {
        pcAnswers = false
        store["pair/${qr.i}/ok"] = """{"host":"penguin","check":"0000000000000000"}"""
        assertThrows(PairError.BadConfirm::class.java) { runBlocking { pairer().pair(qr, "p", "Pixel 9", watch) } }
    }
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.PhonePairerTest'`
Expected: FAIL, compilazione: `Unresolved reference 'PhonePairer'`.

- [ ] **Step 3: Implementazione**

`core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PhonePairer.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.transport.Rtdb
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.security.KeyPair

data class WatchPeer(val uid: String, val name: String)

class PhonePairResult(val key: ByteArray, val host: String, val uids: List<String>)

sealed class PairError(msg: String) : Exception(msg) {
    class Expired : PairError("code expired")
    /** Il nodo non c'è: QR già usato, scaduto e spazzato, o di un altro relay. */
    class Unknown : PairError("no such pairing")
    class NoConfirm : PairError("the PC did not confirm")
    class BadConfirm : PairError("PC check failed")
    class Network(msg: String) : PairError(msg)
}

/** Passi 4 e 5 del flusso (design 24/09): la risposta in `/pair/<id>/watch` con gli uid di telefono e orologio, poi la conferma del PC. */
class PhonePairer(
    private val rtdb: Rtdb,
    private val deviceKeyPair: () -> KeyPair = { Pairing.newKeyPair() },
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val pollMs: Long = 1000,
    private val timeoutMs: Long = 30_000,
) {
    suspend fun pair(qr: PairQr, phoneUid: String, phoneName: String, watch: WatchPeer?): PhonePairResult {
        if (qr.expired(now())) throw PairError.Expired()
        try {
            rtdb.get("pair/${qr.i}") ?: throw PairError.Unknown()
            val kp = deviceKeyPair()
            val key = Pairing.sharedKey(kp.private, qr.c)
            rtdb.put("pair/${qr.i}/watch", response(Pairing.publicB64(kp), phoneUid, phoneName, Pairing.checkCode(key, qr.i), watch).toString())
            val ok = withTimeoutOrNull(timeoutMs) {
                while (true) {
                    rtdb.get("pair/${qr.i}/ok")?.let { return@withTimeoutOrNull Json.parseToJsonElement(it).jsonObject }
                    delay(pollMs)
                }
                @Suppress("UNREACHABLE_CODE") null
            } ?: throw PairError.NoConfirm()
            if (ok["check"]?.jsonPrimitive?.content != Pairing.checkCode(key, "${qr.i}:pc")) throw PairError.BadConfirm()
            return PhonePairResult(key, ok["host"]?.jsonPrimitive?.content ?: qr.h, listOfNotNull(phoneUid, watch?.uid))
        } catch (e: TransportException) {
            throw PairError.Network(e.message ?: "network")
        }
    }

    companion object {
        /** Il nodo si chiama ancora `watch`, per compatibilità con il relay: lo scrive il telefono per tutti e due. */
        fun response(pub: String, phoneUid: String, phoneName: String, check: String, watch: WatchPeer?): JsonObject = buildJsonObject {
            put("watch_pub", pub)
            put("uid", phoneUid)
            put("name", phoneName)
            put("check", check)
            putJsonArray("uids") { add(phoneUid); watch?.let { add(it.uid) } }
            putJsonObject("names") { put(phoneUid, phoneName); watch?.let { put(it.uid, it.name) } }
        }
    }
}
```

- [ ] **Step 4: Verifica che passino**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PhonePairer.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PhonePairerTest.kt
git commit -m "feat(core): phone answers the PC for itself and the watch, checked against the contract fixture"
```

---

### Task 7: Lato orologio puro, avvio di Firebase, preferenze nuove

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/WatchHandoff.kt`
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/FirebaseBoot.kt`
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairingRecord.kt`
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/settings/Prefs.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/WatchHandoffTest.kt`, `FirebaseBootTest.kt`, `PairingRecordTest.kt`

**Interfaces:**
- Consumes: `FirebaseConfig` (Task 3), `Handoff`, `KeyRequest`, `HandoffException` (Task 5).
- Produces: `WatchHandoff(nowMs, newKeyPair, ttlMs)` con `needsRestart(active, incoming)`, `open(uid): String`, `take(req): ByteArray`; `FirebaseBoot.pick(stored, bundled)`, `FirebaseBoot.active`, `FirebaseBoot.bundled(ctx)`, `FirebaseBoot.start(ctx, stored)`, `FirebaseBoot.retopic(topic)`; `PairingRecord(uids, names, watchUid?, watchName?, watchPending)` con `toJson()`/`fromJson()`; `Settings.firebaseJson`, `Settings.pairingJson`, `Settings.resumeQr`; `interface SettingsStore { current(); update(block) }` implementata da `Prefs`.

- [ ] **Step 1: Test che falliscono**

`WatchHandoffTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import org.junit.Assert.*
import org.junit.Test

class WatchHandoffTest {
    private var now = 0L
    private val cfg = FirebaseConfig("k", "p", "1:1:android:a", "https://a")
    private val key = ByteArray(32) { 7 }
    private fun h() = WatchHandoff(nowMs = { now })
    private fun keyFor(eph: String, uid: String): KeyRequest {
        val phone = Pairing.newKeyPair()
        return KeyRequest(host = "penguin", eph = Pairing.publicB64(phone), box = Handoff.seal(key, phone.private, eph, uid))
    }

    @Test fun restartOnlyForAnotherProject() {
        val w = h()
        assertFalse(w.needsRestart(null, cfg))
        assertFalse(w.needsRestart(cfg, cfg.copy(topic = "t2")))
        assertTrue(w.needsRestart(cfg, cfg.copy(projectId = "other")))
    }

    @Test fun keyOpensWithinFiveMinutesAndOnlyOnce() {
        val w = h()
        val req = keyFor(w.open("wuid"), "wuid")
        now = 5 * 60_000L
        assertArrayEquals(key, w.take(req))
        assertEquals(HandoffMessages.ERR_NO_SESSION, assertThrows(HandoffException::class.java) { w.take(req) }.code)
    }

    @Test fun expiredSessionAsksForHelloAgain() {
        val w = h()
        val req = keyFor(w.open("wuid"), "wuid")
        now = 5 * 60_000L + 1
        assertEquals(HandoffMessages.ERR_NO_SESSION, assertThrows(HandoffException::class.java) { w.take(req) }.code)
    }

    @Test fun noHelloNoKey() {
        val e = assertThrows(HandoffException::class.java) { h().take(KeyRequest(host = "h", eph = "x", box = "y")) }
        assertEquals(HandoffMessages.ERR_NO_SESSION, e.code)
    }
}
```

`FirebaseBootTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class FirebaseBootTest {
    private val a = FirebaseConfig("k", "p", "1:1:android:a", "https://a")
    private val b = a.copy(projectId = "q")

    /** Quella arrivata dal telefono vince su quella dentro la build; senza tutte e due, Firebase resta spento. */
    @Test fun storedWinsOverBundled() {
        assertEquals(a, FirebaseBoot.pick(a, b))
        assertEquals(b, FirebaseBoot.pick(null, b))
        assertNull(FirebaseBoot.pick(null, null))
    }
}
```

`PairingRecordTest.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import org.junit.Assert.*
import org.junit.Test

class PairingRecordTest {
    @Test fun roundTrip() {
        val r = PairingRecord(listOf("p", "w"), mapOf("p" to "Pixel 9", "w" to "Pixel Watch 5"), "w", "Pixel Watch 5", watchPending = true)
        assertEquals(r, PairingRecord.fromJson(r.toJson()))
        assertNull(PairingRecord.fromJson("x"))
        assertNull(PairingRecord.fromJson(null))
    }
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `$GW :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.pairing.*'`
Expected: FAIL, compilazione: `Unresolved reference 'WatchHandoff'`, `'FirebaseBoot'`, `'PairingRecord'`.

- [ ] **Step 3: Implementazione**

`WatchHandoff.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import java.security.KeyPair

/**
 * Il lato orologio del passaggio (design 24/09): `hello` con un altro progetto Firebase chiede il riavvio; altrimenti una
 * chiave temporanea X25519, tenuta in memoria per 5 minuti, che apre la K mandata dal telefono.
 */
class WatchHandoff(
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val newKeyPair: () -> KeyPair = { Pairing.newKeyPair() },
    private val ttlMs: Long = 5 * 60_000L,
) {
    private var eph: KeyPair? = null
    private var ephAt = 0L
    private var uid: String? = null

    /** Firebase si riavvia solo se ne gira già uno su un altro progetto: un topic nuovo non basta. */
    fun needsRestart(active: FirebaseConfig?, incoming: FirebaseConfig): Boolean = active != null && !active.sameProject(incoming)

    /** Una sessione nuova per l'orologio con questo uid: la chiave pubblica temporanea della risposta a `hello`. */
    @Synchronized fun open(uid: String): String {
        val kp = newKeyPair()
        eph = kp; ephAt = nowMs(); this.uid = uid
        return Pairing.publicB64(kp)
    }

    /** K dal `key` del telefono. La sessione si chiude comunque: un secondo tentativo riparte da `hello`. */
    @Synchronized fun take(req: KeyRequest): ByteArray {
        val kp = eph; val u = uid
        eph = null; uid = null
        if (kp == null || u == null || nowMs() - ephAt > ttlMs) throw HandoffException(HandoffMessages.ERR_NO_SESSION)
        return Handoff.open(req.box, kp.private, req.eph, u)
    }
}
```

`FirebaseBoot.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Firebase avviato dall'app, non dalla build (design 24/09, «Orologio: Firebase a runtime»). Una volta per processo: con
 * un progetto diverso l'app si riavvia, perché riavviare Firebase dentro un processo avviato è fragile (spike B).
 */
object FirebaseBoot {
    /** Quella arrivata dal telefono vince su quella dentro la build. */
    fun pick(stored: FirebaseConfig?, bundled: FirebaseConfig?): FirebaseConfig? = stored ?: bundled

    @Volatile var active: FirebaseConfig? = null
        private set

    /** I valori del plugin google-services, letti per nome: esistono solo se la build aveva il file. */
    @SuppressLint("DiscouragedApi")
    fun bundled(ctx: Context): FirebaseConfig? = FirebaseConfig.fromResources(FirebaseConfig.RESOURCE_NAMES.associateWith { n ->
        ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let(ctx::getString)
    })

    /** Avvia Firebase con la configurazione scelta; null se non ce n'è una o se l'avvio fallisce. */
    @Synchronized fun start(ctx: Context, stored: FirebaseConfig?): FirebaseConfig? {
        active?.let { return it }
        val cfg = pick(stored, bundled(ctx)) ?: return null
        val opts = FirebaseOptions.Builder()
            .setApiKey(cfg.apiKey).setApplicationId(cfg.appId).setProjectId(cfg.projectId)
            .setDatabaseUrl(cfg.databaseUrl).setGcmSenderId(cfg.senderId)
            .build()
        return runCatching { FirebaseApp.initializeApp(ctx.applicationContext, opts); cfg }
            .onFailure { android.util.Log.w("cmwatch", "firebase: ${it.message}") }
            .getOrNull()
            ?.also { active = it }
    }

    /** Stesso progetto, topic nuovo: basta aggiornare quello attivo. */
    fun retopic(topic: String) { active = active?.copy(topic = topic) }
}
```

`PairingRecord.kt`:

```kotlin
package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Solo telefono: con chi è accoppiato e se all'orologio manca ancora K (design 24/09, «Casi particolari»). */
@Serializable
data class PairingRecord(
    val uids: List<String>,
    val names: Map<String, String>,
    val watchUid: String? = null,
    val watchName: String? = null,
    val watchPending: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun fromJson(s: String?): PairingRecord? = s?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
```

In `Prefs.kt`:
1. In `data class Settings`, dopo `seenQuestions`:

```kotlin
    /** Configurazione Firebase arrivata dal telefono (JSON di FirebaseConfig); null = quella dentro la build, se c'è. */
    val firebaseJson: String? = null,
    /** Solo telefono: l'accoppiamento (JSON di PairingRecord). */
    val pairingJson: String? = null,
    /** Solo telefono: il QR da riprendere dopo il riavvio per un altro progetto Firebase. */
    val resumeQr: String? = null,
```

2. Prima di `private val Context.dataStore`:

```kotlin
/** Le preferenze viste dal codice che si prova sulla JVM (il controller del telefono). */
interface SettingsStore {
    suspend fun current(): Settings
    suspend fun update(block: (Settings) -> Settings)
}
```

3. `class Prefs(private val ctx: Context) : SettingsStore {`; `override` su `current()` e `update(...)`.
4. In `object K`: `val firebaseJson = stringPreferencesKey("firebaseJson"); val pairingJson = stringPreferencesKey("pairingJson"); val resumeQr = stringPreferencesKey("resumeQr")`.
5. Nella lettura di `flow`: `firebaseJson = p[K.firebaseJson], pairingJson = p[K.pairingJson], resumeQr = p[K.resumeQr],`.
6. Nella scrittura di `update`:

```kotlin
            s.firebaseJson?.let { p[K.firebaseJson] = it } ?: p.remove(K.firebaseJson)
            s.pairingJson?.let { p[K.pairingJson] = it } ?: p.remove(K.pairingJson)
            s.resumeQr?.let { p[K.resumeQr] = it } ?: p.remove(K.resumeQr)
```

- [ ] **Step 4: Verifica che passino**

Run: `$GW :core:testDebugUnitTest`
Expected: PASS, tutta la suite di `core`.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/pairing/WatchHandoff.kt core/src/main/kotlin/it/pixelbox/cmwatch/pairing/FirebaseBoot.kt core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairingRecord.kt core/src/main/kotlin/it/pixelbox/cmwatch/settings/Prefs.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/WatchHandoffTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/FirebaseBootTest.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairingRecordTest.kt
git commit -m "feat(core): watch side of the handoff, Firebase started by the app, pairing record and new preferences"
```

---

### Task 8: Modulo comune `:ui-tokens`

Spostamento puro di `CmColors`: gli screenshot dell'orologio non cambiano (verificato nel Task 15).

**Files:**
- Create: `ui-tokens/build.gradle.kts`, `ui-tokens/src/main/kotlin/it/pixelbox/cmwatch/ui/tokens/CmColors.kt` (spostato)
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml`, `wear/build.gradle.kts`, `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/theme/Theme.kt` e ogni file di `wear/src` che usa `CmColors` (gli import)

**Interfaces:**
- Produces: `it.pixelbox.cmwatch.ui.tokens.CmColors` (stessi campi di oggi).

- [ ] **Step 1: Modulo**

In `settings.gradle.kts`: `include(":core", ":wear", ":mobile", ":ui-tokens")`.

In `gradle/libs.versions.toml`, sotto `[libraries]`:

```toml
compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
```

`ui-tokens/build.gradle.kts`:

```kotlin
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
```

In `wear/build.gradle.kts`, nei `dependencies`: `implementation(project(":ui-tokens"))`.

- [ ] **Step 2: Sposta `CmColors` e correggi gli import**

```bash
python3 - <<'EOF'
from pathlib import Path
theme = Path("wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/theme/Theme.kt")
lines = theme.read_text().split("\n")
start = next(i for i, l in enumerate(lines) if l.startswith("object CmColors {"))
if lines[start - 1].startswith("/**"):
    start -= 1
end = next(i for i in range(start, len(lines)) if lines[i] == "}")
out = Path("ui-tokens/src/main/kotlin/it/pixelbox/cmwatch/ui/tokens/CmColors.kt")
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("package it.pixelbox.cmwatch.ui.tokens\n\nimport androidx.compose.ui.graphics.Color\n\n" + "\n".join(lines[start:end + 1]) + "\n")
theme.write_text("\n".join(lines[:start] + lines[end + 1:]))
print("spostate", end - start + 1, "righe")
EOF
grep -rl "it.pixelbox.cmwatch.wear.ui.theme.CmColors" wear/src | xargs sed -i 's/it\.pixelbox\.cmwatch\.wear\.ui\.theme\.CmColors/it.pixelbox.cmwatch.ui.tokens.CmColors/g'
python3 - <<'EOF'
from pathlib import Path
for f in Path("wear/src").rglob("*.kt"):
    t = f.read_text()
    if "CmColors" in t and "import it.pixelbox.cmwatch.ui.tokens.CmColors" not in t:
        lines = t.split("\n")
        i = next(k for k, l in enumerate(lines) if l.startswith("package "))
        lines[i + 1:i + 1] = ["", "import it.pixelbox.cmwatch.ui.tokens.CmColors"]
        f.write_text("\n".join(lines))
        print("import aggiunto:", f)
EOF
grep -rn "wear.ui.theme.CmColors" wear/src || echo "nessun riferimento vecchio"
```

Expected: «spostate N righe» (circa 70), almeno `Theme.kt` fra gli «import aggiunto», e «nessun riferimento vecchio».

- [ ] **Step 3: Compila orologio e test**

Run: `$GW :wear:compileDebugKotlin :wear:compileDebugUnitTestKotlin :core:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Se un file lamenta un import doppio o mancante, correggilo a mano e ricompila.

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts gradle/libs.versions.toml ui-tokens/build.gradle.kts ui-tokens/src/main/kotlin/it/pixelbox/cmwatch/ui/tokens/CmColors.kt wear/build.gradle.kts
git add $(git diff --name-only -- wear/src)
git commit -m "refactor(wear): CmColors moves to the shared ui-tokens module, for the phone too"
```

(`git diff --name-only -- wear/src` elenca i file per nome: nessun `git add` di cartella.)

---

### Task 9: Orologio, Firebase avviato dall'app

**Files:**
- Modify: `wear/src/main/AndroidManifest.xml`
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt`
- Modify: `wear/build.gradle.kts` (via `buildConfigField FIREBASE`)
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/TransportChoice.kt` (solo il commento)

**Interfaces:**
- Consumes: `FirebaseBoot.start`, `FirebaseBoot.active` (Task 7), `FirebaseConfig.fromJson`, `Settings.firebaseJson`.

- [ ] **Step 1: Manifest**

Aggiungi `xmlns:tools="http://schemas.android.com/tools"` al tag `<manifest>` e, dentro `<application>`:

```xml
        <!-- Firebase lo avvia l'app, con la configurazione arrivata dal telefono o con quella della build (design 24/09). -->
        <provider
            android:name="com.google.firebase.provider.FirebaseInitProvider"
            android:authorities="${applicationId}.firebaseinitprovider"
            tools:node="remove" />
```

- [ ] **Step 2: `CmApp`**

1. In `onCreate`, subito dopo `val settings = runBlocking { prefs.current() }`:

```kotlin
        FirebaseBoot.start(this, FirebaseConfig.fromJson(settings.firebaseJson))
```

2. `choose()` diventa:

```kotlin
    /** Firebase solo se accoppiato, con la chiave nel vault e Firebase avviato; altrimenti il finto sulle fixture. */
    fun choose(settings: Settings): Transport {
        val key = settings.wrappedKey?.let { runCatching { KeyVault.unwrap(it, KeyVault.keystoreKek()) }.getOrNull() }
        val fb = FirebaseBoot.active
        val kind = TransportChoice.pick(settings.paired, key != null, fb != null, demo = settings.demoMode)
        return when (kind) {
            TransportChoice.Kind.FAKE -> fake
            TransportChoice.Kind.FIREBASE -> FirebaseTransport(
                rtdb = Rtdb(fb!!.databaseUrl.removeSuffix("/"), token = { FirebaseAuthToken.token() }),
                key = { key }, uid = { FirebaseAuthToken.uid() }, deviceKeyPair = { Pairing.newKeyPair() },
                now = { System.currentTimeMillis() / 1000 },
            )
        }
    }
```

3. In `pairingTransport()`: `val url = FirebaseBoot.active?.databaseUrl`.
4. `subscribeTopic()` diventa:

```kotlin
    fun subscribeTopic() {
        val fb = FirebaseBoot.active ?: return
        runCatching {
            val fm = FirebaseMessaging.getInstance()
            fm.token.addOnCompleteListener { t -> android.util.Log.i("cmwatch", "fcm token: " + (if (t.isSuccessful) "ok (${t.result?.take(12)}…)" else "failed ${t.exception?.message}")) }
            fm.subscribeToTopic(fb.topic).addOnCompleteListener { t -> android.util.Log.i("cmwatch", "fcm topic ${fb.topic}: " + (if (t.isSuccessful) "subscribed" else "failed ${t.exception?.message}")) }
        }.onFailure { android.util.Log.w("cmwatch", "fcm: ${it.message}") }
    }
```

5. Togli `companion object { const val FCM_TOPIC = "watch" }` e l'import di `BuildConfig`, se non serve più; aggiungi gli import `it.pixelbox.cmwatch.pairing.FirebaseBoot` e `it.pixelbox.cmwatch.pairing.FirebaseConfig`.

Controlla che nessun altro usi quello che togli:

```bash
grep -rn "FCM_TOPIC\|BuildConfig.FIREBASE" wear/src core/src || echo "nessun uso rimasto"
```

- [ ] **Step 3: Build, e commento di `TransportChoice`**

In `wear/build.gradle.kts` togli la riga `buildConfigField("boolean", "FIREBASE", …)`. In `TransportChoice.kt` il commento della classe diventa: `/** Quale Transport usare: Firebase solo se accoppiato, con la chiave nel vault e Firebase avviato (dal telefono o dalla build); altrimenti il finto. */`.

- [ ] **Step 4: Test e build**

Run: `$GW :core:testDebugUnitTest :wear:assembleDebug`
Expected: BUILD SUCCESSFUL; `TransportChoiceTest` invariato e verde.

- [ ] **Step 5: Prova sull'emulatore con la configurazione dentro la build**

`wear/google-services.json` c'è, quindi è il percorso della release di Franz:

```bash
/usr/bin/adb -s emulator-5554 install -r wear/build/outputs/apk/debug/wear-debug.apk
/usr/bin/adb -s emulator-5554 shell pm clear it.pixelbox.cmwatch
/usr/bin/adb -s emulator-5554 logcat -c
/usr/bin/adb -s emulator-5554 shell am start -n it.pixelbox.cmwatch/.wear.MainActivity
sleep 15
/usr/bin/adb -s emulator-5554 logcat -d -s cmwatch | grep -E "fcm topic|firebase:"
```

Expected: `fcm topic watch: subscribed`, nessun `firebase:` d'errore. Firebase parte senza `FirebaseInitProvider`, dai valori della build.

- [ ] **Step 6: Commit**

```bash
git add wear/src/main/AndroidManifest.xml wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt wear/build.gradle.kts core/src/main/kotlin/it/pixelbox/cmwatch/rules/TransportChoice.kt
git commit -m "feat(watch): Firebase started by the app from the phone's config or the build's, topic from the config"
```

---

### Task 10: Orologio, il ricevitore dell'accoppiamento

**Files:**
- Create: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairReceiver.kt`
- Create: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairListenerService.kt`
- Create: `wear/src/main/res/values/wear.xml`
- Modify: `wear/src/main/AndroidManifest.xml`, `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt`, `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt`, `wear/build.gradle.kts`, `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: `WatchHandoff`, `FirebaseBoot`, `HandoffMessages` e i messaggi (Task 5 e 7), `KeyVault`, `FirebaseAuthToken`, `Haptics`.
- Produces: `CmApp.pairReceiver`, `CmApp.restartSoon()`; `PairReceiver.handle(path, body): ByteArray`.

- [ ] **Step 1: Dipendenze**

In `gradle/libs.versions.toml`:

```toml
[versions]
playServicesWearable = "20.0.1"

[libraries]
play-services-wearable = { module = "com.google.android.gms:play-services-wearable", version.ref = "playServicesWearable" }
coroutines-play-services = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "coroutines" }
```

In `wear/build.gradle.kts`: `implementation(libs.play.services.wearable)` e `implementation(libs.coroutines.play.services)`.

- [ ] **Step 2: Ricevitore**

`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairReceiver.kt`:

```kotlin
package it.pixelbox.cmwatch.wear.pair

import it.pixelbox.cmwatch.crypto.KeyVault
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.HandoffException
import it.pixelbox.cmwatch.pairing.HandoffMessages
import it.pixelbox.cmwatch.pairing.HelloRequest
import it.pixelbox.cmwatch.pairing.HelloResponse
import it.pixelbox.cmwatch.pairing.KeyRequest
import it.pixelbox.cmwatch.pairing.KeyResponse
import it.pixelbox.cmwatch.pairing.WatchHandoff
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.haptics.Haptics

/** Il lato orologio dell'accoppiamento dal telefono (design 24/09): risponde a `hello` e a `key`. */
class PairReceiver(private val app: CmApp, private val handoff: WatchHandoff = WatchHandoff()) {

    suspend fun handle(path: String, body: ByteArray): ByteArray = when (path) {
        HandoffMessages.HELLO -> HandoffMessages.encode(HelloResponse.serializer(), hello(body))
        HandoffMessages.KEY -> HandoffMessages.encode(KeyResponse.serializer(), key(body))
        else -> HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST))
    }

    private suspend fun hello(body: ByteArray): HelloResponse {
        val req = HandoffMessages.decode(HelloRequest.serializer(), body) ?: return HelloResponse(error = HandoffMessages.ERR_BAD_REQUEST)
        val incoming = req.f.config()
        if (handoff.needsRestart(FirebaseBoot.active, incoming)) {
            // Un altro progetto: si salva e si riparte; il prossimo `hello` del telefono trova Firebase già su quello nuovo.
            app.prefs.update { it.copy(firebaseJson = incoming.toJson()) }
            app.restartSoon()
            return HelloResponse(restart = true)
        }
        if (FirebaseBoot.active == null && FirebaseBoot.start(app, incoming) == null) return HelloResponse(error = HandoffMessages.ERR_AUTH)
        app.prefs.update { it.copy(firebaseJson = incoming.toJson()) }
        FirebaseBoot.retopic(incoming.topic)
        FirebaseAuthToken.token() ?: return HelloResponse(error = HandoffMessages.ERR_AUTH)
        val uid = FirebaseAuthToken.uid() ?: return HelloResponse(error = HandoffMessages.ERR_AUTH)
        return HelloResponse(uid = uid, name = app.prefs.current().deviceName, eph = handoff.open(uid))
    }

    private suspend fun key(body: ByteArray): KeyResponse {
        val req = HandoffMessages.decode(KeyRequest.serializer(), body) ?: return KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST)
        val k = try { handoff.take(req) } catch (e: HandoffException) { return KeyResponse(error = e.code) }
        val wrapped = runCatching { KeyVault.wrap(k, KeyVault.keystoreKek()) }.getOrNull() ?: return KeyResponse(error = HandoffMessages.ERR_STORE)
        app.prefs.update { it.copy(paired = true, uid = FirebaseAuthToken.uid(), host = req.host, wrappedKey = wrapped) }
        app.subscribeTopic()
        app.reconfigure()
        Haptics.play(app, Haptics.Kind.CONFIRMED)
        return KeyResponse(ok = true)
    }
}
```

`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairListenerService.kt`:

```kotlin
package it.pixelbox.cmwatch.wear.pair

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.WearableListenerService
import it.pixelbox.cmwatch.pairing.HandoffMessages
import it.pixelbox.cmwatch.pairing.KeyResponse
import it.pixelbox.cmwatch.wear.CmApp
import kotlinx.coroutines.launch

/** Le richieste del telefono (`/cmwatch/pair/…`) arrivano anche ad app chiusa: il servizio si avvia da sé (spike C). */
class PairListenerService : WearableListenerService() {
    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray> {
        val app = application as CmApp
        val done = TaskCompletionSource<ByteArray>()
        app.scope.launch {
            done.setResult(runCatching { app.pairReceiver.handle(path, request) }.getOrElse {
                android.util.Log.w("cmwatch", "pair request $path", it)
                HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST))
            })
        }
        return done.task
    }
}
```

- [ ] **Step 3: Manifest e capacità**

In `wear/src/main/AndroidManifest.xml`, dentro `<application>`:

```xml
        <service android:name=".wear.pair.PairListenerService" android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.REQUEST_RECEIVED" />
                <data android:scheme="wear" android:host="*" android:pathPrefix="/cmwatch/pair" />
            </intent-filter>
        </service>
```

`wear/src/main/res/values/wear.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@array/android_wear_capabilities">
    <string-array name="android_wear_capabilities" translatable="false">
        <item>cmwatch_wear</item>
    </string-array>
</resources>
```

- [ ] **Step 4: `CmApp`**

Accanto alle altre proprietà:

```kotlin
    val pairReceiver by lazy { it.pixelbox.cmwatch.wear.pair.PairReceiver(this) }

    /** Dopo aver risposto `restart` al telefono: il processo si chiude, e il messaggio seguente lo riavvia con la configurazione nuova. */
    fun restartSoon() {
        scope.launch { kotlinx.coroutines.delay(800); android.os.Process.killProcess(android.os.Process.myPid()) }
    }
```

- [ ] **Step 5: «Accoppiato con \<PC\>» anche quando accoppia il telefono**

In `MainActivity`, subito dopo `CmConfirm(conferma) { conferma = null }`:

```kotlin
        // Accoppiato dal telefono (design 24/09): le preferenze passano a paired, qui si dice con chi.
        var wasPaired by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(settings?.paired, settings?.host) {
            val now = settings?.paired ?: return@LaunchedEffect
            if (wasPaired == false && now && pairing !is PairingStatus.Done) {
                conferma(Icons.Rounded.Check, CmColors.briefGood, getString(R.string.pairing_done, settings?.host.orEmpty()))
            }
            wasPaired = now
        }
```

Import: `androidx.compose.material.icons.rounded.Check` (se manca).

- [ ] **Step 6: Build**

Run: `$GW :core:testDebugUnitTest :wear:assembleDebug`
Expected: BUILD SUCCESSFUL. La prova vera è nel Task 18: la logica è coperta da `WatchHandoffTest`, e il canale dallo spike C.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml wear/build.gradle.kts wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairReceiver.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PairListenerService.kt wear/src/main/res/values/wear.xml wear/src/main/AndroidManifest.xml wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt
git commit -m "feat(watch): answer the phone's pairing — hello with a temporary key, the sealed key stored in the Keystore"
```

---

### Task 11: Orologio, schermata «Accoppia» con il telefono per primo

**Files:**
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/PairingScreen.kt`
- Create: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PhoneLauncher.kt`
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt`, `wear/src/main/res/values/strings.xml`, `wear/src/main/res/values-en/strings.xml`, `wear/src/test/kotlin/it/pixelbox/cmwatch/wear/ScreensSnapshotTest.kt`, `wear/build.gradle.kts`, `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: `FirebaseBoot.bundled` (Task 7).
- Produces: `PairingScreen(status, withCode, onOpenPhone, onEnterCode, onRetry)`; `PhoneLauncher.open(ctx): Boolean`.

- [ ] **Step 1: Dipendenza**

`gradle/libs.versions.toml`: `remoteInteractions = "1.2.0"` in `[versions]` e `wear-remote-interactions = { module = "androidx.wear:wear-remote-interactions", version.ref = "remoteInteractions" }` in `[libraries]`. In `wear/build.gradle.kts`: `implementation(libs.wear.remote.interactions)`.

- [ ] **Step 2: Testi**

In `wear/src/main/res/values/strings.xml`, togli `pairing_hint`, cambia `pairing_enter_code` e aggiungi:

```xml
    <string name="pairing_enter_code">Usa il codice</string>
    <string name="pairing_hint_phone">Sul telefono apri claude-master e tocca Accoppia.</string>
    <string name="pairing_hint_both">Sul telefono apri claude-master e tocca Accoppia, oppure usa il codice di relay pair.</string>
    <string name="pairing_open_phone">Apri sul telefono</string>
    <string name="pairing_continue_phone">Continua sul telefono</string>
```

In `values-en/strings.xml`, stesso:

```xml
    <string name="pairing_enter_code">Use the code</string>
    <string name="pairing_hint_phone">On your phone, open claude-master and tap Pair.</string>
    <string name="pairing_hint_both">On your phone, open claude-master and tap Pair, or use the code from relay pair.</string>
    <string name="pairing_open_phone">Open on phone</string>
    <string name="pairing_continue_phone">Continue on phone</string>
```

Controlla che `pairing_hint` non serva altrove: `grep -rn "pairing_hint\b" wear/src core/src` (atteso: niente).

- [ ] **Step 3: Snapshot che falliscono**

In `ScreensSnapshotTest.kt` sostituisci il test `pairing` e aggiungine uno:

```kotlin
    // Design 24/09: prima il telefono; con la configurazione dentro la build resta anche il codice a 6 cifre.
    @Test fun pairing() = paparazzi.snapshot { CmTheme { PairingScreen(PairingStatus.Idle, withCode = true, {}, {}, {}) } }
    @Test fun pairingPhoneOnly() = paparazzi.snapshot { CmTheme { PairingScreen(PairingStatus.Idle, withCode = false, {}, {}, {}) } }
```

Run: `$GW :wear:compileDebugUnitTestKotlin`
Expected: FAIL, `No parameter with name 'withCode'`. Paparazzi gira solo in CI: le immagini si registrano nel Task 15.

- [ ] **Step 4: Schermata**

`PairingScreen` diventa:

```kotlin
/** Accoppiamento (design 24/09): dal telefono, che legge il QR di `relay pair`; il codice a 6 cifre solo se la build ha la configurazione dentro. */
@Composable
fun PairingScreen(status: PairingStatus, withCode: Boolean, onOpenPhone: () -> Unit, onEnterCode: () -> Unit, onRetry: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.pairing_title)) } }
            item {
                val text = when (status) {
                    PairingStatus.Idle -> stringResource(if (withCode) R.string.pairing_hint_both else R.string.pairing_hint_phone)
                    PairingStatus.Working -> stringResource(R.string.pairing_working)
                    is PairingStatus.Done -> stringResource(R.string.pairing_done, status.host)
                    is PairingStatus.Failed -> stringResource(R.string.pairing_failed)
                }
                val color = if (status is PairingStatus.Failed) CmColors.gone else CmColors.text2
                Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.fillMaxWidth().morph(this, spec))
            }
            item {
                if (status is PairingStatus.Failed) {
                    WideButton(stringResource(R.string.question_retry), onClick = onRetry, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
                } else {
                    WideButton(stringResource(R.string.pairing_open_phone), onClick = onOpenPhone, primary = true, enabled = status !is PairingStatus.Working, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
                }
            }
            if (withCode && status !is PairingStatus.Failed) item {
                WideButton(stringResource(R.string.pairing_enter_code), onClick = onEnterCode, primary = false, enabled = status !is PairingStatus.Working, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
        }
    }
}
```

`wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PhoneLauncher.kt`:

```kotlin
package it.pixelbox.cmwatch.wear.pair

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** «Apri sul telefono»: l'app se c'è (capacità cmwatch_phone), altrimenti la sua pagina su Play. */
object PhoneLauncher {
    private const val PHONE_CAPABILITY = "cmwatch_phone"

    suspend fun open(ctx: Context): Boolean = runCatching {
        val hasApp = Wearable.getCapabilityClient(ctx).getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await().nodes.isNotEmpty()
        val uri = if (hasApp) "cmwatch://pair" else "market://details?id=${ctx.packageName}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE)
        withContext(Dispatchers.IO) { RemoteActivityHelper(ctx).startRemoteActivity(intent).get(20, TimeUnit.SECONDS) }
        true
    }.getOrDefault(false)
}
```

- [ ] **Step 5: `MainActivity`**

Il `composable(Routes.PAIRING)` diventa:

```kotlin
            composable(Routes.PAIRING) {
                val withCode = remember { FirebaseBoot.bundled(this@MainActivity) != null }
                PairingScreen(
                    pairing,
                    withCode = withCode,
                    onOpenPhone = {
                        scope.launch {
                            if (PhoneLauncher.open(this@MainActivity)) conferma(Icons.Rounded.PhoneAndroid, CmColors.primary, getString(R.string.pairing_continue_phone))
                            else Haptics.play(this@MainActivity, Haptics.Kind.ERROR)
                        }
                    },
                    onEnterCode = {
                        runCatching { codeInput.launch(Keyboard.intent(getString(R.string.pairing_code_label))) }
                            .onFailure { pairing = PairingStatus.Failed("no keyboard") }
                    },
                    onRetry = { pairing = PairingStatus.Idle },
                )
            }
```

Import: `it.pixelbox.cmwatch.pairing.FirebaseBoot`, `it.pixelbox.cmwatch.wear.pair.PhoneLauncher`, `androidx.compose.material.icons.rounded.PhoneAndroid`.

- [ ] **Step 6: Compila**

Run: `$GW :wear:assembleDebug :wear:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml wear/build.gradle.kts wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/PairingScreen.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/pair/PhoneLauncher.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt wear/src/main/res/values/strings.xml wear/src/main/res/values-en/strings.xml wear/src/test/kotlin/it/pixelbox/cmwatch/wear/ScreensSnapshotTest.kt
git commit -m "feat(watch): pairing screen leads to the phone; the 6-digit code stays for builds with the config inside"
```

---

### Task 12: Telefono, modulo, tema e prima schermata

**Files:**
- Modify: `mobile/build.gradle.kts` (intero), `mobile/src/main/AndroidManifest.xml` (intero), `gradle/libs.versions.toml`, `.github/workflows/build-android.yml`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt`, `.../mobile/ui/PhoneTheme.kt`, `.../mobile/ui/NotPairedScreen.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt` (intero)
- Create: `mobile/src/main/res/values/themes.xml`, `values/colors.xml`, `values/wear.xml`, `values-en/strings.xml`; Modify: `values/strings.xml`
- Create (copie dall'orologio): `mobile/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `mobile/src/main/res/drawable/ic_launcher_fg.xml`, `ic_launcher_mono.xml`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt`

**Interfaces:**
- Consumes: `CmColors` (Task 8), `Prefs`, `FirebaseBoot`, `FirebaseConfig` (Task 7).
- Produces: `PhoneApp` (`scope`, `prefs`, `phoneName`), `CmPhoneTheme { }`, `CmMotion.easing`, `CmMotion.spec(off)`, `animationsOff()`, `NotPairedScreen(onPair, onPaste)`.

- [ ] **Step 1: Librerie**

`gradle/libs.versions.toml`: `codeScanner = "16.1.0"` in `[versions]`; in `[libraries]`:

```toml
play-services-code-scanner = { module = "com.google.android.gms:play-services-code-scanner", version.ref = "codeScanner" }
compose-material3 = { module = "androidx.compose.material3:material3" }
```

- [ ] **Step 2: `mobile/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

// Come per l'orologio: google-services.json solo nelle build di sviluppo; la build di Play non lo ha (design 24/09).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "it.pixelbox.cmwatch.mobile"
    compileSdk = 36
    defaultConfig {
        // Lo stesso dell'orologio: il canale di Wear OS unisce solo app con stesso pacchetto e stessa firma.
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
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui-tokens"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.code.scanner)
    implementation(libs.wear.remote.interactions)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 3: Manifest e risorse**

`mobile/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:name=".PhoneApp"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:allowBackup="false"
        android:theme="@style/Theme.CmPhone">
        <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- «Apri sul telefono» dall'orologio -->
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="cmwatch" android:host="pair" />
            </intent-filter>
        </activity>

        <!-- Firebase lo avvia l'app, con la configurazione del QR (design 24/09). -->
        <provider
            android:name="com.google.firebase.provider.FirebaseInitProvider"
            android:authorities="${applicationId}.firebaseinitprovider"
            tools:node="remove" />
        <!-- Lo scanner di Play scarica il suo modulo all'installazione, non alla prima lettura. -->
        <meta-data android:name="com.google.mlkit.vision.DEPENDENCIES" android:value="barcode_ui" />
    </application>
</manifest>
```

Icona, la stessa dell'orologio:

```bash
mkdir -p mobile/src/main/res/mipmap-anydpi-v26 mobile/src/main/res/drawable mobile/src/main/res/values-en
cp wear/src/main/res/mipmap-anydpi-v26/ic_launcher.xml wear/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml mobile/src/main/res/mipmap-anydpi-v26/
cp wear/src/main/res/drawable/ic_launcher_fg.xml wear/src/main/res/drawable/ic_launcher_mono.xml mobile/src/main/res/drawable/
```

`mobile/src/main/res/values/colors.xml`:

```xml
<resources>
    <color name="ic_bg">#000000</color>
</resources>
```

`mobile/src/main/res/values/themes.xml`:

```xml
<resources>
    <!-- Solo scuro, come l'orologio (design 24/09); Compose disegna il resto. -->
    <style name="Theme.CmPhone" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
```

`mobile/src/main/res/values/wear.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@array/android_wear_capabilities">
    <string-array name="android_wear_capabilities" translatable="false">
        <item>cmwatch_phone</item>
    </string-array>
</resources>
```

`mobile/src/main/res/values/strings.xml` (sostituisce il file):

```xml
<resources>
    <string name="app_name">claude-master</string>
    <string name="not_paired_title">Collega il PC</string>
    <string name="not_paired_body">Sul PC lancia claude-master relay pair e inquadra il QR. Il telefono accoppia anche l\'orologio.</string>
    <string name="pair_button">Accoppia</string>
    <string name="paste_code">Incolla il codice</string>
</resources>
```

`mobile/src/main/res/values-en/strings.xml`:

```xml
<resources>
    <string name="not_paired_title">Connect your PC</string>
    <string name="not_paired_body">On the PC run claude-master relay pair and scan the QR. The phone pairs the watch too.</string>
    <string name="pair_button">Pair</string>
    <string name="paste_code">Paste the code</string>
</resources>
```

- [ ] **Step 4: Snapshot che fallisce**

`mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.NotPairedScreen
import org.junit.Rule
import org.junit.Test

/** Le schermate del telefono (design 24/09). Si registrano in GitHub Actions: `./gradlew :mobile:recordPaparazziDebug`. */
class PhoneScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun notPaired() = paparazzi.snapshot { CmPhoneTheme { NotPairedScreen(onPair = {}, onPaste = {}) } }
}
```

Run: `$GW :mobile:compileDebugUnitTestKotlin`
Expected: FAIL, `Unresolved reference 'CmPhoneTheme'`.

- [ ] **Step 5: Tema, schermata, app**

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PhoneTheme.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.ui

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import it.pixelbox.cmwatch.ui.tokens.CmColors

private val scheme = darkColorScheme(
    primary = CmColors.primary, onPrimary = CmColors.onPrimary,
    background = CmColors.bg, onBackground = CmColors.text,
    surface = CmColors.surfaceLow, onSurface = CmColors.text,
    surfaceVariant = CmColors.surface, onSurfaceVariant = CmColors.text2,
    surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surfaceHigh,
    outline = CmColors.line, error = CmColors.gone,
)

/** Solo scuro, i colori dell'orologio, niente colori dinamici (design 24/09, «Aspetto e movimento»). */
@Composable
fun CmPhoneTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

/** Tempi e curva del movimento: 250 ms e l'easing del sito; con le animazioni spente, subito lo stato finale. */
object CmMotion {
    val easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
    fun <T> spec(off: Boolean): AnimationSpec<T> = if (off) snap() else tween(250, easing = easing)
}

/** «Riduci animazioni» o animazioni di sistema a zero. Negli snapshot vale sempre «accese». */
@Composable
fun animationsOff(): Boolean {
    if (LocalInspectionMode.current) return false
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NotPairedScreen.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Primo avvio: due righe e un solo bottone pieno (design 24/09, schermata 1). */
@Composable
fun NotPairedScreen(onPair: () -> Unit, onPaste: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.not_paired_title), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        Text(stringResource(R.string.not_paired_body), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
        Spacer(Modifier.weight(1f))
        Button(onClick = onPair, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.pair_button)) }
        TextButton(onClick = onPaste, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paste_code), color = CmColors.actionIcon) }
    }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile

import android.app.Application
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.settings.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

class PhoneApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var prefs: Prefs
    val phoneName: String by lazy {
        android.provider.Settings.Global.getString(contentResolver, android.provider.Settings.Global.DEVICE_NAME) ?: android.os.Build.MODEL
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        FirebaseBoot.start(this, FirebaseConfig.fromJson(runBlocking { prefs.current() }.firebaseJson))
    }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt` (provvisorio, completato nel Task 14):

```kotlin
package it.pixelbox.cmwatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.NotPairedScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CmPhoneTheme { NotPairedScreen(onPair = {}, onPaste = {}) } }
    }
}
```

- [ ] **Step 6: CI**

In `.github/workflows/build-android.yml`:
1. `paths:` aggiunge `'ui-tokens/**'`; la `key:` della cache aggiunge `'mobile/build.gradle.kts', 'ui-tokens/build.gradle.kts'` dentro `hashFiles(…)`.
2. `Unit tests` → `run: ./gradlew :core:testDebugUnitTest :mobile:testDebugUnitTest --console=plain`.
3. Dopo il passo `Screenshot tests (Paparazzi, verify)`:

```yaml
      - name: Screenshot tests del telefono (Paparazzi, verify)
        if: hashFiles('mobile/src/test/snapshots/**') != '' && github.event.inputs.record != 'true'
        run: ./gradlew :mobile:verifyPaparazziDebug --console=plain
```

4. Il passo di record: `run: ./gradlew :wear:recordPaparazziDebug :mobile:recordPaparazziDebug --console=plain`.
5. `Upload snapshots`, `path:` diventa:

```yaml
          path: |
            wear/src/test/snapshots
            mobile/src/test/snapshots
```

- [ ] **Step 7: Build**

Run: `$GW :mobile:assembleDebug :mobile:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml mobile/build.gradle.kts mobile/src/main/AndroidManifest.xml mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PhoneTheme.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NotPairedScreen.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/main/res/values/themes.xml mobile/src/main/res/values/colors.xml mobile/src/main/res/values/wear.xml mobile/src/main/res/mipmap-anydpi-v26/ic_launcher.xml mobile/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml mobile/src/main/res/drawable/ic_launcher_fg.xml mobile/src/main/res/drawable/ic_launcher_mono.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt .github/workflows/build-android.yml
git commit -m "feat(phone): the phone app becomes it.pixelbox.cmwatch — dark theme from ui-tokens, first screen, CI for its tests"
```

---

### Task 13: Telefono, il controller dell'accoppiamento

Tutta la logica del flusso del telefono, provata sulla JVM con telefono, orologio e PC finti.

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/Ports.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingController.kt`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingControllerTest.kt`

**Interfaces:**
- Consumes: `PairQr`, `FirebaseConfig` (Task 3); `Handoff`, messaggi (Task 5); `PairError`, `PhonePairResult`, `WatchPeer` (Task 6); `WatchHandoff` (solo nei test), `PairingRecord`, `SettingsStore`, `Settings` (Task 7); `Rtdb`.
- Produces: `WatchNode(id, name)`; `WatchLink` (`find()`, `anyConnected()`, `request(node, path, body)`, `openPlayOnWatch(node)`); `Ensure` (`Ready(uid)`, `Restart`, `Failed`); `PhoneFirebase` (`ensure(cfg)`, `rtdb()`); `KeyWrap` (`wrap`, `unwrap`); `fun interface PcPairer`; `Step`, `StepState`, `PairFail`, `Phase`, `PairUi`; `PairingController(store, firebase, link, keys, pairer, phoneName, now, restartWaitMs, newEph)` con `ui: StateFlow<PairUi>`, `run(text, withoutWatch)`, `retry(withoutWatch)`, `completePending()`, `installOnWatch()`, `reset()`.

- [ ] **Step 1: Test che falliscono**

`mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingControllerTest.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.pairing.*
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.settings.SettingsStore
import it.pixelbox.cmwatch.transport.Rtdb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

private class MemStore(var s: Settings = Settings()) : SettingsStore {
    override suspend fun current() = s
    override suspend fun update(block: (Settings) -> Settings) { s = block(s) }
}

private object PlainKeys : KeyWrap {
    override fun wrap(key: ByteArray): String = Base64.getEncoder().encodeToString(key)
    override fun unwrap(wrapped: String): ByteArray = Base64.getDecoder().decode(wrapped)
}

private class FakeFirebase(var result: Ensure = Ensure.Ready("phoneUid")) : PhoneFirebase {
    override suspend fun ensure(cfg: FirebaseConfig) = result
    override fun rtdb(): Rtdb = error("non usato: il PC è finto")
}

/** Un orologio finto che risponde come PairReceiver, con il WatchHandoff vero. */
private class FakeWatch : WatchLink {
    var uid = "watchUid"; var restartsLeft = 0; var reachable = true; var connectedWithoutApp = false; var dropKey = false
    var received: ByteArray? = null
    private val handoff = WatchHandoff()
    private val node = WatchNode("n1", "Pixel Watch 5")
    override suspend fun find() = node.takeIf { reachable }
    override suspend fun anyConnected() = node.takeIf { reachable || connectedWithoutApp }
    override suspend fun openPlayOnWatch(node: WatchNode) = true
    override suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray = when (path) {
        HandoffMessages.HELLO -> HandoffMessages.encode(HelloResponse.serializer(),
            if (restartsLeft > 0) { restartsLeft--; HelloResponse(restart = true) }
            else HelloResponse(uid = uid, name = "Pixel Watch 5", eph = handoff.open(uid)))
        HandoffMessages.KEY -> {
            if (dropKey) throw java.io.IOException("watch gone")
            received = handoff.take(HandoffMessages.decode(KeyRequest.serializer(), body)!!)
            HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(ok = true))
        }
        else -> error(path)
    }
}

class PairingControllerTest {
    private val qrText = File("../contract/pair-qr.json").readText()
    private val qr = PairQr.parse(qrText)!!
    private val key = ByteArray(32) { 9 }
    private val store = MemStore()
    private val watch = FakeWatch()
    private val fb = FakeFirebase()
    private var pcWatch: WatchPeer? = null
    private var pcError: PairError? = null

    private fun controller(now: Long = qr.e - 60) = PairingController(
        store, fb, watch, PlainKeys,
        pairer = { PcPairer { _, uid, _, w -> pcError?.let { throw it }; pcWatch = w; PhonePairResult(key, "penguin", listOfNotNull(uid, w?.uid)) } },
        phoneName = "Pixel 9", now = { now }, restartWaitMs = 0,
    )

    @Test fun phoneAndWatchPaired() = runTest {
        val c = controller()
        c.run(qrText)
        val ui = c.ui.value
        assertEquals(Phase.DONE, ui.phase)
        assertEquals(listOf(StepState.DONE, StepState.DONE, StepState.DONE), Step.entries.map { ui.steps[it] })
        assertEquals(WatchPeer("watchUid", "Pixel Watch 5"), pcWatch)
        assertArrayEquals(key, watch.received)
        val s = store.s
        assertTrue(s.paired); assertEquals("phoneUid", s.uid); assertEquals("penguin", s.host)
        assertArrayEquals(key, PlainKeys.unwrap(s.wrappedKey!!))
        assertEquals(PairingRecord(listOf("phoneUid", "watchUid"), mapOf("phoneUid" to "Pixel 9", "watchUid" to "Pixel Watch 5"), "watchUid", "Pixel Watch 5", watchPending = false),
            PairingRecord.fromJson(s.pairingJson))
        assertEquals(qr.f.config(), FirebaseConfig.fromJson(s.firebaseJson))
    }

    @Test fun notOurQrStopsBeforeTheNetwork() = runTest {
        val c = controller()
        c.run("https://example.com")
        assertEquals(PairFail.INVALID, c.ui.value.fail)
        assertNull(store.s.firebaseJson)
    }

    @Test fun expiredQr() = runTest {
        val c = controller(now = qr.e + 1)
        c.run(qrText)
        assertEquals(PairFail.EXPIRED, c.ui.value.fail)
    }

    @Test fun noWatchAsksThenPairsThePhoneAlone() = runTest {
        watch.reachable = false
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.NO_WATCH, c.ui.value.fail)
        assertFalse(store.s.paired)
        c.retry(withoutWatch = true)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertEquals(StepState.SKIPPED, c.ui.value.steps[Step.WATCH])
        assertNull(pcWatch)
        assertEquals(listOf("phoneUid"), PairingRecord.fromJson(store.s.pairingJson)!!.uids)
    }

    @Test fun watchWithoutTheApp() = runTest {
        watch.reachable = false; watch.connectedWithoutApp = true
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.WATCH_APP_MISSING, c.ui.value.fail)
    }

    @Test fun watchRestartsForAnotherProjectThenAnswers() = runTest {
        watch.restartsLeft = 1
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertArrayEquals(key, watch.received)
    }

    @Test fun pcSilentIsAnError() = runTest {
        pcError = PairError.NoConfirm()
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.PC_NO_CONFIRM, c.ui.value.fail)
        assertFalse(store.s.paired)
    }

    @Test fun watchGoneAfterThePcKeepsTheKeyForLater() = runTest {
        watch.dropKey = true
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertEquals(StepState.PENDING, c.ui.value.steps[Step.WATCH])
        assertTrue(PairingRecord.fromJson(store.s.pairingJson)!!.watchPending)
        watch.dropKey = false
        assertTrue(c.completePending())
        assertArrayEquals(key, watch.received)
        assertFalse(PairingRecord.fromJson(store.s.pairingJson)!!.watchPending)
    }

    @Test fun aWatchWithANewUidIsNotGivenTheKey() = runTest {
        watch.dropKey = true
        val c = controller()
        c.run(qrText)
        watch.dropKey = false; watch.uid = "someoneElse"
        assertFalse(c.completePending())
        assertNull(watch.received)
        assertEquals(PairFail.WATCH_UID_CHANGED, c.ui.value.fail)
    }

    @Test fun anotherFirebaseProjectRestartsThePhone() = runTest {
        fb.result = Ensure.Restart
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.RESTART, c.ui.value.phase)
        assertEquals(qrText, store.s.resumeQr)
        assertFalse(store.s.paired)
    }
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `$GW :mobile:testDebugUnitTest --tests 'it.pixelbox.cmwatch.mobile.pair.*'`
Expected: FAIL, compilazione: `Unresolved reference 'PairingController'`, `'WatchLink'`, ecc.

- [ ] **Step 3: Porte**

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/Ports.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.pairing.PairQr
import it.pixelbox.cmwatch.pairing.PhonePairResult
import it.pixelbox.cmwatch.pairing.WatchPeer
import it.pixelbox.cmwatch.transport.Rtdb

data class WatchNode(val id: String, val name: String)

/** Il canale di Wear OS verso l'orologio. */
interface WatchLink {
    /** Un orologio raggiungibile con la nostra app (capacità `cmwatch_wear`). */
    suspend fun find(): WatchNode?
    /** Un orologio collegato qualsiasi: distingue «nessun orologio» da «manca l'app». */
    suspend fun anyConnected(): WatchNode?
    suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray
    suspend fun openPlayOnWatch(node: WatchNode): Boolean
}

sealed class Ensure {
    data class Ready(val uid: String) : Ensure()
    /** Gira già un altro progetto Firebase: si riparte dal QR salvato. */
    data object Restart : Ensure()
    data object Failed : Ensure()
}

/** Firebase sul telefono: avviato con la configurazione del QR e con l'accesso anonimo fatto. */
interface PhoneFirebase {
    suspend fun ensure(cfg: FirebaseConfig): Ensure
    fun rtdb(): Rtdb
}

/** K sotto la chiave del Keystore del telefono. */
interface KeyWrap {
    fun wrap(key: ByteArray): String
    fun unwrap(wrapped: String): ByteArray
}

/** I passi 4 e 5 (PhonePairer), sostituibili nei test. */
fun interface PcPairer {
    suspend fun pair(qr: PairQr, phoneUid: String, phoneName: String, watch: WatchPeer?): PhonePairResult
}
```

- [ ] **Step 4: Controller**

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingController.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.crypto.Pairing
import it.pixelbox.cmwatch.pairing.*
import it.pixelbox.cmwatch.settings.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyPair

enum class Step { PHONE, WATCH, PC }
enum class StepState { WAIT, WORKING, DONE, PENDING, SKIPPED, FAILED }
enum class PairFail { EXPIRED, INVALID, NO_WATCH, WATCH_APP_MISSING, NETWORK, PC_NO_CONFIRM, WATCH_FAILED, WATCH_UID_CHANGED }
enum class Phase { IDLE, RUNNING, DONE, FAILED, RESTART }

data class PairUi(
    val phase: Phase = Phase.IDLE,
    val steps: Map<Step, StepState> = Step.entries.associateWith { StepState.WAIT },
    val fail: PairFail? = null,
    /** L'orologio si riavvia per un progetto nuovo: non è un errore, si aspetta. */
    val restarting: Boolean = false,
    val host: String? = null,
    val watchName: String? = null,
)

/** Il flusso del telefono (design 24/09, «Flusso»): telefono, orologio, PC, poi K all'orologio. */
class PairingController(
    private val store: SettingsStore,
    private val firebase: PhoneFirebase,
    private val link: WatchLink,
    private val keys: KeyWrap,
    private val pairer: (PhoneFirebase) -> PcPairer,
    private val phoneName: String,
    private val now: () -> Long = { System.currentTimeMillis() / 1000 },
    private val restartWaitMs: Long = 3_000,
    private val newEph: () -> KeyPair = { Pairing.newKeyPair() },
) {
    private val _ui = MutableStateFlow(PairUi())
    val ui: StateFlow<PairUi> = _ui
    private val mutex = Mutex()
    private var lastQr: String? = null

    suspend fun run(text: String, withoutWatch: Boolean = false) = mutex.withLock { runLocked(text, withoutWatch) }

    suspend fun retry(withoutWatch: Boolean = false) { lastQr?.let { run(it, withoutWatch) } }

    fun reset() { _ui.value = PairUi() }

    suspend fun installOnWatch(): Boolean = link.anyConnected()?.let { link.openPlayOnWatch(it) } ?: false

    private suspend fun runLocked(text: String, withoutWatch: Boolean) {
        lastQr = text
        _ui.value = PairUi(phase = Phase.RUNNING)
        val qr = PairQr.parse(text) ?: return fail(PairFail.INVALID, Step.PHONE)
        if (qr.expired(now())) return fail(PairFail.EXPIRED, Step.PHONE)
        val cfg = qr.f.config()

        step(Step.PHONE, StepState.WORKING)
        val phoneUid = when (val e = firebase.ensure(cfg)) {
            is Ensure.Ready -> e.uid
            Ensure.Restart -> {
                store.update { it.copy(firebaseJson = cfg.toJson(), resumeQr = text) }
                _ui.value = _ui.value.copy(phase = Phase.RESTART)
                return
            }
            Ensure.Failed -> return fail(PairFail.NETWORK, Step.PHONE)
        }
        store.update { it.copy(firebaseJson = cfg.toJson(), resumeQr = null) }
        step(Step.PHONE, StepState.DONE)

        step(Step.WATCH, StepState.WORKING)
        val node = link.find()
        var hello: HelloResponse? = null
        if (node == null) {
            if (link.anyConnected() != null) return fail(PairFail.WATCH_APP_MISSING, Step.WATCH)
            if (!withoutWatch) return fail(PairFail.NO_WATCH, Step.WATCH)
            step(Step.WATCH, StepState.SKIPPED)
        } else {
            hello = hello(node, cfg) ?: return fail(PairFail.WATCH_FAILED, Step.WATCH)
            _ui.value = _ui.value.copy(watchName = hello.name ?: node.name)
            step(Step.WATCH, StepState.DONE)
        }
        val peer = hello?.let { WatchPeer(it.uid!!, it.name ?: node!!.name) }

        step(Step.PC, StepState.WORKING)
        val result = try {
            pairer(firebase).pair(qr, phoneUid, phoneName, peer)
        } catch (e: PairError) {
            return fail(when (e) {
                is PairError.Expired -> PairFail.EXPIRED
                is PairError.Unknown -> PairFail.INVALID
                is PairError.Network -> PairFail.NETWORK
                is PairError.NoConfirm, is PairError.BadConfirm -> PairFail.PC_NO_CONFIRM
            }, Step.PC)
        }
        val record = PairingRecord(
            uids = result.uids,
            names = buildMap { put(phoneUid, phoneName); peer?.let { put(it.uid, it.name) } },
            watchUid = peer?.uid, watchName = peer?.name, watchPending = peer != null,
        )
        store.update { it.copy(paired = true, uid = phoneUid, host = result.host, wrappedKey = keys.wrap(result.key), pairingJson = record.toJson()) }
        step(Step.PC, StepState.DONE)
        _ui.value = _ui.value.copy(host = result.host)

        if (node != null && hello != null) {
            if (deliver(node, hello, result.key, result.host)) store.update { it.copy(pairingJson = record.copy(watchPending = false).toJson()) }
            else step(Step.WATCH, StepState.PENDING)
        }
        _ui.value = _ui.value.copy(phase = Phase.DONE)
    }

    /** L'orologio si è ricollegato dopo l'accoppiamento: riceve K (design 24/09, «Casi particolari»). */
    suspend fun completePending(): Boolean = mutex.withLock {
        val s = store.current()
        val record = PairingRecord.fromJson(s.pairingJson)?.takeIf { it.watchPending } ?: return@withLock false
        val cfg = FirebaseConfig.fromJson(s.firebaseJson) ?: return@withLock false
        val key = s.wrappedKey?.let { runCatching { keys.unwrap(it) }.getOrNull() } ?: return@withLock false
        val node = link.find() ?: return@withLock false
        val hello = hello(node, cfg) ?: return@withLock false
        if (hello.uid != record.watchUid) {
            // Dati cancellati sull'orologio: l'uid nuovo non è in /allowed, K non gli serve e non gli si dà.
            fail(PairFail.WATCH_UID_CHANGED, Step.WATCH)
            return@withLock false
        }
        val ok = deliver(node, hello, key, s.host.orEmpty())
        if (ok) store.update { it.copy(pairingJson = record.copy(watchPending = false).toJson()) }
        ok
    }

    /** `hello`, con fino a tre attese se l'orologio si riavvia per un progetto nuovo. */
    private suspend fun hello(node: WatchNode, cfg: FirebaseConfig): HelloResponse? {
        repeat(4) {
            val body = runCatching {
                link.request(node, HandoffMessages.HELLO, HandoffMessages.encode(HelloRequest.serializer(), HelloRequest(f = cfg.compact())))
            }.getOrNull() ?: return null
            val r = HandoffMessages.decode(HelloResponse.serializer(), body) ?: return null
            when {
                r.restart -> { _ui.value = _ui.value.copy(restarting = true); delay(restartWaitMs) }
                r.error != null || r.uid == null || r.eph == null -> return null
                else -> { _ui.value = _ui.value.copy(restarting = false); return r }
            }
        }
        return null
    }

    /** Passo 6: K cifrata per l'orologio. Un `no_session` (orologio riavviato nel frattempo) rifà `hello` una volta. */
    private suspend fun deliver(node: WatchNode, hello: HelloResponse, key: ByteArray, host: String, retry: Boolean = true): Boolean {
        val eph = newEph()
        val req = KeyRequest(host = host, eph = Pairing.publicB64(eph), box = Handoff.seal(key, eph.private, hello.eph!!, hello.uid!!))
        val body = runCatching { link.request(node, HandoffMessages.KEY, HandoffMessages.encode(KeyRequest.serializer(), req)) }.getOrNull() ?: return false
        val r = HandoffMessages.decode(KeyResponse.serializer(), body) ?: return false
        if (r.ok) return true
        if (retry && r.error == HandoffMessages.ERR_NO_SESSION) {
            val cfg = FirebaseConfig.fromJson(store.current().firebaseJson) ?: return false
            val again = hello(node, cfg) ?: return false
            if (again.uid != hello.uid) return false
            return deliver(node, again, key, host, retry = false)
        }
        return false
    }

    private fun step(s: Step, st: StepState) { _ui.value = _ui.value.copy(steps = _ui.value.steps + (s to st)) }

    private fun fail(f: PairFail, at: Step) {
        _ui.value = _ui.value.copy(phase = Phase.FAILED, fail = f, steps = _ui.value.steps + (at to StepState.FAILED))
    }
}
```

- [ ] **Step 5: Verifica che passino**

Run: `$GW :mobile:testDebugUnitTest --tests 'it.pixelbox.cmwatch.mobile.pair.*'`
Expected: PASS (10 test).

- [ ] **Step 6: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/Ports.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingController.kt mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/pair/PairingControllerTest.kt
git commit -m "feat(phone): pairing controller — phone, watch, PC, then the key to the watch, pending delivery and restarts"
```

---

### Task 14: Telefono, collegamenti Android e schermate

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/WearWatchLink.kt`, `SdkPhoneFirebase.kt`, `KeystoreKeyWrap.kt`, `PhoneListenerService.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/Device.kt` (scanner, riavvio, vibrazione)
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairingScreen.kt`, `PairedScreen.kt`, `PasteDialog.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt`, `MainActivity.kt` (intero), `mobile/src/main/AndroidManifest.xml`, `mobile/src/main/res/values/strings.xml`, `values-en/strings.xml`, `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt`

**Interfaces:**
- Consumes: tutto il Task 13; `FirebaseBoot`, `FirebaseAuthToken`, `KeyVault`, `Rtdb`, `PhonePairer`.
- Produces: `PhoneApp.pairing: PairingController`; `PairingScreen(ui, onRetry, onRescan, onWithoutWatch, onInstallOnWatch, onDone)`, `PairedScreen(host, phoneName, watchName, watchPending, onRepair)`, `PasteDialog(onPair, onDismiss)`.

- [ ] **Step 1: Testi**

Aggiungi a `mobile/src/main/res/values/strings.xml`:

```xml
    <string name="pair_title">Accoppiamento</string>
    <string name="pair_done_title">Fatto</string>
    <string name="step_phone">Telefono</string>
    <string name="step_watch">Orologio</string>
    <string name="step_pc">PC</string>
    <string name="step_watch_restarting">Orologio in riavvio</string>
    <string name="step_watch_pending">In attesa dell\'orologio</string>
    <string name="step_watch_skipped">Senza orologio</string>
    <string name="pair_retry">Riprova</string>
    <string name="pair_rescan">Leggi di nuovo il QR</string>
    <string name="pair_finish">Fine</string>
    <string name="pair_without_watch">Continua senza orologio</string>
    <string name="pair_install_watch">Installa sull\'orologio</string>
    <string name="pair_err_expired">Codice scaduto: rilancia relay pair.</string>
    <string name="pair_err_invalid">Codice non valido: rilancia relay pair.</string>
    <string name="pair_err_no_watch">Orologio non trovato: è collegato al telefono?</string>
    <string name="pair_err_watch_app">Sull\'orologio manca claude-master.</string>
    <string name="pair_err_network">Nessuna rete: riprova.</string>
    <string name="pair_err_pc">Il PC non ha confermato: controlla relay pair e riprova.</string>
    <string name="pair_err_watch">L\'orologio non ha risposto: riprova.</string>
    <string name="pair_err_watch_uid">L\'orologio è cambiato: rifai l\'accoppiamento.</string>
    <string name="paired_title">Collegato a %1$s</string>
    <string name="paired_this_phone">Questo telefono</string>
    <string name="paired_key_delivered">Chiave consegnata</string>
    <string name="paired_repair">Rifai l\'accoppiamento</string>
    <string name="paste_title">Incolla il codice di relay pair</string>
    <string name="cancel">Annulla</string>
```

E a `values-en/strings.xml`:

```xml
    <string name="pair_title">Pairing</string>
    <string name="pair_done_title">Done</string>
    <string name="step_phone">Phone</string>
    <string name="step_watch">Watch</string>
    <string name="step_pc">PC</string>
    <string name="step_watch_restarting">Watch restarting</string>
    <string name="step_watch_pending">Waiting for the watch</string>
    <string name="step_watch_skipped">No watch</string>
    <string name="pair_retry">Try again</string>
    <string name="pair_rescan">Scan the QR again</string>
    <string name="pair_finish">Done</string>
    <string name="pair_without_watch">Continue without a watch</string>
    <string name="pair_install_watch">Install on the watch</string>
    <string name="pair_err_expired">Code expired: run relay pair again.</string>
    <string name="pair_err_invalid">Invalid code: run relay pair again.</string>
    <string name="pair_err_no_watch">No watch found: is it connected to the phone?</string>
    <string name="pair_err_watch_app">claude-master is missing on the watch.</string>
    <string name="pair_err_network">No network: try again.</string>
    <string name="pair_err_pc">The PC did not confirm: check relay pair and try again.</string>
    <string name="pair_err_watch">The watch did not answer: try again.</string>
    <string name="pair_err_watch_uid">The watch has changed: pair again.</string>
    <string name="paired_title">Connected to %1$s</string>
    <string name="paired_this_phone">This phone</string>
    <string name="paired_key_delivered">Key delivered</string>
    <string name="paired_repair">Pair again</string>
    <string name="paste_title">Paste the relay pair code</string>
    <string name="cancel">Cancel</string>
```

- [ ] **Step 2: Snapshot che falliscono**

In `PhoneScreensTest.kt`, import `it.pixelbox.cmwatch.mobile.pair.*`, `it.pixelbox.cmwatch.mobile.ui.PairingScreen`, `it.pixelbox.cmwatch.mobile.ui.PairedScreen`; dentro la classe:

```kotlin
    private fun steps(p: StepState, w: StepState, c: StepState) = mapOf(Step.PHONE to p, Step.WATCH to w, Step.PC to c)

    @Test fun pairingRunning() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.RUNNING, steps(StepState.DONE, StepState.WORKING, StepState.WAIT)), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingNoWatch() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.FAILED, steps(StepState.DONE, StepState.FAILED, StepState.WAIT), fail = PairFail.NO_WATCH), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingDone() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.DONE, steps(StepState.DONE, StepState.DONE, StepState.DONE), host = "penguin", watchName = "Pixel Watch 5"), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingDoneWatchPending() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.DONE, steps(StepState.DONE, StepState.PENDING, StepState.DONE), host = "penguin", watchName = "Pixel Watch 5"), {}, {}, {}, {}, {}) }
    }
    @Test fun paired() = paparazzi.snapshot { CmPhoneTheme { PairedScreen("penguin", "Pixel 9", "Pixel Watch 5", watchPending = false, onRepair = {}) } }
    @Test fun pairedWatchPending() = paparazzi.snapshot { CmPhoneTheme { PairedScreen("penguin", "Pixel 9", "Pixel Watch 5", watchPending = true, onRepair = {}) } }
```

Run: `$GW :mobile:compileDebugUnitTestKotlin`
Expected: FAIL, `Unresolved reference 'PairingScreen'`.

- [ ] **Step 3: Schermate**

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairingScreen.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.ui

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.pair.PairFail
import it.pixelbox.cmwatch.mobile.pair.PairUi
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.pair.Step
import it.pixelbox.cmwatch.mobile.pair.StepState
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * I tre passi che si accendono uno dopo l'altro, l'errore sotto, un solo bottone pieno (design 24/09, schermata 3). Con un QR
 * scaduto, non valido o un orologio cambiato riprovare lo stesso QR non serve: si legge di nuovo.
 */
@Composable
fun PairingScreen(ui: PairUi, onRetry: () -> Unit, onRescan: () -> Unit, onWithoutWatch: () -> Unit, onInstallOnWatch: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(if (ui.phase == Phase.DONE) R.string.pair_done_title else R.string.pair_title), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        StepRow(stringResource(R.string.step_phone), null, ui.steps.getValue(Step.PHONE))
        StepRow(stringResource(R.string.step_watch), watchNote(ui), ui.steps.getValue(Step.WATCH))
        StepRow(stringResource(R.string.step_pc), ui.host, ui.steps.getValue(Step.PC))
        ui.fail?.let { Text(stringResource(failText(it)), style = MaterialTheme.typography.bodyLarge, color = CmColors.gone) }
        Spacer(Modifier.weight(1f))
        val big = Modifier.fillMaxWidth().height(56.dp)
        when {
            ui.phase == Phase.DONE -> Button(onDone, big) { Text(stringResource(R.string.pair_finish)) }
            ui.fail == PairFail.WATCH_APP_MISSING -> {
                Button(onInstallOnWatch, big) { Text(stringResource(R.string.pair_install_watch)) }
                OutlinedButton(onRetry, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_retry)) }
            }
            ui.fail == PairFail.NO_WATCH -> {
                Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
                OutlinedButton(onWithoutWatch, Modifier.fillMaxWidth()) { Text(stringResource(R.string.pair_without_watch)) }
            }
            ui.fail in RESCAN -> Button(onRescan, big) { Text(stringResource(R.string.pair_rescan)) }
            ui.fail != null -> Button(onRetry, big) { Text(stringResource(R.string.pair_retry)) }
        }
    }
}

private val RESCAN = setOf<PairFail?>(PairFail.EXPIRED, PairFail.INVALID, PairFail.WATCH_UID_CHANGED)

@Composable
private fun watchNote(ui: PairUi): String? = when {
    ui.restarting -> stringResource(R.string.step_watch_restarting)
    ui.steps[Step.WATCH] == StepState.PENDING -> stringResource(R.string.step_watch_pending)
    ui.steps[Step.WATCH] == StepState.SKIPPED -> stringResource(R.string.step_watch_skipped)
    else -> ui.watchName
}

@StringRes
private fun failText(f: PairFail): Int = when (f) {
    PairFail.EXPIRED -> R.string.pair_err_expired
    PairFail.INVALID -> R.string.pair_err_invalid
    PairFail.NO_WATCH -> R.string.pair_err_no_watch
    PairFail.WATCH_APP_MISSING -> R.string.pair_err_watch_app
    PairFail.NETWORK -> R.string.pair_err_network
    PairFail.PC_NO_CONFIRM -> R.string.pair_err_pc
    PairFail.WATCH_FAILED -> R.string.pair_err_watch
    PairFail.WATCH_UID_CHANGED -> R.string.pair_err_watch_uid
}

@Composable
private fun StepRow(label: String, note: String?, state: StepState) {
    val off = animationsOff()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Crossfade(targetState = state, animationSpec = CmMotion.spec(off), label = "passo") { s -> StepMark(s, off) }
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (state == StepState.WAIT) CmColors.text2 else CmColors.text)
            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = if (state == StepState.PENDING) CmColors.waiting else CmColors.text2) }
        }
    }
}

@Composable
private fun StepMark(s: StepState, off: Boolean) {
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        when (s) {
            StepState.WAIT -> Box(Modifier.size(12.dp).border(1.dp, CmColors.line, CircleShape))
            StepState.WORKING -> Breathing(off)
            StepState.DONE -> Icon(Icons.Rounded.CheckCircle, null, tint = CmColors.briefGood, modifier = Modifier.size(28.dp))
            StepState.PENDING -> Icon(Icons.Rounded.Schedule, null, tint = CmColors.waiting, modifier = Modifier.size(28.dp))
            StepState.SKIPPED -> Icon(Icons.Rounded.RemoveCircleOutline, null, tint = CmColors.text2, modifier = Modifier.size(28.dp))
            StepState.FAILED -> Icon(Icons.Rounded.Cancel, null, tint = CmColors.gone, modifier = Modifier.size(28.dp))
        }
    }
}

/** Il passo in corso respira, come il bagliore dell'orologio: l'unica animazione che si ripete (design 24/09). */
@Composable
private fun Breathing(off: Boolean) {
    val alpha = if (off) 0.7f else rememberInfiniteTransition(label = "respiro").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = CmMotion.easing), RepeatMode.Reverse), label = "luce",
    ).value
    Box(Modifier.size(12.dp).background(CmColors.accent.copy(alpha = alpha), CircleShape))
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairedScreen.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Accoppiato: il PC e i due dispositivi (design 24/09, schermata 4); la base della regia del pezzo 3. */
@Composable
fun PairedScreen(host: String, phoneName: String, watchName: String?, watchPending: Boolean, onRepair: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.paired_title, host), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        Surface(color = CmColors.surface, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DeviceRow(Icons.Rounded.PhoneAndroid, phoneName, stringResource(R.string.paired_this_phone), CmColors.text2)
                watchName?.let {
                    DeviceRow(Icons.Rounded.Watch, it, stringResource(if (watchPending) R.string.step_watch_pending else R.string.paired_key_delivered),
                        if (watchPending) CmColors.waiting else CmColors.text2)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onRepair, Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.paired_repair)) }
    }
}

@Composable
private fun DeviceRow(icon: ImageVector, name: String, note: String, noteColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = CmColors.actionIcon)
        Column {
            Text(name, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
            Text(note, style = MaterialTheme.typography.bodyMedium, color = noteColor)
        }
    }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PasteDialog.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «Incolla il codice»: il testo di `relay pair --text`, quando il QR non si legge. */
@Composable
fun PasteDialog(onPair: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CmColors.surfaceHigh,
        title = { Text(stringResource(R.string.paste_title)) },
        text = {
            OutlinedTextField(text, { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        },
        confirmButton = { Button({ onPair(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.pair_button)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
```

- [ ] **Step 4: Colla Android**

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/WearWatchLink.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class WearWatchLink(private val ctx: Context) : WatchLink {
    override suspend fun find(): WatchNode? = runCatching {
        Wearable.getCapabilityClient(ctx).getCapability(WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await()
            .nodes.firstOrNull()?.let { WatchNode(it.id, it.displayName) }
    }.getOrNull()

    override suspend fun anyConnected(): WatchNode? = runCatching {
        Wearable.getNodeClient(ctx).connectedNodes.await().firstOrNull()?.let { WatchNode(it.id, it.displayName) }
    }.getOrNull()

    override suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray =
        withTimeout(20_000) { Wearable.getMessageClient(ctx).sendRequest(node.id, path, body).await() }

    override suspend fun openPlayOnWatch(node: WatchNode): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}")).addCategory(Intent.CATEGORY_BROWSABLE)
        withContext(Dispatchers.IO) { RemoteActivityHelper(ctx).startRemoteActivity(intent, node.id).get(20, TimeUnit.SECONDS) }
        true
    }.getOrDefault(false)

    companion object { const val WATCH_CAPABILITY = "cmwatch_wear" }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/SdkPhoneFirebase.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import android.content.Context
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.transport.Rtdb

class SdkPhoneFirebase(private val ctx: Context) : PhoneFirebase {
    override suspend fun ensure(cfg: FirebaseConfig): Ensure {
        val active = FirebaseBoot.active
        if (active != null && !active.sameProject(cfg)) return Ensure.Restart
        if (active == null && FirebaseBoot.start(ctx, cfg) == null) return Ensure.Failed
        FirebaseAuthToken.token() ?: return Ensure.Failed
        return FirebaseAuthToken.uid()?.let { Ensure.Ready(it) } ?: Ensure.Failed
    }

    override fun rtdb(): Rtdb = Rtdb(FirebaseBoot.active!!.databaseUrl.removeSuffix("/"), token = { FirebaseAuthToken.token() })
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/KeystoreKeyWrap.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.crypto.KeyVault

object KeystoreKeyWrap : KeyWrap {
    override fun wrap(key: ByteArray): String = KeyVault.wrap(key, KeyVault.keystoreKek())
    override fun unwrap(wrapped: String): ByteArray = KeyVault.unwrap(wrapped, KeyVault.keystoreKek())
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/PhoneListenerService.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile.pair

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.WearableListenerService
import it.pixelbox.cmwatch.mobile.PhoneApp
import kotlinx.coroutines.launch

/** L'orologio torna raggiungibile: se gli manca ancora K, gliela si consegna anche ad app chiusa. */
class PhoneListenerService : WearableListenerService() {
    override fun onCapabilityChanged(info: CapabilityInfo) {
        if (info.nodes.isEmpty()) return
        val app = application as PhoneApp
        app.scope.launch { app.pairing.completePending() }
    }
}
```

`mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/Device.kt`:

```kotlin
package it.pixelbox.cmwatch.mobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.VibratorManager
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.tasks.await

/** Lo scanner dei servizi Google Play: niente permesso fotocamera. Null se l'utente annulla. */
object Scanner {
    suspend fun scan(ctx: Context): String? = runCatching {
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        GmsBarcodeScanning.getClient(ctx, options).startScan().await().rawValue
    }.getOrNull()
}

/** Da capo, con il QR salvato: Firebase si avvia una volta per processo, e il QR è di un altro progetto. */
object Restarter {
    fun restart(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

/** «Fatto» con la stessa vibrazione dell'orologio (`Haptics.Kind.CONFIRMED`). */
object Buzz {
    fun done(ctx: Context) {
        ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 60, 20), -1))
    }
}
```

In `PhoneApp`, aggiungi la proprietà e, in fondo a `onCreate`, la costruzione:

```kotlin
    lateinit var pairing: it.pixelbox.cmwatch.mobile.pair.PairingController
```

```kotlin
        pairing = it.pixelbox.cmwatch.mobile.pair.PairingController(
            store = prefs,
            firebase = it.pixelbox.cmwatch.mobile.pair.SdkPhoneFirebase(this),
            link = it.pixelbox.cmwatch.mobile.pair.WearWatchLink(this),
            keys = it.pixelbox.cmwatch.mobile.pair.KeystoreKeyWrap,
            pairer = { fb -> it.pixelbox.cmwatch.mobile.pair.PcPairer { qr, uid, name, w -> it.pixelbox.cmwatch.pairing.PhonePairer(fb.rtdb()).pair(qr, uid, name, w) } },
            phoneName = phoneName,
        )
```

(Nel codice vero usa gli import invece dei nomi completi; qui i nomi completi evitano ambiguità con `it`, il parametro implicito: dentro la lambda di `pairer` non usare `it`.)

In `mobile/src/main/AndroidManifest.xml`, dentro `<application>`:

```xml
        <service android:name=".pair.PhoneListenerService" android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.CAPABILITY_CHANGED" />
                <data android:scheme="wear" android:host="*" android:path="/cmwatch_wear" />
            </intent-filter>
        </service>
```

- [ ] **Step 5: `MainActivity` completa**

```kotlin
package it.pixelbox.cmwatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.pairing.PairingRecord
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as PhoneApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CmPhoneTheme {
                val settings by app.prefs.flow.collectAsStateWithLifecycle(initialValue = null)
                val ui by app.pairing.ui.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var paste by remember { mutableStateOf(false) }
                // Dopo un riavvio per un altro progetto Firebase si riprende il QR salvato.
                LaunchedEffect(Unit) { app.prefs.current().resumeQr?.let { app.pairing.run(it) } }
                LaunchedEffect(ui.phase) {
                    when (ui.phase) {
                        Phase.RESTART -> Restarter.restart(this@MainActivity)
                        Phase.DONE -> Buzz.done(this@MainActivity)
                        else -> Unit
                    }
                }
                fun scan() { scope.launch { Scanner.scan(this@MainActivity)?.let { app.pairing.run(it) } } }
                // Indietro da un errore: si torna alla schermata di prima, niente resta a metà.
                BackHandler(enabled = ui.phase == Phase.FAILED) { app.pairing.reset() }
                val s = settings ?: return@CmPhoneTheme
                when {
                    ui.phase != Phase.IDLE -> PairingScreen(
                        ui,
                        onRetry = { scope.launch { app.pairing.retry() } },
                        onRescan = ::scan,
                        onWithoutWatch = { scope.launch { app.pairing.retry(withoutWatch = true) } },
                        onInstallOnWatch = { scope.launch { app.pairing.installOnWatch() } },
                        onDone = { app.pairing.reset() },
                    )
                    s.paired -> {
                        val r = PairingRecord.fromJson(s.pairingJson)
                        PairedScreen(s.host.orEmpty(), app.phoneName, r?.watchName, r?.watchPending == true, onRepair = ::scan)
                    }
                    else -> NotPairedScreen(onPair = ::scan, onPaste = { paste = true })
                }
                if (paste) PasteDialog(onPair = { t -> paste = false; scope.launch { app.pairing.run(t) } }, onDismiss = { paste = false })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.scope.launch { app.pairing.completePending() }
    }
}
```

- [ ] **Step 6: Test e build**

Run: `$GW :mobile:testDebugUnitTest :mobile:assembleDebug :mobile:compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL, i 10 test del controller verdi.

- [ ] **Step 7: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/WearWatchLink.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/SdkPhoneFirebase.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/KeystoreKeyWrap.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/pair/PhoneListenerService.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/Device.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairingScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairedScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PasteDialog.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/AndroidManifest.xml mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt
git commit -m "feat(phone): QR scanner, watch link, pairing and paired screens, key delivery when the watch comes back"
```

---

### Task 15: Screenshot in CI

**Files:**
- Create/Modify: `wear/src/test/snapshots/images/*pairing*.png`, `mobile/src/test/snapshots/images/*.png`

- [ ] **Step 1: Ok di Franz per il push**

Una riga a Franz: «Pusho `feature/fondamenta` su origin (pubblico, frsorrentino/claude-master-watch): N commit (lista), per registrare gli screenshot in CI; poi un secondo push con le immagini. Va bene?» Aspetta l'ok.

- [ ] **Step 2: Push e registrazione**

```bash
git push -u origin feature/fondamenta
gh workflow run build-android.yml -R frsorrentino/claude-master-watch --ref feature/fondamenta -f record=true
sleep 30
RUN=$(gh run list -R frsorrentino/claude-master-watch --branch feature/fondamenta --workflow build-android.yml --event workflow_dispatch --limit 1 --json databaseId -q '.[0].databaseId')
gh run watch $RUN -R frsorrentino/claude-master-watch --exit-status
gh run download $RUN -R frsorrentino/claude-master-watch -n paparazzi-snapshots -D $SCR/snap
```

Expected: run verde; in `$SCR/snap` le cartelle `wear/src/test/snapshots` e `mobile/src/test/snapshots`.

- [ ] **Step 3: Solo la schermata d'accoppiamento cambia sull'orologio**

```bash
cp -r $SCR/snap/wear/src/test/snapshots/. wear/src/test/snapshots/
mkdir -p mobile/src/test/snapshots && cp -r $SCR/snap/mobile/src/test/snapshots/. mobile/src/test/snapshots/
git status --short wear/src/test/snapshots mobile/src/test/snapshots
```

Expected: fra quelli dell'orologio, modificato solo `…ScreensSnapshotTest_pairing.png` e nuovo `…_pairingPhoneOnly.png`; nel telefono, sette immagini nuove. Qualunque altra immagine dell'orologio modificata vuol dire che lo spostamento dei colori (Task 8) ha cambiato qualcosa: fermati e confronta prima e dopo.

- [ ] **Step 4: Franz guarda le schermate nuove**

Manda con `SendUserFile` le due dell'orologio e le sette del telefono. Correggi quello che chiede (ripeti dallo Step 2 per le schermate toccate) e prosegui solo con il suo ok.

- [ ] **Step 5: Commit e verifica**

```bash
git add $(git status --short wear/src/test/snapshots mobile/src/test/snapshots | awk '{print $2}')
git commit -m "test: pairing screens recorded on watch and phone"
git push origin feature/fondamenta
```

Expected: il run del push è verde, compresi `Screenshot tests (Paparazzi, verify)` e `Screenshot tests del telefono (Paparazzi, verify)`.

---

### Task 16: R1 consegnata: fixture identiche, README del contratto, QR vero

Parte quando claude-master risponde al Task 4.

**Files:**
- Modify: `contract/README.md`

- [ ] **Step 1: Fixture identiche**

```bash
CM=~/…/claude-master
cmp contract/pair-qr.json $CM/tests/fixtures/relay/pair-qr.json && cmp contract/pair-response.json $CM/tests/fixtures/relay/pair-response.json && echo identiche
```

Expected: `identiche`. Altrimenti scrivi a claude-master quale byte differisce e aspetta.

- [ ] **Step 2: Il QR vero è leggibile dalla regola dell'app**

Nessun dispositivo risponde, quindi `/allowed` non si tocca: il relay aspetta, scade dopo 15 s e cancella il nodo.

```bash
timeout 25 claude-master relay pair --text --timeout 15 > $SCR/qr.txt; echo "uscita $?"
python3 - "$SCR/qr.txt" <<'EOF'
import base64, json, re, sys
line = next(l for l in open(sys.argv[1]) if l.strip().startswith("{"))
q = json.loads(line)
assert q["v"] == 1 and re.fullmatch(r"[A-Za-z0-9_-]{22}", q["i"]), q
assert len(base64.b64decode(q["c"])) == 32
f = q["f"]; assert all(f[k] for k in "kpat") and f["d"].startswith("https://"), f
print("QR valido per PairQr.parse:", q["h"], f["p"])
EOF
```

Expected: `uscita 3` (scaduto, come previsto) e `QR valido per PairQr.parse: <host> <progetto>`.

- [ ] **Step 3: README del contratto**

In `contract/README.md`, dopo il paragrafo «Contratto 1.14 …»:

```markdown
Contratto 1.15 (24/09/2026, solo aggiunte, richiesta dell'app approvata da Franz; design `docs/plans/2026-09-24-app-telefono-fondamenta-design.md`): accoppiamento dal telefono. `relay pair` mostra un QR con il JSON di `pair-qr.json` (id di 22 caratteri base64url, `pc_pub`, host, scadenza, configurazione Firebase con chiavi brevi) e, sotto, il codice a 6 cifre: stesso documento in `/pair/<id>` e in `/pair/<code>`, vince la prima risposta valida. La risposta in `/pair/<…>/watch` può portare `uids` e `names` (`pair-response.json`, con i vettori: PC = scalare 0..31, telefono = 32..63); `/allowed` riceve tutti gli uid. I controlli HMAC usano la stringa del nodo (`id` e `id + ":pc"`). `v` resta 1.
```

E nell'elenco dei file, dopo `cmd-result-sample.json`:

```markdown
- `pair-qr.json` — il contenuto del QR di `relay pair` (1.15).
- `pair-response.json` — la risposta del telefono per sé e per l'orologio, e la conferma del PC (1.15).
```

- [ ] **Step 4: Commit**

```bash
git add contract/README.md
git commit -m "docs(contract): 1.15 — pairing from the phone, QR payload and two-uid answer"
```

---

### Task 17: Richiesta R2 a claude-master

Solo dopo che R1 è chiusa (Task 16).

- [ ] **Step 1: Manda la richiesta**

```
Richiesta da claude-master-watch (una sola, dopo R1; design docs/plans/2026-09-24-app-telefono-fondamenta-design.md, R2): `claude-master relay setup`. Guidato, idempotente, con --dry-run. Crea un progetto Firebase o ne sceglie uno esistente; crea l'istanza del Realtime Database e ci mette le regole del README (/state, /events, /result leggibili solo da un uid in /allowed; /cmd scrivibile solo da loro; /pair/<code>/watch scrivibile dall'accesso anonimo); attiva l'accesso anonimo; registra l'app Android it.pixelbox.cmwatch senza impronta SHA e ne salva i dati in relay.firebase_app; scarica la chiave del service account nel percorso relay.service_account con permessi 0600; scrive relay.firebase_url e relay.fcm_topic nella configurazione in uso, anche con CLAUDE_MASTER_CONFIG. La chiave API dell'app non va limitata a un'impronta SHA: la firma di Play sarà diversa da quella di sviluppo. Dove un passo non si può automatizzare, stampa il link esatto della console e aspetta Invio; quali passi si possono automatizzare lo verifichi tu (CLI di Firebase, gcloud o API REST). Alla fine un riepilogo come doctor. Prova su un progetto nuovo con una configurazione di prova, senza toccare quella principale. Quando è fatto, scrivimi commit, versione del plugin e i passi rimasti manuali.
```

- [ ] **Step 2: Aspetta con un avviso armato**

`SendMessage` con `notify_when_idle: true` o `claude-master wait claude-master`.

---

### Task 18: Checklist dal vivo

Serve Franz; serve R2 (per il Firebase nuovo) oppure un progetto di prova creato a mano da Franz.

**Files:**
- Create: `docs/verifiche/fondamenta-accoppiamento.md`

- [ ] **Step 1: La checklist**

`docs/verifiche/fondamenta-accoppiamento.md`:

```markdown
# Verifica dal vivo: accoppiamento dal telefono (fondamenta)

Design `docs/plans/2026-09-24-app-telefono-fondamenta-design.md`, piano `docs/plans/2026-09-24-app-telefono-fondamenta-piano.md`.
Relay di prova: `CLAUDE_MASTER_CONFIG=<configurazione di prova>`, mai quella principale finché Franz non lo decide.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| A1 | `relay setup` crea un Firebase nuovo senza console, salvo i passi che dichiara manuali | configurazione di prova, progetto nuovo | |
| A2 | QR letto, tre passi accesi uno dopo l'altro, «Fatto» con la vibrazione; l'orologio mostra «Accoppiato con \<PC\>» | `relay pair`, telefono, «Accoppia» | |
| A3 | Un push sveglia l'orologio | una sessione che cambia stato, o il push di prova del Task 1 | |
| A4 | Un comando dall'orologio va a buon fine | Scheda, un'azione qualsiasi | |
| A5 | Rifare l'accoppiamento esclude i dispositivi vecchi | un secondo accoppiamento, poi `/allowed` | |
| A6 | Orologio spento dopo la conferma del PC: telefono «In attesa dell'orologio», poi «Chiave consegnata» alla riaccensione | spegnere l'orologio al passo PC | |
| A7 | La build con la configurazione dentro si accoppia ancora con il codice a 6 cifre | release di sviluppo, «Usa il codice» | |
| A8 | Con «riduci animazioni» niente movimento, tutto al suo stato finale | impostazioni di accessibilità del telefono | |
| A9 | QR scaduto, QR di un'altra app, testo incollato sbagliato: messaggio giusto, niente attese | i tre casi | |
```

- [ ] **Step 2: Decisione sui dispositivi**

Chiedi a Franz, in una riga, dove si prova: sul suo telefono e sul suo orologio, installando la build di sviluppo sopra la release (stessa firma, la configurazione dentro la build la tiene accoppiata al relay principale finché non accoppia il telefono con il relay di prova), oppure no. Senza il suo ok non installare niente sul polso.

- [ ] **Step 3: Esegui e annota**

Una riga per voce in «Esito», con ora e build. Un ✗ ha la sua causa e la sua correzione nello stesso file, come in `fase-5-lettura.md`.

- [ ] **Step 4: Commit**

```bash
git add docs/verifiche/fondamenta-accoppiamento.md
git commit -m "docs: live checklist for pairing from the phone"
```

Le fondamenta sono fatte quando A1-A4 sono ✅ (il «Fatto» della specifica) e gli altri non hanno ✗ aperti.

## Risultati dello spike

**A e B, 24/09/2026 20:14, emulatore `emulator-5554` (immagine Wear, Android 13), build `:wear:assembleDebug` senza
`google-services.json` e con `FirebaseInitProvider` rimosso dal manifest.** Configurazione del progetto di Franz iniettata
via `adb` e salvata nelle preferenze; Firebase avviato in `CmApp.onCreate` con `FirebaseOptions.Builder`.

- **A: sì.** Accesso anonimo, iscrizione al topic e messaggio FCM del relay ricevuti:

  ```
  20:14:11.811 I cmwatch : spike: firebase https://claude-master-relay-3761-default-rtdb.europe-west1.firebasedatabase.app
  20:14:14.314 I cmwatch : spike: token true uid rQNQH9ZGVhMRQVoZhLsmk25IEJh2
  20:14:16.870 I cmwatch : spike: topic true
  20:14:49.023 I cmwatch : fcm message: {kind=spike, session=spike}
  ```

  Il push è partito da `fcm_send` del relay (`True`) alle 20:14:28 e ha svegliato il processo in 21 s.
- **B: sì.** Configurazione con l'URL del database che finisce in `/`, salvata, processo fermato, riavviato: il processo
  nuovo parte con la configurazione nuova e l'uid anonimo resta lo stesso (stesso progetto):

  ```
  20:15:23.061 I cmwatch : spike: firebase https://claude-master-relay-3761-default-rtdb.europe-west1.firebasedatabase.app/
  20:15:23.380 I cmwatch : spike: token true uid rQNQH9ZGVhMRQVoZhLsmk25IEJh2
  20:15:24.112 I cmwatch : spike: topic true
  ```

- **Decisione:** la configurazione a runtime della specifica regge. Basta riavviare il processo, non serve reinizializzare
  Firebase a caldo.
- **Due cose imparate, da tenere nei Task 7 e 10:**
  - `Process.killProcess` dentro l'activity `singleTask` fa rilanciare l'activity con lo stesso intent (11 riavvii in 15 s):
    nel prodotto il riavvio dopo `hello` con progetto diverso deve chiudere l'activity (`finishAndRemoveTask`) prima di
    uccidere il processo, e comunque non dipendere da un intent con extra.
  - L'emulatore Wear di questa macchina non ha `com.google.wear.services.ambient.AmbientComponentState`: `MainActivity`
    ci va in crash (preesistente, non c'entra con Firebase). Per lo spike l'activity si è chiusa prima di `setContent`;
    le prove di schermata dell'orologio si fanno sul polso o con Paparazzi.

**C, 25/09/2026 07:18, telefono Pixel 11 Pro XL (Android 17) e Pixel Watch 5 di Franz (Android 17), app di prova
`it.pixelbox.cmwatch.spike` su tutti e due (debug, stessa firma), release dell'orologio non toccata.** Sì: il telefono trova
l'orologio per capacità `cmwatch_wear` e la richiesta `sendRequest` arriva al `WearableListenerService` dell'orologio con
l'app chiusa (il servizio parte da sé):

```
telefono  07:18:36.473 I cmwatch : spike: nodes [Pixel Watch 5]
orologio  07:18:46.204 I cmwatch : spike: request /spike/ping from 74c779f1
telefono  07:18:47.082 I cmwatch : spike: reply pong:hi in 10504 ms
```

La prima risposta arriva in 10,5 s (avvio del servizio più canale Bluetooth): sotto il timeout di 20 s del design, ma da
tenere presente nella schermata dell'accoppiamento (il passo «Orologio» respira per qualche secondo). App di prova
disinstallate da tutti e due alle 07:20.
