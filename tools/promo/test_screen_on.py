"""Lo schermo del terminale acceso dal primo fotogramma (corto, Franz 23/09 22:17)."""
import unittest
import numpy as np
from screen_on import BAND, first_on, screen_on

def lit(k):
    """Un fotogramma acceso come quelli della clip: titolo in alto, righe nella fascia, tasto sotto."""
    f = np.zeros((480, 480, 3), np.uint8)
    f[150:160, 100:380] = 180                      # il titolo
    f[200:215, 60:400] = 240 - k                   # una riga del terminale, dentro la fascia
    f[290:420, 102:377] = (209, 225, 252)          # il tasto Write
    return f

class SchermoAcceso(unittest.TestCase):
    def test_il_primo_fotogramma_acceso(self):
        frames = [np.zeros((480, 480, 3), np.uint8)] * 4 + [lit(0), lit(1)]
        self.assertEqual(first_on(frames), 4)

    def test_i_fotogrammi_neri_diventano_lo_schermo_senza_righe(self):
        frames = [np.zeros((480, 480, 3), np.uint8)] * 4 + [lit(0), lit(1), lit(2)]
        out, k = screen_on(frames)
        self.assertEqual(k, 4)
        for i in range(4):
            self.assertEqual(int(out[i][BAND[0]:BAND[1] + 1].max()), 0, f"fotogramma {i}: nella fascia delle righe c'è ancora testo")
            self.assertTrue(np.array_equal(out[i][:BAND[0]], frames[4][:BAND[0]]), f"fotogramma {i}: sopra la fascia non è il primo acceso")
            self.assertTrue(np.array_equal(out[i][BAND[1] + 1:], frames[4][BAND[1] + 1:]), f"fotogramma {i}: sotto la fascia non è il primo acceso")
        for i in range(4, 7):
            self.assertTrue(np.array_equal(out[i], frames[i]), f"il fotogramma {i} era acceso e non deve cambiare")

if __name__ == "__main__":
    unittest.main()
