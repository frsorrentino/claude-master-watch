/** La griglia dei battiti: il film si scrive in battiti; i fotogrammi si ricavano da qui e solo da qui. */
export type Grid = { bpm: number; fps: number; offsetSeconds: number };

export const beatToFrame = (g: Grid, beat: number): number => Math.round((g.offsetSeconds + (beat * 60) / g.bpm) * g.fps);

/** Durata di un tratto come differenza di due posizioni arrotondate: le scene in fila non derivano. */
export const spanFrames = (g: Grid, at: number, len: number): number => beatToFrame(g, at + len) - beatToFrame(g, at);
