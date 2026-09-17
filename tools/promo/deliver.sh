#!/usr/bin/env bash
# Consegna del film: video e audio si rendono separati e li unisce ffmpeg.
# Perché: l'MP4 che esce da Remotion ha l'audio AAC senza compensazione del ritardo d'innesco del codificatore (2048 campioni
# a 48 kHz = 42,7 ms, misurato il 17/09 con la correlazione sul file tagliato): la musica arriverebbe 43 ms dopo i tagli.
# L'audio reso in WAV è a 0,0 ms, e l'unione con ffmpeg scrive la lista di montaggio giusta (rimisurato: 0,0 ms).
# uso: deliver.sh <nome> [--scale=0.5]
set -euo pipefail
cd "$(dirname "$0")/remotion"
name="$1"; shift || true
mkdir -p out/consegna
npx remotion render Film "out/consegna/$name.video.mp4" --muted "$@"
npx remotion render Film "out/consegna/$name.wav"
ffmpeg -v error -y -i "out/consegna/$name.video.mp4" -i "out/consegna/$name.wav" -map 0:v -map 1:a -c:v copy -c:a aac -b:a 256k -movflags +faststart "out/consegna/$name.mp4"
ffmpeg -v error -y -i "out/consegna/$name.mp4" -an -c:v copy "out/consegna/$name.muto.mp4"
ffprobe -v error -show_entries format=duration,size -of csv=p=0 "out/consegna/$name.mp4"
