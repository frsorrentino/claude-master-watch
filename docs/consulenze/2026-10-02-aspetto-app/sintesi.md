# Consulenza sull'aspetto dell'app: sintesi (02/10/2026)

Richiesta di Franz alle 23:17: un parere di Codex e di Antigravity su aspetto, ottimizzazioni e funzionalità, dagli screen
delle schermate. Mandati il brief (`brief.md`) e 22 provini Paparazzi (17 telefono, 5 orologio, dati della Demo: nessun
dato di clienti). Codex 0.154.0 con le immagini allegate; Antigravity (agy, gemini-3.7-flash, effort high, chiave del
free tier di fable-director: solo materiale non sensibile, come da decisione del 23/09). Risposte integrali in
`risposta-codex.md` e `risposta-antigravity.md`.

## Rilievi che sono effetti dei provini, non dell'app

- **t03 «contenuto»**: il provino della shell usa un segnaposto al posto della lista; la lista vera è in `sessionsQuestion`.
- **Nome della sessione assente nella chat** (Codex, punto 1 dei cinque): i provini di `SessionSheet` non includono la barra
  in alto, dove il nome c'è («claude-master-phone ▾»). Resta vero che la testata della chat non lo ripete.
- **Fondi grigi in t02 e t12**: area vuota del provino e foglio modale senza la schermata dietro.
- **Tile Wear «Ti aspettano»** (Antigravity, funzionalità 5): tile e complicazioni esistono già.
- **Frasi preimpostate** (Antigravity, funzionalità 3): c'erano e Franz le ha tolte il 01/10 23:34; ora le sostituiscono i
  consigli `Prossimi:` scritti dalla sessione.

## Dove concordano (priorità alta)

1. **Icone di stato uniche ovunque.** L'orologio e il badge usano ancora `?`, ▶ verde per «al lavoro», la spunta; nel
   Registro ▶ apre e chiude la notte. Proposta: mano, bandierina, fulmine, pausa in ogni badge (telefono e polso), ▶
   solo per la lettura vocale, chevron per aprire e chiudere.
2. **Una sola scheda domanda** per t02, t06, t13 e o03: oggi la prima opzione è ambra in «Per te» e azzurra nella chat;
   le secondarie cambiano stile. Un componente condiviso, un solo bottone pieno, le altre delineate.
3. **Chi ti aspetta prima dell'ultimo esito** nella casa della master quando c'è una domanda, e in cima al Quadro.
4. **Nomi e lingua coerenti**: personale/lavoro (oggi anche personal, work, professionale); sul polso «Overview»,
   «Sessions», «Thu», «Terminal» da tradurre; «da» per le durate, «alle» per gli orari, «aggiornato» per la freschezza.
5. **t05, azioni dei messaggi**: copia e modifica sotto ogni messaggio fanno rumore. Nota: la pressione lunga ora seleziona
   il testo, quindi andrebbero in un menu del messaggio, non sulla pressione lunga.
6. **t12 Lancia**: le forme dell'account sembrano caselle da spuntare (riempirle); il tasto Lancia spento non dice che
   manca il progetto («Scegli un progetto»).
7. **t16 Condividi**: miniatura dell'immagine e sessione scelta evidenziata («Manda a ledger-api»).
8. **t17 Cerca**: numero di risultati, tasto per svuotare, parola cercata evidenziata.

## Rilievi di uno solo, che condivido

- **Orologio, bordi tondi** (Antigravity): in o03 la seconda opzione è sotto la curva, in o05 l'ultima riga tocca il
  bordo; più spazio in fondo e il ▶ meno ingombrante in o03.
- **Stato del collegamento** (entrambi, in forme diverse): un segno discreto «PC raggiungibile · ultimo contatto» nella casa.
- **t09 Stop** (Codex): dopo il tocco «Interruzione richiesta» finché il PC non conferma.
- **t08 tabelle**: numeri allineati a destra nella griglia; etichette delle schede più chiare (contrasto).
- **t14 terminale**: margine sinistro e un segno «dal vivo» accanto al titolo.

## Funzionalità proposte

| Proposta | Da | Parere |
|---|---|---|
| «Da quando sei uscito»: solo esiti, blocchi ed errori nuovi, con ▶ | Codex | Utile e coerente con «Per te»: un riepilogo in testa alla casa dopo un'assenza. |
| Sollecito delle domande ferme, raggruppato | Codex | Utile; le notifiche ci sono, manca il sollecito. |
| «Avvisami quando…» (finita, test conclusi, quota ripristinata) | Codex | Utile, piccolo: da «Segui» a condizioni. |
| Dal polso al telefono sullo stesso punto | Codex | Utile; serve un collegamento orologio→telefono. |
| Coda dei messaggi modificabile prima della presa in carico | Codex | Utile ma richiede il relay. |
| «Fai da solo 30 min» per letture e test | Antigravity | Tocca i permessi delle sessioni: da valutare con prudenza. |
| Notifiche critiche oltre il «Non disturbare» per domande notturne | Antigravity | Utile per la notte; canale dedicato. |
| Diff sintetico dei file modificati prima di rispondere | Antigravity | Molto utile per le domande di commit e deploy; richiede il relay. |
| Coda notturna riordinabile e condizionata | Antigravity | Utile più avanti. |

## Vincolo che entrambi chiedono di rivedere

Il taglio senza puntini: per domande e opzioni ritorno a capo completo (Codex); per percorsi tecnici troncamento al centro
(Antigravity). Oggi domande e opzioni vanno già a capo; resta il caso dei percorsi.

## Proposta di ordine

1. Icone di stato uniche (telefono e polso) e ▶ solo per la voce.
2. Scheda domanda condivisa (t02, t06, t13, o03).
3. Lingua e nomi coerenti (polso compreso).
4. Chi ti aspetta prima dell'ultimo esito.
5. Ritocchi puntuali: Lancia, Condividi, Cerca, orologio ai bordi.
Poi le funzionalità, a partire da «Da quando sei uscito» e dagli avvisi a condizione.
