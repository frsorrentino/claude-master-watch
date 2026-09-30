# Telefono: attenzione, quota e widget — piano

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** la coda «Ti aspettano», lo swipe fra le sessioni, l'avviso della quota con «Invia alla ripartenza» e «Manda
stanotte», la ricerca, le frasi rapide, e il widget personalizzabile nello stile di ads-widget.

**Architecture:** regole in `core` con TDD (`AttentionQueue`, `QuotaWarning`, `ChatRules` con lo stato programmato,
`ChatSearch`, `QuickPhrases`, `WidgetModel`); in `mobile` le schermate, un `Worker` per l'invio programmato e il widget in
Jetpack Glance 1.2.0. Niente dal relay: le parti che ne dipendono (barra fable-director, «Al lavoro» con il budget,
Scrivania, file) hanno il loro piano dopo le rispettive richieste.

**Tech Stack:** Kotlin, Compose Material 3 1.5.0-alpha18, WorkManager, Glance `glance-appwidget` e `glance-material3`
1.2.0, JUnit 4, Paparazzi in CI.

**Spec:** `docs/plans/2026-09-30-telefono-panoramica-scrivania-widget-design.md` (approvata in chat, «Prosegui» dopo
l'invio alle 21:24).

## Global Constraints

- Ogni dato ha una sola casa (tabella della specifica); un solo bottone pieno per schermata; mai «…» nel corpo; ▶ sopra
  120 caratteri e su esiti, risposte e domande; testi in `strings.xml` it ed en; colori solo `CmColors`.
- In locale `:core:testDebugUnitTest` e `:mobile:compileDebugUnitTestKotlin`; Paparazzi e build in CI.
- Commit in inglese, `git add` per nome di file.

## Review Focus

1. **Invio programmato con il telefono spento o senza rete all'ora giusta:** parte appena possibile, una volta sola, e la
   chat lo dice. Test `scheduledSendsOnceWhenLate` in Task 4.
2. **Coda con una domanda risposta al PC mentre è aperta:** la coda salta alla successiva, non risponde a una domanda
   sparita. Test `answeredElsewhereLeavesTheQueue` in Task 1.
3. **Quota senza lettura delle 5 ore o dato vecchio:** nessun avviso inventato. Test `noWarningWithoutAFreshReading` in
   Task 3.
4. **Widget con lo stato non ancora arrivato:** disegna il vuoto con «in attesa del PC», non si ferma. Test
   `widgetWithoutStateSaysWaiting` in Task 7.
5. **Ricerca con accenti e maiuscole:** «perche» trova «perché». Test `accentsAndCaseIgnored` in Task 5.

---

### Task 1: coda «Ti aspettano» (`AttentionQueue`)

**Files:** Create `core/.../rules/AttentionQueue.kt` e test; `mobile/.../ui/QueueScreen.kt`; Modify `OverviewScreen.kt`
(la card «Domande aperte» diventa «Ti aspettano», tocco = coda), `MainActivity.kt`, stringhe; snapshot `QueueScreenTest`.

**Interfaces — Produces:**
```kotlin
object AttentionQueue {
    data class Item(val session: String, val questionId: String, val askedAt: Long)
    fun items(state: State): List<Item>                 // domande aperte, dalla più vecchia
    fun next(state: State, current: String?): Item?     // la successiva dopo `current` (id domanda), o la prima
}
```
- [ ] Test: `oldestFirst`, `nextAfterCurrent`, `answeredElsewhereLeavesTheQueue` (la domanda corrente non c'è più →
  `next` dà la più vecchia rimasta), `emptyWhenNoQuestions`. RED → implementa → GREEN.
- [ ] `QueueScreen`: `HorizontalPager` sulle voci; ogni pagina = testata della sessione (badge, nome) e la card della
  domanda di `SessionSheet` (stesse regole: prima opzione piena, pressione lunga per il rischio alto); dopo una risposta
  passa alla successiva; vuota = «Nessuno ti aspetta». Si apre dalla card e dall'intent delle notifiche con domanda.
- [ ] Commit `feat(phone): the waiting queue — answer every open question in a row`.

### Task 2: swipe fra le sessioni

**Files:** Modify `MainActivity.kt` (la scheda in un `HorizontalPager` sulle sessioni vive nell'ordine di `PhoneBoard`),
`AppShell` (il menu segue la pagina); snapshot invariati.
- [ ] Pager con `key = name`; la pagina corrente aggiorna `open`; il volo card → scheda resta sulla prima apertura.
- [ ] Compilazione → OK. Commit `feat(phone): swipe between sessions in the sheet, in board order`.

### Task 3: avviso quota (`QuotaWarning`)

**Files:** Create `core/.../rules/QuotaWarning.kt` e test; Modify `SessionSheet.kt` (riga sopra la barra), stringhe.

**Interfaces — Produces:**
```kotlin
object QuotaWarning {
    data class Warn(val account: String, val pct: Int, val resetAt: Long, val projected: Boolean)
    fun of(state: State, session: Session, samples: List<QuotaHistory.Sample>, now: Long): Warn?
}
```
Regola: l'account della sessione (`Accounts`); avviso se `h5 >= 90`, o se `QuotaHistory.pace(...).projected >= 100`;
mai con `stale`, senza `h5` o senza `resetH5` futuro.
- [ ] Test: `warnsAtNinety`, `warnsWhenThePaceRunsOut`, `noWarningWithoutAFreshReading`, `otherAccountDoesNotWarn`.
- [ ] Riga «finestra al 94 %, riparte alle 21:50» con due bottoni testuali «Invia alla ripartenza» e «Manda stanotte»;
  tocco sulla riga = Panoramica. Commit `feat(phone): quota warning above the send bar`.

### Task 4: invio programmato e «Manda stanotte»

**Files:** Modify `core/.../rules/ChatRules.kt` (`Sent.scheduledFor: Long?`, `Status.SCHEDULED`), test; Create
`mobile/.../ScheduledSend.kt` (Worker); Modify `MainActivity.kt`, `SessionSheet.kt`, `OverviewScreen.kt` (segno
sull'anello all'ora dell'invio), dipendenza `androidx.work:work-runtime-ktx`.
- [ ] Test `scheduledIsItsOwnStatus`, `scheduledSendsOnceWhenLate` (una regola `ChatRules.due(list, now)` che dà i
  messaggi da mandare e non li ridà dopo `sentAt` aggiornato).
- [ ] Worker unico periodico + uno esatto all'ora: manda `prompt` con lo stesso testo, aggiorna il `Sent` con l'id vero.
- [ ] «Manda stanotte»: `night_add` con la cartella del progetto della sessione (da `state.projects`), il messaggio va
  nella card «Notte». Commit `feat(phone): send at the quota reset or tonight`.

### Task 5: ricerca (`ChatSearch`)

**Files:** Create `core/.../rules/ChatSearch.kt` e test; `mobile/.../ui/SearchScreen.kt`; icona in `AppShell`.
- [ ] Test `findsInSentAndOutcomes`, `findsInDiaryEvents`, `accentsAndCaseIgnored`, `newestFirst`.
- [ ] Schermata: campo, risultati con sessione, ora e riga trovata in grassetto; tocco = scheda della sessione.
  Commit `feat(phone): search across chats and the diary`.

### Task 6: frasi rapide (`QuickPhrases`)

**Files:** Create `core/.../rules/QuickPhrases.kt` e test; Modify `SessionSheet.kt` (chip sopra la barra).
- [ ] Test `topThreeByUseForTheProject`, `ignoresOneOffs` (almeno due usi), `noDuplicatesOfTheSuggestion`.
- [ ] Chip tonali sopra la barra: tocco = manda, pressione lunga = nel campo. Commit `feat(phone): quick phrases per project`.

### Task 7: widget

**Files:** Create `core/.../rules/WidgetModel.kt` e test; `mobile/.../widget/CmWidget.kt` (Glance),
`WidgetConfigActivity.kt`, `res/xml/cm_widget_info.xml`; Modify `AndroidManifest.xml`, `PhoneApp.kt` (aggiorna i widget a
ogni stato), `gradle/libs.versions.toml` e `mobile/build.gradle.kts` (Glance 1.2.0), stringhe.

**Interfaces — Produces:**
```kotlin
object WidgetModel {
    enum class Mode { ACCOUNT, SESSION, BOARD }
    enum class Metric { WEEK, WORKING, WAITING, IDLE, NIGHT, CONTEXT, TURN_AGE, OUTCOME }
    data class Config(val mode: Mode, val target: String?, val metrics: List<Metric>, val opacity: Int, val corners: Int, val mono: Boolean)
    data class Card(val title: String, val arcPct: Int?, val arcLabel: String, val columns: List<Pair<Metric, String>>, val updatedAt: Long?)
    fun cards(state: State?, config: Config, now: Long): List<Card>
}
```
- [ ] Test: `accountArcIsTheFiveHourQuota`, `sessionArcIsTheContext`, `boardCountsWaitingWorkingIdle`,
  `widgetWithoutStateSaysWaiting`, `metricsKeepTheirOrder`.
- [ ] Glance: una card per riga come ads-widget (fondo scuro traslucido, arco a sinistra, tre colonne, testata ▸ nome, ora,
  ↻); `SizeMode.Responsive` per striscia, piccola, media, grande; tocco = Panoramica o scheda; ↻ = `repo.refresh()`.
- [ ] Configurazione alla posa: modo, account o sessione, colonne, opacità, angoli, monocromo.
- [ ] Commit `feat(phone): home screen widget in the ads-widget style, configurable`.

### Task 8: registrazione, provini, revisione

- [ ] Push, `build-android.yml` con `record=true`, due passaggi sui provini, revisione finale del ramo, correzioni degli
  Important con test, immagini committate, APK sul telefono, provini a Franz; righe nella checklist dal vivo (coda,
  swipe, avviso quota e invio programmato, ricerca, frasi rapide, widget).
