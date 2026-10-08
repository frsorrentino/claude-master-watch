# Prestazioni: misure dell'orologio dopo il piano (08/10/2026)

Piano `docs/plans/2026-10-07-prestazioni-app.md`, Task 14 (parte orologio). Fonte: `adb logcat -s cmwatch:I` sul Pixel
Watch 5 con le build `4dd0075`/`3838adb`, finestre 11:36-12:00 e 16:32-16:58, uso normale. Il debug wireless si spegne al
polso, quindi le finestre sono brevi.

| Misura | Campioni | Mediana | p90 | Massimo | Obiettivo |
|---|---|---|---|---|---|
| `tile:` (ms per disegnare la tile) | 14 | 153 ms | — | 465 ms | meno di 50 ms: **non raggiunto** |
| `state age` (s fra raccolta e arrivo) | 54 | 16 s | 70 s | 3659 s | — |

- Il massimo di 3659 s è il guasto del relay delle 10:47-11:49 (push ferme, codice nuovo col demone vecchio): rimedio
  strutturale nella 0.7.5 (`relay ensure` riavvia il demone e fa la push di guardia), evento `relay_stale` (1.45).
- La tile resta sopra l'obiettivo: da guardare con un profilo del Task 3 (prima risposta dalla cache, poi aggiornamento).
- Da misurare ancora: la web (letture all'ora) quando torna aperta sulla plancia.
