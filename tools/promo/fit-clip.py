#!/usr/bin/env python3
"""Ricuce una registrazione del polso sui battiti del film.

La registrazione scorre al ritmo del dito: soste lunghe, scorrimenti lenti, e una
sosta per account dove il film ne vuole una sola. Qui si dice, segmento per segmento,
quanto deve durare nel film: le soste si accorciano o si allungano (sono ferme, non
si vede), gli scorrimenti si accelerano, e la sosta di troppo si salta cucendo i due
scorrimenti che la circondano. Così la card sul polso si ferma sul battito del pannello.

Uso: fit-clip.py sorgente.mp4 destinazione.mp4 "a-b:durata[,a-b:durata...]"
     (tempi in secondi del sorgente; un segmento «a-b+c-d:durata» cuce due pezzi)

La cartella public/scenes/ non sta nel repo: la clip della Panoramica si rifà così, dalla registrazione
fatta al polso il 21/09 sera (overview9: demo accesa con l'orologio alle 08:17, così i campioni del ritmo sono una rampa
sola e il grafico è una retta; stato DEPLOYED → FOLLOWUP → SHOPASK, domanda aperta di storefront; scorrimenti a passo di
300 px, che è la distanza vera fra una scheda e l'altra, con un salto da 900 px oltre le due schede che il film non usa).
I punti della ricetta stanno alla stessa distanza dagli scorrimenti della ripresa del mattino (overview6), con le stesse
durate: le schede si fermano sugli stessi battiti (misurato: scarto verticale 0 px in tutte le soste).

  cd tools/promo/remotion/public/scenes
  ffmpeg -fflags +genpts -i ../../../out/clips/overview9.mp4 -vsync cfr -r 30 -c:v libx264 -crf 18 n_overview9_cut.mp4
  python3 ../../../fit-clip.py n_overview9_cut.mp4 n_overview_fit.mp4 \
    "0.0-2.03:2.0,2.03-4.3:1.27,4.3-6.13:2.0,6.13-8.53:1.0,8.53-10.33:1.7,10.33-12.23:0.75,12.23-13.83:1.2,13.83-15.67:0.71,15.67-17.47:5.58"

Ogni sosta dura quanto il conteggio del pannello accanto (il numero finisce di salire mentre la card è ancora ferma),
e ogni scheda si ferma da sola, al centro: Quota, 5-hour pace, Work, Open questions, Context.
Il risultato ha un fotogramma chiave ogni mezzo secondo: con quelli radi di screenrecord il film non riusciva a leggere
un fotogramma in tempo e il render si fermava su un delayRender scaduto (21/09).
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
           "-map", "[out]", "-r", "30", "-c:v", "libx264", "-crf", "18", "-g", "15", "-keyint_min", "15", "-sc_threshold", "0", "-pix_fmt", "yuv420p", dst]
    subprocess.run(cmd, check=True)
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", dst],
                         capture_output=True, text=True, check=True).stdout.strip()
    print(f"{dst}: {out} s")
    return 0

if __name__ == "__main__":
    sys.exit(main())
