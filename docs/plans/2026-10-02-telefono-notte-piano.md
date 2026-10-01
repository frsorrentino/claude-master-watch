# Telefono: piano della notte del 02/10 (casa della master, widget Master, voce, passaggi)

> **Per chi esegue:** superpowers:executing-plans, un task dopo l'altro, TDD su `core`. Le caselle si spuntano solo con la
> prova in mano (test verdi, compilazione, provino guardato). Alla fine una sola pubblicazione: push, CI di registrazione,
> provini guardati e committati, installazione sul telefono (Franz, 02/10 00:04: «fai tutte le modifiche e poi pubblica
> app completa»).

**Goal:** l'app del telefono con la master come casa, il secondo widget (Master), i consigli sotto la risposta, la voce
senza simboli e a blocchi, i passaggi compatti.

**Architecture:** regole in `core` (oggetti puri, testati con JUnit sui fixture del contratto), schermate in `mobile`
(Compose, Glance per i widget). La casa della master riusa la scheda sessione della master con due blocchi in cima alla
chat; il Quadro completo è il foglio della Panoramica che esiste già.

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (1.5.0-alpha18), Glance 1.2, TextToSpeech di sistema, Paparazzi per i
provini (in CI).

**Spec:** `docs/plans/2026-10-01-telefono-casa-master-design.md`, `docs/plans/2026-10-01-telefono-widget-master-design.md`,
`docs/plans/2026-10-01-telefono-consigli-design.md`.

## Global Constraints

- Testi visibili in italiano in `res/values/strings.xml` (con `values-en/`), mai cablati nel Kotlin.
- Mai «…» nel corpo dei testi; una riga logica occupa la riga fisica; un solo bottone pieno per schermata.
- Nessun cambio di contratto in questo piano: solo dati già nello stato e nella trascrizione.
- Commit in inglese, file per nome, mai `git add` di cartella; niente chiavi o `google-services.json`.
- Build locale: `./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`; se la memoria
  non basta, la CI fa fede (CLAUDE.md).
- Animazioni continue (led, sfumature) ferme solo con `LocalStill` (i provini), mai con la scala di sistema.

## Review Focus

1. **Master assente o chiusa:** la casa deve restare utile (Quadro, Per te, «master non attiva» con «Riapri»), mai vuota.
2. **Per te senza nulla da fare:** la card non compare; niente titolo vuoto.
3. **Testo con markdown letto a voce:** nessun «asterisco»; il codice si annuncia, non si legge.
4. **Widget senza stato** (app appena installata, PC spento): «in attesa del PC», mai un riquadro vuoto o deforme.
5. **Gesto indietro e «Tutte le sessioni» dalla casa:** non devono riaprire la master (la correzione `a14b670`).

---

### Task 1: Consigli sotto la risposta (variante C)

Già scritto: `core/.../rules/NextSteps.kt` + `NextStepsTest`, `SessionSheet.kt` (consigli sotto l'ultima risposta,
suggerimento nel campo con «Usa»), stringhe `next_steps`, `suggestion_use`.

- [ ] Test `NextStepsTest` verdi e `:mobile:compileDebugKotlin` riuscita.
- [ ] Commit `feat(phone): next-step suggestions under the last reply, Claude Code's suggestion as ghost text in the field`.

### Task 2: La voce legge testo pulito

**Files:** Modify `mobile/.../Speech.kt`.

- [ ] `Speech.speak(text)` legge `SpeechText.chunks(SpeechText.clean(text, ctx.getString(R.string.tts_code)))` in coda
  (primo pezzo `QUEUE_FLUSH`, gli altri `QUEUE_ADD`, come `Speaker` dell'orologio); `speaking` resta il testo originale,
  così il tasto ■ resta legato al suo testo. Se manca `R.string.tts_code` nel telefono, aggiungerla («Codice» / «Code»).
- [ ] Prima della pulizia: `NextSteps.parse(text).text` e `OutcomeLine.forPhone(…)`, così non legge «Prossimi:» né «Watch:».
- [ ] Compilazione; commit `fix(phone): read-aloud text without markdown symbols, in chunks under the engine limit`.

### Task 3: Passaggi compatti, contenitore nuovo

**Files:** Modify `mobile/.../ui/SessionSheet.kt` (`StepsCard`).

- [ ] Chiuso: **due righe al massimo**. Riga 1: icona del tipo dell'ultimo passaggio, «N passaggi» e i conteggi (nomi
  corti), freccia; riga 2: il testo in chiaro dell'ultimo passaggio, una riga. Niente comando sotto quando è chiuso.
- [ ] Contenitore: niente fondo grigio. Fondo trasparente, bordo 1 dp `CmColors.line`, angoli 14 dp, una barretta
  verticale di 2 dp a sinistra nel colore del tipo dell'ultimo passaggio. Aperto: tutte le righe come oggi.
- [ ] Provino `sheetTranscript` da riregistrare in CI (Task 9).
- [ ] Commit `feat(phone): steps card closes to two lines in a light outlined container`.

### Task 4: Foglio del contesto e «Fai controllare alla master»

**Files:** Modify `mobile/.../ui/SessionSheet.kt` (`SheetHeader`, `SheetActions`), `MainActivity.kt`, strings.
**Interfaces:** `ContextActions.urgent(pct)`, `ContextActions.wider(s, choices)`, `ContextActions.master(state)` (esistono).

- [ ] Tocco sull'anello del contesto → `ModalBottomSheet` «Contesto N%»: «Scrivi l'handoff e riparti pulita» (pieno da
  80 %, tonale sotto) = prompt `R.string.ctx_handoff_prompt` («Scrivi l'handoff con /fable-director:handoff --here, poi
  riavviati pulita con claude-master restart --clean.»); «Compatta» = prompt `/compact`; «Passa alla finestra da 1M»
  solo se `wider` non è null = `setModel(id)`.
- [ ] Menu ⋮ della scheda: «Fai controllare alla master» (non sulla master stessa) = prompt alla master
  `R.string.ask_master_prompt` («Controlla la sessione %1$s e dimmi in breve a che punto è e se serve qualcosa da me.»),
  registrato nella chat della master; toast «Chiesto alla master».
- [ ] `SheetActions` nuovi: `askMaster: (() -> Unit)?`, il foglio usa `send` e `setModel` esistenti.
- [ ] Commit `feat(phone): context sheet (handoff and restart, compact, 1M window) and ask the master to check a session`.

### Task 5: Regole della casa della master (core, TDD)

**Files:** Create `core/.../rules/MasterHome.kt`, `core/src/test/.../MasterHomeTest.kt`.
**Produces:**

```kotlin
object MasterHome {
    enum class Kind { QUESTION, CONTEXT, NIGHT_REPORT, NIGHT, NEXT_STEP, SCHEDULED }
    data class Row(val kind: Kind, val title: String, val detail: String?, val session: String?, val project: String? = null)
    data class ForYou(val rows: List<Row>, val more: Int)
    const val MAX = 3
    fun forYou(state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String> = emptySet()): ForYou
}
```

- Ordine: domande (`s.question != null`, la più vecchia prima), contesto ≥ 80 (`ContextActions.urgent`), resoconto della
  notte (evento `NIGHT_REPORT` di oggi, prima delle 12 locali, se la sua chiave non è in `read`), notte (dalle 20 locali,
  se `state.night.items != null`), prossimo passo (dal `state.recap` di oggi, per progetti la cui sessione è ferma o
  chiusa; titolo `progetto · prossimo passo`, dettaglio = `next`), invii programmati (`ChatRules.waiting`).
- Titoli e dettagli sono **dati**, non frasi: le frasi le compone l'app dalle stringhe (`Row.kind` decide quale).
- Test (uno per tipo, più): ordine; taglio a tre con `more`; niente righe = `rows` vuoto; resoconto sparisce dopo le 12 e
  dopo la lettura; notte solo dalle 20; prossimo passo assente se la sessione del progetto lavora.
- [ ] Test rossi, implementazione, test verdi, commit `feat(core): rules for the master home (Per te rows)`.

### Task 6: Casa della master (UI)

**Files:** Modify `core/.../rules/StartRoute.kt` (+ test), `mobile/.../ui/AppShell.kt`, `MainActivity.kt`,
`mobile/.../ui/SessionSheet.kt` (slot in cima alla chat); Create `mobile/.../ui/MasterHome.kt`, `mobile/.../ui/TechStyle.kt`.

- [ ] `StartRoute.Tab` = `MASTER, SESSIONS, DIARY`; partenza `MASTER`; lo scorrimento laterale segue il nuovo ordine
  (test di `StartRouteTest` aggiornati). Barra in basso: Master (icona persona), Sessioni, Diario. Menu ≡: «Quadro» apre
  il foglio della Panoramica (`overviewSheet`), Sessioni, Diario, Impostazioni.
- [ ] Scheda Master: se `ContextActions.master(state)` c'è, la sua `SessionSheet` con un nuovo parametro
  `top: (LazyListScope.() -> Unit)?` che aggiunge in cima alla chat «Per te» e il Quadro compatto (scorrono via con la
  chat). Se non c'è: colonna con Per te, Quadro e la card «master non attiva» con «Riapri» (`CmdOp.REOPEN` su `master`).
- [ ] `MasterHome.kt`: `ForYouCard(forYou, onAction)` e `QuadroStrip(model, onOpen)`: righe con testo e tasto (pieno solo
  il primo); Quadro con led, «aggiornato ora · PC», due mini anelli con numeri da strumento, chip delle sessioni (barretta
  di colore, alone su chi lavora, contesto da 75 %). Tocco sul Quadro = `overviewSheet`.
- [ ] `TechStyle.kt`: `Modifier.dotGrid()` (puntini 1 dp, bianco 7,5 %, passo 14 dp), `GlassCard` (bordo 14 %, riflesso),
  `RuledLabel` (11 sp, maiuscolo, 2 sp, riga), `Led` (8 dp, alone, pulsa 1,6 s, fermo con `LocalStill`), numeri
  monospazio. Valori dalla spec, sezione «Stile».
- [ ] Azioni di Per te: Rispondi = coda; Handoff e riavvio = prompt del Task 4 alla sessione; Ascolta = voce sul corpo
  del resoconto e segna letto (in memoria + `rememberSaveable`); Aggiungi = foglio notte; Avvia = prompt `next` alla
  sessione del progetto se ferma, altrimenti `CmdOp.LAUNCH` sul progetto; Vedi = scheda della sessione dei programmati.
- [ ] Commit `feat(phone): the master is the app's home (Per te, Quadro, master chat), bold tech style`.

### Task 7: Widget Master

**Files:** Create `core/.../rules/MasterWidgetModel.kt` (+ test), `mobile/.../widget/MasterWidget.kt`,
`mobile/.../widget/WriteToMasterActivity.kt`, `mobile/src/main/res/xml/master_widget_info.xml`; Modify
`AndroidManifest.xml`, `PhoneApp.kt` (anteprima), strings.

```kotlin
object MasterWidgetModel {
    data class Chip(val name: String, val seg: WorkPanel.Seg?, val waiting: Boolean)
    data class Model(val master: Session?, val asking: Boolean, val text: String?, val at: Long?, val chips: List<Chip>, val more: Int)
    fun build(state: State?, maxChips: Int): Model
}
```

- Testo: la domanda se c'è (`asking`), altrimenti l'esito pieno della master (`outcome.full`) senza le righe `Prossimi:`
  e `Watch:`; chip: tutte le sessioni vive tranne la master, prima chi aspetta, poi chi lavora, poi ferme, poi notte;
  `more` = quante non stanno.
- Widget (Glance): testata con icona a tratto + parola dello stato (ibrido), testo, barra «Scrivi alla master» /
  «Rispondi alla master» che apre `WriteToMasterActivity` (tema trasparente, foglio con il campo a fuoco, invio = prompt o
  risposta alla master con il percorso della scheda). 4×4: chip in righe calcolate a larghezza fissa, «+N». Tocchi:
  testata/testo = scheda della master, chip = sua scheda, «+N» = Quadro.
- Anteprima generata come per il primo widget (misura 4×2), monocromo acceso di partenza.
- [ ] Test `MasterWidgetModelTest` verdi, compilazione, commit `feat(widget): the Master widget (last message or question,
  write bar, session chips)`.

### Task 8: Voce, scelta e blocchi

**Files:** Modify `mobile/.../Speech.kt`, `SettingsScreen` (impostazioni), `Prefs` del telefono, `SessionSheet.kt`
(`ClaudeBubble`).

- [ ] `Speech`: voci italiane del motore (`voices: StateFlow<List<String>>`), `setVoice(name)` salvata nelle preferenze;
  `speakBlocks(blocks: List<List<String>>, from: Int)` e `block: StateFlow<Int?>` come `Speaker` dell'orologio.
- [ ] Impostazioni: riga «Voce» con l'elenco e «Prova».
- [ ] `ClaudeBubble`: durante la lettura il testo si mostra a paragrafi (`AnswerText.blocks`), quello letto evidenziato;
  tocco su un paragrafo = si legge da lì.
- [ ] Commit `feat(phone): choose the reading voice, read replies by paragraph and restart from a tapped one`.

### Task 9: Pubblicazione

- [ ] Push di `feature/telefono`; `gh workflow run build-android.yml --ref feature/telefono -f record=true`; annullare il
  run del push (i provini cambiano).
- [ ] Scaricare `paparazzi-snapshots`, guardare ogni provino cambiato, committare quelli giusti
  (`test(phone): goldens … (CI run N)`), push.
- [ ] Installazione sul telefono (`scratchpad/install.sh SHA`, porta da `avahi-browse -rpt _adb-tls-connect._tcp`).
- [ ] Checklist dal vivo `docs/verifiche/casa-master.md`: apertura sulla Master, Per te con azioni vere, Quadro, chat
  della master, gesto indietro, widget Master posato, voce senza simboli, blocchi, passaggi compatti.

## Fuori da stanotte (approvate il 01/10, da pianificare dopo)

- Notifica al contesto 80 % con «Handoff e riavvio» e tasto «Invia il suggerimento» nella notifica di fine turno: toccano
  `PhoneNotifier` e il percorso delle notifiche, da provare dal vivo a parte.
- Tasto «Segnala» nell'app (cattura e manda alla master).
- Riepilogo della sessione dal menu in alto: con la casa della master la Panoramica non è più una scheda; da ridiscutere.
- Regola `Prossimi:` nell'hook di claude-master: richiesta da mandare dopo il tetto delle sessioni.
