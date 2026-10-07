# Prestazioni e reattività dell'app (A1-A9): piano di attuazione

> **Per chi esegue:** sotto-skill obbligatoria `superpowers:subagent-driven-development` (consigliata) oppure
> `superpowers:executing-plans`. I passi usano le caselle (`- [ ]`) per il tracciamento.

**Obiettivo:** telefono, tablet, orologio e web restano reattivi e dicono il vero anche quando il PC è lento. Lo stato
mostra la sua età vera, nessun comando parte due volte, la tile non blocca, la web smette di martellare il relay.

**Architettura:** il lavoro sta quasi tutto in `core` (trasporto, Repo, regole pure con test JUnit) e nella web
(`web/src/lib/*.ts` con vitest). I punti che toccano il contratto (A2, A4, A5) partono solo dopo l'accordo con la sessione
team-supervisor e con la versione 1.43 scritta in `contract/README.md`. Le schermate cambiano poco: leggono i nuovi
campi del modello.

**Stack:** Kotlin, OkHttp 4 con SSE, Room, WorkManager, Wear Tiles 1.6.2, Jetpack Compose; Svelte 5 con vitest.

**Fonti:** `~/.team-supervisor/analisi/2026-10-07-prestazioni/sintesi.md` (piano consolidato),
`verifica.md` (rilievi controllati), `app.md` (report completo dell'app). Il design dell'app resta
`docs/plans/2026-09-12-app-polso-design.md`.

## Vincoli globali

- **Build Android su win:** sorgenti in `D:\workspaces\team-supervisor-app`, aggiornati con un bundle git. Mai build
  Android sul Chromebook (CLAUDE.md globale, «Chromebook leggero»). Il keystore su win è ammesso (Franz, 07/10 17:34).
- **Test della web in locale** con `nice -n 19 npx vitest run`; `npm run check` deve restare a 0 errori.
- **Su win `ChatLogTest` fallisce** (5 test, solo Windows, in CI su Linux passano): non è un rosso di questo piano.
  Il verde di riferimento per `core` resta la CI.
- **Contratto:** lo possiede questa sessione. `published_at` (A2), `sessions[].tx` (A4) e il ritiro di `cmd/<id>`
  (A5) formano la 1.43, concordata con team-supervisor il 07/10 alle 19:43 (piano relay:
  `team-supervisor/docs/plans/2026-10-07-prestazioni-relay.md`). Lato relay: `published_at` arriva con la sua fase 1,
  `tx` = "<size>-<mtime_ns>" del .jsonl con la fase 2, il ritiro con la fase 2. Campi assenti = comportamento di oggi:
  ogni passo deve funzionare anche con un relay vecchio.
- **Testi visibili** in `strings.xml` (it ed en) e in `web/src/lib/t.ts`; mai «…» nel corpo dei testi.
- **Commit** in inglese, solo file per nome. Con il piano approvato, push e release sono approvati (CLAUDE.md
  globale); il push passa dalla guardia come compito di un piano approvato.
- **Misure prima e dopo** prese dal log del relay e dai dispositivi veri, senza carichi sintetici (Task 0).

## Fasi

| Fase | Punti | Dipende da |
|---|---|---|
| 0. Misure di partenza | script e numeri di oggi | niente |
| 1. Subito, senza contratto | A9 chat ferma, A1 ricevuta, A3 tile, A6 trasporto, A4a web a cadenza | Task 0 |
| 2. Con il contratto 1.43 | A2 freschezza vera (dalla fase 1 del relay), A4b letture per marcatore e A5 comandi scaduti (dalla fase 2 del relay) | rilascio del relay |
| 3. Pulizia del carico | A7 eventi, widget e tick, A8 cache delle chat | fase 1 |
| 4. Misure dopo e rilascio | stesso script, confronto, APK e web | tutte |

## Scelta A5, decisa da Franz il 07/10 alle 21:42: «A5 come proposto»

Quando un comando non ha risposta dopo 20 s, si sceglie fra **rifiuto** e **«in ritardo»**. Il piano propone
tutte e due, decise da un fatto e non da un orologio:
- se il relay **non ha ancora preso** il comando, l'app lo ritira da `/cmd`: «non consegnato», con Riprova;
- se il relay **l'ha già preso**, l'app non lo ritira più e mostra «in esecuzione sul PC»; aspetta fino a 10 minuti, senza Riprova.

Il contratto 1.43 concordato con team-supervisor copre già i due rami: il relay «prende» il comando con la sua DELETE
condizionata prima di eseguirlo, quindi il ritiro del client riesce solo se il relay non l'ha ancora preso.

## Punti delicati da controllare in revisione

1. **Stato vecchio che sovrascrive uno nuovo.** Un GET lento (sveglia FCM, tile) arriva dopo lo stream: `Repo.accept`
   deve scartare uno stato con `ts` minore di quello in uso (Task 6).
2. **Relay vecchio senza i campi 1.43.** Senza `published_at`, `tx` o il ritiro: l'app fa come oggi, mai «sempre
   vecchio» né «mai rilette» (Task 7, Task 8, Task 9).
3. **Comando ritirato mentre il relay lo prende.** La DELETE condizionata dà 412: l'app deve passare a «in esecuzione»,
   non a FAILED (Task 9).
4. **Pagina della chat diversa dalla sessione aperta.** Dopo un riordino delle pagine la chat a schermo deve ancora
   aggiornarsi (Task 1).
5. **Rete che cambia con gli stream aperti.** Passando da Wi-Fi a LTE gli stream si riaprono subito, senza i 90 + 30 s
   di oggi (Task 4).

---

### Task 0: misure di partenza

**Files:**
- Create: `scripts/relay-measures.py`
- Create: `docs/verifiche/2026-10-07-prestazioni-prima.md`

**Interfacce:**
- Produce: `python3 scripts/relay-measures.py LOG --since HH:MM --until HH:MM` stampa un JSON con
  `push_interval_s` (mediana, p90), `push_per_hour`, `transcript_reads_per_hour` per dispositivo (`web (locale)`, nome di
  ogni telefono e orologio), `prompts_per_hour`, `superseded_per_hour`. Il Task 15 lo rilancia con le stesse finestre.

- [ ] **Passo 1: script deterministico.** Legge `~/.team-supervisor/relay/relay.log`. Righe:
  `2026-10-07T16:19:27 push: 7 sessioni, 0 eventi` e
  `2026-10-07T16:20:28 cmd <id>: transcript <sessione> da <dispositivo> → ok …`.

```python
#!/usr/bin/env python3
"""Misure del relay dal suo log (piano prestazioni 07/10): cadenza delle push e carico dei comandi per dispositivo."""
import json, re, statistics, sys
from collections import Counter
from datetime import datetime

PUSH = re.compile(r"^(\S+) push: ")
CMD = re.compile(r"^(\S+) cmd \S+: (\w+) (\S+) da (.+?) → ")

def main(path, since, until):
    pushes, ops = [], Counter()
    for line in open(path, encoding="utf-8", errors="replace"):
        t = line[11:16]
        if not (since <= t < until):
            continue
        if m := PUSH.match(line):
            pushes.append(datetime.fromisoformat(m.group(1)))
        elif m := CMD.match(line):
            ops[(m.group(2), m.group(4))] += 1
    hours = max((int(until[:2]) * 60 + int(until[3:]) - int(since[:2]) * 60 - int(since[3:])) / 60, 1 / 60)
    gaps = [(b - a).total_seconds() for a, b in zip(pushes, pushes[1:])]
    out = {
        "window": f"{since}-{until}",
        "push_per_hour": round(len(pushes) / hours, 1),
        "push_interval_s": {"median": statistics.median(gaps) if gaps else None,
                            "p90": sorted(gaps)[int(len(gaps) * 0.9)] if gaps else None},
        "per_hour": {f"{op} da {dev}": round(n / hours, 1) for (op, dev), n in sorted(ops.items())},
    }
    print(json.dumps(out, ensure_ascii=False, indent=1))

if __name__ == "__main__":
    a = sys.argv
    main(a[1], a[a.index("--since") + 1], a[a.index("--until") + 1])
```

- [ ] **Passo 2: lancialo su tre finestre del 07/10** (normale 15:00-16:00, incidente 16:20-16:40, ripresa 16:40-17:00)
  e copia i tre JSON nella nota `docs/verifiche/2026-10-07-prestazioni-prima.md`.

Run: `nice -n 19 python3 scripts/relay-measures.py ~/.team-supervisor/relay/relay.log --since 15:00 --until 16:00`
Atteso: `transcript da web (locale)` intorno a 900/h (il report ne conta 902 nell'ora delle 15).

- [ ] **Passo 3: misure sui dispositivi**, senza carichi sintetici, nella stessa nota:
  - **Tile:** una riga di log `cmwatch tile: <ms> ms` in `onTileRequest`, misurata dall'inizio a `completer.set`. Vale
    10 aperture della tile, prese da Franz al polso; per leggerle serve il debug wireless acceso sull'orologio.
  - **Età dello stato:** una riga `cmwatch state age: <now - ts> s` in `Repo.accept` (Task 6), su telefono e orologio,
    durante un'ora d'uso normale. Fa da base per il «dopo».
  - **Prompt dal telefono:** dal log del relay, il tempo fra `issued` del comando e la sua riga `cmd … prompt … → ok`.
    La riga con `issued`, l'arrivo dallo stream, l'attesa in fila, l'esecuzione e la scrittura dell'esito arriva con la
    fase 0 del piano relay. `issued` è l'ora del telefono, quindi conta la differenza fra i passi del relay e non fra
    due orologi. Prima di quella fase questa misura non c'è, e si dice. Lo script del Passo 1 va esteso per leggere i
    campi nuovi, e deve continuare a leggere anche le righe di oggi.

- [ ] **Passo 4: commit**

```bash
git add scripts/relay-measures.py docs/verifiche/2026-10-07-prestazioni-prima.md
git commit -m "docs: performance baseline from the relay log (pushes, reads per device)"
```

---

## Fase 1: subito, senza contratto

### Task 1 (A9): la chat a schermo si aggiorna anche se la pagina e la sessione aperta divergono

Causa trovata il 07/10 (segnalazione delle 16:09): il telefono mostrava la pagina di motion-graphic-video mentre `open`
era team-supervisor-app. Il giro di lettura (`MainActivity.kt:457`) segue `chatName` = `open`, quindi la pagina a
schermo restava sulla copia in cache. Le pagine sono ordinate per stato (`SwipePages.of`), quindi si riordinano a ogni
cambio di stato, e `LaunchedEffect(open, pages, active)` (`MainActivity.kt:1071`) non riprova se in quel momento il
pager sta scorrendo.

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/SwipePages.kt`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/MainActivity.kt:1048-1078`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/SwipePagesTest.kt`

**Interfacce:**
- Produce: `SwipePages.realign(pages: List<String?>, settled: Int, open: String?, scrolling: Boolean): Int?`: l'indice
  a cui riportare il pager, oppure null se va bene così.

- [ ] **Passo 1: test che falliscono**

```kotlin
@Test fun aReorderLeavesTheSettledPageOnAnotherSessionAndItGoesBack() {
    val pages = listOf(null, "team-supervisor-app", "motion-graphic-video")
    assertEquals(1, SwipePages.realign(pages, settled = 2, open = "team-supervisor-app", scrolling = false))
}
@Test fun whileScrollingNothingMoves() {
    assertNull(SwipePages.realign(listOf(null, "a", "b"), settled = 2, open = "a", scrolling = true))
}
@Test fun aligned() {
    assertNull(SwipePages.realign(listOf(null, "a", "b"), settled = 1, open = "a", scrolling = false))
}
@Test fun openNotAPageNothingToDo() {
    assertNull(SwipePages.realign(listOf(null, "a"), settled = 1, open = "zeta", scrolling = false))
}
```

- [ ] **Passo 2:** su win, `.\gradlew.bat :core:testDebugUnitTest --tests "*SwipePagesTest*"`. Atteso: errore di
  compilazione, `realign` non esiste.

- [ ] **Passo 3: regola**

```kotlin
/**
 * Dopo un riordino (le pagine seguono lo stato delle sessioni) il pager può restare su una sessione diversa da quella
 * aperta, e la chat a schermo non si rilegge più (segnalazione del 07/10 16:09). Fermo il pager, lo si riporta su quella aperta.
 */
fun realign(pages: List<String?>, settled: Int, open: String?, scrolling: Boolean): Int? {
    if (scrolling) return null
    val i = pages.indexOf(open)
    return if (i < 0 || i == settled) null else i
}
```

- [ ] **Passo 4: nel pager**, al posto di `LaunchedEffect(open, pages, active)`, un raccoglitore che guarda le tre cose
  insieme e riprova quando lo scorrimento finisce:

```kotlin
LaunchedEffect(pager, active) {
    if (!active) return@LaunchedEffect
    androidx.compose.runtime.snapshotFlow { Triple(currentPages, open, pager.isScrollInProgress) to pager.settledPage }
        .collect { (t, settled) ->
            val (pp, o, scrolling) = t
            val to = it.pixelbox.cmwatch.rules.SwipePages.realign(pp, settled, o, scrolling) ?: return@collect
            if (kotlin.math.abs(to - pager.currentPage) == 1) pager.animateScrollToPage(to) else pager.scrollToPage(to)
        }
}
```

  `currentPages` è già un `rememberUpdatedState(pages)`; `open` è uno stato di Compose, quindi `snapshotFlow` lo vede.

- [ ] **Passo 5:** su win, test verdi e `.\gradlew.bat :mobile:assembleDebug`.
- [ ] **Passo 6: prova dal vivo** (Franz, telefono): apri A, manda un prompt, passa a B dal menu, torna ad A scorrendo
  mentre A lavora. La risposta di A deve comparire entro 10 s. Nel log del relay devono comparire letture di A dal
  telefono.
- [ ] **Passo 7: commit** `fix(mobile): the chat on screen keeps reading when a reorder leaves the pager on another session`

---

### Task 2 (A1): la ricevuta `/seen` non ritarda il primo stato

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/transport/FirebaseTransport.kt:68-90,112-116`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/transport/FirebaseTransportTest.kt` (se manca, nuovo, con un `Rtdb` finto)

**Interfacce:**
- Consuma: `Rtdb.put(path, body)`.
- Produce: `FirebaseTransport(..., seenScope: CoroutineScope)`, il parametro nuovo. Le app passano il loro scope di
  processo; nei test si usa `TestScope`.

Il commento di oggi dice perché `markSeen` sta prima di `emit`: chi prende solo il primo stato (`first()`) chiude il
flusso, e la ricevuta non partirebbe più. Lanciarla in uno scope che non è quello del flusso conserva la ragione e
toglie l'attesa.

- [ ] **Passo 1: test.** Con un `Rtdb` finto in cui `put("seen/…")` sospende per 5 s, `transport.state.first()`
  torna in meno di 100 ms di tempo virtuale, e dopo `advanceUntilIdle()` la PUT di `seen/<uid>` risulta fatta.
  Lo stesso per `fetchState()`.
- [ ] **Passo 2:** su win il test fallisce: oggi `first()` aspetta la PUT.
- [ ] **Passo 3: codice**

```kotlin
private fun markSeenLater() { seenScope.launch { markSeen() } }
// nello stream:  if (!marked) { marked = true; markSeenLater() }   // fuori dal flusso: chi prende solo il primo stato lo chiude
// fetchState:    return ContractJson.decodeState(...).also { markSeenLater() }
```

  `PhoneApp` e `CmApp` passano lo scope che già usano per il Repo.
- [ ] **Passo 4:** test verdi su win.
- [ ] **Passo 5: commit** `perf(core): the /seen receipt no longer delays the first state`

---

### Task 3 (A3): la tile risponde subito e si aggiorna dopo

**Files:**
- Modify: `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/tile/CmTileService.kt:73-97`
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/rules/TileTexts.kt` (decisione «da rinfrescare»)
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/rules/TileTextsTest.kt`

**Interfacce:**
- Produce: `TileTexts.needsRefresh(receivedAtS: Long?, nowS: Long): Boolean`, vero se non c'è stato o se è stato
  ricevuto più di 60 s fa. Decide dall'ora di ricezione locale, non da `ts`, quindi nessun confronto fra orologi.
- Consuma: `Snapshot.receivedAt` (Task 6).

- [ ] **Passo 1: test della regola**: `needsRefresh(null, 1000)` vero; `needsRefresh(950, 1000)` falso;
  `needsRefresh(900, 1000)` vero.
- [ ] **Passo 2: codice della tile.** Niente `runBlocking`: la future si completa da una coroutine e il disegno usa
  solo i dati già in memoria. Se servono dati freschi, si rinfresca in background e poi si chiede un nuovo disegno.

```kotlin
private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> =
    CallbackToFutureAdapter.getFuture { completer ->
        val app = application as CmApp
        scope.launch {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val prefs = app.prefs.current()
            val snap = app.repo.snapshot.value
            completer.set(tile(requestParams, snap, prefs))
            android.util.Log.i("cmwatch", "tile: ${android.os.SystemClock.elapsedRealtime() - t0} ms")
            if (TileTexts.needsRefresh(snap.receivedAt, System.currentTimeMillis() / 1000)) {
                runCatching { kotlinx.coroutines.withTimeout(10_000) { app.repo.refresh() } }
                getUpdater(this@CmTileService).requestUpdate(CmTileService::class.java)
            }
        }
        "tile"
    }

override fun onDestroy() { scope.cancel(); super.onDestroy() }
```

  `tile(...)` raccoglie il codice di oggi che costruisce `TileBuilders.Tile` (root, risorse, freschezza). Un secondo
  rinfresco non parte se il primo è ancora in corso: lo controlla un `AtomicBoolean refreshing`.
- [ ] **Passo 3:** test della regola verdi; `:wear:assembleDebug` su win; golden della tile invariati
  (`:wear:verifyPaparazziDebug` in CI).
- [ ] **Passo 4: misura dal vivo**: le 10 aperture del Task 0 rifatte. Atteso: la riga `tile:` sotto 50 ms (prima, fino
  a 2,5 s con lo stato vecchio).
- [ ] **Passo 5: commit** `perf(tile): answer from memory at once, refresh in the background and request an update`

---

### Task 4 (A6): trasporto più robusto (backoff, cambio di rete, chiamate cancellabili, esito)

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/transport/FirebaseTransport.kt:62-67,161-172`
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/transport/Rtdb.kt:33,43-66`
- Modify: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/PhoneApp.kt:113-115`,
  `wear/src/main/kotlin/it/pixelbox/cmwatch/wear/CmApp.kt:104`
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/data/Repo.kt` (`live(on)` e il nuovo `reconnect()`)
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/transport/FirebaseTransportTest.kt`

**Interfacce:**
- Produce: `Repo.reconnect()`: chiude e riapre gli stream se sono attivi. `Rtdb.get/put/delete` diventano cancellabili.
  `Rtdb.stream` emette un errore anche su `auth_revoked` e `cancel`.

- [ ] **Passo 1: test**, con `Rtdb` finto e tempo virtuale:
  1. quattro cadute di fila e poi uno stream che consegna un evento: la caduta dopo riparte dopo 1 s, non dopo 30;
  2. un evento `auth_revoked` chiude lo stream e lo riapre;
  3. `send()` usa attese crescenti fra le GET di `result` (0,5, 1, 2, 4 s, poi 4 s fisse): in 20 s di tempo virtuale
     ne fa al massimo 8, contro le 20 di oggi.
- [ ] **Passo 2: codice**
  - In `resilient`, `attempt = 0` alla prima emissione, non solo all'uscita pulita, più un po' di jitter (±20 %):

```kotlin
private fun <T> resilient(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = flow {
    var attempt = 0
    while (true) {
        try { block(FlowCollector { v -> attempt = 0; emit(v) }) } catch (e: CancellationException) { throw e } catch (e: Exception) { /* riprova */ }
        val base = backoffMs[minOf(attempt, backoffMs.size - 1)]
        delay(base + (base * (Random.nextDouble() - 0.5) * 0.4).toLong()); attempt++
    }
}
```

  - Negli stream `if (ev.event == "auth_revoked" || ev.event == "cancel") throw TransportException.Network(ev.event)`.
    Così si riapre con un token fresco, come già fa il relay.
  - In `Rtdb`: `execute()` diventa `await()` cancellabile (`suspendCancellableCoroutine` con `call.enqueue` e
    `cont.invokeOnCancellation { call.cancel() }`); `callTimeout` da 30 a 10 s per le REST brevi. Lo streaming non cambia,
    e nemmeno la PUT di `/share/<id>`: con il tetto degli allegati a 10 MB (richiesta del 07/10 19:55) un caricamento su
    LTE può durare più di 10 s, quindi tiene un `callTimeout` di 120 s.
  - Cambio di rete: in `onAvailable` e in `onLost`, `repo.reconnect()` oltre a `flushQueue()`.
- [ ] **Passo 3:** test verdi su win; `:mobile:assembleDebug :wear:assembleDebug`.
- [ ] **Passo 4: prova dal vivo:** sul telefono in primo piano, si spegne il Wi-Fi. Lo stato deve tornare in meno di
  10 s su LTE; prima del fix servivano fino a 120 s.
- [ ] **Passo 5: commit** `perf(core): streams reopen on network change and after auth_revoked, cancellable REST, backoff reset on success`

---

### Task 5 (A4a): la web legge a cadenza, una lettura alla volta

**Files:**
- Create: `web/src/lib/readPlan.ts`, `web/src/lib/readPlan.test.ts`
- Modify: `web/src/App.svelte:152-169,327-345`

**Interfacce:**
- Produce: `shouldRead(s: {state: string}, last: {at: number, answered: boolean} | undefined, changed: boolean, now: number): boolean`,
  la stessa regola di `PhoneTerminal.shouldAskChat`: 10 s se la sessione lavora, 60 s se è ferma, subito se lo stato di
  quella sessione è cambiato, 21 s di attesa se la lettura precedente non ha avuto risposta. Più `READ_IN_FLIGHT_MAX = 1`.

- [ ] **Passo 1: test vitest** (in italiano come gli altri):

```ts
import { describe, expect, it } from 'vitest'
import { shouldRead } from './readPlan'
describe('cadenza delle letture', () => {
  it('la prima lettura parte subito', () => expect(shouldRead({ state: 'idle' }, undefined, false, 100)).toBe(true))
  it('ferma: una al minuto', () => {
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 159)).toBe(false)
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 160)).toBe(true)
  })
  it('al lavoro: ogni 10 s', () => expect(shouldRead({ state: 'busy' }, { at: 100, answered: true }, false, 110)).toBe(true))
  it('stato cambiato: subito', () => expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, true, 101)).toBe(true))
  it('senza risposta: aspetta 21 s', () => {
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 120)).toBe(false)
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 121)).toBe(true)
  })
})
```

- [ ] **Passo 2:** `nice -n 19 npx vitest run src/lib/readPlan.test.ts`. Atteso: FAIL, il modulo manca.
- [ ] **Passo 3: codice**

```ts
// La cadenza delle letture della conversazione, la stessa del telefono (PhoneTerminal.kt): la web chiedeva una lettura per
// sessione a ogni stato, 900 all'ora il 07/10, ed era il carico più grande sulla fila dei comandi del relay.
export const BUSY_S = 10, IDLE_S = 60, LOST_S = 21
export function shouldRead(s: { state: string }, last: { at: number; answered: boolean } | undefined, changed: boolean, now: number): boolean {
  if (!last) return true
  if (!last.answered) return now - last.at >= LOST_S
  if (changed) return true
  const busy = s.state === 'busy' || s.state === 'awaiting'
  return now - last.at >= (busy ? BUSY_S : IDLE_S)
}
```

  In `App.svelte`, l'effetto che oggi chiama `loadTranscript` a ogni stato tiene `lastRead[name]` e il valore precedente
  di `state` e di `turn_started` per sessione (`changed` = uno dei due è diverso). Chiama `loadTranscript` solo se
  `shouldRead` dice sì e se non c'è già un'altra lettura in volo nella pagina. Alla cronologia (`timeline`) basta una
  lettura ogni 60 s, con lo stesso registro.
- [ ] **Passo 4:** vitest verde, `npm run check` a 0 errori, `vite build` in una cartella di prova, poi chrome-bridge
  sulla plancia con quattro colonne per 5 minuti.
  Atteso nel log del relay: `transcript da web (locale)` da circa 900/h a meno di 150/h, con i messaggi nuovi a schermo
  entro 10 s nelle sessioni al lavoro.
- [ ] **Passo 5: commit** `perf(web): transcript reads at the phone's cadence, one in flight per page`

---

### Task 6: Repo tiene l'ora di ricezione e scarta gli stati più vecchi

Serve a A2, A3 e A7: oggi `accept` applica qualunque stato, anche uno più vecchio di quello in uso (`Repo.kt:126-129`).

**Files:**
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/data/Repo.kt:124-129` e la classe `Snapshot`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/data/RepoTest.kt`

**Interfacce:**
- Produce: `Snapshot.receivedAt: Long?`, l'epoch in secondi in cui il dispositivo ha ricevuto lo stato in uso. Lo usano
  `TileTexts.needsRefresh` e `Freshness.of` (Task 7).

- [ ] **Passo 1: test:** `accept(ts=200)` e poi `accept(ts=150)`: lo stato resta quello con ts=200, ma `receivedAt` si
  aggiorna, perché il PC ha risposto. Con `accept(ts=200)` due volte, il secondo aggiorna solo `receivedAt`.
- [ ] **Passo 2: codice**

```kotlin
private suspend fun accept(s: State) {
    val cur = _snapshot.value.state
    val t = now()
    if (cur != null && s.ts < cur.ts) { _snapshot.update { it.copy(receivedAt = t) }; return }   // un GET lento arrivato dopo lo stream
    val ordered = s.copy(sessions = Order.sessions(s.sessions))
    store.saveState(ordered, t)
    _snapshot.update { it.copy(state = ordered, receivedAt = t, freshness = Freshness.of(ordered, t)) }
    android.util.Log.i("cmwatch", "state age: ${t - ordered.ts} s")
    recordQuota(ordered)
}
```

  `receivedAt` si salva anche in Room accanto allo stato (`saveState` già riceve `now()`), così una tile disegnata a
  freddo sa quanto è vecchio.
- [ ] **Passo 3:** test verdi su win.
- [ ] **Passo 4: commit** `fix(core): keep the receive time and ignore a state older than the one in use`

---

## Fase 2: con il contratto 1.43

Accordo con team-supervisor raggiunto il 07/10 alle 19:43. Primo passo della fase: scrivere la 1.43 in
`contract/README.md` e aggiornare le fixture, con i test che leggono le fixture. Il Task 7 può partire appena il relay
rilascia la sua fase 1 (`published_at`); i Task 8 e 9 con la sua fase 2.

### Task 7 (A2): freschezza su `published_at`, «PC lento» e la lettura in attesa nella chat

**Files:**
- Modify: `contract/README.md` (sezione 1.43), `contract/state-sample.json` (`published_at`)
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Model.kt` (`State.publishedAt: Double? = null`)
- Modify: `core/src/main/kotlin/it/pixelbox/cmwatch/contract/Order.kt:15-26`
- Modify: `mobile/.../MainActivity.kt:781`, `mobile/.../ui/SessionSheet.kt:335`, i testi in `strings.xml` (it, en)
- Modify: `web/src/lib/contract.ts`, `web/src/lib/durations.ts`, `web/src/lib/t.ts`
- Test: `core/src/test/.../contract/OrderTest.kt`, `web/src/lib/durations.test.ts`

**Interfacce:**
- Produce: `Freshness.of(state: State, now: Long): Freshness` con un terzo caso, `Freshness.Slow(lagS: Int)`: stato
  ricevuto da poco, ma raccolto più di 30 s prima della pubblicazione. `Fresh` e `Stale(minutes)` restano.

- [ ] **Passo 1: test**
  - `published_at` assente: si fa come oggi (età da `ts`, `Stale` da 180 s);
  - `published_at = ts + 5`, adesso = `published_at + 10`: `Fresh`;
  - `published_at = ts + 45`: `Slow(45)`;
  - `published_at` di 200 s fa: `Stale(3)`.
- [ ] **Passo 2: codice**

```kotlin
data class Slow(val lagS: Int) : Freshness()
const val SLOW_LAG_S = 30L
fun of(s: State, now: Long): Freshness {
    val pub = s.publishedAt?.toLong() ?: s.ts
    if (now - pub >= STALE_AFTER_S) return Stale(((now - pub) / 60).toInt())
    val lag = pub - s.ts
    return if (s.publishedAt != null && lag >= SLOW_LAG_S) Slow(lag.toInt()) else Fresh
}
```

  Il menu dice «PC lento · stato di N s fa» (`menu_updated_slow`) al posto di «aggiornato ora». Nella chat, quando una
  lettura è in volo da più di 10 s e la chat ha già voci, una riga sottile in fondo dice «in attesa del PC da N s»
  (`chat_waiting_pc`): oggi la rotella c'è solo a chat vuota. Stessi testi e stessa regola nella web
  (`durations.ts`, `Chat.svelte`).
- [ ] **Passo 3:** test verdi; golden del menu e della chat da registrare in CI con `record=true`, guardati prima del commit.
- [ ] **Passo 4: commit** `feat(contract): 1.43 published_at; freshness from it, 'PC slow' and the pending read in the chat`

### Task 8 (A4b): letture solo quando la conversazione è cambiata (`sessions[].tx`)

**Files:**
- Modify: `contract/README.md`, `contract/state-sample.json` (`tx` su due sessioni)
- Modify: `core/.../contract/Model.kt` (`Session.tx: String? = null`), `web/src/lib/contract.ts`
- Modify: `web/src/lib/readPlan.ts` (Task 5) e `core/.../rules/PhoneTerminal.kt:34-40`
- Test: `web/src/lib/readPlan.test.ts`, `core/src/test/.../rules/PhoneTerminalTest.kt`

**Interfacce:**
- Produce: `shouldRead(..., txNow: string | undefined, txRead: string | undefined)`. Con tutti e due i valori presenti e
  uguali, la risposta è sempre no: niente di nuovo da leggere. Con `tx` assente vale la cadenza del Task 5.
  `PhoneTerminal.shouldAskChat(..., txNow: String?, txRead: String?)`, stessa regola.

- [ ] **Passo 1: test:** tx uguale a quello letto e sessione al lavoro da 30 s: no. tx cambiato: sì, subito. tx
  assente: cadenza di prima.
- [ ] **Passo 2: codice:** `txRead` si salva quando una lettura riesce, con il `tx` dello stato in uso al momento della
  richiesta. Così una voce arrivata durante la lettura cambia `tx` e fa partire un'altra lettura.
- [ ] **Passo 3: misura** con lo script del Task 0 su 30 minuti di plancia: `transcript da web (locale)` sotto 30/h con
  le sessioni ferme.
- [ ] **Passo 4: commit** `feat(contract): 1.43 sessions[].tx; web and phone read a transcript only when it changed`

### Task 9 (A5): il comando scaduto si ritira, o si aspetta se il PC l'ha preso

**Files:**
- Modify: `contract/README.md` (ritiro di `cmd/<id>`). Nessuna fixture di result: per un comando ritirato il relay
  non scrive niente, solo una riga nel suo log.
- Modify: `core/.../transport/Rtdb.kt` (`getWithEtag`, `deleteIfMatch`)
- Modify: `core/.../transport/Transport.kt` e `FirebaseTransport.kt` (`withdraw(id): Withdraw`, `awaitResult(id, maxMs)`)
- Modify: `core/.../data/Repo.kt:260-275` (stato `PendingStatus.ON_PC`)
- Modify: `core/.../rules/ChatRules.kt` (stato in chat «in esecuzione sul PC»), `wear/.../push/Notifier.kt:194-198`
  (niente Riprova per `ON_PC`), `web/src/App.svelte:257` e `web/src/lib/transport.ts`
- Test: `core/src/test/.../transport/FirebaseTransportTest.kt`, `core/src/test/.../data/RepoTest.kt`, `web/src/lib/transport.test.ts`

**Interfacce:**
- Produce: `sealed interface Withdraw { Withdrawn; Taken }`. `Withdrawn` = il comando è stato ritirato da /cmd e non
  partirà mai. `Taken` = il relay l'ha già preso. `Rtdb.getWithEtag(path): Pair<String?, String>` (corpo ed ETag);
  `Rtdb.deleteIfMatch(path, etag): Boolean` (false su HTTP 412).

- [ ] **Passo 0:** aspettare la conferma di team-supervisor che RTDB accetta `if-match` sulla DELETE (la prova con una
  chiamata vera la fa lui). Se non la accetta, il ritiro diventa una PUT condizionata a `null` con lo stesso `if-match`, e
  `deleteIfMatch` si chiama `clearIfMatch`: il resto del task non cambia.
- [ ] **Passo 1: test del trasporto**, con un `Rtdb` finto che risponde con l'ETag:
  - `cmd/<id>` ancora presente e DELETE condizionata riuscita: `Withdrawn`;
  - presente, ma la DELETE dà 412: `Taken`;
  - già null: `Taken`.
- [ ] **Passo 2: test del Repo:** `Withdrawn` porta a `FAILED`, con Riprova; Riprova rimanda lo stesso id. `Taken`
  porta a `ON_PC` e poi al risultato, se arriva entro 10 minuti con attese di 2, 4, 8, … fino a 30 s; senza risultato
  dopo 10 minuti si va a `FAILED` senza Riprova.
- [ ] **Passo 3: codice del ritiro**

```kotlin
override suspend fun withdraw(id: String): Withdraw {
    val (body, etag) = rtdb.getWithEtag("cmd/$id")
    if (body == null) return Withdraw.Taken
    return if (rtdb.deleteIfMatch("cmd/$id", etag)) Withdraw.Withdrawn else Withdraw.Taken
}
```

  `getWithEtag` aggiunge l'header `X-Firebase-ETag: true` e legge `ETag` dalla risposta; `deleteIfMatch` manda
  `if-match: <etag>`.
- [ ] **Passo 4: in `Repo.dispatch`**, al timeout: `transport.withdraw(cmd.id)`, poi `FAILED` oppure `ON_PC` con
  `awaitResult`. La web fa lo stesso in `transport.ts`: la sua `/api/cmd` locale è sincrona e non scade allo stesso
  modo, quindi lì il ritiro serve solo per i comandi passati dal bus.
- [ ] **Passo 5:** test verdi; golden della chat con lo stato «in esecuzione sul PC» registrati in CI.
- [ ] **Passo 6: prova dal vivo** con il relay rallentato davvero (una sessione pesante in corso, nessun carico
  sintetico): un prompt dal telefono mentre il PC è lento non deve mai arrivare due volte alla sessione.
- [ ] **Passo 7: commit** `feat(contract): 1.43 withdraw an expired cmd unless the relay took it; 'running on the PC' instead of a false failure`

---

## Fase 3: pulizia del carico

### Task 10 (A7): eventi scaricati per differenza e scritti solo se nuovi

**Files:**
- Modify: `core/.../transport/FirebaseTransport.kt:92-110`, `core/.../data/Repo.kt:110-116,325`, `core/.../data/Db.kt:28`
- Test: `core/src/test/.../data/RepoTest.kt`

- [ ] **Passo 1: test:** dopo 300 eventi in Room, uno nuovo dallo stream produce un solo insert, e `_events` contiene
  301 voci. La potatura gira una volta al giorno e tiene 7 giorni (oggi 30).
- [ ] **Passo 2: codice.** Lo stream di `/events` si apre con `orderBy="$key"` e `startAt` sull'ultima chiave vista; il
  GET separato degli ultimi 200 sparisce, perché il `put` iniziale dello stream filtrato lo sostituisce. Il Repo
  inserisce solo le chiavi nuove (`store.insertEvents`) e unisce in memoria.
- [ ] **Passo 3: commit** `perf(core): events streamed from the last key and inserted only when new, 7 days kept`

### Task 11 (A7): sveglia FCM accorpata e con retry

**Files:**
- Modify: `wear/.../push/WakeWorker.kt:16-27`, `mobile/.../push/PhoneMessagingService.kt:20`
- Modify: `core/.../data/Repo.kt` (`refresh()` dice se è riuscito)

- [ ] **Passo 1:** `refresh(): Boolean`. Nel worker, `if (!ok && runAttemptCount < 3) Result.retry() else Result.success()`,
  con il backoff esponenziale di WorkManager a 10 s. `ExistingWorkPolicy.KEEP` al posto di `APPEND_OR_REPLACE`: N
  sveglie ravvicinate diventano un GET solo. Sul telefono, lo stesso worker al posto del GET diretto nel servizio FCM.
- [ ] **Passo 2:** test del Repo su `refresh()` con trasporto che fallisce; `:wear:assembleDebug :mobile:assembleDebug` su win.
- [ ] **Passo 3: commit** `perf(push): wake work kept unique and retried when the GET fails`

### Task 12 (A7): widget ridisegnati solo se cambia il loro modello; tick della scheda live a 1 s

**Files:**
- Modify: `mobile/.../PhoneApp.kt:135-142`, `wear/.../live/LiveActivity.kt:35`

- [ ] **Passo 1:** in `PhoneApp`, `repo.snapshot.map { CmWidget.model(it) }.distinctUntilChanged().debounce(2_000)`
  prima di `updateAll`, e lo stesso per `MasterWidget`. Se la funzione modello pura non c'è, si estrae da quella che
  disegna, con un test che due stati diversi solo nel `ts` diano lo stesso modello.
- [ ] **Passo 2:** nella scheda live, tick da 250 ms solo durante la tenuta o il conto alla rovescia (`card.holding ||
  card.countdown != null`), altrimenti 1 s.
- [ ] **Passo 3: commit** `perf: widgets redraw only when their model changes; live card ticks at 1 s when idle`

### Task 13 (A8): la cache delle chat sopravvive alla ricreazione dell'activity

**Files:**
- Create: `core/src/main/kotlin/it/pixelbox/cmwatch/data/FeedCache.kt`
- Modify: `mobile/.../MainActivity.kt:450-453` (al posto di `remember { mutableStateMapOf }`), `mobile/.../PhoneApp.kt`
- Test: `core/src/test/.../data/FeedCacheTest.kt`

**Interfacce:**
- Produce: `class FeedCache(dir: File) { val entries: StateFlow<Map<String, List<TranscriptEntry>>>; fun put(name: String, list: List<TranscriptEntry>); fun get(name: String): List<TranscriptEntry> }`.
  Tiene in memoria le ultime 50 voci per sessione e le salva su file, una sessione per file, scrivendo in modo atomico
  (file temporaneo e poi rename), come fa `ChatLog`.

- [ ] **Passo 1: test:** `put` poi un'istanza nuova sulla stessa cartella; `get` restituisce le stesse 50 voci. Una
  sessione con 80 voci ne salva 50.
- [ ] **Passo 2: codice**, istanza in `PhoneApp`, letta da `MainActivity`.
- [ ] **Passo 3: prova:** sul Chromebook, con la finestra dell'app ridimensionata, le colonne riappaiono subito con le
  voci di prima e nel log del relay non parte una lettura completa per colonna.
- [ ] **Passo 4: commit** `perf(mobile): chat cache kept by the app process and on disk, not in the composition`

---

## Fase 4: misure dopo e rilascio

### Task 14: le stesse misure, confrontate

- [ ] **Passo 1:** `scripts/relay-measures.py` su tre finestre dopo l'installazione, con un uso normale paragonabile a
  quello del Task 0. Nota `docs/verifiche/2026-10-xx-prestazioni-dopo.md` con la tabella prima e dopo:
  - push all'ora e intervallo, che dipendono soprattutto dalla parte relay;
  - letture della web all'ora, con obiettivo meno di 150 e meno di 30 da ferme;
  - letture del telefono all'ora;
  - righe `tile:` (obiettivo meno di 50 ms);
  - `state age` mediana e p90;
  - comandi doppi: zero.
- [ ] **Passo 2:** la checklist dal vivo `docs/verifiche/2026-10-xx-prestazioni-dal-vivo.md` (Task 1 passo 6, Task 4
  passo 4, Task 9 passo 6, Task 13 passo 3), con le prove fatte da Franz e gli esiti veri.

### Task 15: rilascio

- [ ] **Passo 1:** golden registrati in CI (`record=true`), guardati uno per uno, committati.
- [ ] **Passo 2:** APK release su win, firmato col keystore che ora sta lì: `.\gradlew.bat :mobile:assembleRelease
  :wear:assembleRelease`. Installazione con `adb install -r` sui dispositivi giusti per modello (memoria
  adb-phone-vs-watch), solo quando Franz non li sta usando.
- [ ] **Passo 3:** web con `npm run build`, che pubblica sul relay.
- [ ] **Passo 4:** push di `feature/tablet` (approvato con il piano) e riga nel recap.

## Ordine consigliato e costo

Fase 1 in questo ordine: Task 0, 1, 6, 2, 3, 5, 4. Prima i guasti visibili a Franz (chat ferma, tile), poi il carico
web, che è il più grande sul relay. La fase 2 parte appena team-supervisor rilascia la 1.43; la fase 3 può andare in
parallelo alla 2.

## Stato al 07/10 alle 23:20

Fatti, verificati su win con i test (falliscono solo i 5 di `ChatLogTest`, noti su Windows) e in CI:
- **Fase 1:** Task 0 (`fcc6093`), 1 (`a3618ce`), 2 (`96af37b`), 3 (`05697a0`), 4 (`807d954`), 5 (`b977f67`, web pubblicata), 6 (`b644175`).
- **Fase 3:** Task 10 (`47d92b5`), 11 (`29ca7dd`), 12 (`d849f64`), 13 (`1e546eb`).

Deviazioni dal testo dei task, decise durante il lavoro:
- **Task 4:** l'esito di un comando si legge ancora una volta al secondo. Le attese crescenti avrebbero ritardato di
  qualche secondo la risposta che si vede dopo un prompt; lo stream dell'esito resta da fare.
- **Task 10:** Room tiene ancora 30 giorni e non 7, perché il Registro mostra i giorni passati. Cambia solo il modo:
  uno stream filtrato agli ultimi 200 e solo le chiavi nuove scritte.
- **Task 11:** sveglie FCM con `REPLACE` e non `KEEP`: con `KEEP` si perderebbe uno stato nuovo arrivato mentre il GET
  di prima era già partito, e la chiamata in corso ora si può interrompere.

Da fare:
- **Fase 2** (Task 7, 8, 9): aspetta il relay; `published_at` arriva con la sua fase 1, `tx` e il ritiro con la fase 2.
- **Task 14:** misure del «dopo». La web va misurata quando torna aperta. Tile ed età dello stato vanno misurate col debug
  wireless dell'orologio acceso.
- **Task 15:** rilascio finale e prove dal vivo con Franz (chat che segue, tile veloce, cambio di rete).
