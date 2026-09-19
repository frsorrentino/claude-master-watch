#!/usr/bin/env python3
"""Sposta la pausa del brano dove serve al film, senza accorciarlo.

Il brano ha due stacchi brevi (0,5 s) a 24,04 s e 28,40 s, cioè agli inizi di battuta 11 e 13. Il film li vorrebbe sotto
lo zoom della complication, che cade al battito 13,5 (7,36 s dall'inizio del montato, cioè 13,9 s di traccia visto che il
film salta le prime tre battute). Si toglie un numero intero di battute PRIMA della pausa e se ne rimette altrettante
DOPO, in un punto dove il brano si ripete: la pausa arriva prima, la durata totale non cambia, e i tagli cadono sui
confini di battuta con una dissolvenza corta, dove il brano ha già i suoi stacchi.

uso: sposta_pausa.py [--bars 5] [--cut 8.73] [--paste 43.64] [--out music.v8.wav]
"""
import argparse
import pathlib
import wave

import numpy as np

HERE = pathlib.Path(__file__).resolve().parents[1] / "remotion/public/audio"
BPM = 110.0
BAR = 4 * 60 / BPM          # 2,1818 s
XF = 0.030                  # dissolvenza sulle giunte: 30 ms, un battito di ciglia


def read(p: pathlib.Path) -> tuple[int, np.ndarray]:
    with wave.open(str(p)) as w:
        sr, n, ch = w.getframerate(), w.getnframes(), w.getnchannels()
        x = np.frombuffer(w.readframes(n), dtype="<i2").astype(np.float32) / 32767
    return sr, x.reshape(-1, ch).mean(1) if ch == 2 else x


def write(p: pathlib.Path, sr: int, x: np.ndarray) -> None:
    with wave.open(str(p), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())


def join(a: np.ndarray, b: np.ndarray, sr: int) -> np.ndarray:
    """Attacca `b` dopo `a` con una dissolvenza incrociata: senza, la giunta fa un click."""
    n = int(XF * sr)
    if len(a) < n or len(b) < n:
        return np.concatenate([a, b])
    ramp = np.linspace(0, 1, n, dtype=np.float32)
    mid = a[-n:] * (1 - ramp) + b[:n] * ramp
    return np.concatenate([a[:-n], mid, b[n:]])


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default=str(HERE / "music.wav"))
    ap.add_argument("--out", default=str(HERE / "music.v8.wav"))
    ap.add_argument("--bars", type=float, default=5, help="quante battute togliere prima della pausa")
    ap.add_argument("--cut", type=float, default=4 * BAR, help="secondo da cui togliere (confine di battuta)")
    ap.add_argument("--paste", type=float, default=20 * BAR, help="secondo in cui rimettere le battute (confine di battuta)")
    a = ap.parse_args()
    sr, x = read(pathlib.Path(a.src))
    n = lambda s: int(round(s * sr))
    cut0, cut1 = n(a.cut), n(a.cut + a.bars * BAR)
    keep = np.concatenate([x[:cut0], x[cut1:]])                      # senza le battute tolte
    paste = n(a.paste - a.bars * BAR)                                # il punto di reinserimento scala con il taglio
    filler = x[n(a.paste): n(a.paste + a.bars * BAR)]                # le battute da ripetere, prese dopo la pausa
    out = join(join(keep[:paste], filler, sr), keep[paste:], sr)
    write(pathlib.Path(a.out), sr, out)
    print(f"{a.out}: {len(out) / sr:.2f} s (originale {len(x) / sr:.2f})")
    print(f"  tolte {a.bars:g} battute da {a.cut:.2f} s, rimesse a {a.paste:.2f} s")
    print(f"  la pausa di 24,04 s si sposta a {24.04 - a.bars * BAR:.2f} s, cioè al battito {(24.04 - a.bars * BAR - 6.528) / (60 / BPM):.1f} del film")
