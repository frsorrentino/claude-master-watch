# Contratto PC ↔ orologio (v1, aggiunte 1.1, 1.2 e 1.3)

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

Regole che i test verificano: `state` ≤ 8 KB; `outcome.short` ≤ 60; `outcome.full` ≤ 600;
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

Semantica dei tempi, dal relay (per non reinterpretarla ogni volta): `since` è la nascita della sessione per
busy/idle/awaiting, l'istante della domanda per waiting, l'ultimo avvistamento per gone, e non cambia a ogni cambio di
stato; `turn_started` è l'ultimo prompt o ripresa ed è valorizzato solo mentre lo stato è busy o awaiting, poi torna
null; il movimento di una sessione ferma lo dà `outcome.at`, che il relay aggiorna a ogni fine turno. Quindi
«ultimo movimento» = max(since, turn_started, outcome.at).
