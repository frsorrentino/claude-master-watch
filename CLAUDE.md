# claude-master-watch

App nativa Wear OS per claude-master: le sessioni Claude Code del PC sul Pixel Watch 5
(45 mm, Wear OS 7). Progetto personale di Franz, account `personale`.

## Leggi prima di tutto

1. `docs/plans/2026-09-12-app-polso-design.md` — il design approvato sezione per sezione
   il 12/09/2026. Non si ridiscute: si esegue. Se qualcosa non torna, dillo in una frase e
   proponi, non cambiare in silenzio.
2. `contract/README.md` e `contract/*.json` — il contratto con il PC. I test Kotlin leggono
   questi file identici; il Python del relay li produce identici.
3. `~/Desktop/workspaces/personali/watchface/` — repo gemello per la catena di build
   (Gradle, JDK 17, SDK 35/36, GitHub Actions con keystore da secret, `adb` wireless).
   Copia il pattern, non i quadranti.

## Cosa esiste già sulla macchina

- JDK 17 in `/usr/bin/java`, `adb` in `/usr/bin/adb`, SDK in `~/android-sdk` e `~/Android`
  (`platforms;android-35`, `android-36`). Niente Android Studio: build da riga di comando.
- VM da 6 GB: `org.gradle.jvmargs=-Xmx2048m` in `gradle.properties`, un solo daemon Gradle,
  `kotlin.daemon.jvmargs=-Xmx1024m`. Se la build locale non passa per memoria, la costruisce
  GitHub Actions come per il watchface.
- Emulatore Wear OS presente (`adb devices` lo mostra `emulator-5554`, spesso offline: avviarlo).
- Il relay lato PC (`cm-relay.py`) vive nel plugin `~/Desktop/workspaces/personali/claude-master/`
  e lo sviluppa la sessione `claude-master`. Parlale con `SendMessage` (nome `claude-master`)
  o `claude-master talk claude-master "…"`: contratto, pairing, fixture. Mai modificare quel repo da qui.

## Stack fisso (dal design, sezione «Decisioni fisse»)

Kotlin · Wear Compose Material 3 (`TransformingLazyColumn`, `ScreenScaffold`, `TimeText`) ·
ProtoLayout Material 3 per la tile · `ComplicationDataSourceService` · Room · FCM ·
Firebase RTDB via REST/stream dietro l'interfaccia `Transport` · AES-256-GCM sul blob,
chiave nel Keystore · `OngoingActivity` · TTS di sistema con tasto ▶ · niente microfono in-app.
Niente Flutter, niente React Native, niente Horologist se non serve davvero.

## Come si lavora qui

- Superpowers: `writing-plans` sul design per il piano delle fasi 2-5, poi
  `executing-plans` / `subagent-driven-development`; `test-driven-development` su ogni pezzo;
  `verification-before-completion` prima di dire «fatto».
- Fasi e «fatto» verificabile: design, sezione 6. Non si passa alla fase dopo senza il «fatto»
  della precedente provato dal vivo o in emulatore.
- Test: unit sul contratto con `contract/*.json`; screenshot test (Paparazzi) delle schermate
  principali; una checklist dal vivo per fase in `docs/verifiche/`.
- Commit in inglese, tipo `feat(watch): …`, `feat(tile): …`, `test: …`, `docs: …`. Mai `git add`
  di cartella o `-A`: solo i file per nome. Mai keystore, `google-services.json`, chiavi o token nel
  repo (`.gitignore` li esclude; ricontrolla prima di ogni commit).
- Testi visibili all'utente in italiano, in `res/values/strings.xml` (mai cablati nel Kotlin);
  `values-en/` quando c'è tempo. Icone di stato ❓ ▶ ✓ ✗ come icone, stessi significati di Telegram.
- Ogni turno chiude con una riga «Esito: …» (cosa è cambiato o deciso). Se serve Franz al polso
  o al telefono (pairing, prova dal vivo, screenshot), dillo in una riga e fermati lì: non inventare
  esiti di prove non fatte.
- Screenshot e foto del polso: `docs/screenshots/<fase>-<schermata>.png` (i `.jpg` sono ignorati).

## Regole di larghezza e forma (Franz, 12/09)

Una riga logica occupa la riga fisica; mai colonne di frammenti; mai «…» nel corpo dei testi;
un solo bottone pieno per schermata; liste che si deformano allo scroll come le app Google
(`TransformingLazyColumn` con `SurfaceTransformation`); il tasto ▶ per la lettura vocale accanto
a ogni testo sopra `tts.min_chars` (120) o di tipo esito/risposta/domanda.
