"""Le misure approvate il 17/09: se cambiano, il mockup non è più quello che Franz ha visto."""
import subprocess, sys, unittest
from pathlib import Path
import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
MAT = HERE.parent / "materiali/foto"

class Posa(unittest.TestCase):
    def test_inclinazione_e_distanza(self):
        import pose
        p, err = pose.fit()
        self.assertLess(err, 1e-6)
        self.assertAlmostEqual(np.degrees(p[3]), 34.0, delta=0.5)
        self.assertAlmostEqual(p[2], 8.99, delta=0.1)

    def test_convergenza_del_display(self):
        import pose
        q = np.array(pose.display_quad(pose.fit()[0]))
        lontano, vicino = np.linalg.norm(q[3] - q[0]), np.linalg.norm(q[2] - q[1])
        self.assertAlmostEqual(lontano / vicino, 0.899, delta=0.01)   # ~11 % di convergenza
        np.testing.assert_allclose(q[0], [441.4, 366.2], atol=1.0)

class Frontale(unittest.TestCase):
    def test_scontorno_per_costruzione(self):
        out = subprocess.run([sys.executable, str(HERE / "matte_front.py"), str(MAT)], capture_output=True, text=True, check=True).stdout
        scarto = float(out.split("scarto medio ")[1].split(" px")[0])
        self.assertLess(scarto, 0.6)
        cx, cy, rc = np.load(MAT / "f5_case.npy")
        m = np.asarray(Image.open(MAT / "f5_mask.png").convert("L"))
        self.assertEqual(m[int(cy), int(cx)], 255)
        self.assertEqual(m[5, 5], 0)
        self.assertGreaterEqual(m[int(cy), int(cx - rc + 3)], 250)      # lo smusso lucido resta dentro, anche ai lati
        self.assertLess(m[int(cy), int(cx - rc - 4)], 10)

class Esportazione(unittest.TestCase):
    def test_immagini_e_misure(self):
        import json
        subprocess.run([sys.executable, str(HERE / "export.py")], check=True)
        pub = HERE.parent / "remotion/public/mockup"; g = json.loads((HERE.parent / "remotion/src/film/mockup.geometry.json").read_text())
        self.assertAlmostEqual(g["front"]["displayR"] / g["front"]["glassR"], 0.86, places=3)
        self.assertEqual(len(g["q34"]["quad"]), 4)
        for name in ("front_body.png", "q34_body.png"):
            self.assertEqual(Image.open(pub / name).mode, "RGBA")
        r = np.asarray(Image.open(pub / "q34_reflections.png").convert("L"))
        self.assertEqual(int(r[5, 5]), 0)                         # fuori dal display: nero, neutro nella fusione «schermo»
        self.assertGreater(int(r.max()), 60)                      # dentro: i riflessi veri ci sono

if __name__ == "__main__":
    unittest.main()
