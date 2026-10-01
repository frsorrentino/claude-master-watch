# Telefono: widget Master (design, 01/10/2026)

Richiesta di Franz (01/10 18:20): un widget che metta al centro la master, con il suo ultimo messaggio, un campo
per scriverle e le sessioni aperte. Si aggiunge al widget che c'è già (account, sessione, regia), non lo
sostituisce. Mockup: https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz

## Cosa mostra

- **Testata:** stato della master, nome «Master», ora dell'ultimo stato dal PC, ↻ per chiederlo di nuovo.
- **Ultimo messaggio della master** (Franz, 01/10 18:29): il suo ultimo messaggio per intero, a tutta larghezza, mai
  tagliato con «…»; sotto «ultimo messaggio · HH:MM» e il tasto Ascolta (altoparlante), che lo legge con il TTS di
  sistema come nella chat. Scartati l'esito breve con il prossimo passo e il recap della sessione: la barra sotto serve
  a rispondere, quindi conta quello che la master ha appena detto. Se la master ti fa una domanda, la domanda («Ti
  chiede») prende il posto del messaggio e la barra diventa «Rispondi alla master».
- **Barra «Scrivi alla master»:** in un widget Android non esiste un campo di testo (RemoteViews/Glance non hanno
  input). La barra è un tocco che apre un foglio dal basso, trasparente sopra la home, con il campo già a fuoco e la
  tastiera aperta. Invio = `PhonePrimary.Target.PROMPT` alla master, stesso percorso della scheda sessione (stato
  ottimistico, coda, ricevuta). Niente microfono nell'app (decisioni fisse): si detta con la tastiera.
- **Sessioni aperte come chip** (solo 4×4; Franz, 01/10 18:29: la lista a righe intere non va): un chip per sessione
  con stato e nome, su una o due righe; prima chi ti aspetta (fondo ambra), poi chi lavora, poi le ferme, poi la notte.
  La master non c'è. Glance non ha un `FlowRow`: le righe di chip si calcolano in `core` dalla larghezza del widget e
  dalla lunghezza dei nomi; i chip che non stanno diventano un chip «+N» che apre la Panoramica.

## Taglie

- **4×2:** testata, ultimo messaggio (o domanda), barra.
- **4×4:** come il 4×2, più le righe di chip. Se il messaggio non sta nell'altezza, il widget mostra il suo inizio
  fino all'ultima frase intera che sta, e il tocco apre la scheda della master.
- Le misure seguono `WidgetLayout` (porting di ads-widget), con la font scale del telefono.

## Stato di una sessione

Franz non vuole le icone ❓ ▶ ✓ ✗ (01/10 18:25). Tre varianti nel mockup, da scegliere:

- **A, punto e parola:** punto colorato e «lavora», «ti aspetta», «ferma», «notte»; il punto di chi lavora sale e
  scende piano come l'asterisco della chat.
- **B, icone a tratto Lucide:** battito, campanella, doppia spunta, luna; stessa famiglia delle `ic_w_*`.
- **C, solo colore:** barretta sul bordo sinistro del chip, fondo ambra per chi ti aspetta.

Nella testata lo stato della master; nei chip lo stato di ogni sessione (in A il punto senza parola). Colori dai
token: `waiting` per ti aspetta, `idle` per ferma, `widgetAccent` per lavora, un viola tenue per la notte.

## Tocchi

- Testata o messaggio: la scheda della master.
- Un chip: la scheda di quella sessione; «+N»: la Panoramica.
- ↻: chiede lo stato al PC, come il widget attuale.
- Ascolta: un broadcast all'app, che legge con `Speech` senza aprire nulla.
- Barra: il foglio di scrittura.

## Quale sessione è la master

Alla posa si sceglie la sessione; il valore proposto è quella che si chiama `master`. Se la scelta non c'è più nello
stato, il widget lo dice («master non attiva») e la barra resta, ma inattiva.

## Dati e aggiornamento

- Nessun cambio di contratto: servono `state`, `since`, `outcome.full`, `question` di `Session`, già nello stato.
- Si aggiorna a ogni stato che arriva (push), come il widget attuale; stato in Glance (`PreferencesGlanceStateDefinition`).

## Tecnica

- Regole in `core`, con TDD: `MasterWidgetModel` (scelta della master, messaggio o domanda, chip ordinati e disposti
  in righe, taglio sull'ultima frase intera).
- App: `MasterWidget` (Glance) e `MasterWidgetReceiver` in `mobile/.../widget/`; `WriteToMasterActivity` con tema
  trasparente e foglio dal basso in Compose; configurazione alla posa come `WidgetConfigActivity`.
- Anteprime prima di Franz: `WidgetRenderReceiver` sull'Android del Chromebook (memoria `render-before-handing-over`).

## Test

- Unit su `MasterWidgetModel`: master trovata, sparita, domanda al posto del messaggio, ordine dei chip, righe di chip
  per larghezza, «+N», taglio del messaggio sull'ultima frase intera.
- Rendering delle due taglie con `WidgetRenderReceiver`, confrontato con il mockup.
- Dal vivo: posa, scrittura alla master, tocchi, Ascolta.

## Fuori

- I tasti delle scelte di una domanda (si risponde scrivendo nel foglio; le scelte restano nella scheda).
- Frasi rapide nel widget.
