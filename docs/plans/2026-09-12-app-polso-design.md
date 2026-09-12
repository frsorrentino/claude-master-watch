# App nativa Wear OS per claude-master — design (12/09/2026)

Decisioni prese in sessione con Franz il 12/09/2026, dopo il censimento di 14 app
(«Polso» in `docs/research/2026-09-11-ecosistema.md`) e la lettura del codice di
claude-watch (shobhit99), claude-watch/Remmy (fotescodev) e ccwearos.

## Decisioni fisse

| Cosa | Scelta | Perché |
|---|---|---|
| Scopo | colpo d'occhio **e** controllo pieno | Franz, 10:48 |
| Orologio | Pixel Watch 5, 45 mm, Wear OS 7, tondo, corona | |
| Stack | Kotlin, Wear Compose Material 3 (`TransformingLazyColumn`), ProtoLayout M3 (tile), `ComplicationDataSourceService`, Room, FCM | tile/complication e scroll delle app Google; Flutter/RN non li hanno |
| Trasporto | Firebase RTDB + FCM dietro interfaccia `Transport`; relay PHP su hosting proprio = v2 | unico senza polling in entrambi i versi, sveglia push, zero infrastruttura |
| Architettura | **C+**: modello di dominio + feed eventi sul bus; presentazione, cache e preferenze sull'orologio | né grezzo (B) né pre-renderizzato (A) |
| Repo | `personali/claude-master-watch` (app) + `cm-relay.py` nel plugin | l'app è Gradle e pesa, il plugin resta piccolo |
| Voce | niente microfono in-app: tastiera Wear OS (detta già); **TTS** con tasto ▶ per testi > `tts.min_chars` (120) o di tipo esito/risposta/domanda | Franz, 10:56 e 11:00 |
| Identità | cobalto pixel-ui, un solo bottone pieno per schermata, tema scuro unico | Franz, 11:05 |
| Larghezza | ogni riga logica occupa la riga fisica; niente colonne di frammenti; niente «…» nel corpo | Franz, 11:14 e 11:18 |

Dal codice altrui: timeout/verità sul PC (mai una domanda che vive solo sul polso);
un solo `ViewState` con priorità; tier di rischio → colore e gesto; refresh adattivo con
un solo debounce; rilevanza di tile/complication solo con domanda aperta; aptica per tipo;
E2E sopra il bus; feed terminale formattato per tool. Da non copiare: modalità permessi
cosmetica, due palette, azioni di notifica non gestite.

## 1. Architettura e flusso

```
hook Stop/PermissionRequest/SessionStart/End ─┐
bot serve (segui, quota) · cron 60 s (battito) ─┼─▶ cm-relay.py push ──▶ RTDB /state  (documento intero)
                                               │                    ──▶ RTDB /events/<ts>  (append)
                                               │                    ──▶ FCM data {kind, session}
cm-relay.py serve ◀── SSE su /cmd ──────────────┘
      │ esegue: claude-master answer|talk|launch|screen|follow|resume
      └─▶ RTDB /result/<id>, poi ripubblica /state

Pixel Watch: FCM sveglia → 1 GET /state → tile + complication + notifica locale
             app aperta → stream /state e /events → Room → UI
             tap → /cmd/<uuid> (ottimistico) → attende /result
```

**Cifratura.** Ogni documento è `{"v":1,"enc":"<base64>"}`: blob intero AES-256-GCM con
chiave concordata al pairing (X25519 via `/pair/<code>`, codice a 6 cifre mostrato dal PC,
chiave nel Keystore dell'orologio, `~/.claude-master/relay/key` 0600 sul PC). Firebase vede
solo timestamp e dimensioni. Auth: orologio anonimo con uid registrato al pairing; PC con
service account; regole: solo quell'uid legge `/state` `/events` `/result` e scrive `/cmd`.

**`/state`** (sostituito intero, ≤ 8 KB):
- `v, ts, host`
- `sessions[]`: `id, name, account, project, state (waiting|busy|idle|awaiting|gone), since, turn_started, tool, link, attached, followed`
  - `question: {id, kind (permission|ask|plan), text, options[{n,label}], tier (low|medium|high), asked_at} | null`
  - `outcome: {short ≤ 60, full ≤ 600, at} | null`
  - `next | null`
- `quota: {personale: {h5, w7, reset_w7, stale}, agenzia: {…}}`
- `projects[]`: `{path, name, account}` lanciabili
- `night: {queued, running}` · `recap: {date, items[{project, done, next}]}`

**`/events/<ts>_<seq>`**: `{kind (question|answered|outcome|gone|launched|quota|resumed), session, account, ts, title, body, ref}`. Il PC cancella oltre 7 giorni; l'orologio tiene 30 giorni in Room.

**`/cmd/<uuid>`**: `{op (answer|prompt|launch|follow|unfollow|resume|screen|allow_all), session, arg, issued, by}`.
**`/result/<uuid>`**: `{ok, text, at}`; per `screen`, `text` = 30 righe formattate per tool.

**Freschezza e guasti.** `ts` battuto ogni 60 s: oltre 3 min l'app mostra «PC fermo da N min»
e disabilita i comandi. Comando senza `/result` in 20 s → «non consegnato», Riprova.
Nessun timeout sul polso: la domanda resta nel registro finché qualcuno risponde da un
canale qualsiasi; `/state` la toglie quando è risolta.

## 2. Superfici dell'orologio

**Complication** (tre tipi, stessa sorgente): `SHORT_TEXT` «1?» / «▶3» / «✓», tap → sessione
ferma; `RANGED_VALUE` anello quota 5 h dell'account scelto, tap → quota; `LONG_TEXT`
«❓ ledger-api · Deploy now?» o «▶ 2 · ✓ 3» per i quadranti WFF. Refresh su ogni FCM;
timeline 30 s con domande, 15 min idle; un solo coordinatore dei reload.

**Tile** (ProtoLayout M3), fissa: «5 sessioni · 1? · 1✗»; la sessione ferma con la domanda
su una riga intera; bottoni **Apri** (deep link) · **Sessioni**. Senza domande: la seguita o
la più recente, bottoni **Sessioni** · **Quota**. Solo `LaunchAction`.

**Notifiche** (locali da FCM, solo con app non in primo piano): domanda = «❓ nome» + testo
intero, azioni prime due opzioni + **Apri**, `RemoteInput` «Rispondi», vibrazione doppia;
esito seguito = «✓ nome» + `short`, vibrazione singola; sparita = «✗ nome», lunga; quota a
soglia = «⚠ 95 % personale, reset 13:10». Tutto il resto silenzioso. Una notifica per
sessione, sostituita, mai accumulata.

**App**, un solo `ViewState` con priorità `offline > domanda > schermata scelta`:
- **Sessioni**: `TransformingLazyColumn`, una riga a tutta larghezza «❓ 🔴 ledger-api · 2 m»; ordine ❓ ▶ ✓ ✗; chip «PC fermo» solo se serve.
- **Scheda**: riga nome · account · stato · durata; `→ prossimo`; esito con ▶; bottoni **Rispondi** / **Scrivi** / **Terminale** / **Segui**.
- **Domanda**, schermo intero: testo intero, opzioni come bottoni pieni larghi uno sotto l'altro; `tier=high` con pressione lunga, niente swipe; testo libero con la tastiera di sistema; auto-apertura all'arrivo.
- **Esito**: `short` grande, `full`, ▶ per leggerlo, **Leggi tutto** chiede al PC.
- **Terminale**: 30 righe mono, una per tool. **Timeline**: `/events` per giorno, filtro sessione.
- **Lancia**: `projects` con account. **Quota**: due anelli e reset. **Recap**: voci del giorno. **Notte**: coda.
- **Impostazioni**: soglia TTS, vibrazioni per tipo, account della complication, pairing.
- **Segui**: `OngoingActivity` «▶ ledger-api 4 m» sul quadrante. **Ambient**: icona e nome, bassa densità.

## 3. Sistema visivo

Tema unico scuro. Token come ruoli: `bg` #000000 · `surface` #121417 · `line` #2A2E35 (bordi,
niente ombre) · `text` #F2F4F7 · `text2` #9AA3B2 · `accent` cobalto #4C7DFF (premuto #3457D5),
solo su bottone pieno, scroll bar, chip «seguita», anello quota. Stati: `waiting` ambra
#FFB020 · `busy` #7FA1FF · `idle` #34C759 · `gone` #FF453A · `stale` #6B7280. Account: pallino
🔴/🟢 identico a Telegram. Tier: low neutro, medium pieno ambra, high pieno rosso con
pressione lunga.

Tipografia (Roboto Flex di sistema): nomi e terminale `Roboto Mono` 14 sp; riga di lista 16 sp
con ellissi solo sul nome; domanda 18/15 sp; esito 20/15 sp; minimo 13 sp.

Layout (456 px): margini 5,2 %, `ScreenScaffold` con `TimeText`; schede piene con bordo
`line`, raggio 20 dp; pillole 52 dp, una piena per schermata; bersagli ≥ 48 dp; opzioni mai
affiancate; corona per scroll e snap.

Movimento: molla Material (damping 0,8); respiro 3 s dell'icona ▶ della seguita; niente con
Reduce Motion; nessuna waveform finta. Aptica (`VibrationEffect`): domanda 2×60 ms; esito
40 ms; sparita 200 ms; inviato tick; confermato due tick; errore tre colpi. Icone: Material
Symbols Rounded; stati ❓ ▶ ✓ ✗ come icone, stessi significati ovunque.

## 4. Lato PC: `cm-relay.py`

Comandi: `relay pair` (codice 6 cifre, X25519, TTL 5 min, 5 tentativi) · `relay push [--async]`
(costruisce `/state`, scrive `/events`, FCM; `--async` chiude in < 2 s) · `relay serve`
(SSE su `/cmd`, esegue, `/result`, ripubblica; lock, pidfile, backoff 1-2-5-15-30 s) ·
`relay ensure` · `relay status` · `relay install|uninstall` (cron: ensure + battito 60 s) ·
`relay off`.

Sorgenti di `/state`, tutte esistenti: `cm-sessions.py --json`; `cm-answer.parse` (senza piè
di pagina); `watch` + ultimi 600 caratteri dal ledger; cache del recap; `cm-quota.py --json`;
inventario per `projects`; coda di `cm-night`. `tier` da euristica sul testo del permesso
(`rm -rf`, `git push`, `deploy`, `DROP`, `ssh` → high; Edit/Write/Bash → medium; Read/Grep/
WebFetch → low), lista in `config.json` `relay.tier_high[]`.

Chi chiama `push`: `cm-hook.py` su `PermissionRequest`, `Stop`, `SessionStart`, `SessionEnd`
(`--async`, dopo il lavoro attuale); `bot serve` su segui/smetti e soglia quota; il cron per
il battito. Debounce 2 s.

Esecuzione, allow-list fissa via CLI esistente: `answer` → `claude-master answer NOME N`
(checkpoint git come dal bot) · `prompt` → `talk NOME "Da Franz via polso: …"` con richiesta
della riga `Watch:` · `launch` → `launch PATH` · `follow/unfollow` → stato del bot · `resume`
→ «riprendi dove eri» · `screen` → `cm-screen` formattato, 30 righe · `allow_all` → `answer`
con «don't ask again» se esiste, altrimenti errore. `mode` rinviato: senza `claude-master
mode` sarebbe cosmetico. Ogni comando: `result`, poi `push`; duplicati per `uuid` ignorati.

Config: `relay.enabled` (off) · `relay.firebase_url` · `relay.service_account` ·
`relay.fcm_topic` · `relay.tier_high[]` · `relay.state_max_kb` (8) · `relay.events_days` (7).
Niente SDK: `requests` su REST + SSE di RTDB e FCM HTTP v1.

Test: RTDB finto (HTTP locale in `tests/lib/`), claude finto e Telegram finto esistenti;
fixture `tests/fixtures/relay/state-*.json` generate dal Python e copiate identiche nel repo
dell'app per i test Kotlin.

## 5. Errori, offline, sicurezza

- **PC spento o relay fermo**: `ts` vecchio → chip «PC fermo da N min», comandi disabilitati,
  ultimo stato leggibile da Room; la tile mostra «PC fermo» al posto dei conteggi.
- **Orologio senza rete**: l'app apre subito da Room; i comandi si accodano localmente
  (max 10, 10 min) e partono al ritorno della rete, con «in attesa» visibile; oltre 10 min
  si scartano con avviso.
- **Comando senza risultato** entro 20 s: «non consegnato», Riprova; il PC ignora i
  duplicati per `uuid`, quindi Riprova è sicuro.
- **Domanda già risposta altrove** (Telegram, terminale): `/state` non la contiene più,
  la schermata Domanda si chiude con «già risposta da …» e vibrazione singola.
- **Due orologi o un telefono in più**: un uid per dispositivo, stessa chiave; `by` nel
  comando dice chi ha risposto; il registro lo annota.
- **Chiave persa** (orologio resettato): `relay pair` di nuovo; il PC revoca l'uid vecchio.
- **Sicurezza**: blob cifrato E2E, Firebase non legge nulla; service account solo sul PC
  (0600), mai nel repo; regole RTDB per uid; nessun comando fuori allow-list; `tier=high`
  richiede pressione lunga; `allow_all` non esiste per `high`; `launch` solo su `projects`
  pubblicati dal PC, mai percorsi liberi dal polso.
- **Batteria**: nessun servizio in primo piano; nessuna connessione persistente ad app
  chiusa; FCM + un GET; stream RTDB solo con app aperta; tile e complication con un solo
  debounce e timeline adattiva.

## 6. Build, test, rilascio, fasi

- **Build**: come `personali/watchface`: Gradle con JDK 17, SDK 35/36, `-Xmx2048m`; GitHub
  Actions `assembleRelease` con keystore da secret base64; `adb` wireless sul Pixel Watch;
  emulatore Wear OS per le schermate. Play Store fuori scope.
- **Test**: unit Kotlin sul contratto con le fixture condivise (`state-*.json`); screenshot
  test delle schermate principali con `Paparazzi`; test Python del relay con RTDB finto;
  una prova dal vivo per fase con la checklist sotto.
- **Fasi**, ognuna con «fatto» verificabile:
  1. `cm-relay.py` push + pair + RTDB finto; `/state` cifrato letto da uno script di prova. *Fatto: fixture generate e verificate.*
  2. App: pairing, Room, lista Sessioni, Scheda, Domanda con `answer`. *Fatto: una domanda reale risposta dal polso, registrata nel ledger.*
  3. Complication + tile + notifiche + aptica. *Fatto: domanda vista sul quadrante senza aprire l'app.*
  4. Esito con TTS, Terminale, Timeline, Lancia, Segui con ongoing activity, Quota, Recap, Notte, Impostazioni. *Fatto: checklist di 10 voci dal vivo.*
  5. Rifinitura visiva sul Pixel Watch: larghezze, ambient, movimento. *Fatto: screenshot delle 8 schermate approvati da Franz.*
- **Stima**: ~2.500-3.000 righe Kotlin, ~500 Python, 6-7 giorni. Fasi 1-3 = il 90 % del
  valore al polso; 4-5 completano l'app.
- **Fuori scope v1**: `mode` dal polso, telefono companion, relay su hosting proprio, Play
  Store, storico oltre 30 giorni, più PC.

## Prossimo passo

Piano di implementazione con `superpowers:writing-plans`, partendo dalla fase 1 nel repo
claude-master (relay) e dalla fase 2 nel nuovo repo `personali/claude-master-watch`.
