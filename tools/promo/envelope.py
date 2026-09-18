#!/usr/bin/env python3
"""Inviluppo RMS a 30 Hz di una traccia vocale, normalizzato al picco, per la forma d'onda del film (`UiWave`).
Uso: envelope.py remotion/public/audio/question.wav   →   scrive question.env.json accanto ({fps, peak, env[]}).
"""
import json, sys, wave
from pathlib import Path
import numpy as np

src = Path(sys.argv[1]); fps = 30
with wave.open(str(src)) as w:
    sr, ch, sw = w.getframerate(), w.getnchannels(), w.getsampwidth()
    a = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16 if sw == 2 else np.int32).astype(float)
if ch > 1: a = a.reshape(-1, ch).mean(axis=1)
a /= 32768 if sw == 2 else 2 ** 31
hop = sr / fps
env = [float(np.sqrt((a[int(i * hop):int((i + 1) * hop)] ** 2).mean())) for i in range(int(len(a) / hop))]
peak = max(env) or 1
out = src.with_suffix(".env.json")
json.dump({"fps": fps, "peak": round(peak, 4), "env": [round(v / peak, 3) for v in env]}, out.open("w"))
print(f"{out}: {len(env)} fotogrammi, picco RMS {peak:.3f}")
