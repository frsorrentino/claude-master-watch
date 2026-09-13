# Fase 5 — rifinitura visiva sul Pixel Watch

«Fatto» della fase (design, sezione 6): screenshot delle 8 schermate approvati da Franz.

| # | Voce | Come | Esito |
|---|---|---|---|
| 1 | Margini del tondo: nessun testo tagliato dal cerchio in alto e in basso | tutte le liste usano `roundListPadding()` (lati 7 %, alto 16 %, basso 21 %) | ✅ dal vivo 12/09, dopo due correzioni (`fase2-domanda-watch.png`) |
| 2 | Nessuna sovrapposizione fra righe | `transformedHeight` solo dove c'è `SurfaceTransformation` | ✅ dal vivo 12/09 |
| 3 | Contrasto sui pieni: nero su ambra, bianco su cobalto | luminanza calcolata in `WideButton`, WCAG in `Badge` | ✅ dal vivo (`fase2-domanda-watch.png`) |
| 4 | Nomi distinguibili: due sessioni con lo stesso prefisso non sembrano la stessa | marquee sulla riga centrale + taglio in mezzo da fermo (`NameText`) | da riguardare sul polso |
| 5 | Testi lunghi scorrono dopo 2 s, tre giri, poi fermi | `cmMarquee`, solo la riga al centro | da provare |
| 6 | Ambient: badge a contorno, niente riempimenti, niente seconda riga, niente tasto Menu | `rememberAmbient` | da provare (coprire lo schermo) |
| 7 | Respiro di 3 s sul badge della sessione seguita mentre lavora | `SessionBadge` | da provare |
| 8 | Niente animazioni con le animazioni di sistema spente | `animationsOff()` | da provare |
| 9 | Icona nel launcher e nella lista app | icona adattiva L1 | ✅ 12/09 (`fase5-icona-launcher-watch.png`) |
| 10 | Screenshot delle 8 schermate approvati | Sessioni, Scheda, Domanda, Esito, Terminale, Timeline, Quota, Impostazioni | 5 su 8 fatti, mancano Esito, Timeline e Impostazioni dal polso |
