# Dopo la v1

Funzioni decise da Franz per dopo la chiusura della v1. Ognuna che tocca il relay è una richiesta di cambio del
contratto a claude-master, una per volta, mai durante una release del plugin.

## Suggerimento di Claude Code al polso (Franz, 14/09/2026 15:51)

Claude Code propone spesso l'input successivo, in grigio dopo `❯`. Al polso diventa un bottone accanto a «Scrivi».

- Verificato il 14/09 alle 15:3x: nella cattura tmux con `-e` il suggerimento arriva attenuato (`ESC[2m`) dopo `❯`,
  il testo scritto davvero no. Esempio dal pane di `master`: «sì, aggiungila al documento macrocode».
- Relay, contratto 1.7: ogni sessione porta `suggestion`, il testo attenuato dopo `❯`; null se la sessione lavora,
  aspetta una domanda o sul PC c'è già testo scritto.
- Orologio, Scheda: accanto a «Scrivi», allo stesso livello, un bottone non pieno con il testo intero, per esempio
  «↵ sì, aggiungila al documento macrocode». Un tocco lo invia come prompt con lo stesso comando di «Scrivi»,
  aptica di invio.
- All'invio il relay ricontrolla che il prompt sul PC sia ancora vuoto e il suggerimento lo stesso; altrimenti
  risponde con un errore invece di inviare un suggerimento vecchio.
- Poi: lo stesso bottone nella notifica di esito.

## S08: lingua di default per la beta pubblica (da decidere nel piano di S08)

Dal 14/09 l'app ha le stringhe inglesi in `values-en/` (card del README); il default resta l'italiano in `values/`.
Chi ha l'orologio in una terza lingua oggi vedrebbe l'italiano. Per una beta pubblica probabilmente conviene
l'inglese come default e l'italiano in `values-it/`: da decidere con il piano di S08, non prima.

## Progetto dedotto dai percorsi (proposta del 14/09, rimandata da Franz)

Con i percorsi assoluti del contratto 1.5 in `tool`, dire quale progetto sta toccando una sessione confrontandoli
con `projects`. Solo lato orologio, nessun cambio del contratto.
