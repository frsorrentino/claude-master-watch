"""Contorno del tre quarti: `seed` lo semina dalla soglia dei prototipi, `check` disegna il poligono sulla foto a 3x in
otto riquadri da guardare uno per uno. I punti si correggono a mano in q34_outline.json e si rilancia `check`."""
import json, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"
OUT = HERE.parent / "out/contorno"
JSON = HERE / "q34_outline.json"
CX, CY, A, B, ROT = 872., 880., 525., 640., 7.
SHADOW = (7.5, 47.5)                                         # gradi dal centro, in senso orario dalle ore 3: tra la corona e l'ansa in basso

def mask(points, n, ss=4):
    m = Image.new("L", (n * ss, n * ss), 0)
    ImageDraw.Draw(m).polygon([(x * ss, y * ss) for x, y in points], fill=255)
    return m.resize((n, n), Image.LANCZOS).filter(ImageFilter.GaussianBlur(1.2))

def seed():
    im = Image.open(MAT / "q10_clean.png").convert("L").filter(ImageFilter.GaussianBlur(2)); n = im.width
    L = np.asarray(im).astype(np.float32); yy, xx = np.mgrid[0:n, 0:n]
    def ell(cx, cy, a, b, rot):
        c, s = np.cos(np.deg2rad(rot)), np.sin(np.deg2rad(rot)); u = (xx - cx) * c + (yy - cy) * s; v = -(xx - cx) * s + (yy - cy) * c
        return (u / a) ** 2 + (v / b) ** 2
    dark = np.asarray(Image.fromarray(((L < 78) * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(9)).filter(ImageFilter.MinFilter(9))) > 0
    m = dark | (ell(CX + 17, CY + 12, A + 17, B + 12, ROT) <= 1) | (ell(1495, 830, 64, 104, 4) <= 1)
    s = Image.fromarray((m * 255).astype(np.uint8)).copy(); ImageDraw.floodfill(s, (int(CX), int(CY)), 77); body = np.asarray(s) == 77
    wall = ell(CX + 30, CY + 14, A + 34, B + 16, ROT) <= 1       # bordo esterno della parete della cassa sul lato della corona (misurato sugli ingrandimenti)
    pts = []
    for deg in np.arange(0, 360, 1.0):                       # il raggio esce dal centro: tengo l'ultimo pixel del corpo
        r = np.arange(0, n * 0.75, 0.5); x = CX + r * np.cos(np.deg2rad(deg)); y = CY + r * np.sin(np.deg2rad(deg))
        ok = (x >= 0) & (x < n - 1) & (y >= 0) & (y < n - 1); hit = np.where(body[y[ok].astype(int), x[ok].astype(int)])[0]
        j = hit.max()
        if SHADOW[0] <= deg <= SHADOW[1]:                     # sotto la corona l'ombra ha la luminosità del metallo: lì vale la parete della cassa, costruita
            j = np.where(wall[y[ok].astype(int), x[ok].astype(int)])[0].max()
        pts.append([round(float(x[ok][j]), 1), round(float(y[ok][j]), 1)])
    JSON.write_text(json.dumps({"size": n, "points": pts}, indent=0)); print(len(pts), "punti in", JSON)

def check():
    d = json.loads(JSON.read_text()); im = Image.open(MAT / "q10_clean.png").convert("RGB"); n = im.width; OUT.mkdir(parents=True, exist_ok=True)
    big = im.resize((n * 3, n * 3), Image.LANCZOS); dr = ImageDraw.Draw(big); P = [(x * 3, y * 3) for x, y in d["points"]]
    dr.line(P + P[:1], fill=(0, 255, 0), width=2)
    for i, (x, y) in enumerate(P):
        dr.ellipse([x - 4, y - 4, x + 4, y + 4], outline=(255, 255, 0)); dr.text((x + 6, y - 6), str(i), fill=(255, 255, 0))
    w = n * 3 // 2
    for k in range(8):                                       # otto riquadri lungo il contorno, non una griglia cieca
        cx, cy = P[k * len(P) // 8]; box = [int(cx - w / 2), int(cy - w / 2), int(cx + w / 2), int(cy + w / 2)]
        big.crop(box).resize((1400, 1400), Image.LANCZOS).save(OUT / f"contorno_{k}.jpg", quality=88)
    cut = Image.new("RGB", (n, n), (150, 60, 160)); cut.paste(im, (0, 0), mask(d["points"], n)); cut.save(OUT / "ritaglio_su_viola.jpg", quality=90)
    print("guardare", OUT)

if __name__ == "__main__":
    {"seed": seed, "check": check}[sys.argv[1]]()
