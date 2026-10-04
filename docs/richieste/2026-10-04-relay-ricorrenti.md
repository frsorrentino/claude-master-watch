# Richiesta al relay (contratto 1.33): le azioni ricorrenti della master

Mandata a claude-master il 04/10 sera, dopo la release 0.6.5. Idea di Franz alle 20:24, ok alle 20:32 («Ok modifiche
prossimi e ricorrenti»).

## Cosa vuole Franz

Nella chat della master, sopra il campo, un box «Ricorrenti» chiuso di default: le azioni fatte più volte, come l'analisi
dei post su X, il changelog delle nuove release, la concorrenza dei plugin. Si apre, si sceglie un'azione: il tocco la
mette nel campo (se c'è già testo si accoda con «e poi»), ↗ la manda subito alla master. Stessi gesti del box «Prossimi».

La lista sta sul PC e non nell'app: lì c'è lo storico (registro dei compiti, recap), anche delle cose lanciate dal
terminale, e una lista sola vale per telefono, tablet e Chromebook.

## Cosa serve dal plugin e dal relay

1. **La lista sul PC**, tenuta dalla master. Dove e in che formato si salva lo decide claude-master; proposta: un comando
   `claude-master recurring add|remove|list` che la master usa.
2. **Nello stato**: `state.recurring: [{id, label, prompt, param}]`, già nell'ordine da mostrare (l'ultima usata in cima),
   al massimo 8. Senza lista il campo manca, e l'app non mostra il box.
   - `id`: stabile, `[a-z0-9-]`.
   - `label`: al massimo 40 caratteri, quello che l'app mostra nella riga.
   - `prompt`: il testo che va nel campo o parte.
   - `param`: true se il prompt aspetta un pezzo da aggiungere (un link, un numero di release). L'app lo mette nel campo
     col cursore in fondo e non offre l'invio diretto. Facoltativo, false se manca.
3. **Il riempimento.** La prima volta la master propone a Franz, dal suo storico, le azioni fatte almeno 3 volte, col prompt
   che ha funzionato; Franz approva. Poi la master propone un'aggiunta quando nota una ripetizione, o Franz dice «aggiungi
   ai ricorrenti».
4. **L'ordine.** Quando la master esegue un'azione della lista, da qualunque parte arrivi, la porta in cima.

## Contratto

- Versione 1.33, solo aggiunte. Fixture: `state-*.json` con `recurring` di 3 voci con dati demo (repo pubblico), una con
  `param: true`.
- L'invio usa la strada di oggi, un prompt alla master: nessuna op nuova.
