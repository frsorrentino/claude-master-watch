# Richiesta al relay: i browser come dispositivi (contratto 1.39)

Franz, 05/10 21:21 (piano `docs/plans/2026-10-05-web-strada-remota.md` approvato) e 21:25 («ok 8 dispositivi»): la web
app remota (Mac, PC, iPad, iPhone) si accoppia come un dispositivo in più, con `pair --add`.

1. `PAIR_MAX_DEVICES` da 4 a 8: oggi telefono, orologio, tablet e Chromebook riempiono i posti e un browser riceve «full».
2. `kind` "web" fra `DEVICE_KINDS`: la risposta di accoppiamento del browser porta `kind: "web"` e `kinds: {<uid>: "web"}`;
   oggi un tipo sconosciuto si scarta e `state.devices[].kind` resta null.
3. Op `unpair`, `arg` = uid: lo toglie da `/allowed` e da `devices.json`; rifiuti «unknown device <uid>», e per l'ultimo
   dispositivo rimasto «cannot remove the last device». Il browser lo usa da Impostazioni, «Scollega questo browser».
   `ops` comprende `unpair`.

Fixture: un dispositivo `kind` "web" negli stati, un `unpair` riuscito e uno rifiutato in `cmd-result-sample.json`.
`v` resta 1.
