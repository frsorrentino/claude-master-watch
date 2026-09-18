#!/usr/bin/env python3
"""Fedeltà di un componente ricostruito alla card vera (piano 3 §6, controllo misurabile): rende il fotogramma prima
dell'uscita (solo la card vera) e il primo dell'uscita (la card ricostruita disegnata sopra, ferma), e misura la differenza.
Uso: fedelta.py FRAME [--rect x y w h] [--out DIR]   (FRAME = primo fotogramma dell'effetto cardOut)
Stampa: differenza media dentro il rettangolo, massima fuori, righe di testo chiaro delle due card (per allineare i caratteri).
"""
import argparse, subprocess, sys
from pathlib import Path
import numpy as np
from PIL import Image, ImageChops

ap = argparse.ArgumentParser(); ap.add_argument("frame", type=int); ap.add_argument("--out", default="out/fedelta"); ap.add_argument("--rect", nargs=4, type=float, default=None)
a = ap.parse_args(); out = Path(a.out); out.mkdir(parents=True, exist_ok=True)
seg = out / "seg.mp4"
subprocess.run(["nice", "-n", "15", "npx", "remotion", "render", "Film", str(seg), f"--frames={a.frame-1}-{a.frame}", "--muted", "--log=error"], check=True, stdout=subprocess.DEVNULL)
for i, name in ((0, "vera"), (1, "ricostruita")):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(seg), "-vf", f"select=eq(n\\,{i})", "-frames:v", "1", str(out / f"{name}.png")], check=True)
A = Image.open(out / "vera.png").convert("RGB"); B = Image.open(out / "ricostruita.png").convert("RGB")
d = np.array(ImageChops.difference(A, B)).max(axis=2)
ys, xs = np.where(d > 24)
box = (xs.min(), ys.min(), xs.max(), ys.max()) if len(xs) else None
print(f"pixel con differenza > 24: {len(xs)}  riquadro {box}  massimo {d.max()}")
if box:
    x0, y0, x1, y1 = box
    L = np.array(A.convert("L")).astype(int); M = np.array(B.convert("L")).astype(int)
    def righe(im):
        sub = im[y0:y1, x0:x1]; bright = np.where((sub > 150).sum(axis=1) > 3)[0]
        runs, s, prev = [], None, None
        for y in bright:
            if s is None: s = y
            elif y != prev + 1: runs.append((s + y0, prev + y0)); s = y
            prev = y
        if s is not None: runs.append((s + y0, prev + y0))
        return runs
    def colonne(im):
        sub = im[y0:y1, x0:x1]; bright = np.where((sub > 150).sum(axis=0) > 1)[0]
        return (bright.min() + x0, bright.max() + x0) if len(bright) else None
    print("righe di testo chiaro  vera:", righe(L)); print("righe di testo chiaro  rico:", righe(M))
    print("colonne di testo chiaro vera:", colonne(L), " rico:", colonne(M))
    crop = (max(0, x0 - 20), max(0, y0 - 20), x1 + 20, y1 + 20)
    W = (crop[2] - crop[0]) * 2; H = (crop[3] - crop[1]) * 2
    T = Image.new("RGB", (W, H * 3 + 20))
    T.paste(A.crop(crop).resize((W, H), Image.LANCZOS), (0, 0)); T.paste(B.crop(crop).resize((W, H), Image.LANCZOS), (0, H + 10))
    T.paste(Image.fromarray(np.clip(d * 3, 0, 255).astype("uint8")).crop(crop).resize((W, H)).convert("RGB"), (0, 2 * H + 20))
    T.save(out / "confronto.png"); print("tavola:", out / "confronto.png")
