"""Loudness di consegna: guadagno COSTANTE fino a −14 LUFS integrati, poi un limitatore solo sui picchi (True Peak ≤ −1 dB).
Perché non `loudnorm`: con questo mix il modo `linear` non riesce a rispettare il picco e ripiega da solo sul modo dinamico,
che alzava l'apertura quieta di circa 6 dB più del resto (misurato il 22/09 su film.tethys.wav: +11,2 dB contro +4,4/+5,7).
Qui il guadagno è lo stesso in ogni punto del film; il limitatore tocca solo i transienti sopra −1,5 dBFS.
Uso: python3 normalize.py <ingresso.wav> <uscita.wav> [LUFS=-14] [TP=-1]"""
import json, re, subprocess, sys


def measure(path: str) -> tuple[float, float]:
    """Loudness integrata (LUFS) e True Peak (dBTP) secondo EBU R128, da ffmpeg."""
    err = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", path, "-af", "ebur128=peak=true", "-f", "null", "-"],
                         capture_output=True, text=True).stderr
    summary = err[err.rindex("Summary:"):]
    return float(re.search(r"I:\s+(-?[\d.]+) LUFS", summary).group(1)), float(re.search(r"Peak:\s+(-?[\d.]+) dBFS", summary).group(1))


def render(src: str, dst: str, gain_db: float, ceiling_db: float) -> None:
    limit = 10 ** (ceiling_db / 20)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", src, "-af",
                    f"volume={gain_db:.3f}dB,alimiter=limit={limit:.4f}:attack=5:release=50:level=false:asc=true,aresample=48000",
                    "-c:a", "pcm_s16le", dst], check=True)


def normalize(src: str, dst: str, target: float = -14.0, tp_max: float = -1.0) -> dict:
    i0, tp0 = measure(src)
    gain = target - i0
    ceiling = tp_max - 0.5                       # margine: il limitatore guarda i campioni, il True Peak anche tra un campione e l'altro
    for _ in range(4):                           # il limitatore toglie un filo di loudness: si ricompensa col guadagno, sempre costante
        render(src, dst, gain, ceiling)
        i, tp = measure(dst)
        if abs(i - target) <= 0.1: break
        gain += target - i
    if not (abs(i - target) <= 0.2 and tp <= tp_max): raise SystemExit(f"normalizzazione fallita: {i} LUFS, {tp} dBTP")
    return {"ingresso_lufs": i0, "ingresso_tp": tp0, "guadagno_db": round(gain, 2), "uscita_lufs": i, "uscita_tp": tp}


if __name__ == "__main__":
    args = sys.argv[1:]
    print(json.dumps(normalize(args[0], args[1], *(float(a) for a in args[2:4]))))
