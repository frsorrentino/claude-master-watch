# Film promozionale, piano 3: l'interfaccia ricostruita (momenti «alla Google»)

Approvato da Franz il 18/09/2026 00:53 («approvo tutto») sulle sei proposte dell'handoff del 18/09, nate dalla lettura
fotogramma per fotogramma di tre film di riferimento (Google «Ask Search Anything», Google «Canvas in Gemini», LangEase).
Non sostituisce i piani 1 e 2: motore, musica, scaletta e riprese restano. Qui si aggiunge uno strato.

## 1. Principio

I film di riferimento non mostrano registrazioni dello schermo: **ricostruiscono i componenti dell'interfaccia** e li fanno
recitare (isolati, enormi, montati e smontati, inclinati nello spazio). Noi teniamo le due cose: **dentro l'orologio le
registrazioni vere** («gira davvero qui»), **fuori dall'orologio i componenti ricostruiti** in Remotion, fedeli all'app
(colori di `CmColors`, forme di `WideButton`, `SessionRow`, `Gauge`, monospazio del terminale). Niente che l'app non faccia
davvero: il componente ricostruito mostra lo stesso contenuto che in quel momento è sul display.

## 2. Componenti (`tools/promo/remotion/src/film/ui/`)

| Componente | Cosa ricostruisce | Parametri di recitazione |
|---|---|---|
| `UiTokens.ts` | `CmColors`: `surfaceHigh #292F3A`, `text #F2F4F7`, `text2 #B0B8C4`, `primary #D3E3FD`, `onPrimary #0A2050`, `waiting #FFB020`, `busy #7FA1FF`, `idle #34C759`, corallo del segno `#D97757` | — |
| `UiCard.tsx` | card di sessione: badge di stato, nome monospazio, età, testo | `build` 0-1 (contorno → pieno → contenuto), `state` ❓/▶/✓ con giro dell'icona |
| `UiOption.tsx` | tasto della domanda: primario pieno o scuro | `build`, `press` 0-1 (anello della pressione lunga attorno al tasto), `sent` |
| `UiGauge.tsx` | anello della quota con valore | `draw` 0-1, `value` |
| `UiTerminal.tsx` | righe monospazio | `lines`, `reveal` per riga |
| `Plane3D.tsx` | piano inclinato nello spazio con deriva lenta e sfocatura di profondità | `rx`, `ry`, `z`, `blurFar` |
| `Whip.tsx` | frustata corallo tra gli atti (striscia sfocata che attraversa il quadro in 6 fotogrammi) | `at` |

Ogni parametro è una funzione pura del fotogramma (testabile come `moves.ts`); i componenti non leggono il tempo da soli.

## 3. I sei momenti, agganciati alla scaletta esistente

| Scena | Oggi | Con lo strato nuovo |
|---|---|---|
| list · «Every session. One glance.» | lista che scorre nel display | a metà scena la card di payments-api **esce dal display**, cresce inclinata (`Plane3D`) accanto all'orologio, l'icona di stato gira ❓→▶; rientra prima del taglio |
| speaks · «It speaks.» | schermo fermo, parole a sinistra | dal ▶ parte una **forma d'onda** corallo guidata dall'ampiezza vera della voce; le parole si compongono da sillabe |
| answer · «You answer.» | arco attorno alla cassa | i due tasti **nascono come contorno** fuori dall'orologio, «1 yes» si riempie, l'anello della pressione lunga corre attorno **al tasto**, poi «Sent» |
| watch · «Watch it work.» | righe a sinistra | le righe escono dall'orologio su un **piano inclinato** con profondità di campo |
| limits · «Know your limits.» | contatore 11 % | **primo piano del gauge** che si disegna, il numero conta dentro la frase («11 % of your 5 hours») |
| cambi d'atto | scivolata laterale | **frustata corallo** sul battito; filo di luce corallo sul bordo della cassa all'ingresso dell'orologio |

Apertura e chiusura restano come sono (quadrante → gauge; gauge del logo → orologio): sono già la cornice.

## 4. Scaletta

Nuovi effetti in `timeline.ts`, con validazione e test come gli altri: `cardOut` (at, len, state), `optionsBuild` (at, len, hold),
`wave` (at, len, voice), `terminalPlane` (al posto di `terminal`), `gaugeHero` (al posto di `counter`), `whip` (at). Regole:
un solo momento forte per scena; il momento forte non copre mai il titolo; dura almeno 3 battiti e finisce un battito prima
del taglio.

## 5. Ordine di lavoro e verifiche

1. `UiTokens` + `UiCard` + `Plane3D` + effetto `cardOut` nella scena `list` → tre fotogrammi + 4 s di movimento a Franz
   (**punto di controllo A**: è lo stile giusto?). Non si costruisce il resto prima della sua risposta.
2. `UiOption` + `optionsBuild` (answer) · `UiGauge` + `gaugeHero` (limits) · `UiTerminal` + `terminalPlane` (watch).
3. `wave` (ampiezza dalla traccia della voce: inviluppo RMS a 30 Hz calcolato con `measure.py`, salvato in JSON) e sillabe.
4. `Whip` e filo di luce; anteprima completa con `deliver.sh` (**punto di controllo B**).
- Test: funzioni pure dei parametri (estremi, monotonia, atterraggio morbido); validazione dei nuovi effetti; `npm run check`.
- A vista: tavola di fotogrammi per scena; confronto affiancato componente ricostruito / fotogramma vero dell'app (stessi
  colori, stessi raggi, stesso carattere): se non è fedele non entra.
- Limite dichiarato: niente 3D vero né sfocatura di movimento reale; profondità simulata con `filter: blur` per piano.

## 6. Primo abbozzo e critica (18/09, 01:15)

Costruiti `UiTokens`, `UiCard`, `Plane3D`, `heroes.ts` (`cardOutAt`, con test) e l'effetto `cardOut` nella scena `list`
(`out/puntoA/`). Franz, 01:12: «dobbiamo evitare di apparire posticci, serve cura per i dettagli e rivederlo più volte per
perfezionarlo, modificandolo con idee creative». L'abbozzo È posticcio, per quattro motivi precisi:

1. **Non è lo stesso oggetto.** Sul display ci sono blog e payments-api, fuori esce «storefront». Nei riferimenti il
   componente che esce è quello che stava nell'interfaccia. Regola nuova: il momento forte prende **rettangolo di partenza e
   contenuto** dalla card che in quell'istante è ferma sul display (coordinate 480 nella scaletta), parte esattamente da lì
   alla stessa grandezza, e sul display al suo posto resta il vuoto (una toppa del colore della superficie che si dissolve al
   rientro). Serve la ripresa vera del piano 2, dove lo scorrimento guidato ferma la lista in un punto noto.
2. **Si posa sotto il titolo come un adesivo.** Deve avere una sua inquadratura: il titolo esce, la card prende il centro
   sinistro a grandezza da protagonista, poi il titolo della scena dopo entra. Un solo protagonista per volta.
3. **L'inclinazione sembra una stortura.** Serve uno spazio credibile: ombra a terra coerente con l'orologio, luce dallo stesso
   lato del vetro (alto a sinistra), rotazione attorno a un asse solo durante il volo e piano quasi frontale all'arrivo.
4. **Il bordo luminoso verde è un effetto facile.** Via: la profondità la danno ombra e sfocatura, il colore resta quello
   dell'app (badge di stato), come nei riferimenti.

Metodo per tutti i momenti forti: abbozzo → tavola di fotogrammi → critica scritta contro i riferimenti → rifacimento,
almeno tre giri prima di mostrarlo; confronto affiancato con il fotogramma vero dell'app a ogni giro.
