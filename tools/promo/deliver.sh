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
npx remotion render Film "out/consegna/$name.video.mp4" --muted --gl="$GL" "$@"
npx remotion render Film "out/consegna/$name.wav" --gl="$GL" "$@"
# Loudness finale a due passate: prima si misura, poi si applica in modo lineare (niente compressione): -14 LUFS, picco -1 dB.
m=$(ffmpeg -hide_banner -nostats -i "out/consegna/$name.wav" -af loudnorm=I=-14:TP=-1:LRA=11:print_format=json -f null - 2>&1 | sed -n '/^{/,/^}/p')
g() { echo "$m" | python3 -c "import json,sys; print(json.load(sys.stdin)['$1'])"; }
ffmpeg -v error -y -i "out/consegna/$name.wav" -af "loudnorm=I=-14:TP=-1:LRA=11:measured_I=$(g input_i):measured_TP=$(g input_tp):measured_LRA=$(g input_lra):measured_thresh=$(g input_thresh):offset=$(g target_offset):linear=true" -ar 48000 "out/consegna/$name.norm.wav"
ffmpeg -v error -y -i "out/consegna/$name.video.mp4" -i "out/consegna/$name.norm.wav" -map 0:v -map 1:a -c:v copy -c:a aac -b:a 256k -movflags +faststart "out/consegna/$name.mp4"
ffmpeg -v error -y -i "out/consegna/$name.mp4" -an -c:v copy "out/consegna/$name.muto.mp4"
ffprobe -v error -show_entries format=duration,size -of csv=p=0 "out/consegna/$name.mp4"
