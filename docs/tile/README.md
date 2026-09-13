# Cinque proposte di tile (13/09/2026)

Disegni a 384 px, palette M3 sobria: sfondo nero, superfici #1E2228, testo #F2F4F7, secondario #9AA3B2,
un solo accento cobalto sull'EdgeButton, i colori semantici solo dove dicono qualcosa (ambra dell'attesa,
badge della sessione). Sopra la tile Wear OS disegna da sé icona e nome dell'app: il titolo non li ripete.

| # | Nome | Cosa mostra | Quando è utile | EdgeButton |
|---|---|---|---|---|
| 1 | `tile-1-domanda` | badge e nome della sessione ferma, la domanda in grande su tre righe, da quanto aspetta | c'è una domanda aperta: è l'unica cosa che conta | Rispondi |
| 2 | `tile-2-colpo-occhio` | un numero grande («3 attive») e la fila dei badge di tutte le sessioni, ognuno col suo colore e glifo | sapere in un colpo d'occhio quante sono e come stanno, senza leggere nomi | Sessioni |
| 3 | `tile-3-ultima` | card con badge, nome, strumento in esecuzione e durata del turno | seguire chi sta lavorando adesso | Apri |
| 4 | `tile-4-quota` | anello della quota 5 h con la percentuale dentro, accanto sessioni attive, domande aperte e ora del reset | decidere se lanciare un altro lavoro | Quota |
| 5 | `tile-5-prossimo` | il «→ prossimo» dell'ultima sessione e, sotto, l'esito breve | riprendere il filo di cosa resta da fare | Scrivi |

La tile in produzione può cambiare forma da sola: 1 quando c'è una domanda, 3 o 5 quando non ce ne sono,
e lo stato «PC fermo» quando il battito manca da più di tre minuti. Le proposte 2 e 4 sono alternative
per lo stato senza domande, da scegliere.
