#!/usr/bin/env bash
# Mette una registrazione dello schermo dell'orologio (480x480) dentro la cornice di frame.py, con lo zoom d'apertura dei
# video di prodotto: si parte stretti sullo schermo e ci si allarga fino all'orologio intero.
# Uso: compose.sh clip.mp4 [DIR_CORNICE] [DIR_USCITA]     FINAL=1 per WebM di qualità (lento su ARM)
set -euo pipefail
clip=$1
here=$(cd "$(dirname "$0")" && pwd)
frame=${2:-$here/out}
out=${3:-$here/out}
[ -f "$frame/mask.png" ] || python3 "$here/frame.py" "$frame" --width 2880
read -r sx sy sd W H < "$frame/geometry"
name=$(basename "${clip%.*}")
Z0=${ZOOM_FROM:-1.7}; ZT=${ZOOM_SECONDS:-1.8}

# 1. schermo tondo sopra cornice e vetro, sulla tela di frame.py (1,5 volte l'uscita, così lo zoom non perde dettaglio);
# 2. zoom con `zoompan` a uscita fissa, centrato, su un easing cubico dal fotogramma 0 a ZT secondi. `scale` a misura
#    variabile più `crop` spostava l'orologio fuori centro: `crop` fissa le misure al primo fotogramma (16/09 17:27).
OW=${OUT_WIDTH:-1920}; OH=$((OW / 2))
filter="[0:v]fps=30,scale=${sd}:${sd}:flags=lanczos,format=rgba[raw];[3:v]format=gray[msk];[raw][msk]alphamerge[scr];\
[1:v][scr]overlay=${sx}:${sy}:shortest=1[base];[base][2:v]overlay=0:0:shortest=1[comp];\
[comp]zoompan=z='1+(${Z0}-1)*pow(max(0\,1-on/(30*${ZT}))\,3)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s=${OW}x${OH}:fps=30,format=yuv420p[v]"

# Le immagini fisse entrano in loop: senza `-t` alla durata della clip il secondo overlay non finiva mai (16/09 17:25).
# START: secondi da tagliare all'inizio. Le prime frazioni di ogni registrazione mostrano l'avvio a freddo e quello che
# c'era prima sullo schermo (il 16/09 anche le impostazioni del debug wireless con indirizzo e porta): si tagliano sempre.
START=${START:-0}
dur=$(python3 -c "print(max(0.5, $(ffprobe -v error -show_entries format=duration -of csv=p=0 "$clip") - $START))")
ffmpeg -hide_banner -loglevel error -y -ss "$START" -i "$clip" -loop 1 -t "$dur" -i "$frame/bg.png" -loop 1 -t "$dur" -i "$frame/glass.png" -loop 1 -t "$dur" -i "$frame/mask.png" \
  -filter_complex "$filter" -map "[v]" -c:v libx264 -preset medium -crf 18 -movflags +faststart "$out/$name.mp4"
if [ "${FINAL:-0}" = 1 ]; then vp9="-deadline good -cpu-used 2"; else vp9="-deadline realtime -cpu-used 6"; fi
ffmpeg -hide_banner -loglevel error -y -i "$out/$name.mp4" -c:v libvpx-vp9 -b:v 0 -crf 34 $vp9 -an "$out/$name.webm"
# GIF per GitHub: metà larghezza, 15 fps, tavolozza calcolata sulla clip (senza, i pastelli fanno banding).
ffmpeg -hide_banner -loglevel error -y -i "$out/$name.mp4" -vf "fps=15,scale=$((OW / 2)):-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=192[p];[b][p]paletteuse=dither=sierra2_4a" "$out/$name.gif"
ls -l "$out/$name".{mp4,webm,gif} | awk '{print $5, $9}'
