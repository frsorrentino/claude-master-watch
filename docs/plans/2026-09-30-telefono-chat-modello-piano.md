# Telefono: invio, modello ed effort, chat, «Lancia», riaccoppiamento — piano

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** la barra di scrittura sopra la tastiera, modello ed effort cambiabili, i messaggi inviati come chat con
stato ed esito, il completamento nella ricerca di «Lancia» e un «Accoppia di nuovo» che non sembri un «Salva».

**Architecture:** le regole in `core`, con TDD: `State.choices`, `LaunchSuggest.ranked`, `ChatRules` (stati e passaggi
dei messaggi) e `ChatLog` (i messaggi su file, 7 giorni). In `mobile` le schermate. Il contratto non cambia.

**Tech Stack:** Kotlin, kotlinx.serialization, Compose Material 3 1.5.0-alpha18, JUnit 4, Paparazzi in CI.

**Spec:** `docs/plans/2026-09-30-telefono-chat-modello-design.md`.

## Global Constraints

- Un solo bottone pieno per schermata; mai «…» nel corpo; una riga logica su una riga fisica; ▶ sopra 120 caratteri e
  su esiti, risposte e domande.
- Testi solo in `mobile/src/main/res/values/strings.xml` e `values-en/strings.xml`; colori solo `CmColors`.
- Paparazzi solo in CI (`build-android.yml`, `record=true`); in locale `:core:testDebugUnitTest` e
  `:mobile:compileDebugUnitTestKotlin` (la VM uccide il daemon con `assembleDebug`).
- Commit in inglese, `git add` per nome di file.

## Review Focus

1. **Orologi diversi fra telefono e PC:** `sentAt` è del telefono, `turnStarted` del PC; qualche secondo di scarto non
   deve lasciare un messaggio «in coda» per sempre. Test `smallClockSkewStillCountsTheTurn` in Task 3.
2. **Turno velocissimo, mai visto in «lavora»:** fra due stati la sessione parte e finisce; il messaggio deve passare a
   «elaborato» con l'esito. Test `fastTurnSeenOnlyByItsOutcome` in Task 3.
3. **Esito vecchio:** l'esito della sessione di prima dell'invio non va agganciato al messaggio. Test
   `outcomeOfAnEarlierTurnIsNotAttached` in Task 3.
4. **File della chat rovinato o assente:** l'app parte con la chat vuota, non si ferma. Test `corruptFileStartsEmpty`
   in Task 4.
5. **Ricerca con maiuscole e spazi:** «  Atlas » trova `atlas-shop`. Test `caseAndSpacesIgnored` in Task 2.

---

### Task 1: `State.choices` (contratto 1.12)

**Files:** Modify `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt`; Test
`core/src/test/kotlin/it/pixelbox/cmwatch/contract/ChoicesTest.kt`.

**Interfaces — Produces:**
```kotlin
@Serializable data class Choices(val models: List<Model> = emptyList(), val efforts: List<String> = emptyList())
// in State: val choices: Choices? = null
```

- [ ] **Step 1, test:**
```kotlin
class ChoicesTest {
    @Test fun choicesReadFromTheFixture() {
        val c = ContractJson.decodeState(Fixtures.stateQuestion).choices!!
        assertEquals("claude-fable-5-1", c.models[1].id)
        assertEquals(listOf("low", "medium", "high", "xhigh", "max"), c.efforts)
    }
    @Test fun olderRelayHasNoChoices() =
        assertNull(ContractJson.decodeState("""{"v":1,"ts":1,"host":"h"}""").choices)
}
```
  `./gradlew :core:testDebugUnitTest --tests '*ChoicesTest'` → FAIL (`Unresolved reference 'choices'`).
- [ ] **Step 2:** aggiungi `Choices` e `val choices: Choices? = null` in fondo a `State`, con il KDoc «Contratto 1.12».
  Stesso comando → PASS.
- [ ] **Step 3:** commit `feat(core): read model and effort choices from the state (contract 1.12)`.

### Task 2: `LaunchSuggest.ranked`

**Files:** Modify `core/src/main/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggest.kt`; Test
`core/src/test/kotlin/it/pixelbox/cmwatch/rules/LaunchSuggestTest.kt` (aggiungi).

**Interfaces — Produces:**
```kotlin
/** `account` null = tutti e due gli account. */
fun ranked(state: State, typed: String, account: String? = null, limit: Int = 6): List<Project>
```

- [ ] **Step 1, test** (stato costruito nel test con `Project(path, name, account, lastUsed)`):
  - `prefixBeforeContains`: «at» → `atlas-shop` prima di `data-tools` anche se `data-tools` è più recente;
  - `pathMatchesLast`: «clients» trova `ledger-api` (cartella `/w/clients/ledger-api`) dopo i nomi;
  - `bothAccountsWhenNoFilter`: con `account = null` tornano progetti di tutti e due;
  - `emptyGivesMostRecent`: testo vuoto → i più recenti, al massimo `limit`;
  - `caseAndSpacesIgnored`: «  Atlas » trova `atlas-shop`.

  Esegui → FAIL.
- [ ] **Step 2:**
```kotlin
fun ranked(state: State, typed: String, account: String? = null, limit: Int = 6): List<Project> {
    val t = typed.trim().lowercase()
    fun rank(p: Project): Int? = when {
        t.isEmpty() -> 0
        p.name.lowercase().startsWith(t) -> 0
        t in p.name.lowercase() -> 1
        t in p.path.lowercase() -> 2
        else -> null
    }
    return state.projects.filter { account == null || it.account == account }
        .mapNotNull { p -> rank(p)?.let { it to p } }
        .sortedWith(compareBy<Pair<Int, Project>> { it.first }.thenByDescending { it.second.lastUsed ?: Long.MIN_VALUE })
        .map { it.second }.take(limit)
}
```
  → PASS. Commit `feat(core): ranked project search for Launch — prefix, contains, folder, recent, both accounts`.

### Task 3: `ChatRules`

**Files:** Create `core/src/main/kotlin/it/pixelbox/cmwatch/rules/ChatRules.kt`,
`core/src/test/kotlin/it/pixelbox/cmwatch/rules/ChatRulesTest.kt`.

**Interfaces — Produces:**
```kotlin
@Serializable data class Sent(
    val id: String, val session: String, val text: String, val sentAt: Long,
    val startedAt: Long? = null, val doneAt: Long? = null, val outcomeShort: String? = null, val outcomeFull: String? = null,
)
object ChatRules {
    enum class Status { SENDING, FAILED, DELIVERED, QUEUED, WORKING, DONE }
    const val SKEW_S = 10L
    const val KEEP_S = 7 * 86_400L
    fun status(m: Sent, pending: PendingStatus?, result: CmdResult?, s: Session?): Status
    fun advance(m: Sent, s: Session?, now: Long): Sent
    fun prune(list: List<Sent>, now: Long): List<Sent>
}
```

Regole:
- `status`: `FAILED` se `pending == FAILED` o `result?.ok == false`; poi `DONE` se `doneAt != null`; `WORKING` se
  `startedAt != null`; `SENDING` se `result == null && pending != null`; `QUEUED` se la sessione è `BUSY` con
  `turnStarted < sentAt - SKEW_S`; altrimenti `DELIVERED`.
- `advance` (idempotente, niente se `doneAt != null`):
  - turno visto partire: `s.state == BUSY && s.turnStarted != null && s.turnStarted >= sentAt - SKEW_S` → `startedAt =
    turnStarted`;
  - turno visto finire: `startedAt != null && s.state != BUSY && s.state != AWAITING` → `doneAt = now`, esito
    agganciato solo se `s.outcome.at >= startedAt`;
  - turno velocissimo: `startedAt == null && s.state != BUSY && s.state != AWAITING && s.outcome != null &&
    s.outcome.at >= sentAt - SKEW_S` → `startedAt = sentAt`, `doneAt = outcome.at`, esito agganciato.
- `prune`: toglie i messaggi con `sentAt < now - KEEP_S`.

- [ ] **Step 1, test:** `sendingUntilThePcAnswers`, `failedWhenRejected` (result ok=false), `failedWhenLost` (pending
  FAILED), `deliveredWhenIdle`, `queuedBehindARunningTurn`, `workingWhenItsTurnStarts`, `doneWithItsOutcome`,
  `outcomeOfAnEarlierTurnIsNotAttached`, `fastTurnSeenOnlyByItsOutcome`, `smallClockSkewStillCountsTheTurn`
  (turnStarted = sentAt - 5 → WORKING), `advanceIsIdempotentOnceDone`, `pruneAfterSevenDays`. → FAIL.
- [ ] **Step 2:** implementa come sopra → PASS. Commit `feat(core): chat rules — delivery and turn status for sent messages`.

### Task 4: `ChatLog`

**Files:** Create `core/src/main/kotlin/it/pixelbox/cmwatch/data/ChatLog.kt`,
`core/src/test/kotlin/it/pixelbox/cmwatch/data/ChatLogTest.kt`.

**Interfaces — Produces:**
```kotlin
class ChatLog(private val file: java.io.File, private val now: () -> Long) {
    val messages: StateFlow<List<Sent>>
    fun add(m: Sent)
    fun advance(state: State?)          // ChatRules.advance per ogni messaggio della sua sessione, poi prune e salva
    fun forSession(name: String): List<Sent>
}
```
  Salva in JSON (`ListSerializer(Sent.serializer())`) con scrittura su file temporaneo e `renameTo`; un file assente o
  rovinato vale lista vuota.

- [ ] **Step 1, test** (cartella temporanea di JUnit): `addPersistsAcrossInstances`, `advanceMovesToDone`,
  `corruptFileStartsEmpty`, `oldMessagesPrunedOnAdvance`. → FAIL.
- [ ] **Step 2:** implementa → PASS; tutta la suite `:core:testDebugUnitTest` verde. Commit
  `feat(core): chat log — sent messages on disk for seven days`.

### Task 5: barra di scrittura e chat nella scheda sessione

**Files:** Modify `mobile/.../PhoneApp.kt` (un `ChatLog` in `filesDir/chat.json`, `advance` a ogni stato), `MainActivity.kt`
(dopo `prompt`/`answerText` un `chatLog.add(Sent(id, session.name, text, now))`; `retry` per i falliti),
`ui/SessionSheet.kt`, stringhe; Test `SessionSheetTest` (`sheetChat` con sei messaggi nei sei stati, `sheetTyping`).

- [ ] **Step 1:** in `SessionSheet` la colonna scorrevole resta sopra; in fondo una `Row` fissa con `imePadding()`:
  `OutlinedTextField(weight(1f), maxLines = 5)` e a destra `FilledIconButton` (pieno) o `FilledTonalIconButton`
  (tonale quando `PhonePrimary.button == OPTION`), icona `Send`, abilitato con testo. Sessione chiusa: al posto della
  barra il bottone pieno «Riapri».
- [ ] **Step 2:** sopra la barra la chat: per ogni `Sent` della sessione (dal più vecchio) un fumetto a destra
  (`surfaceHigh`, `shapes.large`) con testo, ora e icona di stato (`Schedule`, `Error` in `gone`, `Check`, `Check` in
  `text2`, `PlayArrow` che respira, `DoneAll`), e sotto un `DONE` con esito un fumetto a sinistra (`briefCard`) con il
  ▶. Tocco su un fallito = Riprova. `SheetActions` riceve `chat: List<ChatRow>` con `ChatRow(sent, status)` calcolato
  in `MainActivity` con `ChatRules.status`.
- [ ] **Step 3:** stringhe dello stato come descrizione delle icone (`chat_sending` «in invio», `chat_failed` «non
  consegnato, tocca per riprovare», `chat_delivered` «consegnato», `chat_queued` «in coda», `chat_working` «in
  lavorazione», `chat_done` «elaborato»), it ed en.
- [ ] **Step 4:** compilazione → OK. Commit `feat(phone): send bar above the keyboard and a chat of sent messages with status and outcome`.

### Task 6: modello ed effort

**Files:** Modify `ui/SessionSheet.kt` (testata), `MainActivity.kt`, stringhe; Test `SessionSheetTest.sheetModelPicker`.

- [ ] **Step 1:** in `SheetHeader` modello ed effort diventano `AssistChip` con freccia; toccabili se
  `state.choices != null` e non in Demo. Il tocco apre un `ModalBottomSheet` con due gruppi: i modelli di
  `choices.models` (radio, corrente = `session.model.id`) e gli effort di `choices.efforts` (radio, corrente =
  `session.effort`). La scelta chiama `SheetActions.setModel(id)` / `setEffort(level)` → `repo.command(CmdOp.MODEL |
  CmdOp.EFFORT, session.name, arg)` e chiude il foglio.
- [ ] **Step 2:** stringhe `model_title` «Modello», `effort_title` «Effort», `choice_this_session` «Vale solo per
  questa sessione»; it ed en. Compilazione → OK. Commit `feat(phone): change model and effort of a session from its sheet`.

### Task 7: completamento in «Lancia» e «Accoppia di nuovo»

**Files:** Modify `ui/LaunchSheet.kt`, `ui/SettingsScreen.kt`, stringhe; Test `LaunchSheetTest.launchSheetTyping`,
`SettingsScreenTest` (esistenti).

- [ ] **Step 1, Lancia:** `ExposedDropdownMenuBox` sul campo del progetto; le voci da `LaunchSuggest.ranked(state,
  typed, filter)` con `AccountDot` e il nome con la parte trovata in grassetto (`buildAnnotatedString`). Scegliere
  imposta `chosen` e `account`. I bottoni segmentati diventano il filtro facoltativo (nessuno scelto = tutti).
- [ ] **Step 2, Impostazioni:** accoppiati, «Accoppia di nuovo» diventa una riga con icona `QrCodeScanner` in fondo alla
  card del PC, `TextButton`; niente bottone in fondo. Non accoppiati: card «Collega il PC» in cima con il bottone pieno
  «Accoppia».
- [ ] **Step 3:** compilazione → OK. Commit `feat(phone): Launch search with autocomplete; Pair again moves into the paired card`.

### Task 8: badge come l'orologio (Franz, 20:31)

**Files:** Create `mobile/.../ui/SessionBadge.kt`; Modify `ui/SessionCard.kt` (via `AccountDot`), `ui/SessionSheet.kt`,
`ui/ShareScreen.kt`, `ui/OverviewScreen.kt`, `ui/LaunchSheet.kt`, `ui/SessionsScreen.kt`; snapshot esistenti.

- [ ] **Step 1:** `SessionBadge(s: Session, size: Dp = 22.dp)` disegna `Badge.of(s.account, s.color, s.state, s.icon,
  s.accountKind)` di `core`: cerchio o quadrato arrotondato (raggio 30 %) riempito con `spec.fill`, glifo di stato
  (`PlayArrow`, `Check`, `QuestionMark`, `Close`) nel colore `spec.glyphColor`; respiro per `Badge.breathes`, fermo con
  `animationsOff()`; descrizione per TalkBack con stato e account.
- [ ] **Step 2:** `AccountMark(personal: Boolean, color = CmColors.text2)`: la sola forma vuota (bordo 2 dp), per dove
  c'è solo l'account (anelli della quota, righe di quota delle Sessioni, voci di «Lancia», filtro account).
- [ ] **Step 3:** sostituisci `AccountDot` + icona di stato con `SessionBadge` in card, scheda, Condividi, righe del
  contesto; `AccountDot` sparisce. La pillola di stato resta (testo ed età) ma senza icona, che ora è nel badge.
- [ ] **Step 4:** compilazione → OK. Commit `feat(phone): session badges like the watch — account shape, session colour, state glyph`.

### Task 8b: barra in alto con il menu delle sessioni

**Files:** Modify `ui/AppShell.kt`, `MainActivity.kt`, stringhe; snapshot `ShellScreensTest`.

- [ ] **Step 1:** `AppShell` riceve `sessions: List<Session>`, `current: String?`, `onPick: (String?) -> Unit`. Al posto
  del titolo un `ExposedDropdownMenuBox` compatto (testo `titleMedium`, freccia): etichetta = sessione aperta o
  «Tutte le sessioni» (`all_sessions`); voci = sessioni non chiuse da `PhoneBoard.sections`, con `SessionBadge`; «Tutte
  le sessioni» in testa chiude la scheda. Barra alta 56 dp, ⚙ a destra, fascia Demo sotto.
- [ ] **Step 2:** in `MainActivity` la scheda aperta passa ad `AppShell`; la scheda sessione vive dentro `AppShell`
  (anche per il terminale resta la sua testata). Compilazione → OK. Commit `feat(phone): session menu replaces the title in the top bar`.

### Task 8c: Stop (quando il relay ha `interrupt`)

- [ ] Dopo la risposta di `pix-claude-master`: `CmdOp.INTERRUPT` nel contratto con il test sulla fixture, e nella barra di
  scrittura `FilledTonalIconButton` Stop quando `session.state == BUSY` e il campo è vuoto e il relay lo supporta.

### Task 9: registrazione, provini, revisione

- [ ] **Step 1:** push di `feature/telefono`, `gh workflow run build-android.yml --ref feature/telefono -f record=true`,
  scarica `paparazzi-snapshots` e `cmwatch-apk`.
- [ ] **Step 2:** due passaggi sui provini (testo tagliato, «…», un solo bottone pieno, carattere 1,3); correggi e registra.
- [ ] **Step 3:** revisione finale del ramo con un revisore fresco; correzioni degli Important con test.
- [ ] **Step 4:** committa le immagini, installa l'APK release sul telefono, manda i provini a Franz. Checklist
  `docs/verifiche/pezzo-3-telefono.md`: aggiungi
  `- [ ] 12. Prompt dal telefono: arriva nella sessione, passa da consegnato a elaborato con l'esito.` e
  `- [ ] 13. Modello ed effort cambiati da una scheda: il PC conferma e la testata si aggiorna.`
