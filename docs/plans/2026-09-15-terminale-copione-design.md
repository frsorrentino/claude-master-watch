# Terminale «copione» e aggiornamento dal vivo — design

Approvato da Franz il 15/09/2026 21:28. Nasce dal confronto con Agent Watch (eventi degli hook in SSE): da loro si
prende l'idea del terminale che si muove da solo, non il collegamento diretto in LAN (perderebbe fuori casa e E2E).

## 1. Le voci (core, `TerminalText`)

`Row` guadagna un `kind`, ereditato dalla testa del blocco fino alla testa successiva:

| kind | riconoscimento | segno tolto |
|---|---|---|
| `USER` | riga che inizia con `❯` o `›`; blocco «User answered Claude's questions:» e le sue righe `· … → …` | `❯`, `›` |
| `CLAUDE` | `⏺`/`●` seguito da testo che non è una chiamata a strumento; righe a capo sotto | `⏺`, `●` |
| `TOOL` | chiamata a strumento (`TOOL` attuale, anche dopo `⏺`) | `⏺`, `●` |
| `OUTPUT` | righe sotto un `TOOL` | — |

Le righe prima della prima testa sono `OUTPUT`. Titoli `#` restano `CLAUDE` in grassetto. Il resto della pulizia
(ANSI, cornici, rumore, numeri di riga, `MAX` 30) non cambia.

## 2. L'aspetto (wear, `TerminalScreen`)

- `USER`: sans 15 sp, `CmColors.actionIcon`, filo `CmColors.accent` 3 dp a sinistra su ogni riga del blocco
  (disegnato nell'item: resta continuo con `SurfaceTransformation`), 8 dp dopo il filo.
- `CLAUDE`: sans 15 sp, `CmColors.text`, interlinea 21 sp.
- `TOOL`: mono 13 sp grassetto, `CmColors.text2`. `OUTPUT`: mono 13 sp, `CmColors.text2`.
- 8 dp prima di ogni blocco `USER` o `CLAUDE`; nessuna riga vuota aggiunta.
- Divisore fra Risposta e terminale: filo sottile + «Terminale · HH:mm» (ora dell'ultima cattura arrivata), al posto
  di «Righe del terminale».
- Piena larghezza, niente bolle, niente allineamento a destra.

## 3. Aggiornamento dal vivo (wear, rotta `TERMINAL`)

- Con la schermata aperta, a ogni cambio della voce della sessione in `/state` (già in streaming): nuovo `screen`.
- Una richiesta alla volta, almeno 3 s fra due richieste; quella persa nell'intervallo parte alla fine.
- Passaggio della sessione a ferma (inattiva o in attesa): anche `last`, così la Risposta si aggiorna.
- Durante la richiesta resta la cattura precedente (niente «Chiedo al PC» sopra un testo già visto).
- In fondo alla lista la vista segue le righe nuove; scrollata in su, resta dov'è.
- Icona Aggiorna invariata. Contratto invariato.

## 4. Verifica

- `TerminalTextTest`: tipi di riga su catture vere (prompt, risposta a domanda, prosa, strumento con output).
- Paparazzi: `terminal` in `ScreensSnapshotTest`, record in CI.
- Al polso: Terminale aperto su una sessione che lavora, le righe arrivano da sole; scroll in su non salta.
