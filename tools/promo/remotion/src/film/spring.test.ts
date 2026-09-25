import test from "node:test";
import assert from "node:assert/strict";
import { spring as remotionSpring } from "remotion";
import { ZETA, overshootOf, springEase, springRest, springSettle, springs } from "./spring.ts";

const sample = (f: (t: number) => number, n = 600, to = 1) => Array.from({ length: n + 1 }, (_, i) => f((i / n) * to));

test("la molla parte da 0, arriva ESATTAMENTE a 1 a fine corsa e prima di 0 è ferma", () => {
  assert.equal(springEase(0), 0);
  assert.equal(springEase(1), 1);
  assert.equal(springEase(-0.3), 0);
  assert.equal(springEase(1.7), 1);
});

test("scavalco minimo, mai rimbalzo: supera 1 di poco una volta sola e non ricade sotto 1", () => {
  const xs = sample(springEase);
  const max = Math.max(...xs);
  assert.ok(max > 1.001, "un filo di scavalco si deve sentire");
  assert.ok(max < 1 + overshootOf(ZETA) + 1e-3, `scavalco ${max}`);
  assert.ok(overshootOf(ZETA) < 0.02, "meno del due per cento");
  const first = xs.findIndex((v) => v >= 1);
  for (let i = first; i < xs.length; i++) assert.ok(xs[i] >= 1 - 0.0015, `ricade sotto 1 a ${i}: ${xs[i]}`);
});

test("smorzamento critico (zeta 1): non supera mai 1 e resta monotona", () => {
  const xs = sample((t) => springEase(t, 1));
  for (let i = 1; i < xs.length; i++) { assert.ok(xs[i] <= 1 + 1e-9); assert.ok(xs[i] >= xs[i - 1] - 1e-9); }
});

test("springSettle: per opacità e colori, mai sopra 1, monotona, e a 1 solo a fine corsa", () => {
  const xs = sample(springSettle, 1000);
  for (let i = 1; i < xs.length; i++) { assert.ok(xs[i] <= 1); assert.ok(xs[i] >= xs[i - 1]); }
  assert.ok(springSettle(0.99) < 1 && springSettle(1) === 1);
});

test("è la spring() di Remotion: stesso smorzamento (damping 16, stiffness 100, mass 1 = zeta 0,8), stesso tempo fisico, stessi valori", () => {
  // Remotion integra passo per passo con omega = sqrt(k/m) = 10 rad/s; qui la corsa unitaria vale omega_1 = -ln(REST)/zeta rad:
  // un fotogramma a 30 fps è t = (f/30)·(10/omega_1) della corsa. Lo scarto è zero a meno dell'integrazione numerica.
  const w1 = -Math.log(0.005) / ZETA;
  let worst = 0;
  for (let f = 0; f <= 40; f++) {
    const r = remotionSpring({ frame: f, fps: 30, config: { damping: 16, stiffness: 100, mass: 1 } });
    const t = (f / 30) * (10 / w1);
    if (t >= 1) break;
    worst = Math.max(worst, Math.abs(r - springEase(t) * springRest()));
  }
  assert.ok(worst < 1e-3, `scarto massimo ${worst.toFixed(5)}`);
});

test("un valore che cambia bersaglio più volte è la somma di una molla per cambio: a riposo vale l'ultimo bersaglio", () => {
  const ch = [{ at: 0, to: 1, len: 1 }, { at: 2, to: 0.5, len: 1 }, { at: 4, to: 2, len: 0.5 }];
  assert.equal(springs(-1, 0, ch), 0);
  assert.equal(springs(0, 0, ch), 0);
  assert.ok(Math.abs(springs(1.5, 0, ch) - 1) < 1e-9);
  assert.ok(Math.abs(springs(3.5, 0, ch) - 0.5) < 1e-9);
  assert.ok(Math.abs(springs(9, 0, ch) - 2) < 1e-9);
  const mid = springs(2.4, 0, ch);
  assert.ok(mid < 1 && mid > 0.5, `a metà del secondo cambio ${mid}`);
});

test("i cambi si sommano anche se il secondo parte mentre il primo è ancora in corsa: nessun salto", () => {
  const ch = [{ at: 0, to: 1, len: 1 }, { at: 0.5, to: 0, len: 1 }];
  const xs = Array.from({ length: 301 }, (_, i) => springs(i / 100, 0, ch));
  for (let i = 1; i < xs.length; i++) assert.ok(Math.abs(xs[i] - xs[i - 1]) < 0.03, `salto a ${i / 100}`);
  assert.ok(Math.abs(xs[300] - 0) < 1e-9);
});

test("springs: ogni cambio può avere il suo smorzamento", () => {
  const v = (t: number) => springs(t, 0, [{ at: 0, to: 1, len: 1, zeta: 1 }]);
  for (let t = 0; t <= 1; t += 0.01) assert.ok(v(t) <= 1 + 1e-9);   // critico: mai sopra
});
