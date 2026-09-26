# Video promozionali — piano di implementazione

> **Per chi esegue:** skill richiesta `superpowers:executing-plans` (inline, consigliata qui: lavoro visivo e al polso,
> non delegabile) oppure `superpowers:subagent-driven-development` solo per i Task 2 e 7. Passi con checkbox `- [ ]`.

**Obiettivo:** un video di ~75 s, «Il rilascio 2.8.0, dal polso», registrato dall'orologio vero in demo inglese e montato
in una cornice d'orologio, più i ritagli per README e pagina del plugin.

**Architettura:** la demo dell'app diventa pilotabile a scene via adb (stati della storia, dettatura simulata, eventi solo
demo, scorrimento guidato già fatto). `tools/promo/record.sh` recita una scena per registrazione. Le clip si rivedono nella
pagina HTML con la cassa approvata (fase A), poi si montano con Remotion in MP4 e GIF (fase B).

**Tecnologie:** Kotlin, Wear Compose Material 3 1.6.2, JUnit (core), adb su TLS (`/usr/bin/adb` + server qemu), ffmpeg 5.1,
Remotion 4.0.490 (Node 24, arm64), Pillow.

**Specifica:** `docs/plans/2026-09-16-video-promo-piano.md` (sequenza, prerequisiti, verifiche, decisioni).

## Vincoli globali

- Fixture del contratto (`contract/*.json`) **mai modificate**: il test R0 di claude-master le confronta byte per byte.
  Nomi e testi della demo cambiano solo in `wear/.../DemoText.kt`, al caricamento.
- Tutto ciò che è demo si attiva **solo via adb** e **solo con `demoMode` acceso**; nell'uso normale nessun effetto.
- Testi visibili in `res/values/strings.xml` e `values-en/`; commit in inglese, mai `git add -A`, mai file sensibili.
- Build locale: `./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1400m -Pkotlin.compiler.execution.strategy=in-process`,
  release firmata con `set -a; . ~/Desktop/workspaces/personali/watchface/release-keystore-credentials.txt;
  KEYSTORE_PATH=~/…/watchface/release.jks; set +a`, lint vital saltato
  (`-x lintVitalAnalyzeRelease -x lintVitalReportRelease -x lintVitalRelease`). Mai due Gradle insieme (6 GB).
- Installazione: copia di sicurezza (`pm path` + `adb pull`), controllo Firebase nell'APK
  (`unzip -p APK resources.arsc | grep -a -c firebaseio`), `install -r`, controllo crash all'avvio.
- Registrazioni: mai fotogrammi con quadrante personale, impostazioni, indirizzi, dati di salute. Controllo a un fotogramma
  ogni mezzo secondo su **tutta** la clip.
- Dopo ogni sessione di registrazione: `record.sh teardown` (dati veri, italiano, timeout schermo originale).
- Non modificare `~/Desktop/workspaces/personali/video/storytelling-video`: se ne copia l'impianto.

---

### Task 1: «Write» e gli altri tasti di bordo come ultima voce della lista

Nelle clip «Write» restava pieno sopra il contenuto durante lo scorrimento. La lista sessioni ha già lo schema giusto
(`SessionsScreen.kt:123`: il tasto è l'ultimo `item` della lista, curvo sul bordo). Si applica a Scheda, Terminale, Domanda.

**File:**
- Modifica: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/TerminalScreen.kt` (slot `edgeButton` a riga ~121)
- Modifica: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/QuestionScreen.kt` (slot a riga ~94)
- Modifica: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/SessionScreen.kt` (slot a righe ~92-104)

**Interfacce:** nessuna nuova; `CmEdgeButton(label, onClick, enabled)` invariato.

- [ ] **Passo 1: Terminale.** Togliere il parametro `edgeButton = { CmEdgeButton(stringResource(R.string.card_write), onClick = onWrite) },`
  da `ScreenScaffold(...)` e aggiungere come ultima voce dentro `TransformingLazyColumn { … }`, dopo il blocco `when`:

```kotlin
            // «Write» in fondo alla lista, curvo sul bordo come in Sessioni: nello slot fisso dello scaffold restava pieno
            // sopra il testo mentre si scorreva (video del 16/09 18:48).
            item { CmEdgeButton(stringResource(R.string.card_write), onClick = onWrite) }
```

- [ ] **Passo 2: Domanda.** Stessa cosa con `stringResource(R.string.question_write)` e l'`onClick` che oggi è nello slot.
  L'ultima voce va **dopo** le opzioni di risposta.
- [ ] **Passo 3: Scheda.** Lo slot contiene un `when` su cinque casi. Spostare l'intero `when` in un `item { … }` finale
  della lista, identico nei rami; `ScreenScaffold(scrollState = listState)` resta senza `edgeButton`.
- [ ] **Passo 4: Build e installazione** con i comandi dei vincoli globali. Atteso: `BUILD SUCCESSFUL`, `installata`,
  `crash dopo l'avvio: 0`.
- [ ] **Passo 5: Verifica al polso** (demo accesa, `record.sh setup`): aprire `cmwatch://terminal/storefront`, scorrere con
  `am start … --ei scroll_px 400 --ei scroll_ms 2000`, screenshot con `adb exec-out screencap -p`. Atteso: «Write» non
  compare finché la lista non arriva in fondo; in fondo è curvo sul bordo.
- [ ] **Passo 6: Commit**

```bash
git add wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/TerminalScreen.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/QuestionScreen.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/ui/screens/SessionScreen.kt
git commit -m "fix(watch): edge buttons as the last list item, so Write no longer covers the text while scrolling"
```

---

### Task 2: gli stati della storia nel trasporto finto (core, TDD)

Ogni scena parte da uno stato noto. I nomi nel trasporto finto sono quelli caricati (in app già rinominati da `DemoText`),
quindi gli stati si costruiscono **per ruolo**, non per nome: nella fixture `state-1-question` la sessione 0 ha la domanda,
la 1 lavora, la 2 è ferma, la 3 è chiusa.

**File:**
- Modifica: `core/src/main/kotlin/it/pixelbox/cmwatch/transport/FakeTransport.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/transport/FakeTransportTest.kt`

**Interfacce:**
- Produce: `enum class DemoStep { CALM, QUESTION, DEPLOYED, FOLLOWUP, BLOG }` (in `FakeTransport.kt`, top level) e
  `fun FakeTransport.demoStep(step: DemoStep)`; `CmdOp.LAUNCH` nella demo restituisce `CmdResult.session` = nome del progetto
  e mette quella sessione al lavoro; `CmdOp.SCREEN` dopo `FOLLOWUP` restituisce righe in più a ogni chiamata.

- [ ] **Passo 1: test che falliscono** (in coda a `FakeTransportTest`):

```kotlin
    // ---- Storia dei video (piano 16/09): ogni scena parte da uno stato noto, costruito per ruolo e non per nome. ----

    @Test fun calmaNessunaDomanda() = runTest {
        val tr = t(); tr.demoStep(DemoStep.CALM)
        val s = tr.state.first()
        assertTrue(s.sessions.none { it.question != null })
        assertEquals(SessionState.IDLE, s.sessions[0].state)
    }

    @Test fun laDomandaDelDeployArrivaAdesso() = runTest {
        val tr = t(); tr.demoStep(DemoStep.CALM); tr.demoStep(DemoStep.QUESTION)
        val q = tr.state.first().sessions.first { it.question != null }
        assertEquals(SessionState.WAITING, q.state)
        assertEquals(clock, q.question!!.askedAt)
    }

    @Test fun deployFattoConEsitoESeguita() = runTest {
        val tr = t(); tr.demoStep(DemoStep.DEPLOYED)
        val s = tr.state.first().sessions.first { it.outcome?.short == "Deployed 2.8.0, smoke tests green" }
        assertEquals(SessionState.IDLE, s.state); assertNull(s.question); assertTrue(s.followed)
        assertEquals(clock, s.outcome!!.at)
    }

    @Test fun ilPassoDopoLavoraEIlTerminaleCresce() = runTest {
        val tr = t(); tr.demoStep(DemoStep.FOLLOWUP)
        val s = tr.state.first().sessions.first { it.toolNote == "Update the changelog and tag the release" }
        assertEquals(SessionState.BUSY, s.state)
        val uno = tr.send(Cmd("s1", CmdOp.SCREEN, s.name, null, clock, "test")).text.lines().size
        val due = tr.send(Cmd("s2", CmdOp.SCREEN, s.name, null, clock, "test")).text.lines().size
        assertTrue("il terminale deve crescere: $uno → $due", due > uno)
    }

    @Test fun ilLaunchDellaDemoCreaLaSessione() = runTest {
        val tr = t()
        val p = tr.state.first().projects.first()
        val r = tr.send(Cmd("l1", CmdOp.LAUNCH, null, p.path, clock, "test", text = "Draft a post about the 2.8.0 release"))
        assertTrue(r.ok); assertEquals(p.name, r.session)
        val s = tr.state.first().sessions.first { it.name == p.name }
        assertEquals(SessionState.BUSY, s.state); assertEquals("Draft a post about the 2.8.0 release", s.toolNote)
    }
```

- [ ] **Passo 2: verificare il rosso.** `./gradlew … :core:testDebugUnitTest --tests '*FakeTransportTest*'`.
  Atteso: `Unresolved reference 'demoStep'` / `'DemoStep'`.
- [ ] **Passo 3: implementazione** in `FakeTransport.kt`. Aggiungere in cima al file, fuori dalla classe:

```kotlin
/** Gli stati della storia dei video promozionali (piano 16/09): uno per scena, raggiungibili via adb. */
enum class DemoStep { CALM, QUESTION, DEPLOYED, FOLLOWUP, BLOG }
```

  Dentro la classe, accanto a `useFixture`:

```kotlin
    private val base = rebase(ContractJson.decodeState(load("state-1-question")))
    private val originalQuestion = base.sessions[0].question
    private var screenGrowth = 0

    /**
     * Porta la demo allo stato di una scena. Per ruolo: sessione 0 = quella del deploy, 1 = al lavoro, 2 = ferma (il blog),
     * come nella fixture `state-1-question`. I testi sono in inglese come la demo.
     */
    fun demoStep(step: DemoStep) {
        val s = current.value
        val t = now()
        fun at(i: Int, f: (Session) -> Session) = s.copy(sessions = s.sessions.mapIndexed { j, x -> if (j == i) f(x) else x })
        current.value = when (step) {
            DemoStep.CALM -> at(0) { it.copy(state = SessionState.IDLE, question = null, since = t - 900) }
            DemoStep.QUESTION -> at(0) { it.copy(state = SessionState.WAITING, since = t, question = originalQuestion?.copy(askedAt = t)) }
            DemoStep.DEPLOYED -> at(0) {
                it.copy(
                    state = SessionState.IDLE, question = null, since = t, followed = true,
                    outcome = Outcome("Deployed 2.8.0, smoke tests green",
                        "Deployed 2.8.0 to production. Smoke tests are green on checkout, refunds and webhooks; error rate unchanged after ten minutes.", t),
                )
            }
            DemoStep.FOLLOWUP -> { screenGrowth = 0; at(0) { it.copy(state = SessionState.BUSY, turnStarted = t, since = t, tool = "Bash", toolNote = "Update the changelog and tag the release") } }
            DemoStep.BLOG -> at(2) { it.copy(state = SessionState.BUSY, turnStarted = t, since = t, toolNote = "Draft a post about the 2.8.0 release") }
        }
    }
```

  Nel `when (cmd.op)` di `send`, sostituire i rami `LAUNCH` e `SCREEN`:

```kotlin
            CmdOp.LAUNCH -> s.projects.firstOrNull { it.path == cmd.arg }?.let { p ->
                // Demo (piano 16/09): la sessione del progetto nasce al lavoro, così la storia arriva sulla sua Scheda.
                current.value = s.copy(sessions = s.sessions.map {
                    if (it.name == p.name) it.copy(state = SessionState.BUSY, turnStarted = now(), since = now(), toolNote = cmd.text ?: it.toolNote) else it
                })
                CmdResult(cmd.id, true, "launched ${p.name} (${p.account})", now(), session = p.name)
            } ?: ko("unknown project")
            CmdOp.SCREEN -> if (ses == null) ko("no session ${cmd.session}") else {
                // Dopo il passo FOLLOWUP il terminale cresce a ogni cattura: nel video le righe arrivano dal vivo.
                val extra = listOf("Edit CHANGELOG.md", "+ ## 2.8.0 — refund endpoint, webhook retries", "$ git tag v2.8.0", "$ git push --tags", "Tag v2.8.0 pushed")
                val righe = extra.take(screenGrowth.coerceAtMost(extra.size)); screenGrowth++
                ok((listOf("$ pytest -q tests", "42 passed in 3.1s") + righe).joinToString("\n"))
            }
```

  Nota: se il ramo `LAUNCH` del `when` oggi restituisce una `String` tramite `ok(…)`, adattare restituendo un `CmdResult`
  come mostrato; `ok`/`ko` sono funzioni locali già presenti che costruiscono `CmdResult`.
- [ ] **Passo 4: verde.** Stesso comando, poi tutta la suite `:core:testDebugUnitTest`. Atteso: tutti verdi
  (erano 396 prima di questo task, ora 401).
- [ ] **Passo 5: commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/transport/FakeTransport.kt core/src/test/kotlin/it/pixelbox/cmwatch/transport/FakeTransportTest.kt
git commit -m "feat(demo): story steps for the promo video, launch that creates the session, terminal that grows"
```

---

### Task 3: pilotare la storia via adb (app)

**File:**
- Modifica: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt` (accanto a `setDemo`)
- Modifica: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt` (`demoExtra`, `write`, `launchKeyboard`, rotta QUOTA)

**Interfacce:**
- Consuma: `FakeTransport.demoStep(DemoStep)` (Task 2), `app.repo.refresh()`, `settings.demoMode`.
- Produce: extra `demo_step` (`calm|question|deployed|followup|blog`), extra `demo_dictation` (testo), rotta QUOTA che in
  demo legge `app.fake.events`.

- [ ] **Passo 1: `CmApp`** — aggiungere dopo `setDemo`:

```kotlin
    /**
     * Porta la demo a una scena della storia dei video (`--es demo_step question`). La rilettura passa dal diff degli stati,
     * lo stesso delle notifiche vere: la domanda arriva con la sua notifica anche ad app chiusa.
     */
    fun setDemoStep(name: String) {
        val step = runCatching { it.pixelbox.cmwatch.transport.DemoStep.valueOf(name.uppercase()) }.getOrNull() ?: return
        scope.launch {
            if (!prefs.current().demoMode) return@launch
            fake.demoStep(step)
            repo.refresh()
            runCatching { CmTileService.requestUpdate(this@CmApp) }; runCatching { CmComplicationService.requestUpdate(this@CmApp) }
        }
    }

    /** Il testo che la prossima «dettatura» della demo restituisce al posto della tastiera (`--es demo_dictation "…"`). */
    @Volatile var demoDictation: String? = null
```

- [ ] **Passo 2: `MainActivity.demoExtra`** — aggiungere in fondo alla funzione:

```kotlin
        intent?.getStringExtra(EXTRA_DEMO_STEP)?.let { app.setDemoStep(it) }
        intent?.getStringExtra(EXTRA_DEMO_DICTATION)?.let { app.demoDictation = it }
```

  e nel `companion object`: `const val EXTRA_DEMO_STEP = "demo_step"; const val EXTRA_DEMO_DICTATION = "demo_dictation"`.
- [ ] **Passo 3: dettatura simulata.** In `write(name)` e nell'`onWrite` di `LaunchScreen`, prima di aprire la tastiera:

```kotlin
            // Demo per i video: la «dettatura» preparata via adb va come se l'avesse restituita la tastiera.
            val dettato = app.demoDictation
            if (settings?.demoMode == true && dettato != null) {
                app.demoDictation = null
                scope.launch { sentId = app.repo.prompt(name, dettato); Haptics.play(this@MainActivity, Haptics.Kind.SENT) }
                return
            }
```

  (in `onWrite` del launch: `launch(p.path, dettato)` al posto di `app.repo.prompt`).
- [ ] **Passo 4: «Today» solo demo.** Nella rotta `Routes.QUOTA`, sostituire la lettura degli eventi:

```kotlin
                val eventiVeri by app.repo.events.collectAsStateWithLifecycle()
                val eventiDemo by app.fake.events.collectAsStateWithLifecycle(emptyList())
                // In demo «Today» conta solo gli eventi della demo: con quelli veri salvati su Room diceva 336 (16/09).
                val events = if (settings?.demoMode == true) eventiDemo else eventiVeri
```

- [ ] **Passo 5: build, installazione, verifica.** Con la demo accesa: `am start -n it.pixelbox.cmwatch/.wear.MainActivity
  --es demo_step question` ad app chiusa (`input keyevent KEYCODE_HOME` prima). Atteso: notifica di payments-api con
  vibrazione. `--es demo_dictation "Great. Now update the changelog and tag the release"`, poi tocco su «Write» nella Scheda.
  Atteso: conferma «Sent», nessuna tastiera. Panoramica: «Today» con un numero piccolo (eventi demo).
- [ ] **Passo 6: commit**

```bash
git add wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt wear/src/main/kotlin/it/pixelbox/cmwatch/wear/MainActivity.kt
git commit -m "feat(demo): story steps, simulated dictation and demo-only events over adb for the promo recordings"
```

---

### Task 4: l'orologio pronto (Franz, una volta, prima delle registrazioni)

Nessun codice. Da chiedere a Franz e verificare con uno screenshot prima di registrare.

- [ ] **Passo 1:** quadrante neutro (senza complication personali), con la complication della quota di Claude Master.
- [ ] **Passo 2:** tile di Claude Master al primo posto del carosello (a destra del quadrante).
- [ ] **Passo 2b:** lingua di sistema in inglese per le registrazioni (Franz, 16/09 22:23): la lingua per app non basta,
  i testi del sistema restano italiani («Ora» nella notifica, data del quadrante, conferme). Dopo le registrazioni si
  torna all'italiano.
- [ ] **Passo 3:** debug wireless chiuso sullo schermo dell'orologio, batteria sopra il 50 %, fuori dal caricatore.
- [ ] **Passo 4: verifica.** `record.sh setup`, poi `input keyevent KEYCODE_HOME`, screenshot; `input swipe 420 240 60 240 180`,
  screenshot. Atteso: quadrante neutro con la nostra complication; subito la nostra tile.

---

### Task 5: le scene nel copione

**File:** modifica `tools/promo/record.sh`

**Interfacce:** consuma `go`, `scroll`, `hold`, `tap`, `back`, `pause`, `snap` già presenti; `demo_step` e `demo_dictation` (Task 3).

- [ ] **Passo 1:** aggiungere gli aiuti e le scene prima di `setup()`:

```bash
step() { sh am start -n $ACT --es demo_step "$1" >/dev/null 2>&1; pause "${2:-1.5}"; }
dictate() { sh am start -n $ACT --es demo_dictation "$1" >/dev/null 2>&1; pause 0.3; }
home() { sh input keyevent KEYCODE_HOME; pause "${1:-1.5}"; }

scene_0() { step calm; home 3.0; snap quadrante; }
scene_1() {   # un'occhiata: tile, poi lista
  sh input swipe 420 240 60 240 380; pause 2.5; snap tile
  tap 240 "${Y_TILE_SESSIONS:-420}"; pause 2.4; snap lista
  scroll 260 2400; snap lista_giu
}
scene_2() {   # arriva la domanda: notifica, ascolto, risposta
  home 1.5; step question 3.5; snap notifica
  tap 240 "${Y_NOTIF:-240}"; pause 2.8; snap domanda
  tap "${X_PLAY:-400}" "${Y_PLAY:-95}"; pause 3.0; snap ascolto
  hold 240 "${Y_YES:-440}"; pause 3.0; snap risposta
}
scene_3() {   # la seguo
  go sessions; pause 2.0
  hold 240 "${Y_ROW0:-130}" 900; pause 2.5; snap seguita
}
scene_4() {   # il deploy è fatto
  home 1.0; step deployed 3.5; snap esito_notifica
  tap 240 "${Y_NOTIF:-240}"; pause 2.8; snap scheda
  tap "${X_PLAY:-400}" "${Y_PLAY_CARD:-150}"; pause 1.5
  scroll 300 2600; scroll 300 2400; snap scheda_giu
}
scene_5() {   # il passo dopo, a voce; poi il terminale dal vivo
  dictate "Great. Now update the changelog and tag the release"
  scroll 400 2000 1.0; tap 240 "${Y_WRITE:-440}"; pause 2.5; snap inviato
  step followup 1.0; go terminal/payments-api; pause 9.0; snap terminale
}
scene_6() {   # quanta quota resta
  go quota; pause 2.4; scroll 300 2600; snap ritmo; scroll 360 2400; snap lavoro
}
scene_7() {   # il post sul blog
  dictate "Draft a post about the 2.8.0 release"
  go launch; pause 2.2; tap 240 "${Y_PROJECT_BLOG:-290}"; pause 1.5
  tap 240 "${Y_WRITE_FIRST:-300}"; pause 3.5; snap sessione_nata
}
scene_8() { home 3.0; snap chiusura; }
```

- [ ] **Passo 2:** rendere le scene registrabili come le clip: in `run()` sostituire `"clip_$CLIP"` con
  `if declare -F "scene_$CLIP" >/dev/null; then "scene_$CLIP"; else "clip_$CLIP"; fi` (nei due rami).
- [ ] **Passo 3: prova delle coordinate.** `WATCH=… OUT=… tools/promo/record.sh probe 0 1 2 3 4 5 6 7 8`, tavola degli
  screenshot come nei giri precedenti; correggere `Y_*`/`X_*` finché ogni scatto mostra quello che dice il suo nome.
- [ ] **Passo 4: commit**

```bash
git add tools/promo/record.sh
git commit -m "feat(promo): the nine scenes of the 2.8.0 release storyline"
```

---

### Task 6: registrazione, pulizia, revisione nella pagina HTML (fase A)

**File:** crea `tools/promo/web.sh`; aggiorna la pagina di prova (artifact `https://claude.ai/artifact/8v3N9exfyGDoCpYNS1HuVt`,
file locale nello scratchpad della sessione; se lo scratchpad non c'è più, rileggerla con `Artifact action=read`).

- [ ] **Passo 1: `tools/promo/web.sh`** — taglio e ricodifica a fps costante di una registrazione:

```bash
#!/usr/bin/env bash
# Una registrazione di screenrecord (frequenza variabile) in una clip web a 30 fps costanti, tagliata all'inizio.
# Uso: web.sh ingresso.mp4 uscita.mp4 SECONDI_DA_TAGLIARE
set -euo pipefail
ffmpeg -hide_banner -loglevel error -y -ss "$3" -i "$1" -vf "fps=30,format=yuv420p" -fps_mode cfr -r 30 \
  -c:v libx264 -preset slow -crf 24 -movflags +faststart -an "$2"
```

- [ ] **Passo 2: registrare** `record.sh setup`, `record.sh record 0 1 2 3 4 5 6 7 8`, `record.sh teardown`.
- [ ] **Passo 3: tagli.** Per ogni scena la tavola a un fotogramma ogni mezzo secondo dei primi 8 s
  (`ffmpeg -t 8 -i scena.mp4 -vf "fps=2,scale=110:110,tile=16x1" -frames:v 1 inizio.png`), taglio dal primo fotogramma utile
  con `web.sh`.
- [ ] **Passo 4: verifica di tutta la clip** (non solo l'inizio): `fps=2` su tutta la durata; nessun quadrante personale,
  impostazioni, indirizzi, dati di salute; nessuna parola italiana.
- [ ] **Passo 5: revisione con Franz** nella pagina HTML: un pulsante per scena e un pulsante «Sequenza» che le riproduce in
  fila. Si approva scena per scena; le scene respinte tornano al Task 5.
- [ ] **Passo 6: commit** di `tools/promo/web.sh`.

---

### Task 7: montaggio con Remotion (fase B)

Solo sulle scene approvate. Remotion è già installato in `~/Desktop/workspaces/personali/video/storytelling-video`
(4.0.490, compositore arm64, chrome-headless-shell): si copia l'impianto, non si modifica l'originale.

**File:**
- Crea: `tools/promo/remotion/` (copia di `package.json`, `package-lock.json`, `remotion.config.ts`, `tsconfig.json` dal
  progetto originale; `node_modules` con `npm ci`, oppure symlink a quello originale se `npm ci` costa troppo sulla VM)
- Crea: `tools/promo/remotion/src/Root.tsx`, `src/Watch.tsx`, `src/Release.tsx`, `public/scenes/*.mp4`
- Aggiungi a `.gitignore`: `tools/promo/remotion/node_modules/`, `tools/promo/remotion/out/`, `tools/promo/remotion/public/scenes/`

**Interfacce:** le clip web del Task 6 in `public/scenes/scene_N.mp4`.

- [ ] **Passo 1: leggere il `CLAUDE.md` di storytelling-video** e le skill `.claude/skills/remotion-best-practices` e
  `remotion-render`: sono la fonte per versioni e comandi; se contraddicono questo piano, vince il progetto.
- [ ] **Passo 2: `src/Watch.tsx`** — la cassa approvata (nera opaca, vetro), stessa geometria della pagina HTML:

```tsx
import React from "react";
import { AbsoluteFill, OffthreadVideo, staticFile } from "remotion";

export const Watch: React.FC<{ src: string; d: number; startFrom?: number }> = ({ src, d, startFrom = 0 }) => (
  <div style={{ position: "relative", width: d * 1.1, height: d * 1.1, borderRadius: "50%",
    background: "radial-gradient(circle at 32% 28%, #2b2e33 0%, #0f1113 68%)",
    boxShadow: "0 30px 60px -20px rgba(0,0,0,.55), inset 0 0 0 2px #4a4f57", display: "grid", placeItems: "center" }}>
    <div style={{ position: "absolute", right: -d * 0.035, top: "50%", transform: "translateY(-50%)", width: d * 0.06,
      height: d * 0.2, borderRadius: d * 0.03, background: "linear-gradient(90deg,#0f1113,#2b2e33 60%,#0f1113)" }} />
    <div style={{ width: d * 1.02, height: d * 1.02, borderRadius: "50%", background: "#050506", display: "grid", placeItems: "center" }}>
      <div style={{ width: d, height: d, borderRadius: "50%", overflow: "hidden", position: "relative", background: "#000" }}>
        <OffthreadVideo src={staticFile(src)} startFrom={startFrom} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        <AbsoluteFill style={{ borderRadius: "50%",
          background: "radial-gradient(120% 70% at 30% 8%, rgba(255,255,255,.16) 0%, rgba(255,255,255,0) 42%)" }} />
      </div>
    </div>
  </div>
);
```

- [ ] **Passo 3: `src/Release.tsx`** — la sequenza: sfondo al tramonto, orologio centrato, zoom d'apertura con easing,
  didascalie di scena, dissolvenze tra scene. Le durate in fotogrammi vengono dalle clip approvate
  (`ffprobe -show_entries format=duration`):

```tsx
import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { TransitionSeries, linearTiming } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { Watch } from "./Watch";

export const SCENES: { file: string; seconds: number; caption?: string }[] = [
  { file: "scenes/scene_0.mp4", seconds: 3 },
  { file: "scenes/scene_1.mp4", seconds: 9, caption: "Everything at a glance" },
  { file: "scenes/scene_2.mp4", seconds: 14, caption: "Answer Claude from your wrist" },
  { file: "scenes/scene_3.mp4", seconds: 5, caption: "Follow a session" },
  { file: "scenes/scene_4.mp4", seconds: 12, caption: "A few minutes later" },
  { file: "scenes/scene_5.mp4", seconds: 12, caption: "Reply by voice, watch it work" },
  { file: "scenes/scene_6.mp4", seconds: 10, caption: "Know your quota and your pace" },
  { file: "scenes/scene_7.mp4", seconds: 10, caption: "Start a new session" },
  { file: "scenes/scene_8.mp4", seconds: 3 },
];

const Scene: React.FC<{ file: string; caption?: string }> = ({ file, caption }) => {
  const frame = useCurrentFrame();
  const { fps, width, height } = useVideoConfig();
  const zoom = interpolate(frame, [0, fps * 1.4], [1.18, 1], { extrapolateRight: "clamp", easing: Easing.out(Easing.cubic) });
  const captionIn = interpolate(frame, [fps * 0.3, fps * 0.9], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const d = Math.round(height * 0.62);
  return (
    <AbsoluteFill style={{ background: "linear-gradient(160deg,#2b3450 0%,#5a4a78 55%,#c9a8c9 100%)", alignItems: "center", justifyContent: "center" }}>
      <div style={{ transform: `scale(${zoom})` }}><Watch src={file} d={d} /></div>
      {caption ? (
        <div style={{ position: "absolute", left: width * 0.06, top: height * 0.44, maxWidth: width * 0.28, opacity: captionIn,
          transform: `translateY(${(1 - captionIn) * 16}px)`, color: "#f4f1fb", fontFamily: "Instrument Sans, system-ui, sans-serif",
          fontSize: 54, fontWeight: 600, lineHeight: 1.1 }}>{caption}</div>
      ) : null}
    </AbsoluteFill>
  );
};

export const Release: React.FC = () => {
  const { fps } = useVideoConfig();
  return (
    <TransitionSeries>
      {SCENES.flatMap((s, i) => [
        <TransitionSeries.Sequence key={`s${i}`} durationInFrames={Math.round(s.seconds * fps)}>
          <Scene file={s.file} caption={s.caption} />
        </TransitionSeries.Sequence>,
        i < SCENES.length - 1 ? (
          <TransitionSeries.Transition key={`t${i}`} presentation={fade()} timing={linearTiming({ durationInFrames: 12 })} />
        ) : null,
      ])}
    </TransitionSeries>
  );
};
```

- [ ] **Passo 4: `src/Root.tsx`**:

```tsx
import React from "react";
import { Composition } from "remotion";
import { Release, SCENES } from "./Release";

const FPS = 30;
const TRANSITIONS = (SCENES.length - 1) * 12;
export const RemotionRoot: React.FC = () => (
  <Composition id="Release" component={Release} fps={FPS} width={2400} height={1200}
    durationInFrames={SCENES.reduce((n, s) => n + Math.round(s.seconds * FPS), 0) - TRANSITIONS} />
);
```

- [ ] **Passo 5: prova di 3 secondi.** `cd tools/promo/remotion && npx remotion render Release out/prova.mp4 --frames=0-89 --concurrency=1`.
  Atteso: file di 3 s, orologio centrato con la scena 0, nessun errore di memoria. Controllo con una tavola di fotogrammi.
- [ ] **Passo 6: render completo.** `npx remotion render Release out/release.mp4 --concurrency=2` (se va in memoria, `=1`).
  GIF per il README dal ritaglio (scene 2, 4, 6):
  `ffmpeg -i out/release.mp4 -vf "select=…,fps=15,scale=960:-1,split[a][b];[a]palettegen[p];[b][p]paletteuse" out/readme.gif`
  con i tempi delle scene calcolati da `SCENES`.
- [ ] **Passo 7: verifica finale** con la checklist del piano (dati personali, scatti, sovrapposizioni, italiano) su tavola
  a un fotogramma al secondo di tutto `release.mp4`, poi invio a Franz.
- [ ] **Passo 8: commit** di `tools/promo/remotion/src/`, config e `.gitignore` (mai `node_modules`, `out`, `public/scenes`).

---

## Autoverifica del piano

- **Copertura della specifica:** sequenza (Task 5-7), prerequisiti demo 1-5b (Task 2-3), «Write» (Task 1), orologio
  (Task 4), audio (decisione aperta: se si aggiunge la voce, un passo in più nel Task 7 con `<Audio>` e un TTS esterno),
  ritagli (Task 7 passo 6), verifiche (Task 6 passo 4, Task 7 passo 7).
- **Decisioni ancora aperte** (dal piano): dettatura simulata (Task 3 la implementa; se Franz detta dal vivo si salta il
  passo 3), audio della lettura, conferma delle due fasi.
- **Nomi coerenti:** `DemoStep`/`demoStep` (Task 2) usati da `setDemoStep` (Task 3) e `step` in `record.sh` (Task 5);
  `demo_dictation`/`demoDictation` (Task 3) usati da `dictate` (Task 5).
