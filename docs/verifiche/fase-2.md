# Fase 2 — checklist dal vivo

«Fatto» della fase (design, sezione 6): una domanda reale risposta dal polso, registrata nel ledger.
Stato 12/09 16:16: relay reale attivo, Firebase creato (auth anonima), Pixel Watch accoppiato via adb wireless; pairing X25519 reale
riuscito alle 16:10 (uid ZVp5…, check del PC ok); `/state` vero decifrato e mostrato (`fase2-sessioni-watch.png`); `/cmd screen` →
`/result` reso nel Terminale (`fase4-terminale-watch.png`); prima domanda reale arrivata (`fase2-domanda-reale-watch.png`) ma con
testo grezzo e senza opzioni dal relay: risposta «2» dal polso e ledger in attesa della correzione lato relay.

Quello che si prova senza orologio, sul `Transport` finto (fixture del contratto):

| # | Prova | Come | Esito 12/09/2026 |
|---|---|---|---|
| 1 | L'app apre da Room senza rete | `adb shell svc wifi disable` poi avvio: lista dall'ultimo stato | da provare sul polso (ARC non ha Wi-Fi separato) |
| 2 | Lista in ordine ❓ ▶ ✓ ✗, pallino account, domanda sotto la riga | avvio con fixture 1 | ✅ ARC, `docs/screenshots/fase2-sessioni-arc.png` |
| 3 | Scheda con → prossimo, esito, Rispondi / Scrivi / Terminale / Segui | tap su una riga | ✅ ARC, `fase2-scheda-arc.png` |
| 4 | Domanda a schermo intero: testo intero, bottoni larghi uno sotto l'altro, ambra per tier medium | apertura automatica all'avvio | ✅ ARC, `fase2-domanda-arc.png` |
| 5 | Risposta «1» → domanda sparita, sessione ▶, vibrazione «inviato» + «confermato» | tap su «1 · yes» | ✅ ARC (vibrazione non verificabile su ARC), `fase2-dopo-risposta-arc.png` |
| 6 | Fixture 3 → chip «PC fermo da N min», comandi disabilitati | Impostazioni → Demo → 3-stale | ✅ ARC, `fase2-pc-fermo-arc.png` |
| 7 | «Scrivi» apre la tastiera di sistema e manda un `prompt` | Scheda → Scrivi | solo sul polso (ARC non ha la tastiera Wear) |
| 8 | Pairing con codice a 6 cifre | avvio pulito → Inserisci codice | ✅ reale sul Pixel Watch alle 16:10 (via extra `pair_code`; con la tastiera Wear il risultato non torna all'app: da indagare) |
| 9 | Tier high: pressione lunga obbligatoria | fixture con `tier: high` (da aggiungere quando il relay la produce) | da provare |
| 10 | Domanda risposta altrove → «Già risposta da un altro canale» e chiusura | serve il relay reale | in attesa |

Unit test (`./gradlew :core:testDebugUnitTest`): 57 verdi il 12/09/2026 — contratto sulle fixture, ordine, freschezza,
blob AES-GCM, X25519/HKDF, KeyVault, FakeTransport, Repo (Room finto, ottimismo, timeout 20 s, coda offline), ViewState, testi.

Screenshot test (Paparazzi 1.3.5, `wear/src/test/kotlin/…/ScreensSnapshotTest.kt`, 456×456 tondo): su questa macchina
(linux-aarch64) il layoutlib nativo non si carica («Failed to init Bridge»), quindi si registrano in GitHub Actions
(`workflow_dispatch` con `record = true`, artifact `paparazzi-snapshots` da committare in `wear/src/test/snapshots/`) e da lì
in poi il workflow li verifica a ogni push. In locale: `./gradlew :core:testDebugUnitTest` (i test di `:wear` sono solo Paparazzi).
