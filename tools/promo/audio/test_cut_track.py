"""Il taglio sulle battute: attacco a metà battuta e salita dal silenzio (corto, 23/09 18:24: la musica parte con il video)."""
import unittest
import numpy as np
from cut_track import cut, fade_in, fade_out

SR, BPM, FIRST = 8000, 120.0, 0.1          # 120 bpm: battito 0,5 s, battuta 2 s

class Taglio(unittest.TestCase):
    def test_battute_frazionarie(self):
        # 0.25-1 = gli ultimi tre battiti della battuta 0; poi la battuta 0 intera
        x = np.arange(int(SR * 12), dtype=np.float32) / SR
        y = cut(x, SR, BPM, FIRST, [(0.25, 1), (0, 1)])
        self.assertAlmostEqual(len(y) / SR, 1.5 + 2.0, delta=0.02)
        self.assertAlmostEqual(float(y[int(0.0075 * SR)]), FIRST + 0.5, delta=0.01)   # parte dal battito 1 della battuta 0
    def test_la_salita_parte_dal_silenzio_e_arriva_piena(self):
        y = fade_in(np.ones(SR * 4, np.float32), SR, BPM, 3)
        self.assertEqual(float(y[0]), 0.0)
        self.assertLess(float(y[int(0.75 * SR)]), 0.6)                 # a metà salita è ancora sotto
        self.assertAlmostEqual(float(y[int(1.5 * SR)]), 1.0, places=5)  # dopo tre battiti è piena
        self.assertAlmostEqual(float(y[-1]), 1.0, places=5)
    def test_la_sfumatura_finale_arriva_al_silenzio_e_non_tocca_il_resto(self):
        # la sfumatura degli ultimi battiti: piena fino a lì, poi scende e basta fino al silenzio (Franz, 24/09 01:04)
        y = fade_out(np.ones(SR * 2, np.float32), SR, BPM, 0.25)
        n = int(0.25 * 0.5 * SR)                                        # un quarto di battito a 120 bpm
        self.assertTrue(np.all(y[:-n] == 1.0))                          # prima della chiusura non cambia niente
        self.assertEqual(float(y[-1]), 0.0)
        self.assertTrue(np.all(np.diff(y[-n:]) <= 0))                   # scende e basta

if __name__ == "__main__":
    unittest.main()
