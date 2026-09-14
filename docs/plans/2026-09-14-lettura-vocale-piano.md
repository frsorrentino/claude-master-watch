# Lettura vocale del testo intero — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development o superpowers:executing-plans,
> task per task. I passi usano le caselle (`- [ ]`).

**Goal:** il tasto ▶ legge il testo intero, non la coda di 600 caratteri, e c'è ovunque serva ascoltare senza guardare:
Esito, Terminale, scheda della sessione, Domanda, Recap, notifiche di esito e di domanda (Franz, 14/09 12:17: «applica tutte»).

**Architecture:** il PC tiene solo gli ultimi 600 caratteri della risposta, quindi il testo intero lo chiede l'orologio con
un comando nuovo, `last` (contratto 1.4, lato relay lo fa claude-master). Le regole del testo parlato stanno in `core`
(`SpeechText`: pulizia del markdown, pezzi a fine frase, domanda, recap, scelta fra risposta e ripiego), testate. Sul
polso un `Reader` chiede, aspetta 8 s e legge a pezzi; ogni lettura passa da `SpeakService`, in primo piano finché
parla, così continua a schermo spento.

**Tech Stack:** Kotlin, Wear Compose Material 3, `TextToSpeech`, servizio in primo piano `mediaPlayback`, JUnit 4.

## Vincoli globali

- Testi visibili in italiano in `wear/src/main/res/values/strings.xml`, mai cablati nel Kotlin.
- Una riga logica per riga fisica; mai «…» nel corpo; un solo bottone pieno per schermata; il ▶ sta accanto al testo.
- `TextToSpeech.getMaxSpeechInputLength()` è 4000: pezzi da al massimo 3500 caratteri, tagliati a fine frase.
- Contratto 1.4, solo aggiunte: `/cmd` op `last`, `session` = nome, `arg` null; `/result.text` = ultimo messaggio
  assistant intero fino a 4000 caratteri. Le fixture si copiano identiche da quelle del relay.
- Test core: `./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process :core:testDebugUnitTest`.
- Build: stessa riga con `:wear:assembleRelease`, credenziali da `release-keystore-credentials.txt` del watchface;
  si installa solo un APK con l'URL del database; mai un APK della CI.
- Commit in inglese, file per nome, mai `git add -A`.

---

### Task 1: regole del testo parlato (`SpeechText`)

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/SpeechText.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/SpeechTextTest.kt`

**Interfaces:**
- Produces: `SpeechText.MAX_CHUNK: Int` (3500); `clean(text: String, codeLabel: String): String`;
  `chunks(text: String, max: Int = MAX_CHUNK): List<String>`; `question(q: Question, option: String): String`;
  `recap(recap: Recap, next: String): String`; `pick(result: CmdResult?, fallback: String?): String?`.

- [ ] **Step 1: test rosso** — il file di test di questo commit (`SpeechTextTest.kt`): codice annunciato e non letto,
  markdown senza simboli, tabelle a elenco, pezzi a fine frase, testo lungo senza perdite, frase senza punti tagliata a
  uno spazio, domanda con opzioni numerate, recap per progetto, risposta del PC o ripiego.
- [ ] **Step 2:** con uno stub vuoto i test falliscono sulle asserzioni.
- [ ] **Step 3: implementazione** — `SpeechText.kt` come nel commit: regex per blocchi di codice, codice in linea,
  link, indirizzi, titoli, elenchi, separatori e celle di tabella, grassetto con asterischi; ogni riga non vuota chiude
  con un punto; `chunks` accumula frasi fino a `max` e taglia a uno spazio le frasi più lunghe.
- [ ] **Step 4:** suite core verde.
- [ ] **Step 5: commit** `feat(core): speech text rules, whole-reply chunks at sentence ends`.

### Task 2: contratto 1.4, comando `last`

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt` (`CmdOp`: `@SerialName("last") LAST`)
- Copy: `contract/cmd-result-sample.json` identico a `~/Desktop/workspaces/personali/claude-master/tests/fixtures/relay/cmd-result-sample.json`
- Modify: `contract/README.md` (riga «Contratto 1.4» come la scrive il relay)
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/contract/ContractTest.kt`

- [ ] **Step 1: test rosso** — `lastAsksForTheWholeReply`: il comando `last` della fixture si decodifica, si ricodifica
  con `"op":"last"` e il suo risultato è `ok` con testo non vuoto; il conteggio dei risultati sale di uno.
- [ ] **Step 2:** rosso senza `CmdOp.LAST`. **Step 3:** aggiunta dell'enum. **Step 4:** verde.
- [ ] **Step 5: commit** `feat(contract): 1.4 adds the last command, the whole reply for the voice`.

### Task 3: lettura a pezzi, `Reader`, servizio in primo piano

**Files:**
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/tts/Speaker.kt` (`speakAll(parts)`: primo pezzo `QUEUE_FLUSH`,
  gli altri `QUEUE_ADD`; `speaking` vero da subito fino alla fine dell'ultimo pezzo)
- Create: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/tts/Reader.kt` (`toggleLast(session, fallback)`,
  `toggleText(text)`, `stop()`, `preparing: StateFlow<Boolean>`, `busy: Boolean`, `WAIT_MS = 8000`)
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt` (`val reader: Reader by lazy { Reader(this) }`)
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/push/SpeakService.kt` (azioni `LAST`, `TEXT`, `STOP`; in primo
  piano `mediaPlayback` finché `busy`; helper `last(ctx, …)`, `text(ctx, …)`, `lastIntent`, `textIntent`)
- Modify: `wear/src/main/AndroidManifest.xml` (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
  `android:foregroundServiceType="mediaPlayback"` sul servizio)
- Modify: `wear/src/main/res/values/strings.xml` (`tts_reading`, `tts_code`, `tts_option`, `tts_recap_next`,
  `card_listen`, `recap_listen`)

- [ ] Verifica: compila; la prova vera è al polso (Task 6).
- [ ] **Commit** `feat(wear): whole replies read aloud in chunks, kept alive by a foreground service`.

### Task 4: il ▶ nelle schermate

**Files:** `wear/.../MainActivity.kt`, `ui/screens/QuestionScreen.kt`, `ui/screens/SessionScreen.kt`,
`ui/screens/RecapScreen.kt`.

- Esito e Terminale: `onSpeak = { t -> SpeakService.last(ctx, name, t) }`, icona da `speaking || preparing`.
- Scheda: voce «Ascolta la risposta» prima di «Esito», che diventa «Ferma la lettura» mentre legge.
- Domanda: ▶ accanto al testo, legge `SpeechText.question(q, tts_option)`.
- Recap: riga «Ascolta il recap» con ▶ sotto il titolo, legge `SpeechText.recap(recap, tts_recap_next)`.
- [ ] **Commit** `feat(wear): the play button on the question, the session card and the recap`.

### Task 5: «Leggi» nelle notifiche

**Files:** `wear/.../push/Notifier.kt`.

- Esito: l'azione «Leggi» avvia `SpeakService.lastIntent(ctx, s.name, plan.bigText)` con `getForegroundService`.
- Domanda: nuova azione «Leggi» dopo le altre, `SpeakService.textIntent(ctx, SpeechText.question(q, tts_option))`.
- [ ] **Commit** `feat(wear): read aloud from outcome and question notifications`.

### Task 6: build, installazione, verifica dal vivo

- [ ] Build lean, controllo firma, codice nuovo e URL del database, installazione.
- [ ] Checklist `docs/verifiche/fase-5-lettura.md`: Esito e Terminale leggono oltre i 600 caratteri; scheda, Domanda,
  Recap, notifiche di esito e di domanda leggono; a schermo spento la lettura continua; «Ferma» nella notifica
  «Lettura in corso»; PC spento: dopo 8 s legge il testo corto che ha già.
- [ ] Push dopo la verifica.

## Ripresa (14/09/2026 13:35, Franz in movimento)

Sul polso: build delle 13:24, con il ▶ del testo intero verificato (servizio in primo piano, «last» in meno di un secondo).
Richieste arrivate dopo il piano: frase intera dell'esito in carattere più piccolo, description del comando sulla tile
(contratto 1.5, `tool_note`), voce maschile scelta a orecchio. Da fare, in quest'ordine:

1. Già su disco: fixture 1.5 copiate, `Session.toolNote`, `Settings.ttsVoice` con la sua chiave in `Prefs`,
   `OutcomeText.headline` implementato, stub `ToolText.describe` e `VoiceRules`, test `OutcomeTextTest`, `ToolNoteTest`,
   `VoiceRulesTest`. Lanciare quelle tre classi più `ContractTest`: rossi solo `ToolNoteTest` e `VoiceRulesTest`.
2. Implementare `describe` (la nota se non è vuota, altrimenti `phrase`), `TileTexts.activity` che la usa,
   `VoiceRules.next` (predefinita, poi le voci in ordine, poi di nuovo la predefinita) e `position` (0 = predefinita).
3. Lato polso, non ancora applicato: Esito con `OutcomeText.headline` in `titleMedium`; `SessionHeader` con `describe`;
   `Speaker.voices` e `setVoice`; riga «Voce N di M» nelle Impostazioni con un campione a ogni tocco; voce salvata
   applicata in `CmApp`; stringhe `settings_tts_voice`, `settings_tts_voice_default`, `tts_voice_sample`.
4. Suite core intera; build release senza lint vital, che su questa VM si blocca per memoria; installazione con
   l'orologio su questa rete e la pagina Debug wireless aperta.
5. Prova dal vivo con Franz e checklist `docs/verifiche/fase-5-lettura.md`; un commit per pezzo; push.
