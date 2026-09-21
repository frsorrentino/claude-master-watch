#!/usr/bin/env python3
"""Tavola di fotogrammi di un video, per la verifica a vista (progetto §6).

Due modi:
  tavola.py VIDEO [USCITA.png] [--fps 1] [--cols 6] [--width 320]
      griglia di fotogrammi, N al secondo, 6 per riga.
  tavola.py VIDEO [USCITA.png] --start FOTOGRAMMA [--beats-per-row 8]
      linea del tempo di uno spezzone del film che comincia al fotogramma FOTOGRAMMA: una riga ogni due battute, e sotto
      ogni riga di fotogrammi la forma d'onda dell'audio COMPOSTO (quello che si sente nel reso, non le tracce sorgente),
      la griglia dei battiti (spessa sulle battute), i tagli di scena della scaletta e i fotogrammi neri misurati (i battiti
      di ciglia). Idea di master (21/09): il modello deve LEGGERE dove cade un taglio rispetto alla musica, non indovinarlo
      guardando fotogrammi sparsi. Un conto sulla durata di un file non vede uno stop-and-go; questa tavola sì.
      Se accanto alla traccia c'è la sua mappa (`music.v8.mappa.json`, fatta con mappa-musica.py di master), sopra la
      griglia teorica compaiono in arancio i DOWNBEAT MISURATI, riportati sul tempo del film attraverso lo stop-and-go:
      dove la tacca arancio non cade sulla battuta bianca, la scaletta e la musica non si parlano.
"""
import argparse, json, subprocess, tempfile, glob
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ap = argparse.ArgumentParser()
ap.add_argument("video"); ap.add_argument("out", nargs="?")
ap.add_argument("--fps", type=float, default=1); ap.add_argument("--cols", type=int, default=6); ap.add_argument("--width", type=int, default=320)
ap.add_argument("--start", type=int, help="fotogramma del film da cui parte lo spezzone: attiva la linea del tempo")
ap.add_argument("--beats-per-row", type=int, default=8)
ap.add_argument("--timeline", default=str(Path(__file__).parent / "remotion/src/film/timeline.json"))
ap.add_argument("--mappa", help="mappa della traccia (mappa-musica.py); di default quella accanto alla traccia della scaletta")
a = ap.parse_args()
out = Path(a.out or Path(a.video).with_suffix(".tavola.png"))

if a.start is None:
    with tempfile.TemporaryDirectory() as d:
        subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", a.video, "-vf", f"fps={a.fps},scale={a.width}:-1", f"{d}/f%04d.png"], check=True)
        fs = sorted(glob.glob(f"{d}/f*.png")); ims = [Image.open(f) for f in fs]
        w, h = ims[0].size; rows = (len(ims) + a.cols - 1) // a.cols
        W = Image.new("RGB", (a.cols * w, rows * h))
        for i, im in enumerate(ims): W.paste(im, ((i % a.cols) * w, (i // a.cols) * h))
        W.save(out); print(f"{out}: {len(ims)} fotogrammi, {W.size[0]}×{W.size[1]}")
    raise SystemExit

t = json.loads(Path(a.timeline).read_text())
FPS, BPM, OFF = t["fps"], t["bpm"], t["offsetSeconds"]
fpb = 60 / BPM * FPS                                   # fotogrammi per battito
beat_of = lambda f: (f / FPS - OFF) * BPM / 60         # fotogramma del film -> battito
frame_of = lambda b: OFF * FPS + b * fpb

# audio composto dello spezzone: RMS per fotogramma video, in dB
raw = subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-i", a.video, "-ac", "1", "-ar", "12000", "-f", "s16le", "-"], capture_output=True, check=True).stdout
pcm = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768
spf = 12000 // FPS
n = len(pcm) // spf
rms = np.sqrt(np.maximum(1e-12, (pcm[: n * spf].reshape(n, spf) ** 2).mean(axis=1)))
db = 20 * np.log10(rms)

# fotogrammi: tutti, piccoli, per trovare i neri e per le miniature
with tempfile.TemporaryDirectory() as d:
    subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-y", "-i", a.video, "-vf", "scale=192:-1", f"{d}/f%05d.png"], check=True)
    fs = sorted(glob.glob(f"{d}/f*.png"))
    frames = [Image.open(f).convert("RGB") for f in fs]
N = min(len(frames), n)
dark = [np.asarray(frames[i].convert("L")).mean() < 8 for i in range(N)]
f0, f1 = a.start, a.start + N - 1
b0 = int(np.floor(beat_of(f0))); b1 = int(np.ceil(beat_of(f1)))
cuts = [(s["at"], s["id"]) for s in t["scenes"] if b0 <= s["at"] <= b1]

# i downbeat misurati, portati sul film (stesse regole di Soundtrack.tsx: ritardo iniziale, silenzio, rientro da musicFrom)
mpath = Path(a.mappa) if a.mappa else (Path(a.timeline).parents[2] / "public" / t["music"]).with_suffix(".mappa.json")
downs = []
if mpath.exists():
    m = json.loads(mpath.read_text())
    bf = lambda b: round((OFF + b * 60 / BPM) * FPS)
    start = round((t.get("musicDelayBeats", 0) * 60) / BPM * FPS) + t.get("musicDelayFrames", 0)
    nap = None
    for k, sc in enumerate(t["scenes"][:-1]):
        if sc.get("sleep") and sc["sleep"].get("musicFrom") is not None:
            nx = t["scenes"][k + 1]; fr = bf(sc["at"] + sc["sleep"]["len"]) - bf(sc["at"]); cut = bf(nx["at"])
            nap = dict(hush=cut - round(fr * (0.75 - 0.125)), back=bf(nx["at"] + sc["sleep"].get("musicBackBeats", 4)), frm=sc["sleep"]["musicFrom"])
    for ts in m["downbeats"]:
        f = start + ts * FPS
        if nap is None or f < nap["hush"]: downs.append(f)                              # prima del silenzio: la traccia così com'è
        if nap and ts >= nap["frm"]: downs.append(nap["back"] + (ts - nap["frm"]) * FPS)  # dopo il rientro: dal punto `musicFrom`
TW, TH = frames[0].size                                  # miniatura
COLW = TW                                                # una colonna per battito
ROW_W = a.beats_per_row * COLW
WAVE_H, LAB_H, GAP = 90, 22, 14
ROW_H = LAB_H + TH + WAVE_H + GAP
rows = int(np.ceil((b1 - b0) / a.beats_per_row))
W = Image.new("RGB", (ROW_W + 60, rows * ROW_H + 30), (18, 18, 20))
dr = ImageDraw.Draw(W)
try: font = ImageFont.truetype("DejaVuSans.ttf", 13)
except OSError: font = ImageFont.load_default()
x_of = lambda b, r0: 50 + (b - r0) * COLW

for r in range(rows):
    r0 = b0 + r * a.beats_per_row; y = 20 + r * ROW_H
    # miniatura al centro di ogni battito (se il fotogramma è nello spezzone)
    for k in range(a.beats_per_row):
        fb = int(round(frame_of(r0 + k + 0.5))) - f0
        if 0 <= fb < N: W.paste(frames[fb], (int(x_of(r0 + k, r0)), y + LAB_H))
    wy = y + LAB_H + TH
    dr.rectangle([50, wy, 50 + ROW_W, wy + WAVE_H], fill=(28, 30, 34))
    # forma d'onda: una barra per fotogramma; sotto -40 dB è silenzio e si colora
    for i in range(N):
        b = beat_of(f0 + i)
        if not (r0 <= b < r0 + a.beats_per_row): continue
        x = x_of(b, r0); v = min(1, max(0, (db[i] + 60) / 60))
        col = (90, 170, 250) if db[i] > -40 else (200, 70, 70)
        dr.line([x, wy + WAVE_H, x, wy + WAVE_H - v * (WAVE_H - 4)], fill=col, width=2)
        if dark[i]: dr.rectangle([x - 1, y + LAB_H, x + 1, wy + WAVE_H], fill=(255, 60, 60))   # battito di ciglia misurato
    # griglia: ogni battito sottile, ogni battuta spessa, col numero
    for k in range(a.beats_per_row + 1):
        b = r0 + k; x = x_of(b, r0)
        bar = b % 4 == 0
        dr.line([x, y + LAB_H - 4, x, wy + WAVE_H], fill=(200, 200, 200) if bar else (80, 80, 85), width=2 if bar else 1)
        dr.text((x + 3, y + 2), f"{b}" + (" ▌" if bar else ""), fill=(230, 230, 230) if bar else (150, 150, 150), font=font)
    # downbeat misurati (mappa-musica.py): tacca arancio in cima alla forma d'onda
    for fd in downs:
        b = beat_of(fd)
        if r0 <= b <= r0 + a.beats_per_row:
            x = x_of(b, r0)
            dr.polygon([(x - 6, wy), (x + 6, wy), (x, wy + 12)], fill=(255, 150, 40))
    # tagli di scena della scaletta
    for at, sid in cuts:
        if r0 <= at <= r0 + a.beats_per_row:
            x = x_of(at, r0)
            dr.line([x, y + LAB_H, x, wy + WAVE_H], fill=(250, 200, 60), width=2)
            dr.text((x + 3, wy + WAVE_H - 16), sid, fill=(250, 200, 60), font=font)
dr.text((50, rows * ROW_H + 10), f"battiti {b0}–{b1} · blu: suono · rosso: sotto -40 dB · linee rosse: fotogrammi neri · gialle: inizio scena · triangoli arancio: downbeat misurati" + ("" if downs else " (nessuna mappa)"), fill=(170, 170, 170), font=font)
W.save(out); print(f"{out}: battiti {b0}-{b1}, {rows} righe, {W.size[0]}×{W.size[1]}")
