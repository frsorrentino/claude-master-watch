/** La griglia dei battiti: il film si scrive in battiti; i fotogrammi si ricavano da qui e solo da qui. */
export type Grid = { bpm: number; fps: number; offsetSeconds: number };

export const beatToFrame = (g: Grid, beat: number): number => Math.round((g.offsetSeconds + (beat * 60) / g.bpm) * g.fps);
/** L'inverso continuo, senza arrotondare: le funzioni pure del battito (la forma) si leggono in ogni fotogramma e sotto-fotogramma. */
export const frameToBeat = (g: Grid, frame: number): number => ((frame / g.fps - g.offsetSeconds) * g.bpm) / 60;

/** Durata di un tratto come differenza di due posizioni arrotondate: le scene in fila non derivano. */
export const spanFrames = (g: Grid, at: number, len: number): number => beatToFrame(g, at + len) - beatToFrame(g, at);
