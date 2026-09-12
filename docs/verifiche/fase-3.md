# Fase 3 — checklist dal vivo

«Fatto» della fase (design, sezione 6): una domanda vista sul quadrante senza aprire l'app.
Stato 12/09 17:00: Firebase reale (auth anonima, RTDB europe-west1, FCM), relay attivo, Pixel Watch accoppiato con chiave X25519
reale (16:10). Verificato dal vivo: `/state` decifrato e stream, `/cmd screen` → `/result`, domanda reale con tre opzioni risposta
dal polso e registrata nel ledger (16:23). Lato app: token FCM ottenuto e topic «watch» iscritto (16:57). Nessun risveglio FCM
ricevuto finora: invio dal relay da verificare (in corso con la sessione claude-master).

| # | Prova | Come | Esito |
|---|---|---|---|
| 1 | FCM sveglia l'app chiusa: un GET di /state, notifica «❓ nome» con testo intero e azioni «1 · …», «2 · …», Apri, Rispondi | `claude-master relay push` con una domanda aperta, orologio con app chiusa | in attesa (Firebase) |
| 2 | Tocco su «1 · yes» nella notifica → `answer` sul PC, notifica sparita, vibrazione | idem | in attesa |
| 3 | «Rispondi» dettato → `prompt` sul PC | idem | in attesa |
| 4 | Esito della sessione seguita → «✓ nome» + short, vibrazione singola | `claude-master follow NOME`, poi fine turno | in attesa |
| 5 | Sessione sparita → «✗ nome», vibrazione lunga | chiusura della sessione | in attesa |
| 6 | Quota ≥ 95 % → «⚠ 95 % personale» | quota vera o `relay push` con fixture | in attesa |
| 7 | Complication SHORT_TEXT «1?» / «▶3» / «✓», tap → sessione ferma | quadrante con lo slot | in attesa (Pixel Watch) |
| 8 | Complication RANGED_VALUE anello quota 5 h dell'account scelto, tap → Quota | idem | in attesa |
| 9 | Complication LONG_TEXT «❓ ledger-api · Deploy now?» | quadrante WFF | in attesa |
| 10 | Tile: «4 sessioni · 1? · 1✗», la domanda su una riga intera, Apri · Sessioni; senza domande Sessioni · Quota; «PC fermo» | tile aggiunta | in attesa (Pixel Watch) |
| 11 | Nessuna notifica con l'app in primo piano | app aperta + push | in attesa |
| 12 | Dopo il pairing reale il Transport passa a Firebase senza riavvio | Impostazioni → Nuovo pairing → codice del PC | ✅ 16:10, lista con le sessioni vere subito dopo (`fase2-sessioni-watch.png`) |

Provato senza orologio (12/09/2026): unit test `:core` 94 verdi (FirebaseTransport su MockWebServer: stream /state, GET, /cmd → /result,
timeout, pairing con verifica del PC, chiave errata; regole Wake, testi di notifiche/complication/tile; scelta del Transport);
APK debug installato su ARC, avvio con permesso notifiche e Domanda (`docs/screenshots/fase3-domanda-arc.png`).
Tile e complication non esistono su ARC: solo compilazione e manifest.
