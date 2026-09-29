# App del telefono, pezzo 3, piano A: l'app sul contratto attuale

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** l'app del telefono diventa l'app vera (regia, scheda sessione, terminale, lancio, diario, impostazioni,
notifiche, Demo), tutta sul contratto di oggi, senza richieste a claude-master.

**Architecture:** il telefono riusa da `core` `Repo`, `SwitchableTransport`, `FirebaseTransport`, `FakeTransport`,
`RoomStore`, `Wake` e le regole di testo, come l'orologio in `CmApp`. Le regole nuove sono funzioni pure in `core`
(`rules/Phone*.kt`), provate con TDD; `mobile` aggiunge schermate Compose Material 3, FCM e il canale con l'orologio.
La Demo è il `FakeTransport` scelto da `TransportChoice` con `demoMode`.

**Tech Stack:** Kotlin, Compose Material 3 (telefono), Room (da `core`), Firebase RTDB via REST (`Rtdb`), FCM,
`play-services-wearable`, Paparazzi, JUnit 4.

**Spec:** `docs/plans/2026-09-29-telefono-pezzo-3-design.md` (con `2026-09-24-app-telefono-fondamenta-design.md`,
sezione «Aspetto e movimento»).

**Piano B** (dopo questo, uno per richiesta): R3 coda della notte modificabile, R4 resoconto della notte e storico del
diario, R5 «Condividi», R6 telefono fra i dispositivi che ricevono. Si scrive quando claude-master ha fissato la forma di
R3 nel contratto.

## Global Constraints

- `minSdk = 33`, `compileSdk = 36`, `targetSdk = 36`, JDK 17; `org.gradle.jvmargs=-Xmx2048m`, un solo daemon.
- applicationId `com.francescosorrentino.cmaster` su `mobile` e `wear`; namespace e package Kotlin restano `it.pixelbox.cmwatch`.
- Tema solo scuro, colori solo da `it.pixelbox.cmwatch.ui.tokens.CmColors`; niente colori dinamici.
- Un solo bottone pieno per schermata; bottone pieno azzurro pastello con testo blu notte (`CmColors`).
- Una riga logica su una riga fisica; mai «…» nel corpo dei testi.
- Icone di stato ❓ ▶ ✓ ✗ con i significati dell'orologio: ambra aspetta te, cobalto lavora, grigio ferma, rosso spento chiusa.
- Tasto ▶ (lettura a voce) accanto a ogni testo sopra `tts.min_chars` (120) e a esiti, risposte, domande.
- Testi solo in `mobile/src/main/res/values/strings.xml` (italiano) e `values-en/strings.xml` (inglese), mai nel Kotlin.
- Movimento 200-350 ms, easing `cubic-bezier(0.3, 0, 0.2, 1)`, niente cicli tranne il respiro di «lavora»; con
  «riduci animazioni» lo stato finale subito.
- Commit in inglese (`feat(phone): …`, `test: …`, `docs: …`), `git add` solo per nome di file, mai `-A`.
- Nessun keystore, `google-services.json`, chiave o token nel repo.
- Paparazzi si registra in GitHub Actions (`./gradlew :mobile:recordPaparazziDebug`), non in locale.

## Review Focus

1. **Due account con lo stesso nome di progetto:** le card restano distinte per `id`, e il pallino dice l'account. Test
   in Task 2.
2. **Domanda chiusa sul PC mentre il telefono ha il campo di testo pieno:** la scheda non manda la risposta a una domanda
   che non c'è più; mostra lo stato nuovo e tiene il testo. Test in Task 3 (`PhonePrimary`).
3. **Quota senza `reset_h5` o con `stale = true`:** nessuna ora inventata; «dato vecchio» al posto dell'ora. Test in Task 2.
4. **Orologio accoppiato ma spento o lontano:** il telefono suona. Test in Task 4 (`PhoneAlert`).
5. **Demo accesa con un accoppiamento vero salvato:** spegnendo la Demo torna Firebase senza rifare l'accoppiamento.
   Test in Task 1 (`TransportChoice`, caso già coperto: si aggiunge l'asserzione per il ritorno).

---

### Task 1: Il telefono ha Repo, Transport e Demo

**Files:**
- Move: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/DemoText.kt` → `core/src/main/kotlin/it/pixelbox/cmwatch/transport/DemoText.kt`
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt` (import di `DemoText`)
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt`
- Modify: `mobile/build.gradle.kts` (aggiunge `libs.lifecycle.process`)
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/TransportChoiceTest.kt`

**Interfaces:**
- Produces: `PhoneApp.transport: SwitchableTransport`, `PhoneApp.repo: Repo`, `PhoneApp.fake: FakeTransport`,
  `PhoneApp.setDemo(on: Boolean)`, `PhoneApp.isOnline(): Boolean`; `it.pixelbox.cmwatch.transport.DemoText.dress(json: String): String`.

- [ ] **Step 1: Test del ritorno dalla Demo**

In `TransportChoiceTest.kt` aggiungi:

```kotlin
@Test fun demoOffWithPairingGoesBackToFirebase() {
    assertEquals(TransportChoice.Kind.FAKE, TransportChoice.pick(paired = true, hasKey = true, firebase = true, demo = true))
    assertEquals(TransportChoice.Kind.FIREBASE, TransportChoice.pick(paired = true, hasKey = true, firebase = true, demo = false))
}
```

- [ ] **Step 2: Esegui**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.TransportChoiceTest'`
Expected: PASS (la regola c'è già; il test fissa il comportamento per il telefono). Se fallisce, fermati: la regola
dell'orologio è cambiata e va capito perché prima di proseguire.

- [ ] **Step 3: Sposta `DemoText` in `core`**

```bash
git mv wear/src/main/kotlin/it/pixelbox/cmwatch/wear/DemoText.kt core/src/main/kotlin/it/pixelbox/cmwatch/transport/DemoText.kt
sed -i 's/^package it.pixelbox.cmwatch.wear$/package it.pixelbox.cmwatch.transport/' core/src/main/kotlin/it/pixelbox/cmwatch/transport/DemoText.kt
grep -rl 'DemoText' wear/src | xargs sed -i 's/^import it.pixelbox.cmwatch.wear.DemoText$/import it.pixelbox.cmwatch.transport.DemoText/'
```

Nei file di `wear` che usavano `DemoText` senza import (stesso package), aggiungi
`import it.pixelbox.cmwatch.transport.DemoText`. Verifica con `./gradlew :wear:compileDebugKotlin`.

- [ ] **Step 4: `PhoneApp` con Repo, Transport e Demo**

In `PhoneApp.kt`, dopo `pairing = PairingController(…)`:

```kotlin
lateinit var transport: SwitchableTransport
lateinit var repo: Repo
val fake: FakeTransport by lazy { FakeTransport(load = { DemoText.dress(assets.open("contract/$it.json").bufferedReader().readText()) }) }

// in onCreate, dopo pairing:
val settings = runBlocking { prefs.current() }
transport = SwitchableTransport(choose(settings))
repo = Repo(RoomStore.open(this), transport, scope, { System.currentTimeMillis() / 1000 }, ::isOnline, phoneName)
repo.start(live = false)
ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = repo.live(true)
    override fun onStop(owner: LifecycleOwner) = repo.live(false)
})
```

e, come in `CmApp`:

```kotlin
fun choose(settings: Settings): Transport {
    val key = settings.wrappedKey?.let { runCatching { KeyVault.unwrap(it, KeyVault.keystoreKek()) }.getOrNull() }
    val fb = FirebaseBoot.active
    return when (TransportChoice.pick(settings.paired, key != null, fb != null, demo = settings.demoMode)) {
        TransportChoice.Kind.FAKE -> fake
        TransportChoice.Kind.FIREBASE -> FirebaseTransport(
            rtdb = Rtdb(fb!!.databaseUrl.removeSuffix("/"), token = { FirebaseAuthToken.token() }),
            key = { key }, uid = { FirebaseAuthToken.uid() }, deviceKeyPair = { Pairing.newKeyPair() },
            now = { System.currentTimeMillis() / 1000 },
        )
    }
}

fun setDemo(on: Boolean) {
    scope.launch {
        prefs.update { it.copy(demoMode = on) }
        transport.switchTo(choose(prefs.current()))
        if (on) repo.seedQuotaSamples(fake.demoQuotaSamples()) else repo.refresh()
    }
}

/** Dopo l'accoppiamento o il suo rifacimento: il Transport cambia a caldo. */
fun reconfigure() { scope.launch { transport.switchTo(choose(prefs.current())) } }

fun isOnline(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java)
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
```

Il telefono tiene la chiave AES dell'accoppiamento come l'orologio: verifica che `PairingController` salvi
`wrappedKey` in `Prefs` (`grep -n wrappedKey mobile/src/main/kotlin -r`). Se la salva sotto un altro nome, `choose`
legge quello; non cambiare il formato. In `MainActivity`, quando `ui.phase` diventa `Phase.DONE`, chiama
`app.reconfigure()`.

- [ ] **Step 5: Test e build**

Run: `./gradlew :core:testDebugUnitTest :wear:testDebugUnitTest :mobile:testDebugUnitTest :mobile:assembleDebug`
Expected: BUILD SUCCESSFUL, test verdi.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/transport/DemoText.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/TransportChoiceTest.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/build.gradle.kts
git add wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt   # più ogni file di wear toccato dallo Step 3, per nome
git commit -m "feat(phone): Repo, switchable transport and Demo on the phone, DemoText moves to core"
```

---

### Task 2: La regia in `core` (`PhoneBoard`)

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneBoard.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneBoardTest.kt`

**Interfaces:**
- Consumes: `Order.sessions`, `Accounts.isPersonalQuota`, `QuotaBar.of`, `SessionState`.
- Produces:
  ```kotlin
  object PhoneBoard {
      enum class Group { WAITING, WORKING, IDLE, CLOSED }
      data class Section(val group: Group, val sessions: List<Session>)
      data class QuotaRow(val account: String, val personal: Boolean, val pct: Int?, val resetAt: Long?, val stale: Boolean)
      fun sections(state: State): List<Section>          // gruppi non vuoti, in ordine; sessioni per Order.sessions
      fun quotaRows(state: State): List<QuotaRow>        // personale prima, poi alfabetico
  }
  ```

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

class PhoneBoardTest {
    private fun s(id: String, name: String, st: SessionState, account: String = "personale") =
        Session(id = id, name = name, account = account, project = "p", state = st, since = 0)

    private val state = State(v = 1, ts = 100, host = "pc", sessions = listOf(
        s("1", "docs", SessionState.IDLE), s("2", "kb", SessionState.BUSY), s("3", "ledger", SessionState.WAITING),
        s("4", "old", SessionState.GONE), s("5", "watch", SessionState.AWAITING), s("6", "kb", SessionState.BUSY, account = "lavoro"),
    ), quota = mapOf(
        "lavoro" to QuotaAccount(h5 = 18, resetH5 = 500, kind = "work"),
        "personale" to QuotaAccount(h5 = 62, resetH5 = 400, kind = "personal"),
    ))

    @Test fun groupsInOrderAwaitingCountsAsWorking() {
        val g = PhoneBoard.sections(state)
        assertEquals(listOf(PhoneBoard.Group.WAITING, PhoneBoard.Group.WORKING, PhoneBoard.Group.IDLE, PhoneBoard.Group.CLOSED), g.map { it.group })
        assertEquals(listOf("2", "6", "5"), g[1].sessions.map { it.id })
    }

    @Test fun sameNameOnTwoAccountsStaysTwoCards() {
        val working = PhoneBoard.sections(state).first { it.group == PhoneBoard.Group.WORKING }
        assertEquals(2, working.sessions.count { it.name == "kb" })
    }

    @Test fun emptyGroupsAreLeftOut() {
        val only = state.copy(sessions = listOf(s("1", "docs", SessionState.IDLE)))
        assertEquals(listOf(PhoneBoard.Group.IDLE), PhoneBoard.sections(only).map { it.group })
    }

    @Test fun quotaPersonalFirst() {
        val q = PhoneBoard.quotaRows(state)
        assertEquals(listOf("personale", "lavoro"), q.map { it.account })
        assertEquals(PhoneBoard.QuotaRow("personale", true, 62, 400, false), q[0])
    }

    @Test fun staleOrMissingResetInventsNoTime() {
        val q = PhoneBoard.quotaRows(state.copy(quota = mapOf("personale" to QuotaAccount(h5 = 40, stale = true))))
        assertNull(q[0].resetAt); assertTrue(q[0].stale)
    }
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhoneBoardTest'`
Expected: FAIL, `Unresolved reference: PhoneBoard`.

- [ ] **Step 3: Implementazione**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*

/** La regia del telefono (design 29/09): gruppi di card e righe di quota, dallo stato. */
object PhoneBoard {
    enum class Group { WAITING, WORKING, IDLE, CLOSED }
    data class Section(val group: Group, val sessions: List<Session>)
    data class QuotaRow(val account: String, val personal: Boolean, val pct: Int?, val resetAt: Long?, val stale: Boolean)

    private fun group(s: SessionState) = when (s) {
        SessionState.WAITING -> Group.WAITING
        SessionState.BUSY, SessionState.AWAITING -> Group.WORKING
        SessionState.IDLE -> Group.IDLE
        SessionState.GONE -> Group.CLOSED
    }

    fun sections(state: State): List<Section> {
        val byGroup = Order.sessions(state.sessions).groupBy { group(it.state) }
        return Group.entries.mapNotNull { g -> byGroup[g]?.let { Section(g, it) } }
    }

    fun quotaRows(state: State): List<QuotaRow> =
        state.quota.map { (name, q) ->
            QuotaRow(name, Accounts.isPersonalQuota(name, q), q.h5, if (q.stale) null else q.resetH5, q.stale)
        }.sortedWith(compareBy<QuotaRow> { !it.personal }.thenBy { it.account })
}
```

- [ ] **Step 4: Verifica che passino**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhoneBoardTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneBoard.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneBoardTest.kt
git commit -m "feat(core): phone board — card groups in order and quota rows, personal first"
```

---

### Task 3: Il bottone pieno della scheda (`PhonePrimary`)

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhonePrimary.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhonePrimaryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object PhonePrimary {
      enum class Button { SEND, REOPEN, NONE }
      enum class Target { ANSWER_TEXT, PROMPT }
      fun button(s: Session, draft: String): Button
      /** Dove va il testo scritto: risposta libera se c'è ancora una domanda, altrimenti prompt; null se non si può mandare. */
      fun target(s: Session, draft: String): Target?
  }
  ```

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

class PhonePrimaryTest {
    private val q = Question("q1", QuestionKind.ASK, "Quale?", listOf(Option(1, "A")), Tier.MEDIUM, 0)
    private fun s(st: SessionState, question: Question? = null) =
        Session(id = "1", name = "kb", account = "personale", project = "p", state = st, since = 0, question = question)

    @Test fun emptyDraftNoButtonExceptClosed() {
        assertEquals(PhonePrimary.Button.NONE, PhonePrimary.button(s(SessionState.IDLE), "  "))
        assertEquals(PhonePrimary.Button.REOPEN, PhonePrimary.button(s(SessionState.GONE), ""))
    }

    @Test fun draftGivesSend() = assertEquals(PhonePrimary.Button.SEND, PhonePrimary.button(s(SessionState.IDLE), "vai"))

    @Test fun closedSessionNeverSends() {
        assertEquals(PhonePrimary.Button.REOPEN, PhonePrimary.button(s(SessionState.GONE), "vai"))
        assertNull(PhonePrimary.target(s(SessionState.GONE), "vai"))
    }

    @Test fun textGoesToAnswerOnlyWhileQuestionExists() {
        assertEquals(PhonePrimary.Target.ANSWER_TEXT, PhonePrimary.target(s(SessionState.WAITING, q), "B"))
        // La domanda si è chiusa sul PC mentre il campo era pieno: il testo diventa un prompt, non una risposta a vuoto.
        assertEquals(PhonePrimary.Target.PROMPT, PhonePrimary.target(s(SessionState.IDLE), "B"))
    }
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhonePrimaryTest'`
Expected: FAIL, `Unresolved reference: PhonePrimary`.

- [ ] **Step 3: Implementazione**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/** Il solo bottone pieno della scheda sessione (design 29/09) e dove va il testo scritto. */
object PhonePrimary {
    enum class Button { SEND, REOPEN, NONE }
    enum class Target { ANSWER_TEXT, PROMPT }

    fun button(s: Session, draft: String): Button = when {
        s.state == SessionState.GONE -> Button.REOPEN
        draft.isNotBlank() -> Button.SEND
        else -> Button.NONE
    }

    fun target(s: Session, draft: String): Target? = when {
        s.state == SessionState.GONE || draft.isBlank() -> null
        s.question != null -> Target.ANSWER_TEXT
        else -> Target.PROMPT
    }
}
```

- [ ] **Step 4: Verifica che passino**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhonePrimaryTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhonePrimary.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhonePrimaryTest.kt
git commit -m "feat(core): the one filled button of the phone session sheet, and where typed text goes"
```

---

### Task 4: Suono o silenzio (`PhoneAlert`)

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneAlert.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneAlertTest.kt`

**Interfaces:**
- Consumes: `Wake.NotifyKind` (QUESTION, OUTCOME, GONE, QUOTA).
- Produces:
  ```kotlin
  object PhoneAlert {
      enum class Mode { SOUND, SILENT }
      /** watchPaired: c'è un orologio nel PairingRecord; watchReachable: il suo nodo risponde ora. */
      fun mode(kind: Wake.NotifyKind, watchPaired: Boolean, watchReachable: Boolean): Mode
  }
  ```

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneAlertTest {
    @Test fun watchFirstWhenReachable() =
        assertEquals(PhoneAlert.Mode.SILENT, PhoneAlert.mode(Wake.NotifyKind.QUESTION, watchPaired = true, watchReachable = true))

    @Test fun soundsWhenWatchOffOrAway() =
        assertEquals(PhoneAlert.Mode.SOUND, PhoneAlert.mode(Wake.NotifyKind.QUESTION, watchPaired = true, watchReachable = false))

    @Test fun soundsWithoutWatch() =
        assertEquals(PhoneAlert.Mode.SOUND, PhoneAlert.mode(Wake.NotifyKind.OUTCOME, watchPaired = false, watchReachable = false))

    @Test fun quotaIsAlwaysSilent() =
        assertEquals(PhoneAlert.Mode.SILENT, PhoneAlert.mode(Wake.NotifyKind.QUOTA, watchPaired = false, watchReachable = false))
}
```

- [ ] **Step 2: Verifica che falliscano**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhoneAlertTest'`
Expected: FAIL, `Unresolved reference: PhoneAlert`.

- [ ] **Step 3: Implementazione**

```kotlin
package it.pixelbox.cmwatch.rules

/**
 * Chi avvisa (Franz, 29/09): l'orologio per primo. Con l'orologio accoppiato e raggiungibile il telefono mostra in
 * silenzio; altrimenti suona. Gli avvisi di quota, come diario e notte, sono sempre silenziosi sul telefono.
 */
object PhoneAlert {
    enum class Mode { SOUND, SILENT }

    fun mode(kind: Wake.NotifyKind, watchPaired: Boolean, watchReachable: Boolean): Mode = when {
        kind == Wake.NotifyKind.QUOTA -> Mode.SILENT
        watchPaired && watchReachable -> Mode.SILENT
        else -> Mode.SOUND
    }
}
```

- [ ] **Step 4: Verifica che passino**

Run: `./gradlew :core:testDebugUnitTest --tests 'it.pixelbox.cmwatch.rules.PhoneAlertTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneAlert.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneAlertTest.kt
git commit -m "feat(core): phone alerts stay silent while the paired watch is reachable"
```

---

### Task 5: Struttura dell'app: due schede, ⚙, fascia Demo

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AppShell.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`
- Modify: `mobile/src/main/res/values/strings.xml`, Create: `mobile/src/main/res/values-en/strings.xml`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/ShellScreensTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class Tab { SESSIONS, DIARY }
  @Composable fun AppShell(tab: Tab, demo: Boolean, onTab: (Tab) -> Unit, onSettings: () -> Unit, content: @Composable () -> Unit)
  ```

- [ ] **Step 1: Testi**

In `values/strings.xml` aggiungi (e in `values-en` la traduzione):

```xml
<string name="tab_sessions">Sessioni</string>
<string name="tab_diary">Diario</string>
<string name="settings">Impostazioni</string>
<string name="demo_banner">Demo: sessioni di esempio, nessun PC collegato</string>
```

```xml
<!-- values-en -->
<string name="tab_sessions">Sessions</string>
<string name="tab_diary">Diary</string>
<string name="settings">Settings</string>
<string name="demo_banner">Demo: sample sessions, no computer connected</string>
```

Copia in `values-en` anche le stringhe esistenti del telefono, tradotte (oggi il telefono ha solo `values`).

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test

class ShellScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun shellSessions() = paparazzi.snapshot { CmPhoneTheme { AppShell(Tab.SESSIONS, demo = false, {}, {}) { Text("contenuto") } } }
    @Test fun shellDemo() = paparazzi.snapshot { CmPhoneTheme { AppShell(Tab.DIARY, demo = true, {}, {}) { Text("contenuto") } } }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*ShellScreensTest'`
Expected: FAIL di compilazione, `Unresolved reference: AppShell`.

- [ ] **Step 3: Implementazione**

`AppShell` è uno `Scaffold` Material 3:
- `topBar`: titolo «Claude Master» a sinistra, `IconButton` con `Icons.Rounded.Settings` e `contentDescription = stringResource(R.string.settings)` a destra; sotto, se `demo`, una fascia a tutta larghezza in `CmColors` ambra spento con `R.string.demo_banner` su una riga.
- `bottomBar`: `NavigationBar` con due `NavigationBarItem` (icone `Icons.Rounded.List` e `Icons.Rounded.MenuBook`, etichette `tab_sessions`, `tab_diary`).
- il `content` riempie il resto, con i `paddingValues` dello `Scaffold`.

In `MainActivity`, il ramo `s.paired || s.demoMode` mostra `AppShell` con uno stato `rememberSaveable { mutableStateOf(Tab.SESSIONS) }`; il ramo non accoppiato resta `NotPairedScreen`. Il ⚙ apre `SettingsScreen` (Task 11) come destinazione a schermo intero con `BackHandler`.

- [ ] **Step 4: Compila e verifica**

Run: `./gradlew :mobile:testDebugUnitTest --tests '*ShellScreensTest' :mobile:assembleDebug`
Expected: PASS (Paparazzi in verifica senza golden registra solo in CI; in locale basta che compili e giri).

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AppShell.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/ShellScreensTest.kt
git commit -m "feat(phone): two tabs, settings entry and the Demo band; English strings for the phone"
```

---

### Task 6: Schermata Sessioni (la regia)

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionsScreen.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionCard.kt`
- Modify: `mobile/src/main/res/values/strings.xml`, `values-en/strings.xml`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SessionsScreensTest.kt`

**Interfaces:**
- Consumes: `PhoneBoard.sections`, `PhoneBoard.quotaRows`, `SessionsText.row`, `SessionsText.sub`, `Freshness.of`, `Snapshot`.
- Produces:
  ```kotlin
  @Composable fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (sessionId: String) -> Unit, onLaunch: () -> Unit)
  @Composable fun SessionCard(s: Session, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier)
  ```

- [ ] **Step 1: Testi**

```xml
<string name="launch">Lancia</string>
<string name="closed_n">Chiuse (%1$d)</string>
<string name="quota_resets_at">si azzera alle %1$s</string>
<string name="quota_old">dato vecchio</string>
<string name="stale_data">dati di %1$d min fa</string>
<string name="no_sessions">Nessuna sessione aperta sul PC</string>
```

(`values-en`: «Launch», «Closed (%1$d)», «resets at %1$s», «old data», «data from %1$d min ago», «No open sessions on the computer».)

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File
import org.junit.Rule
import org.junit.Test

class SessionsScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private fun fixture(n: String) = ContractJson.decodeState(File("../contract/$n.json").readText())

    @Test fun sessionsQuestion() {
        val s = fixture("state-1-question")
        paparazzi.snapshot { CmPhoneTheme { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
    @Test fun sessionsStale() {
        val s = fixture("state-3-stale")
        paparazzi.snapshot { CmPhoneTheme { SessionsScreen(Snapshot(s, Freshness.Stale(6)), s.ts + 360, {}, {}) } }
    }
    @Test fun sessionsEmpty() {
        val s = fixture("state-2-idle").copy(sessions = emptyList())
        paparazzi.snapshot { CmPhoneTheme { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
    @Test fun sessionsLargeFont() {
        val s = fixture("state-1-question")
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", fontScale = 1.3f))
        paparazzi.snapshot { CmPhoneTheme { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*SessionsScreensTest'`
Expected: FAIL di compilazione, `Unresolved reference: SessionsScreen`.

- [ ] **Step 3: Implementazione**

`SessionsScreen`:
- se `snapshot.state == null`: spinner centrato, nient'altro;
- `LazyColumn` con, in ordine: una riga per `PhoneBoard.quotaRows(state)` (pallino colore account, nome, barra `LinearProgressIndicator` con colore da `QuotaBar.of(pct)`, percentuale, poi `quota_resets_at` con l'ora locale `HH:mm` di `resetAt` oppure `quota_old` se `stale`); se `snapshot.freshness is Freshness.Stale`, la fascia `stale_data`; poi per ogni `Section` le card, e per `Group.CLOSED` una sola riga `closed_n` che, toccata, apre le card chiuse sotto (`rememberSaveable` booleano);
- `Modifier.animateItem()` sulle card, così scivolano quando l'ordine cambia;
- lista vuota: l'illustrazione «Nessuna sessione» (Task 13) con `no_sessions`;
- in fondo, sopra la barra delle schede, il solo bottone pieno `launch` (`Button` con i colori pieni di `CmColors`).

`SessionCard`: `Card` con superficie «card» di `CmColors`; riga 1: pallino account (`Accounts.isPersonal`), icona di stato (❓ ▶ ✓ ✗ come `Icon` vettoriali del modulo wear, copiati in `mobile/src/main/res/drawable/` se non sono in `ui-tokens`), nome intero con `maxLines = 1` e `softWrap = false` solo se entra, altrimenti a capo dopo il trattino come `NameText` dell'orologio; riga 2: `SessionsText.row(s, now)`; riga 3, se non nulla: `SessionsText.sub(s, now, closed = stringResource(R.string.state_closed))`. Per `Group.WORKING` il bordo sinistro cobalto «respira» con `rememberInfiniteTransition` (alpha 0,55 ↔ 1, 2400 ms), fermo se le animazioni di sistema sono spente (`LocalAccessibilityManager` o `Settings.Global.ANIMATOR_DURATION_SCALE == 0`).

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest --tests '*SessionsScreensTest' :mobile:assembleDebug`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionsScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionCard.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SessionsScreensTest.kt
git commit -m "feat(phone): sessions board — quota per account, cards grouped by state, closed ones folded, Launch"
```

---

### Task 7: Scheda sessione

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionSheet.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/Speech.kt` (TTS di sistema)
- Modify: `strings.xml` (it, en)
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SessionSheetTest.kt`

**Interfaces:**
- Consumes: `PhonePrimary.button`, `PhonePrimary.target`, `QuestionRules.optionLabel`, `Repo.answer`, `Repo.answerText`,
  `Repo.prompt`, `Repo.command(CmdOp.ALLOW_ALL|FOLLOW|UNFOLLOW|REOPEN, …)`, `Repo.retry`, `Snapshot.pending`.
- Produces:
  ```kotlin
  data class SheetActions(
      val answer: (Int) -> Unit, val allowAll: () -> Unit, val send: (PhonePrimary.Target, String) -> Unit,
      val follow: (Boolean) -> Unit, val reopen: () -> Unit, val terminal: () -> Unit, val openInClaude: () -> Unit,
      val speak: (String) -> Unit, val retry: (cmdId: String) -> Unit,
  )
  @Composable fun SessionSheet(s: Session, now: Long, pending: List<Pending>, ttsMinChars: Int, actions: SheetActions)
  ```

- [ ] **Step 1: Testi**

```xml
<string name="write_prompt">Scrivi un prompt</string>
<string name="answer_free">Rispondi con parole tue</string>
<string name="send">Invia</string>
<string name="reopen">Riapri</string>
<string name="terminal">Terminale</string>
<string name="follow">Segui</string>
<string name="unfollow">Non seguire</string>
<string name="open_in_claude">Apri in Claude</string>
<string name="allow_all">Consenti tutto</string>
<string name="read_aloud">Leggi ad alta voce</string>
<string name="not_delivered">Non consegnato</string>
<string name="retry">Riprova</string>
```

(`values-en`: «Write a prompt», «Answer in your own words», «Send», «Reopen», «Terminal», «Follow», «Unfollow», «Open in Claude», «Allow all», «Read aloud», «Not delivered», «Retry».)

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File
import org.junit.Rule
import org.junit.Test

class SessionSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    @Test fun sheetQuestion() = paparazzi.snapshot { CmPhoneTheme { SessionSheet(st.sessions.first { it.question != null }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetIdleWithOutcome() = paparazzi.snapshot { CmPhoneTheme { SessionSheet(st.sessions.first { it.state == SessionState.IDLE }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetClosed() = paparazzi.snapshot { CmPhoneTheme { SessionSheet(st.sessions.first().copy(state = SessionState.GONE, question = null), st.ts, emptyList(), 120, none) } }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*SessionSheetTest'`
Expected: FAIL di compilazione, `Unresolved reference: SessionSheet`.

- [ ] **Step 3: Implementazione**

`SessionSheet`, colonna scorrevole:
1. testata: nome (titolo), progetto e account su una riga, stato con durata (`SessionsText.row`);
2. se `s.question != null`: il testo intero della domanda, con ▶ se lungo più di `ttsMinChars`; le opzioni come
   `FilledTonalButton` a tutta larghezza, testo `QuestionRules.optionLabel(option)`; per `QuestionKind.PERMISSION`
   anche `allow_all` come `OutlinedButton`;
3. ultima risposta/esito (`s.outcome?.full`) con ▶ sempre (esito);
4. campo `OutlinedTextField` multilinea, etichetta `answer_free` se c'è una domanda, `write_prompt` altrimenti;
   `var draft by rememberSaveable { mutableStateOf("") }` — il testo resta se lo stato cambia;
5. azioni come `TextButton` in una `FlowRow`: `terminal`, `follow`/`unfollow` (da `s.followed`), `open_in_claude`
   (solo se `s.link` non è vuoto), `reopen` non qui: è il bottone pieno;
6. in fondo il solo bottone pieno secondo `PhonePrimary.button(s, draft)`: `SEND` → `actions.send(PhonePrimary.target(s, draft)!!, draft)` e svuota `draft`; `REOPEN` → `actions.reopen()`; `NONE` → niente;
7. per ogni `pending` di questa sessione con `PendingStatus.FAILED`, uno `Snackbar` con `not_delivered` e azione `retry`.

`Speech.kt`: un `TextToSpeech` pigro nell'`Application`, `speak(text)` con `QUEUE_FLUSH`; `stop()` quando la scheda esce.

In `MainActivity`, la navigazione dalla card alla scheda: `var open by rememberSaveable { mutableStateOf<String?>(null) }`;
`SheetActions` collegate a `app.repo` dentro `scope.launch`. `openInClaude` apre `Intent(ACTION_VIEW, Uri.parse(s.link))`.

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest --tests '*SessionSheetTest' :mobile:assembleDebug`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionSheet.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/Speech.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SessionSheetTest.kt
git commit -m "feat(phone): session sheet — question with options and allow all, free text, outcome read aloud, one filled button"
```

---

### Task 8: Terminale a schermo grande

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/TerminalScreen.kt`
- Modify: `strings.xml` (it, en)
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/TerminalScreenTest.kt`

**Interfaces:**
- Consumes: `Repo.command(CmdOp.SCREEN, sessionId, null): String` (id del comando), `Repo.resultsById: StateFlow<Map<String, CmdResult>>`, `TerminalText.blocks(text)`.
- Produces: `@Composable fun TerminalScreen(name: String, text: String?, loading: Boolean, onRefresh: () -> Unit)`

- [ ] **Step 1: Testi**

```xml
<string name="refresh">Aggiorna</string>
<string name="terminal_empty">Tocca Aggiorna per leggere lo schermo della sessione</string>
```
(`values-en`: «Refresh», «Tap Refresh to read the session's screen».)

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test

class TerminalScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val sample = "● Read(core/src/Repo.kt)\n  ⎿  Read 240 lines\n\n● I'll add the phone board next.\n\n> "

    @Test fun terminalText() = paparazzi.snapshot { CmPhoneTheme { TerminalScreen("kb", sample, loading = false, onRefresh = {}) } }
    @Test fun terminalEmpty() = paparazzi.snapshot { CmPhoneTheme { TerminalScreen("kb", null, loading = false, onRefresh = {}) } }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*TerminalScreenTest'`
Expected: FAIL di compilazione.

- [ ] **Step 3: Implementazione**

Colonna: titolo con `name`; `LazyColumn` dei blocchi di `TerminalText.blocks(text)` in `FontFamily.Monospace`, righe
a capo; ogni blocco nuovo (chiave = indice + hash del testo) entra con `fadeIn(tween(250))`, i vecchi restano; con
`text == null` il testo `terminal_empty`; bottone pieno `refresh`, disabilitato con `loading`. In `MainActivity`:
`onRefresh` → `val id = app.repo.command(CmdOp.SCREEN, s.id, null)`, poi il testo arriva da `repo.resultsById` con
quell'id (`CmdResult.text`).

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest --tests '*TerminalScreenTest' :mobile:assembleDebug`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/TerminalScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/TerminalScreenTest.kt
git commit -m "feat(phone): full-screen terminal read on request, new blocks fade in"
```

---

### Task 9: Lancia (foglio) e suggerimenti dei progetti

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggest.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggestTest.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/LaunchSheet.kt`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/LaunchSheetTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object LaunchSuggest { fun projects(state: State, account: String, typed: String, limit: Int = 6): List<Project> }
  @Composable fun LaunchSheet(state: State, onLaunch: (project: Project, firstMessage: String) -> Unit, onDismiss: () -> Unit)
  ```
- Il comando: `Repo.command(CmdOp.LAUNCH, session = null, arg = project.path, text = firstMessage.ifBlank { null })`.
  Prima di scrivere il Task, controlla in `contract/README.md` (contratto 1.13) quale campo porta la cartella di `launch`
  e il suo account; se è diverso da `arg = path`, usa quello del README.

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchSuggestTest {
    private val st = State(v = 1, ts = 0, host = "pc", projects = listOf(
        Project("/a/kb", "kb", "personale", lastUsed = 50), Project("/a/docs", "docs", "personale", lastUsed = 90),
        Project("/w/kb", "kb", "lavoro", lastUsed = 99), Project("/a/old", "old", "personale", lastUsed = null),
    ))

    @Test fun onlyThatAccountMostRecentFirst() =
        assertEquals(listOf("/a/docs", "/a/kb", "/a/old"), LaunchSuggest.projects(st, "personale", "").map { it.path })

    @Test fun typedFiltersByNamePiece() =
        assertEquals(listOf("/a/kb"), LaunchSuggest.projects(st, "personale", "K").map { it.path })
}
```

Run: `./gradlew :core:testDebugUnitTest --tests '*LaunchSuggestTest'`
Expected: FAIL, `Unresolved reference: LaunchSuggest`.

- [ ] **Step 2: Implementazione**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.State

/** I progetti da proporre nel foglio «Lancia»: dell'account scelto, per pezzo di nome, i più recenti prima. */
object LaunchSuggest {
    fun projects(state: State, account: String, typed: String, limit: Int = 6): List<Project> {
        val t = typed.trim().lowercase()
        return state.projects
            .filter { it.account == account && (t.isEmpty() || t in it.name.lowercase()) }
            .sortedByDescending { it.lastUsed ?: Long.MIN_VALUE }
            .take(limit)
    }
}
```

Run: `./gradlew :core:testDebugUnitTest --tests '*LaunchSuggestTest'` → PASS.

- [ ] **Step 3: Foglio**

Testi: `launch_project` «Progetto» / «Project», `launch_account` «Account», `launch_first` «Primo messaggio» / «First
message». `LaunchSheet` in `ModalBottomSheet`: scelta account con `SegmentedButton` (dalle chiavi di `state.quota`,
personale prima con `Accounts.isPersonalQuota`), campo progetto con la lista di `LaunchSuggest.projects` sotto (tocco =
scelto), campo multilinea `launch_first`, bottone pieno `launch` abilitato solo con un progetto scelto.

Snapshot in `LaunchSheetTest` con lo stato di `contract/state-2-idle.json`: `paparazzi.snapshot { CmPhoneTheme { LaunchSheet(st, { _, _ -> }, {}) } }`.
Se il fixture non ha `projects`, costruisci lo stato con la lista del test di `LaunchSuggestTest`.

- [ ] **Step 4: Verifica**

Run: `./gradlew :core:testDebugUnitTest :mobile:testDebugUnitTest --tests '*Launch*' :mobile:assembleDebug`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggest.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggestTest.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/LaunchSheet.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/LaunchSheetTest.kt
git commit -m "feat(phone): Launch sheet with the account's recent projects and a first message"
```

---

### Task 10: Scheda Diario con i dati di oggi

Il contratto porta già `state.recap` (voci del diario per progetto), `state.night` (lavori in coda e quello in corso) e
gli eventi `quota`. Qui si mostrano; la coda modificabile, il resoconto della notte e i giorni precedenti arrivano con il
piano B (R3, R4).

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/DiaryScreen.kt`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/DiaryScreenTest.kt`

**Interfaces:**
- Consumes: `State.recap: Recap(date, items: List<RecapItem(project, done, next)>)`, `State.night: Night(queued, running)`, `Repo.events` filtrati per `EventKind.QUOTA`.
- Produces: `@Composable fun DiaryScreen(state: State, quotaEvents: List<Event>, ttsMinChars: Int, onSpeak: (String) -> Unit)`

- [ ] **Step 1: Testi**

```xml
<string name="diary_title">Diario del %1$s</string>
<string name="diary_next">Prossimo passo: %1$s</string>
<string name="night_queued">Stanotte in coda: %1$d</string>
<string name="night_running">In corso: %1$s</string>
<string name="night_empty">Nessun lavoro in coda per stanotte</string>
<string name="quota_alerts">Avvisi di quota</string>
<string name="diary_empty">Il diario arriva alle 20:00</string>
```
(`values-en`: «Diary of %1$s», «Next step: %1$s», «Queued for tonight: %1$d», «Running: %1$s», «Nothing queued for
tonight», «Quota alerts», «The diary arrives at 20:00».)

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File
import org.junit.Rule
import org.junit.Test

class DiaryScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText()).copy(
        recap = Recap("2026-09-29", listOf(RecapItem("kb", "Distillata l'analisi Play", "Rivedere l'indice"), RecapItem("watch", "Specifica del pezzo 3"))),
        night = Night(queued = 2, running = null),
    )
    private val quota = listOf(Event("q1", EventKind.QUOTA, account = "personale", ts = st.ts, title = "personale al 95%", body = "si azzera alle 18:40"))

    @Test fun diaryFull() = paparazzi.snapshot { CmPhoneTheme { DiaryScreen(st, quota, 120, {}) } }
    @Test fun diaryEmpty() = paparazzi.snapshot { CmPhoneTheme { DiaryScreen(st.copy(recap = Recap(), night = Night()), emptyList(), 120, {}) } }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*DiaryScreenTest'` → FAIL di compilazione.

- [ ] **Step 3: Implementazione**

`LazyColumn`: sezione diario (titolo `diary_title` con la data in formato locale; per voce: progetto in grassetto,
`done`, e `diary_next` se `next` non è nullo; ▶ sulla voce se lunga oltre `ttsMinChars`); sezione notte
(`night_queued` o `night_empty`, e `night_running` se c'è); sezione `quota_alerts` con gli eventi (titolo e corpo).
Senza voci del diario né notte né avvisi: l'illustrazione «Diario vuoto» (Task 13) con `diary_empty`. Nessun bottone
pieno qui fino al piano B («Aggiungi alla notte» arriva con R3).

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest --tests '*DiaryScreenTest' :mobile:assembleDebug` → PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/DiaryScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/DiaryScreenTest.kt
git commit -m "feat(phone): Diary tab with today's recap, the night queue count and quota alerts from the current contract"
```

---

### Task 11: Impostazioni e «Prova la demo»

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SettingsScreen.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NotPairedScreen.kt` (link «Prova la demo»)
- Modify: `PhoneScreensTest.kt` (firma nuova di `NotPairedScreen`)
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SettingsScreenTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable fun SettingsScreen(host: String?, phoneName: String, watchName: String?, watchPending: Boolean, demo: Boolean,
      version: String, onRepair: () -> Unit, onDemo: (Boolean) -> Unit, onNotifications: () -> Unit, onPrivacy: () -> Unit)
  @Composable fun NotPairedScreen(onPair: () -> Unit, onPaste: () -> Unit, onDemo: () -> Unit)
  ```

- [ ] **Step 1: Testi**

```xml
<string name="try_demo">Prova la demo</string>
<string name="demo_mode">Modalità demo</string>
<string name="demo_mode_sub">Sessioni di esempio, senza PC</string>
<string name="notifications">Notifiche</string>
<string name="privacy">Privacy</string>
<string name="version">Versione %1$s</string>
<string name="privacy_url" translatable="false">https://github.com/frsorrentino/claude-master-watch/blob/master/PRIVACY.md</string>
```
(`values-en`: «Try the demo», «Demo mode», «Sample sessions, no computer», «Notifications», «Privacy», «Version %1$s».)

- [ ] **Step 2: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun settingsPaired() = paparazzi.snapshot { CmPhoneTheme { SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}) } }
    @Test fun settingsDemo() = paparazzi.snapshot { CmPhoneTheme { SettingsScreen(null, "Pixel 9", null, false, true, "0.2", {}, {}, {}, {}) } }
}
```

In `PhoneScreensTest.notPaired` passa `onDemo = {}`.

Run: `./gradlew :mobile:testDebugUnitTest --tests '*SettingsScreenTest'` → FAIL di compilazione.

- [ ] **Step 3: Implementazione**

`SettingsScreen`: il contenuto di `PairedScreen` di oggi in testa (host, telefono, orologio con «in attesa»), poi
`ListItem` con `Switch` per `demo_mode`, `ListItem` `notifications` (→ `Settings.ACTION_APP_NOTIFICATION_SETTINGS`),
`privacy` (→ `privacy_url`), `version` da `BuildConfig`/`packageManager`. Bottone pieno: «Rifai l'accoppiamento»
(stringa già esistente). `NotPairedScreen`: sotto «Incolla il codice», un `TextButton` `try_demo` → `app.setDemo(true)`.
`PairedScreen` resta per compatibilità dei test dell'accoppiamento, oppure viene rimosso se nessuno lo usa più: in quel
caso togli anche i suoi snapshot da `PhoneScreensTest`.

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug` → PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SettingsScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NotPairedScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SettingsScreenTest.kt mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/PhoneScreensTest.kt
git commit -m "feat(phone): settings with pairing, Demo mode, notifications and privacy; Try the demo on the first screen"
```

---

### Task 12: Notifiche sul telefono

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/PhoneMessagingService.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/PhoneNotifier.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/ReplyReceiver.kt`
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/WatchPresence.kt`
- Modify: `mobile/src/main/AndroidManifest.xml`, `mobile/build.gradle.kts` (`libs.firebase.messaging`), `PhoneApp.kt`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/push/ReplyRouteTest.kt`

**Interfaces:**
- Consumes: `Wake.plan(prev, cur)`, `Wake.Action.Notify(kind, session)`, `Wake.Action.CloseQuestion(session)`,
  `NotificationPlan.question(s, labels, history)`, `PhoneAlert.mode`, `Repo.answer`, `Repo.answerText`.
- Produces:
  ```kotlin
  object ReplyRoute {
      sealed class Cmd { data class Option(val session: String, val n: Int) : Cmd(); data class Text(val session: String, val text: String) : Cmd() }
      fun from(action: String?, session: String?, n: Int, text: String?): Cmd?
  }
  class WatchPresence(ctx: Context) { suspend fun reachable(): Boolean }   // CapabilityClient, capacità "cmwatch_wear"
  ```

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile.push

import org.junit.Assert.*
import org.junit.Test

class ReplyRouteTest {
    @Test fun optionAction() = assertEquals(ReplyRoute.Cmd.Option("kb", 2), ReplyRoute.from(ReplyRoute.OPTION, "kb", 2, null))
    @Test fun textAction() = assertEquals(ReplyRoute.Cmd.Text("kb", "sì"), ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, " sì "))
    @Test fun blankTextIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, "  "))
    @Test fun missingSessionIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.OPTION, null, 1, null))
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*ReplyRouteTest'` → FAIL di compilazione.

- [ ] **Step 2: `ReplyRoute`**

In `ReplyReceiver.kt`:

```kotlin
object ReplyRoute {
    const val OPTION = "it.pixelbox.cmwatch.mobile.OPTION"
    const val REPLY = "it.pixelbox.cmwatch.mobile.REPLY"
    sealed class Cmd { data class Option(val session: String, val n: Int) : Cmd(); data class Text(val session: String, val text: String) : Cmd() }
    fun from(action: String?, session: String?, n: Int, text: String?): Cmd? {
        if (session == null) return null
        return when (action) {
            OPTION -> if (n > 0) Cmd.Option(session, n) else null
            REPLY -> text?.trim()?.takeIf { it.isNotEmpty() }?.let { Cmd.Text(session, it) }
            else -> null
        }
    }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*ReplyRouteTest'` → PASS.

- [ ] **Step 3: Servizi e canali**

- `PhoneMessagingService : FirebaseMessagingService`: `onMessageReceived` → `(application as PhoneApp).onWake()`, che
  fa `repo.refresh()`. In `PhoneApp.onCreate`, iscrizione al topic come `CmApp.subscribeTopic()` (topic da
  `FirebaseBoot.active.topic`), solo se accoppiato e non in Demo; e il diff degli stati come in `CmApp`: per ogni
  `Wake.plan(prev, cur)` con l'app non in primo piano, `Notify` → `PhoneNotifier`, `CloseQuestion` → chiude.
- `PhoneNotifier`: due canali, `questions` (`IMPORTANCE_HIGH`) e `quiet` (`IMPORTANCE_LOW`). Il canale lo sceglie
  `PhoneAlert.mode(kind, watchPaired = PairingRecord.fromJson(settings.pairingJson)?.watchName != null, watchReachable = WatchPresence(ctx).reachable())`:
  `SOUND` → `questions`, `SILENT` → `quiet`. Domanda: `NotificationPlan.question(...)` per titolo, testo e azioni
  (fino a tre `Option` come `PendingIntent` a `ReplyReceiver` con `OPTION`, più `Reply` con `RemoteInput`). Id della
  notifica = hash del nome della sessione, così `CloseQuestion` la chiude.
- `ReplyReceiver : BroadcastReceiver`: `ReplyRoute.from(...)` → `repo.answer` o `repo.answerText` in `goAsync()`; la
  notifica diventa «in attesa di rete» se `!app.isOnline()`, poi si chiude quando la domanda sparisce dallo stato.
- `WatchPresence.reachable()`: `Wearable.getCapabilityClient(ctx).getCapability("cmwatch_wear", CapabilityClient.FILTER_REACHABLE).await().nodes.isNotEmpty()`, con `runCatching { … }.getOrDefault(false)`.
- Manifest: `<service android:name=".push.PhoneMessagingService" android:exported="false"><intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT"/></intent-filter></service>`,
  `<receiver android:name=".push.ReplyReceiver" android:exported="false"/>`, permesso `POST_NOTIFICATIONS` chiesto al
  primo avvio accoppiato (non in Demo).
- Testi: `channel_questions` «Domande», `channel_quiet` «Avvisi silenziosi», `waiting_network` «In attesa di rete»
  (`values-en`: «Questions», «Quiet notices», «Waiting for network»).

- [ ] **Step 4: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug` → PASS.

- [ ] **Step 5: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/PhoneMessagingService.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/PhoneNotifier.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/ReplyReceiver.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/push/WatchPresence.kt mobile/src/main/AndroidManifest.xml mobile/build.gradle.kts mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/push/ReplyRouteTest.kt
git commit -m "feat(phone): notifications — questions with answers from the shade, silent while the watch is reachable"
```

---

### Task 13: Illustrazioni

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/art/Scenes.kt`
- Modify: `NotPairedScreen.kt`, `PairingScreen.kt`, `SettingsScreen.kt`, `SessionsScreen.kt`, `DiaryScreen.kt`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/ScenesTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable fun ScanScene(modifier: Modifier = Modifier)                          // telefono che inquadra il QR sul PC
  @Composable fun LinkScene(phone: StepState, watch: StepState, pc: StepState, modifier: Modifier = Modifier)
  @Composable fun PairedScene(watch: Boolean, modifier: Modifier = Modifier)
  @Composable fun EmptySessionsScene(modifier: Modifier = Modifier)
  @Composable fun EmptyDiaryScene(modifier: Modifier = Modifier)
  @Composable fun rememberReducedMotion(): Boolean
  ```

- [ ] **Step 1: Snapshot che falliscono**

```kotlin
package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.pair.StepState
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.art.*
import org.junit.Rule
import org.junit.Test

class ScenesTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val m = Modifier.fillMaxWidth().height(320.dp)

    @Test fun scan() = paparazzi.snapshot { CmPhoneTheme { ScanScene(m) } }
    @Test fun linkWatchFailed() = paparazzi.snapshot { CmPhoneTheme { LinkScene(StepState.DONE, StepState.FAILED, StepState.WAIT, m) } }
    @Test fun paired() = paparazzi.snapshot { CmPhoneTheme { PairedScene(watch = true, m) } }
    @Test fun emptySessions() = paparazzi.snapshot { CmPhoneTheme { EmptySessionsScene(m) } }
    @Test fun emptyDiary() = paparazzi.snapshot { CmPhoneTheme { EmptyDiaryScene(m) } }
}
```

Run: `./gradlew :mobile:testDebugUnitTest --tests '*ScenesTest'` → FAIL di compilazione.

- [ ] **Step 2: Implementazione**

Ogni scena è un `Canvas` con forme semplici (rettangoli arrotondati per PC, telefono e orologio; linee di
collegamento; il QR come griglia 7×7 di quadrati) nei `CmColors` (superfici, corallo per l'identità, ambra, cobalto,
rosso spento per il passo fallito). Ingresso: `Animatable(0f → 1f)` con `tween(300, easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f))`
che scala l'opacità e sposta di 8 dp; `LinkScene` accende i collegamenti nell'ordine dei passi e si ferma al passo
`FAILED`. `rememberReducedMotion()` legge `Settings.Global.ANIMATOR_DURATION_SCALE` (0 → true): con `true` il valore
parte da 1f. In Paparazzi le animazioni non girano: il test vede lo stato finale se il valore iniziale, con
`LocalInspectionMode.current`, è 1f — imposta così.

Metti le scene nelle schermate: metà alta di `NotPairedScreen` (`ScanScene`), `PairingScreen` (`LinkScene` dallo
stato dei passi), `SettingsScreen` sopra l'accoppiamento (`PairedScene`), `SessionsScreen` vuota
(`EmptySessionsScene`), `DiaryScreen` vuota (`EmptyDiaryScene`).

- [ ] **Step 3: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug` → PASS.

- [ ] **Step 4: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/art/Scenes.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/NotPairedScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PairingScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SettingsScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionsScreen.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/DiaryScreen.kt mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/ScenesTest.kt
git commit -m "feat(phone): contextual scenes drawn in Compose — scan, link, paired, empty board and diary"
```

---

### Task 14: Il «volo» dalla card alla scheda

**Files:**
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`
- Modify: `SessionCard.kt`, `SessionSheet.kt`

- [ ] **Step 1: Implementazione**

Avvolgi la destinazione «regia ↔ scheda» in `SharedTransitionLayout` con `AnimatedContent(targetState = open)`. Card e
scheda condividono la chiave `"card-${s.id}"` con `Modifier.sharedBounds(rememberSharedContentState(key), animatedVisibilityScope, boundsTransform = { _, _ -> tween(320, easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)) }, resizeMode = RemeasureToBounds)`,
senza dissolvenza finale (`enter = EnterTransition.None`, `exit = ExitTransition.None` sul contenuto condiviso). Il
gesto indietro usa `PredictiveBackHandler`: il progresso del gesto porta la scheda verso la card
(`SeekableTransitionState.seekTo(progress)`). Con `rememberReducedMotion()` true: `snap()` al posto di `tween`.

- [ ] **Step 2: Verifica**

Run: `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug` → PASS. La prova visiva è nella checklist dal vivo
(Task 16, punto 2).

- [ ] **Step 3: Commit**

```bash
git add mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionCard.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionSheet.kt
git commit -m "feat(phone): the card flies into the session sheet, predictive back shrinks it home"
```

---

### Task 15: Percorso Demo senza rete

La prova che l'attestazione a Play è vera: in Demo ogni schermata ha contenuto, senza rete.

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneRoutes.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneRoutesTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  object PhoneRoutes {
      enum class Route { SESSIONS, SHEET_QUESTION, SHEET_IDLE, TERMINAL, LAUNCH, DIARY, SETTINGS }
      /** Le rotte raggiungibili da uno stato: una rotta manca se lo stato non ha ciò che serve a mostrarla piena. */
      fun reachable(state: State): Set<Route>
  }
  ```

- [ ] **Step 1: Test che falliscono**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneRoutesTest {
    @Test fun demoReachesEveryRouteOffline() = runTest {
        val fake = FakeTransport(load = { DemoText.dress(File("../contract/$it.json").readText()) })
        val st = fake.state.first()
        assertEquals(PhoneRoutes.Route.entries.toSet(), PhoneRoutes.reachable(st))
    }
}
```

Run: `./gradlew :core:testDebugUnitTest --tests '*PhoneRoutesTest'` → FAIL, `Unresolved reference: PhoneRoutes`.

- [ ] **Step 2: Implementazione**

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Rotte del telefono (design 29/09) e quelle che uno stato riempie: in Demo devono esserci tutte. */
object PhoneRoutes {
    enum class Route { SESSIONS, SHEET_QUESTION, SHEET_IDLE, TERMINAL, LAUNCH, DIARY, SETTINGS }

    fun reachable(state: State): Set<Route> = buildSet {
        add(Route.SETTINGS)
        if (state.sessions.isNotEmpty()) { add(Route.SESSIONS); add(Route.TERMINAL) }
        if (state.sessions.any { it.question != null }) add(Route.SHEET_QUESTION)
        if (state.sessions.any { it.state == SessionState.IDLE && it.outcome != null }) add(Route.SHEET_IDLE)
        if (state.projects.isNotEmpty()) add(Route.LAUNCH)
        if (state.recap.items.isNotEmpty() || state.night.queued > 0) add(Route.DIARY)
    }
}
```

- [ ] **Step 3: Verifica, e completa la Demo se manca qualcosa**

Run: `./gradlew :core:testDebugUnitTest --tests '*PhoneRoutesTest'`.
Se fallisce per `LAUNCH` o `DIARY`, la fixture `contract/state-1-question.json` non ha `projects`, `recap` o `night`:
non toccare le fixture del contratto (le produce il relay). Arricchisci invece lo stato iniziale della Demo in
`FakeTransport` (costruttore: `current = MutableStateFlow(rebase(demoExtras(decodeState(...))))`), con
`demoExtras(s)` che aggiunge, solo se vuoti, tre `Project` presi dai nomi delle sessioni della fixture, un `Recap` di
due voci inglesi e `Night(queued = 2)`. Testi della Demo in inglese, come il resto della Demo. Poi il test passa.
Verifica che gli snapshot dell'orologio non cambino: `./gradlew :wear:testDebugUnitTest`.

- [ ] **Step 4: Commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneRoutes.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/PhoneRoutesTest.kt core/src/main/kotlin/it/pixelbox/cmwatch/transport/FakeTransport.kt
git commit -m "test: the Demo fills every phone route offline — projects, recap and night added to the demo state"
```

---

### Task 16: Snapshot in CI e checklist dal vivo

**Files:**
- Create: `docs/verifiche/pezzo-3-telefono.md`
- Modify: `.github/workflows/build-android.yml` (solo se la registrazione Paparazzi non copre già `:mobile`)

- [ ] **Step 1: Registrazione in CI**

Controlla che il job con `record: true` registri anche `:mobile:recordPaparazziDebug`
(`grep -n recordPaparazzi .github/workflows/build-android.yml`). Se registra solo `:wear`, aggiungi `:mobile` allo
stesso comando. Push del ramo, poi `gh workflow run build-android.yml --ref feature/telefono -f record=true`, scarica
l'artifact degli snapshot e committa le immagini nuove in `mobile/src/test/snapshots/images/`, per nome. Guarda ogni
immagine prima del commit: niente testo tagliato, niente «…», un solo bottone pieno.

- [ ] **Step 2: Checklist**

Scrivi `docs/verifiche/pezzo-3-telefono.md` con queste righe, ognuna con esito e data vuoti da riempire dal vivo:

```markdown
# Pezzo 3 del telefono: verifiche dal vivo

- [ ] 1. Regia con i due account veri e la quota di entrambi.
- [ ] 2. Card → scheda con il volo; gesto indietro che la riporta alla card.
- [ ] 3. Risposta a una domanda dal telefono; la notifica sparisce anche dall'orologio.
- [ ] 4. Con l'orologio collegato la notifica del telefono è silenziosa; a orologio spento suona.
- [ ] 5. Risposta dalla notifica, con un'opzione e con testo scritto.
- [ ] 6. Lancio di una sessione con il primo messaggio; terminale letto con «Aggiorna».
- [ ] 7. Diario delle 20:00 visto nella scheda Diario.
- [ ] 8. Demo da installazione pulita, senza PC: tutte le schermate, fascia «Demo» sempre visibile.
- [ ] 9. «Riduci animazioni» acceso: nessun movimento, stati finali subito.
- [ ] Piano B: coda della notte modificabile (R3), resoconto della notte (R4), Condividi (R5), riserva di Telegram (R6).
```

- [ ] **Step 3: Commit**

```bash
git add docs/verifiche/pezzo-3-telefono.md
git add mobile/src/test/snapshots/images/<ogni file nuovo, per nome>
git commit -m "test: phone piece 3 snapshots recorded in CI; live checklist"
```

---

## Dopo il piano A

1. Richiesta R3 a claude-master (coda della notte), quando non è in release. Poi il piano B.
2. Build in CI, AAB 31/32 per la traccia interna (versionName 0.3), su richiesta di Franz.
3. Checklist dal vivo con Franz.
