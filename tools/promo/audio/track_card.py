"""Scheda misurata di una traccia candidata: tempo, primo battito, energia per battuta, dove cade la salita.
Uso: python3 track_card.py <file audio> → <file>.card.json e una tabella a schermo."""
import json, sys, tempfile
from pathlib import Path
import numpy as np
import measure as M

src = Path(sys.argv[1])
with tempfile.TemporaryDirectory() as d:
    sr, x = M.read_wav(M.to_wav(src, Path(d) / "t.wav"))
env, times = M.onset_env(x, sr); bpm, first, top = M.tempo(env, times); bars = M.bar_energy_db(x, sr, bpm, first)
e = np.array(bars); rise = int(np.argmax(np.diff(e))) + 1                                        # la prima battuta al livello nuovo, dopo il salto di energia più grande
lufs, peak = M.loudness(src)
card = {"file": src.name, "seconds": round(len(x) / sr, 1), "bpm": bpm, "bpm_candidates": top, "first_beat_s": round(first, 3),
        "bars": len(bars), "bar_seconds": round(240 / bpm, 3), "bar_energy_db": bars, "biggest_rise_at_bar": rise,
        "intro_bars_below_body": int(np.argmax(e > np.median(e) - 3)), "lufs": lufs, "peak_dbfs": peak,
        "film_bars": round(66 / (240 / bpm), 1)}
Path(str(src) + ".card.json").write_text(json.dumps(card, indent=1))
print(f"{src.name}: {bpm} bpm (alternative {top[1:]}), primo battito a {first:.3f} s, {len(bars)} battute, salita alla battuta {rise}, introduzione rada per {card['intro_bars_below_body']} battute")
print(" ".join(f"{v:.0f}" for v in bars))
