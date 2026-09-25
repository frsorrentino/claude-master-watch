# Corto «forma unica» — lista di lavoro

Franz, 25/09 22:33: «prosegui in autonomia fino a realizzazione video completa, in concerto con fable advisor e master
per le decisioni e revisioni di qualità». Specifica: `2026-09-25-corto-forma-unica-design.md`; piano 1:
`2026-09-25-corto-forma-unica-piano-1-motore.md`. Regole fisse: rese complete una alla volta, solo dopo il via di master
(«render pronto» con fotogrammi e stima); niente Gradle durante una resa; niente push senza ok di Franz via master;
prima/dopo in `tools/promo/remotion/out/review/`, consegna a Franz con SendUserFile. Test con Node 24
(`PATH=~/.local/share/fnm/node-versions/v24.13.1/installation/bin:$PATH`). Cwd della sessione alla radice del repo.

## Passo 1 — motore
- [x] Task 1-3 (esecutore Opus): `020ea71`, `75f59a4`, `be93b25`; 214 test verdi, tsc 0, guardia Short identica.
- [ ] Revisione avversaria del motore (workflow `wf_fb84b805-0db`), correzioni dei difetti confermati.

## Passo 2 — tavola della sola forma
- [ ] Traccia `shape` in `timeline.short.json` (card1 a 19, end misurato dalla resa vera).
- [ ] Test sulla traccia: forma sempre presente; spostamenti > 4 px solo in finestra di chiave; contenuti mai sovrapposti;
      §8.5 nessun battito morto (molla attiva, scambio, o contenuto che si anima da sé); 52 e 56 su battito 1 di battuta.
- [ ] Resa `ShapeBoard` 0,25× + `battiti.py`; revisione advisor + master; copia a Franz con la lista di ciò che non mostra.

## Passo 3 — tratto finale list → end (47,5-73,5)
- [ ] Contenuti veri (card ✓, pannelli Work/Questions/Context con barre dal bordo), linea dello slogan, anello del logo
      (`ring`), aggancio al display di tre quarti (geometria vera), forma sotto il primo piano dove serve.
- [ ] Via palpebre e neri del tratto; tavola per battito; «render pronto» a master; resa del tratto; prima/dopo.

## Passo 4 — testa face → answer (0-19)
- [ ] Notifica come copia del display (omografia), voce, tasti, `bump()` → molla ζ 0,7, takeover → chiave; 18-19 vivo.
- [ ] Tavola, via di master, resa, prima/dopo.

## Passo 5 — centro loop e watch (19-47,5)
- [ ] Corsia come cambi di bersaglio, dettatura, ✓ che si apre, finestra del terminale; via takeIn/takeover.
- [ ] Tavola, via di master, resa, prima/dopo.

## Chiusura
- [ ] Resa completa del corto (via di master), prima/dopo intero, consegna a Franz.
- [ ] Push (ok di Franz via master), Paparazzi da CI, `tools/promo` in repo proprio (§8.9) dopo la resa finale.

## Aperti annotati
- Identità col display in face/end: vera per costruzione finché `display()` è a riposo; il test vero arriva con la posa reale (passi 3-4).
- Ordine di sovrapposizione: la forma sopra le scene copre titolo e orologio dove i rect si toccano (terminale); serve
  una proprietà per chiave che la metta sotto il primo piano (passo 3/5).
- Proposte dell'esecutore, non fatte: `engines`/`.nvmrc` per Node ≥ 22.6; `eslint.config.mjs` col config di Remotion.
