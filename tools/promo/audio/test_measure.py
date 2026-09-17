"""Segnali a risposta nota: se la misura sbaglia qui, non vale niente sulla musica vera."""
import subprocess, tempfile, unittest, wave
from pathlib import Path
import numpy as np
import measure as M

SR = 44100
def click_times(bpm, seconds, first=0.30):
    return [first + i * 60 / bpm for i in range(int((seconds - 0.2 - first) * bpm / 60) + 1)]      # per indice: sommando 0,6 sedici volte si resta sotto 9,9 e nasce un clic in più
def click_track(bpm, seconds, first=0.30):
    x = np.zeros(int(SR * seconds), np.float32)
    for t in click_times(bpm, seconds, first):
        i = int(t * SR); x[i:i + 220] += np.hanning(220) * np.sin(2 * np.pi * 1800 * np.arange(220) / SR) * 0.8
    return x + np.random.default_rng(1).normal(0, 0.002, len(x)).astype(np.float32)
def write(x, path):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())

class Tempo(unittest.TestCase):
    def test_cento_battiti(self):
        env, times = M.onset_env(click_track(100, 40), SR); bpm, first, _ = M.tempo(env, times)
        self.assertAlmostEqual(bpm, 100, delta=0.1)
        self.assertAlmostEqual(first % 0.6, 0.30, delta=0.005, msg=f"scarto {first % 0.6 - 0.30:+.4f} s: correggere ENV_BIAS_S di questo valore")
    def test_novantasette(self):
        self.assertAlmostEqual(M.tempo(*M.onset_env(click_track(97, 40), SR))[0], 97, delta=0.1)
    def test_attacchi(self):
        env, times = M.onset_env(click_track(100, 10), SR); on = M.onsets(env, times)
        want = click_times(100, 10); self.assertEqual(len(on), len(want)); self.assertLess(max(abs(o - w) for o, w in zip(on, want)), 0.010)

class Livelli(unittest.TestCase):
    def test_cresta(self):
        s = np.sin(2 * np.pi * 440 * np.arange(SR) / SR).astype(np.float32)
        self.assertAlmostEqual(M.crest_db(s), 3.01, delta=0.05)
        self.assertLess(M.crest_db(np.clip(s * 4, -1, 1)), 1.5)            # la distorsione schiaccia la cresta
    def test_banda(self):
        s = np.sin(2 * np.pi * 300 * np.arange(SR) / SR)
        self.assertGreater(M.band_db(s, SR, 200, 400), -0.1); self.assertLess(M.band_db(s, SR, 2000, 4000), -60)
    def test_lufs(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "s.wav"; write(0.1 * np.sin(2 * np.pi * 1000 * np.arange(SR * 5) / SR), p)
            lufs, peak = M.loudness(p)
            self.assertAlmostEqual(peak, -20.0, delta=0.3); self.assertAlmostEqual(lufs, -23.0, delta=1.0)
    def test_energia_per_battuta(self):
        x = click_track(100, 24); x[int(SR * 12):] *= 4
        e = M.bar_energy_db(x, SR, 100, 0.30)
        self.assertEqual(len(e), 9); self.assertAlmostEqual(e[7] - e[1], 12.0, delta=1.0)

if __name__ == "__main__":
    unittest.main()
