#!/usr/bin/env bash
# Mette una registrazione dello schermo dell'orologio (480x480) dentro la cornice di frame.py ed esporta
# MP4 (sito), WebM (sito) e GIF leggera (README). Uso: compose.sh clip.mp4 [DIR_CORNICE] [DIR_USCITA]
set -euo pipefail
clip=$1
here=$(cd "$(dirname "$0")" && pwd)
frame=${2:-$here/out}
out=${3:-$here/out}
[ -f "$frame/bg.png" ] || python3 "$here/frame.py" "$frame"
read -r sx sy sd < "$frame/geometry"
name=$(basename "${clip%.*}")

# Schermo ritagliato tondo con una maschera generata al volo, poi sopra lo sfondo con la cassa, poi il riflesso del vetro.
filter="[0:v]fps=30,scale=${sd}:${sd}:flags=lanczos,format=rgba,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='if(lte(hypot(X-${sd}/2,Y-${sd}/2),${sd}/2-1),255,0)'[scr];\
[1:v][scr]overlay=${sx}:${sy}:shortest=1[base];[base][2:v]overlay=0:0,format=yuv420p[v]"

ffmpeg -hide_banner -loglevel error -y -i "$clip" -loop 1 -i "$frame/bg.png" -loop 1 -i "$frame/glass.png" \
  -filter_complex "$filter" -map "[v]" -c:v libx264 -preset slow -crf 18 -movflags +faststart "$out/$name.mp4"
ffmpeg -hide_banner -loglevel error -y -i "$out/$name.mp4" -c:v libvpx-vp9 -b:v 0 -crf 34 -an "$out/$name.webm"
# GIF per GitHub: 480 px, 15 fps, tavolozza calcolata sulla clip.
ffmpeg -hide_banner -loglevel error -y -i "$out/$name.mp4" -vf "fps=15,scale=480:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=160[p];[b][p]paletteuse=dither=sierra2_4a" "$out/$name.gif"
ls -l --time-style=+%H:%M "$out/$name".{mp4,webm,gif} | awk '{print $5, $7}'
