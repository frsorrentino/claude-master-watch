#!/usr/bin/env bash
# uso: speaker_fx.sh voce.wav uscita_leggera.wav uscita_forte.wav
set -euo pipefail
fx="highpass=f=450,lowpass=f=3600,acompressor=threshold=-22dB:ratio=4:attack=4:release=60,volume=4dB"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.65 0.35':normalize=0" "$2"
ffmpeg -v error -y -i "$1" -filter_complex "[0:a]asplit[c][s];[s]${fx}[t];[c][t]amix=inputs=2:weights='0.30 0.70':normalize=0" "$3"
