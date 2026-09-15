# Prompt per generare una icona migliore (ChatGPT, Gemini)

Da incollare così com'è in ChatGPT (GPT Image) o in Gemini. È in inglese perché i modelli di immagini lavorano meglio
così. Sotto, in coda, le note per valutare le proposte. L'icona attuale è `docs/icona/icona-L1-centrata.svg`
(disco corallo `#CC785C`, raggiera nera, cartellino nero con `</>`), resa in `wear/src/main/res/drawable/ic_launcher_fg.xml`.

---

## Prompt

I need a new app icon for a **Wear OS smartwatch app**. Give me several distinct concepts, then the final artwork as
**SVG** (and a 1024×1024 PNG preview of each concept).

**The app.** `claude-master-watch` — an open-source Wear OS companion for a desktop tool. Source and screenshots:
https://github.com/frsorrentino/claude-master-watch (the README shows every screen). It is the wrist client of a
terminal plugin, https://github.com/frsorrentino/claude-master , that runs many parallel AI coding sessions (Claude
Code) on one computer, each in its own terminal tab. On the watch you see every session, which one is waiting for an
answer, and you answer it from your wrist with one tap or by voice; you also read the session's terminal output, its
outcome, and your usage quota. The feeling to convey: **many parallel workers on your machine, one wrist that keeps
them moving** — calm control, not alarm; a developer tool, not a toy.

**What the icon must work as, technically:**
- Android **adaptive icon**: a 108×108 dp square viewport; only the **central circle of 72 dp** is guaranteed visible
  (Wear OS masks icons to a circle), and the safest content area is a circle of about 66 dp. Deliver two layers:
  a flat **background** (solid colour, no detail) and a **foreground** drawing.
- A **monochrome** version of the foreground (single colour, solid shapes, no strokes thinner than ~3 dp at 108 dp)
  for themed icons.
- Legible at **48 dp** in the app launcher list and at **24 dp** as a small glyph; it must still read at 1/4 scale.
- **No text, no letters, no words** (the current one has `</>` — I am open to dropping it).
- Flat vector, Material 3 Expressive style: bold geometric shapes, generous negative space, at most 2–3 colours,
  no photorealism, no bevels, no drop shadows, no long gradients, no thin outlines, no tiny details.
- It sits on **black watch backgrounds** and inside a white or black launcher circle: it must hold up on both.

**Palette to use (do not invent new hues):**
- coral `#CC785C` — the brand colour of the app today (it is also the colour of Claude's sparkle);
- pastel blue `#D3E3FD` and navy `#0A2050` — the pair already used for buttons and the tile icon;
- black `#000000` and off-white `#F2F4F7`.
Use coral as the identity colour. Blue is the "action" colour of the app, so use it sparingly, as an accent.

**Do not** reproduce or imitate the Anthropic or Claude logo, any Google or Wear OS logo, or any trademark. The icon
must be original artwork. Avoid the obvious clichés: a robot head, a chat bubble, a wrench, a generic terminal window
with a blinking cursor, a brain, a rocket.

**Directions worth exploring** (one concept each, plus any idea of your own):
1. **Many into one**: several short strokes or shapes converging into a single dot or ring — the parallel sessions
   and the one wrist that answers.
2. **The wrist that answers**: a ring (the watch bezel) broken by one filled arc or a single dot that stands out —
   one session needs you, the others are fine.
3. **Radial sessions**: a compact starburst of unequal rays, each ray a session, one ray longer or a different
   colour — related to the current icon but simpler and stronger.
4. **A card on a dial**: one rounded card shape placed on a circle, cropped by it — the session card on the watch.

**For each concept give me:** a one-sentence rationale, the 1024×1024 PNG preview, and, for the one you consider best,
the final **SVG** with the foreground on a transparent background, sized to a 108×108 viewport with the artwork inside
the central 66 dp circle, plus the background colour as a flat fill, plus a one-colour monochrome variant. Also show
me the icon rendered at 48 px and 24 px on both a black and a white circle so I can judge legibility.

---

## Come valuto le proposte (note per me, non per il modello)

- Si riconosce a 24 px? Se a quella misura diventa una macchia, è fuori.
- Regge in monocromatico? La raggiera attuale regge; un disegno a linee sottili no.
- Dice «sessioni parallele» senza spiegare? L'icona di oggi dice «Claude» più «codice», non dice il parallelismo.
- Resta diversa dal quadrante e dalle icone di sistema quando è nell'elenco app, dove sta accanto a tondi bianchi e blu.
- Il corallo resta il colore dell'identità: se una proposta funziona solo in blu, non è la nostra.
