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
    """La vibrazione al polso (Franz, 23:26: «non somiglia a una vibrazione»). Non è un tonfo grave: è un motore lineare,
    che gira attorno ai 180 Hz con l'ampiezza modulata a ~55 Hz — il ronzio ruvido che si sente sulla cassa — in due
    impulsi come la notifica di Wear OS, ciascuno con attacco e stacco netti."""
    n = int(0.42 * SR)
    t = np.arange(n) / SR
    carrier = np.sin(2 * np.pi * 180 * t) + 0.35 * np.sin(2 * np.pi * 360 * t)
    rough = 0.72 + 0.28 * np.sin(2 * np.pi * 55 * t)          # il motore non è liscio: batte
    gate = np.zeros(n)
    for off, dur in ((0.0, 0.115), (0.185, 0.095)):           # bzz-bzz
        i, j = int(off * SR), int((off + dur) * SR)
        m = j - i
        gate[i:j] = np.clip(np.arange(m) / (0.008 * SR), 0, 1) * np.clip((m - np.arange(m)) / (0.02 * SR), 0, 1)
    body = 0.22 * lowpass(noise(n, 3), 400)                   # il corpo dell'orologio che risuona
    return (carrier * rough + body) * gate


def press_rise() -> np.ndarray:
    """La pressione lunga, variante A: micro-tocchi che accelerano mentre l'anello corre attorno al tasto, e un click
    secco quando si chiude. È un feedback aptico ripetuto, non una nota che sale."""
    n = int(0.66 * SR)
    x = np.zeros(n)
    # numero fisso di tocchi, non un `while` sulla somma: i ritardi si stringono in progressione geometrica e la loro
    # somma converge (0,105·0,82/(1−0,82) ≈ 0,48 s), quindi una condizione sul tempo non finirebbe mai
    offs, gap, tpos = [], 0.105, 0.0
    for _ in range(9):
        offs.append(tpos)
        tpos += gap
        gap *= 0.82
    for k, off in enumerate(offs):
        i = int(off * SR)
        m = min(int(0.05 * SR), n - i)
        g = 0.45 + 0.55 * (k / max(1, len(offs) - 1))
        x[i:i + m] += g * lowpass(noise(m, 20 + k), 2000) * env(m, 0.0006, 0.012, 5.0)
    i = int(0.585 * SR)
    m = n - i
    x[i:] += 1.0 * lowpass(noise(m, 99), 3000) * env(m, 0.0006, 0.03, 3.0)   # il click di conferma
    return x


def press_hum() -> np.ndarray:
    """La pressione lunga, variante B: un ronzio sordo che si apre (il filtro sale con l'anello) e si chiude in un click.
    Meno «interfaccia», più fisico."""
    n = int(0.66 * SR)
    t = np.arange(n) / SR
    raw = noise(n, 31)
    slow = lowpass(raw, 300)
    open_ = lowpass(raw, 1500)
    k = np.clip(t / 0.5, 0, 1) ** 1.4
    x = (slow * (1 - k) + open_ * k) * (0.25 + 0.75 * k)
    hum = 0.3 * np.sin(2 * np.pi * 110 * t) * k
    i = int(0.585 * SR)
    m = n - i
    click = np.zeros(n)
    click[i:] = lowpass(noise(m, 32), 3000) * env(m, 0.0006, 0.03, 3.0)
    return (x + hum) * np.clip((0.62 - t) / 0.06, 0, 1) + click


def whoosh() -> np.ndarray:
    """Il soffio sotto un titolo che entra: rumore filtrato che passa e se ne va, senza sibilo."""
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    x = highpass(lowpass(noise(n, 7), 1800), 180)
    return x * np.exp(-((t - 0.16) ** 2) / 0.006)


SOUNDS = {"tick": tick, "shutter": shutter, "notify": bell, "thump": thump, "pressRise": press_rise, "pressHum": press_hum, "whoosh": whoosh}

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=str(OUT))
    ap.add_argument("--only", nargs="*", help="rigenera solo questi suoni (la macchina ha 4 GB: con un render in corso conviene)")
    a = ap.parse_args()
    out = pathlib.Path(a.dir)
    for name, fn in ({k: v for k, v in SOUNDS.items() if k in a.only} if a.only else SOUNDS).items():
        x = fn()
        write(name, x, out)
        print(f"{name}.wav · {len(x) / SR:.2f} s")
