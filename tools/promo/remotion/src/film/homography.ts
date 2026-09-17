/** Prospettiva vera del tre quarti: l'interfaccia (un quadrato) finisce sul quadrilatero misurato dal modello di camera. */
export type Pt = [number, number];
export type Quad = [Pt, Pt, Pt, Pt];

const solve = (A: number[][], b: number[]): number[] => {
  const n = b.length;
  const M = A.map((row, i) => [...row, b[i]]);
  for (let c = 0; c < n; c++) {
    let p = c;
    for (let r = c + 1; r < n; r++) if (Math.abs(M[r][c]) > Math.abs(M[p][c])) p = r;
    [M[c], M[p]] = [M[p], M[c]];
    for (let r = 0; r < n; r++) {
      if (r === c) continue;
      const f = M[r][c] / M[c][c];
      for (let k = c; k <= n; k++) M[r][k] -= f * M[c][k];
    }
  }
  return M.map((row, i) => row[n] / row[i]);
};

export const homography = (n: number, dst: Quad): number[] => {
  const src: Quad = [[0, 0], [n, 0], [n, n], [0, n]];
  const A: number[][] = [];
  const b: number[] = [];
  src.forEach(([x, y], i) => {
    const [X, Y] = dst[i];
    A.push([x, y, 1, 0, 0, 0, -X * x, -X * y]); b.push(X);
    A.push([0, 0, 0, x, y, 1, -Y * x, -Y * y]); b.push(Y);
  });
  return [...solve(A, b), 1];
};

export const applyH = (h: number[], [x, y]: Pt): Pt => {
  const w = h[6] * x + h[7] * y + h[8];
  return [(h[0] * x + h[1] * y + h[2]) / w, (h[3] * x + h[4] * y + h[5]) / w];
};

/** CSS vuole la 4×4 per colonne; con `transform-origin: 0 0`. */
export const toMatrix3d = (h: number[]): string =>
  `matrix3d(${[h[0], h[3], 0, h[6], h[1], h[4], 0, h[7], 0, 0, 1, 0, h[2], h[5], 0, h[8]].join(",")})`;
