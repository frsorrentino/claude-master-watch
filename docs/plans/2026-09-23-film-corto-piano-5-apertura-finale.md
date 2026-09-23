# Film corto, piano 5: apertura sul quadrante, frasi nuove, tre blink, slogan e finale su nero

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (i piani del corto si eseguono in linea). Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** la scaletta del corto della revisione del 23/09 sera: si apre sul quadrante con la complication, «Claude has a question.» sulla notifica, «Hear it out.» sul tocco di ▶, carrellata con tre blink (Work, Open questions, Context), tapparella che va nel nero, slogan «Claude Code, on your wrist.» su nero, cartello su nero sull'accordo finale.

**Architecture:** quasi tutto è scaletta (`timeline.short.json`) e taglio della musica; nel motore un solo campo nuovo e opzionale, `blinds.to: "black"`, che porta i listelli al nero nella seconda metà. Il volo del terminale e lo stesso orologio fra terminale e lista restano al piano 6.

**Tech Stack:** Remotion 4.0.490, React, TypeScript, `node:test`; Python per il taglio della musica (`tools/promo/audio/cut_track.py`) e per la clip del quadrante (ffmpeg).

**Spec:** `docs/plans/2026-09-23-film-corto-design.md`, sezione «2 bis. Revisione del 23/09 sera»: scene, battiti e taglio della musica sono quelli.

## Global Constraints

- Il film lungo resta identico al pixel: controllo su f150, f475, f499, f524, f850, f900, f1300, f1682, f1710, f1737, f1800, f1840 (rumore noto 2-7 px), f2300.
- Tagli e blink sui battiti interi; testo: mezzo battito a parola più 2 battiti di lettura dentro la scena; accanto all'orologio righe di al massimo 14 caratteri.
- Il colpo della musica sulla card «Deployed», lo stop mentre il «yes» riempie il quadro, l'accordo finale sul logo: stessi rapporti di adesso.
- `public/scenes/` e `public/audio/` non stanno nel repo: la clip del quadrante e la musica si rifanno coi comandi scritti qui.
- Commenti e test in italiano, commit in inglese, `git add` solo per nome.

## Review Focus

1. Il primo fotogramma: il quadrante, niente testo, niente nero.
2. Il tocco su ▶ al più 5 battiti dopo la notifica, e il dito sul «yes» dopo la voce.
3. I tre blink a 48, 50 e 52, ognuno con la sua scheda a sinistra leggibile.
4. Dalla tapparella al nero senza lampi chiari; lo slogan si legge; il logo nasce sull'accordo al 64.
5. Il film lungo non cambia (la tapparella nera è solo del corto).

---

### Task 1: apertura sul quadrante e frasi nuove

**Files:** `tools/promo/remotion/public/scenes/n_face.mp4` (nuova, fuori dal repo), `src/film/timeline.short.json`, `src/film/short.test.ts`.

- [ ] **Step 1: la clip.** `ffmpeg -fflags +genpts -i ../out/clips3/n_face_to_tile.mp4 -t 2.8 -vsync cfr -r 30 -c:v libx264 -crf 12 -g 15 -keyint_min 15 -sc_threshold 0 -pix_fmt yuv420p public/scenes/n_face.mp4` (da `tools/promo/remotion`); controllare a occhio il primo e l'ultimo fotogramma: quadrante, nessuno scorrimento verso la tile.
- [ ] **Step 2: i test.** Al posto dei test su «wake»: la prima scena è `face` (0–1,5, `n_face.mp4`, senza testo); `asks` 1,5–5,5 con «Claude has» / «a question.»; `speaks` 5,5–12 con «Hear it out.», il tocco a 6 (non più di 5 battiti dopo la notifica), la voce a 6,5; la camera senza scatti fra face, asks e speaks; nessuna scena `wake`. Il colpo a 20 sulla card «Deployed», lo stop a 18,25 dentro la crescita del «yes»; corsia 19–33 con i suoi tempi relativi al colpo; il dito preme dopo la voce. Durata 73,5 battiti.
- [ ] **Step 3:** i test falliscono.
- [ ] **Step 4: la scaletta.** Scene `face`, `asks`, `speaks` come in «2 bis»; `answer` a 12, `loop` a 19, `watch` a 33, `list` a 43,5, `work` a 48 (contenuti invariati).
- [ ] **Step 5:** `npm run check` verde (con i test di Task 2 e 3 ancora da scrivere, la durata la chiude il Task 3).
- [ ] **Step 6:** commit `feat(promo): the short opens on the watch face and the notification — «Claude has a question.», «Hear it out.»`.

### Task 2: tre blink, con Open questions

**Files:** `src/film/timeline.short.json`, `src/film/short.test.ts`.

- [ ] **Step 1: i test.** La carrellata: lista 43,5–48, Work 48–50, Open questions 50–52, Context 52–58; blink a 48, 50, 52 (stesso passo); la scena `questions` ha la scheda `questions` del film lungo (n, note, quote uguali) con `fadeIn` 0,2 e `draw` 0,5, la Panoramica che scorre (`clipStart` 8,25: la schermata arriva a 8,5 s nel primo mezzo battito) e `out: "blink"`.
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** scena `questions` nella scaletta; `context` a 52.
- [ ] **Step 4:** verde; commit `feat(promo): a third blink in the short's sweep — Open questions between Work and Context`.

### Task 3: la tapparella nel nero, lo slogan, il cartello su nero

**Files:** `src/film/timeline.ts` (`blinds.to?: "black"` e validazione), `src/film/ui/blinds.ts` (+ test), `src/film/ui/Blinds.tsx`, `src/film/timeline.short.json`, `src/film/short.test.ts`, `src/film/timeline.test.ts`.

- [ ] **Step 1: i test.** `blinds.test.ts`: una funzione pura del colore dei listelli nella seconda metà, che senza `to` è quella di sempre e con `to: "black"` arriva al nero quando i listelli finiscono di voltarsi. `timeline.test.ts`: `to` diverso da "black" è un errore. `short.test.ts`: la scena `slogan` 58–64 senza orologio, atto `close`, «Claude Code,» / «on your wrist.»; il cartello 64–73,5 su nero (tavolozza `close` nera, niente `endTone`); la tapparella del Context va nel nero; l'accordo finale (battuta 44) cade al 64, il finale piano (43) sotto lo slogan, silenzio quando arriva il nome (68); durata 73,5.
- [ ] **Step 2:** falliscono.
- [ ] **Step 3:** il motore (`blinds.to`) e la scaletta.
- [ ] **Step 4:** verde; film lungo sui 13 fotogrammi identico; commit `feat(promo): the short ends on black — the blinds turn to black, the slogan, then the logo on the final chord`.

### Task 4: la musica

**Files:** `tools/promo/remotion/public/audio/music.short.wav` (fuori dal repo).

- [ ] **Step 1:** misurare la giunta 10 → 40 con lo stesso criterio della 10 → 42 (coda della 10 contro coda della 39, testa della 40 contro testa della 11); se sotto 0,9, provare altre tre battute che finiscono con la 42.
- [ ] **Step 2:** `python3 cut_track.py $M $M.card.json 0-1 0-11 40-45 --fadein=3 --out=../remotion/public/audio/music.short.wav` (da `tools/promo/audio`), poi `track_card.py`: 68 battiti; forte dal 20 al 60, piano 60-64, accordo al 64, silenzio dal 68.

### Task 5: la bozza 13

- [ ] **Step 1:** `gl3d.py` sulla scaletta del corto, resa a segmenti (egl, swangle per il 3D), audio in swangle, montaggio in `tools/promo/out/review/corto-bozza-13.mp4`.
- [ ] **Step 2:** controlli: durata 40,1 s, audio = video; volume a battiti (colpo al 20, accordo al 64, silenzio dal 68); fotogrammi del quadrante, della notifica, dei tre blink, della tapparella nel nero, dello slogan e del logo.
- [ ] **Step 3:** consegna a Franz con l'avanzamento aggiornato.
