# Richiesta al relay (contratto 1.35): la web app locale servita dal relay

Da claude-master-watch, decisione di Franz del 05/10 (14:31 «realizzerei entrambe», 14:36 «la tecnologia con l'esperienza
d'uso migliore»). Piano: `docs/plans/2026-10-05-web-app-piano.md`. Sul Chromebook l'app Android passa per ARC ed è lenta;
la web app nel Chrome nativo, servita dal relay sulla stessa macchina, è la versione più veloce possibile.

## Cosa serve dal relay

1. **Un server HTTP su `127.0.0.1`.** La porta si legge dalla config, con una proposta di default (per esempio 8765). Si
   accende con `relay serve` o sta dentro il relay già in servizio, a scelta di claude-master.
2. **Un token locale.** Lo genera il relay e lo stampa `claude-master relay web`, che apre anche
   `http://127.0.0.1:<porta>/?t=<token>` in Chrome. Ogni chiamata porta `Authorization: Bearer <token>`. L'origine è solo
   localhost, CORS chiuso.
3. **Le API, con lo stesso JSON del contratto e in chiaro** (niente buste {v, enc}: non escono dalla macchina):
   - `GET /api/state`: lo stato, come `/state`;
   - `GET /api/events?since=<ts>`: gli eventi;
   - uno stream, SSE `GET /api/stream`, che manda lo stato a ogni cambio (al posto dello stream RTDB);
   - `POST /api/cmd` con un `Cmd`: risponde con il `CmdResult` quando c'è, le stesse op del contratto;
   - `GET /api/file/<id>`: il file chiesto con `file`, in chiaro e intero (niente pezzi, niente tetto di 25 MB in locale);
   - `POST /api/share/<id>`: un'immagine o un file da allegare (`report`), in chiaro.
4. **I file statici della web app**, serviti dalla stessa porta su `/`, da una cartella che l'app compila (path nella config).

## Contratto

- Versione 1.35, solo aggiunte, il JSON è identico. Fixture: `local-api.json` con un esempio per endpoint (richiesta e
  risposta), così i test della web app e del relay leggono lo stesso file.
- Telefono, tablet e orologio non cambiano.
