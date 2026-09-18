/** L'onda della voce (piano 3): funzioni pure del fotogramma, testabili senza React. */

/** L'onda della voce in un fotogramma: ampiezza dall'inviluppo RMS (`<voce>.env.json`, 30 Hz, normalizzato al picco) con un
 *  po' di memoria, perché l'orecchio sente la sillaba intera e non il singolo fotogramma. Funzione pura: testabile. */
export const waveAmplitudeAt = (env: number[], frame: number): number => {
  if (frame < 0 || !env.length) return 0;
  const i = Math.min(env.length - 1, Math.floor(frame));
  const prev = env[Math.max(0, i - 1)], next = env[Math.min(env.length - 1, i + 1)];
  return Math.min(1, Math.max(env[i], 0.6 * prev, 0.6 * next));
};

/** Il tracciato dell'onda: una linea che ondeggia con l'ampiezza data e scorre piano verso destra (la voce va verso chi ascolta). */
export const wavePath = (width: number, mid: number, amp: number, phase: number, waves = 3.2): string => {
  const pts: string[] = [];
  const n = 48;
  for (let k = 0; k <= n; k++) {
    const x = (width * k) / n;
    // l'onda è piena al centro e si spegne ai due capi, come un arco di luce
    const bell = Math.sin((Math.PI * k) / n);
    const y = mid + amp * bell * Math.sin((2 * Math.PI * waves * k) / n - phase);
    pts.push(`${k === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`);
  }
  return pts.join(" ");
};


/**
 * L'onda giro 2 (piano 4): nasce dal tasto ▶ sul display (`from`), attraversa il vetro e corre verso le parole, poi ondeggia
 * per tutta la larghezza della colonna (`x0`..`x1`, a quota `y`). `reach` 0-1: quanto è arrivata (da ▶ alla colonna).
 * Tracciato in coordinate del quadro: un ramo dal ▶ alla fine destra della colonna, poi l'onda verso sinistra.
 */
export const wavePathFrom = (from: [number, number], x0: number, x1: number, y: number, amp: number, phase: number, reach: number, waves = 3.4, top?: number): string => {
  const n = 64;
  const pts: [number, number][] = [];
  // ramo: dal ▶ esce dal vetro verso l'alto e gira attorno all'orologio fino alla colonna (Bézier cubica campionata): non
  // attraversa mai il testo sul display
  const [fx, fy] = from; const m = 24;
  const ty = top ?? fy - 260;
  const P = [[fx, fy], [fx - 160, ty], [x1 - 140, ty + 60], [x1, y]];   // scende a sinistra del vetro, mai sopra il testo del display
  for (let k = 0; k <= m; k++) {
    const t = k / m, a = 1 - t;
    const x = a * a * a * P[0][0] + 3 * a * a * t * P[1][0] + 3 * a * t * t * P[2][0] + t * t * t * P[3][0];
    const yy = a * a * a * P[0][1] + 3 * a * a * t * P[1][1] + 3 * a * t * t * P[2][1] + t * t * t * P[3][1];
    pts.push([x, yy]);
  }
  for (let k = 1; k <= n; k++) {
    const x = x1 - ((x1 - x0) * k) / n;
    const bell = Math.sin((Math.PI * k) / n);
    pts.push([x, y + amp * bell * Math.sin((2 * Math.PI * waves * k) / n + phase)]);
  }
  const keep = Math.max(2, Math.round(pts.length * Math.min(1, Math.max(0, reach))));
  return pts.slice(0, keep).map(([x, yy], i) => `${i === 0 ? "M" : "L"} ${x.toFixed(1)} ${yy.toFixed(1)}`).join(" ");
};
