"""Taglia la traccia sulle battute (introduzione, corpo, salto al finale vero) con dissolvenze a pari potenza di 15 ms
centrate sul battito: la griglia resta uniforme attraverso le giunte e la musica finisce con il film, non sfuma.
Uso: python3 cut_track.py <traccia> <scheda.card.json> 0-4 10-24 40-47"""
import json, sys, tempfile, wave
from pathlib import Path
import numpy as np
import measure as M

def cut(x, sr, bpm, first_beat_s, segments, fade_ms=15):
    bar = 240 / bpm; h = int(sr * fade_ms / 2000); out = None
    for a, b in segments:
        i0 = int((first_beat_s + a * bar) * sr); i1 = int((first_beat_s + b * bar) * sr)
        if out is None: out = x[:i1 + h].copy() if a == 0 else x[max(0, i0 - h):i1 + h].copy(); continue
        seg = x[i0 - h:i1 + h]; t = np.linspace(0, np.pi / 2, 2 * h)
        out[-2 * h:] = out[-2 * h:] * np.cos(t) + seg[:2 * h] * np.sin(t); out = np.r_[out, seg[2 * h:]]
    return out[:len(out) - h]

if __name__ == "__main__":
    src, card = Path(sys.argv[1]), json.loads(Path(sys.argv[2]).read_text()); segs = [tuple(int(v) for v in s.split("-")) for s in sys.argv[3:]]
    with tempfile.TemporaryDirectory() as d: sr, x = M.read_wav(M.to_wav(src, Path(d) / "t.wav"))
    y = cut(x, sr, card["bpm"], card["first_beat_s"], segs)
    dst = Path(__file__).resolve().parent.parent / "remotion/public/audio/music.wav"; dst.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(dst), "wb") as w: w.setnchannels(1); w.setsampwidth(2); w.setframerate(sr); w.writeframes((np.clip(y, -1, 1) * 32767).astype("<i2").tobytes())
    print(f"{dst}: {len(y) / sr:.2f} s · bpm {card['bpm']} · offsetSeconds {card['first_beat_s']}")
