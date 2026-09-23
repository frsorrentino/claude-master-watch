#!/usr/bin/env python3
"""Tiene fermo il tasto Write in fondo al terminale del polso (corto, Franz 23/09 18:58).

Nell'app il tasto Write del terminale è l'ultima voce della lista, non l'EdgeButton dello ScreenScaffold: a ogni riga
nuova la lista scende e poi risale, e il tasto sobbalza con lei. In n_watch_fit.mp4 succede quattro volte, a 3,0, 3,7,
4,4 e 5,0 s: il tasto scende fino a 64 px e torna su in 9-10 fotogrammi. Finché l'app non lo mette nello ScreenScaffold,
qui il tasto che si muove si copre col nero del fondo e sopra, nel posto dove sta fermo, si rimette il tasto preso da un
fotogramma fermo, col suo bordo sfumato: il testo che risale gli passa dietro, come sotto un EdgeButton vero.
I fotogrammi col tasto fermo restano quelli della registrazione.

Uso: pin_button.py sorgente.mp4 destinazione.mp4

  cd tools/promo/remotion/public/scenes
  python3 ../../../pin_button.py n_watch_fit.mp4 n_watch_pinned.mp4
"""
import subprocess, sys
from collections import Counter
import numpy as np
from PIL import Image, ImageDraw

COLOR = (209, 225, 252)   # il fondo del tasto nella registrazione (misurato al centro, 23/09)
TOP = 150                 # il tasto sta sotto questa riga: sopra ci sono l'ora e l'intestazione
MIN_PIXELS = 500          # meno di così non è il tasto
MARGIN = 3                # il bordo sfumato attorno al tasto
TOL = 2                   # la compressione fa oscillare il bordo di basso di 2 px anche a lista ferma

def button_mask(f: np.ndarray) -> np.ndarray:
    """I pixel del fondo del tasto: azzurri, non bianchi (il testo bianco ha blu e rosso uguali)."""
    r, g, b = (f[..., i].astype(np.int32) for i in range(3))
    return (abs(r - COLOR[0]) < 25) & (abs(g - COLOR[1]) < 25) & (b > 235) & (b - r > 25)

def button_box(f: np.ndarray):
    """(alto, basso, sinistra, destra) del tasto, o None se non si vede."""
    m = button_mask(f)
    m[:TOP] = False
    ys, xs = np.nonzero(m)
    if len(ys) < MIN_PIXELS:
        return None
    return (int(ys.min()), int(ys.max()), int(xs.min()), int(xs.max()))

def rest_box(boxes):
    """Il posto del tasto è quello in cui sta più a lungo."""
    return Counter(b for b in boxes if b is not None).most_common(1)[0][0]

def fill_holes(m: np.ndarray) -> np.ndarray:
    """La maschera con i buchi chiusi: tutto quello che il fondo esterno non raggiunge (la scritta dentro il tasto).
    Non scipy.ndimage: quella di sistema è compilata per numpy 1 e qui non si carica (23/09)."""
    img = Image.fromarray(np.pad(m, 1).astype(np.uint8) * 255).copy()     # senza copy() Pillow 9.4 riempie una copia e questa resta com'era
    ImageDraw.floodfill(img, (0, 0), 128)
    return (np.asarray(img) != 128)[1:-1, 1:-1]

def dilate(m: np.ndarray, n: int) -> np.ndarray:
    """La maschera allargata di n pixel in orizzontale e in verticale."""
    for _ in range(n):
        d = m.copy()
        d[1:] |= m[:-1]; d[:-1] |= m[1:]; d[:, 1:] |= m[:, :-1]; d[:, :-1] |= m[:, 1:]
        m = d
    return m

def sprite_at(f: np.ndarray, box):
    """Il tasto ritagliato da un fotogramma fermo: ((alto, sinistra), colori premoltiplicati sul nero, copertura 0-1)."""
    t, b, l, r = box
    t0, l0 = max(t - MARGIN, 0), max(l - MARGIN, 0)
    crop = f[t0:min(b + MARGIN + 1, f.shape[0]), l0:min(r + MARGIN + 1, f.shape[1])].astype(np.float32)
    body = fill_holes(button_mask(crop))                             # il fondo, con la scritta scura dentro
    rim = dilate(body, MARGIN) & ~body
    # sul nero il bordo sfumato è il colore del fondo per la sua copertura
    alpha = np.where(body, 1.0, np.where(rim, np.clip(crop[..., 1] / COLOR[1], 0, 1), 0.0)).astype(np.float32)
    return (t0, l0), np.where(alpha[..., None] > 0, crop, 0), alpha

def fix(f: np.ndarray, box, sprite) -> np.ndarray:
    """Copre il tasto che si è mosso col nero del fondo e rimette sopra quello fermo."""
    g = f.copy()
    t, b, l, r = box
    g[max(t - MARGIN, 0):b + MARGIN + 1, max(l - MARGIN, 0):r + MARGIN + 1] = 0
    (t0, l0), spr, alpha = sprite
    h, w = alpha.shape
    under = g[t0:t0 + h, l0:l0 + w].astype(np.float32)
    g[t0:t0 + h, l0:l0 + w] = np.clip(spr + (1 - alpha[..., None]) * under + 0.5, 0, 255).astype(np.uint8)
    return g

def pin(frames):
    """(fotogrammi col tasto fermo, quanti ne ha corretti)."""
    boxes = [button_box(f) for f in frames]
    rest = rest_box(boxes)
    sprite = sprite_at(frames[boxes.index(rest)], rest)
    out, moved = [], 0
    for f, b in zip(frames, boxes):
        if b is None or all(abs(x - y) <= TOL for x, y in zip(b, rest)):
            out.append(f)
        else:
            out.append(fix(f, b, sprite)); moved += 1
    return out, moved

def main(argv) -> int:
    src, dst = argv
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
                            "-of", "csv=p=0", src], capture_output=True, text=True, check=True).stdout.strip()
    w, h = (int(x) for x in probe.split(","))
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", src, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"], capture_output=True, check=True).stdout
    frames = list(np.frombuffer(raw, np.uint8).reshape(-1, h, w, 3))
    out, moved = pin(frames)
    # un fotogramma chiave ogni mezzo secondo come in fit-clip.py, qualità più alta perché è una seconda compressione
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{w}x{h}", "-r", "30", "-i", "-",
                    "-c:v", "libx264", "-crf", "12", "-g", "15", "-keyint_min", "15", "-sc_threshold", "0", "-pix_fmt", "yuv420p", dst],
                   input=b"".join(f.tobytes() for f in out), check=True)
    print(f"{dst}: {len(out)} fotogrammi, tasto rimesso a posto in {moved}")
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
