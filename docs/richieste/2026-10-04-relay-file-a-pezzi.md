# Richiesta al relay (contratto 1.34): i file della chat a pezzi, senza il limite di 0,8 MB

Da mandare a claude-master dopo la release 0.6.6. Segnalazione di Franz alle 21:55 del 04/10, dal tablet: un HTML di 3,6 MB
mandato dalla master non si apre nell'app, che mostra «too large: 3630371». Franz chiede di correggere alla radice.
Analisi della causa della master (21:57).

## Oggi (contratto 1.24)

- `file_open` (cm-relay.py, righe 1321-1358) mette tutto il file in un solo nodo, `/file/<id>` = `{v, enc}`; in chiaro
  `{mime, data}` con `data` in base64, quindi il file passa due volte per base64 (circa 1,78 volte la sua misura).
- Il limite è `S.SHARE_MAX_BYTES` = 1.500.000 caratteri di `enc` (cm-relay-state.py:60), circa 840 KB di file. Oltre, le
  immagini si riducono in JPEG e il resto si rifiuta con «too large: <byte>».
- RTDB regge stringhe fino a 10 MB: il limite è nostro.

## Cosa serve

1. **Il file a pezzi.** Se il comando `file` porta `"parts": true` (lo manda solo l'app nuova), il relay scrive:
   - `/file/<id>/meta` = `{v, enc}`, in chiaro `{n, size, sha256, mime, name}`;
   - `/file/<id>/parts/<k>` = `{v, enc}` per k da 0 a n-1, in chiaro i byte grezzi del pezzo, circa 1 MB l'uno, senza
     JSON né base64 interni (una sola base64, quella di `enc`).
   L'app legge `meta`, scarica i pezzi in ordine su un file temporaneo, controlla `sha256` e cancella `/file/<id>`. Tetto
   complessivo 25 MB; il TTL di 10 minuti resta.
2. **Senza `parts`** tutto come oggi: un'app vecchia continua a funzionare col limite di adesso.
3. **Il rifiuto dice il limite**: «too large: <byte> max <byte>», così l'app scrive «il file è di 30 MB, il
   trasferimento arriva a 25 MB» senza numeri scritti nell'app.

## Contratto

- Versione 1.34, solo aggiunte. Fixture proposta: `file-parts.json` con il `meta` e due pezzi di un file demo piccolo,
  cifrati con la chiave di prova delle altre fixture, e lo `sha256` del file intero.
- L'app intanto mostra il rifiuto in chiaro: la misura del file in MB invece dei byte.
