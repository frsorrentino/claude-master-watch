# Film promozionale, piano 4: un gesto solo

Nasce dalla critica di Franz alla v5 (18/09, 06:59): «una fila di scene con l'orologio come unica costante; la card che esce e
rientra nella stessa inquadratura è forzata; It asks / It speaks non hanno filo; la sequenza dopo è piatta; il cerchio due
volte; il finale è una somma di movimenti; poco dei riferimenti; niente che passi da una scena all'altra». Riprese nuove del
18/09 mattina (13 clip in inglese, `tools/promo/out/clips2/`, testi demo mai vuoti, dettatura vera di Franz).
Motore, musica (110 bpm), momenti forti già costruiti (piano 3) e regole (stesso oggetto, un accento, niente bordi luminosi)
restano. Qui cambiano scaletta, transizioni e i tre fili trasversali.

## 1. Tre fili che attraversano il film

1. **Il segno corallo** (il gauge aperto dell'icona): è il punto finale di ogni titolo (al posto del «.»), diventa l'anello
   della pressione lunga in «You answer», l'arco che si disegna in «Know your limits», la frustata tra gli atti, e alla fine il
   logo. Un solo accento nel film, sempre lo stesso.
2. **La camera** non entra e non esce a ogni scena: dentro un atto è un movimento solo (avvicinamento lento, laterale lento);
   le scene si passano il posto con un gesto (battito di ciglia, frustata, dissolvenza), mai con l'orologio che scivola dentro.
3. **L'interfaccia ricostruita recita**: ogni momento forte parte da un componente vero sul display, lo ingrandisce a
   protagonista e lo riporta a posto **in un'inquadratura diversa** (mai lo stesso quadro di partenza).

## 2. Scaletta (battiti a 110 bpm; le clip sono quelle del 18/09)

| # | Scena · titolo | Battiti | Sul display (clip, da → a) | Fuori dal display | Passaggio alla scena dopo |
|---|---|---|---|---|---|
| 1 | Claude is working. | 4 | — | titolo | dissolvenza |
| 2 | You're not at your desk. | 6 | — | titolo | dissolvenza |
| 3 | That's fine. | 6 | `tile` 0-4 s: quadrante acceso, poi lo swipe verso la tile | orologio già in quadro, dal nero, senza scivolate | la tile entra scorrendo: è la scena dopo |
| 4 | Every session. One glance. | 8 | `tile` 4-10 s: la tile con la card e la barra delle 5 ore | **la parola «glance.» cresce fino a riempire il quadro** (riferimento LangEase «Audio»), poi **battito di ciglia** (nero da sopra e sotto) | alla riapertura l'orologio è a tutto schermo sulla lista |
| 5 | (lista, senza titolo) | 6 | `list` 5-11 s: la lista scorre lenta | la card di payments-api **esce** a protagonista mentre la lista scorre sotto, e **rientra sulla lista ferma** (inquadratura cambiata: orologio grande) | frustata |
| 6 | It asks. | 6 | `asks` 9-13 s: la domanda si apre da sola | anelli di vibrazione dalla cassa (unica volta) | stacco sul battito |
| 7 | It speaks. | 16 | `speaks` 13-22 s: ▶ diventa ■, il testo scorre fino ai tasti, ■ torna ▶ | **musica: stop sul tocco di ▶** (una battuta di vuoto), la voce sul breakdown; **onda giro 2** che nasce dal ▶ e corre sotto le parole dette | la band rientra sulla frustata |
| 8 | You answer. | 8 | `answer` 8-16 s: pressione lunga su «1 · yes», «Sent», lista con «Deploying 2.8.0 to production» | **tasti che nascono da contorno** fuori dal display, «1 · yes» si riempie, l'anello corallo corre attorno al tasto (metro Ask 13,5 s) | dissolvenza |
| 9 | Follow what matters. | 6 | `follow` 9-16 s: pressione lunga sulla card, campanella accesa | la campanella esce dalla card e si accende grande (componente ricostruito, stesso oggetto) | dissolvenza |
| 10 | Know when it's done. | 6 | `done` 10-17 s: la card diventa ✓ «Deployed 2.8.0, smoke tests green», poi la schermata dell'esito | il badge ✓ della card esce e diventa il punto del titolo (il segno) | stacco |
| 11 | Say what's next. | 10 | `say_franz2` 19-31 s a 1,3×: Write, microfono, le parole compaiono, invio | le parole dettate si compongono grandi accanto (dalla clip, stesso testo) | frustata |
| 12 | Watch it work. | 8 | `watch` 7-15 s: il terminale cresce sul TICK | **terminale giro 5**: il pannello intero esce come un oggetto, in fuga, le righe arrivano dentro (metro Canvas 36,0 s) | dissolvenza |
| 13 | Know your limits. | 14 | `overview` 2-30 s a 1,4×: Quota → ritmo a 5 ore → Work → domande aperte | gauge protagonista (piano 3) all'inizio, poi la Panoramica scorre continua come un piano unico | frustata |
| 14 | Start something new. | 10 | `new` 7-17 s: progetto, «Write the first message», la sessione nasce | la card nuova di storefront esce a protagonista con il testo dettato | tuffo nello schermo (diveIn) |
| 15 | chiusura | 16 | tre quarti con l'icona | logo dal segno, cartello | — |

Totale 130 battiti ≈ 71 s (la traccia originale ne ha 180: il taglio si rifà su questa griglia). Il cerchio compare **una** volta
(scena 8). Nessuna scena entra con `slideIn`/`riseIn`: l'orologio si muove solo per avvicinarsi (momenti forti) o per tuffarsi
(finale).

## 3. Musica: stop and go costruito

La traccia originale (45 battute) non ha silenzi ma ha un **breakdown alle battute 19-27** (−8/−11 dB). Taglio nuovo con
`cut_track.py`: intro 1-7 sotto le scene 1-4; **stop secco sul battito in cui si tocca ▶** (scena 7): una battuta di vuoto con
la coda naturale; il breakdown sotto la voce (abbassato di 12 dB come oggi); la band rientra piena sulla frustata di «You
answer» (scena 8) con le battute 28-45. Le giunte a pari potenza di 15 ms sulla griglia, come nel piano 1.

## 4. Ordine di lavoro

1. **Montaggio grezzo v6a** con le clip nuove nella scaletta qui sopra, senza momenti nuovi (solo card e gauge già fatti):
   a Franz, per vedere il vero al polso. Poi, uno per volta e con il metodo del piano 3 (tavola, metro, ≥2 giri):
2. battito di ciglia + «glance.» che cresce (scene 4-5) · 3. tasti da contorno (8) · 4. onda giro 2 e stop and go (7) ·
5. terminale giro 5 (12) · 6. campanella, badge ✓ e card nuova (9, 10, 14) · 7. segno corallo come punto dei titoli ·
8. taglio della musica · 9. anteprima v6 intera con `deliver.sh`, tavola a 1 fps, misure audio.

Regole di macchina: `remotion render` con `nice -n 15 --concurrency 1`, uno per volta; disco al 99 %: tenere `out/` pulita.

## 5. Difetti dell'app visti durante le riprese (non per il film)
- Dopo l'invio di un messaggio dettato l'app torna alla scheda senza conferma visiva (solo vibrazione): una riga «Sent».
- Il popup di sistema della domanda non è comparso sul quadrante (17/09 sì): da capire; l'icona grande «?» nella notifica non
  piace a Franz (sostituire con il badge della sessione o togliere).
- All'apertura a freddo compare per un attimo la scritta «Already answered elsewhere» (stato vecchio).
- Franz, 18/09 07:35: le sigle degli account al posto di quadrato/cerchio: **USR** e **PRO**.
