# Tablet: piano della notte del 04/10/2026

Approvato da Franz dal telefono: mockup «va bene, approvato» (00:03) e «va bene A» (00:10), cioè lo sviluppo lo fa
questa sessione di notte, dopo un riavvio con handoff. Compito nel registro: `tablet-notte-a` (approvato: commit e push
su `feature/tablet`, CI con provini e APK, installazione su Chromebook e Pixel).

## Design approvato

Mockup in `docs/mockup/2026-10-04-tablet/`: `mockup-tablet.html` (le sezioni a grandezza vera, file autonomo),
`plancia.dc.html` e `colonne.dc.html` (sorgenti del canvas), le sezioni in WebP, `dc2png.py` + `vals.js` (rendono un
`.dc.html` in PNG con Chromium headless). Canvas: https://claude.ai/artifact/HDFsoDrkLD9wcaEfqN24Tr.

- Solo schermi larghi (larghezza ≥ 840 dp: tablet orizzontale, finestra del Chromebook). Il telefono non cambia; il
  tablet in verticale (< 840 dp) resta l'app del telefono allargata.
- **Plancia** (vista di partenza):
  - riga di stato in alto, una riga sola: PC e aggiornamento, quote 5h e 7g per account con la ripartenza, sessioni per
    stato, notte, orologio, ora;
  - barra di navigazione a sinistra (plancia, colonne, registro, cerca, + lancia, impostazioni);
  - colonna delle sessioni densa (spia di stato, nome, età, barra del contesto, riga di attività) e in fondo il pannello
    quote: **quota 5h con la previsione al ritmo attuale** fino alla ripartenza (linea piena fino a ora, tratteggiata
    dopo, soglie 80 e 100) e la settimana;
  - al centro la conversazione della sessione scelta (la `SessionSheet` di oggi);
  - a destra l'ispettore: progetto, account, aperta da, turno; contesto, passaggi e commit di oggi; obiettivo;
    **cronologia di oggi** dal contratto 1.29 (prompt, test verdi o rossi, commit, esiti, compiti).
- **Colonne**: da 1 a 4 sessioni in colonne verticali affiancate, ognuna con conversazione e campo suo; la sessione che
  aspetta ha il bordo arancio. Barra delle sessioni a sinistra **fissa o richiudibile** (preferenza ricordata); le
  sessioni in colonna si scelgono dalla barra o dalle pillole in alto.
- Stile: i token di oggi (`CmColors`), monospazio per i numeri, griglia di puntini, niente card ripetute in griglia.

## I pezzi, in ordine

Ogni pezzo: test prima (regole nel core), poi il codice, `./gradlew -q :core:testDebugUnitTest
:mobile:compileReleaseKotlin :mobile:compileDebugUnitTestKotlin`, commit per nome, push su `feature/tablet`, CI con
`record=true`, provini guardati uno per uno, provini committati. Un provino tablet nuovo per ogni vista (Paparazzi con
una `DeviceConfig` da 1440×900 dp in orizzontale).

1. **Interruttore**: larghezza ≥ 840 dp → `TabletShell`, altrimenti l'app di oggi. Niente librerie nuove (la UI
   condivisa e Material3 adaptive aspettano AGP 9.1): basta la larghezza della finestra.
2. **Plancia, struttura**: riga di stato (regola nel core per conteggi e testi), barra di navigazione, colonna delle
   sessioni (gruppi di `Summary`), conversazione al centro (`SessionSheet` della sessione scelta), ispettore con i dati
   che lo stato ha già.
3. **Cronologia** nell'ispettore: richiesta `timeline` (contratto 1.29, `ContractJson.decodeTimeline` esiste già) per la
   sessione scelta, come lettura passiva (`Repo`, come `search`); commit di oggi contati dalla cronologia; demo in
   `FakeTransport` già pronta.
4. **Quote**: previsione 5h da `QuotaHistory.pace` (esiste) disegnata in Compose; settimana: se lo storico per giorno non
   c'è, mostrare il totale e la previsione al rinnovo, e dirlo a Franz. Il grafico del contesto nella giornata richiede
   campioni che oggi non si salvano: farlo solo se costa poco (campioni locali come `QuotaHistory`), se no rimandarlo.
5. **Colonne**: sessioni fissate e scelta fissa/richiudibile nelle preferenze; 1–4 colonne con `SessionSheet` compatta.
6. **Chromebook**: Invio manda, Maiusc+Invio a capo; Ctrl+K cerca se resta tempo.

## Al mattino

- APK installato sul Chromebook (`emulator-5554`, Android ARC: provare in Demo, finestra ridimensionabile) e sul Pixel.
- A Franz: un file HTML autonomo con i provini tablet della notte (immagini incorporate in WebP, codifica dichiarata,
  sotto 1 MB, controllato a 412 px), cosa è fatto, cosa no e perché.
- Non si chiude il ramo `feature/telefono`: la checklist dal vivo e il merge restano per il giorno.
