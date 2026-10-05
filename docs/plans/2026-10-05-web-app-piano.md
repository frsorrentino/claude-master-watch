# Web app di claude-master: piano (bozza da approvare)

Richiesta di Franz, 05/10 14:26-14:31: sul Chromebook l'app Android passa per ARC ed è lenta (ANR, 41 ms a fotogramma di
GPU). Serve una web app: la più veloce possibile sul Chromebook e usabile ovunque (Mac, PC, iPad, iPhone). «Io realizzerei
entrambe».

## L'idea: una web app sola, due strade per i dati

Due web app distinte vorrebbero dire due interfacce da tenere allineate. Meglio una web app con due trasporti, come
l'interfaccia `Transport` dell'app Android.

- **Locale (Chromebook, e qualunque PC dove gira il relay).** Il relay serve la web app e una piccola API su `localhost`:
  stato, eventi, comandi, trascrizione e file, con lo stesso JSON del contratto. Non serve Firebase né cifratura, perché
  tutto resta sulla macchina; basta un token locale. È la versione più veloce possibile: Chrome nativo, nessuna rete in
  mezzo. Installata come app di Chrome ha finestra e icona sue, e la barra del titolo personalizzata (Window Controls Overlay).
- **Remota (Mac, PC, iPad, iPhone).** La stessa web app, servita da Firebase Hosting, legge e scrive su RTDB come il
  telefono. Usa le buste AES-GCM e l'accoppiamento X25519 + HKDF con WebCrypto, e si accoppia col QR o col link di
  `pair --add`. Le notifiche push vanno via web push (su iPhone se aggiunta alla schermata Home).

La web app sceglie da sola: se `localhost` risponde usa la strada locale, altrimenti Firebase.

## Tecnologia: la decisione da prendere

1. **TypeScript + un framework web leggero (consigliata).**
   - Pro: interfaccia web vera (veloce, mouse, tastiera, finestre) e la più veloce da far bene; nessun peso Wasm.
   - Contro: le regole del core (Summary, Prossimi, quote, testi) vanno riscritte. Restano allineate perché i test
     leggono le stesse `contract/*.json`, più una serie di casi condivisi in JSON.
2. **Kotlin Multiplatform: regole del core in comune, compilate in JS, con l'interfaccia web in TypeScript.**
   - Pro: una sola copia delle regole.
   - Contro: va prima spostato il core in KMP. Oggi è una libreria Android con Room e i log di Android, ed è un lavoro
     a sé di alcuni giorni.
3. **Compose Multiplatform per il web (Wasm).** Riusa anche le schermate, ma oggi sono piene di cose solo Android
   (risorse, Activity, Toast, launcher). Il peso del Wasm e un'interfaccia che sul web non sembra web vanno contro lo
   scopo: la velocità sul Chromebook.

## Fasi

1. Relay: API locale e servizio della web app su `localhost` (richiesta a claude-master, contratto 1.35).
2. Web app, strada locale: home (riepilogo), chat della sessione con il campo, Prossimi e Ricorrenti, colonne
   affiancate come sul tablet. Fatto quando Franz la usa sul Chromebook al posto dell'app Android.
3. Strada remota: cifratura con WebCrypto, verificata sui vettori di `contract/` (`pair-add.json`, `file-parts.json`),
   accoppiamento col QR, Firebase Hosting.
4. Notifiche web push e installazione su iPhone e iPad.

## Stima

- Fase 1: un giorno lato relay.
- Fase 2: 3-5 giorni.
- Fase 3: 2-3 giorni.
- Fase 4: 1-2 giorni.

Con la tecnologia 2 si aggiungono 2-3 giorni prima di tutto.
