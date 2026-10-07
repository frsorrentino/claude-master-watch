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

Contratto 1.16 (25/09/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 16:54): ogni sessione porta
`low_priority` — «off», «offered» (limite raggiunto: Claude Code propone «Continue now at lower priority» e aspetta),
«active» (viva ma lenta oltre il limite: «Working at lower priority · waiting for capacity»), null senza riquadro tmux o
per una gone; il relay lo legge dallo schermo tmux — e `goal`, la condizione di completamento data con `/goal`:
`{"text", "since", "met"}` dal transcript (ultimo attachment `goal_status`; `since` epoch s della sentinella che l'ha
impostato, può essere null; `met` true quando l'ultimo controllo la trova soddisfatta), null senza obiettivo, dopo
`/goal clear` o con un goal fallito. Il polso: bassa priorità a parole accanto all'età (lista, testata, card della
tile), «Obiettivo: …» intero nella Scheda e come riga della card della tile mentre lavora. `v` resta 1. Relay:
claude-master 0.4.24 (`5e681be`).


Contratto 1.17 (29/09/2026, solo aggiunte, richiesta R3 dell'app, design `docs/plans/2026-09-29-telefono-pezzo-3-design.md`): la coda della notte dall'app. `night_add` porta in `arg` il `path` di un progetto pubblicato e in `text` il prompt; il `/result` riuscito dice «queued for tonight: <name> (<account>), job <id>» e porta `job`, l'id del lavoro (8 caratteri esadecimali). I rifiuti, con `ok=false` e il testo in chiaro: «<path> is not a published project», «empty prompt: nothing to queue», «tonight's queue is full (8 jobs): remove one first». `night_remove` porta in `arg` l'id; riesce con «removed from tonight's queue: <id>», rifiuta con «no job <id> in tonight's queue» o «job <id> has already started: it cannot be removed». In `/state`, `night.items` elenca i lavori nell'ordine di esecuzione, `{id, dir, name, prompt, added, started}`: `prompt` su una riga, tagliato a fine parola entro 160 caratteri e senza «…»; `started` epoch o null. `items` c'è sempre, anche vuoto, per un relay che supporta la 1.17: la sua assenza dice all'app di chiedere l'aggiornamento di claude-master. La coda tiene al massimo 8 lavori (`night.max_queued`). `v` resta 1. Relay: claude-master 0.5.0 (`5793a05`), non ancora rilasciato.

Contratto 1.18 (29/09/2026, solo aggiunte, richiesta R4 dell'app): il diario delle 20:00 e il resoconto della notte arrivano anche all'app come eventi, con il push FCM al topic (`data.kind` = il kind). `kind: recap` nasce da `recap --send`: `title` «Diario del dd/mm» (nella lingua della config), `body` il diario come a Telegram ma senza HTML, `ref` il giorno ISO; se il diario viene rimandato nello stesso giorno arrivano due eventi con lo stesso `ref` e vale l'ultimo per `ts`. `kind: night_report` nasce da `night run --send`, solo se almeno un lavoro è girato: `title` «Notte: N lavori, M riusciti», `body` il resoconto, `ref` il giorno ISO della notte. `session` e `account` sono null per tutti e due. Gli avvisi di quota restano `kind: quota`: la soglia ha `title` con ⚠ e `body` «reset HH:MM» della finestra superata; la ripresa all'azzeramento, nuova, ha `title` con ✓ e nel `body` la riga della guardia dopo l'ora. `body` tagliato a fine riga entro 4000 caratteri, senza «…»; `title` su una riga. La timeline del polso salta `recap` e `night_report` e degli altri mostra la prima riga del corpo. `v` resta 1. Relay: claude-master 0.5.0 (`da8b9fb`), non ancora rilasciato.

Contratto 1.19 (29/09/2026, solo aggiunte, richiesta R5 dell'app): «Condividi» verso una sessione. Con un'immagine il dispositivo scrive prima `/share/<id>` (id `[A-Za-z0-9_-]` fino a 64 caratteri; busta `{v, enc}` come `/cmd`, in chiaro `{mime, data}` con `mime` `image/jpeg` o `image/png` e `data` in base64), poi il comando `report` con `session` = nome di una sessione viva, `arg` = id di `/share` o null per il solo testo, `text` = il messaggio (vuoto con l'immagine: la sessione riceve «(image from the phone)»). `state.share = {max_bytes}` (1500000) dice che il relay lo supporta; il limite vale sulla lunghezza della stringa `enc` e lo controllano anche le regole RTDB di `relay setup` (va rilanciato su un progetto già configurato). `/result` riuscito: «sent to <session>: image saved as <percorso relativo al progetto>» o «sent to <session>»; rifiuti: «no session <name>» (sparita o sconosciuta), «image missing or unreadable», «text too long: at most 4000 characters», «empty report: nothing to send». Il relay cancella `/share/<id>` dopo il comando, e quelli non letti dopo 10 minuti. `v` resta 1. Relay: claude-master 0.5.0 (`3cbcde2`), non ancora rilasciato.

Contratto 1.20 (30/09/2026, solo aggiunte, richiesta R6 dell'app): il telefono conta fra i dispositivi che ricevono, per il ripiego su Telegram. Ogni dispositivo accoppiato (orologio e telefono) scrive `/seen/<uid>` = `{".sv": "timestamp"}`, i millisecondi del server Firebase, senza busta cifrata, dopo il GET di `/state` alla sveglia FCM e all'apertura dello stream. Regole RTDB di `relay setup` (va rilanciato su un progetto già configurato): ognuno scrive solo il proprio nodo, se è in `/allowed`, e solo un numero; legge il PC. Il relay considera letto un evento se un dispositivo accoppiato ha `/seen` dopo la sua ora (60 s di tolleranza); se nessuno lo legge entro `relay.telegram_fallback_after_s` (600 s) va su Telegram una volta. Nessun segnale nello stato: con un relay o regole precedenti la scrittura viene ignorata o rifiutata senza danni, e non blocca mai la lettura. La scrittura sta in `FirebaseTransport`, comune a orologio e telefono: le due app la portano nella stessa versione, come chiede il relay. `v` resta 1. Relay: claude-master 0.5.0 (`db2b750`), non ancora rilasciato.

Contratto 1.21 (30/09/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 20:36): il tasto Stop. Op `interrupt`, `session` = nome del contratto, `arg` null. Il relay esegue `claude-master interrupt <tmux>`: un solo Esc, e solo se nelle ultime righe dello schermo c'è «esc to interrupt» (un turno in corso), quindi niente doppio Esc su un prompt fermo e niente Esc su un dialogo aperto. Risultato: `ok` «<name>: stopped»; `ok=false` «<name>: nothing to stop», «<name>: Esc sent, but the turn is still running» (dopo 5 s), «<name> is not running». Dopo un ok la sessione esce da awaiting e parte subito una push. Nuovo campo `ops` in `/state`: la lista delle op di `/cmd` che questo relay esegue; l'app mostra Stop solo se contiene `interrupt`, e varrà anche per le op future. Assente con un relay precedente. Fixture: `ops` nei tre stati; in `cmd-result-sample` due `interrupt`, «stopped» e «nothing to stop». `v` resta 1.

Contratto 1.22 (30/09/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 20:57): la conversazione della sessione per la chat del telefono. Op `transcript`, `arg` = "n" (ultime n voci, fino a 200, 50 se null), "n:before=<id>" (pagina più vecchia) o "n:after=<id>" (solo quello che è venuto dopo; legge la coda del file, costa poco). Il `text` del risultato è JSON `{"entries": [...], "more": bool}`, al massimo 60 KB: oltre si tolgono le voci più vecchie (o le più nuove per `after`) e `more` = true. Ogni voce ha sempre tutte le chiavi, null quando non servono: `id` (opaco, solo per before/after), `role` (user, assistant, tool), `text`, `at`, `tool`, `note`, `error`, `cut` (testo accorciato oltre 4000 caratteri, senza «…»), `turn` ({started, ended, in, out} sull'ultima voce di un turno chiuso; `in` comprende la cache), `files` ([{path, mime, size}] dei file prodotti: media, documenti, invii di file, immagini di `report`; non codice né .md), `origin` (per le voci user: pc, phone, watch o remote). Le voci user sono solo quello che la persona ha scritto, senza il prefisso del relay; i subagenti sono esclusi; i messaggi arrivati dal relay (anche come peer) diventano una sola voce. Rifiuti: argomento non valido, «no entry <id> in the transcript» (si riparte da "n"), «<name> is not running», «<name>: no transcript». Nuovo campo facoltativo `device` in `/cmd` ("phone" o "watch"): con `prompt`, `resume` e `launch` il relay usa il prefisso di quel dispositivo e la trascrizione ne ricava `origin`; senza, `origin` = "remote". `ops` comprende `transcript`. Fixture: in `cmd-result-sample` le pagine 19 e 20 e il prompt 21 con `device`. `v` resta 1.

Contratto 1.23 (30/09/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 20:57): il prompt suggerito. Ogni sessione in `/state` ha `suggestion`, stringa o null: il testo attenuato (SGR 2) che il terminale mostra dopo `❯` a campo vuoto, letto con i colori (capture-pane -e), spazi uniti, fino a 300 caratteri, senza «…». Valorizzato solo con `state` = "idle"; null quando dopo `❯` c'è testo digitato, quando la riga è un'opzione di dialogo, a turno in corso, e per il segnaposto `Try "…"` delle sessioni nuove. Si aggiorna alle push normali. Fixture: `suggestion` in ogni sessione dei tre stati; in `state-1` field-notes ha "commit the README changes and open a PR". `v` resta 1.


Contratto 1.27 (03/10/2026, solo aggiunte, richiesta dell'app approvata da Franz alle 16:57): la ricerca nelle conversazioni di tutte le sessioni. Op `search`, `session` null, `arg` = il testo, da 1 a 200 caratteri dopo aver unito gli spazi. Il relay cerca nelle trascrizioni dei due account, sessioni vive e chiuse degli ultimi 7 giorni, solo le voci di testo `user` e `assistant` come in `transcript` (niente tool, niente subagenti, user senza il prefisso del relay), ignorando maiuscole e accenti; un risultato per voce, sulla prima occorrenza. Il `text` del risultato è JSON `{"hits": [...], "more": bool}`, al massimo 50 risultati e 60 KB, dal più recente; oltre i limiti, o dopo 10 s, quello trovato con `more` = true. Ogni hit ha sempre tutte le chiavi: `session` (il nome del contratto se viva, se no quello della cartella), `live`, `project` (percorso), `entry` (l'id di `transcript`, anche «q<ms>»), `role`, `at`, `snippet` (fino a 160 caratteri intorno alla parola, a capo uniti, senza «…», può tagliare una parola ai bordi), `match` ([inizio, fine) nello snippet). Rifiuto: «bad query: from 1 to 200 characters». `ops` comprende `search`. La prima ricerca dopo l'avvio del relay può fermarsi a 10 s con `more` = true finché la cache non è calda. Fixture: in `cmd-result-sample` un search «tuesday» con 5 risultati fra una sessione viva e una chiusa; `search` in `ops` nei tre stati. `v` resta 1.

Contratto 1.28 (03/10/2026, solo aggiunte, richiesta dell'app approvata da Franz): file di qualunque formato da «Condividi» e dal «+» della chat. Il blob di `/share/<id>` porta in chiaro `{mime, data, name}`, con `name` facoltativo: il nome originale, che il relay ripulisce (solo l'ultimo pezzo del percorso; lettere, cifre e « ._-()+,@=»; spazi uniti; niente punto iniziale; vuoto o oltre 120 caratteri = rifiuto «bad name»; senza `name`, «file» più l'estensione del tipo). `image/jpeg` e `image/png` passano come prima (il nome si ignora); ogni altro `tipo/sottotipo` diventa un file in `<cartella della sessione>/.claude-master-inbox/<AAAAMMGG-HHMMSS>-<nome>` (permessi 600, cartella con un `.gitignore` «*»), e la sessione riceve, con il prefisso del dispositivo, «ti ho mandato il file <nome> (<mime>, <dimensione>): <percorso>» e poi il testo del report, se c'è. Risultato `ok` «sent <nome> to <sessione>». Il limite resta `share.max_bytes` (1 500 000 sulla busta cifrata); il rifiuto è «too large» per tutti i tipi. In `/state` `share.any` = true quando il relay accetta ogni formato. Fixture: in `cmd-result-sample` un report di un PDF (nome «../../Preventivo  cliente?.pdf», ripulito in «Preventivo cliente.pdf»); `share.any` nei tre stati. `v` resta 1.
Contratto 1.29 (03/10/2026, solo aggiunte, voce a) «cronologia degli agenti» approvata da Franz alle 18:32): cosa hanno fatto le sessioni, in ordine di tempo. `/cmd` op `timeline`, `session` = un nome come `sessions[].name` o null per tutte, `arg` = «90m», «6h», «2d» (al massimo 7 giorni) o un epoch in secondi da cui partire; null vale 6h. Rifiuto: «timeline <arg>: expected 90m, 6h, 2d (7 days at most) or an epoch». Il `text` del risultato è JSON `{since, sessions, more}`: le sessioni dalla più recente, ognuna `{session, live, project, events}`, con `project` relativo alla radice dei progetti come `state.sessions[].project` e `live` che distingue la stessa cartella viva e chiusa. Gli eventi sono in ordine di tempo, al massimo 40 per sessione, mai dopo l'istante della richiesta, sempre con tutte le chiavi `{at, kind, text, ok, ref}`: `prompt` (`ref` = phone o watch se arriva dal relay, null se scritto al PC), `test` (`text` = la suite, `ok` verde o rosso, rosso anche se il riepilogo dice FAIL, `ref` = la riga di riepilogo), `commit` (`ref` = hash corto), `outcome` (la riga «Esito:»), `task` (`ok` dal controllo rieseguito, `ref` = id del compito). `text` al massimo 160 caratteri su una riga. Oltre 60 KB il relay toglie gli eventi più vecchi della sessione più lunga e `more` = true. `ops` comprende `timeline`. Fixture: in `cmd-result-sample` un timeline di tutte le sessioni da un epoch, con ogni kind (una sessione chiusa con prompt, test rosso e verde, commit, esito e compito; una viva con cinque prompt, uno dal telefono); `timeline` in `ops` nei tre stati. `v` resta 1. Relay: claude-master `92be2ee`.

Contratto 1.30 (04/10/2026, solo aggiunte, chiesto dall'app per il tablet): `claude-master relay pair --add` accoppia un dispositivo IN PIÙ accanto a quelli che ci sono. Il QR è quello della 1.15 più `m: "add"`; i nodi `/pair/<code>` e `/pair/<id>` portano `mode: "add"`. Stretta di mano e `check` invariati, ma la chiave del relay non si deriva: la conferma `/pair/<nodo>/ok` = `{host, check, key}`, dove `key` è la busta `{v, enc}` di `/state` cifrata con la chiave del giro (HKDF di X25519, quella che `check` dimostra) e in chiaro contiene `{"key": "<64 cifre hex>"}`, la chiave da salvare. `/allowed` e `devices.json` sono l'unione con i dispositivi di prima, eventi e risultati restano; oltre quattro dispositivi il nodo diventa `{"error": "full"}` e il PC esce con 4 (l'app lo legge da `/pair/<id>/error` e lo dice subito). Senza una chiave salvata sul PC `--add` non parte. Il codice a 6 cifre dell'orologio non conosce `mode`: un'aggiunta si fa dal QR, con il telefono o il tablet. Fixture: `pair-add.json` (scalare del PC 0..31, del dispositivo 64..95, `relay_key` = byte 96..127), con `qr`, `watch`, `ok` e `relay_key`. `v` resta 1. Relay: claude-master 0.6.3 (`de90b09`).

Contratto 1.31 (04/10/2026, solo aggiunte, chiesto da Franz dal telefono alle 13:47: «pairing dal telefono senza PC»): `/cmd` op `pair_add`, `session` e `arg` null. Il relay apre da sé `relay pair --add --text` in un processo suo, valido 5 minuti, e risponde `ok` = true con `text` = JSON `{qr, code, exp}`: `qr` è il documento del QR della 1.30 (con `m: "add"`), null se mancano i dati dell'app Firebase; `code` il codice a 6 cifre; `exp` la scadenza in epoch s. Il telefono mostra il QR al dispositivo nuovo; da lì è tutta la 1.30. Rifiuti con `ok` = false: «a pairing is already open on the PC», «pair --add: no saved key…» (in italiano «pair --add: nessuna chiave salvata…»), «already 4 devices: no room for another», «pairing not started: <motivo>». È una lettura: niente push dopo. `ops` comprende `pair_add`. Fixture: in `cmd-result-sample` una coppia con id `6f1c2d3e-0200-4000-8000-000000000200` (il `qr` è quello di `pair-add.json`, il codice 482913); `pair_add` in `ops` nei tre stati. `v` resta 1. Relay: claude-master `0d2c3d1`.

Contratto 1.32 (04/10/2026, solo aggiunte, chiesto dall'app per Franz: «ok c» alle 15:48 e lo schema dei collegamenti B alle 17:11). Parte 1, l'invito a zero tocchi sul Chromebook: con `relay pair --add`, e con `pair_add` dal telefono, se `relay.adb` (di solito `/usr/bin/adb`) vede `relay.adb_serial` (`emulator-5554`), il relay esegue `am start -W -a android.intent.action.VIEW -d 'cmwatch://pair?q=<base64url della riga di --text, senza padding>' <relay.app_package>`, con timeout di 4 s per get-state e 8 s per am start, un `adb reconnect` e un secondo tentativo; se adb non risponde e l'uscita è un terminale, la riga va negli appunti con OSC 52; con `pair_add` niente appunti. Con `--add` la stampa dice cosa fare nell'app (il QR o la riga di `--text`) e i minuti; il codice a 6 cifre resta per un orologio. L'app chiede sempre conferma prima di accoppiare. Parte 2: `state.devices` = `[{uid, name, kind, seen}]`, uno per dispositivo di `devices.json` (cioè `/allowed`), nell'ordine di `paired_at`; `kind` = phone, watch, tablet o chromebook, dalla risposta di accoppiamento (`kind` per sé, `kinds` = uid → kind per gli uid che porta con sé), altrimenti dal nome, altrimenti null; `seen` = epoch s dell'ultimo `/seen/<uid>`, letto al massimo una volta al minuto, null se non è mai arrivato. Fixture: `pair-link.json` (nuova: `line` la riga compatta del qr di `pair-add.json`, `uri` il link), `pair-add.json` con `watch.kind` = tablet, i tre stati con `devices` (cinque: phone, watch dedotto dal nome, tablet, chromebook, uno mai visto con kind null). `v` resta 1. Relay: claude-master `655397f`.

Contratto 1.33 (04/10/2026, solo aggiunte, chiesto dall'app per Franz: idea alle 20:24, ok alle 20:32): le azioni ricorrenti della master. `state.recurring` = `[{id, label, prompt, param}]`, dall'ultima usata in cima, al massimo 8; il campo manca senza lista. `label` al massimo 40 caratteri; `param` = il prompt aspetta un pezzo da aggiungere (l'app lo mette nel campo col cursore in fondo, senza invio diretto). La lista la tiene la master sul PC (`claude-master recurring add|remove|list|used`); un prompt alla master che comincia con il prompt di una voce, anche col pezzo aggiunto, la porta in cima. Nessuna op nuova: l'invio è un prompt alla master. Fixture: i tre stati con `recurring` di tre voci demo, `release-changelog` con `param` true. `v` resta 1. Relay: claude-master `d4009ce`.

Contratto 1.34 (04/10/2026, solo aggiunte, chiesto dall'app per Franz: un HTML di 3,6 MB non arrivava, «too large: 3630371», 21:55): i file della chat a pezzi. Il comando `file` con `"parts": true` fa scrivere al relay `/file/<id>/parts/0..n-1`, ognuno `{v, enc}` dei byte grezzi (1048576 byte prima della cifratura, stessa AES-GCM e stesso AAD della busta di `/state`, senza JSON), e per ultimo `/file/<id>/meta` = `{v, enc}` con in chiaro `{n, size, sha256, mime, name}`: quando `meta` c'è, i pezzi ci sono tutti. Tetto 25 MB (26214400); TTL 10 minuti; l'app cancella `/file/<id>` intero dopo averlo letto. Il rifiuto diventa «too large: <byte> max <byte>»: senza `parts` il massimo resta 843650, con `parts` è 26214400. Nessun cambio alle regole del database. Fixture: `file-parts.json` (un file di 14 byte in due pezzi da 8, la chiave di prova come in `pair-add.json`, `meta_plain`, `meta` e i due pezzi). `v` resta 1. Relay: claude-master `9d66d17`.

Contratto 1.35 (05/10/2026, solo aggiunte, chiesto dall'app per Franz: sul Chromebook l'app Android passa per ARC ed è lenta): la web app locale. Con `relay.web.enabled` il daemon `relay serve` ascolta su `127.0.0.1:<relay.web.port>` (8765) e serve la web app compilata (`relay.web.dir`) su `/` e un'API con lo stesso JSON del contratto, in chiaro (niente {v, enc}): `GET /api/state`; `GET /api/events?since=<ts>` (ts > since, dal più recente); `GET /api/stream` (SSE, `event: state` con lo stato intero all'apertura e a ogni cambio); `POST /api/cmd` (un Cmd → il suo CmdResult, le stesse op; senza id lo mette il relay); `GET /api/file/<id>` (il file chiesto con `file` <id>, intero, niente pezzi né tetto, per 10 minuti); `POST /api/share/<id>` ({mime, data, name} per `report` arg=<id>). Ogni chiamata porta `Authorization: Bearer <token>`; le GET anche `?t=<token>`. Host 127.0.0.1 o localhost su quella porta, Origin diverso rifiutato, nessuna intestazione CORS. `claude-master relay web` stampa e apre `http://127.0.0.1:<porta>/?t=<token>`. Fixture: `local-api.json` (un esempio per endpoint, richiesta e risposta; `at` varia). `v` resta 1. Relay: claude-master `e45c26b`.

Contratto 1.36 (05/10/2026, solo aggiunte, chiesto dall'app per Franz alle 19:47: un prompt della web app arrivava come «Dall'utente via polso (watch)»): `device` accetta anche "web". Il prefisso diventa «Dall'utente via web app.», con la stessa riga «Watch:» del telefono. In `transcript` le voci user prendono `origin` "web"; in `timeline` il `ref` del prompt è "web". Senza `device` il prefisso è neutro («Dall'utente via app.», `origin` "remote"); il vecchio prefisso che nominava il polso resta riconosciuto come "remote". L'API locale (1.35) mette `device` "web" se manca. Fixture: un `prompt` con `device` "web" in fondo a `cmd-result-sample.json`, la voce `w1.0` con `origin` "web" nelle pagine di trascrizione e in `transcript-sample.jsonl`, il suo prompt con `ref` "web" nella timeline. `v` resta 1. Relay: claude-master `f065b58`.

Contratto 1.37 (05/10/2026, solo aggiunte, proposta della master accettata da Franz alle 19:58): la master al servizio dell'app.
- `sessions[].advice` = il consiglio di fable-director {model, effort, reason, switch_cost_tokens, at, source, when, differs}, letto dallo snapshot della sua statusline; solo se ha meno di 6 ore e se modello ed effort sono fra `choices`.
- `sessions[].finished` = ferma, con «Esito:» e senza «Prossimi:» nell'ultimo messaggio.
- `sessions[].duplicate_of` = il nome della sessione aperta prima sulla stessa cartella e la stessa conversazione.
- `advice`, `finished` e `duplicate_of` ci sono solo quando valorizzati (assenti = null / false / null).
- `state.approvals` = i compiti del registro in awaiting_ok, {task, title, what, where, deploy, requested_at}, dal più vecchio; `deploy` vero se cosa o dove nomina la produzione.
- Op nuove: `approve` (arg = compito, text) → `claude-master task approve`, solo per un compito che aspetta l'ok; `decision` (text, arg = progetto facoltativo) → `talk master`, che la scrive in memoria.
- Fixture: advice e approvals in state-1, finished in state-1 e state-2, approve (riuscito e rifiutato) e decision in fondo a cmd-result-sample.
- `v` resta 1. Relay: claude-master `de41a5f`.

Contratto 1.38 (05/10/2026, solo aggiunte, chiesto da Franz alle 21:10: «Colora i tasti che sbloccano»): un «!» davanti a una voce della riga «Prossimi:» la segna come voce che sblocca un lavoro fermo (un ok, una scelta). `sessions[].next_steps` = le voci dell'ultima riga «Prossimi:», [{text, blocking}], al massimo tre, col «!» tolto dal testo; c'è solo quando ci sono voci e un esito. Anche `outcome.full` porta la riga senza «!». Regola 10 del kernel: le voci che sbloccano vanno per prime, col «!» (che non conta nei 40 caratteri). Fixture: `next_steps` di atlas-shop in state-1. `v` resta 1. Relay: claude-master `f35fbd4`.

Contratto 1.39 (05/10/2026, solo aggiunte, chiesto dall'app per Franz: piano approvato alle 21:21, otto posti alle 21:25): i browser come dispositivi. `PAIR_MAX_DEVICES` passa da 4 a 8. `kind` "web" entra fra i tipi: la risposta di accoppiamento del browser lo conserva e `state.devices[].kind` vale "web". Op nuova `unpair` (arg = uid): toglie il dispositivo da `/allowed`, `/seen` e `devices.json` («removed <name>»); rifiuta un uid sconosciuto («unknown device <uid>») e l'ultimo dispositivo rimasto («cannot remove the last device»). `ops` comprende `unpair`. Fixture: il dispositivo «Safari on Mac» di tipo web nei tre stati; un unpair riuscito e uno rifiutato in fondo a `cmd-result-sample.json`. `v` resta 1. Relay: claude-master `f5a3840`.

Contratto 1.40 (06/10/2026, solo aggiunte, chiesto da Franz il 05/10 alle 23:49 dopo un riavvio di claude-master-phone rimasto spento): evento restart_failed, un riavvio di sessione non riuscito; session null, ref = nome della sessione, title «⚠ riavvio di <nome> non riuscito», body = motivo e cosa fare. Fixture: l'ultimo evento di events-sample.json. v resta 1. Relay: claude-master b93fd60.

Contratto 1.42 (07/10/2026, solo aggiunte, chiesto dall'app per la modalità live: specifica approvata da Franz il 06/10 alle 21:50): due campi facoltativi del comando. `prompt` con `voice: true` = un prompt dettato in cuffia; il relay aggiunge al prompt il testo d'istruzioni vocale «Dall'utente a voce, in modalità live: rispondi in 2 o 3 frasi, senza tabelle, percorsi né codice; se proponi seguiti, Prossimi numerati; chiudi con la riga Watch.», come aggiunge la richiesta della riga «Watch», per qualsiasi sessione. `approve` con `via: "live"` e `confirmations` = un ok dato al polso con la doppia conferma; il relay lo registra come gli altri, col testo «(modalità live, doppia conferma)» accanto al dispositivo; con `via` "live" e `confirmations` assente o minore di 2 rifiuta: «<task>: live approval needs a double confirmation». Senza `via` l'approve resta quello della 1.37. Campi assenti = non scritti. Fixture: in fondo a `cmd-result-sample.json`, un prompt a voce alla master (…0380), un approve live con 2 conferme (…0381) e uno rifiutato con una conferma sola (…0382). `v` resta 1.
