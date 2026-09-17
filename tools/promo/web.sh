#!/usr/bin/env bash
# Una registrazione di screenrecord (frequenza variabile) in una clip web a 30 fps costanti, tagliata all'inizio.
# Uso: web.sh ingresso.mp4 uscita.mp4 SECONDI_DA_TAGLIARE [DURATA]
set -euo pipefail
dur=()
[ -n "${4:-}" ] && dur=(-t "$4")
ffmpeg -hide_banner -loglevel error -y -ss "$3" -i "$1" "${dur[@]}" -vf "fps=30,format=yuv420p" -fps_mode cfr -r 30 \
  -c:v libx264 -preset slow -crf 24 -movflags +faststart -an "$2"
