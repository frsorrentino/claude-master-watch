# Telefono: la master come casa dell'app (design, 01/10/2026)

Decisioni di Franz della sera del 01/10 (dal telefono, via la master):

- 21:19: proposte approvate, e servono proposte per dare alla master «un valore e una centralità diversi»;
- 21:31: layout 1, «master come casa», ma senza perdere le informazioni della Panoramica;
- 21:37: il Diario entra «in modo selezionato e ragionato», senza appesantire;
- 21:47: lo stile resta quello attuale, solo «più tech».

Mockup: https://claude.ai/artifact/L673nZRfAVVgnZgXZpKpUh (privato dell'account personale; le immagini sono state
mandate a Franz in chat, non stanno nel repo pubblico perché mostrano i nomi veri delle sessioni).

## Navigazione

- In basso: **Master · Sessioni · Diario**. La Panoramica come scheda sparisce: i suoi dati stanno nel Quadro (sotto).
- L'app si apre sulla Master (oggi: sulla Panoramica, `StartRoute`).
- Lo scorrimento laterale passa fra le tre schede come oggi; tirare giù aggiorna lo stato dal PC.
- Il menu ≡ apre anche il Quadro completo, le Impostazioni e la ricerca, come oggi.
- La master è la sessione che si chiama `master` (`ContextActions.master`). Senza master aperta la scheda mostra il
  Quadro e «Per te», con al posto della chat «master non attiva» e il tasto per lanciarla.

## Schermata Master, dall'alto

1. **Testata:** badge e nome della master con il suo stato; sotto, pillole di modello ed effort e l'anello del contesto
   (toccabile: il foglio del contesto).
2. **Per te** (sotto).
3. **Quadro compatto** (sotto).
4. **Chat della master:** la stessa della sua scheda sessione (trascrizione, passaggi, file, riga dal vivo), con la
   barra «Scrivi alla master» sempre in fondo.

Scorrendo la chat verso l'alto, «Per te» e il Quadro si chiudono in una riga sottile (stato della master e due mini
anelli); tornando in cima si riaprono.

## Per te

Una lista di righe, ognuna con un testo di una riga, una seconda riga piccola e un tasto. Al massimo tre righe; le
altre in «+N», che apre la lista intera in un foglio. Ordine per urgenza:

| Tipo | Quando | Tasto |
|---|---|---|
| Domanda | una sessione aspetta una risposta | Rispondi (apre la coda) |
| Contesto | una sessione, master compresa, dall'80 % | Handoff e riavvio |
| Resoconto della notte | al mattino, finché non è ascoltato o letto (al più fino alle 12) | Ascolta |
| Notte | dalle 20, con la notte attiva sul PC | Aggiungi |
| Prossimo passo | dal recap di oggi, solo per progetti con la sessione ferma o chiusa | Avvia |
| Invii programmati | ci sono messaggi in attesa della ripartenza | Vedi |

- Una riga sparisce quando l'azione è fatta o il contenuto letto; il resoconto della notte letto non torna.
- Il tasto pieno è uno solo: quello della prima riga. Le altre hanno il tasto tonale.
- «Avvia» manda il prossimo passo alla sessione del progetto se è ferma, o lancia una sessione nel progetto se è chiusa.
- Tutto si calcola nell'app con dati che ha già (stato, eventi, recap, invii programmati): nessuna richiesta al relay.

## Quadro

- **Compatto:** etichetta «Quadro» con il led e «aggiornato ora · PC»; due mini anelli (5 ore, con la settimana nella
  riga sotto, ora della ripartenza o «dato vecchio»); i chip delle sessioni con il loro stato, e da 75 % la percentuale
  del contesto sul chip. La master non è fra i chip. Tocco = Quadro completo.
- **Completo:** un foglio dal basso con la Panoramica di oggi senza cambiamenti (`OverviewScreen`: quote con il ritmo,
  Adesso, Domande, Contesto, Oggi, Notte, aggiornato).

Ogni dato della Panoramica ha un posto:

| Panoramica di oggi | Dove va |
|---|---|
| Quota per account, settimana, ripartenza | Quadro compatto; il ritmo nel foglio |
| Adesso | chip delle sessioni; conteggi e barra nel foglio |
| Domande | Per te |
| Contesto | pillola della master, chip da 75 %, Per te da 80 %, elenco nel foglio |
| Oggi, Notte | foglio; la notte resta anche nel Diario |
| Invii programmati | Per te |
| Aggiornato | testata del Quadro |

Il Diario resta la sua scheda e l'archivio: recap completo, giorni passati, avvisi di quota, gestione della coda della
notte. In Per te compaiono solo i rimandi del momento (tabella sopra), come chiede la regola «un dato, una casa» del 30/09.

## Stile: l'attuale, più tech

Stessi caratteri, colori, card piene, pillole e fumetti di oggi, con quattro tocchi (Franz, 01/10 23:02: approvata):

1. **Cifre monospaziate** per numeri e orari: percentuali (contesto, quote), orari, «aggiornato ora · PC», durate. Il
   monospazio di sistema con cifre a larghezza fissa, stessa dimensione e colore di oggi; il resto del testo non cambia.
2. **Etichette di sezione** («PER TE», «QUADRO»): 11 sp, maiuscolo, 1,5 sp fra le lettere, semigrassetto; ambra per
   Per te, verde per Quadro. Oggi sono titoli in grassetto da 13 sp.
3. **Un filo di bordo** sulle card: 1 dp, bianco al 7 %; angoli da 20 a 18 dp.
4. **Un led** accanto a «aggiornato ora»: pallino verde da 7 dp con un alone, che pulsa (opacità dal 100 al 35 % e
   ritorno in 1,6 s); con un dato vecchio è ambra e fermo. Sui chip delle sessioni al lavoro la barretta ha lo stesso
   alone. Fermo solo nei provini, come l'asterisco della riga dal vivo.

L'anello del contesto nella pillola è già disegnato nell'app (`ContextRing`): il simbolo ◔ era solo nel mockup.

Si applica alla Master e, per coerenza, alle altre schermate dove ci sono gli stessi elementi.

## Regole in core (TDD)

- `MasterHome.forYou(state, events, sent, now, zone)`: le righe di Per te con tipo, testi e azione, ordinate e
  tagliate a tre più il conto delle altre.
- `MasterHome.strip(state, now, stale)`: il Quadro compatto (anelli, chip, contesto da mostrare, aggiornato).
- Riuso: `PhoneOverview.build` per il foglio, `ContextActions` per la master e il contesto, `PhoneDiary` per il
  resoconto e il recap.

## App

- `MasterScreen` (nuova) in `mobile/.../ui/`: testata, Per te, Quadro compatto, chat della master con la barra.
- `AppShell` e `StartRoute`: le tre schede nuove; la Master come partenza.
- Lo stile più tech in `ui-tokens` e nei componenti condivisi.

## Test

- Unit su `MasterHome`: ogni tipo di riga, l'ordine, il taglio a tre, la sparizione dopo l'azione, la mattina e la sera.
- Provini Paparazzi: Master di sera, Master di mattina, Quadro completo, master non attiva.
- Dal vivo, checklist in `docs/verifiche/`: apertura sulla Master, Per te con le azioni vere, Quadro, chat, scorrimenti.

## Fuori

- «La master propone»: proposte scritte dalla master e mandate dal relay (serve un campo nel contratto).
- Più suggerimenti generati dal PC.
- Il widget Master ha la sua specifica: `2026-10-01-telefono-widget-master-design.md`.
