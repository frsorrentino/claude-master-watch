# Richiesta al relay (contratto 1.32): accoppiamento a zero tocchi sul Chromebook e dispositivi accoppiati nello stato

Mandata a claude-master alle 17:12 del 04/10, dopo la release 0.6.4. Scelta di Franz alle 15:48 del 04/10: «ok c», cioè zero tocchi; la
parte dell'app è già pronta su `feature/telefono` (`4c6bfc2`).

## Cosa fa l'app (già fatto)

- `cmwatch://pair?q=<base64url della riga di relay pair --text>`, padding facoltativo, apre l'invito. Senza `q` il link
  resta «Apri sul telefono» dall'orologio.
- L'app accetta solo un QR di claude-master valido e chiede **sempre** conferma: «Accoppiare questo dispositivo a
  <host>?». Dice anche se gli altri dispositivi restano (`m: "add"`) o si scollegano. Senza il tocco non succede niente:
  il link è esportato e apribile da una pagina web.

## Cosa serve dal relay

1. **C, zero tocchi.** Con `relay pair --add`, e anche con `pair_add` dal telefono, se nel PATH c'è un `adb` che vede
   l'Android della stessa macchina (ARC del Chromebook, `emulator-5554`), il relay apre l'invito:
   `adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d 'cmwatch://pair?q=<base64url>' com.francescosorrentino.cmaster`.
   - Timeout brevi: oggi `adb shell` sull'ARC si è bloccato più volte e si è sbloccato solo con `adb -s emulator-5554 reconnect`.
   - L'`adb` giusto è `/usr/bin/adb`: quello in `~/android-sdk/platform-tools` è per un'altra architettura (Exec format error).
2. **B, ripiego.** Se adb non risponde, la riga va negli appunti di ChromeOS, che Linux e Android condividono. In Crostini
   non ci sono né `wl-copy` né `xclip`: il meccanismo lo sceglie il relay. L'app, con la finestra «Incolla il codice»,
   potrà riempirla da sola: è un passo successivo, da fare quando gli appunti funzionano.
3. **A, testi.** Con `--add`, o quando il dispositivo non è un orologio, la stampa non deve dire «scrivilo
   sull'orologio» ma cosa fare nell'app: il QR o la riga di `--text`. Il numero a 6 cifre serve solo all'orologio.

## Contratto

- Versione 1.32, solo aggiunte. Fixture proposta: `pair-link.json` con `line` (la riga di `pair-add.json`, compatta) e
  `uri` (lo stesso invito in base64url senza padding), così l'app verifica la decodifica sullo stesso vettore del relay.
- Nessun cambio a `/pair`, `/allowed` o alla consegna della chiave: da lì in poi è la 1.30.

## Parte 2: i dispositivi accoppiati nello stato (Franz, 04/10 14:51 e 17:11)

Lo schema dei collegamenti nelle Impostazioni deve mostrare i dispositivi veri: oggi il telefono vede telefono e
orologio, il tablet vede un telefono che non c'è e niente orologio. Franz ha scelto la variante B: il PC sopra, i
dispositivi in fila sotto, ognuno col suo filo e il puntino dell'ultima lettura.

- `state.devices`: `[{uid, name, kind, seen}]`, uno per ogni uid di `/allowed` (devices.json), nell'ordine di
  accoppiamento. `kind` = «phone», «watch», «tablet», «chromebook» o null se non si sa. `seen` = epoch s dell'ultimo
  `/seen/<uid>` (oggi in ms), null se non è mai arrivato.
- Da dove viene `kind`: dalla 1.32 il dispositivo che si accoppia lo scrive nella sua risposta in `/pair/<nodo>/watch`
  (`kind`, e `kinds` = uid → kind per gli uid che porta con sé, come l'orologio del telefono). Per gli accoppiamenti di
  prima il relay lo deduce dal nome se può («watch» → watch, «Tablet» → tablet), se no null: l'app mostra un'icona
  generica.
- Fixture: i tre stati con `devices` (telefono, orologio, tablet, Chromebook; uno mai visto), e `pair-add.json` con `kind`.
