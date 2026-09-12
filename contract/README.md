# Contratto PC ↔ orologio (v1)

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
`state` ∈ waiting|busy|idle|awaiting|gone; ordine delle sessioni: waiting, busy, idle, gone,
poi alfabetico.
