#!/usr/bin/env python3
"""I suoni d'interfaccia del film, sintetizzati (Franz, 18/09: «click fotografico soft al glance, campanella leggera ma
crescente alla campanella»). Sintetizzati e non campionati per tre motivi: nessuna licenza da rispettare, si rifanno
identici cambiando un numero, e si accordano alla musica (110 bpm, tonalità di Sol) invece di litigarci.

Scrive `remotion/public/audio/sfx/*.wav` a 48 kHz, mono, 16 bit. I nomi sono quelli che `sound.ts` innesca.
uso: python3 sfx.py [--dir PERCORSO]
"""
import argparse
import pathlib
import wave

import numpy as np

SR = 48000
OUT = pathlib.Path(__file__).resolve().parent / "remotion/public/audio/sfx"
# la musica sta in Sol: le campanelle usano gradi di quella scala, così il suono non stona con il pezzo
G5, D6, B6 = 784.0, 1174.7, 1975.5


def env(n: int, attack: float, decay: float, curve: float = 2.5) -> np.ndarray:
    """Inviluppo attacco-decadimento in secondi; `curve` alto = coda che si spegne in fretta."""
    t = np.arange(n) / SR
    a = np.clip(t / max(attack, 1e-4), 0, 1)
    d = np.exp(-curve * np.clip((t - attack) / max(decay, 1e-4), 0, None))
    return a * d


def noise(n: int, seed: int) -> np.ndarray:
    return np.random.default_rng(seed).standard_normal(n)


def lowpass(x: np.ndarray, hz: float) -> np.ndarray:
    """Un polo, sufficiente per togliere il vetro dai click senza tirarsi dietro una libreria di filtri."""
    a = np.exp(-2 * np.pi * hz / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc = (1 - a) * v + a * acc
        y[i] = acc
    return y


def highpass(x: np.ndarray, hz: float) -> np.ndarray:
    return x - lowpass(x, hz)


def norm(x: np.ndarray, peak: float = 0.9) -> np.ndarray:
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def write(name: str, x: np.ndarray, out: pathlib.Path) -> None:
    out.mkdir(parents=True, exist_ok=True)
    data = (np.clip(norm(x), -1, 1) * 32767).astype("<i2")
    with wave.open(str(out / f"{name}.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def tick() -> np.ndarray:
    """Il tocco sul vetro: un click corto e opaco, niente scintillio."""
    n = int(0.05 * SR)
    body = lowpass(noise(n, 1), 2600) * env(n, 0.0008, 0.018, 4.0)
    tone = np.sin(2 * np.pi * 1200 * np.arange(n) / SR) * env(n, 0.0005, 0.012, 5.0) * 0.5
    return body + tone


def shutter() -> np.ndarray:
    """Il click fotografico del battito di ciglia: due scatti a 55 ms, il secondo più chiuso."""
    n = int(0.22 * SR)
    x = np.zeros(n)
    for k, (off, gain, hz) in enumerate(((0.0, 1.0, 3200), (0.055, 0.72, 2200))):
        i = int(off * SR)
        m = n - i
        x[i:] += gain * lowpass(noise(m, 10 + k), hz) * env(m, 0.0006, 0.014, 5.0)
    return x


def bell() -> np.ndarray:
    """La campanella della notifica: due rintocchi che CRESCONO (il secondo più forte e più alto), coda lunga."""
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for off, gain, f in ((0.0, 0.55, D6), (0.14, 1.0, B6)):
        i = int(off * SR)
        m = n - i
        e = env(m, 0.004, 0.42, 2.2)
        p = np.arange(m) / SR
        x[i:] += gain * e * (np.sin(2 * np.pi * f * p) + 0.42 * np.sin(2 * np.pi * f * 2.76 * p) + 0.18 * np.sin(2 * np.pi * f * 5.4 * p))
    return x * (0.6 + 0.4 * np.clip(t / 0.3, 0, 1))          # il crescendo chiesto da Franz


def thump() -> np.ndarray:
    """La vibrazione sotto la campanella: un tonfo grave che si sente più che sentirsi."""
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    f = 74 * np.exp(-6 * t)                                   # scende da 74 a ~40 Hz
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * env(n, 0.002, 0.09, 3.0)


def press_rise() -> np.ndarray:
    """La pressione lunga: un tono che sale mentre l'anello corre attorno al tasto."""
    n = int(0.62 * SR)
    t = np.arange(n) / SR
    f = G5 * (1 + 0.5 * (t / t[-1]) ** 1.6)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.7 + np.sin(4 * np.pi * np.cumsum(f) / SR) * 0.12
    return body * env(n, 0.05, 0.5, 1.2) * np.clip(t / 0.12, 0, 1)


def whoosh() -> np.ndarray:
    """Il soffio sotto un titolo che entra: rumore filtrato che passa e se ne va, senza sibilo."""
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    x = highpass(lowpass(noise(n, 7), 1800), 180)
    return x * np.exp(-((t - 0.16) ** 2) / 0.006)


SOUNDS = {"tick": tick, "shutter": shutter, "notify": bell, "thump": thump, "pressRise": press_rise, "whoosh": whoosh}

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=str(OUT))
    a = ap.parse_args()
    out = pathlib.Path(a.dir)
    for name, fn in SOUNDS.items():
        x = fn()
        write(name, x, out)
        print(f"{name}.wav · {len(x) / SR:.2f} s")
