# Fase 2 — checklist dal vivo

«Fatto» della fase (design, sezione 6): una domanda reale risposta dal polso, registrata nel ledger.
Richiede il relay della fase 1 (repo claude-master), il progetto Firebase e il Pixel Watch: **in attesa di Franz**.

Quello che si prova senza orologio, sul `Transport` finto (fixture del contratto):

| # | Prova | Come | Esito 12/09/2026 |
|---|---|---|---|
| 1 | L'app apre da Room senza rete | `adb shell svc wifi disable` poi avvio: lista dall'ultimo stato | da provare sul polso (ARC non ha Wi-Fi separato) |
| 2 | Lista in ordine ❓ ▶ ✓ ✗, pallino account, domanda sotto la riga | avvio con fixture 1 | ✅ ARC, `docs/screenshots/fase2-sessioni-arc.png` |
| 3 | Scheda con → prossimo, esito, Rispondi / Scrivi / Terminale / Segui | tap su una riga | ✅ ARC, `fase2-scheda-arc.png` |
| 4 | Domanda a schermo intero: testo intero, bottoni larghi uno sotto l'altro, ambra per tier medium | apertura automatica all'avvio | ✅ ARC, `fase2-domanda-arc.png` |
| 5 | Risposta «1» → domanda sparita, sessione ▶, vibrazione «inviato» + «confermato» | tap su «1 · yes» | ✅ ARC (vibrazione non verificabile su ARC), `fase2-dopo-risposta-arc.png` |
| 6 | Fixture 3 → chip «PC fermo da N min», comandi disabilitati | Impostazioni → Demo → 3-stale | ✅ ARC, `fase2-pc-fermo-arc.png` (vedi sotto) |
| 7 | «Scrivi» apre la tastiera di sistema e manda un `prompt` | Scheda → Scrivi | solo sul polso (ARC non ha la tastiera Wear) |
| 8 | Pairing con codice a 6 cifre (finto: qualsiasi) | avvio pulito → Inserisci codice | ✅ schermata su ARC (`fase2-pairing-arc.png`); tastiera solo sul polso |
| 9 | Tier high: pressione lunga obbligatoria | fixture con `tier: high` (da aggiungere quando il relay la produce) | da provare |
| 10 | Domanda risposta altrove → «Già risposta da un altro canale» e chiusura | serve il relay reale | in attesa |

Unit test (`./gradlew :core:testDebugUnitTest`): 57 verdi il 12/09/2026 — contratto sulle fixture, ordine, freschezza,
blob AES-GCM, X25519/HKDF, KeyVault, FakeTransport, Repo (Room finto, ottimismo, timeout 20 s, coda offline), ViewState, testi.
