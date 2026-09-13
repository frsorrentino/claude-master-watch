#!/usr/bin/env python3
"""Genera l'anteprima della tile per il selettore di Wear OS.

L'anteprima è una risorsa statica (`androidx.wear.tiles.PREVIEW`): il sistema non la ricava dal layout,
quindi VA RIGENERATA A OGNI MODIFICA DELLA TILE (Franz, 13/09). Rispecchia lo stato «a riposo»:
due card in stile Gmail (chi e quando sopra, cosa sotto) e il bottone di bordo pastello con il conteggio.

Uso: python3 tools/make_tile_preview.py
Scrive: wear/src/main/res/drawable-nodpi/tile_preview.png
"""
from PIL import Image, ImageDraw, ImageFont
import pathlib

S = 384
BG = (0, 0, 0, 255)
CARD = (42, 49, 60, 255)          # surfaceContainer, misurato sulla tile di Gmail
TXT = (235, 241, 255, 255)        # onSurface
SEC = (194, 198, 210, 255)        # onSurfaceVariant (8:1 sulla card)
LABEL = (211, 227, 253, 255)      # etichetta, come il mittente di Gmail (10,1:1)
EDGE = (211, 227, 253, 255)       # primary, misurato sull'EdgeButton di Gmail
EDGE_TXT = (10, 32, 80, 255)      # onPrimary
YEL = (244, 208, 63, 255)         # badge della sessione (colore del contratto)

FONTS = [
    "/home/demo/workspaces/personal/watchface/tools/fonts/googlesans_700.ttf",
    "/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf",
]


def font(size):
    for path in FONTS:
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            pass
    return ImageFont.load_default()


def main():
    im = Image.new("RGBA", (S, S), BG)
    d = ImageDraw.Draw(im)

    def right(text, y, f, fill, x2):
        w = d.textbbox((0, 0), text, font=f)[2]
        d.text((x2 - w, y), text, font=f, fill=fill)

    def center(text, y, f, fill):
        w = d.textbbox((0, 0), text, font=f)[2]
        d.text(((S - w) / 2, y), text, font=f, fill=fill)

    # card 1: ultima attività
    d.rounded_rectangle((14, 52, S - 14, 158), radius=40, fill=CARD)
    d.ellipse((40, 70, 70, 100), fill=YEL)
    d.polygon([(51, 77), (62, 85), (51, 93)], fill=(0, 0, 0, 255))
    d.text((80, 76), "master", font=font(20), fill=LABEL)
    right("2 m", 76, font(20), SEC, S - 38)
    d.text((38, 112), "Bash pytest -q tests", font=font(24), fill=TXT)

    # card 2: quota, con la barra lineare (percentuale della finestra di 5 ore)
    d.rounded_rectangle((14, 170, S - 14, 272), radius=40, fill=CARD)
    d.text((38, 186), "Quota personale", font=font(20), fill=LABEL)
    right("reset 15:20", 186, font(18), SEC, S - 38)
    d.text((38, 218), "24 %", font=font(28), fill=TXT)
    bar_x0, bar_x1, gap, pct = 140, S - 38, 6, 0.24
    fill_w = int((bar_x1 - bar_x0 - gap) * pct)
    d.rounded_rectangle((bar_x0, 228, bar_x0 + fill_w, 244), radius=8, fill=EDGE)
    d.rounded_rectangle((bar_x0 + fill_w + gap, 228, bar_x1, 244), radius=8, fill=(60, 68, 82, 255))

    # bottone di bordo
    d.ellipse((52, S - 70, S - 52, S + 48), fill=EDGE)
    center("3 attive", S - 60, font(22), EDGE_TXT)

    out = pathlib.Path(__file__).resolve().parent.parent / "wear/src/main/res/drawable-nodpi/tile_preview.png"
    im.save(out)
    print(out)


if __name__ == "__main__":
    main()
