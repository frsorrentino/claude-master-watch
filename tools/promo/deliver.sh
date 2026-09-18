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
# GPU virtuale della VM (virgl): con --gl=egl il render va 2,5-3× più veloce di SwiftShader, qualità identica (master, 18/09 16:26:
# 84 pixel di antialiasing su 2 milioni). GL=swangle per tornare al software in un colpo.
GL=${GL:-egl}
# I tratti con 3D (ThreeCanvas) non rendono con egl: il contesto WebGL non si crea, serve swangle, che però costa 2,6 s a
# fotogramma contro 0,6. Si rende il film in egl, i soli tratti 3D in swangle e si monta: 32 minuti invece di 96 (master, 22:24).
# `gl3d.py` legge gli intervalli dalla scaletta, così non c'è un elenco da tenere aggiornato a mano.
GL3D=${GL3D:-swangle}
VCODEC=(--codec h264 --crf 18)
mapfile -t tratti < <(python3 ../gl3d.py)
if [ "${#tratti[@]}" -eq 0 ] || [ "$GL" = "$GL3D" ]; then
  npx remotion render Film "out/consegna/$name.video.mp4" --muted --gl="$GL" "${VCODEC[@]}" "$@"
else
  total=$(python3 ../gl3d.py --total); ultimo=$((total - 1)); cur=0; n=0; rm -rf out/segmenti; mkdir -p out/segmenti
  : > out/segmenti/lista.txt
  extra=("$@")
  seg() { # seg <primo> <ultimo> <renderer>
    local f="out/segmenti/$(printf '%03d' "$n").mp4"
    npx remotion render Film "$f" --muted --gl="$3" --frames="$1-$2" "${VCODEC[@]}" ${extra[@]+"${extra[@]}"}
    echo "file '$(basename "$f")'" >> out/segmenti/lista.txt; n=$((n + 1))
  }
  for r in "${tratti[@]}"; do
    read -r a b <<< "$r"
    [ "$cur" -lt "$a" ] && seg "$cur" "$((a - 1))" "$GL"
    seg "$a" "$b" "$GL3D"
    cur=$((b + 1))
  done
  [ "$cur" -le "$ultimo" ] && seg "$cur" "$ultimo" "$GL"
  ffmpeg -v error -y -f concat -safe 0 -i out/segmenti/lista.txt -c copy "out/consegna/$name.video.mp4"
  atteso=$total; reso=$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of default=nw=1:nk=1 "out/consegna/$name.video.mp4")   # csv=p=0 lascia una virgola in coda e il confronto fallisce sempre
  [ "$reso" = "$atteso" ] || { echo "montaggio sbagliato: $reso fotogrammi invece di $atteso"; exit 1; }
fi
# anche il render dell'"'"'audio monta i componenti, quindi inciampa sul ThreeCanvas: se c'"'"'è 3D in scaletta va in swangle
npx remotion render Film "out/consegna/$name.wav" --gl="$([ "${#tratti[@]}" -gt 0 ] && echo "$GL3D" || echo "$GL")" "$@"
# Loudness finale a due passate: prima si misura, poi si applica in modo lineare (niente compressione): -14 LUFS, picco -1 dB.
m=$(ffmpeg -hide_banner -nostats -i "out/consegna/$name.wav" -af loudnorm=I=-14:TP=-1:LRA=11:print_format=json -f null - 2>&1 | sed -n '/^{/,/^}/p')
g() { echo "$m" | python3 -c "import json,sys; print(json.load(sys.stdin)['$1'])"; }
ffmpeg -v error -y -i "out/consegna/$name.wav" -af "loudnorm=I=-14:TP=-1:LRA=11:measured_I=$(g input_i):measured_TP=$(g input_tp):measured_LRA=$(g input_lra):measured_thresh=$(g input_thresh):offset=$(g target_offset):linear=true" -ar 48000 "out/consegna/$name.norm.wav"
ffmpeg -v error -y -i "out/consegna/$name.video.mp4" -i "out/consegna/$name.norm.wav" -map 0:v -map 1:a -c:v copy -c:a aac -b:a 256k -movflags +faststart "out/consegna/$name.mp4"
ffmpeg -v error -y -i "out/consegna/$name.mp4" -an -c:v copy "out/consegna/$name.muto.mp4"
ffprobe -v error -show_entries format=duration,size -of csv=p=0 "out/consegna/$name.mp4"
