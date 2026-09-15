#!/usr/bin/env python3
"""Card del README (docs/readme/card-*.svg + .png, 1200×630), stesso stile delle card del plugin claude-master
(tools-readme-cards.py nel suo repo): gradiente scuro, titolo 52 bold, orologi con gli screenshot veri di Paparazzi
sul set demo. Zero modelli, solo cairosvg. Le due pagine si rimandano: qui il plugin è il requisito.

Uso: python3 tools/readme-cards.py (dopo aver aggiornato docs/readme/*.png dagli snapshot della CI)."""
import cairosvg
import pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "docs" / "readme"
FONT = "font-family=\"'DejaVu Sans',Helvetica,Arial,sans-serif\""
MONO = "'DejaVu Sans Mono',Menlo,monospace"
GREEN, YEL, BLUE = "#3ddc84", "#ffd43b", "#5aa7ff"
INK, DIM, LINE, PANEL, CMD = "#e8eefc", "#9aa7c7", "#2f3f63", "#0b1020", "#9ad1ff"
WARN = []


def head(bg1, bg2):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1200" height="630" viewBox="0 0 1200 630" {FONT} xml:space="preserve">
  <defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{bg1}"/><stop offset="1" stop-color="{bg2}"/></linearGradient>
  <marker id="m" markerWidth="7" markerHeight="7" refX="5" refY="3.5" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L7,3.5 L0,7 z" fill="{GREEN}"/></marker></defs>
  <rect width="1200" height="630" fill="url(#bg)"/>'''


def label(x, y, text, size=22, color=INK, weight="500", mono=False, anchor="start"):
    ff = f' font-family="{MONO}"' if mono else ""
    est = len(text) * size * (0.62 if mono else (0.6 if weight in ("700", "800") else 0.56))
    right = x + est if anchor == "start" else (x + est / 2 if anchor == "middle" else x)
    if right > 1150:
        WARN.append(f"«{text[:50]}» size {size} → ~{int(right)} px")
    return f'<text x="{x}" y="{y}" fill="{color}" font-size="{size}" font-weight="{weight}"{ff} text-anchor="{anchor}">{text}</text>'


def title(t, sub):
    return label(70, 100, t, 52, "#ffffff", "800") + label(70, 146, sub, 26, "#b9c7e8")


def panel(x, y, w, h, stroke=LINE, fill=PANEL):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="{fill}" stroke="{stroke}"/>'


def foot(text):
    return label(70, 592, text, 20, DIM)


def watch_shot(cx, cy, r, png, caption=None):
    """Screenshot tondo vero (Paparazzi, 456×456) in una cassa disegnata: niente foto di un dispositivo commerciale."""
    assert (OUT / png).is_file(), png
    cid = f"clip{cx}{cy}"
    o = f'<circle cx="{cx}" cy="{cy}" r="{r + 18}" fill="#1b1f2a" stroke="#3a4152" stroke-width="3"/>'
    o += f'<rect x="{cx + r + 14}" y="{cy - 22}" width="12" height="44" rx="5" fill="#3a4152"/>'
    o += f'<clipPath id="{cid}"><circle cx="{cx}" cy="{cy}" r="{r}"/></clipPath>'
    o += f'<image x="{cx - r}" y="{cy - r}" width="{2 * r}" height="{2 * r}" xlink:href="{png}" clip-path="url(#{cid})"/>'
    if caption:
        o += label(cx, cy + r + 46, caption, 16, DIM, anchor="middle")
    return o


arrow = f'stroke="{GREEN}" stroke-width="3" marker-end="url(#m)"'
cards = {}

# ---------------------------------------------------------------- hero: l'app e il plugin che le serve
s = head("#0f2a2a", "#08161a") + title("claude-master, on your wrist.", "The Wear OS app of the claude-master plugin")
s += panel(70, 196, 500, 336, "#2f6b55", "#0b1a1a")
s += label(96, 234, "1  on your PC: the plugin", 20, "#b9e8d0", "700")
s += label(96, 266, "$ claude plugin marketplace add \\", 15, CMD, "700", True)
s += label(96, 288, "    frsorrentino/claude-master", 15, CMD, "700", True)
s += label(96, 310, "$ claude plugin install \\", 15, CMD, "700", True)
s += label(96, 332, "    claude-master@claude-master-dev", 15, CMD, "700", True)
s += label(96, 378, "2  your Firebase: the relay", 20, "#b9e8d0", "700")
s += label(96, 410, "$ claude-master relay pair", 15, CMD, "700", True)
s += label(96, 456, "3  on the watch: the 6-digit code", 20, "#b9e8d0", "700")
s += label(96, 490, "then every session, on the wrist", 17, DIM)
s += watch_shot(679, 350, 74, "sessions.png", "the sessions") + watch_shot(884, 350, 74, "question.png", "a question") + watch_shot(1089, 350, 74, "card.png", "a session's card")
s += foot("no Wear OS watch? claude-master works without the app: Telegram, any phone")
cards["card-hero"] = s + "</svg>"

# ---------------------------------------------------------------- come si collega
s = head("#151a33", "#0a0e1c") + title("Your PC, your Firebase, your wrist.", "End to end encrypted: Firebase only stores blobs")
cols = [("your PC", "#ff8c42", ["claude-master plugin", "cm-relay.py:", "  push the state", "  run the commands", "  from the watch"]),
        ("your Firebase", YEL, ["Realtime Database", "+ Cloud Messaging", "AES-256-GCM blobs,", "rules: only paired", "devices read"]),
        ("your watch", GREEN, ["this app", "key in the Keystore", "paired over X25519", "with a 6-digit code", "from the PC"])]
for i, (h, col, lines) in enumerate(cols):
    x = 70 + i * 370
    s += panel(x, 200, 320, 280, col) + label(x + 26, 242, h, 24, col, "800")
    for j, t in enumerate(lines):
        s += label(x + 26, 284 + j * 32, t, 19)
    if i < 2:
        s += f'<line x1="{x + 326}" y1="320" x2="{x + 362}" y2="320" {arrow}/>'
        s += f'<line x1="{x + 362}" y1="360" x2="{x + 326}" y2="360" {arrow}/>'
s += label(70, 530, "state, events and outcomes go up; answers, prompts and launches come back", 19, INK)
s += foot("no server of ours in between: bring your own Firebase project")
cards["card-bus"] = s + "</svg>"

for name, svg in cards.items():
    (OUT / f"{name}.svg").write_text(svg)
    cairosvg.svg2png(bytestring=svg.encode(), write_to=str(OUT / f"{name}.png"), output_width=1200, url=str(OUT / f"{name}.svg"), unsafe=True)
print("ok", len(cards))
if WARN:
    print("SFORANO:"); print("\n".join(WARN))
