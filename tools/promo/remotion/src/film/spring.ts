/**
 * Molle in forma chiusa (direttive di motion del 25/09, dal post di Raphael Aubry): «Springs are closed-form step
 * responses. A value that changes target many times is the sum of one spring per change, so it stays a pure function
 * of time.» Qui la risposta al gradino di un oscillatore smorzato, scritta come funzione pura del tempo, sostituisce le
 * curve di Bézier dei movimenti: stessa firma delle curve di `moves.ts` (tempo 0-1 → avanzamento), così entra nelle
 * funzioni pure e nei loro test senza passare da Remotion. È la stessa matematica di `spring()` di Remotion (che invece
 * la integra passo per passo), verificata nel test con lo stesso smorzamento.
 *
 * Smorzamento `ZETA` 0,8: uno scavalco di un centesimo e mezzo, una volta sola, e nessun rimbalzo (a 0,5, il valore di
 * Remotion, la molla oscilla tre volte: è la «bouncy easing» vietata). A 1 è critica: nessuno scavalco.
 */
export const ZETA = 0.8;
/** Quanto resta di ampiezza a fine corsa (t = 1): mezzo per mille, come il `durationRestThreshold` di Remotion. */
const REST = 0.005;

/** Lo scavalco massimo di una molla con smorzamento `zeta` (frazione della corsa): e^(−πζ/√(1−ζ²)). */
export const overshootOf = (zeta: number): number => (zeta >= 1 ? 0 : Math.exp((-Math.PI * zeta) / Math.sqrt(1 - zeta * zeta)));

const raw = (t: number, zeta: number): number => {
  const w = -Math.log(REST) / zeta;                 // pulsazione tale che a t = 1 l'inviluppo e^(−ζωt) vale REST
  if (zeta >= 1) return 1 - Math.exp(-w * t) * (1 + w * t);
  const wd = w * Math.sqrt(1 - zeta * zeta);
  return 1 - Math.exp(-zeta * w * t) * (Math.cos(wd * t) + (zeta / Math.sqrt(1 - zeta * zeta)) * Math.sin(wd * t));
};

/**
 * La molla come curva: `t` 0-1 è il tempo della corsa, il risultato è l'avanzamento (0 fermo, 1 arrivato). Prima di 0 è
 * ferma, da 1 in poi è ESATTAMENTE 1 (la corsa è normalizzata sul valore di arrivo: le pose che devono combaciare non
 * restano a mezzo millesimo dal posto). Si usa come `soft` o `bezier(…)` di `moves.ts`: `springEase(ramp(p, a, b))`.
 */
export const springEase = (t: number, zeta = ZETA): number => {
  if (t <= 0) return 0;
  if (t >= 1) return 1;
  return raw(t, zeta) / raw(1, zeta);
};

/**
 * La molla senza scavalco (smorzamento critico): per le grandezze che NON possono superare il bersaglio — opacità, miscele
 * di colore, riempimenti e le liste che scorrono (una lista di Wear OS si ferma, non rimbalza). Stessa curva, monotona,
 * arriva a 1 solo a fine corsa. Per posizioni, scale e rotazioni si usa `springEase`, che scavalca di un centesimo.
 */
export const springSettle = (t: number): number => springEase(t, 1);

/**
 * Un cambio di bersaglio: da `at` (nell'unità di `t`) il valore va verso `to` con una molla lunga `len`. `zeta` è lo
 * smorzamento di questo solo cambio (una chiave della forma che si posa senza scavalco), se no quello di `springs()`.
 */
export type Change = { at: number; to: number; len: number; zeta?: number };
/**
 * Un valore che cambia bersaglio più volte, come somma di una molla per cambio: `from` all'inizio, poi ogni cambio
 * aggiunge la sua molla sullo scarto dal bersaglio precedente. Funzione pura del tempo `t`: due cambi che si
 * sovrappongono si sommano senza salti, e a riposo il valore è l'ultimo bersaglio. `changes` in ordine di `at`.
 */
export const springs = (t: number, from: number, changes: readonly Change[], zeta = ZETA): number => {
  let v = from, prev = from;
  for (const c of changes) {
    v += (c.to - prev) * springEase((t - c.at) / Math.max(1e-9, c.len), c.zeta ?? zeta);
    prev = c.to;
  }
  return v;
};

/** Il valore grezzo della molla a fine corsa (t = 1), prima della normalizzazione: serve al test di identità con Remotion. */
export const springRest = (zeta = ZETA): number => raw(1, zeta);
