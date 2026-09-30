# Telefono: Panoramica integrata, attenzione e costi, Scrivania, widget (design, 30/09/2026)

Approvato da Franz in chat il 30/09 alle 20:57 («Approvo tutto»): la regola «ogni dato ha una sola casa», la barra
fable-director, la card «PC», le proposte del secondo e del terzo giro. Il widget (20:59, «ragioniamo anche su un widget
con uno stile simile, personalizzabile») è una proposta da approvare. Sottoprogetti B (Scrivania), C (attenzione e
costi) e F (widget) del programma del 30/09; A (scheda e chat) ha la sua specifica, D (file e cambiamenti) ed E
(Android e voce) avranno la loro.

## Regola: ogni dato ha una sola casa

Una informazione vive in un posto; altrove compare solo come rimando piccolo e toccabile.

| Informazione | Panoramica (tutte le sessioni) | Scheda sessione (quella sola) |
|---|---|---|
| Quota 5 ore e settimana, ritmo | casa: anelli | solo un avviso nella barra di scrittura quando la finestra finisce; tocco = Panoramica |
| Contesto, budget, deleghe | casa: card «Al lavoro», una riga per sessione | barra fable-director con le azioni della sessione |
| Domande e approvazioni | casa: «Ti aspettano», tocco = la coda | la domanda di quella sessione |
| Costo | nella card «Oggi»: eventi per ora e costo del giorno | costo della sessione nel foglio del budget |
| Notte | casa: card «Notte» | «Manda stanotte» dalla barra di scrittura |
| PC e desktop | casa: card «PC» | «Mostra sul PC» nel menu delle sessioni |

## Panoramica

Ordine: Quota (anelli e ritmo) → Ti aspettano → Adesso → Al lavoro → Oggi → Notte → PC → riga dell'aggiornamento.

- **Ti aspettano:** domande aperte e richieste di approvazione, con la più vecchia; tocco = la coda.
- **Al lavoro** (al posto di «Contesto»): per ogni sessione viva nome e badge, contesto con modello ed effort, budget
  fable-director (barra consumato/stimato con tacche a 2× e 3×, verde, ambra, rosso come nella statusline), deleghe in
  corso, avviso «gira a vuoto». Oltre l'80 % di contesto il chip «Handoff» manda `/fable-director:handoff` alla sessione.
- **Oggi:** le colonne per ora come adesso, più il costo del giorno dai budget chiusi.
- **PC:** salute della macchina (RAM, CPU, disco), miniatura dei monitor con le finestre, «Ripristina N sessioni» dopo
  un riavvio, l'interruttore «Sono alla scrivania». Tocco = la pagina Scrivania.
- L'orologio resta com'è: «Al lavoro» esteso e «PC» per ora solo sul telefono. Gli avvisi («gira a vuoto», oltre 3×)
  arrivano però anche al polso, come un avviso unico dappertutto.

## Scheda sessione: barra fable-director

Sopra la barra di scrittura, al posto dei numeri di righe e di «Crea PR» dell'app nativa: budget (barra con tacche,
rapporto ed effort, «1,4× · high»), anellino del contesto con «Handoff» oltre la soglia, deleghe e chiamate esterne,
avviso «gira a vuoto». Senza budget aperto resta il solo contesto. Tocco = foglio con task, comando di verifica, stime e
consumi in ingresso e in uscita, strada scelta e perché, cache, costo della sessione.

## Attenzione

- **Coda «Ti aspettano»:** domande e approvazioni di tutte le sessioni, dalla più vecchia; si risponde e si passa alla
  successiva con uno swipe. Si apre dalla Panoramica e dalla notifica.
- **Scorrere tra le sessioni:** swipe laterale nella scheda, nell'ordine della regia (prima chi aspetta te).
- **Un avviso unico:** «gira a vuoto» o oltre 3× si vedono nella riga «Al lavoro», nel badge della lista, nella
  notifica e sul polso; una sola regola in `core` decide quando.
- **Sessione in primo piano:** la sessione seguita come aggiornamento dal vivo di Android (pillola nella barra di stato
  «atlas-shop ▶ 3 min», dentro lo strumento in uso e la barra fable-director).

## Quota e invio

- Nella barra di scrittura, se la finestra di 5 ore è oltre il 90 % o il ritmo la esaurisce prima della ripartenza:
  «finestra al 94 %, riparte alle 21:50», con due scelte:
  - **Invia alla ripartenza:** il telefono tiene il messaggio (stato «programmato» nella chat) e lo manda un minuto dopo
    la ripartenza; sull'anello della quota un segno all'ora dell'invio;
  - **Manda stanotte:** `night_add` con la cartella della sessione; finisce nella card «Notte».
- **Costo del turno** sul fumetto di Claude: durata, quanto ha consumato della finestra (dai campioni della quota),
  token quando arrivano con `transcript`.
- **Ricerca** in chat e Diario degli ultimi 7 giorni; **frasi rapide** per progetto (i tre prompt più usati) accanto al
  prompt suggerito.

## Scrivania

- Pagina che si apre dalla card «PC» e dal menu delle sessioni.
- Mappa dei monitor con le finestre delle sessioni al loro posto e i badge. Si selezionano una o più sessioni, poi:
  Affianca (colonne, righe, griglia), Unisci in schede, Sposta su un monitor, Porta in primo piano.
- Layout con nome: elenco, «Ripristina», «Salva la disposizione di adesso».
- **Sono alla scrivania:** acceso = sul PC si affiancano le sessioni che aspettano te e il telefono zittisce le notifiche
  (restano le domande a rischio alto); spento = notifiche normali.
- **Menu delle sessioni come telecomando:** Mostra sul PC, Riavvia pulita, Chiudi, colore della sessione.
- **Browser del PC** (chrome-bridge): screenshot della pagina che una sessione sta provando, «Apri questo link sul PC»,
  appunti condivisi nei due sensi.
- Senza chrome-bridge in ascolto la pagina lo dice e mostra solo quello che non ne ha bisogno.

## Widget (proposta)

Nello stile del widget di ads-widget, con la tavolozza dell'app e dell'orologio.

- **Una card per riga**, fondo scuro traslucido, indicatore ad arco a sinistra con valore ed etichetta, tre colonne con
  icona, numero grande ed etichetta, in testata ▸ nome, ora dell'aggiornamento e ↻.
- **Tre modi:**
  - **Account:** arco = quota 5 ore con l'ora della ripartenza; colonne scelte fra settimana, al lavoro, ti aspettano,
    notte, costo di oggi;
  - **Sessione** (una scelta, o la seguita): arco = contesto o budget; colonne fra stato ed età del turno, budget, deleghe,
    ultimo esito; tocco = la sua scheda;
  - **Regia:** arco = la quota più piena; colonne ti aspettano, lavorano, ferme; tocco = Panoramica.
- **Taglie:** striscia (una riga), piccola, media, grande (la lista delle sessioni come la scheda Sessioni).
- **Personalizzazione** alla posa: modo, account o sessione, colonne, tema, angoli, colori/grigi/monocromo, opacità.
- **Aggiornamento:** a ogni stato che arriva (push), non ogni 30 minuti; ↻ chiede lo stato al PC.
- **Tecnica:** Jetpack Glance (`glance-appwidget`, `glance-material3`), dati da `Repo` come la tile dell'orologio; le
  regole di cosa mostrare in `core`, con TDD.

## Media prodotti da Claude nel flusso (Franz, 21:03)

- Immagini, video, PDF e altri file che Claude produce in un turno (scritti su disco, mandati con l'invio file, report)
  compaiono nel flusso della chat come anteprima: miniatura per le immagini, prima pagina per i PDF, primo fotogramma
  con ▶ per i video, icona con nome e peso per gli altri.
- Tocco = visore a tutto schermo: immagine con zoom, PDF sfogliabile, video con i controlli; «Condividi» e «Salva» dal
  visore. Il file arriva dal PC cifrato, solo quando lo si apre (le miniature piccole con la chat).
- Dal relay: le voci di `transcript` portano i file del turno (percorso, tipo, peso), e il comando dei file nei due
  sensi (richiesta 4) li consegna.

## Relay: richieste in coda, una per volta

1. `transcript` (inviata il 30/09 alle 20:58) → 2. `suggestion` → 3. dati fable-director per sessione (budget, deleghe,
«gira a vuoto», soglia di handoff, costo) → 4. file nei due sensi → 5. Scrivania (stato di monitor, finestre e layout;
op affianca, unisci, sposta, primo piano, layout; «sono alla scrivania») → 6. cambiamenti del turno e PR → 7.
approvazioni → 8. browser e appunti → 9. salute, ripristino, chiudi, riavvia, colore. Il widget non chiede niente al
relay.

## Test

- `core`, con TDD: ordine e scorrimento della coda, avviso quota dal ritmo, regola dell'invio programmato, costo del
  turno, ricerca e frasi rapide, soglie della barra fable-director e dell'avviso unico, modello dei dati del widget.
- Paparazzi: Panoramica nuova, barra fable-director, coda, Scrivania; anteprime Glance del widget nelle quattro taglie.
- Dal vivo: una riga per funzione nella checklist, dopo ogni richiesta al relay.

## Fuori

File e cambiamenti (D) e Android e voce (E, compreso l'aggiornamento dal vivo) hanno la loro specifica.
