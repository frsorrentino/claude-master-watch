# App del telefono, restyling: design (30/09/2026)

Approvato da Franz il 30/09: scelte alle 15:58 e 16:00, le due sezioni alle 16:25. Nasce dalla prima prova dal vivo sul
Pixel 11 Pro XL (30/09 15:49): «bug e grafica molto basilare e spartana, serve migliorarla molto». Chiede un restyling
accurato con le ultime pratiche Material You, allineato allo stile dell'orologio, con tutte le funzioni del cruscotto
che l'orologio già ha. Sostituisce la sezione «Aspetto e movimento» di `2026-09-24-app-telefono-fondamenta-design.md`
solo dove la contraddice. Funzioni e contratto restano quelli del pezzo 3 (`2026-09-29-telefono-pezzo-3-design.md`).

## Decisioni di Franz (30/09)

- **Colori:** la palette dell'orologio con Material 3 Expressive, senza colori dinamici. Telefono e orologio si
  riconoscono come la stessa app.
- **Navigazione:** tre schede in basso, «Panoramica», «Sessioni» e «Diario». L'app si apre sulla Panoramica.
- **Lingua:** si sceglie nelle impostazioni dell'app e cambia anche la voce che legge i testi.

## Linguaggio visivo

- **Colori:**
  - fondo nero e tre gradini di superficie come le card «brief» dell'orologio (`CmColors.surface`, `surfaceHigh`, e
    `briefCard` con i suoi inchiostri);
  - il corallo come identità;
  - i colori di stato dell'orologio: ambra aspetta, cobalto lavora, grigio fermo, rosso spento chiuso;
  - i colori degli account (personale, lavoro) come sul polso.
- **Forme:** angoli ampi e variati, secondo la scala di Expressive; card grandi come le card brief e bottoni a pillola.
  La forma segue lo stato: la card di chi aspetta te è più morbida e in rilievo, quella chiusa è piatta.
- **Tipografia:** gerarchia forte. Numeri grandi per quota e contatori, come sul polso; titoli più pesanti; monospazio solo
  per terminale e comandi. Gli stili di Material 3 Expressive, con le varianti enfatizzate per titoli e numeri.
- **Movimento:**
  - molle di Expressive, senza rimbalzo visibile;
  - anelli e barre che si riempiono entrando in vista, numeri che rotolano;
  - il volo card → scheda e il gesto indietro predittivo;
  - con «riduci animazioni» tutto fermo, e lo stato finale subito.
- **Componenti di Expressive:**
  - la barra di navigazione a tre schede;
  - gli indicatori ondulati di caricamento e avanzamento;
  - gruppi di bottoni per le risposte;
  - un bottone mobile con menu, «Lancia» e «Aggiungi alla notte».
- **Regole che restano:** un solo bottone pieno per schermata; mai «…» nel corpo dei testi; una riga logica su una riga
  fisica; il tasto ▶ della lettura a voce sopra `tts.min_chars` e su esiti, risposte e domande; testi in `strings.xml`
  italiano e inglese.

## Schermate e funzioni

1. **Panoramica** (nuova, schermata iniziale): il brief dell'orologio (`QuotaScreen`) in grande, con le regole già in
   `core`.
   - Per ogni account il doppio anello, finestra di 5 ore e settimana, con il ritmo della finestra (`QuotaHistory`) e
     l'ora dell'azzeramento.
   - «Adesso» a segmenti: chi aspetta, chi lavora, chi è fermo.
   - Le domande aperte: toccarle apre la scheda della sessione.
   - Il contesto per sessione, con modello ed effort (`SessionMeters`).
   - «Oggi» a colonne (`DayBars`), poi la notte, poi l'ora dell'aggiornamento.
2. **Sessioni:** card come le celle dell'orologio (`SessionsText`, `TileTexts`):
   - badge dell'account e dello stato;
   - titolo su cosa sta facendo e dettaglio su cosa segue;
   - obiettivo (`/goal`), bassa priorità, contatore del contesto;
   - le chiuse raccolte in fondo.
3. **Scheda sessione:**
   - testata con badge e contatori;
   - la domanda come sull'orologio (`QuestionScreen`): prima opzione piena, pressione lunga per il rischio alto,
     «Chat about this», «Allow all» solo quando il permesso ha opzioni;
   - «Segui», «Apri in Claude», «Terminale», «Riapri» per le chiuse, scrittura libera.
4. **Terminale:**
   - dal vivo finché è aperto, come sull'orologio (`TerminalLive`: una lettura ogni pochi secondi, ferma quando la
     schermata esce);
   - i colori per chi parla (tu, Claude, strumenti) da `TerminalText`.
5. **Diario:** come nel pezzo 3 (diario di oggi e dei giorni prima, notte con la coda, avvisi di quota), nel nuovo
   linguaggio visivo.
6. **Impostazioni:**
   - lingua dell'app (italiano, inglese, di sistema), con `LocaleManager` e la dichiarazione delle lingue
     (`localeConfig`), così compare anche nelle impostazioni di sistema; la voce che legge segue la lingua scelta;
   - voce e soglia di lettura, notifiche, Demo, accoppiamento.
7. **Condividi e Lancia:** gli stessi fogli, nel nuovo linguaggio visivo.

## Bug dal vivo del 30/09

- «Allow all» su un permesso senza opzioni: corretto in 5b572a5, per telefono e orologio. Lato relay la correzione è in
  claude-master 0.5.1.
- «Chat about this» mancava sul telefono: aggiunto in 5b572a5.
- Il terminale partiva vuoto: ora si legge all'apertura (5b572a5); con il restyling diventa dal vivo.
- L'app si apriva sulla scheda di una sessione invece che sulla regia: con il restyling si apre sempre sulla
  Panoramica. La scheda aperta sopravvive solo a una rotazione, non a un nuovo avvio.

## Test e verifiche

- **Regole nuove in `core`, con TDD:** composizione della Panoramica, lingua scelta e voce, ciclo di lettura del terminale
  dal vivo.
- **Paparazzi:** ogni schermata in Demo e con le fixture; telefono standard, carattere 1,3 e schermo piccolo. Registrati in
  CI e riguardati sui provini prima del commit.
- **Revisione visiva:** due passaggi miei sui provini, poi i provini a Franz prima della prova dal vivo.
- **Prova dal vivo:** la checklist `docs/verifiche/pezzo-3-telefono.md`, più due righe per la Panoramica e per la lingua.

## Fatto

- Test verdi e snapshot registrati.
- Provini approvati da Franz.
- La checklist dal vivo passa, compresi Panoramica e lingua.

## Fuori

Widget sulla schermata home, impostazioni sincronizzate con l'orologio, disposizione delle finestre del PC: restano nel
pezzo 4.
