# Terminale «copione» e aggiornamento dal vivo — piano

> Esecuzione: inline in questa sessione (executing-plans), un task per volta, test prima del codice.

**Obiettivo:** nel Terminale si distingue chi parla (tu, Claude, strumenti, output) e le righe arrivano da sole mentre
la schermata è aperta.

**Architettura:** le regole in `core` (tipi di riga e blocchi in `TerminalText`, decisione dal vivo in `TerminalLive`),
testate su JVM; `wear` disegna i blocchi e fa le richieste. Contratto invariato.

**Design:** `docs/plans/2026-09-15-terminale-copione-design.md`.

## Vincoli globali

- Testi visibili in `res/values/strings.xml` e `values-en/`, mai nel Kotlin.
- Piena larghezza, niente bolle; `TransformingLazyColumn` con `morph(this, spec)` su ogni item.
- Build locale: `./gradlew --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process …`, in
  background, senza toccare i sorgenti mentre compila. Paparazzi solo in CI.
- Commit in inglese, file per nome.

---

### Task 1: voci e blocchi in `TerminalText`

**File:** `core/src/main/kotlin/it/pixelbox/cmwatch/rules/TerminalText.kt`,
`core/src/test/kotlin/it/pixelbox/cmwatch/rules/TerminalTextTest.kt`

**Produce:** `TerminalText.Kind { USER, CLAUDE, TOOL, OUTPUT }`; `Row(text, head, kind)` con `shown` (testo senza
`❯ › > ⏺ ●`); `TerminalText.Block(kind: Kind, text: String, heading: Boolean)`; `TerminalText.blocks(text): List<Block>`.
`Row.text` resta com'è (lo usa `SpeechText`).

- [ ] Test che falliscono:

```kotlin
@Test fun leVociDelTerminale() {
    val out = TerminalText.rows(listOf(
        "❯ Lancia i test e poi", "aggiorna il changelog", "⏺ Lancio la suite.",
        "⏺ Bash(pytest -q)", "⎿ 42 passed in 3.1s", "⏺ Tutto verde, passo al", "changelog.",
    ).joinToString("\n")).filter { it.text.isNotEmpty() }
    assertEquals(listOf(USER, USER, CLAUDE, TOOL, OUTPUT, CLAUDE, CLAUDE), out.map { it.kind })
    assertEquals("Lancia i test e poi", out[0].shown)
    assertEquals("Bash(pytest -q)", out[3].shown)
}

@Test fun laRispostaAUnaDomandaEDellUtente() {
    val out = TerminalText.rows("● User answered Claude's questions:\n· Prova dal polso? → Continua\n⏺ Continuo.")
        .filter { it.text.isNotEmpty() }
    assertEquals(listOf(USER, USER, CLAUDE), out.map { it.kind })
}

@Test fun ilPromptVuotoInFondoSparisce() {
    assertEquals(listOf("⏺ Fatto."), TerminalText.lines("⏺ Fatto.\n❯ "))
}

@Test fun primaDellaPrimaTestaEOutput() {
    assertEquals(OUTPUT, TerminalText.rows("42 passed\n❯ ok").first().kind)
}

@Test fun blocchiPerVoce() {
    val b = TerminalText.blocks("❯ Lancia i test e poi\naggiorna il changelog\n⏺ Lancio la suite.\n⏺ Bash(pytest -q)\n⎿ 42 passed in 3.1s\n⏺ Tutto verde.")
    assertEquals(listOf(
        USER to "Lancia i test e poi\naggiorna il changelog", CLAUDE to "Lancio la suite.",
        TOOL to "Bash(pytest -q)", OUTPUT to "42 passed in 3.1s", CLAUDE to "Tutto verde.",
    ), b.map { it.kind to it.text })
}

@Test fun ilTitoloEUnBloccoASe() {
    val b = TerminalText.blocks("⏺ Fatto:\n# Esito\nTutto verde")
    assertEquals(listOf(false, true, false), b.map { it.heading })
}
```

- [ ] `:core:testDebugUnitTest --tests '*TerminalTextTest'` → FAIL (Kind/blocks non esistono).
- [ ] Implementazione: `HEAD_CHARS` + `›`, `●`; `BARE_PROMPTS = setOf("❯", "›", ">")` saltate in `rows`; `kind` nel ciclo
  di `rows`:
  - `User answered` (anche dopo `⏺`/`●`) → USER; primo carattere in `❯ › >` → USER;
  - `⏺`/`●` + chiamata a strumento (`TOOL` sul resto) → TOOL, altrimenti CLAUDE; `TOOL` o `$` → TOOL; `#` → CLAUDE;
  - `•`/`·` → la voce corrente, CLAUDE se la corrente è TOOL/OUTPUT; `✻` → OUTPUT;
  - corpo: OUTPUT se la corrente è TOOL, altrimenti la corrente (all'inizio OUTPUT); riga vuota: la corrente.
  - `blocks`: righe non vuote consecutive con la stessa voce, separate da riga vuota, cambio di voce, o riga `#`
    (blocco a sé con `heading = true`); `text` = `shown` unite da `\n`.
- [ ] Tutti i test di `core` verdi (anche `SpeechTextTest`, che usa `rows`).
- [ ] Commit `feat(core): terminal rows know who speaks — you, Claude, a tool, its output — grouped in blocks`.

### Task 2: `TerminalLive`

**File:** `core/src/main/kotlin/it/pixelbox/cmwatch/rules/TerminalLive.kt`, test `TerminalLiveTest.kt`

**Produce:** `TerminalLive.next(prev: Session?, cur: Session?): TerminalLive.Ask?`, `Ask { SCREEN, SCREEN_AND_LAST }`.

- [ ] Test: `prev` null → null; stesso stato → null; `tool` o `toolNote` o `turnStarted` o `state` cambiati → SCREEN;
  BUSY → IDLE / WAITING / AWAITING / GONE → SCREEN_AND_LAST.

```kotlin
private fun s(state: SessionState = SessionState.BUSY, tool: String? = "Bash pytest -q") =
    Session("1", "atlas-shop", "personal", "p", state, since = 100L, tool = tool)
@Test fun allAperturaNiente() = assertNull(TerminalLive.next(null, s()))
@Test fun fermaNiente() = assertNull(TerminalLive.next(s(), s()))
@Test fun nuovoStrumentoCattura() = assertEquals(TerminalLive.Ask.SCREEN, TerminalLive.next(s(), s(tool = "Read a.kt")))
@Test fun fineTurnoAncheLaRisposta() = assertEquals(TerminalLive.Ask.SCREEN_AND_LAST, TerminalLive.next(s(), s(SessionState.IDLE, null)))
@Test fun domandaAncheLaRisposta() = assertEquals(TerminalLive.Ask.SCREEN_AND_LAST, TerminalLive.next(s(), s(SessionState.WAITING)))
```

- [ ] FAIL, implementazione (confronto di `state`, `since`, `tool`, `toolNote`, `turnStarted`), PASS.
- [ ] Commit `feat(core): TerminalLive decides when the open terminal asks the PC again`.

### Task 3: disegno a voci e divisore con l'ora

**File:** `wear/.../ui/screens/TerminalScreen.kt`, `res/values{,-en}/strings.xml`, `ScreensSnapshotTest.kt`

- `TerminalScreen(…, capturedAt: Long? = null)`; al posto del ciclo sulle righe, `TerminalText.blocks(text)`:
  - USER: `Row(Modifier.height(IntrinsicSize.Min))` con `Box(width 3.dp, fillMaxHeight, background accent)`, 8 dp,
    `Text(sans 15/21, actionIcon)`; CLAUDE: `Text(sans 15/21, text)`, grassetto se `heading`; TOOL: `TerminalStyle` bold
    `text2`; OUTPUT: `TerminalStyle` `text2`. 8 dp sopra i blocchi USER e CLAUDE (non il primo).
  - Divisore sempre quando c'è testo: filo `CmColors.line` 1 dp · «Terminale · HH:mm» (`terminal_divider`, `%1$s`) · filo;
    senza `capturedAt` solo «Terminale» (`card_terminal`). Via `terminal_lines`.
  - Segue il fondo: `follow` diventa vero/falso solo mentre l'utente scorre (`isScrollInProgress`), vero se l'ultimo
    item visibile è l'ultimo (`layoutInfo.visibleItems.last().index == totalItemsCount - 1`); a una cattura nuova
    diversa dalla precedente, se `follow`, `animateScrollToItem(totale - 1)`. La prima cattura non sposta la vista.
- [ ] Snapshot `terminal()` in `ScreensSnapshotTest` con `answer = emptyList()`, `capturedAt` fisso e una cattura con
  prompt, prosa, strumento e output.
- [ ] `:wear:compileDebugUnitTestKotlin` verde; commit `feat(wear): the terminal reads like a script — your lines on a blue rail, Claude in prose, tools in grey mono`.

### Task 4: aggiornamento dal vivo nella rotta `TERMINAL`

**File:** `wear/.../MainActivity.kt`

- `capturedAt` impostato all'arrivo di una cattura riuscita; un errore conta solo se non c'è testo.
- `prev` = sessione alla prima composizione; `LaunchedEffect(session)` chiede `TerminalLive.next(prev, session)`,
  mette `alsoLast` e sveglia un `Channel<Unit>(CONFLATED)`; un solo ciclo `LaunchedEffect(name)` lo consuma: aspetta
  che siano passati 3 s dall'ultima richiesta, manda `SCREEN` (e `LAST` se `alsoLast`), attende il risultato fino a 10 s.
- Aggiorna a mano = stessa via con `alsoLast = true` (niente «Chiedo al PC» sopra il testo).
- [ ] Build release come da memoria `watch-install-release-signing`, test core verdi, installazione con Franz vicino.
- [ ] Prova al polso: Terminale aperto su una sessione che lavora; righe nuove senza toccare; scroll in su non salta;
  fine turno aggiorna la Risposta. Riga nella checklist `docs/verifiche/`.
- [ ] Commit `feat(wear): the open terminal refreshes itself on every tool, one request at a time`.
- [ ] Snapshot Paparazzi: `gh workflow run build-android.yml --ref master -f record=true`, vedere `terminal.png`.
