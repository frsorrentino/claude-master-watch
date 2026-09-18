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


def reson(x: np.ndarray, hz: float, q: float) -> np.ndarray:
    """Risonatore a due poli: fa cantare il rumore attorno a `hz`. È così che suona un motore, non con un seno puro."""
    r = np.exp(-np.pi * hz / (q * SR))
    a1, a2 = 2 * r * np.cos(2 * np.pi * hz / SR), -r * r
    y = np.zeros_like(x)
    y1 = y2 = 0.0
    for i, v in enumerate(x):
        acc = (1 - r) * v + a1 * y1 + a2 * y2
        y[i] = acc
        y2, y1 = y1, acc
    return y


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
    """La vibrazione al polso, terza versione (Franz, 23:45: «meglio altro suono»). Non più seni: è **rumore fatto
    risuonare** attorno a 165 Hz, con un secondo risuonatore basso per il corpo della cassa — il modo in cui suona
    davvero un motore lineare contro il polso. Due impulsi, il secondo più corto, con bordi morbidi e una coda breve."""
    n = int(0.56 * SR)
    t = np.arange(n) / SR
    src = noise(n, 5)
    motor = reson(src, 165, 14) * 4.2 + reson(src, 330, 9) * 1.1      # il motore e la sua seconda
    body = reson(src, 78, 5) * 2.4                                    # la cassa, sorda
    gate = np.zeros(n)
    for off, dur in ((0.0, 0.19), (0.255, 0.15)):
        i, j = int(off * SR), int((off + dur) * SR)
        m = j - i
        gate[i:j] = np.clip(np.arange(m) / (0.03 * SR), 0, 1) ** 0.6 * np.clip((m - np.arange(m)) / (0.07 * SR), 0, 1) ** 0.9
    return (motor + body) * gate * (0.85 + 0.15 * np.sin(2 * np.pi * 31 * t))


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


def press_two() -> np.ndarray:
    """Pressione lunga, variante C (Franz, 23:43: le prime due non convincono): la pressione NON suona. Un tocco morbido
    quando il dito appoggia, silenzio mentre l'anello corre, e un click pieno con un filo di nota quando si conferma."""
    n = int(0.68 * SR)
    x = np.zeros(n)
    m = int(0.08 * SR)
    x[:m] += 0.5 * lowpass(noise(m, 41), 1400) * env(m, 0.004, 0.03, 3.0)         # il dito appoggia: opaco
    i = int(0.56 * SR)
    m = n - i
    p = np.arange(m) / SR
    x[i:] += lowpass(noise(m, 42), 2600) * env(m, 0.0008, 0.02, 4.0)              # il click
    x[i:] += 0.35 * np.sin(2 * np.pi * G5 * p) * env(m, 0.003, 0.09, 3.0)         # e la sua nota, in Sol
    return x


def press_swell() -> np.ndarray:
    """Pressione lunga, variante D: un respiro sordo che cresce da quasi niente mentre l'anello corre — nessun tono,
    niente ronzio — e lo stesso click di conferma."""
    n = int(0.68 * SR)
    t = np.arange(n) / SR
    k = np.clip(t / 0.54, 0, 1) ** 2.2
    x = lowpass(noise(n, 43), 700) * k * 0.55
    i = int(0.56 * SR)
    m = n - i
    p = np.arange(m) / SR
    click = np.zeros(n)
    click[i:] = lowpass(noise(m, 44), 2600) * env(m, 0.0008, 0.02, 4.0) + 0.3 * np.sin(2 * np.pi * G5 * p) * env(m, 0.003, 0.08, 3.0)
    return x * np.clip((0.57 - t) / 0.05, 0, 1) + click


def whoosh() -> np.ndarray:
    """Il soffio sotto un titolo che entra: rumore filtrato che passa e se ne va, senza sibilo."""
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    x = highpass(lowpass(noise(n, 7), 1800), 180)
    return x * np.exp(-((t - 0.16) ** 2) / 0.006)


SOUNDS = {"tick": tick, "shutter": shutter, "notify": bell, "thump": thump, "pressRise": press_rise, "pressHum": press_hum, "pressTwo": press_two, "pressSwell": press_swell, "whoosh": whoosh}

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
