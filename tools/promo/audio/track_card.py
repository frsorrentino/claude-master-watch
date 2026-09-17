"""Scheda misurata di una traccia candidata: tempo, primo battito, energia per battuta, dove cade la salita.
Uso: python3 track_card.py <file audio> [bpm minimo, bpm massimo] → <file>.card.json e una tabella a schermo."""
import json, sys, tempfile
from pathlib import Path
import numpy as np
import measure as M

src = Path(sys.argv[1])
with tempfile.TemporaryDirectory() as d:
    sr, x = M.read_wav(M.to_wav(src, Path(d) / "t.wav"))
lo, hi = (float(sys.argv[2]), float(sys.argv[3])) if len(sys.argv) > 3 else (80.0, 130.0)
env, times = M.onset_env(x, sr); bpm, first, top = M.tempo(env, times, lo, hi); bars = M.bar_energy_db(x, sr, bpm, first)
e = np.array(bars); rise = int(np.argmax(np.diff(e))) + 1                                        # la prima battuta al livello nuovo, dopo il salto di energia più grande
lufs, peak = M.loudness(src)
card = {"file": src.name, "seconds": round(len(x) / sr, 1), "bpm": bpm, "bpm_candidates": top, "first_beat_s": round(first, 3),
        "bars": len(bars), "bar_seconds": round(240 / bpm, 3), "bar_energy_db": bars, "biggest_rise_at_bar": rise,
        "intro_bars_below_body": int(np.argmax(e > np.median(e) - 3)), "lufs": lufs, "peak_dbfs": peak,
        "film_bars": round(66 / (240 / bpm), 1),
        # quanto è ritmata: attacchi raccolti sul battito in deviazioni standard dell'inviluppo, e quota di energia della cassa (40-120 Hz)
        "beat_salience": round(top[0][1] / float(env.std()), 2), "kick_band_db": round(M.band_db(x, sr, 40, 120), 1), "crest_db": round(M.crest_db(x), 1),
        "offbeat_ratio": round(M.offbeat_ratio(env, times, bpm, first), 2),
        # quanto è animata: attacchi distinti al secondo nel corpo della traccia (dal 25 % al 75 % della durata)
        "onsets_per_s": round(len([o for o in M.onsets(env, times, gap_s=0.06) if 0.25 * len(x) / sr < o < 0.75 * len(x) / sr]) / (0.5 * len(x) / sr), 2)}
Path(str(src) + ".card.json").write_text(json.dumps(card, indent=1))
print(f"{src.name}: {bpm} bpm (alternative {top[1:]}), primo battito a {first:.3f} s, {len(bars)} battute, salita alla battuta {rise}, introduzione rada per {card['intro_bars_below_body']} battute")
print(" ".join(f"{v:.0f}" for v in bars))
