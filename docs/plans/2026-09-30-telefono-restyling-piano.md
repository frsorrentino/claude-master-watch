# App del telefono, restyling: piano

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** il telefono con il linguaggio visivo dell'orologio in Material 3 Expressive, la Panoramica (il brief
dell'orologio) come schermata d'apertura, la lingua scelta nell'app e il terminale dal vivo.

**Architecture:** le regole restano in `core`. Si riusano quelle dell'orologio (`BriefCards`, `WorkPanel`, `DayBars`,
`QuotaHistory`, `SessionMeters`, `SessionsText`, `TerminalLive`, `TerminalText`) e se ne aggiungono tre piccole:
`PhoneOverview`, `AppLanguage`, `PhoneTerminal`. In `mobile` il tema passa a `MaterialExpressiveTheme` con i
`CmColors`. Le schermate si rifanno una per una, ognuna con i suoi snapshot.

**Tech Stack:** Compose Material 3 1.4.0 (API Expressive, `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`),
`LocaleManager` (API 33), Paparazzi, JUnit 4.

**Spec:** `docs/plans/2026-09-30-telefono-restyling-design.md` (con `2026-09-29-telefono-pezzo-3-design.md`).

## Global Constraints

- Solo i colori di `it.pixelbox.cmwatch.ui.tokens.CmColors`; niente colori dinamici.
- Un solo bottone pieno per schermata. Con una domanda che ha opzioni, il bottone pieno è la prima opzione; «Invia» è
  tonale.
- Mai «…» nel corpo; una riga logica su una riga fisica; il ▶ della lettura sopra `tts.min_chars` (120) e su esiti,
  risposte e domande.
- Testi solo in `mobile/src/main/res/values/strings.xml` e `values-en/strings.xml`.
- Movimento con `MotionScheme.expressive()`; con `animationsOff()`, o con `CmPhoneTheme(still = true)` nei test, lo stato
  finale subito.
- Paparazzi solo in CI (`build-android.yml`, input `record=true`); in locale solo compilazione, test JVM e
  `assembleDebug`. Prima del commit delle immagini, due passaggi di revisione sui provini.
- Commit in inglese, `git add` per nome di file.

## Review Focus

1. **Carattere a 1,3 e schermo piccolo:** anelli e numeri della Panoramica non si sovrappongono e non tagliano il testo.
   Snapshot in Task 4.
2. **Lingua «di sistema» dopo averne scelta una:** l'app torna a seguire il telefono, e la voce la segue. Test in Task 2.
3. **Terminale aperto su una sessione chiusa:** nessuna lettura dal vivo e nessun ciclo infinito. Test in Task 7.
4. **Nessun campione di quota (appena accoppiato):** l'anello si disegna, il ritmo no, senza «…». Test in Task 3.
5. **App riaperta dal launcher con una scheda aperta prima:** si apre sulla Panoramica. Verifica in Task 5 (test su
   `StartRoute`).

---

### Task 1: Tema Expressive con la palette dell'orologio

**Files:** Modify `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/PhoneTheme.kt`.

- [ ] **Step 1:** In `CmPhoneTheme` usa `MaterialExpressiveTheme(colorScheme = scheme, motionScheme =
  MotionScheme.expressive(), shapes = cmShapes, typography = cmType)`, dentro il `CompositionLocalProvider(LocalStill …)`
  che c'è già. Poi:
  - `cmShapes = Shapes(extraSmall = 8.dp, small = 12.dp, medium = 20.dp, large = 28.dp, extraLarge = 36.dp)`, tutti
    `RoundedCornerShape`;
  - `cmType`: la `Typography()` di default, con `displayLarge`/`displayMedium` a `FontWeight.SemiBold` per i numeri e
    `titleLarge`/`titleMedium` a `FontWeight.Medium`;
  - `scheme` aggiunge `secondaryContainer = CmColors.briefCard`, `onSecondaryContainer = CmColors.briefBig`,
    `tertiary = CmColors.waiting`.
- [ ] **Step 2:** `./gradlew :mobile:compileDebugUnitTestKotlin :mobile:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit `feat(phone): Material 3 Expressive theme on the watch palette`.

### Task 2: Lingua dell'app e voce (`AppLanguage`)

**Files:**
- Create `core/src/main/kotlin/it/pixelbox/cmwatch/rules/AppLanguage.kt`
- Create `core/src/test/kotlin/it/pixelbox/cmwatch/rules/AppLanguageTest.kt`
- Create `mobile/src/main/res/xml/locales_config.xml`
- Modify `mobile/src/main/AndroidManifest.xml` (`android:localeConfig="@xml/locales_config"`), `Speech.kt`,
  `ui/SettingsScreen.kt`

**Interfaces:**
```kotlin
object AppLanguage {
    enum class Choice(val tag: String) { SYSTEM(""), ITALIAN("it"), ENGLISH("en") }
    fun fromTags(tags: String): Choice                                    // "" → SYSTEM
    fun voiceLocale(choice: Choice, system: java.util.Locale): java.util.Locale
}
```

- [ ] **Step 1, test:**
```kotlin
class AppLanguageTest {
    @Test fun systemFollowsThePhone() =
        assertEquals(java.util.Locale.GERMANY, AppLanguage.voiceLocale(AppLanguage.Choice.SYSTEM, java.util.Locale.GERMANY))
    @Test fun chosenLanguageDrivesTheVoice() =
        assertEquals("it", AppLanguage.voiceLocale(AppLanguage.Choice.ITALIAN, java.util.Locale.US).language)
    @Test fun tagsRoundTrip() {
        assertEquals(AppLanguage.Choice.ENGLISH, AppLanguage.fromTags("en"))
        assertEquals(AppLanguage.Choice.SYSTEM, AppLanguage.fromTags(""))
        assertEquals(AppLanguage.Choice.SYSTEM, AppLanguage.fromTags("fr"))
    }
}
```
  `./gradlew :core:testDebugUnitTest --tests '*AppLanguageTest'` → FAIL (`Unresolved reference: AppLanguage`).
- [ ] **Step 2, implementazione:**
```kotlin
object AppLanguage {
    enum class Choice(val tag: String) { SYSTEM(""), ITALIAN("it"), ENGLISH("en") }
    fun fromTags(tags: String): Choice = Choice.entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: Choice.SYSTEM
    fun voiceLocale(choice: Choice, system: java.util.Locale): java.util.Locale =
        if (choice == Choice.SYSTEM) system else java.util.Locale.forLanguageTag(choice.tag)
}
```
  Stesso comando → PASS.
- [ ] **Step 3, telefono:**
  - `locales_config.xml` con `<locale android:name="it"/>` e `<locale android:name="en"/>`;
  - nelle impostazioni un `ListItem` «Lingua» (`language` «Lingua» / «Language»; `language_system` «Come il telefono» /
    «Same as the phone»; «Italiano»; «English») che apre un dialogo a tre scelte e chiama
    `ctx.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(choice.tag)`;
  - `Speech` a ogni `speak` imposta `tts.language = AppLanguage.voiceLocale(AppLanguage.fromTags(localeManager.applicationLocales.toLanguageTags()), Locale.getDefault())`.
- [ ] **Step 4:** Compilazione e build → OK. Commit `feat(phone): app language in settings, the reading voice follows it`.

### Task 3: La Panoramica in `core` (`PhoneOverview`)

**Files:** Create `core/src/main/kotlin/it/pixelbox/cmwatch/rules/PhoneOverview.kt` e `PhoneOverviewTest.kt`.

**Interfaces:**
```kotlin
object PhoneOverview {
    data class Ring(val account: String, val personal: Boolean, val h5: Int?, val w7: Int?, val resetAt: Long?, val pace: QuotaHistory.Pace?)
    data class Model(
        val rings: List<Ring>, val now: WorkPanel.Now, val questions: WorkPanel.Questions?,
        val contexts: List<WorkPanel.Ctx>, val today: List<DayBars.Bar>, val nightQueued: Int, val updatedAt: Long,
    )
    fun build(state: State, events: List<Event>, samples: Map<String, List<QuotaHistory.Sample>>, now: Long, zone: java.time.ZoneId, stale: Boolean): Model
}
```

- [ ] **Step 1, test** (fixture `Fixtures.stateQuestion`):
  - `ringsPersonalFirstWithWeek`: due anelli, personale per primo, `w7` presente;
  - `noSamplesNoPaceButRingStays`: con `samples` vuoti, `pace == null` e l'anello c'è;
  - `staleHidesResetTime`: con `stale = true`, `resetAt == null`;
  - `nowCountsMatchTheBoard`: `now.waiting + now.working + now.idle` = sessioni non chiuse.

  Esegui → FAIL (`Unresolved reference`).
- [ ] **Step 2, implementazione:** `rings` da `state.quota`, con l'ordine e la regola del reset di `PhoneBoard.quotaRows`
  e `pace = samples[account]?.takeIf { it.size >= 2 && resetAt != null }?.let { QuotaHistory.pace(it, resetAt!!, now) }`.
  `now` = `WorkPanel.now(state)`, `questions` = `WorkPanel.questions(state, now)`, `contexts` = `WorkPanel.contexts(state)`,
  `today` = `DayBars.today(events, now, zone)`, `nightQueued` = `state.night.queued`, `updatedAt` = `state.ts`. Test → PASS.
- [ ] **Step 3:** Commit `feat(core): phone overview model from the watch's brief rules`.

### Task 4: Schermata Panoramica

**Files:** Create `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/OverviewScreen.kt` e
`mobile/src/test/kotlin/it/pixelbox/cmwatch/mobile/OverviewScreenTest.kt`.

**Interfaces:** `@Composable fun OverviewScreen(model: PhoneOverview.Model, stale: Freshness, onQuestion: () -> Unit, onSession: (name: String) -> Unit)`.

- [ ] **Step 1, snapshot** con lo stato della Demo e campioni `fake.demoQuotaSamples()`: `overview`,
  `overviewLargeFont` (fontScale 1.3), `overviewNoSamples`, `overviewSmall` (`DeviceConfig.PIXEL_5.copy(screenWidth =
  720, screenHeight = 1280)`). Compilazione → FAIL.
- [ ] **Step 2, componenti:**
  - `QuotaRingCard`: `Canvas` con due archi concentrici (5 h fuori, settimana dentro), percentuale grande al centro,
    sotto «si azzera alle HH:mm» e il ritmo come linea sottile se `pace` c'è;
  - `NowCard`: barra a segmenti colorati (`WorkPanel.Seg`) e i tre contatori con le icone di stato;
  - `QuestionsCard`: tonale, ambra, toccabile;
  - `ContextRows`: nome, modello ed effort, barra con `SessionMeters.contextTone`;
  - `TodayBars`: 24 colonne (`DayBars.Bar`);
  - `NightCard` se `nightQueued > 0`;
  - footer con l'ora dell'aggiornamento o la fascia «dati di N min fa».

  Le card usano `MaterialTheme.shapes.large` e `CmColors.briefCard`. Anelli e colonne si riempiono entrando, con
  `animateFloatAsState(…, MaterialTheme.motionScheme.defaultSpatialSpec())`, e restano fermi con `animationsOff()`.
- [ ] **Step 3:** Compilazione e build → OK. Commit `feat(phone): Overview — the watch's brief on the phone`.

### Task 5: Tre schede, apertura sulla Panoramica, bottone mobile

**Files:** Modify `ui/AppShell.kt`, `MainActivity.kt`. Create `core/.../rules/StartRoute.kt` con il suo test.

- [ ] **Step 1, test:**
```kotlin
class StartRouteTest {
    @Test fun freshLaunchOpensOverview() = assertEquals(StartRoute.Tab.OVERVIEW, StartRoute.tab(restored = null))
    @Test fun rotationKeepsTheTab() = assertEquals(StartRoute.Tab.DIARY, StartRoute.tab(restored = StartRoute.Tab.DIARY))
    @Test fun freshLaunchClosesAnyOpenSheet() = assertNull(StartRoute.openSheet(freshLaunch = true, restored = "kb"))
}
```
  Esegui → FAIL. Implementa `StartRoute` in `core` (`enum class Tab { OVERVIEW, SESSIONS, DIARY }`,
  `tab(restored) = restored ?: OVERVIEW`, `openSheet(freshLaunch, restored) = if (freshLaunch) null else restored`) → PASS.
- [ ] **Step 2:**
  - `AppShell` con tre voci nella `NavigationBar` (`tab_overview` «Panoramica» / «Overview»);
  - in `MainActivity`, `freshLaunch = savedInstanceState == null` passato a `StartRoute`;
  - un `FloatingActionButtonMenu` con `ToggleFloatingActionButton` su Panoramica e Sessioni, con due voci «Lancia» e
    «Aggiungi alla notte», che aprono i fogli esistenti. Il bottone pieno «Lancia» in fondo alla regia sparisce.
- [ ] **Step 3:** Snapshot `ShellScreensTest` aggiornati alle tre schede. Build → OK. Commit
  `feat(phone): three tabs opening on Overview, Launch and Add to tonight in a floating menu`.

### Task 6: Card delle sessioni e scheda come sull'orologio

**Files:** Modify `ui/SessionCard.kt`, `ui/SessionSheet.kt`, `core/.../rules/PhonePrimary.kt` e il suo test.

- [ ] **Step 1, test** in `PhonePrimaryTest`:
```kotlin
@Test fun firstOptionIsTheFilledButtonWhenTheQuestionHasOptions() =
    assertEquals(PhonePrimary.Button.OPTION, PhonePrimary.button(s(SessionState.WAITING, q), "anche testo"))
```
  (`q` ha un'opzione) → FAIL. Aggiungi `OPTION` a `Button`: con `question?.options` non vuote vale `OPTION`, poi le regole
  di oggi → PASS. Adatta `emptyDraftNoButtonExceptClosed` se serve.
- [ ] **Step 2, card:**
  - titolo e dettaglio da `SessionsText.cell(...)` come le celle dell'orologio;
  - riga dell'obiettivo (`SessionsText.goalLine`) e bassa priorità (`SessionsText.priority`);
  - mini-barra del contesto con `SessionMeters`;
  - forma per stato: `shapes.extraLarge` e bordo ambra per chi aspetta, `shapes.large` per gli altri, superficie piatta
    per le chiuse.
- [ ] **Step 3, scheda:**
  - testata con badge e contatori (modello, effort, contesto);
  - opzioni in un `ButtonGroup` verticale, la prima piena (`OPTION`) e le altre tonali;
  - con `Tier.HIGH` risposta a pressione lunga (`combinedClickable`) e una riga di avviso come sull'orologio;
  - «Chat about this», e «Allow all» con `QuestionRules.allowAllVisible`;
  - «Invia» tonale quando c'è una domanda con opzioni.
- [ ] **Step 4:** Snapshot aggiornati (`SessionsScreensTest`, `SessionSheetTest` più `sheetHighTier`). Build → OK. Commit
  `feat(phone): session cards and sheet in the watch's language — goal, priority, context, first option filled`.

### Task 7: Terminale dal vivo (`PhoneTerminal`)

**Files:** Create `core/.../rules/PhoneTerminal.kt` e il suo test; modify `ui/TerminalScreen.kt` e `MainActivity.kt`.

**Interfaces:**
```kotlin
object PhoneTerminal {
    const val POLL_MS = 4_000L
    fun shouldAsk(session: Session?, lastAskedAt: Long?, answered: Boolean, now: Long): Boolean
}
```

- [ ] **Step 1, test:**
  - `asksAtOpen` (`lastAskedAt = null` → vero);
  - `waitsForTheAnswer` (`answered = false` → falso);
  - `asksAgainAfterPoll` (`answered = true`, `now - lastAskedAt >= 4` → vero);
  - `neverForAClosedSession` (`state = GONE` → falso).

  → FAIL. Implementa → PASS.
- [ ] **Step 2:** in `MainActivity` un `LaunchedEffect(name)` che, finché il terminale è aperto, ogni secondo chiede
  `PhoneTerminal.shouldAsk(...)` e, se vero, manda `CmdOp.SCREEN`. Al cambio di sessione nello stato vale anche
  `TerminalLive.next`. Il ciclo si ferma con la schermata.
- [ ] **Step 3:** In `TerminalScreen` le righe di `TerminalText.rows(text)` colorate per `Kind` (utente `CmColors.actionIcon`,
  Claude `CmColors.text`, strumenti `CmColors.text2`, output `CmColors.stale`). «Aggiorna» diventa un'icona nella testata,
  e l'indicatore ondulato Expressive gira durante la lettura.
- [ ] **Step 4:** Snapshot. Build → OK. Commit `feat(phone): live terminal like the watch, colours by speaker`.

### Task 8: Diario, Impostazioni, Condividi, Lancia nel nuovo linguaggio

**Files:** `ui/DiaryScreen.kt`, `ui/SettingsScreen.kt`, `ui/ShareScreen.kt`, `ui/LaunchSheet.kt`.

- [ ] **Step 1:**
  - card `briefCard` con `shapes.large`, titoli di sezione pesanti, spaziature della scala Expressive (16/24 dp);
  - lista delle sessioni di Condividi con le card nuove;
  - `LinearWavyProgressIndicator` dove oggi c'è un indicatore semplice.
- [ ] **Step 2:** Snapshot aggiornati. Build → OK. Commit `feat(phone): Diary, Settings, Share and Launch in the new
  visual language`.

### Task 9: Revisione visiva e registrazione

- [ ] **Step 1:** Push del ramo, poi `gh workflow run build-android.yml --ref feature/telefono -f record=true`. Scarica
  `paparazzi-snapshots`.
- [ ] **Step 2, revisione 1:** provini per schermata. Controlla: niente testo tagliato, niente «…», un solo bottone pieno,
  anelli leggibili a 1,3, colori solo di `CmColors`. Correggi, registra di nuovo.
- [ ] **Step 3, revisione 2:** confronto con le schermate dell'orologio (`wear/src/test/snapshots/images/*QuotaScreen*`,
  `*SessionScreen*`): stessa lingua visiva. Correggi, registra.
- [ ] **Step 4:** Committa le immagini e manda i provini a Franz (SendUserFile), poi aspetta il suo ok.
- [ ] **Step 5:** Checklist `docs/verifiche/pezzo-3-telefono.md`, aggiungi:
  - `- [ ] 10. Panoramica: anelli, Adesso, contesto, Oggi, notte coerenti con l'orologio.`
  - `- [ ] 11. Lingua cambiata nelle impostazioni: testi e voce che legge nella lingua scelta.`

  Commit `docs: live checklist rows for Overview and language`.
