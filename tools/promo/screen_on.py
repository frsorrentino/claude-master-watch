#!/usr/bin/env python3
"""Accende lo schermo del polso dal primo fotogramma (corto, Franz 23/09 22:17: «l'orologio nella scena del terminale parte
da ambient, invece vorrei che avesse già contenuto»).

La registrazione del terminale comincia col display spento: 41 fotogrammi neri, poi la schermata del terminale compare
già con le prime due righe. Qui i fotogrammi neri diventano quella stessa schermata SENZA le righe (la fascia fra il
titolo «Terminal · 17:33» e il tasto Write torna nera): il terminale è aperto e aspetta, e le righe arrivano quando le
mostrava la registrazione, dopo che il PC le ha scritte. Dal primo fotogramma acceso in poi la clip resta quella.

Uso: screen_on.py sorgente.mp4 destinazione.mp4

  cd tools/promo/remotion/public/scenes
  python3 ../../../screen_on.py n_watch_pinned.mp4 n_watch_on.mp4
"""
import subprocess, sys
import numpy as np

BAND = (175, 283)   # le righe nel primo fotogramma acceso: il titolo finisce a 166, il tasto Write comincia a 286 (misurato il 23/09)
LIT = 5.0           # luminosità media oltre la quale lo schermo è acceso (spento vale 0, acceso circa 40)
SCROLL = (175, 300, 440, 480)   # la barra di scorrimento a destra, che scende un filo sotto la fascia (alto, basso, sinistra, destra)

def first_on(frames) -> int:
    """Il primo fotogramma col display acceso."""
    return next(i for i, f in enumerate(frames) if float(f.mean()) > LIT)

def screen_on(frames, band=BAND):
    """(fotogrammi con lo schermo acceso dal primo, indice del primo acceso nella sorgente)."""
    k = first_on(frames)
    primed = frames[k].copy()
    primed[band[0]:band[1] + 1] = 0
    primed[SCROLL[0]:SCROLL[1] + 1, SCROLL[2]:SCROLL[3]] = 0   # senza righe non c'è niente da scorrere
    return [primed if i < k else f for i, f in enumerate(frames)], k

def main(argv) -> int:
    src, dst = argv
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
                            "-of", "csv=p=0", src], capture_output=True, text=True, check=True).stdout.strip()
    w, h = (int(x) for x in probe.split(","))
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", src, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"], capture_output=True, check=True).stdout
    frames = list(np.frombuffer(raw, np.uint8).reshape(-1, h, w, 3))
    out, k = screen_on(frames)
    # un fotogramma chiave ogni mezzo secondo come in fit-clip.py, qualità alta perché è un'altra compressione
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{w}x{h}", "-r", "30", "-i", "-",
                    "-c:v", "libx264", "-crf", "12", "-g", "15", "-keyint_min", "15", "-sc_threshold", "0", "-pix_fmt", "yuv420p", dst],
                   input=b"".join(f.tobytes() for f in out), check=True)
    print(f"{dst}: {len(out)} fotogrammi, schermo acceso dal primo (erano spenti i primi {k})")
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
