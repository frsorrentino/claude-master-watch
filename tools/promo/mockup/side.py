"""Mockup laterale (Franz, 18/09 16:44): l'orologio visto di taglio, display verso l'alto, dalla foto `laterale/foto3.jpg`
(1280×720, fondo parete grigia in alto e foglio bianco in basso). Scontorno per profili: per ogni colonna il bordo superiore
è il primo pixel più scuro del fondo locale, quello inferiore l'ultimo pixel del cinturino (sotto c'è l'ombra sul foglio, che
non entra); profili lisciati con una mediana, maschera ristretta di 2 px e sfumata di 1. Stessa correzione colore del
frontale. Uscite: remotion/public/mockup/side_body.png e le misure in mockup.geometry.json (`side`: cima e larghezza
della cupola, così il display in scorcio è un'ellisse nota)."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageFilter, ImageEnhance

HERE = Path(__file__).resolve().parent
SRC = HERE.parent / "materiali/foto/laterale/foto3.jpg"
PUB = HERE.parent / "remotion/public/mockup"
GEO = HERE.parent / "remotion/src/film/mockup.geometry.json"

def grade(im, contrast, red):
    r, g, b = ImageEnhance.Contrast(im).enhance(contrast).split()
    return Image.merge("RGB", (r.point(lambda v: int(v * red)), g, b.point(lambda v: min(255, int(v * 1.05)))))

def main():
    im = Image.open(SRC).convert("RGB"); a = np.asarray(im).astype(float); L = a.mean(axis=2); H, W = L.shape
    top = np.full(W, H); bot = np.full(W, -1)
    for x in range(W):
        col = L[:, x]; bgv = np.median(col[5:60])
        # cupola e cassa (x 266-1011): primo pixel più scuro della parete; cinturino: soglia assoluta (il nero sotto 90)
        ys = np.where(col < (bgv - 35 if 266 <= x <= 1011 else 90))[0]
        if len(ys): top[x] = ys.min()
        # sotto il cinturino c'è l'ombra sul foglio (170-210): il cinturino è sotto 70; sotto la cassa (x 380-900) il bordo sfuma
        # dal nero al foglio: si taglia dove supera 60
        ys2 = np.where(col < 45)[0]
        if len(ys2): bot[x] = ys2.max()
    k = 4
    top = np.array([np.median(top[max(0, i - k):i + k + 1]) for i in range(W)]).astype(int) + 3
    bot = np.array([np.median(bot[max(0, i - k):i + k + 1]) for i in range(W)]).astype(int) - 1
    m = np.zeros((H, W), bool)
    for x in range(W): m[top[x]:bot[x] + 1, x] = True
    mask = Image.fromarray((m * 255).astype("uint8")).filter(ImageFilter.GaussianBlur(1.0))
    body = grade(im, 1.12, 0.96); body.putalpha(mask)
    PUB.mkdir(parents=True, exist_ok=True); body.save(PUB / "side_body.png")
    # geometria: la cupola del vetro è la parte sopra la fascia di alluminio; misure sul bordo superiore
    case = np.where(top < 200)[0]; x0, x1 = int(case.min()), int(case.max())
    apex_x = int(x0 + np.argmin(top[x0:x1 + 1])); apex_y = int(top[apex_x])
    geo = json.loads(GEO.read_text()) if GEO.exists() else {}
    # origine per la grafica sospesa: il centro della cupola, appena sotto la cima
    geo["side"] = {"width": W, "height": H, "caseX0": x0, "caseX1": x1, "apexX": apex_x, "apexY": apex_y, "bottom": int(np.median(bot[x0:x1])), "displayCx": (x0 + x1) // 2, "displayCy": apex_y + 12}
    GEO.write_text(json.dumps(geo, indent=2) + "\n")
    print(geo["side"])

if __name__ == "__main__": main()
