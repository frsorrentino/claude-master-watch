# Telefono: widget Master (design, 01/10/2026)

Richiesta di Franz (01/10 18:20): un widget che metta al centro la master, con il suo ultimo esito, un campo per
scriverle e la lista delle sessioni aperte. Si aggiunge al widget che c'è già (account, sessione, regia), non lo
sostituisce. Mockup: https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz

## Cosa mostra

- **Testata:** stato della master, nome «Master», ora dell'ultimo stato dal PC, ↻ per chiederlo di nuovo.
- **Ultimo esito della master:** il testo a tutta larghezza, mai tagliato con «…»; sotto «ultimo esito · HH:MM» e il
  tasto Ascolta (altoparlante), che lo legge con il TTS di sistema come nella chat.
- **Barra «Scrivi alla master»:** in un widget Android non esiste un campo di testo (RemoteViews/Glance non hanno
  input). La barra è un tocco che apre un foglio dal basso, trasparente sopra la home, con il campo già a fuoco e la
  tastiera aperta. Invio = `PhonePrimary.Target.PROMPT` alla master, stesso percorso della scheda sessione (stato
  ottimistico, coda, ricevuta). Niente microfono nell'app (decisioni fisse): si detta con la tastiera.
- **Sessioni aperte** (solo 4×4): una riga per sessione con stato, nome, cosa sta facendo (`tool_note`, o la domanda,
  o l'esito breve) ed età dello stato. Ordine: prima chi ti aspetta, poi chi lavora, poi le ferme, poi la notte; la
  master non è nella lista. Le righe che non stanno lasciano il posto a «Tutte le sessioni» (apre la Panoramica).

## Taglie

- **4×2:** testata, esito breve (`outcome.short`), barra.
- **4×4:** testata, esito intero (`outcome.full`, nei limiti di altezza: se non sta, l'esito breve), barra, lista.
- Le misure seguono `WidgetLayout` (porting di ads-widget), con la font scale del telefono.

## Stato di una sessione

Franz non vuole le icone ❓ ▶ ✓ ✗ (01/10 18:25). Tre varianti nel mockup, da scegliere:

- **A, punto e parola:** punto colorato e «lavora», «ti aspetta», «ferma», «notte»; il punto di chi lavora sale e
  scende piano come l'asterisco della chat.
- **B, icone a tratto Lucide:** battito, campanella, doppia spunta, luna; stessa famiglia delle `ic_w_*`.
- **C, solo colore:** barretta a sinistra della riga, fondo ambra per chi ti aspetta.

Colori dai token: `waiting` per ti aspetta, `idle` per ferma, `widgetAccent` per lavora, un viola tenue per la notte.

## Tocchi

- Testata o esito: la scheda della master.
- Una riga: la scheda di quella sessione.
- ↻: chiede lo stato al PC, come il widget attuale.
- Ascolta: un broadcast all'app, che legge con `Speech` senza aprire nulla.
- Barra: il foglio di scrittura.

## Quale sessione è la master

Alla posa si sceglie la sessione; il valore proposto è quella che si chiama `master`. Se la scelta non c'è più nello
stato, il widget lo dice («master non attiva») e la barra resta, ma inattiva.

## Dati e aggiornamento

- Nessun cambio di contratto: servono `state`, `since`, `outcome`, `question`, `tool_note`, `suggestion` di `Session`,
  già nello stato.
- Si aggiorna a ogni stato che arriva (push), come il widget attuale; stato in Glance (`PreferencesGlanceStateDefinition`).

## Tecnica

- Regole in `core`, con TDD: `MasterWidgetModel` (scelta della master, testo dell'esito per taglia, righe ordinate,
  testo della riga, quante righe stanno).
- App: `MasterWidget` (Glance) e `MasterWidgetReceiver` in `mobile/.../widget/`; `WriteToMasterActivity` con tema
  trasparente e foglio dal basso in Compose; configurazione alla posa come `WidgetConfigActivity`.
- Anteprime prima di Franz: `WidgetRenderReceiver` sull'Android del Chromebook (memoria `render-before-handing-over`).

## Test

- Unit su `MasterWidgetModel`: master trovata, sparita, ordine delle righe, esito breve o intero, righe che non stanno.
- Rendering delle due taglie con `WidgetRenderReceiver`, confrontato con il mockup.
- Dal vivo: posa, scrittura alla master, tocchi, Ascolta.

## Fuori

- Risposta alle domande della master dal widget (resta nella scheda).
- Frasi rapide nel widget.
