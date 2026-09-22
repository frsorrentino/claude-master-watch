#!/usr/bin/env bash
# uso: speaker_fx.sh voce.wav uscita_leggera.wav uscita_forte.wav
set -euo pipefail
fx="highpass=f=450,lowpass=f=3600,acompressor=threshold=-22dB:ratio=4:attack=4:release=60,volume=4dB"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.65 0.35':normalize=0" "$2"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.30 0.70':normalize=0" "$3"
# 22/09 (Franz: «aumentiamo l'effetto cassa piccola»): la voce del film è ripassata nel filtro partendo da quella leggera
# (la sorgente pulita del 19/09 non c'è più), 15 % com'era e 85 % filtrata, riportata a -18,9 LUFS con picco a -1 dB:
#   fx=<la catena qui sopra>; ffmpeg -i question_leggera.wav -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.15 0.85':normalize=0" q.wav
#   ffmpeg -i q.wav -af "volume=6.6dB,alimiter=limit=0.89:level=false" -ar 48000 question.wav
