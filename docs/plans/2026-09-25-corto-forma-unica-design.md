# Corto: una forma sola, mai tagliata — design (25/09/2026)

Richiesta di Franz (25/09, 21:10): «la continuità che offre questa tecnica è quello che cercavo sin dall'inizio: un
elemento che attraversa tutte le scene trasformandosi. Applicarla al video per lasciarlo com'è non è utile». Fonte
della tecnica: direttive di motion di Raphael Aubry (25/09), in `personali/docs/metodologie/progetto-video-remotion-da-json.md`,
sezione «Direttive di motion». Impianto: proposta dell'agente Opus 5.5 a freddo («uno schermo, mai tagliato»), con
tre correzioni di questa sessione (clip reali in apertura, gesti veri, sostituzione graduale). Franz ha scelto:
regia e giudizio in sessione, componenti da specifica a esecutori Opus, dopo handoff in sessione nuova.

Vincoli fermi: testi e ordine delle scene della scaletta invariati; le registrazioni vere del display restano dove
sono vere; ogni trasformazione parte da un gesto che nell'app esiste (tocco, pressione lunga, scorrimento, invio);
niente loop (il finale ha nome, repo e avvisi obbligatori); una resa completa solo dopo la tavola per battito.

## 1. L'idea

La forma è il display tondo dell'orologio. Non sparisce mai: si stacca dal quadrante, diventa la notifica, la
domanda con la voce, il tasto «yes», lo sfondo, le card della corsia, la schermata di dettatura, il ✓, la finestra
del terminale, la card ✓ nella lista, i tre pannelli della panoramica, la linea sotto lo slogan, l'arco del logo, e
torna display sull'orologio di tre quarti che si allontana. La camera la insegue: ogni stato riempie il quadro con
lo stesso margine, e il cambio di scala È la transizione. Ogni proprietà della forma (centro, misura, raggio,
colore, contenuto, aggancio) è una somma di molle sui cambi di bersaglio: funzione pura del fotogramma, mai
ricreata da una scena all'altra.

## 2. Gli stati, in battiti (110 bpm, 73,5 battiti)

| Battiti | Scena | Stato della forma | Gesto | Oggi |
|---|---|---|---|---|
| 0-1,5 | face | il display, agganciato al quadrante (omografia) | — | uguale |
| 1,5-5,5 | asks | una copia si stacca dal display e diventa la notifica «Staging is green. Deploy 2.8.0?» accanto al titolo; il display vero resta con la sua clip | il tremito la scuote | titolo entra a parte, notifica solo nel video |
| 5,5-12 | speaks | la notifica si allunga in basso: ▶, onda della voce, le due opzioni dentro | tocco sul ▶ | onda accanto al display |
| 12-19 | answer | il bordo basso si divide nei due tasti; l'anello corre su «yes»; «yes» cresce fino allo sfondo (takeover di oggi, come stato) | pressione lunga | tasti che escono dal display |
| 19-37 | loop | lo sfondo si ritira nella prima card della corsia; ogni passo è un cambio di bersaglio della stessa forma; l'ultima card è la schermata di dettatura; il ✓ è il cerchio che si apre | scorrimento, tocco sul ✓ | già continuo, ma come pezzi separati |
| 37-47,5 | watch | la forma è la finestra del terminale; il display a destra mostra la clip vera (il PC scrive, il polso ripete: due schermi, non una copia) | — | uguale |
| 47,5-52 | list | la riga finale vola e si posa come card ✓: il bordo davanti su una molla più rigida di quello dietro | — | già continuo |
| 52-58 | work → questions → context | spariscono le tre palpebre: la card ✓ diventa il pannello Work, poi Open questions, poi Context, restando al suo posto; cambiano altezza, raggio, contenuto; le barre 62/18/4 % nascono dal bordo | scorrimento (come nell'app) | tre blink |
| 58-64 | slogan | la forma si assottiglia in una linea sotto «Claude Code, on your wrist.» e scorre sotto le parole che entrano | — | nero |
| 64-73,5 | end | la linea si piega nell'arco corallo del logo; il cerchio torna display dell'orologio di tre quarti che si allontana; nome, repo, avvisi | — | nero, poi logo |

Il guadagno maggiore sta in 47,5-73,5: oggi cinque tagli di fila (tre palpebre, due neri) nel terzo finale.

## 3. Testi

Oggi i titoli entrano parola per parola dal basso (`WordMask`, mezzo battito a parola, Bézier) e restano fuori
dalla forma. Regole nuove, dal post e dal nostro stile:

- **Il testo che sta dentro la forma** (notifica, domanda, opzioni, card, pannelli, prompt) ha entrata e uscita
  proprie e sfalsate: esce il vecchio (0,25 battiti, sfocatura di 6 px e scivolo di 8 px), poi entra il nuovo
  (0,25 battiti), mai sovrapposti («Text that swaps inside a morphing container needs its own enter and exit
  timing»). L'a capo non cambia mai durante una trasformazione: il blocco si ricompone solo fra un'uscita e
  un'entrata.
- **I titoli** («Claude has a question.», «Hear it out.», «Say what's next.», «Watch it work.», «Every session,
  at a glance.») restano fuori dalla forma, a sinistra, ma la salita parola per parola passa alla molla (ζ 0,8:
  arrivo con un accenno di peso) e ogni parola cade su un mezzo battito, come oggi. La parola in colore resta.
- **Lo slogan** nasce sopra la linea (la forma) che scorre sotto le parole: la linea si allunga di una parola per
  battito; è la stessa che poi diventa l'arco.
- **Niente effetti per lettera**, niente tracking che si apre, niente glow sui testi: sarebbero il «template» vietato.
  L'unica cosa che si muove nei testi è la posizione (molla) e l'opacità (molla critica).
- **Il terminale** resta com'è: le righe arrivano ai battiti, il polso ripete sei fotogrammi dopo.

## 4. Camera, gesti, suono, colore

- **Camera**: `camera(t)` = molle su centro e scala che inquadrano il `rect` della forma con margine fisso (12 % del
  quadro); limite di scala 1,3× sulle foto (oltre sgranano); si applica a scena e forma insieme; niente `will-change`
  sugli strati scalati. Il dolly per scena della scaletta sparisce dove la camera segue la forma.
- **Gesti**: il dito (cerchio grigio del «mostra tocchi») su ogni cambio: ▶, pressione lunga, ✓; lo scorrimento per
  corsia e panoramica. Nessuna corona: nell'app non si usa.
- **Suono**: ogni cambio di stato cade su un battito; gli effetti (`sfx.py`) sui picchi misurati, come oggi. Le
  palpebre davano un accento sui battiti forti: lo danno ora i cambi di forma, messi sul battito 1 delle battute.
- **Colore e raggio** cambiano davvero fra gli atti (know: notifica chiara; act: blu dell'atto; control: grigio
  del terminale; close: nero e corallo), se no «tutto è una pillola» e il film si appiattisce.
- **Motion blur** solo sullo strato della forma (4 campioni, 180°), non sulle scene: costa quasi niente e sta
  dove il movimento è rapido. Con spostamenti molto grandi in un fotogramma, 8 campioni sul solo tratto.

## 5. Motore

- Traccia `shape` in `timeline.short.json`: fotogrammi chiave in battiti `{at, rect | anchor: "display", r, color,
  content, ease?}`; fra due chiavi ogni proprietà è `springs()` (`spring.ts`) sui cambi; `content` ha il suo scambio
  con uscita/entrata (§3). Validazione in `timeline.ts` (chiavi in ordine, `rect` o `anchor`, colori esadecimali).
- Componente `Shape` in `Film.tsx`, sopra le scene: legge la traccia, calcola stato e contenuto, disegna; con
  `anchor: "display"` il `rect` viene dall'omografia del display in quel fotogramma (`homography.ts`), e il
  passaggio display → quadro è una molla sul peso dell'aggancio.
- `camera(t)` (§4) applicato al contenitore delle scene e alla forma.
- Le scene spengono la loro copia locale quando la forma la copre (come `underTakeover` oggi). Takeover, takeIn
  e blink restano finché la tavola non dice che la forma regge: poi diventano chiavi e il codice si toglie.
- Test (`node --test`): continuità del `rect` fra fotogrammi consecutivi (nessun salto sopra 4 px senza gesto),
  forma presente in ogni fotogramma, contenuto mai sovrapposto, chiavi sui battiti; identità con la geometria del
  display in `face` e in `end`.

## 6. Verifica e ordine

1. Motore: `Shape`, `camera`, traccia e test. Esecutore Opus da questa specifica; «fatto» = test verdi.
2. Tavola della sola forma su fondo piatto, un fotogramma per battito (74 immagini leggere): Franz giudica il
   percorso prima che si tocchino le scene.
3. Tratto finale list → end (47,5-73,5): tavola, poi resa del solo tratto.
4. Testa face → answer (0-19).
5. Centro loop e watch (19-47,5), ribasati sulla forma.
Ogni tratto: tavola per battito, resa parziale con il via della master (regola del 25/09), confronto prima/dopo a
Franz. Stima: tre o quattro sessioni. Rese: 30-60 minuti per tratto con la macchina libera.

## 7. Rischi

Aggancio sulla foto (un salto fra spazio del display e del quadro va nascosto sotto un gesto); zoom oltre 1,3×;
monotonia della pillola; accento sui battiti perso senza le palpebre; tempi di resa; le due proposte (sessione e
Opus) coincidono su guadagno e ordine, il che riduce il rischio di direzione ma non quello di esecuzione.
