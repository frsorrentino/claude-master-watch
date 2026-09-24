# Verifica dal vivo: accoppiamento dal telefono (fondamenta)

Design `docs/plans/2026-09-24-app-telefono-fondamenta-design.md`, piano `docs/plans/2026-09-24-app-telefono-fondamenta-piano.md`.

Relay di prova: `CLAUDE_MASTER_CONFIG=<configurazione di prova>`, mai quella principale finché Franz non lo decide.

| # | Cosa | Come | Esito |
|---|------|------|-------|
| A1 | `relay setup` crea un Firebase nuovo senza console, salvo i passi che dichiara manuali | configurazione di prova, progetto nuovo | |
| A2 | QR letto, tre passi accesi uno dopo l'altro, «Fatto» con la vibrazione; l'orologio mostra «Accoppiato con \<PC\>» | `relay pair`, telefono, «Accoppia» | |
| A3 | Un push sveglia l'orologio | una sessione che cambia stato, o il push di prova del Task 1 | |
| A4 | Un comando dall'orologio va a buon fine | Scheda, un'azione qualsiasi | |
| A5 | Rifare l'accoppiamento esclude i dispositivi vecchi | un secondo accoppiamento, poi `/allowed` | |
| A6 | Orologio spento dopo la conferma del PC: telefono «In attesa dell'orologio», poi «Chiave consegnata» alla riaccensione | spegnere l'orologio al passo PC | |
| A7 | La build con la configurazione dentro si accoppia ancora con il codice a 6 cifre | release di sviluppo, «Usa il codice» | |
| A8 | Con «riduci animazioni» niente movimento, tutto al suo stato finale | impostazioni di accessibilità del telefono | |
| A9 | QR scaduto, QR di un'altra app, testo incollato sbagliato: messaggio giusto, niente attese | i tre casi | |
| A10 | Cambio di progetto Firebase: dopo un accoppiamento con il progetto di prova 1, un QR del progetto di prova 2 riavvia telefono e orologio e li accoppia al 2; se il PC non conferma, tutti e due mostrano «non accoppiato», non un accoppiamento morto | due configurazioni di prova, `relay pair` sulla seconda; poi la seconda con il relay spento | |
