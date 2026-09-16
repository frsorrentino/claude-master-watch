#!/usr/bin/env python3
"""Cornice dei video promozionali (Franz, 16/09 16:05): sfondo al tramonto e cassa di orologio generica, nera opaca, senza
cinturino, con il foro tondo dove va la registrazione dello schermo. Disegnata da zero: niente render né marchi di terzi.

Uscite in DIR (default: tools/promo/out):
  bg.png     1080x1080, sfondo e cassa, schermo nero
  glass.png  1080x1080 RGBA, solo il riflesso del vetro sopra lo schermo
  geometry   una riga «SX SY SD»: angolo in alto a sinistra e diametro dello schermo, per compose.sh
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

SIZE = 1080
SCREEN_D = 600          # diametro dello schermo nella cornice
BEZEL = 34              # anello nero fra schermo e cassa
CASE_RIM = 44           # spessore della cassa oltre la lunetta


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def dusk_background():
    # Tramonto come nella pagina di prova: blu notte in alto a sinistra, viola, rosa in basso a destra.
    stops = [(0.0, (43, 52, 80)), (0.55, (90, 74, 120)), (1.0, (201, 168, 201))]
    img = Image.new("RGB", (SIZE, SIZE))
    px = img.load()
    for y in range(SIZE):
        for x in range(SIZE):
            t = (x * 0.34 + y * 0.94) / (SIZE * 1.28)
            for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
                if t <= t1:
                    px[x, y] = lerp(c0, c1, (t - t0) / (t1 - t0))
                    break
            else:
                px[x, y] = stops[-1][1]
    return img


def radial_disc(d, inner, outer, center=(0.32, 0.28)):
    """Disco con sfumatura radiale da un punto di luce in alto a sinistra: la cassa opaca."""
    img = Image.new("RGBA", (d, d), (0, 0, 0, 0))
    px = img.load()
    cx, cy, r = d * center[0], d * center[1], d / 2
    for y in range(d):
        for x in range(d):
            if (x - r) ** 2 + (y - r) ** 2 > r * r:
                continue
            t = min(1.0, math.hypot(x - cx, y - cy) / (d * 0.68))
            px[x, y] = lerp(inner, outer, t) + (255,)
    return img


def main(out_dir):
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    bg = dusk_background().convert("RGBA")
    c = SIZE // 2
    case_d = SCREEN_D + 2 * (BEZEL + CASE_RIM)

    # Ombra morbida sotto la cassa.
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse((c - case_d // 2 + 10, c - case_d // 2 + 48, c + case_d // 2 - 10, c + case_d // 2 + 48), fill=(0, 0, 0, 150))
    bg = Image.alpha_composite(bg, shadow.filter(ImageFilter.GaussianBlur(38)))

    # Corona a destra, dietro al bordo della cassa.
    crown = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cw, ch = 40, 132
    x0 = c + case_d // 2 - 14
    ImageDraw.Draw(crown).rounded_rectangle((x0, c - ch // 2, x0 + cw, c + ch // 2), radius=18, fill=(34, 37, 42, 255), outline=(62, 66, 73, 255), width=2)
    bg = Image.alpha_composite(bg, crown)

    case = radial_disc(case_d, (58, 62, 69), (14, 16, 19))
    ImageDraw.Draw(case).ellipse((1, 1, case_d - 2, case_d - 2), outline=(80, 85, 93, 255), width=3)
    bg.alpha_composite(case, (c - case_d // 2, c - case_d // 2))

    bezel_d = SCREEN_D + 2 * BEZEL
    ImageDraw.Draw(bg).ellipse((c - bezel_d // 2, c - bezel_d // 2, c + bezel_d // 2, c + bezel_d // 2), fill=(5, 5, 6, 255), outline=(28, 30, 34, 255), width=2)
    sx = sy = c - SCREEN_D // 2
    ImageDraw.Draw(bg).ellipse((sx, sy, sx + SCREEN_D, sy + SCREEN_D), fill=(0, 0, 0, 255))
    bg.convert("RGB").save(out / "bg.png")

    # Riflesso del vetro: una luce diagonale tenue in alto a sinistra, dentro il cerchio dello schermo.
    glass = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    g = ImageDraw.Draw(glass)
    g.ellipse((sx - SCREEN_D * 0.15, sy - SCREEN_D * 0.35, sx + SCREEN_D * 0.95, sy + SCREEN_D * 0.42), fill=(255, 255, 255, 34))
    glass = glass.filter(ImageFilter.GaussianBlur(40))
    mask = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(mask).ellipse((sx, sy, sx + SCREEN_D, sy + SCREEN_D), fill=255)
    glass.putalpha(Image.composite(glass.getchannel("A"), Image.new("L", (SIZE, SIZE), 0), mask))
    glass.save(out / "glass.png")

    (out / "geometry").write_text(f"{sx} {sy} {SCREEN_D}\n")
    print(f"cornice pronta in {out}: schermo {SCREEN_D} px a ({sx},{sy})")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else str(Path(__file__).parent / "out"))
