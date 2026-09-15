# Grafica e animazioni: proposte (15/09/2026)

Richiesta di Franz (15/09 23:28): solo aspetti grafici e di dettaglio, animazioni e piccole migliorie, partendo da quello
che offre l'ultima Wear OS. Fatto: giro su tutte le schermate (snapshot Paparazzi del 15/09 e scatti dal polso), censimento
dei componenti usati, controllo delle API nelle librerie in cache (Wear Compose 1.6.2, ProtoLayout 1.4.2, Tiles 1.6.2).

## Cosa abbiamo oggi

- Liste: `TransformingLazyColumn` con `morph` (deformazione ai bordi) su tutte le schermate. Bene, è il cuore di M3 Expressive.
- Animazioni presenti: il badge che pulsa (`SessionBadge`, `rememberInfiniteTransition`), gli archi della quota (`Gauge`,
  `tween`), una `AnimatedVisibility` nella tile. Nient'altro: cambi di stato, arrivi di righe, invii e caricamenti
  scattano senza transizione.
- Componenti M3 con forme animate disponibili ma non usate: `IconButton`/`IconToggleButton` (`animatedShapes`). I bottoni
  larghi (`Button`, `FilledTonalButton`) non hanno forme animate nella 1.6.2: lì il movimento va dato in altro modo.
- Wear OS 7 (il sistema del Pixel Watch 5): le Ongoing Activity sono sostituite dalle **Live Updates**; nuovi **Wear
  Widgets** (Glance + RemoteCompose) accanto alle tile; Wear Compose 1.6 aggiunge lo snapping e il layout rovesciato alla
  `TransformingLazyColumn`.

Dettagli visti negli scatti: il nome della sessione va a capo a metà («ledger- / api», «atlas-sh / op») accanto a ▶; il
grigio secondario `text2` (#9AA3B2) è poco contrastato anche fuori dal Terminale (età, righe della Timeline, «Chiedo al PC»).

## Proposte, in ordine di valore per costo

Regole per tutte: rispettano `LocalReduceMotion` (con «riduci animazioni» attivo restano solo i cambi netti), si fermano
in ambient, durate brevi (150-300 ms) e molle morbide del motion scheme M3, niente animazioni in loop tranne il badge che
già c'è.

### A. Piccole, subito visibili

1. **▶ che diventa ■ con la forma che si trasforma.** `SpeakButton` passa a `IconToggleButton` con
   `IconToggleButtonDefaults.animatedShapes()`: premuto si schiaccia, in lettura diventa un quadrato arrotondato con ■.
   È l'esempio stesso di M3 Expressive (play/pausa). Tutte le schermate con ▶.
2. **Il nome su una riga.** Nella testata il nome si rimpicciolisce a scalini (14 → 11 sp, `TextAutoSize.StepBased`) prima
   di andare a capo; se non basta, va a capo solo al trattino. Domanda, Terminale, Scheda.
3. **Le righe nuove del Terminale entrano, non compaiono.** I blocchi arrivati con l'aggiornamento dal vivo entrano con
   dissolvenza e allungamento (`animateItem` sulla `TransformingLazyColumn`); l'ora nel divisore cambia con una
   dissolvenza sulle cifre (`AnimatedContent`).
4. **Il paragrafo letto si accende piano.** Il fondo del paragrafo in lettura passa con `animateColorAsState` invece di
   scattare; la lista scorre con la molla del motion scheme.
5. **Le sessioni si riordinano scorrendo.** Quando una sessione sale in cima perché ha una domanda, o scende perché ha
   finito, la card scivola al suo posto (`animateItem`) invece di saltare. L'icona di stato (▶ ✓ ❓ ✗) passa dall'una
   all'altra con dissolvenza e scala (`AnimatedContent`).
6. **Conferma animata dopo un invio.** Risposta a una domanda, prompt da «Scrivi», Riprendi: `ConfirmationDialog` di
   successo (spunta che si disegna, 1,5 s) o di fallimento, oltre alla vibrazione che c'è già.
7. **Contrasto del grigio secondario.** Portare `text2` da #9AA3B2 a un grigio più chiaro in tutta l'app (audit
   dei punti in cui è usato), come fatto oggi nel Terminale.

### B. Medie

8. **Caricamenti con la sagoma del contenuto.** Al posto di «Chiedo al PC», righe segnaposto con il luccichio
   (`Modifier.placeholder` + `placeholderShimmer`) nella forma della risposta e del terminale.
9. **Quota che si riempie all'apertura.** Gli archi partono da zero e arrivano al valore con una molla; dato vecchio =
   arco che sbiadisce. Da decidere insieme alla revisione della pagina Quota già in lista (card «Aggiornato»).
10. **Stato accanto all'ora.** In `TimeText`, testo curvo dopo l'ora quando qualcosa aspetta: «❓ 1» in ambra. Si vede da
    ogni schermata dell'app senza tornare alla lista.
11. **Tile che si accende.** ProtoLayout 1.4: dissolvenza in entrata quando la tile diventa visibile (helper di fade della
    `Transformation`), barre della quota che si riempiono con `DynamicFloat` animato (già in lista dal 15/09), il badge della
    sessione in attesa che pulsa una volta.
12. **Stretch in fondo alla lista.** L'effetto overscroll della `TransformingLazyColumn` (parametro `overscrollEffect`),
    come nelle app Google, al posto dell'arresto secco.

### C. Più grandi, da valutare a parte

13. **Live Updates al posto dell'Ongoing Activity** per la sessione seguita (Wear OS 7): stato in tempo reale nel
    quadrante e nel launcher, con l'API che Google usa ora.
14. **Wear Widget** (Glance + RemoteCompose) accanto alla tile: card piccola e grande, animazioni più ricche e meno
    batteria. È una seconda superficie, non un ritocco.
15. **Colore dinamico dal quadrante** solo per l'accento (non per gli stati, che devono restare gli stessi di Telegram).
16. **Scorrimento a scatti** nel Terminale e nella Risposta (snapping della `TransformingLazyColumn` 1.6): un paragrafo
    per volta al centro.

## Come si verifica

Ogni proposta approvata: snapshot Paparazzi prima e dopo (in CI), prova al polso con «riduci animazioni» acceso e spento,
riga nella checklist `docs/verifiche/`. Le animazioni si giudicano solo sul polso: gli snapshot mostrano lo stato finale.
