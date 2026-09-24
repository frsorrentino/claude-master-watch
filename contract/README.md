# Contratto PC ↔ orologio (v1, aggiunte dalla 1.1 alla 1.15)

Questi file sono la verità condivisa fra `cm-relay.py` (plugin claude-master) e l'app.
Il Python li deve produrre identici (test in claude-master `tests/relay-verify.py`);
il Kotlin li deve leggere identici (test unit su `contract/*.json`). Chi cambia il
contratto cambia i file qui e in `claude-master/tests/fixtures/relay/`, e alza `v`.

Forma sul bus: ogni documento RTDB è `{"v":1,"enc":"<base64 AES-256-GCM>"}`; il contenuto
cifrato è il JSON di questi file. Schema per campo in
`docs/plans/2026-09-12-app-polso-design.md`, sezione 1.

- `state-1-question.json` — quattro sessioni (una ferma su domanda, una che lavora, una idle, una sparita), due account, quota, progetti, notte, recap.
- `state-2-idle.json` — tutto fermo, nessuna domanda.
- `state-3-stale.json` — nessuna sessione, `ts` vecchio: l'app mostra «PC fermo».
- `events-sample.json` — un evento per tipo.
- `cmd-result-sample.json` — un comando per `op` e il suo risultato, compresi due errori.
- `pair-qr.json` — il contenuto del QR di `relay pair` (1.15).
- `pair-response.json` — la risposta del telefono per sé e per l'orologio, e la conferma del PC (1.15).

Regole che i test verificano: `state` ≤ 8 KB; `outcome.short` ≤ 200; `outcome.full` ≤ 600;
`question.text` intero (mai troncato); `options[].n` da 1 senza buchi; `tier` ∈ low|medium|high;
`state` ∈ waiting|busy|idle|awaiting|gone; ordine delle sessioni: waiting, busy e awaiting (stesso rango:
awaiting lavora a un prompt partito dal polso), idle, gone, poi alfabetico.

Contratto 1.1 (12/09/2026, solo aggiunte): ogni sessione porta `icon` (emoji del badge della scheda del PC, stabile per la
vita della sessione; per una gone l'ultima nota o null) e `color` («#RRGGBB» del solo colore: 🟠🟧🧡 #F5A623 · 🟡🟨💛 #F4D03F ·
🔴🟥❤️ #E74C3C · 🟢🟩💚 #2ECC71 · 🔵🟦💙 #3B82F6 · 🟣🟪💜 #9B59B6 · ⚪⬜🤍 #BDC3C7 · 🟤🟫🤎 #8D6E63); forma dall'account
(tondo personale, quadrato gli altri); assenti o null = grigio #9B9B9B. `v` resta 1.

Contratto 1.2 (13/09/2026, solo aggiunte): `next_at` accanto a `next` (epoch della riga di recap che ha prodotto il
«prossimo», mezzanotte locale di quel giorno; null se non c'è un prossimo), così l'orologio sa se il piano è di oggi o
di tre giorni fa e lo può ordinare rispetto a `outcome.at`; `tool` è valorizzato anche per le sessioni `awaiting`, non
solo per le `busy`, e resta null per `idle` e `gone`. `v` resta 1.

Contratto 1.3 (14/09/2026, solo aggiunte): `quota.<account>.reset_h5`, quando riparte la finestra di 5 ore (epoch in
secondi, null se assente); `reset_w7` resta il reset settimanale. `v` resta 1.

Contratto 1.4 (14/09/2026, solo aggiunte): comando `last` — ultimo messaggio della sessione, intero fino a 4000
caratteri, tagliato a fine frase. `v` resta 1.

Contratto 1.5 (14/09/2026, solo aggiunte): ogni sessione porta `tool_note`, la description che Claude scrive accanto al
comando Bash, null se non c'è; i percorsi di Read, Edit e Write in `tool` sono assoluti. `v` resta 1.

Contratto 1.6 (14/09/2026, solo allargamenti): `outcome.short` fino a 200 caratteri (era 60), la riga «Esito:» o
«Watch:» intera tagliata a fine parola, senza «…»; `full` è la coda vera del messaggio, fino a 600. Quando lo stato
supera 8 KB il relay taglia in quest'ordine: voci del recap, sessioni finite più vecchie (restano le 3 più recenti; una
gone che esce dallo stato non genera eventi), progetti oltre il decimo; solo dopo `full` scende a 300 caratteri a fine
parola, e per ultimo `full` = `short`. Le fixture non cambiano. `v` resta 1.

Contratto 1.7 (14/09/2026, solo l'ordine dei tagli): sopra gli 8 KB il relay non toglie più per primo il recap (al
polso arrivava sempre vuoto con molte sessioni). Nuovo ordine: sessioni finite più vecchie (restano le 3 più recenti),
progetti oltre il decimo, `done` e `next` del recap tagliati a 80 caratteri a fine parola, `full` degli esiti a 300 e
poi = `short`, voci del recap dal fondo (ne resta sempre almeno una), sessioni dal fondo. La forma di `/state` non
cambia; le fixture nemmeno. `v` resta 1.

Contratto 1.8 (14/09/2026, solo aggiunte): quale account è personale lo dice il tipo, non il nome (decisione di Franz
delle 22:55; serve anche alla beta pubblica, dove gli account hanno nomi qualunque). Ogni sessione porta
`account_kind` e ogni voce della quota `kind`, entrambi «personal» o «work». Il relay li ricava da `accounts.<nome>.kind`
nella sua configurazione; se manca, l'account di default è «personal» e gli altri «work», e un account solo è
«personal» (claude-master `4e2968f`). Le fixture usano gli account `personal` e `work`, con testi e cartelle in inglese
(i marcatori «Esito:» e «prossimo:» restano, perché l'app li riconosce così). L'orologio usa il tipo per il
badge (tondo = personal), il colore delle notifiche, il ripiego e l'ordine della quota; se il campo manca, un relay
precedente, ripiega sul nome «personale». `v` resta 1.

Contratto 1.9 (15/09/2026, solo aggiunte): comando `reopen` — una sessione gone rilanciata nella sua cartella, stesso
account, senza finestra; `--resume` della sua conversazione se l'id è noto e il transcript esiste, altrimenti
`--continue` solo se nessun'altra sessione è viva nella cartella. Rifiuta un nome vivo, un nome fuori dal registro, una
cartella sparita. `resume` non cambia. `v` resta 1.

Contratto 1.9.1 (15/09/2026, solo comportamento): `reopen` apre la scheda del terminale sul desktop come `launch` (se il
desktop non c'è, parte senza finestra). Forma e testi invariati. `v` resta 1.

Contratto 1.9.2 (15/09/2026, solo comportamento, decisione di Franz delle 19:39): anche `launch` apre la scheda del
terminale sul desktop come `reopen` (se il desktop non c'è, parte senza finestra). Forma e testi invariati. `v` resta 1.

Contratto 1.10 (15/09/2026, solo aggiunte): `answer` accetta come arg anche `text:<testo>` («Type something.» con quel
testo, a capo → spazi) e `chat` («Chat about this»); risultato `answered {n}. {testo}` o `answered {n}. Chat about this`;
`text:` vuoto → «empty text», altri arg non numerici → «answer <arg>: expected a number, text:<text> or chat». Dopo
`chat` la sessione rifiuta la domanda e aspetta un messaggio. `v` resta 1.

Contratto 1.11 (16/09/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 00:26): ogni sessione porta
`model`, `effort` e `context`, letti dalla sua trascrizione. `model` è `{"id": "claude-opus-5[1m]", "label": "Opus 5"}`:
l'id completo come lo scrive Claude Code (il suffisso `[1m]` dice la finestra da 1M) e il nome breve, senza la
parentesi. `effort` è il livello dell'ultimo turno ("low", "medium", "high", "xhigh", "max"). `context` è la
percentuale intera 0-100 di finestra consumata: i token dell'ultimo turno (input + cache letta + cache scritta)
sulla finestra del modello (1M col suffisso `[1m]`, altrimenti 200k).

I tre campi ci sono sempre; valgono null quando non si leggono: nessuna trascrizione (per esempio una sessione
`gone`), o nessun turno dell'assistente. In più `context` è null quando il modello dell'ultimo turno non coincide con
quello della voce di sistema — un cambio di modello a metà sessione: lì la finestra non è certa e il numero NON si
stima. Le op `model` ed `effort` dal polso non fanno parte di questa versione: arrivano con la 1.12, insieme a
`choices`. `v` resta 1.

Contratto 1.11.1 (16/09/2026, correzione, segnalata dall'app alle 08:13): `model.label` e `context` arrivavano null su
ogni sessione viva. La voce di sistema che porta l'id completo e il nome del modello la scrive Claude Code all'AVVIO
della conversazione, quindi in una sessione lunga sta fuori dalla coda che il relay leggeva (misurato: una sola voce, a
221 KB su 4,5 MB). Ora il relay la cerca prima nella coda — un cambio di modello a metà sessione ne riscrive una lì, e
deve vincere — e poi nella testa. `context` resta null solo quando una voce c'è e non coincide con il modello
dell'ultimo turno, oppure quando non c'è nessuna voce: l'id dell'ultimo turno da solo non porta la finestra. Forma
invariata, `v` resta 1.

Contratto 1.12 (16/09/2026, solo aggiunte, richiesta dell'app approvata da Franz): comandi `model` (arg = un id di
`choices.models`) ed `effort` (arg = uno di `choices.efforts`), applicati SOLO a quella sessione dal suo selettore
(«s to use this session only»): il default delle sessioni nuove non si tocca mai. In radice di `/state`, `choices` =
`{"models": [{"id", "label"}], "efforts": ["low", "medium", "high", "xhigh", "max"]}`; l'id del modello è quello
completo che la sessione riporta in `model.id` (col suffisso `[1m]` dove c'è). Risultato: `ok` con una riga del tipo
«field-notes: model Sonnet 5, this session only», oppure `ok=false` con un motivo breve da mostrare così com'è
(sessione al lavoro, domanda aperta, testo scritto nel prompt, valore non disponibile, selettore o conferma non
arrivati). Dopo un ok lo `/state` successivo riporta già il valore nuovo in `model`/`effort` (il relay spinge subito);
con un modello cambiato `context` resta null fino al turno successivo della sessione, perché la finestra del turno
vecchio non è quella del modello nuovo. La scelta vale finché la sessione vive: un riavvio o una ripresa tornano al
default. `v` resta 1.

Contratto 1.13.1 (17/09/2026, solo semantica di `context`, segnalata dall'app alle 14:31: master al 100 % sul polso e al
59 % nel terminale): la finestra non si deduce più dalla sola assenza di `[1m]`. Fonti, in ordine: il suffisso `[1m]`; la
finestra dichiarata da Claude Code alla statusline, se salvata per sessione; più di 200k token → 1M. Per un modello il
cui id non dice la finestra (Fable 5.1) e senza nessuna di queste fonti, `context` è null. Un turno `<synthetic>` non
conta come ultimo turno. `v` resta 1.

Semantica dei tempi, dal relay (per non reinterpretarla ogni volta): `since` è la nascita della sessione per
busy/idle/awaiting, l'istante della domanda per waiting, l'ultimo avvistamento per gone, e non cambia a ogni cambio di
stato; `turn_started` è l'ultimo prompt o ripresa ed è valorizzato solo mentre lo stato è busy o awaiting, poi torna
null; il movimento di una sessione ferma lo dà `outcome.at`, che il relay aggiorna a ogni fine turno. Quindi
«ultimo movimento» = max(since, turn_started, outcome.at).

Contratto 1.13 (16/09/2026, solo aggiunte, richiesta dell'utente): il comando `launch` accetta un campo opzionale `text`, il primo messaggio della sessione; il relay lancia, trova la sessione nata e le consegna il testo come primo prompt con il prefisso del polso. Il `/result` di un launch porta `session`, il nome della sessione nata come in `sessions[].name` (può differire dal progetto: `field-notes-2`), anche quando il messaggio non è stato consegnato (ok=false). Ogni progetto porta `last_used`, epoch s della trascrizione più recente della cartella nel suo account, o null; l'ordine di `projects` resta per nome. `v` resta 1.

Contratto 1.14 (22/09/2026, solo aggiunte, richiesta di Franz tramite la master): ogni sessione porta `fallback`, {from, to, category, at} quando Claude Code l'ha spostata da solo su un modello più vecchio perché le salvaguardie hanno segnalato un messaggio (Opus 5.5, 2.1.280), altrimenti null. Letto dalla riga `model_refusal_fallback` della trascrizione; si spegne al primo turno sul modello di prima o dopo un'op `model` riuscita. Si torna con l'op `model` (id del modello di prima). `v` resta 1. Il polso lo accetta senza cambiare nulla (`ContractJson` ignora i campi sconosciuti); mostrarlo al polso è una funzione nuova, da decidere dopo la v1.


Contratto 1.15 (24/09/2026, solo aggiunte, richiesta dell'app approvata da Franz; design `docs/plans/2026-09-24-app-telefono-fondamenta-design.md`): accoppiamento dal telefono. `relay pair` mostra un QR con il JSON di `pair-qr.json` (id di 22 caratteri base64url, `pc_pub`, host, scadenza, configurazione Firebase con chiavi brevi) e, sotto, il codice a 6 cifre: stesso documento in `/pair/<id>` e in `/pair/<code>`, vince la prima risposta valida. Nel QR `d` è `relay.firebase_url` e `t` è `relay.fcm_topic`, il bus che il relay scrive davvero; `k`, `p` e `a` vengono da `relay.firebase_app` o dal `google-services.json` in `relay.google_services` (client scelto con `relay.app_package`). La risposta in `/pair/<…>/watch` può portare `uids` (fino a 4) e `names` (`pair-response.json`, con i vettori: PC = scalare 0..31, telefono = 32..63); `/allowed` riceve tutti gli uid. I controlli HMAC usano la stringa del nodo (`id` e `id + ":pc"`). Il telefono legge `/pair/<id>` prima di scrivere `/watch`: se il nodo non c'è (QR usato, scaduto o di un altro relay) mostra «codice non valido»; per questo `/pair/<id>` e `/pair/<id>/watch` devono avere le stesse regole RTDB di `/pair/<code>`. `v` resta 1. Relay: claude-master 0.4.21 (`2f02da1`).