Ecco la consulenza completa di design di prodotto per **claude-master-watch**, strutturata per rispondere punto per punto alle tue richieste.

---

### 1. Ottimizzazioni puntuali per ogni schermata

#### Wear OS (Orologio)

*   **`o01-orologio-sessioni.png`**
    1.  *Iconografia e localizzazione:* Sostituire l'icona `?` blu e la campanella con l'icona standard di vincolo (mano arancione per «ti aspetta»); tradurre «5 m · low priority» in «5 min · bassa» e «Sessions» in «Sessioni».
    2.  *Sfruttamento dell'area circolare:* Il padding verticale interno della card è eccessivo; ridurlo di 4dp per far emergere la seconda riga (`atlas-shop`) sopra la curva inferiore ed evitare il senso di schermata monovoce.
*   **`o02-orologio-scheda.png`**
    1.  *Icona di stato nel titolo:* L'icona verde ▶ accanto ad `atlas-shop` si confonde con il tasto di lettura vocale; usare l'icona del fulmine verde per «al lavoro».
    2.  *Gerarchia tipografica:* Distinguere l'obiettivo («Goal: ...») dal corpo dell'ultimo turno tramite sfondo chip a contrasto minimo o etichetta con font mono per dare colpo d'occhio immediato a braccio sollevato.
*   **`o03-orologio-domanda.png`**
    1.  *Safe Area del tasto vocale:* Il tasto ▶ in alto a destra è troppo aderente al bordo circolare (rischio tocco mancato o clipping su schermi piccoli); posizionarlo inline accanto al titolo del progetto o centrarlo nella testata.
    2.  *Taglio delle risposte:* Il pulsante «1 · yes» (pieno celeste) domina, ma l'opzione «2» è tagliata sotto. Portare la card della domanda a dimensione compatta (max 2 righe con scroll) per garantire che entrambe le opzioni primarie siano visibili al primo colpo.
*   **`o04-orologio-quota.png`**
    1.  *Localizzazione e leggibilità account:* Tradurre «Overview» in «Quota» e «Thu» in «gio». Il cerchio «Quota ○» (account personale) è troppo piccolo; sostituire il testo con un badge circolare/quadrato colorato accanto alla percentuale.
    2.  *Grafico a ciambella:* I due anelli concentrici viola/azzurro non indicano visivamente quale corrisponde alla quota 5 ore e quale ai 7 giorni; allineare i colori dell'anello con i colori dei rispettivi testi (es. anello viola per la pillola 7d viola).
*   **`o05-orologio-terminale.png`**
    1.  *Margine di sicurezza inferiore:* Il testo monospace `42 passed in 3.1s` tocca il bordo inferiore sferico. Aggiungere un padding inferiore di almeno 24dp (o `RotaryScrollable` con fading edge) per non troncare i caratteri periferici.
    2.  *Separatore temporale:* Tradurre `— Terminal · 11:00 —` in `— Terminale · 11:00 —` e ridurre lo spessore delle linee laterali per dare respiro al testo.

---

#### Telefono Android

*   **`t01-casa-master.png`**
    1.  *Asimmetria bottoni ultimo esito:* Il tasto rotondo ▶ e la pillola «Apri la conversazione >» hanno altezze e pesi visivi disallineati; unificarli racchiudendo il ▶ all'interno della testata o come icona prefissa nel pulsante di navigazione.
    2.  *Barre quota inferiori:* Il chip «professionale · vecchio» non mostra il valore numerico a colpo d'occhio come il personale («3% · sett. 11%»). Mostrare `--%` o l'ultimo valore noto in grigio per mantenere ritmo orizzontale identico.
*   **`t02-per-te-righe-aperte.png`**
    1.  *Coerenza bottoni di scelta:* La card di `ledger-api` usa un bottone pieno arancione e uno tonale, mentre `atlas-shop` usa due bottoni outlined verdi. Standardizzare: se è una risposta secca a Claude, usare tasti tonali con quello consigliato pieno; se sono azioni successive, usare pillole della stessa famiglia.
    2.  *Spaziatura tra card espanse:* Manca un divisore o un gap marcato (almeno 8dp) tra la card `ledger-api` e `atlas-shop`, creando un blocco unico visivamente pesante.
*   **`t03-sessioni.png`**
    1.  *Empty state/Caricamento:* La schermata presenta solo la scritta `contenuto` in alto a sinistra. Sostituirla con la lista delle sessioni raggruppate per stato (In attesa, Al lavoro, Ferme) o con uno stato vuoto esplicito con pulsante «+ Lancia sessione».
    2.  *Menu a tendina:* «Tutte le sessioni ▾» è isolato; allinearlo a sinistra con un contatore totale accanto (es. `6 attive`) per chiarezza di contesto.
*   **`t04-chat-conversazione.png`**
    1.  *Header sovraccarico:* La seconda riga dell'header (`Obiettivo: ... bassa priorità` in verde su nero) ha un contrasto debole e si sovrappone visivamente al pulsante «Carica i messaggi precedenti». Riorganizzare l'obiettivo in un chip collassabile sotto la barra.
    2.  *Icona vocale:* Sotto il messaggio viene usata l'icona altoparlante standard anziché il glifo ▶ usato nel resto dell'app per il TTS; sostituirla per coerenza.
*   **`t05-chat-messaggi-dal-telefono.png`**
    1.  *Affollamento micro-azioni:* La combinazione stato + timestamp + icona copia + matita modifica sotto ogni messaggio inviato crea troppo rumore orizzontale. Mostrare solo `✓ stato · ora` e rivelare copia/modifica con tocco lungo (long press) o swipe.
    2.  *Sovrapposizione banner errore:* La notifica di mancata risposta del PC (`! non consegnato... Il PC non ha risposto`) è tagliata dalla barra inferiore di digitazione. Usare uno snackbar ancorato sopra la barra di testo.
*   **`t06-chat-domanda.png`**
    1.  *Pulsante alternativo («Parliamone»):* Il pulsante `Parliamone` ha lo stesso stile di contenitore del campo di input sottostante («Rispondi con parole tue»), creando ambiguità su dove toccare per scrivere a mano libera.
    2.  *Contrasto del pulsante primario:* Il bottone «1 · yes» in azzurro chiaro all'interno di una card con bordo arancione crea un conflitto cromatico (arancione = attesa, azzurro = selezione). Allineare il colore del primario all'accento della domanda.
*   **`t07-avviso-altra-sessione.png`**
    1.  *Banner invasivo:* Il banner `ledger-api ti aspetta >
*   **`t07-avviso-altra-sessione.png`** *(seguito)*
    1.  *Banner di avviso superiore:* Il banner `ledger-api ti aspetta > Rispondi` occupa molto spazio verticale ed è staccato dal flusso. Usare una barra sottile «floating pill» in cima alla chat con animazione di comparsa/scomparsa, che mostri il tempo di attesa e permetta il salto rapido con un tocco.
    2.  *Priorità dei contesti:* Se l'utente sta leggendo una sessione attiva, l'avviso deve essere visibile ma non coprire i primi messaggi della conversazione corrente (aggiungere `contentPadding` in cima alla lista quando il banner è visibile).
*   **`t08-tabelle-nel-testo.png`**
    1.  *Formato griglia / schede:* La tabella `file | righe` in puro testo monospace crea disallineamento visivo con le card arrotondate `post del film` sottostanti. Racchiudere anche la tabella a due colonne in un blocco con micro-superficie tonale per mantenere coerenza nei blocchi di dati.
    2.  *Contrasto etichette secondarie:* Le diciture `a mezzogiorno:` e `ora:` all'interno delle card hanno un grigio scuro a basso contrasto su fondo scuro; portare il colore del testo a `onSurfaceVariant` con contrasto WCAG AA minimo (4.5:1).
*   **`t09-sessione-al-lavoro.png`**
    1.  *Pulsante Stop:* Il pulsante di stop (quadrato bianco/blu) in basso a destra nella barra di digitazione ha un forte impatto, ma non è chiaro se fermi l'intera sessione o solo il turno/tool corrente. Aggiungere un micro-feedback di conferma o separare l'azione «Interrompi turno» da «Chiudi sessione».
    2.  *Stato di avanzamento in tempo reale:* La riga `* 1 min 40 s · Run the test suite` è molto statica. Aggiungere un indicatore a barra pulsante o shimmer sottile sul bordo superiore della barra inferiore per dare percezione di processo vivo mentre si è lontani dal PC.
*   **`t10-quadro.png`**
    1.  *Card «Ti aspettano» dominante:* La card gialla/arancione a tutta larghezza in basso attira l'occhio più della sezione «Adesso». Essendo un'azione urgente, spostarla in cima alla pagina (sopra o subito sotto Quota) come primo blocco operativo.
    2.  *Grafico a barre orizzontale in «Adesso»:* La barra tripartita (giallo, azzurro, verde) non ha una scala visiva né separatori chiari rispetto alla legenda sottostante. Aggiungere percentuali o valori numerici diretti su ogni segmento della barra.
*   **`t11-registro.png`**
    1.  *Azione «Aggiungi» e «Togli» in Stanotte:* Il tasto «Aggiungi» in alto a destra è un pulsante pieno tonale, mentre «Togli» è un semplice testo cliccabile allineato a destra. Uniformare la riga con un'icona di rimozione (cestino/meno) o tocco lungo per evitare tocchi accidentali su «Togli».
    2.  *Chiusura e collassabilità:* La sezione «Notte: 2 lavori, 1 riuscito» usa un'icona ▶ a destra che fa pensare a un'esecuzione o audio, mentre nelle sezioni «Oggi» e «Ieri» usa una freccia verso l'alto/basso `^ / v`. Unificare l'indicatore di espansione con la classica freccia di chevron.
*   **`t12-lancia.png`**
    1.  *Pulsante primario disabilitato:* Il tasto «Lancia» in basso appare grigio scuro su nero quasi invisibile. Aumentare il contrasto del testo disabilitato e mostrare un micro-messaggio di aiuto (es. «Seleziona un progetto») se l'utente tocca il pulsante disabilitato.
    2.  *Riconoscimento account nei Recenti:* L'elenco `atlas-shop`, `ledger-api`, `orbit-docs` mostra cerchi e quadrati outlined a sinistra che sembrano radio button o checkbox selezionabili anziché il badge identificativo dell'account (cerchio = personale, quadrato = lavoro). Riempire la sagoma o colorarla per distinguerla da un controllo di selezione.
*   **`t13-fila-ti-aspettano.png`**
    1.  *Paginazione e swipe:* L'indicatore `1 di 2` in basso è piccolo e lontano dal blocco della domanda. Spostarlo in testata accanto al titolo (`Ti aspettano · 1/2`) o usare punti di scorrimento (carousel indicators) per comunicare che la card è scorrevole orizzontalmente.
    2.  *Pulsante vocale:* Il tasto ▶ all'interno della card della domanda è un semplice triangolo outline senza sfondo; renderlo un chip tonale per renderlo un bersaglio di tocco evidente (almeno 48x48dp).
*   **`t14-terminale.png`**
    1.  *Leggibilità font e margini:* Il testo del terminale è allineato all'estremo bordo sinistro senza padding orizzontale sufficiente (almeno 16dp). Aumentare l'interlinea (`lineHeight`) del font monospazio per facilitare la scansione su schermo OLED.
    2.  *Pulsante di refresh/riconnessione:* L'icona circolare di ricarica in alto a destra è isolata e non comunica lo stato del flusso (connesso in tempo reale vs disconnesso/in polling). Aggiungere un piccolo punto verde di stato di connessione relay accanto al titolo.
*   **`t15-impostazioni.png`**
    1.  *Diagramma topologia superiore:* Il grafico PC-Telefono-Orologio è visivamente accattivante ma non interattivo né esplicativo dello stato dei singoli nodi. Aggiungere etichette o colori di stato (verde = sincronizzato, grigio = offline) sotto i tre dispositivi.
    2.  *Target di tocco nelle impostazioni vocali:* Le voci «Lingua», «Voce», «Velocità di lettura» sono righe di testo con descrizione sotto senza freccia di navigazione `>` o indicazione visiva che siano toccabili. Aggiungere uno chevron destro o racchiuderle in chip selettori.
*   **`t16-condividi.png`**
    1.  *Feedback di selezione:* Toccando una sessione di destinazione (`ledger-api`, `atlas-shop`, `field-notes`), lo stato selezionato deve avere un bordo marcato o un checkmark evidente prima di premere «Manda» in basso.
    2.  *Anteprima miniatura:* La riga `Con l'immagine condivisa` ha solo un'icona generica; mostrare una thumbnail reale quadrata dell'immagine catturata (40x40dp) per dare conferma visiva di ciò che si sta per inviare.
*   **`t17-cerca.png`**
    1.  *Differenziazione dei risultati:* I risultati mostrano `Registro` e `ledger-api` con lo stesso colore azzurro e struttura. Evidenziare la cartella o il tipo di documento (es. chip «Chat», chip «Registro») per consentire di filtrare o distinguere a colpo d'occhio.
    2.  *Evidenziazione della query:* La parola `deploy` è in grassetto bianco, ma il resto del testo è anch'esso bianco o grigio chiarissimo. Usare un colore d'accento di sfondo (highlight giallo tenue o celeste) sulla parola cercata.

---

### 2. Incoerenze rilevate tra le schermate

1.  **Icone e significati del tasto ▶:**
    *   In `t01`, `t04`, `t06`, `t13`, `o03` e `o05` il triangolo ▶ indica la **lettura vocale (TTS)**.
    *   In `t11` (riga Notte) il triangolo ▶ indica **espandi/collassa**.
    *   In `o01`, `o02`, `t16` il cerchio verde con ▶ indica **sessione al lavoro** (in contrasto con il vincolo stabilito che impone il fulmine ⚡ come icona per «al lavoro»).
2.  **Stile e colore dei pulsanti di risposta:**
    *   In `t02` la risposta a `ledger-api` usa un bottone pieno giallo/arancio (`1 · yes`) e uno grigio scuro (`2 · no`).
    *   In `t06` la risposta alla stessa domanda usa un bottone pieno azzurro chiaro (`1 · yes`) con il bordo card arancione.
    *   In `t02` per `atlas-shop` i bottoni sono outlined verdi senza opzione piena.
3.  **Rappresentazione degli account (Personale vs Lavoro):**
    *   Nel testo e nelle etichette si usano a volte i termini `personal` / `work` in inglese (`t10`, `t12`) e a volte `personale` / `professionale` o `lavoro` in italiano (`t01`, `t11`).
    *   La forma del badge (cerchio = personale, quadrato = lavoro) in `t12` appare come checkbox vuota non riempita, inducendo l'utente a pensare a un elemento di selezione multipla.
4.  **Indicatori di espansione (Accordion):**
    *   In `t01` e `t02` si usano frecce standard `^` e `v`.
    *   In `t11` si mescolano il triangolo orizzontale `▶` (Notte) con le frecce verso l'alto `^` (Oggi) e verso il basso `v` (Ieri).
5.  **Notifiche e banner di stato:**
    *   L'errore di mancata consegna in `t05` è un testo rosso inline in fondo allo scroll; l'avviso di attesa in `t07` è una card arrotondata marrone/arancione in cima. Manca un componente standardizzato per gli avvisi di sistema.

---

### 3. Funzionalità nuove ad alto valore per lo sviluppatore lontano dal PC

1.  **Azione rapida «Continua senza di me» (Auto-approva comandi di sola lettura):**
    *   *Perché:* Spesso Claude si ferma per chiedere conferme banali su letture di file o esecuzione di test. Un toggle temporaneo da telefono/orologio («Fai da solo per 30 min per letture/test») evita interruzioni continue quando si è alla guida o lontani.
    *   *Dove:* Nell'header della chat (`t04`) e come swipe-action nella scheda dell'orologio (`o02`).
2.  **Sveglia con notifica critica / bypass «Non Disturbare» per Sessioni Bloccate:**
    *   *Perché:* Se un job notturno o una build si blocca su una domanda a mezzanotte, il lavoro si ferma fino al mattino. Una notifica push ad alta priorità (canale allarmi) sveglia o notifica al polso solo se c'è un blocco effettivo.
    *   *Dove:* Nelle impostazioni (`t15`) sotto una nuova voce «Notifiche critiche per domande».
3.  **Comando vocale rapido pre-registrato (Preset di risposte frequenti):**
    *   *Perché:* L'app non ha microfono interno (vincolo). Quando si è a piedi o con l'orologio, digitare è scomodo. Avere 3-4 risposte preconfigurate modificabili (es. «Sì, procedi», «Mostrami prima il diff», «Fermati e aspetta che torni al PC», «Fai il commit e apri PR») inviabili con un tocco.
    *   *Dove:* Come chip scorrevoli orizzontali sopra la barra di input in `t04`, `t06` e nell'orologio sotto le opzioni in `o03`.
4.  **Vista sintetica del Diff / File modificati:**
    *   *Perché:* Prima di dare «yes» a un deploy o a un commit, lo sviluppatore vuole vedere *cosa* è cambiato senza aprire il terminale grezzo. Una vista sintetica a fisarmonica (`+12 -3 in Repo.kt`) dà sicurezza immediata.
    *   *Dove:* Accessibile come pulsante compatto nella card di fine turno (`t02`, `t04`) e come schermata dedicata dopo il tocco sul riepilogo file (`t08`).
5.  **Tile Wear OS «Stato Rapido & Ti aspettano» (Wear OS Tile):**
    *   *Perché:* Permette di controllare senza aprire l'app se qualche sessione è bloccata: uno swipe a sinistra dal quadrante dell'orologio mostra subito il numero di sessioni in attesa e l'anello della quota residua.
    *   *Dove:* Tile nativa di Wear OS dedicata, con tocco diretto che apre `o03` se c'è una domanda pendente.
6.  **Pianificatore sequenziale della Coda Notturna (Night Queue Reorder):**
    *   *Perché:* In `t11` i compiti notturni sono in lista fissa. Poter riordinare i progetti via drag-and-drop o impostare una condizione («Esegui `ledger-api` solo se `atlas-shop` termina con successo») massimizza il tempo notturno senza spreco di token.
    *   *Dove:* Nella sezione «Stanotte» di `t11`.

---

### 4. Le 5 modifiche a più alto impatto e più basso costo

1.  **Unificazione semantica delle icone di stato e del tasto vocale ▶:**
    *   *Costo:* Bassissimo (sostituzione risorse Drawable / Icone Vector in Compose).
    *   *Impatto:* Altissimo. Elimina l'ambiguità su cosa sia un'azione audio, cosa sia un'espansione e cosa sia uno stato attivo (`fulmine ⚡` per al lavoro, `mano ✋` per attesa, `▶` rigorosamente riservato al TTS).
2.  **Correzione del padding e layout circolare su Wear OS (`o03`, `o05`):**
    *   *Costo:* Basso (aggiunta di `ScalingLazyColumn`, `contentPadding` standard e `fadingEdge`).
    *   *Impatto:* Altissimo. Rende le risposte alle domande e le righe di log leggibili e toccabili senza troncamenti fisici sui bordi dell'orologio.
3.  **Localizzazione e standardizzazione totale dei testi (Personale/Lavoro, Quota, Tempi):**
    *   *Costo:* Bassissimo (pulizia dei file stringhe/formattatori).
    *   *Impatto:* Alto. Elimina il mix italo-inglese (`personal` vs `personale`, `Sessions` vs `Sessioni`, `Thu` vs `gio`), donando un aspetto professionale e rifinito.
4.  **Gerarchia della card «Ti aspettano» in cima a Casa e Quadro (`t01`, `t10`):**
    *   *Costo:* Bassissimo (inversione dell'ordine dei composable nel layout principale).
    *   *Impatto:* Alto. Pone l'azione bloccante (la domanda a
4.  **Gerarchia della card «Ti aspettano» in cima a Casa e Quadro (`t01`, `t10`):** *(completamento)*
    *   *Costo:* Bassissimo (inversione dell'ordine dei composable nel layout principale).
    *   *Impatto:* Alto. Pone l'azione bloccante (la domanda a cui l'utente deve rispondere subito) al primo posto visibile senza costringere a scorrere la schermata quando si apre l'app in mobilità.
5.  **Standardizzazione visiva dei pulsanti di risposta tra chat
5.  **Standardizzazione visiva dei pulsanti di risposta tra chat e liste (`t02`, `t06`, `t13`, `o03`):**
    *   *Costo:* Basso (definizione di un unico composable riutilizzabile `ResponseButtonGroup`).
    *   *Impatto:* Altissimo. Uniforma lo stile dell'opzione raccomandata (un unico bottone pieno) e delle risposte secondarie (tonali/outlined), garantendo che l'utente compia la scelta giusta con un solo tocco senza esitazioni.

---

### Vincoli da rivedere (nota separata)
> **Vincolo "Nessun puntino di sospensione (…)":** Il taglio netto senza puntini nelle stringhe lunghe (es. percorsi file o titoli nei chip compatti) rischia di far sembrare i testi parole troncate per errore di layout; per percorsi e identificatori tecnici è consigliabile valutare la troncatura centrale (`core/.../Repo.kt`) anziché il taglio netto a fine riga.
