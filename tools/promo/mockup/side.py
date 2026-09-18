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

def satin(im, sat=0.72, contrast=1.16, black=0.45, high=0.5):
    """Meno fotografico, più oggetto (Franz, 18/09 18:49): si scuriscono SOLO i neri (curva sulle ombre, le alte luci restano),
    si alza un po' il contrasto e si comprimono i riflessi speculari: il vetro diventa satinato invece che specchiante."""
    curve = [int(v * (1 - black * max(0.0, 1 - v / 110) ** 1.5)) for v in range(256)]          # ombre giù, mezzitoni quasi fermi
    curve = [min(255, int(c if c < 190 else 190 + (c - 190) * high)) for c in curve]           # alte luci compresse: satinato
    out = ImageEnhance.Color(im).enhance(sat).point(curve * 3)
    return ImageEnhance.Contrast(out).enhance(contrast)

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
    # il bordo non è la fila di pixel: è una CURVA (Franz, 18/09 18:54). Sulla cassa si adatta un polinomio al profilo
    # misurato (la cupola è un arco liscio), sul cinturino basta una lisciatura lunga; così il ritaglio non è mai frastagliato.
    k = 4
    top = np.array([np.median(top[max(0, i - k):i + k + 1]) for i in range(W)], dtype=float)
    bot = np.array([np.median(bot[max(0, i - k):i + k + 1]) for i in range(W)], dtype=float)
    case = np.where(top < 200)[0]; cx0, cx1 = int(case.min()), int(case.max())
    xs = np.arange(cx0, cx1 + 1)
    good = np.abs(top[xs] - np.poly1d(np.polyfit(xs, top[xs], 6))(xs)) < 6        # via i pelucchi, poi si riadatta
    fit = np.poly1d(np.polyfit(xs[good], top[xs][good], 6))
    # profilo lisciato ovunque (serve al cinturino e al raccordo con la cupola)
    smooth = np.array([top[max(0, i - 18):i + 19].mean() for i in range(W)])
    # peso della cupola: 1 al centro della cassa, 0 sul cinturino, transizione dolce in 90 px attorno alle anse: niente scalini
    d = np.minimum(np.arange(W) - cx0, cx1 - np.arange(W)).astype(float)
    t = np.clip(d / 90.0, 0, 1); wgt = t * t * (3 - 2 * t)
    top = wgt * fit(np.arange(W)) + (1 - wgt) * smooth
    bot = np.array([bot[max(0, i - 18):i + 19].mean() for i in range(W)])
    lo = np.poly1d(np.polyfit(np.arange(W), bot, 3))(np.arange(W))               # il cinturino appoggia su una curva dolce
    bot = np.minimum(bot, lo + 6)
    top = np.round(top).astype(int) + 3; bot = np.round(bot).astype(int) - 1
    m = np.zeros((H, W), bool)
    for x in range(W): m[top[x]:bot[x] + 1, x] = True
    mask = Image.fromarray((m * 255).astype("uint8")).filter(ImageFilter.GaussianBlur(1.0))
    body = satin(grade(im, 1.0, 0.98)); body.putalpha(mask)
    # in piano: il cinturino scende da sinistra a destra di qualche pixel, si raddrizza sulla sua retta dei minimi quadrati
    xs = np.arange(W)[(bot > 0) & ((np.arange(W) < 200) | (np.arange(W) > W - 200))]
    slope = np.polyfit(xs, bot[xs], 1)[0]
    ang = np.degrees(np.arctan(slope))
    body = body.rotate(ang, resample=Image.BICUBIC, center=(W / 2, float(np.median(bot))), expand=False)
    # il cinturino arriva ai bordi del quadro: la tela si allarga e le colonne di bordo si stirano (niente sfumature di taglio)
    EXT = 760
    wide = Image.new("RGBA", (W + 2 * EXT, H), (0, 0, 0, 0))
    wide.paste(body, (EXT, 0))
    left = body.crop((6, 0, 26, H)).resize((EXT + 6, H), Image.BICUBIC)
    right = body.crop((W - 26, 0, W - 6, H)).resize((EXT + 6, H), Image.BICUBIC)
    wide.paste(left, (0, 0)); wide.paste(right, (W + EXT - 6, 0))
    PUB.mkdir(parents=True, exist_ok=True); wide.save(PUB / "side_body.png")
    # geometria: la cupola del vetro è la parte sopra la fascia di alluminio; misure sul bordo superiore
    case = np.where(top < 200)[0]; x0, x1 = int(case.min()), int(case.max())
    apex_x = int(x0 + np.argmin(top[x0:x1 + 1])); apex_y = int(top[apex_x])
    geo = json.loads(GEO.read_text()) if GEO.exists() else {}
    # origine per la grafica sospesa: il centro della cupola, appena sotto la cima
    geo["side"] = {"width": W + 2 * EXT, "height": H, "ext": EXT, "photoW": W, "tilt": round(float(ang), 3), "caseX0": x0 + EXT, "caseX1": x1 + EXT, "apexX": apex_x + EXT, "apexY": apex_y, "bottom": int(np.median(bot[x0:x1])), "displayCx": (x0 + x1) // 2 + EXT, "displayCy": apex_y + 12}
    GEO.write_text(json.dumps(geo, indent=2) + "\n")
    print(geo["side"])

if __name__ == "__main__": main()
