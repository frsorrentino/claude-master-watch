La base è leggibile e adatta all’uso: fondo scuro, superfici ampie, domande riconoscibili. **La priorità è rendere immediato “chi aspetta me e cosa devo decidere”**: oggi compete con modello, priorità, quota e ultimo esito. Sul polso questa competizione pesa ancora di più.

Valuto ciò che appare negli screenshot: dimensioni effettive dei tocchi, comportamento allo scroll e semantica accessibile vanno verificati nell’app.

**1. Ottimizzazioni per schermata**

Le misure sotto sono proposte in dp, non misurazioni delle immagini. Sul telefono prevederei aree toccabili di almeno 48 × 48 dp anche per copia, lettura e modifica, distanziate per evitare sovrapposizioni: è il riferimento di [Android per Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).

| Telefono | Ottimizzazioni concrete |
|---|---|
| **t01 — casa master** | **1.** Quando qualcuno aspetta, porta «Per te» sopra «Ultimo esito» e compatta quest’ultimo a titolo e apertura. **2.** Trasforma le due quote affiancate in due righe intere: account, consumo, aggiornamento; «professionale · vecchio» non chiarisce da quanto. **3.** Togli la trama puntinata: aggiunge rumore proprio nella zona da scandire rapidamente. |
| **t02 — righe aperte** | **1.** Allinea domanda, opzioni e apertura conversazione sullo stesso asse; riduci il rientro sotto il nome per recuperare larghezza. **2.** Usa opzioni esplicite, «Avvia deploy» / «Non avviare», se il contenuto originale lo consente; rendi la seconda delineata. **3.** Elimina lo stacco grigio sotto le schede, se appartiene alla schermata e non alla cattura. |
| **t03 — sessioni** | **1.** «contenuto» è un segnaposto: qui serve una lista verificabile con nome, stato, sintesi e tempo, una riga logica alla volta. **2.** Ordina inizialmente: ti aspettano, al lavoro, finite, ferme; mostra i conteggi nel filtro. **3.** L’icona in alto a destra non dichiara la funzione: usa un simbolo specifico e un’etichetta accessibile, per esempio «Ordina sessioni» se questo è il comando. |
| **t04 — conversazione** | **1.** Metti il nome della sessione nell’intestazione; modello e ragionamento possono stare nei controlli secondari. **2.** Nel gruppo strumenti anteponi «1 passaggio fallito» al conteggio tecnico; «+1» deve diventare «1 altro» o sparire nel riepilogo. **3.** Disponi gli allegati su righe intere con nome, tipo e peso: i due chip affiancati reggono solo nomi corti. |
| **t05 — stati dei messaggi** | **1.** Lascia sotto ogni messaggio stato e ora, spostando copia/modifica nel menu del messaggio: ora le azioni ripetute dominano la chat. **2.** Porta errore e «Riprova» dentro il blocco del messaggio fallito, completamente sopra il composer. **3.** Distingui con parole operative «In attesa sul telefono», «Ricevuto dal PC», «Preso in carico», «Elaborato»; mantieni i dettagli intermedi apribili. |
| **t06 — domanda** | **1.** Mostra il progetto sopra la domanda: modello e quota non identificano il destinatario della risposta. **2.** Riduci il bordo ambra a un tratto sottile e rendi «No» delineato; conserva un solo bottone pieno. **3.** «Parliamone» deve portare il focus a «Rispondi con parole tue», rendendo evidente che si scrive. |
| **t07 — altra sessione** | **1.** Aggiungi al banner il tempo: «ledger-api ti aspetta · 5 min». **2.** Rendi toccabile tutta la fascia come un’unica azione «Rispondi», evitando bersagli separati per testo e freccia. |
| **t08 — tabelle** | **1.** Allinea a destra i valori della colonna «righe», lasciando i nomi a sinistra. **2.** Nelle schede larghe separa ogni confronto: «Visualizzazioni: 223 · prima 121», poi «Repost: 4 · prima 1»; evita due metriche dentro ciascuna frase temporale. **3.** L’esito delle 10:03 dopo la risposta delle 10:51 richiede un’etichetta «Esito precedente» o una collocazione cronologica coerente. |
| **t09 — al lavoro** | **1.** Dai all’attività due righe: «Al lavoro da 1 min 40 s» e «Esecuzione dei test»; togli lo spinner decorativo arancione a favore del fulmine previsto. **2.** Mantieni Stop come unica azione piena e assegna il nome accessibile «Interrompi atlas-shop»; dopo il tocco mostra «Interruzione richiesta» fino alla conferma. |
| **t10 — quadro** | **1.** Specifica «11% usato» e «Contesto» dove pertinente: una percentuale isolata non distingue consumo e disponibilità. **2.** Per lavoro mostra «Consumo attuale non disponibile» e «Aggiornato alle…» al posto di trattino e «dato vecchio». **3.** Sostituisci la barra a tre segmenti del lavoro con tre righe toccabili di stato e conteggio: con 5–6 sessioni sono più utili delle proporzioni. |
| **t11 — registro** | **1.** Separa «In coda» da «In esecuzione»: atlas-shop marcato «partito» contraddice «2 lavori in coda». **2.** Sostituisci «NOTTE: 2 LAVORI, 1 RIUSCITO» con «Stanotte · 1 riuscito, 1 scaduto», senza tutto maiuscolo. **3.** Tieni «Togli» distante dal testo del lavoro e rinominalo «Rimuovi dalla coda». |
| **t12 — lancia** | **1.** Uniforma lo sfondo al resto dell’app: qui il grigio rende tutto simile a uno stato disabilitato. **2.** Rendi evidente l’account selezionato con contorno e segno di selezione, preservando cerchio/quadrato. **3.** Mostra vicino a «Lancia» il requisito mancante, per esempio «Seleziona un progetto»; il solo pulsante spento non spiega come procedere. |
| **t13 — fila domande** | **1.** Porta «1 di 2» vicino al titolo e scrivi «Domanda 1 di 2»: in fondo è distante dal compito. **2.** Riusa esattamente la scheda di t06, compresi stato, opzioni e voce. **3.** Dopo la risposta mostra conferma di ricezione prima di avanzare, evitando che il cambio domanda sembri perdita del comando. |
| **t14 — terminale** | **1.** Aggiungi sotto «kb» una riga «Dal vivo · aggiornato …»; la freccia circolare da sola non dice se i dati arrivano. **2.** Quando l’utente risale, sospendi l’inseguimento del fondo e mostra «Torna al presente» come azione testuale. **3.** Mantieni il monospace per comandi e output, con spaziatura verticale fra chiamata, risultato e testo dell’assistente. |
| **t15 — impostazioni** | **1.** Riduci l’illustrazione dei dispositivi: occupa spazio utile senza aggiungere stato. **2.** «Chiave consegnata» va accompagnato da un’informazione comprensibile, come ultimo contatto dell’orologio; non equivale necessariamente a dispositivo connesso. **3.** Sposta «Rifai l’accoppiamento» nei dettagli del dispositivo, separandolo dalle preferenze ordinarie. |
| **t16 — condividi** | **1.** Mostra una piccola anteprima dell’immagine al posto della sola frase «Con l’immagine condivisa». **2.** Rendi esplicita la sessione scelta con contorno e selezione; il pulsante diventa «Manda a ledger-api». **3.** Sposta stato e durata su una seconda riga, così il nome del progetto non compete con il badge laterale. |
| **t17 — ricerca** | **1.** Aggiungi «3 risultati» e un comando per cancellare il testo. **2.** Per ogni risultato usa righe distinte: progetto, estratto, data e ora; il timestamp laterale sottrae spazio al nome. **3.** Apri il messaggio trovato con evidenziazione temporanea e contesto circostante. |

| Orologio | Ottimizzazioni concrete |
|---|---|
| **o01 — sessioni** | **1.** Il progetto manca nella scheda prioritaria: prima riga «mano + ledger-api», seconda «In attesa da 5 min», poi sintesi. **2.** Togli campanella e priorità dalla prima vista: occupano la zona più leggibile senza aiutare a rispondere. **3.** Elimina «atlas…» nella riga successiva, coerentemente con il vincolo sul taglio senza puntini. |
| **o02 — scheda** | **1.** Ordina il contenuto in «Attività», «Da verificare», «Obiettivo»; oggi tre testi di peso simile si accumulano. **2.** Sposta la quota dopo le azioni della sessione: il frammento visibile in fondo non aiuta il compito corrente. **3.** Prevedi ▶ accanto al riepilogo, distinguendolo dal simbolo di stato. |
| **o03 — domanda** | **1.** Riduci il grande contenitore del ▶ e recupera altezza per domanda e opzioni. **2.** Mantieni domanda completa e risposte nella stessa lista deformabile, con spazio finale sufficiente per portare l’ultima risposta al centro del quadrante. **3.** Usa mano e badge account coerenti con il telefono; il punto interrogativo blu introduce un’altra grammatica. |
| **o04 — quota** | **1.** «Quota ○» diventa «Personale» con badge circolare; «11% usato» chiarisce il valore. **2.** Riduci il grafico e dai righe autonome a «Ripristino alle 16:00» e «Settimana: 36%»; il dato settimanale non deve sembrare un pulsante. **3.** Localizza «Overview», «reset», «Thu» ed esplicita a quale intervallo si riferisce ciascun ripristino. |
| **o05 — terminale** | **1.** Porta «42 test superati · 3,1 s» prima del comando tecnico: è il risultato utile al polso. **2.** Usa «Terminale · 11:00» come metadato secondario; ora compete con il contenuto. **3.** Riduci anche qui il contenitore del ▶ e fai arrivare ogni blocco nella zona centrale tramite scroll, senza rimpicciolire il testo. |

Sul polso conserverei la deformazione delle liste. Verificherei soprattutto che **ogni elemento possa raggiungere la zona centrale**, anche con caratteri ingranditi: Android offre [anteprime Wear per dimensioni e scale del testo](https://developer.android.com/training/wearables/compose/previews).

**2. Incoerenze da eliminare**

- **Stati:** mano e bandierina convivono con `?`, play verde, spunta gialla e spinner arancione. Applica ovunque le quattro icone decise; riserva ▶ esclusivamente alla lettura.
- **Account:** cerchio/quadrato devono accompagnare l’identità dell’account, senza essere confusi con il contenitore dell’icona di stato. Uniforma i nomi a «Personale» e «Lavoro», oggi mescolati con personal, work e professionale.
- **Domande:** t02 usa ambra sul bottone, t06/t13 azzurro, o03 un’altra composizione. Stesso componente concettuale: ambra segnala attesa, colore primario identifica l’azione.
- **Bottoni:** «No» e varie azioni secondarie hanno superfici piene o tonali. Per rispettare chiaramente il vincolo, usa contorno o testo per tutte le azioni secondarie. Nella chat, assegna il bottone pieno all’azione del momento: risposta, invio oppure Stop.
- **Intestazioni:** nelle chat manca il nome del progetto mentre modello e ragionamento sono prominenti; terminale e condivisione identificano invece il contesto. Il destinatario deve occupare una posizione stabile.
- **Tempo e percentuali:** «5 m», «1 min 40 s», «10:45», «vecchio» esprimono cose diverse senza sempre dirlo. Usa «da» per durata, «alle» per eventi e «aggiornato» per freschezza; nomina quota e contesto.
- **Superfici e ritmo:** t02/t12 introducono grandi fondi grigi, le altre schermate nero; bordi ambra e raggi delle schede cambiano molto. Adotta pochi valori condivisi: per esempio margini laterali 16 dp, padding schede 16 dp, intervalli 8/16/24 dp.
- **Lingua e voce:** localizza le etichette dell’interfaccia e i dati demo; preserva il testo originale delle conversazioni. Uniforma ▶ oggi alternato all’altoparlante, con descrizioni come «Leggi la risposta di atlas-shop».

**3. Sei funzionalità nuove**

| Funzionalità | Perché serve | Dove |
|---|---|---|
| **«Da quando sei uscito»** | Riassume solo nuovi esiti, blocchi ed errori: evita di riaprire sei conversazioni. Include ▶ per ascoltare il riepilogo. | Casa master, blocco compatto espandibile. |
| **Notifiche delle attese con sollecito configurabile** | Una domanda blocca lavoro reale; deve distinguersi da un normale completamento. Riunisci le attese multiple e interrompi i solleciti quando la risposta viene ricevuta. | Notifiche telefono/orologio; preferenze in Impostazioni. |
| **«Avvisami quando…»** | Permette di seguire una condizione concreta: sessione finita, test conclusi, quota ripristinata. Riduce i controlli ripetuti. | Menu sessione e scheda quota. |
| **Passaggio polso → telefono sullo stesso punto** | Una domanda lunga o un output richiedono più spazio senza dover ritrovare la sessione. | Azione «Continua sul telefono» in domanda, scheda e terminale Wear. |
| **Coda dei messaggi modificabile prima della presa in carico** | Consente di correggere, riordinare o annullare istruzioni inviate in sequenza. Disponibile solo finché il PC non le prende in carico. | Espansione dello stato «In coda» nella chat. |
| **Stato del collegamento con ultimo contatto** | Distingue una sessione silenziosa da un PC irraggiungibile e chiarisce se il comando deve ancora partire. | Indicatore discreto nella casa; fascia esplicita quando offline; dettaglio in Impostazioni. |

**4. Le cinque modifiche a più alto impatto e basso costo**

1. **Nome del progetto sempre visibile nelle chat e nelle domande.** Riduce subito il rischio di rispondere alla sessione sbagliata.
2. **Unificare icone di stato, ▶ e nomi degli account.** Corregge un’ambiguità presente quasi ovunque senza cambiare i flussi.
3. **Portare le attese sopra l’ultimo esito nella casa.** Basta un ordinamento condizionale per rendere prioritario il lavoro bloccato.
4. **Sistemare t05: errore e Riprova sopra il composer, stati più chiari.** Rende recuperabile un problema oggi parzialmente nascosto.
5. **Uniformare t02/t06/t13 con una scheda domanda condivisa.** Stesse spaziature, opzioni secondarie delineate e un solo bottone pieno.

**Vincolo da rivedere:** consentirei il ritorno a capo completo almeno per domande e opzioni decisionali; il taglio senza puntini può nascondere proprio la condizione necessaria per rispondere.