#!/usr/bin/env python3
"""Cornice dei video promozionali, sul modello dei video di prodotto degli smartwatch (Franz, 16/09 16:58): formato 2:1,
sfondo pastello uniforme, orologio frontale e centrato con il cinturino che esce dal bordo in alto e in basso. La cassa e il
cinturino sono disegnati da zero, generici, nei nostri colori: niente render né marchi di terzi.

Uso: frame.py [DIR] [--bg E7DDF5] [--band 5A5E8A] [--width 1920]
Uscite in DIR (default tools/promo/out):
  bg.png     sfondo, cinturino e cassa, schermo nero
  glass.png  RGBA, solo il riflesso del vetro sopra lo schermo
  geometry   «SX SY SD W H»: angolo e diametro dello schermo, misure della tela
"""
import argparse
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def band(width, height, color):
    """Cinturino in maglia: coste orizzontali sottili con una trama leggera e i bordi più scuri, per dare volume."""
    img = Image.new("RGB", (width, height), color)
    px = img.load()
    rnd = random.Random(7)
    dark = lerp(color, (0, 0, 0), 0.35)
    light = lerp(color, (255, 255, 255), 0.18)
    for y in range(height):
        rib = 0.5 + 0.5 * math.sin(y * 2 * math.pi / 7.0)          # una costa ogni 7 px
        for x in range(width):
            edge = abs(x - width / 2) / (width / 2)                  # 0 al centro, 1 ai bordi
            base = lerp(light, color, 0.35 + 0.65 * rib)
            base = lerp(base, dark, max(0.0, edge - 0.55) / 0.45 * 0.9)
            n = rnd.randint(-6, 6)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base)
    return img


def radial_disc(d, inner, outer, center=(0.34, 0.26)):
    img = Image.new("RGBA", (d, d), (0, 0, 0, 0))
    px = img.load()
    cx, cy, r = d * center[0], d * center[1], d / 2
    for y in range(d):
        for x in range(d):
            if (x - r) ** 2 + (y - r) ** 2 > r * r:
                continue
            t = min(1.0, math.hypot(x - cx, y - cy) / (d * 0.7))
            px[x, y] = lerp(inner, outer, t) + (255,)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("out", nargs="?", default=str(Path(__file__).parent / "out"))
    ap.add_argument("--bg", default="E7DDF5")        # lilla pastello
    ap.add_argument("--band", default="5A5E8A")      # indaco spento, colore nostro
    ap.add_argument("--width", type=int, default=1920)
    a = ap.parse_args()

    W = a.width; H = W // 2
    out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
    cx, cy = W // 2, H // 2
    screen_d = int(H * 0.50)
    bezel = int(screen_d * 0.055)
    rim = int(screen_d * 0.075)
    case_d = screen_d + 2 * (bezel + rim)
    band_w = int(case_d * 0.56)

    bg = Image.new("RGBA", (W, H), hex_rgb(a.bg) + (255,))

    # Ombra morbida dietro orologio e cinturino.
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rectangle((cx - band_w // 2 + 16, 0, cx + band_w // 2 + 16, H), fill=(40, 20, 70, 45))
    sd.ellipse((cx - case_d // 2 + 18, cy - case_d // 2 + 30, cx + case_d // 2 + 18, cy + case_d // 2 + 30), fill=(40, 20, 70, 70))
    bg = Image.alpha_composite(bg, shadow.filter(ImageFilter.GaussianBlur(34)))

    # Cinturino dal bordo in alto a quello in basso, dietro la cassa.
    bg.paste(band(band_w, H, hex_rgb(a.band)), (cx - band_w // 2, 0))

    # Corona a destra.
    crown = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    cw, ch = int(case_d * 0.05), int(case_d * 0.17)
    x0 = cx + case_d // 2 - int(cw * 0.35)
    ImageDraw.Draw(crown).rounded_rectangle((x0, cy - ch // 2, x0 + cw, cy + ch // 2), radius=cw // 2, fill=(40, 43, 49, 255), outline=(78, 82, 90, 255), width=2)
    bg = Image.alpha_composite(bg, crown)

    case = radial_disc(case_d, (70, 74, 82), (16, 18, 22))
    ImageDraw.Draw(case).ellipse((1, 1, case_d - 2, case_d - 2), outline=(96, 101, 110, 255), width=3)
    bg.alpha_composite(case, (cx - case_d // 2, cy - case_d // 2))

    bezel_d = screen_d + 2 * bezel
    dr = ImageDraw.Draw(bg)
    dr.ellipse((cx - bezel_d // 2, cy - bezel_d // 2, cx + bezel_d // 2, cy + bezel_d // 2), fill=(5, 5, 6, 255), outline=(30, 32, 36, 255), width=2)
    sx, sy = cx - screen_d // 2, cy - screen_d // 2
    dr.ellipse((sx, sy, sx + screen_d, sy + screen_d), fill=(0, 0, 0, 255))
    bg.convert("RGB").save(out / "bg.png")

    glass = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(glass).ellipse((sx - screen_d * 0.15, sy - screen_d * 0.35, sx + screen_d * 0.95, sy + screen_d * 0.42), fill=(255, 255, 255, 30))
    glass = glass.filter(ImageFilter.GaussianBlur(36))
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).ellipse((sx, sy, sx + screen_d, sy + screen_d), fill=255)
    glass.putalpha(Image.composite(glass.getchannel("A"), Image.new("L", (W, H), 0), mask))
    glass.save(out / "glass.png")

    # La maschera tonda dello schermo, disegnata una volta: applicarla con `geq` fotogramma per fotogramma su CPU ARM
    # richiedeva più di dieci minuti per quattro secondi di clip (16/09 17:15).
    sm = Image.new("L", (screen_d, screen_d), 0)
    ImageDraw.Draw(sm).ellipse((0, 0, screen_d - 1, screen_d - 1), fill=255)
    sm.save(out / "mask.png")

    (out / "geometry").write_text(f"{sx} {sy} {screen_d} {W} {H}\n")
    print(f"cornice {W}x{H} in {out}: schermo {screen_d} px a ({sx},{sy})")


if __name__ == "__main__":
    main()
