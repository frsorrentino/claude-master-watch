# Film promozionale, piano 5: la UI diventa la scena

Dalla critica di Franz alla v6c (18/09, 16:05). Diagnosi in una riga: i passaggi della v6c sono **piccoli e brevi** (badge da
40 px che attraversano il taglio in 0,7 s); Franz vuole **l'elemento della UI che diventa la scena** (il tasto «yes» che si fa
sfondo è il modello: «un elemento si trasforma davvero in un elemento di tipo completamente diverso, catturando la scena,
ripulendo la precedente per far spazio alla nuova»). Regole nuove, che sostituiscono la catena del piano 4 §2 bis:

1. **Ogni passaggio è un componente che prende tutto il quadro** (≥ 1,5 s = 3 battiti) e si ridistribuisce nella scena
   dopo (sfondo, cornice, elemento di quella scena). Mai oggetti sotto i 200 px, mai sotto i 2 battiti.
2. **Niente esce e rientra.** Se un componente lascia il display, o resta protagonista fino al passaggio, o la scena cambia
   inquadratura (orologio a tutto schermo, o sfumato) e il componente si posa lì dentro con la nuova inquadratura.
3. **Un ingrandimento «sul posto» è sul posto**: il tasto premuto cresce del 6 % dentro il display e torna, senza uscire.
4. **Si tagliano le scene deboli** per dare tempo ai passaggi: l'estetica prima della completezza.

## Stato al 18/09, 23:45 (quanto di questo piano è nel film)

La scaletta ha preso una strada in parte diversa, decisa con Franz mentre si costruiva: va letta lì, non qui.
- **Fatti**: il takeover della card (lista → It asks) e quello delle parole dette (corsia laterale → Watch it work);
  il battito di ciglia con la parola che cresce; la vibrazione; la tile in «One glance»; il terminale del PC come
  sfondo dietro l'orologio; la tapparella in 3D che chiude la Panoramica (revisione 3D, momento 1).
- **Sostituiti**: le scene 9, 10 e 11 («Follow what matters», «Know when it's done», «Say what's next») sono state
  tolte e al loro posto c'è la **scena laterale** con l'orologio a terra e le card che scorrono sopra — Franz,
  18/09 pomeriggio: erano tre scene deboli e ripetitive.
- **Non fatti**: i passaggi 6, 10, 12 e 13 della tabella (badge ❓, ✓ che diventa microfono, cursore che diventa gauge,
  gauge che si apre in due campi). Il 13 è superato dalla tapparella.
- **Aperti con Franz**: domanda corta per «It speaks», voce AI per la dettatura, chiusura (c'è una prova in 3D).

### La durata: 128 battiti dichiarati, 155 nel film (84,5 s)

Ricostruzione, battito per battito, di dove sono andati i 45 battiti in più rispetto alle scene elencate qui (110):
- **limits +25** (10 → 35): Franz ha chiesto la Panoramica **dal video vero e per intero**, con i cinque dati che
  compaiono fermi a sinistra uno dopo l'altro; cinque pannelli non stanno in 10 battiti. Più 8,5 di tapparella a cavallo.
- **loop +8** (6 → 14): la scena laterale con l'orologio a terra sostituisce tre scene del piano (9, 10, 11) che ne
  valevano 24: **il film ci ha guadagnato 10 battiti**, non li ha persi.
- **accounts +6**: scena nuova, «Every account. One view.», chiesta da Franz perché altrimenti le schede duplicate
  degli account non si capiscono.
- **speaks +2, answer +2, list +2**: i tagli previsti qui (18 → 16, 14 → 12) non sono stati fatti perché la domanda
  corta e la voce nuova non ci sono ancora; restano da fare.

Quindi la crescita è la somma di richieste esplicite, non una deriva — con un'eccezione che resta aperta: **la durata
totale non è mai stata ratificata**. Se 84,5 s sono troppi, i posti dove tagliare sono «It speaks» (18 battiti su uno
schermo quasi fermo) e la coda della Panoramica.

## 1. Scaletta (110 bpm; battiti; clip del 18/09 a 1×)

| # | Scena · titolo | Battiti | Sul display | Il passaggio alla scena dopo (≥ 3 battiti, dentro i battiti della scena) |
|---|---|---|---|---|
| 1 | Claude is working. | 4 | — | dissolvenza |
| 2 | You're not at your desk. | 6 | — | dissolvenza |
| 3 | That's fine. | 6 | quadrante, **zoom nella complication con i tempi della v5** (offset 0,025, uscita a 2 battiti) | dal quadrante della complication si riapre: **la tile** (clip da rigirare: quadrante → swipe → tile) |
| 4 | Every session. One glance. | 8 | tile: card di storefront, barra delle 5 ore | **«glance.» e la card crescono insieme** dal loro posto (parola → titolo in alto, card → centro grande), il resto sfuma, occhio si chiude a card piena; riapertura: solo la card grande, l'orologio si materializza attorno con la lista |
| 5 | (lista, «One glance.» in alto) | 8 | la lista scorre sotto la card, si ferma | la card **cresce fino a coprire il quadro** e il suo colore diventa lo sfondo scuro di «It asks» (3 battiti) |
| 6 | It asks. | 6 | la domanda si apre da sola; **vibrazione**: l'orologio trema e la card della domanda si scuote | il badge ❓ **cresce a tutto quadro** (blu), poi si restringe nel cerchio del ▶ della scena dopo |
| 7 | It speaks. | 16 | ▶ → ■, lettura, scorrimento ai tasti; **domanda corta** («Deploy version 2.8.0 to production now? 1 - yes. 2 - no.») | l'onda sotto le parole **cresce fino a riempire il quadro** (nastro corallo pieno) e si appiattisce nelle **due pillole** dei tasti al centro |
| 8 | You answer. | 8 | (orologio sfumato) tasti al centro del quadro; pressione: **il tasto si gonfia sul posto**; anello; «Sent» | il tasto «yes» **diventa lo sfondo** (fatto: il modello) e da quel celeste emerge la scena dopo |
| 9 | Follow what matters. | 6 | pressione sulla card, campanella; **vibrazione** | la campanella **cresce a tutto quadro** (gialla) e diventa il ✓ (verde) di «done» |
| 10 | Know when it's done. | 6 | la card diventa ✓, schermata dell'esito | il ✓ cresce, ruota e diventa il **microfono** della scena dopo |
| 11 | Say what's next. | 12 | Write, microfono, dettatura (**voce AI realistica** al posto del vero: testo corretto) | le parole dettate **si dispongono come righe** e diventano le righe del terminale |
| 12 | Watch it work. | 8 | **titolo prima**, poi appare **la finestra del PC** dietro (più realistica, testo più piccolo) | il gauge della scena dopo nasce **dal cursore** del terminale: il cursore si allarga in **due linee parallele** che si curvano nei due anelli |
| 13 | Know your limits. | 10 | gauge protagonista, contatore | il gauge **si apre in due linee orizzontali** che si allontanano e diventano **due campi di colore** (sopra/sotto): lo sfondo della scena dopo |
| 14 | Start the next one. | 10 | progetto, «Write the first message», sessione nuova | tuffo nello schermo |
| 15 | chiusura | 14 | logo | vedi §3 |

Tagliata: nessuna scena intera; ridotte «speaks» (18 → 16, domanda corta) e «say» (14 → 12). Totale 128 battiti ≈ 70 s.

## 2. Come si costruisce (una grammatica sola)

`Takeover` (nuovo): un componente ricostruito (card, badge, tasto, campanella, ✓, onda, gauge) con quattro fasi:
**grow** (dal suo posto cresce fino a coprire il quadro, 1,5 battiti, `soft`) → **hold** (0,5) → **become** (si trasforma
nella cosa della scena dopo: sfondo pieno, due campi, cornice, pillole; 1 battito) → **settle** (la scena dopo si compone
sopra, 0,5). Il colore dominante del componente diventa il colore dello sfondo della scena dopo (gli atti cambiano colore
proprio qui). Ogni takeover è verificato a 10 fotogrammi al secondo su ±2 s attorno al taglio prima del render intero.

Le due linee (13→14): l'arco del gauge si «srotola» in una linea (arco → linea per punti, già fatto), la linea si sdoppia
nei due colori (5 ore azzurro, settimana lavanda), le due linee si allontanano fino ai bordi e i due campi si riempiono.

## 3. Chiusura (proposta)
Dal tuffo nel testo dettato, nero; il segno **si traccia come un tratto continuo** («>» e «_» in un colpo), l'arco si
riempie come un gauge fino al 70 % con un tick, e l'orologio si materializza dietro già inclinato. Un gesto solo.

## 4. Suoni (Franz, 13:16)
click fotografico soft sull'occhio (4→5) · campanella leggera crescente (9) · tick sui TICK del terminale (12) · whoosh
sulle frustate · tick del gauge (3, 13, chiusura). Asset Material/Pixabay da scaricare, motore già pronto (`sound.ts`).

## 5. Ordine
1. Questo piano a Franz e a master → 2. `Takeover` con test, e i tre takeover del modello (5→6 card, 8→9 tasto, 13→14 gauge)
verificati a 10 fps → 3. gli altri → 4. tile rigirata, domanda corta (voce + clip), voce AI della dettatura → 5. suoni →
6. chiusura → 7. v7 intera.
