# Verifica dal vivo: accoppiamento dal telefono (fondamenta)

Design `docs/plans/2026-09-24-app-telefono-fondamenta-design.md`, piano `docs/plans/2026-09-24-app-telefono-fondamenta-piano.md`.

Relay di prova: `CLAUDE_MASTER_CONFIG=<configurazione di prova>`, mai quella principale finché Franz non lo decide.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| A1 | `relay setup` crea un Firebase nuovo senza console, salvo i passi che dichiara manuali | configurazione di prova, progetto nuovo | ✅ 25/09 07:02-07:14, claude-master `relay setup` su `claude-master-relay-6511` (config `~/.claude-master/relay-prova/config.json`): 0 FAIL, unico passo manuale l'inizializzazione di Authentication in console |
| A2 | QR letto, tre passi accesi uno dopo l'altro, «Fatto» con la vibrazione; l'orologio mostra «Accoppiato con \<PC\>» | `relay pair`, telefono, «Accoppia» | ✅ 25/09 11:31, telefono Pixel 11 Pro XL + Pixel Watch 5, release firmate `ee01fff`: QR di `relay pair` inquadrato dal terminale, tre passi accesi, telefono «Connected to penguin · Key delivered», relay «pair: ok su /pair/<id>» con i due uid in `devices.json`; l'orologio si è riavviato sul progetto 6511 (`fcm topic watch: subscribed` 11:31:31) |
| A3 | Un push sveglia l'orologio | una sessione che cambia stato, o il push di prova del Task 1 | ✅ 25/09 11:33, `relay push` di prova: `/state` pubblicato (8 sessioni), sull'orologio `fcm message: {kind=launched…}` e `wake worker` |
| A4 | Un comando dall'orologio va a buon fine | Scheda, un'azione qualsiasi | ✅ 25/09 11:38, dal polso: `screen cyberflavour-it` → ok (testo del terminale tornato), poi `last cyberflavour-it` → ok; relay di prova «comandi 2» |
| A5 | Rifare l'accoppiamento esclude i dispositivi vecchi | un secondo accoppiamento, poi `/allowed` | ✅ 25/09 12:26, secondo accoppiamento sul 6511 con gli stessi due dispositivi: il relay riscrive `/allowed` e `devices.json` con i soli uid del nuovo accoppiamento (`paired_at` nuovo, stessi uid: l'uid anonimo resta finché il progetto non cambia) |
| A6 | Orologio spento dopo la conferma del PC: telefono «In attesa dell'orologio», poi «Chiave consegnata» alla riaccensione | spegnere l'orologio al passo PC | |
| A7 | La build con la configurazione dentro si accoppia ancora con il codice a 6 cifre | release di sviluppo, «Usa il codice» | |
| A8 | Con «riduci animazioni» niente movimento, tutto al suo stato finale | impostazioni di accessibilità del telefono | |
| A9 | QR scaduto, QR di un'altra app, testo incollato sbagliato: messaggio giusto, niente attese | i tre casi | ✅ 25/09 11:42-11:43: QR di un link → «Invalid code: run relay pair again» con «Scan the QR again»; QR scaduto delle 11:29 → «Code expired: run relay pair again», nessuna chiamata al relay (log fermo alle 11:38) |
| A10 | Cambio di progetto Firebase: dopo un accoppiamento con il progetto di prova 1, un QR del progetto di prova 2 riavvia telefono e orologio e li accoppia al 2; se il PC non conferma, tutti e due mostrano «non accoppiato», non un accoppiamento morto | due configurazioni di prova, `relay pair` sulla seconda; poi la seconda con il relay spento | |
