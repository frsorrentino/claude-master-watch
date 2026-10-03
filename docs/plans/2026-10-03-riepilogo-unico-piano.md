# Riepilogo unico — piano di realizzazione

> **Per chi esegue:** sotto-skill obbligatoria superpowers:executing-plans (o subagent-driven-development). Passi con
> casella `- [ ]` da spuntare. TDD su ogni pezzo di `core`; le schermate si verificano con i provini Paparazzi in CI.

**Obiettivo:** fondere la casa della master e la scheda «Sessioni» in una schermata sola, «Sessioni · N aperte», con
ogni sessione una volta nell'ordine del bisogno, la master agganciata sopra il campo di scrittura e il menu ridisegnato.

**Architettura:** una regola pura in `core` (`Summary.build`) decide gruppi, righe, chiuse e righe di servizio dallo
stato del relay; l'app la disegna riusando `AttentionRow` (già usata da «Per te») e la barra di scrittura della chat
della master (`SessionSheet(home = …)`), con due nuovi pezzi: il riquadro della master in basso e il pannello del menu.
Spariscono la barra delle schede, il pager fra le schede, `SessionsScreen` e il bottone mobile «Lancia».

**Stack:** Kotlin, Jetpack Compose (Material 3 Expressive), JUnit in `core`, Paparazzi in `mobile`.

**Design:** `docs/plans/2026-10-03-riepilogo-unico-design.md` (leggerlo prima: le decisioni non si ridiscutono).

## Vincoli globali

- Testi visibili in `mobile/src/main/res/values/strings.xml` (italiano) e `values-en/strings.xml`; mai nel Kotlin.
- Una riga logica occupa la riga fisica; mai «…» nei testi (si taglia con `TextOverflow.Clip`).
- Un solo bottone pieno per schermata (la prima opzione di una domanda aperta, o Invia).
- Icone di stato solo dai tracciati di `Badge.paths` / drawable `ic_hand`, `ic_flag`, `ic_w_working`, `ic_w_idle`; ▶ solo
  per la lettura vocale.
- Comandi: test di core `./gradlew -q :core:testDebugUnitTest`; compilazione `./gradlew -q :mobile:compileDebugUnitTestKotlin`.
  Un solo daemon Gradle (VM da 6 GB); la prima build del giorno impiega 5-10 minuti: lanciarla in background.
- Provini: push, annullare il run del push, `gh workflow run build-android.yml --ref feature/telefono -f record=true`,
  scaricare `paparazzi-snapshots`, GUARDARE i cambiati, committarli per nome. Push solo con l'ok di Franz.
- Installazione: verificare `adb -s <ip:porta> shell getprop ro.product.model` = «Pixel 11 Pro XL» prima di installare
  (in rete c'è anche l'orologio). Comando adb: `qemu-x86_64-static ~/android-sdk/platform-tools/adb`.
- Commit in inglese, file per nome, mai `git add -A`.

## Attenzione in revisione

1. **Master chiusa** (nessuna sessione `master` viva): il riepilogo funziona; al posto del riquadro della master c'è
   «Riapri la master» (`MasterAbsent`) sopra il campo, che resta spento. Test in Task 1 (`noMasterStillBuilds`).
2. **La master aspetta una domanda**: compare in «Ti aspetta» (non solo nel riquadro in basso). Test in Task 1.
3. **Una sessione ferma con un turno finito recente** sta in «Ha finito», non anche in «Ferme». Test in Task 1.
4. **Indietro da una chat** torna al riepilogo, non a una scheda che non esiste più (oggi dalla chat della master si
   finiva su Sessioni). Verifica dal vivo in Task 7.
5. **Molte sessioni (10 o più)**: «Ferme» resta una riga breve ciascuna e «Chiuse» una riga sola. Test in Task 1
   (`closedAreOneRow`) e provino in Task 3.

---

### Task 1: la regola del riepilogo in core

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/Summary.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/SummaryTest.kt`

**Interfacce:**
- Usa: `MasterHome.forYou(state, events, sent, now, zone, read, limit)` (righe FINISHED e di servizio),
  `MasterHome.working(state)`, `ContextActions.MASTER`.
- Produce:
  ```kotlin
  object Summary {
      enum class Group { WAITING, FINISHED, WORKING, STILL }
      data class Row(val group: Group, val session: Session, val text: String?, val at: Long?, val key: String? = null)
      data class Model(val rows: List<Row>, val closed: List<Session>, val service: List<MasterHome.Row>, val open: Int, val master: Session?)
      fun build(state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String>): Model
  }
  ```

- [ ] **Step 1: test che falliscono** — `SummaryTest.kt`:

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.Summary.Group
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Il riepilogo unico (design 03/10): ogni sessione una volta, nell'ordine del bisogno. */
class SummaryTest {
    private val zone = ZoneId.of("Europe/Rome")
    private fun at(h: Int, m: Int = 0) = LocalDate.of(2026, 10, 3).atTime(h, m).atZone(zone).toEpochSecond()
    private fun s(name: String, st: SessionState = SessionState.IDLE, since: Long = at(9)) =
        Session(id = name, name = name, account = "personale", project = name, state = st, since = since)
    private fun q(id: String, asked: Long) = Question(id, QuestionKind.ASK, "Pubblico?", emptyList(), Tier.LOW, asked)
    private fun done(at: Long) = Outcome("Fatto", "Test verdi.\nProssimi: tagga · apri la PR", at)
    private fun st(vararg ss: Session) = State(v = 1, ts = at(15), host = "pc", sessions = ss.toList())
    private fun build(state: State, sent: List<Sent> = emptyList()) = Summary.build(state, emptyList(), sent, at(15), zone, emptySet())

    @Test fun groupsInOrderOfNeed() {
        val m = build(st(
            s("idle"), s("busy", SessionState.BUSY),
            s("asks", SessionState.WAITING).copy(question = q("1", at(14))),
            s("fin").copy(outcome = done(at(14, 50)), followed = true),
        ))
        assertEquals(listOf(Group.WAITING, Group.FINISHED, Group.WORKING, Group.STILL), m.rows.map { it.group })
        assertEquals(listOf("asks", "fin", "busy", "idle"), m.rows.map { it.session.name })
    }

    @Test fun aFinishedSessionIsNotAlsoStill() {
        val m = build(st(s("fin").copy(outcome = done(at(14, 50)), followed = true)))
        assertEquals(listOf(Group.FINISHED), m.rows.map { it.group })
    }

    @Test fun theMasterOnlyAppearsWhenItAsks() {
        assertTrue(build(st(s("master", SessionState.BUSY))).rows.isEmpty())
        assertEquals(listOf("master"), build(st(s("master", SessionState.WAITING).copy(question = q("1", at(14))))).rows.map { it.session.name })
    }

    @Test fun closedAreOneRow() {
        val m = build(st(s("a"), s("x", SessionState.GONE), s("y", SessionState.GONE)))
        assertEquals(listOf("x", "y"), m.closed.map { it.name })
        assertTrue(m.rows.none { it.session.state == SessionState.GONE })
    }

    @Test fun openCountsLiveSessionsWithoutTheMaster() = assertEquals(2, build(st(s("a"), s("b"), s("master"), s("x", SessionState.GONE))).open)

    @Test fun stillMostRecentFirst() =
        assertEquals(listOf("new", "old"), build(st(s("old", since = at(8)), s("new", since = at(12)))).rows.map { it.session.name })

    @Test fun noMasterStillBuilds() {
        val m = build(st(s("a")))
        assertNull(m.master); assertEquals(1, m.rows.size)
    }

    @Test fun serviceRowsAreTheOtherKinds() {
        val state = st(s("a")).copy(night = Night(items = emptyList()))
        val m = Summary.build(state, emptyList(), emptyList(), at(21), zone, emptySet())
        assertEquals(listOf(MasterHome.Kind.NIGHT), m.service.map { it.kind })
    }
}
```

- [ ] **Step 2: eseguire e vedere fallire** — `./gradlew -q :core:testDebugUnitTest --tests '*SummaryTest'`. Atteso:
  `Unresolved reference 'Summary'`.

- [ ] **Step 3: implementazione minima** — `Summary.kt`:

```kotlin
package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.ZoneId

/**
 * Il riepilogo unico (design 03/10, approvato da Franz): Sessioni e casa della master fuse. Ogni sessione una volta,
 * nell'ordine del bisogno: ti aspetta, ha finito, al lavoro, ferme; le chiuse a parte; le righe di servizio in fondo.
 * La master è la cornice (in basso, vicino al campo) e compare nella lista solo quando aspetta.
 */
object Summary {
    enum class Group { WAITING, FINISHED, WORKING, STILL }
    data class Row(val group: Group, val session: Session, val text: String?, val at: Long?, val key: String? = null)
    data class Model(val rows: List<Row>, val closed: List<Session>, val service: List<MasterHome.Row>, val open: Int, val master: Session?)

    private val SERVICE = setOf(MasterHome.Kind.CONTEXT, MasterHome.Kind.NIGHT_REPORT, MasterHome.Kind.NIGHT, MasterHome.Kind.NEXT_STEP, MasterHome.Kind.SCHEDULED)

    fun build(state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String>): Model {
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val master = live.firstOrNull { it.name == ContextActions.MASTER }
        val others = live.filter { it.name != ContextActions.MASTER }
        val forYou = MasterHome.forYou(state, events, sent, now, zone, read, limit = Int.MAX_VALUE).rows
        val waiting = live.filter { it.question != null }.sortedBy { it.question!!.askedAt }
            .map { Row(Group.WAITING, it, it.question!!.text, it.question!!.askedAt) }
        val finished = forYou.filter { it.kind == MasterHome.Kind.FINISHED }
            .mapNotNull { r -> others.firstOrNull { it.name == r.session }?.let { Row(Group.FINISHED, it, r.detail, r.at, r.key) } }
        val working = MasterHome.working(state).map { Row(Group.WORKING, it.session, it.detail, it.session.turnStarted ?: it.session.since) }
        val taken = (waiting + finished + working).map { it.session.name }.toSet()
        val still = others.filter { it.state == SessionState.IDLE && it.name !in taken }.sortedByDescending { it.since }
            .map { Row(Group.STILL, it, it.outcome?.full, it.since) }
        return Model(
            rows = waiting + finished + working + still,
            closed = state.sessions.filter { it.state == SessionState.GONE },
            service = forYou.filter { it.kind in SERVICE },
            open = others.size,
            master = master,
        )
    }
}
```

- [ ] **Step 4: eseguire e vedere passare** — stesso comando; poi l'intera suite di core (`./gradlew -q :core:testDebugUnitTest`).
- [ ] **Step 5: commit** — `git add core/src/main/kotlin/it/pixelbox/cmwatch/rules/Summary.kt core/src/test/kotlin/it/pixelbox/cmwatch/rules/SummaryTest.kt`
  e `git commit -m "feat(core): Summary — one row per session in order of need, closed apart, service rows at the end"`.

### Task 2: le rotte senza la scheda Sessioni

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/StartRoute.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/StartRouteTest.kt`

**Interfacce:** produce `StartRoute.Tab { OVERVIEW, DIARY }` (OVERVIEW = il riepilogo; il nome resta per lo stato
salvato); `StartRoute.swipe` sparisce (non più usata).

- [ ] **Step 1:** nel test togliere `swipeMovesBetweenTheThreeTabs` e aggiungere:
```kotlin
    // Design 03/10: il riepilogo unico prende il posto di Master e Sessioni; resta il Registro, aperto dal menu.
    @Test fun onlyTheSummaryAndTheRegister() = assertEquals(listOf(StartRoute.Tab.OVERVIEW, StartRoute.Tab.DIARY), StartRoute.Tab.entries.toList())
```
- [ ] **Step 2:** eseguire `--tests '*StartRouteTest'`: fallisce (SESSIONS c'è ancora).
- [ ] **Step 3:** in `StartRoute.kt` `enum class Tab { OVERVIEW, DIARY }` e togliere `swipe`. La compilazione di `mobile`
  si romperà dove usa `Tab.SESSIONS`: si sistema in Task 7 (in questo task solo core).
- [ ] **Step 4:** test di core verdi.
- [ ] **Step 5: commit** — `feat(core): two routes left, the summary and the register`.

### Task 3: la lista del riepilogo

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SummaryList.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/MasterHome.kt` (`AttentionRow`: varianti e dettagli; `ForYouRow` da `private` a `internal`)
- Modify: `mobile/src/main/res/values/strings.xml`, `values-en/strings.xml`
- Test: `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SummaryListTest.kt`

**Interfacce:**
- Usa: `Summary.Model`, `Summary.Row`, `AttentionRow`, `QuestionOptions`, `ForYouRow`.
- Produce:
  ```kotlin
  @Composable fun SummaryList(
      model: Summary.Model, now: Long, onOpen: (String) -> Unit, onAnswer: (String, Int) -> Unit, onStep: (String, String) -> Unit,
      onService: (MasterHome.Row) -> Unit, onClosed: () -> Unit, initiallyOpen: String? = null,
  )
  ```

- [ ] **Step 1: stringhe** (it / en):
  `summary_title` «Sessioni» / «Sessions»; `summary_open` «%1$d aperte» / «%1$d open»;
  `summary_waiting` «Ti aspetta · %1$d» / «Waiting for you · %1$d»; `summary_finished` «Ha finito · %1$d» / «Finished · %1$d»;
  `summary_working` «Al lavoro · %1$d» / «Working · %1$d»; `summary_still` «Ferme · %1$d» / «Idle · %1$d»;
  `summary_closed` «Chiuse · %1$d» / «Closed · %1$d»; `summary_since` «da %1$s» / «for %1$s».
- [ ] **Step 2: `AttentionRow`** — al posto di `working: Boolean` un parametro `variant: Summary.Group` (WAITING mano ambra,
  FINISHED bandierina verde, WORKING fulmine azzurro, STILL pausa grigia `ic_w_idle` e «· da 2 h»); nuovo parametro
  `details: List<String> = emptyList()`, mostrato da aperta sotto il testo in `bodySmall` `CmColors.text2`, una riga per
  voce. I chiamanti di oggi (`ForYouCard`) passano `Summary.Group.WAITING/FINISHED/WORKING` invece di `working`.
- [ ] **Step 3: `SummaryList`** — per ogni gruppo non vuoto l'intestazione (stile di `WorkingHeader`, colore del gruppo,
  testo `summary_*` maiuscolo con il numero) e le righe `AttentionRow`; una riga aperta alla volta (`rememberSaveable`,
  chiave `group:nome`, iniziale `initiallyOpen` per i provini). I dettagli di una riga aperta:
  `SessionsText.goalLine`, `SessionsText.priority`, e «`ModelText.short(model)` · effort · contesto% · account».
  Se `model.closed` non è vuota, la riga «Chiuse · N» (croce grigia, freccia a destra) chiama `onClosed`. In fondo,
  con un filo sopra, le righe di servizio con `ForYouRow`.
- [ ] **Step 4: provini** in `SummaryListTest` (stesso impianto di `SessionSheetTest`, fixture `state-1-question.json`):
  `summaryClosed` (tutte chiuse) e `summaryOpen` (`initiallyOpen = "WAITING:ledger-api"`), più `summaryMany` con 10
  sessioni ferme finte (copie della fixture con nomi diversi) per l'attenzione n. 5.
- [ ] **Step 5:** `./gradlew -q :mobile:compileDebugUnitTestKotlin` verde; commit
  `feat(phone): SummaryList — groups in order of need, one open row at a time, details of the old cards`.

### Task 4: la master in basso

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/MasterDock.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionSheet.kt` (parametri `dock` e `header`)
- Modify: strings (`dock_master` «MASTER · %1$s · %2$s · %3$d%%» / «MASTER · …»; `dock_listen` «Ascolta l'ultimo esito
  della master»; `dock_conversation` «Apri la conversazione della master»; `master_placeholder` «Scrivi alla master» /
  «Write to the master»)
- Test: provino `dockMaster` e `dockAbsent` in `SummaryListTest`

**Interfacce:**
- Produce: `@Composable fun MasterDock(master: Session, hero: MasterHome.Hero?, onSpeak: () -> Unit, onConversation: () -> Unit)`;
  in `SessionSheet` i parametri `header: Boolean = true` (false nel riepilogo: niente pillole modello/effort in alto) e
  `dock: (@Composable () -> Unit)? = null`, disegnato fra il contenuto e la barra di scrittura, fermo (non scorre).

- [ ] **Step 1:** `MasterDock`: riga con `SessionBadge(master, 20.dp)`, colonna (etichetta `dock_master` con ora
  dell'esito, `ModelText.short`, contesto; sotto `hero.headline` su una riga, `Clip`), tondo ▶ (`FilledTonalIconButton`,
  44 dp, `Icons.Rounded.PlayArrow`, descrizione `dock_listen`) e tondo conversazione (icona fumetto, `dock_conversation`).
  Fondo `CmColors.surfaceLow`, angoli in alto 26 dp, filo `CmColors.line` sopra.
- [ ] **Step 2:** `SessionSheet`: `if (header) SheetHeader(…)`; prima della barra di scrittura `dock?.invoke()`; con
  `home != null` il segnaposto del campo diventa `master_placeholder`.
- [ ] **Step 3:** provini `dockMaster` (fixture idle rinominata «master», hero dal test di `masterHomeA`) e `dockAbsent`
  (`MasterAbsent` al posto del riquadro). Compilazione verde; commit `feat(phone): the master docked above the input`.

### Task 5: il menu ridisegnato

**Files:**
- Create: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AppMenuPanel.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AppShell.kt` (`AppMenu` apre il pannello)
- Modify: strings (`menu_connected` «Collegato a %1$s · %2$s» / «Connected to %1$s · %2$s»; `menu_launch` «Lancia una
  sessione» + `menu_launch_sub` «Un progetto, con il primo messaggio»; `menu_register` «Registro» + `menu_register_sub`
  «Stanotte, i giorni, la coda della notte»; `menu_quadro` «Quadro e quota» + `menu_quadro_sub` «Finestre di 5 ore e
  settimana, ritmo»; `menu_search` «Cerca nelle conversazioni» + `menu_search_sub` «Messaggi, esiti e registro»)
- Test: provino `menuOpen` in `ShellScreensTest`

**Interfacce:**
- Produce: `@Composable fun AppMenuPanel(host: String?, updated: String, stale: Boolean, onLaunch: () -> Unit, onRegister: () -> Unit, onQuadro: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit, onDismiss: () -> Unit)`.

- [ ] **Step 1:** pannello in un `Popup` allineato in alto (sotto la barra, margini 12 dp) con lo scrim
  (`Color.Black.copy(alpha = 0.55f)` a tutto schermo, tocco = chiudi): `Surface` angoli 28 dp, `CmColors.surface`,
  ombra. In testa: punto verde (ambra se `stale`), `menu_connected`, tondo ✕. Quattro voci alte almeno 64 dp: icona in
  tondo 40 dp su fondo tonale (`CmColors.surfaceHigh`; la prima su `CmColors.accent` al 25%), titolo `titleMedium`,
  spiegazione `bodySmall` `text2`. Filo, poi Impostazioni (icona senza fondo, testo `text2`). Indietro chiude.
- [ ] **Step 2:** `AppMenu` (≡ in `AppShell`) apre `AppMenuPanel` al posto del `DropdownMenu`.
- [ ] **Step 3:** provino `menuOpen` (parametro `startOpen` di `AppMenu` solo per i provini, come `LaunchFab`).
  Commit `feat(phone): the app menu as a panel with the link state and explained items`.

### Task 6: il guscio senza schede

**Files:**
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AppShell.kt`
- Delete: `LaunchFab` (stesso file) e il suo uso in `MainActivity`
- Test: `ShellScreensTest` (togliere `shellSessions`, `shellFabOpen`; aggiornare gli altri)

- [ ] **Step 1:** togliere `bottomBar` e il pager delle schede; il contenuto è `content(tab)` dentro il
  `PullToRefreshBox`. Senza scheda aperta la barra in alto mostra «Sessioni» (`summary_title`, `titleLarge`) e
  `summary_open` in `text2` al posto di `SessionMenu`; con una scheda aperta resta `SessionMenu`.
- [ ] **Step 2:** sotto la barra, senza scheda aperta, `QuotaBars(rings, onOpen = onQuadro)` in una riga (nuovo
  parametro `quota: (@Composable () -> Unit)?` di `AppShell`).
- [ ] **Step 3:** compilazione di `mobile` ancora rossa fino a Task 7; commit insieme a Task 7.

### Task 7: MainActivity e verifica

**Files:**
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt`
- Delete: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/SessionsScreen.kt`,
  `mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/SessionsScreensTest.kt` e i suoi png in `mobile/src/test/snapshots/images/`
  (`git rm` per nome); in `SessionCard.kt` togliere la funzione `SessionCard` se `grep -rn "SessionCard(" mobile/src` non
  trova altri usi (restano `StatePill`, `ContextBar`, `stateColor`, usati altrove).
- Create: `docs/verifiche/riepilogo-unico.md`

- [ ] **Step 1:** ovunque `tab = StartRoute.Tab.SESSIONS; open = n` diventa `open = n` (righe ~166, 243, 254, 256,
  355-367, 522-523, 566, 603, 659, 666 al 03/10). `onPick` del menu in alto: la master apre la sua chat (`open = master`).
- [ ] **Step 2:** la pagina senza scheda aperta: `sessionPage(master, masterEntries, home)` con `home` =
  `SummaryList(Summary.build(...), …)`, `header = false`, `dock = { MasterDock(...) }`; senza master la lista in una
  `Column` con `MasterAbsent` in basso. `onClosed` apre un `ModalBottomSheet` con le chiuse e «Riapri» (`CmdOp.REOPEN`).
  `onService` = l'attuale `forYouAction`. Via `ForYouCard` dalla casa (resta il componente, usato da `SummaryList` per
  le righe di servizio) e via `RunningList` se rimasto.
- [ ] **Step 3:** Registro: `diaryOpen` come `settingsOpen` (schermo intero, `BackHandler` che chiude); il menu lo apre.
  Lancia, Quadro, Cerca, Impostazioni dal pannello del Task 5.
- [ ] **Step 4:** `./gradlew -q :core:testDebugUnitTest :mobile:compileDebugUnitTestKotlin` verde.
- [ ] **Step 5:** `docs/verifiche/riepilogo-unico.md`, checklist dal vivo (una riga ciascuna): apertura dell'app sul
  riepilogo; una domanda in «Ti aspetta» risposta da lì; rischio alto chiede la pressione lunga; risposta rapida
  mandata a una sessione al lavoro; «Chiuse» e Riapri; ▶ e conversazione dal riquadro della master; menu: Registro,
  Lancia, Quadro, Cerca, Impostazioni; Indietro da una chat torna al riepilogo; master chiusa: «Riapri la master».
- [ ] **Step 6:** commit `feat(phone): the summary replaces home and Sessions — no tab bar, master docked, register from the menu`
  (file per nome, compresi i `git rm`). Chiedere l'ok di Franz al push; poi provini (Vincoli globali), controllo di
  ogni provino cambiato, commit dei png, installazione sul telefono verificato, checklist dal vivo con Franz.
