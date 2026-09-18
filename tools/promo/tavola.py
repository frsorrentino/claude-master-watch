#!/usr/bin/env python3
"""Tavola di fotogrammi di un video: N fotogrammi al secondo, 6 per riga, per la verifica a vista (progetto §6).
Uso: tavola.py VIDEO [USCITA.png] [--fps 1] [--cols 6] [--width 320]
"""
import argparse, subprocess, tempfile, glob
from pathlib import Path
from PIL import Image

ap = argparse.ArgumentParser(); ap.add_argument("video"); ap.add_argument("out", nargs="?"); ap.add_argument("--fps", type=float, default=1); ap.add_argument("--cols", type=int, default=6); ap.add_argument("--width", type=int, default=320)
a = ap.parse_args(); out = Path(a.out or Path(a.video).with_suffix(".tavola.png"))
with tempfile.TemporaryDirectory() as d:
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", a.video, "-vf", f"fps={a.fps},scale={a.width}:-1", f"{d}/f%04d.png"], check=True)
    fs = sorted(glob.glob(f"{d}/f*.png")); ims = [Image.open(f) for f in fs]
    w, h = ims[0].size; rows = (len(ims) + a.cols - 1) // a.cols
    W = Image.new("RGB", (a.cols * w, rows * h))
    for i, im in enumerate(ims): W.paste(im, ((i % a.cols) * w, (i // a.cols) * h))
    W.save(out); print(f"{out}: {len(ims)} fotogrammi, {W.size[0]}×{W.size[1]}")
