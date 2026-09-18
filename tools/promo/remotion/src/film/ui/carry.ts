/**
 * La catena dei passaggi (piano 4 §2 bis, Franz 18/09 13:22: «un morphing ben fatto e sensato degli elementi della UI che si
 * fondono con quelli del video tra una scena e l'altra, che guidi l'attenzione»). Un oggetto attraversa ogni taglio: parte
 * dalla forma che ha nell'ultima scena (`from`) e arriva alla forma che ha nella prima della scena dopo (`to`).
 * Funzioni pure: forme campionate come polilinee e interpolate punto per punto (niente morphing di tracciati arbitrari:
 * solo posizione, misura, raggio, colore; i glifi dentro si scambiano in dissolvenza).
 */
import { bezier } from "../moves.ts";

export type Shape = "circle" | "pill" | "square" | "line" | "arc";
export type Glyph = "play" | "question" | "check" | "bell" | "mic" | "none";
/** Una forma nel quadro: centro, misura (w×h), colore del contenitore, glifo dentro. `arc`: anello aperto in basso (gauge). */
export type Key = { shape: Shape; x: number; y: number; w: number; h: number; color: string; glyph?: Glyph; glyphColor?: string; stroke?: number };

const clamp = (t: number) => Math.min(1, Math.max(0, t));
/** Il passaggio: parte deciso, atterra morbido (come `soft`), con un'anticipazione minima. */
export const carryEase = bezier(0.3, 0, 0.1, 1);

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
const hex = (c: string): [number, number, number] => [1, 3, 5].map((i) => parseInt(c.slice(i, i + 2), 16)) as [number, number, number];
export const mixColor = (a: string, b: string, t: number): string => {
  const A = hex(a), B = hex(b);
  return `rgb(${A.map((v, i) => Math.round(lerp(v, B[i], t))).join(",")})`;
};

/** Raggio degli angoli del contenitore per forma: cerchio = metà, pillola = metà dell'altezza, quadrato = 23 % (come il badge). */
export const radiusOf = (k: Key): number => (k.shape === "circle" ? Math.max(k.w, k.h) / 2 : k.shape === "pill" ? k.h / 2 : k.shape === "square" ? Math.min(k.w, k.h) * 0.23 : 0);

/** Lo stato del contenitore a `t` 0-1 tra due chiavi: centro, misura, raggio e colore interpolati; glifi in dissolvenza incrociata
 *  a metà (0,35-0,65), così non si vedono mai due glifi sovrapposti a metà forza. */
/** Quanto l'oggetto sale a protagonista a metà passaggio (Franz, 13:22: il morphing deve guidare l'attenzione): un piccolo
 *  badge da 40 px passa per 220 px al centro del taglio, poi si posa alla misura d'arrivo. Gli oggetti già grandi non crescono. */
export const HERO_MIN = 220;
export const carryAt = (from: Key, to: Key, t: number) => {
  const e = carryEase(clamp(t));
  const bell = Math.sin(Math.PI * clamp(t));
  const w0 = lerp(from.w, to.w, e), h0 = lerp(from.h, to.h, e);
  const boost = Math.max(0, HERO_MIN - Math.min(w0, h0)) * bell;
  const w = w0 + boost, h = h0 + boost;
  const rr = lerp(radiusOf(from), radiusOf(to), e);
  // a metà passaggio l'oggetto si sposta verso il campo libero a sinistra dell'orologio (x 600, y 540 nel quadro): il morphing
  // avviene davanti agli occhi, non sopra il display che lo coprirebbe
  const lift = bell * bell;
  return {
    x: lerp(lerp(from.x, to.x, e), 600, lift * 0.85), y: lerp(lerp(from.y, to.y, e), 540, lift * 0.85), w, h,
    r: rr * (w / Math.max(1, w0)), color: mixColor(from.color, to.color, e),
    fromGlyph: 1 - clamp((t - 0.35) / 0.3), toGlyph: clamp((t - 0.35) / 0.3),
  };
};

/** Una linea o un arco come 48 punti nel quadro: la linea va da sinistra a destra alla quota `y`; l'arco è l'anello del gauge
 *  (apertura di 31° in basso, da 105,5° in senso orario) inscritto in `w`×`h` attorno a (`x`, `y`). Stesso numero di punti:
 *  il passaggio da linea ad arco è un'interpolazione punto per punto, e la lunghezza resta simile (la linea è lunga quanto l'arco). */
export const strokePoints = (k: Key, n = 48): [number, number][] => {
  const pts: [number, number][] = [];
  if (k.shape === "arc") {
    const r = Math.min(k.w, k.h) / 2;
    for (let i = 0; i <= n; i++) { const d = ((105.5 + (329 * i) / n) * Math.PI) / 180; pts.push([k.x + r * Math.cos(d), k.y + r * Math.sin(d)]); }
  } else {
    for (let i = 0; i <= n; i++) pts.push([k.x - k.w / 2 + (k.w * i) / n, k.y]);
  }
  return pts;
};
export const strokeAt = (from: Key, to: Key, t: number): [number, number][] => {
  const e = carryEase(clamp(t)), A = strokePoints(from), B = strokePoints(to);
  return A.map((p, i) => [lerp(p[0], B[i][0], e), lerp(p[1], B[i][1], e)]);
};
export const isStroke = (k: Key): boolean => k.shape === "line" || k.shape === "arc";
