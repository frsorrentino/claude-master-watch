# Richiesta al relay: l'etichetta dei comandi della web app (contratto 1.36)

Franz, 05/10 19:47: «chiedi fix etichetta web al relay».

## Il problema

Un `prompt` o un `report` mandato dalla web app locale (`POST /api/cmd`, senza `device`, `by` = "web") arriva alla
sessione con il prefisso «Dall'utente via polso (watch)». Visto dal vivo alle 19:45 (prompt e file `prova-web.txt`
verso claude-master-phone). Per la 1.22, senza `device` la trascrizione dice `origin` = "remote", e il prefisso non
dovrebbe nominare il polso.

## Cosa chiedo

1. `device` accetta anche "web": il prefisso diventa «Dall'utente via web app» (in inglese «From the user via the web
   app»), con la stessa regola di chiusura (riga «Watch:» solo se c'è un orologio che la mostra, come per il telefono).
   In `transcript` le voci user prendono `origin` = "web"; in `timeline` il `ref` del prompt è "web".
2. Senza `device` il prefisso non dice «polso»: quello generico del telefono o uno neutro, a scelta del relay.
3. La web app manderà sempre `device` = "web" con `prompt`, `resume`, `launch` e `report`, sia dalla strada locale sia,
   più avanti, da quella remota.

Fixture: un prompt con `device` "web" in `cmd-result-sample.json` e una voce `origin` "web" nella pagina di trascrizione.
`v` resta 1.
