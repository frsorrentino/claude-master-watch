"""Immagini della scheda Play (26/09/2026): icona 512, grafica 1024×500 in due varianti ciascuna, screenshot.
Nessuna build: gli screenshot vengono dagli snapshot Paparazzi già nel repository (dati della Demo e delle fixture).
  python3 docs/play/gen_store_assets.py
Il segno è la stessa geometria di docs/icona/gen_icona.py (icona «>_ nel quadrante», variante A5)."""
import base64, io, math
from pathlib import Path

import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "docs/play"
WEAR = ROOT / "wear/src/test/snapshots/images"
PHONE = ROOT / "mobile/src/test/snapshots/images"

CORAL, TRACK = "#D97757", "#3A404C"
BG, SURFACE, SURFACE_HIGH = "#000000", "#23272E", "#292F3A"
TEXT, TEXT2, PRIMARY = "#F2F4F7", "#B0B8C4", "#D3E3FD"


def mark(cx, cy, r_out, fg=CORAL, track=TRACK):
    """Il segno in SVG: quadrante aperto in basso, riempito al 70 %, «>» e «_» dentro."""
    u = r_out / 43; g = 0.72 * u; w = 7.2 * g; yc = cy - 2 * u; r = 40.2 * u; aw = 5.6 * u

    def pt(deg):
        return cx + r * math.cos(math.radians(deg)), cy + r * math.sin(math.radians(deg))

    def arc(a0, a1, color):
        (x0, y0), (x1, y1) = pt(a0), pt(a1)
        large = 1 if (a1 - a0) > 180 else 0
        return (f'<path d="M{x0:.2f},{y0:.2f} A{r:.2f},{r:.2f} 0 {large} 1 {x1:.2f},{y1:.2f}" fill="none" '
                f'stroke="{color}" stroke-width="{aw:.2f}" stroke-linecap="round"/>')

    out = [arc(130, 410, track)] if track else []
    out.append(arc(130, 130 + 280 * 0.7, fg))
    out.append(f'<path d="M{cx - 22 * g:.2f},{yc - 17 * g:.2f} L{cx - 3 * g:.2f},{yc:.2f} L{cx - 22 * g:.2f},{yc + 17 * g:.2f}" '
               f'fill="none" stroke="{fg}" stroke-width="{w:.2f}" stroke-linecap="round" stroke-linejoin="round"/>')
    y = yc + 13.4 * g + w / 2
    out.append(f'<path d="M{cx + 2 * g + w / 2:.2f},{y:.2f} L{cx + 23 * g - w / 2:.2f},{y:.2f}" fill="none" '
               f'stroke="{fg}" stroke-width="{w:.2f}" stroke-linecap="round"/>')
    return "".join(out)


def png(svg: str, path: Path, w: int, h: int):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = cairosvg.svg2png(bytestring=svg.encode(), output_width=w, output_height=h)
    Image.open(io.BytesIO(data)).convert("RGB").save(path)   # niente trasparenza: Play la rifiuta
    print(path.relative_to(ROOT), w, "x", h)


def data_uri(img: Image.Image) -> str:
    buf = io.BytesIO(); img.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def watch(name: str) -> Image.Image:
    """Snapshot rotondo dell'orologio, gli angoli trasparenti portati a nero come in una cattura dal dispositivo."""
    src = Image.open(WEAR / f"it.pixelbox.cmwatch.wear_{name}.png").convert("RGBA")
    flat = Image.new("RGBA", src.size, (0, 0, 0, 255)); flat.alpha_composite(src)
    return flat.convert("RGB")


def svg_doc(w, h, body):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
            f'width="{w}" height="{h}" viewBox="0 0 {w} {h}">{body}</svg>')


def icons():
    # A: l'icona del launcher così com'è, a tutto quadrato (Play applica da sé la maschera arrotondata).
    png(svg_doc(512, 512, f'<rect width="512" height="512" fill="{BG}"/>' + mark(256, 256, 150)),
        OUT / "icona/icon-A-launcher.png", 512, 512)
    # B: lo stesso segno più grande su un fondo che sale dal nero alla superficie delle card, come la luce della tile.
    grad = (f'<defs><radialGradient id="g" cx="50%" cy="42%" r="70%"><stop offset="0" stop-color="{SURFACE_HIGH}"/>'
            f'<stop offset="1" stop-color="{BG}"/></radialGradient></defs>')
    png(svg_doc(512, 512, grad + '<rect width="512" height="512" fill="url(#g)"/>' + mark(256, 262, 176)),
        OUT / "icona/icon-B-luce.png", 512, 512)


def round_image(img, cx, cy, d, clip_id):
    r = d / 2
    return (f'<defs><clipPath id="{clip_id}"><circle cx="{cx}" cy="{cy}" r="{r}"/></clipPath></defs>'
            f'<circle cx="{cx}" cy="{cy}" r="{r + 10}" fill="{SURFACE}"/>'
            f'<image x="{cx - r}" y="{cy - r}" width="{d}" height="{d}" clip-path="url(#{clip_id})" '
            f'xlink:href="{data_uri(img)}"/>')


def features():
    font = "font-family=\"Google Sans, Inter, sans-serif\""
    # A: nero, segno e nome a sinistra, un orologio con le sessioni a destra.
    body = (f'<rect width="1024" height="500" fill="{BG}"/>' + mark(120, 196, 58)
            + f'<text x="64" y="330" {font} font-size="60" font-weight="500" fill="{TEXT}">Claude Master</text>'
            + f'<text x="66" y="382" {font} font-size="28" fill="{TEXT2}">Your Claude Code sessions,</text>'
            + f'<text x="66" y="418" {font} font-size="28" fill="{TEXT2}">on your wrist and your phone.</text>'
            + round_image(watch("ScreensSnapshotTest_sessions"), 790, 250, 400, "w1"))
    png(svg_doc(1024, 500, body), OUT / "grafica/feature-A-sessioni.png", 1024, 500)
    # B: la domanda in primo piano e la quota dietro, su un fondo che schiarisce verso gli orologi.
    grad = (f'<defs><linearGradient id="bg" x1="0" x2="1"><stop offset="0" stop-color="{BG}"/>'
            f'<stop offset="1" stop-color="{SURFACE}"/></linearGradient></defs>')
    body = (grad + '<rect width="1024" height="500" fill="url(#bg)"/>' + mark(92, 100, 34)
            + f'<text x="64" y="250" {font} font-size="44" font-weight="500" fill="{TEXT}">Answer Claude Code</text>'
            + f'<text x="64" y="302" {font} font-size="44" font-weight="500" fill="{TEXT}">from your wrist.</text>'
            + f'<text x="66" y="372" {font} font-size="22" fill="{TEXT2}">Every session, every account, one tap away.</text>'
            + round_image(watch("ScreensSnapshotTest_quota"), 905, 165, 220, "w2")
            + round_image(watch("ScreensSnapshotTest_question"), 775, 300, 300, "w3"))
    png(svg_doc(1024, 500, body), OUT / "grafica/feature-B-domanda.png", 1024, 500)


def screenshots():
    # Orologio: 1:1, senza trasparenza né cornice (requisito WO-G5), 456 px come gli snapshot.
    for i, name in enumerate(["sessions", "question", "card", "quota", "terminal", "pairing"], 1):
        p = OUT / f"screenshot/watch/{i}-{name}.png"; p.parent.mkdir(parents=True, exist_ok=True)
        watch(f"ScreensSnapshotTest_{name}").save(p); print(p.relative_to(ROOT), "456 x 456")
    # Telefono: gli snapshot sono 461×1000, oltre il rapporto 2:1 che Play accetta; si allarga il fondo nero a
    # 500×1000 (lo sfondo dell'app è nero) e si porta a 1080×2160.
    for i, name in enumerate(["notPaired", "pairingRunning", "pairingDone", "paired"], 1):
        src = Image.open(PHONE / f"it.pixelbox.cmwatch.mobile_PhoneScreensTest_{name}.png").convert("RGBA")
        canvas = Image.new("RGBA", (500, 1000), (0, 0, 0, 255))
        canvas.alpha_composite(src, ((500 - src.width) // 2, 0))
        p = OUT / f"screenshot/phone/{i}-{name}.png"; p.parent.mkdir(parents=True, exist_ok=True)
        canvas.convert("RGB").resize((1080, 2160), Image.LANCZOS).save(p); print(p.relative_to(ROOT), "1080 x 2160")


if __name__ == "__main__":
    icons(); features(); screenshots()
