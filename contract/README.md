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
