# Contratto PC ↔ orologio (v1, aggiunte dalla 1.1 alla 1.7)

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

Semantica dei tempi, dal relay (per non reinterpretarla ogni volta): `since` è la nascita della sessione per
busy/idle/awaiting, l'istante della domanda per waiting, l'ultimo avvistamento per gone, e non cambia a ogni cambio di
stato; `turn_started` è l'ultimo prompt o ripresa ed è valorizzato solo mentre lo stato è busy o awaiting, poi torna
null; il movimento di una sessione ferma lo dà `outcome.at`, che il relay aggiorna a ogni fine turno. Quindi
«ultimo movimento» = max(since, turn_started, outcome.at).
