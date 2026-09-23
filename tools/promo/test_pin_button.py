"""Il tasto Write resta fermo in fondo al terminale del polso (corto, Franz 23/09 18:58)."""
import os, subprocess, tempfile, unittest
import numpy as np
from pin_button import COLOR, button_box, rest_box, sprite_at, pin, main

REST = (286, 425, 102, 377)          # dove sta il tasto nella clip vera quando la lista è ferma

def frame(top=REST[0], text=None):
    """Un fotogramma come quelli della clip: fondo nero, il tasto azzurro con «Write» scuro dentro, una riga bianca sopra."""
    f = np.zeros((480, 480, 3), np.uint8)
    t, b, l, r = top, top + REST[1] - REST[0], REST[2], REST[3]
    f[t:b + 1, l:r + 1] = COLOR
    f[t + 60:t + 80, l + 100:r - 100] = (20, 30, 60)       # la scritta
    f[t - 1, l:r + 1] = [c // 2 for c in COLOR]           # il bordo sfumato sopra il tasto
    if text is not None:
        f[text:text + 10, 40:440] = 240
    return f

class TastoFermo(unittest.TestCase):
    def test_trova_il_tasto_e_non_il_testo_bianco(self):
        self.assertEqual(button_box(frame(text=200)), REST)
        self.assertIsNone(button_box(np.zeros((480, 480, 3), np.uint8)))

    def test_il_posto_del_tasto_e_quello_piu_frequente(self):
        a, b = REST, (310, 449, 102, 377)
        self.assertEqual(rest_box([None, a, a, b, a, None]), a)

    def test_il_bordo_sfumato_resta_sfumato(self):
        (t0, l0), sprite, alpha = sprite_at(frame(), REST)
        self.assertEqual(float(alpha[REST[0] - t0 + 70, REST[2] - l0 + 150]), 1.0)     # dentro la scritta: pieno
        self.assertAlmostEqual(float(alpha[REST[0] - t0 - 1, REST[2] - l0 + 150]), 0.5, delta=0.02)
        self.assertEqual(float(alpha[0, 0]), 0.0)

    def test_il_tasto_che_scende_resta_al_suo_posto(self):
        frames = [frame(text=250), frame(text=250), frame(REST[0] + 64), frame(REST[0] + 15, text=REST[0] + 15 - 30), frame(text=250)]
        out, moved = pin(frames)
        self.assertEqual(moved, 2)
        for f in out:
            self.assertEqual(button_box(f), REST)
        for i in (0, 1, 4):
            self.assertTrue(np.array_equal(out[i], frames[i]), f"il fotogramma {i} era fermo e non deve cambiare")
        # la riga che sale sopra il tasto resta visibile dove il tasto non c'è
        self.assertTrue((out[3][REST[0] + 15 - 30:REST[0] + 15 - 25, 40:440] == 240).all())
        # sotto il tasto fermo, dove era sceso, torna il nero del fondo
        self.assertEqual(int(out[2][REST[1] + 20:, :].max()), 0)

    def test_la_clip_riscritta(self):
        frames = [frame()] * 6 + [frame(REST[0] + d) for d in (64, 40, 20, 8)] + [frame()] * 6
        with tempfile.TemporaryDirectory() as d:
            src, dst = os.path.join(d, "a.mp4"), os.path.join(d, "b.mp4")
            enc = subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "480x480", "-r", "30",
                                  "-i", "-", "-c:v", "libx264", "-crf", "12", "-pix_fmt", "yuv420p", src], input=b"".join(f.tobytes() for f in frames))
            self.assertEqual(enc.returncode, 0)
            self.assertEqual(main([src, dst]), 0)
            raw = subprocess.run(["ffmpeg", "-v", "error", "-i", dst, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"], capture_output=True, check=True).stdout
            got = np.frombuffer(raw, np.uint8).reshape(-1, 480, 480, 3)
            self.assertEqual(len(got), len(frames))
            for i, f in enumerate(got):
                b = button_box(f)
                self.assertIsNotNone(b, f"fotogramma {i}")
                self.assertTrue(all(abs(x - y) <= 2 for x, y in zip(b, REST)), f"fotogramma {i}: tasto in {b}")

if __name__ == "__main__":
    unittest.main()
