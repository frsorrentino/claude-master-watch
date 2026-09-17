"""Porta il mockup nel montaggio: immagini con trasparenza in remotion/public/mockup/, misure in src/film/mockup.geometry.json."""
import json
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter
import outline, pose

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"
PUB = HERE.parent / "remotion/public/mockup"
GEO = HERE.parent / "remotion/src/film/mockup.geometry.json"
CH, K, DEPTH = 7.8, 0.86, -0.05                    # smusso lucido (come matte_front.py), raggio del display sul vetro, profondità apparente del pannello

def grade(im, contrast, red):                      # la stessa correzione dei fotogrammi di prova: contrasto e dominante fredda
    r, g, b = ImageEnhance.Contrast(im).enhance(contrast).split()
    return Image.merge("RGB", (r.point(lambda v: int(v * red)), g, b.point(lambda v: min(255, int(v * 1.05)))))

def front():
    im = grade(Image.open(MAT / "f5_body.png").convert("RGB"), 1.12, 0.96); im.putalpha(Image.open(MAT / "f5_mask.png").convert("L"))
    im.save(PUB / "front_body.png")
    cx, cy, rc = (float(v) for v in np.load(MAT / "f5_case.npy")); glass = rc - CH - 1.5
    return {"size": im.width, "cx": round(cx, 2), "cy": round(cy, 2), "caseR": round(rc, 2), "glassR": round(glass, 2),
            "coverR": round(glass * 0.915, 2), "displayR": round(glass * K, 2)}

def q34():
    photo = Image.open(MAT / "q10_clean.png").convert("RGB"); n = photo.width
    body = grade(photo, 1.10, 0.95); pts = json.loads((HERE / "q34_outline.json").read_text())["points"]
    rgba = body.copy(); rgba.putalpha(outline.mask(pts, n)); rgba.save(PUB / "q34_body.png")
    # Profondità NEGATIVA: dal Pixel Watch 4 il display è vicino al vetro della cupola, e la cupola scende lungo i fianchi: la
    # giunzione vetro/cassa (l'ellisse misurata) sta più in basso del pannello. Misurato sulla foto reale a schermo acceso del
    # 17/09 (inclinazione 39°): margine lontano 0,073 del semiasse maggiore, margine verso la corona circa doppio; con +0,03 i
    # due margini uscivano uguali.
    quad = pose.display_quad(pose.fit()[0], k=K, depth=DEPTH); m = 960
    disc = Image.new("L", (m, m), 0); ImageDraw.Draw(disc).ellipse([2, 2, m - 3, m - 3], fill=255)
    M, rhs = [], []
    for (x, y), (X, Y) in zip(quad, [(0, 0), (m, 0), (m, m), (0, m)]):      # coefficienti uscita -> ingresso, come vuole PIL
        M.append([x, y, 1, 0, 0, 0, -X * x, -X * y]); rhs.append(X); M.append([0, 0, 0, x, y, 1, -Y * x, -Y * y]); rhs.append(Y)
    co = np.linalg.solve(np.array(M, float), np.array(rhs, float))
    a = np.asarray(disc.filter(ImageFilter.GaussianBlur(2)).transform((n, n), Image.PERSPECTIVE, tuple(co), Image.BICUBIC)).astype(np.float32) / 255
    refl = np.clip((np.asarray(body).astype(np.float32) - 40) * 1.20, 0, 255) * a[..., None]
    Image.fromarray(refl.astype(np.uint8)).save(PUB / "q34_reflections.png")
    return {"size": n, "cx": 872, "cy": 880, "b": 640, "quad": [[round(float(v), 1) for v in q] for q in quad]}

if __name__ == "__main__":
    PUB.mkdir(parents=True, exist_ok=True); GEO.parent.mkdir(parents=True, exist_ok=True)
    GEO.write_text(json.dumps({"front": front(), "q34": q34()}, indent=2) + "\n"); print(GEO.read_text())
