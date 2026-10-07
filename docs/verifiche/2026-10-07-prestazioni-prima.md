# Prestazioni: misure di partenza (07/10/2026)

Piano: `docs/plans/2026-10-07-prestazioni-app.md`, Task 0. Fonte: `~/.team-supervisor/relay/relay.log`, letto con
`scripts/relay-measures.py LOG --day 2026-10-07 --since HH:MM --until HH:MM` (test: `scripts/test_relay_measures.py`).
Uso vero della giornata, nessun carico sintetico.

## Dal log del relay

| Finestra | Che cosa | Push/h | Intervallo fra push (mediana · p90 · max) | Letture di conversazione all'ora |
|---|---|---|---|---|
| 15:00-16:00 | uso normale, web aperta sulla plancia | 85 | 41,5 s · 67 s · 240 s | web 888 · telefono 47 |
| 16:20-16:40 | blocco: push in fila sul lock | 78 | 35 s · 86 s · 114 s | telefono 162 |
| 16:40-17:00 | ripresa dopo lo sblocco | 105 | 23,5 s · 64 s · 80 s | telefono 93 |

Altri comandi all'ora, 15:00-16:00: prompt dalla web 9, report dal telefono 4, file dalla web 3, prompt dal telefono 2.

Cosa dicono:
- La web fa da sola circa 19 volte le letture del telefono. È il carico più grande sulla fila dei comandi del relay
  (Task 5 e Task 8).
- Durante il blocco il telefono è passato da 47 a 162 letture all'ora. Ogni lettura senza risposta si richiedeva dopo
  21 s con un id nuovo, e allungava la fila (Task 5 e Task 9).

## Non misurato, e perché

- **Tempo di disegno della tile e età dello stato sui dispositivi:** servono le righe di log `tile:` e `state age:`, che
  entrano con i Task 3 e 6, e il debug wireless acceso per leggerle. Il «prima» si prende con la prima build che le
  contiene, su un'ora d'uso normale. Fino ad allora questa voce resta senza numero.
- **Attesa di un prompt dal telefono:** arriva con la riga di log della fase 0 del relay, che riporta `issued`,
  l'arrivo, l'attesa in fila e l'esecuzione. Oggi il log registra solo la fine del comando.
