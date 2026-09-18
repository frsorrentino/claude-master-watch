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
  // dal ■ un gancio corto verso l'alto a destra (70 px), poi dritto in orizzontale sopra il quadro fino alla colonna e giù in
  // verticale: il filo non segue la ghiera (a 1 fps sembrava un anello attorno all'orologio, un secondo cerchio: master, 12:05).
  // Meno filo, più onda: i lobi stanno solo sotto le parole.
  const [fx, fy] = from; const m = 24;
  const ty = top ?? fy - 260;
  const K: [number, number][] = [[fx, fy], [fx + 50, fy - 70], [fx + 50, ty], [x1, ty], [x1, y]];
  const segs = K.slice(1).map((q, i) => Math.hypot(q[0] - K[i][0], q[1] - K[i][1]));
  const total = segs.reduce((a, b) => a + b, 0);
  for (let k = 0; k <= m; k++) {
    let d = (total * k) / m, j = 0;
    while (j < segs.length - 1 && d > segs[j]) { d -= segs[j]; j++; }
    const t = segs[j] ? d / segs[j] : 0;
    pts.push([K[j][0] + (K[j + 1][0] - K[j][0]) * t, K[j][1] + (K[j + 1][1] - K[j][1]) * t]);
  }
  for (let k = 1; k <= n; k++) {
    const x = x1 - ((x1 - x0) * k) / n;
    const bell = Math.sin((Math.PI * k) / n);
    pts.push([x, y + amp * bell * Math.sin((2 * Math.PI * waves * k) / n + phase)]);
  }
  const keep = Math.max(2, Math.round(pts.length * Math.min(1, Math.max(0, reach))));
  return pts.slice(0, keep).map(([x, yy], i) => `${i === 0 ? "M" : "L"} ${x.toFixed(1)} ${yy.toFixed(1)}`).join(" ");
};
