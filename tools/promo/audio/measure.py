"""Misure audio con numpy e wave soltanto (lo scipy di sistema non funziona con numpy 2.4.6). Claude non sente: misura."""
import re, subprocess, wave
import numpy as np

HOP_S = 0.005          # passo NOMINALE dell'inviluppo: quello vero è un numero intero di campioni, e lo dicono i tempi restituiti
ENV_BIAS_S = -0.0057   # l'inviluppo anticipa l'attacco vero di 5,7 ms (misurato su clic a 97, 100 e 108 battiti: −5,6/−5,8/−5,7): costante dello strumento, non della traccia

def read_wav(path):
    with wave.open(str(path), "rb") as w:
        sr, ch, sw, n = w.getframerate(), w.getnchannels(), w.getsampwidth(), w.getnframes()
        if sw != 2: raise ValueError("solo 16 bit: passare da to_wav()")
        x = np.frombuffer(w.readframes(n), dtype="<i2").astype(np.float32) / 32768.0
    return sr, x.reshape(-1, ch).mean(axis=1)

def to_wav(src, dst, sr=44100):
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(src), "-ac", "1", "-ar", str(sr), "-c:a", "pcm_s16le", str(dst)], check=True); return dst

def rms_db(x): return 20 * np.log10(max(float(np.sqrt(np.mean(np.square(x, dtype=np.float64)))), 1e-9))
def crest_db(x): return 20 * np.log10(max(float(np.max(np.abs(x))), 1e-9)) - rms_db(x)

def band_db(x, sr, lo, hi):
    X = np.abs(np.fft.rfft(x)) ** 2; f = np.fft.rfftfreq(len(x), 1 / sr); sel = (f >= lo) & (f < hi)
    return 10 * np.log10(max(float(X[sel].sum() / max(X.sum(), 1e-20)), 1e-12))

def onset_env(x, sr, n=1024, block=2000):
    """Flusso spettrale positivo su spettro compresso; i tempi sono il centro della finestra, corretti di ENV_BIAS_S."""
    hop = int(sr * HOP_S); w = np.hanning(n).astype(np.float32); k = 1 + (len(x) - n) // hop; prev = None; flux = []
    for s in range(0, k, block):                                  # a blocchi: una traccia di 3 minuti non deve occupare 1 GB
        idx = np.arange(n)[None, :] + hop * np.arange(s, min(k, s + block))[:, None]
        S = np.log1p(50 * np.abs(np.fft.rfft(x[idx] * w, axis=1)))
        if prev is not None: S = np.vstack([prev, S])
        flux.append(np.maximum(S[1:] - S[:-1], 0).sum(axis=1)); prev = S[-1:]
    env = np.r_[0.0, np.concatenate(flux)]
    return env, (np.arange(len(env)) * hop + n / 2) / sr - ENV_BIAS_S

def tempo(env, times, lo=80.0, hi=130.0, step=0.05):
    """Per ogni tempo candidato, la fase che raccoglie più attacchi. Restituisce bpm, primo battito (s), i tre migliori."""
    e = env - env.mean(); t = np.arange(len(e)); hop_s = float(times[1] - times[0]); res = []      # 220 campioni a 44,1 kHz sono 4,989 ms, non 5: con 5 tondi il tempo esce sbagliato dello 0,23 %
    for bpm in np.arange(lo, hi, step):
        period = 60 / bpm / hop_s; ph = np.arange(0, period, 0.5)
        beats = ph[:, None] + period * np.arange(int(len(e) / period) - 1)[None, :]
        sc = np.interp(beats, t, e).mean(axis=1); i = int(sc.argmax()); res.append((float(sc[i]), float(bpm), float(ph[i])))
    res.sort(reverse=True); _, bpm, ph = res[0]
    first = float(np.interp(ph, t, times))
    top = []
    for s, b, _ in res:
        if all(abs(b - q) > 1 for q, _ in top): top.append((round(b, 2), round(s, 3)))
        if len(top) == 3: break
    return round(bpm, 2), first, top

def onsets(env, times, k=2.5, gap_s=0.12):
    thr = np.median(env) + k * env.std(); out = []
    for i in range(1, len(env) - 1):
        if env[i] > thr and env[i] >= env[i - 1] and env[i] > env[i + 1] and (not out or times[i] - out[-1] > gap_s): out.append(float(times[i]))
    return out

def bar_energy_db(x, sr, bpm, first_beat_s, beats_per_bar=4):
    bar = int(sr * 60 / bpm * beats_per_bar); s = int(first_beat_s * sr)
    return [round(rms_db(x[i:i + bar]), 1) for i in range(s, len(x) - bar + 1, bar)]

def loudness(path):
    err = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(path), "-af", "ebur128=peak=true", "-f", "null", "-"], capture_output=True, text=True).stderr
    tail = err[err.rfind("Summary:"):]
    return float(re.search(r"I:\s+(-?[\d.]+) LUFS", tail).group(1)), float(re.search(r"Peak:\s+(-?[\d.]+) dBFS", tail).group(1))

def offbeat_ratio(env, times, bpm, first_beat_s):
    """Quanto il ritmo è spezzato: forza media degli attacchi sui sedicesimi in levare (¼ e ¾ di battito) divisa per quella
    sui battiti. Cassa dritta: vicino a 0; ritmo sincopato o spezzato: sale."""
    e = np.maximum(env - np.median(env), 0); beat = 60 / bpm; n = int((times[-1] - first_beat_s) / beat) - 1
    at = lambda frac: float(np.interp(first_beat_s + (np.arange(n) + frac) * beat, times, e).mean())
    return (at(0.25) + at(0.75)) / 2 / max(at(0.0), 1e-9)
