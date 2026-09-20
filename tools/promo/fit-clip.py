#!/usr/bin/env python3
"""Ricuce una registrazione del polso sui battiti del film.

La registrazione scorre al ritmo del dito: soste lunghe, scorrimenti lenti, e una
sosta per account dove il film ne vuole una sola. Qui si dice, segmento per segmento,
quanto deve durare nel film: le soste si accorciano o si allungano (sono ferme, non
si vede), gli scorrimenti si accelerano, e la sosta di troppo si salta cucendo i due
scorrimenti che la circondano. Così la card sul polso si ferma sul battito del pannello.

Uso: fit-clip.py sorgente.mp4 destinazione.mp4 "a-b:durata[,a-b:durata...]"
     (tempi in secondi del sorgente; un segmento «a-b+c-d:durata» cuce due pezzi)

La cartella public/scenes/ non sta nel repo: la clip della Panoramica si rifà così,
dalla registrazione n_overview_cut.mp4 (le due soste sulla Quota diventano una):

  cd tools/promo/remotion/public/scenes
  python3 ../../../fit-clip.py n_overview_cut.mp4 n_overview_fit.mp4 \
    "0.2-2.2:1.8,2.2-4.9+5.5-8.5:1.47,8.5-9.1:1.8,9.1-11.8:1.2,11.8-13.06:1.26,13.8-16.5:1.2,16.5-18.6:1.67,18.6-21.25:1.2,21.25-21.8:4.16"

Ogni sosta dura quanto il conteggio del pannello accanto (il numero finisce di salire mentre
la card è ancora ferma), non quanto il dito ci si era fermato: la Quota tiene 1,8 s e l'ultima
sosta si spezza in tre (Open questions, scorrimento, Context) perché i pannelli sono due.
"""
import subprocess, sys, tempfile, os

def main() -> int:
    src, dst, plan = sys.argv[1], sys.argv[2], sys.argv[3]
    parts, filt, n = [], [], 0
    for seg in plan.split(","):
        span, dur = seg.split(":")
        dur = float(dur)
        pieces = [tuple(float(x) for x in p.split("-")) for p in span.split("+")]
        raw = sum(b - a for a, b in pieces)
        labels = []
        for a, b in pieces:
            filt.append(f"[0:v]trim={a}:{b},setpts=PTS-STARTPTS[p{n}]")
            labels.append(f"[p{n}]"); n += 1
        if len(labels) > 1:
            filt.append("".join(labels) + f"concat=n={len(labels)}:v=1:a=0[j{n}]")
            cur = f"[j{n}]"; n += 1
        else:
            cur = labels[0]
        filt.append(f"{cur}setpts=PTS*{dur / raw:.6f}[s{n}]")
        parts.append(f"[s{n}]"); n += 1
    filt.append("".join(parts) + f"concat=n={len(parts)}:v=1:a=0[out]")
    cmd = ["ffmpeg", "-loglevel", "error", "-y", "-i", src, "-filter_complex", ";".join(filt),
           "-map", "[out]", "-r", "30", "-c:v", "libx264", "-crf", "18", "-pix_fmt", "yuv420p", dst]
    subprocess.run(cmd, check=True)
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", dst],
                         capture_output=True, text=True, check=True).stdout.strip()
    print(f"{dst}: {out} s")
    return 0

if __name__ == "__main__":
    sys.exit(main())
