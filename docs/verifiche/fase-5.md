# Fase 5 — rifinitura visiva sul Pixel Watch

«Fatto» della fase (design, sezione 6): screenshot delle 8 schermate approvati da Franz.

| # | Voce | Come | Esito |
|---|---|---|---|
| 1 | Margini del tondo: nessun testo tagliato dal cerchio in alto e in basso | tutte le liste usano `roundListPadding()` (lati 7 %, alto 16 %, basso 21 %) | ✅ dal vivo 12/09, dopo due correzioni (`fase2-domanda-watch.png`) |
| 2 | Nessuna sovrapposizione fra righe | `transformedHeight` solo dove c'è `SurfaceTransformation` | ✅ dal vivo 12/09 |
| 3 | Contrasto sui pieni: nero su ambra, bianco su cobalto | luminanza calcolata in `WideButton`, WCAG in `Badge` | ✅ dal vivo (`fase2-domanda-watch.png`) |
| 4 | Nomi distinguibili: due sessioni con lo stesso prefisso non sembrano la stessa | marquee sulla riga centrale + taglio in mezzo da fermo (`NameText`) | ✅ cattura adb 14/09 18:14 (`fase5-sessioni-nomi-seguita.png`): «claude…-watch» accanto a «claude-master», la coda distintiva resta; confermato da Franz alle 20:17 |
| 5 | Testi lunghi scorrono dopo 2 s, tre giri, poi fermi | `cmMarquee`, solo la riga al centro | ✅ dal vivo 14/09 20:28, confermato da Franz («i nomi lunghi scorrono bene»); catture adb delle 20:18 |
| 6 | Ambient: badge a contorno, niente riempimenti, niente seconda riga, niente tasto Menu | `rememberAmbient` | da provare (coprire lo schermo) |
| 7 | Respiro di 3 s sul badge di ogni sessione che lavora, come il pallino dell'app Claude (Franz, 14/09 16:24; prima solo la seguita) | `Badge.breathes`, `SessionBadge` | ✅ due catture adb a 4 s (14/09 18:14): il badge di `master`, al lavoro e non seguita, passa da tenue a pieno; confermato da Franz alle 20:17 |
| 8 | Niente animazioni con le animazioni di sistema spente | `animationsOff()` | ✅ all'avvio (14/09 20:19, scale a 0 via adb e poi rimesse a 1.0): tre catture a 1 s identiche, badge e nome fermi. Difetto: se le si spegne ad app aperta lo scorrimento continua finché la schermata non si ricrea (`animationsOff()` legge una volta), e il badge resta fermo a metà respiro |
| 9 | Icona nel launcher e nella lista app | icona adattiva L1 | ✅ 12/09 (`fase5-icona-launcher-watch.png`) |
| 11 | Tile: orologio davanti alla barra della quota, quota dell'account della sessione mostrata sopra | `quotaAccount`, risorsa `ic_tile_clock` | ✅ dal vivo 14/09 15:51 (`59e6525`) |
| 12 | Tile: il testo della card sta in due righe senza «…», un pensiero intero | `TileTexts.fitTile` | ✅ dal vivo 14/09 17:18 (`544623e`) |
| 16 | Lista: il testo delle righe sta nelle sue righe senza «…», un pensiero intero | `SessionRow` con `TileTexts.fitTile` | ✅ cattura adb 14/09 18:23 (`0725313`) |
| 14 | Testata di Scheda, Domanda ed Esito: badge a 16 dp come nella lista, non tagliato dal tondo | `SessionHeader` | ✅ dal vivo 14/09 17:35 (`a83a0ad`) |
| 15 | Batteria: stream RTDB solo con l'app in primo piano; ad app chiusa niente riconnessioni continue | `Repo.live`, `ProcessLifecycleOwner` | ✅ misurato dal vivo 14/09 18:46-20:04 (`a83a0ad`), a batteria e app mai aperta, senza azzerare le statistiche: 0,66 mAh/h in background (0,56 da processo fermo), contro circa 3,0 mAh/h prima (40,4 mAh in 15 ore da processo fermo); CPU 15,5 s in 1,3 ore. Wi-Fi spento quasi sempre nella finestra, quindi il confronto è indicativo |
| 13 | Lista: la sessione seguita ha il bordo giallino e la campanella accanto al nome | `SessionRow`, `CmColors.followed` | ✅ dal vivo 14/09 16:58 (`472868f`), su `gardenclubnorth-eu` e `claude-master-watch` |
| 10 | Screenshot delle 8 schermate approvati | Sessioni, Scheda, Domanda, Esito, Terminale, Timeline, Quota, Impostazioni | ✅ 8 su 8: Timeline e Impostazioni approvate il 14/09 17:40 (`fase5-timeline.png`, `fase5-impostazioni.png`), Esito rifatto e approvato alle 18:13 (`fase5-esito.png`, `41adb72`) |
