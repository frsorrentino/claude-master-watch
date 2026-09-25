# Corto «forma unica» — lista di lavoro

Franz, 25/09 22:33: «prosegui in autonomia fino a realizzazione video completa, in concerto con fable advisor e master
per le decisioni e revisioni di qualità». Specifica: `2026-09-25-corto-forma-unica-design.md`; piano 1:
`2026-09-25-corto-forma-unica-piano-1-motore.md`. Regole fisse: rese complete una alla volta (notte del 25/09: le lancio da sola,
master 22:47); niente Gradle durante una resa; niente push senza ok di Franz via master;
prima/dopo in `tools/promo/remotion/out/review/`, consegna a Franz con SendUserFile. Test con Node 24
(`PATH=~/.local/share/fnm/node-versions/v24.13.1/installation/bin:$PATH`). Cwd della sessione alla radice del repo.

## Passo 1 — motore
- [x] Task 1-3 (esecutore Opus): `020ea71`, `75f59a4`, `be93b25`; 214 test verdi, tsc 0, guardia Short identica.
- [x] Revisione avversaria del motore (workflow `wf_fb84b805-0db`): 9 difetti, corretti in `ef0d536`, regola `ecbed4e`.

## Passo 2 — tavola della sola forma
- [x] Traccia `shape` in `timeline.short.json` (card1 a 19, end misurato dalla resa vera): `a9e226c`, pannelli a sinistra.
- [x] Test sulla traccia: forma sempre presente; spostamenti > 4 px solo in finestra di chiave; contenuti mai sovrapposti;
      §8.5 nessun battito morto (molla attiva, scambio, o contenuto che si anima da sé); 52 e 56 su battito 1 di battuta.
- [x] Resa `ShapeBoard` 0,25× + `battiti.py` (`out/review/forma.battiti.png`); master: passo 2 verificato.

## Passo 3 — tratto finale list → end (47,5-73,5)
- [x] Motore (piano 2, Task 1-5: `28d55ee`..`3d5c1aa`), contenuti e traccia (`ShapeContent.tsx`, `98b1585`), via palpebre.
- [x] Tavola `out/review/finale-tavola.battiti.png`; resa `dopo-finale.mp4`; prima/dopo `confronto-finale.mp4`.

## Passo 4 — testa face → answer (0-19)
- [x] Piano 3 (`2026-09-25-corto-forma-unica-piano-3-testa-centro.md`): `show`, `inShape`, contenuti (`471f63a`..`6d1917b`); notifica come copia del display, voce, tasti (molla ζ 0,7), «yes» con anello, campo.
- [x] Tavola intera `out/review/corto-tavola.battiti.png`; resa nel corto finale.

## Passo 5 — centro loop e watch (19-47,5)
- [x] Corsia come cambi di bersaglio (schede, dettatura, schermo dell'invio, ✓ che si apre sul terminale); scritte della corsia sopra la forma; takeover via, volo del terminale tenuto (è già continuo, §2).
- [x] Tavola, resa nel corto finale.

## Effetti (Franz, 25/09 22:58: «spettacolari ma coerenti»; scelta con l'advisor)
Criterio: ogni effetto rafforza «una forma sola» e rispetta §3/§8 (niente per lettera, niente glow sul testo, niente
rimbalzo, blur solo sulla forma). Ognuno con un interruttore nella composizione, spento finché la sua tavola per battito
non lo approva: il confronto resta pulito e una versione di riserva si rende sempre.
- [ ] (non costruito stanotte) Contenitore (container transform di Material) nei due momenti a tutto quadro: «yes» 17,5-19 e dettatura 34,5-37.
      La forma è una finestra sulla scena dopo, congelata sul suo primo fotogramma finché non tocca a lei. Passi 4-5.
- [x] (accesa) Onda del tocco dentro la forma, dal punto del dito, su ogni chiave con `gesture` (il feedback vero di Wear OS).
      Test: ogni chiave con gesto ha la sua onda sul battito. Dopo il passo 3.
- [x] (accesa, spenta nell'atto close: sul nero era un alone dietro lo slogan) Luce d'ambiente: l'alone del `Backdrop` prende colore e posizione della forma, a bassa intensità (contro «tutto
      è una pillola», §4). Dopo il passo 3.
- [x] (accesa) Ombra di distacco legata al peso dell'aggancio: zero agganciata, piena libera (i distacchi al 2 e al 52). Sulla
      scatola, dentro il blur. Dopo il passo 3.
- [x] Suono: solo gli accenti al posto delle palpebre (`accent` per chiave: scatto a 52/54/56, soffio al 58), con test che nessun altro suono cambia. Gesti e picchi di velocità: non fatti.
- Interruttori: `effects: { shadow, ripple, light }` nei defaultProps di `Short` (`src/Root.tsx`).
- Scartati: squash & stretch (si legge come rimbalzo), liquido che si divide sui tasti (template), lente o
  `backdrop-filter` (costosa e fragile in headless), qualunque effetto sul testo (§3).
- Ora limite per la resa completa finale: 04:00, con gli effetti approvati fino a quel momento (1203 fotogrammi, circa
  90 minuti). Franz deve trovare un corto intero.

## Chiusura
- [ ] Resa completa del corto (via di master), prima/dopo intero, consegna a Franz.
- [ ] Push (ok di Franz via master), Paparazzi da CI, `tools/promo` in repo proprio (§8.9) dopo la resa finale.

## Aperti annotati
- Identità col display in face/end: vera per costruzione finché `display()` è a riposo; il test vero arriva con la posa reale (passi 3-4).
- Ordine di sovrapposizione: la forma sopra le scene copre titolo e orologio dove i rect si toccano (terminale); serve
  una proprietà per chiave che la metta sotto il primo piano (passo 3/5).
- Proposte dell'esecutore, non fatte: `engines`/`.nvmrc` per Node ≥ 22.6; `eslint.config.mjs` col config di Remotion.
