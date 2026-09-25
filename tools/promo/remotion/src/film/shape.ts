/**
 * La forma unica del corto (specifica 25/09, §1): il display tondo dell'orologio che non sparisce mai. Si stacca dal
 * quadrante, diventa notifica, tasto, sfondo, card, finestra, linea, arco, e torna display. La traccia `shape` della
 * scaletta ne dà le chiavi in battiti; fra due chiavi ogni proprietà (rettangolo, raggio, colore, peso dell'aggancio al
 * display) è una somma di molle sui cambi di bersaglio (`springs`), così lo stato è una funzione pura del battito:
 * ogni fotogramma e ogni sotto-fotogramma del motion blur lo ricalcolano da zero, e nessuna scena la ricrea, quindi
 * non c'è un punto in cui possa saltare.
 */
import { ZETA, springSettle, springs } from "./spring.ts";
import type { Change } from "./spring.ts";
import { INK } from "./theme.ts";

/** x, y, larghezza, altezza in px del quadro, prima della camera. */
export type Rect = [number, number, number, number];
/** I soli gesti che nell'app esistono: ogni trasformazione ne parte da uno (nessuna corona). */
export type Gesture = "tap" | "longPress" | "swipe" | "send";
/** Con `anchor` il `rect` e il raggio sono in unità del display (0-480, anche fuori dal display); senza `rect` la chiave
 *  agganciata è il display intero. Senza `anchor` sono px del quadro. */
export type ShapeKey = {
  at: number;
  rect?: Rect;
  anchor?: "display";
  r: number;
  color: string;
  content?: string;
  len?: number;
  ease?: "spring" | "settle";
  zoom?: number;
  gesture?: Gesture;
};
export type ShapeState = { x: number; y: number; w: number; h: number; r: number; color: string; anchor: number };
/** Il rettangolo del display a quel battito: con l'aggancio la forma È il display, anche mentre la foto si muove. */
export type Display = (beat: number) => Rect;
export type ContentState = { id: string; key: ShapeKey; opacity: number; blur: number; dy: number };

/** Scambio del contenuto (§3, §8.6): esce il vecchio, poi entra il nuovo, mai sovrapposti. Battiti, px, px. */
export const SWAP_OUT = 0.25, SWAP_IN = 0.25, SWAP_BLUR = 6, SWAP_SLIDE = 8, KEY_LEN = 1;
/** Il limite di scala della camera: oltre, le foto sgranano (§4). Qui perché `zoom` è un campo della chiave. */
export const ZOOM_MAX = 1.3;
/** Oltre questo spostamento di un vertice in un fotogramma 4 campioni di blur lasciano vedere le copie: se ne usano 8. */
export const BLUR_FAST = 24;

/** Il display intero in unità del display, col suo raggio: la chiave agganciata senza `rect` (il quadrante tondo). */
const FULL: Rect = [0, 0, 480, 480];
const unitRect = (k: ShapeKey): Rect => k.rect ?? FULL;
const unitR = (k: ShapeKey): number => (k.rect ? k.r : 240);
/** L'origine del display nel quadro e quanti px vale un'unità (0-480). */
export const displayUnits = (d: Rect): { x0: number; y0: number; u: number } => ({ x0: d[0], y0: d[1], u: d[2] / 480 });
/** Un rect in unità del display portato nel quadro, sul display `d`. */
export const toFrameRect = (r: Rect, d: Rect): Rect => {
  const { x0, y0, u } = displayUnits(d);
  return [x0 + r[0] * u, y0 + r[1] * u, r[2] * u, r[3] * u];
};

/** Il rect della chiave nel quadro; per le agganciate, fotografato sul display al battito della chiave. */
export const keyRect = (k: ShapeKey, display: Display): Rect => (k.anchor ? toFrameRect(unitRect(k), display(k.at)) : k.rect!);

const lenOf = (k: ShapeKey) => k.len ?? KEY_LEN;
/** Una grandezza della forma: il valore della prima chiave, poi un cambio per chiave successiva. */
const track = (keys: readonly ShapeKey[], beat: number, value: (k: ShapeKey) => number, zetaOf: (k: ShapeKey) => number) => {
  const changes: Change[] = keys.slice(1).map((k) => ({ at: k.at, to: value(k), len: lenOf(k), zeta: zetaOf(k) }));
  return springs(beat, value(keys[0]), changes);
};
const easeZeta = (k: ShapeKey) => (k.ease === "settle" ? 1 : ZETA);
/** Opacità, colori e peso dell'aggancio non possono scavalcare: sempre critici. */
const critical = () => 1;

const channels = (hex: string) => [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));
const toHex = (c: number) => Math.min(255, Math.max(0, Math.round(c))).toString(16).padStart(2, "0");
/** Miscela scritta così perché con peso 1 esatto il risultato è ESATTAMENTE `b`: la forma agganciata non deriva dal display. */
const mix = (a: number, b: number, w: number) => a * (1 - w) + b * w;

/**
 * Tre tracce di molle. Libera, nel quadro: le chiavi agganciate vi entrano fotografate sul display al loro battito.
 * Agganciata, in unità del display e sulle sole chiavi agganciate: portata nel quadro sul display di ADESSO, così fra due
 * chiavi agganciate la forma segue il display anche durante la corsa (deriva, dolly, tremito). Il peso le miscela.
 */
export const shapeAt = (keys: readonly ShapeKey[], beat: number, display: Display): ShapeState => {
  const free = [0, 1, 2, 3].map((i) => track(keys, beat, (k) => keyRect(k, display)[i], easeZeta));
  const r = track(keys, beat, (k) => (k.anchor ? unitR(k) * displayUnits(display(k.at)).u : k.r), easeZeta);
  // due cambi sovrapposti con durate diverse sommano molle a passi diversi e il peso esce da 0-1: fuori, la miscela
  // amplifica invece di interpolare (larghezze di 4000 px, raggi negativi), e sopra 1 la forma non sarebbe più il display
  const anchor = Math.min(1, Math.max(0, track(keys, beat, (k) => (k.anchor ? 1 : 0), critical)));
  // prima della prima chiave agganciata vale la prima; senza chiavi agganciate il peso è 0 e il display intero non conta
  const pinned = keys.filter((k) => k.anchor);
  const now = display(beat);
  const d = pinned.length ? toFrameRect([0, 1, 2, 3].map((i) => track(pinned, beat, (k) => unitRect(k)[i], easeZeta)) as Rect, now) : now;
  const dr = pinned.length ? track(pinned, beat, unitR, easeZeta) * displayUnits(now).u : now[2] / 2;
  const color = "#" + [0, 1, 2].map((i) => toHex(track(keys, beat, (k) => channels(k.color)[i], critical))).join("");
  // lo scavalco di ζ 0,8 verso una linea sottile porta la misura sotto zero: la forma resta almeno un pixel, e il raggio
  // non supera mezza misura (oltre, il CSS lo riduce da solo e la forma cambierebbe senza che la traccia lo dica)
  const w = Math.max(1, mix(free[2], d[2], anchor)), h = Math.max(1, mix(free[3], d[3], anchor));
  return {
    x: mix(free[0], d[0], anchor),
    y: mix(free[1], d[1], anchor),
    w,
    h,
    r: Math.min(Math.min(w, h) / 2, Math.max(0, mix(r, dr, anchor))),
    color,
    anchor,
  };
};

/**
 * Il contenuto a quel battito. La chiave che lo porta è quella dove è entrato, non l'ultima passata: il blocco si
 * impagina alla misura di quella chiave e l'a capo non cambia finché un nuovo scambio non lo ricompone (§3).
 */
export const contentAt = (keys: readonly ShapeKey[], beat: number): ContentState | null => {
  let i = 0;
  while (i + 1 < keys.length && keys[i + 1].at <= beat) i++;
  const bearer = (j: number) => {
    while (j > 0 && keys[j - 1].content === keys[j].content) j--;
    return keys[j];
  };
  const now = bearer(i);
  const b = keys[i].at;
  if (i > 0 && now === keys[i] && beat < b + SWAP_OUT + SWAP_IN) {
    const old = bearer(i - 1);
    if (beat < b + SWAP_OUT) {
      const p = (beat - b) / SWAP_OUT;
      return old.content === undefined ? null
        : { id: old.content, key: old, opacity: 1 - springSettle(p), blur: SWAP_BLUR * p, dy: -SWAP_SLIDE * p };
    }
    const p = (beat - b - SWAP_OUT) / SWAP_IN;
    return now.content === undefined ? null
      : { id: now.content, key: now, opacity: springSettle(p), blur: SWAP_BLUR * (1 - p), dy: SWAP_SLIDE * (1 - p) };
  }
  return now.content === undefined ? null : { id: now.content, key: now, opacity: 1, blur: 0, dy: 0 };
};

/** Il massimo spostamento dei quattro vertici nell'ultimo fotogramma, in px: decide quanti campioni di blur servono. */
export const shapeSpeed = (keys: readonly ShapeKey[], beat: number, display: Display, beatsPerFrame: number): number => {
  const corners = (s: ShapeState) => [[s.x, s.y], [s.x + s.w, s.y], [s.x, s.y + s.h], [s.x + s.w, s.y + s.h]];
  const a = corners(shapeAt(keys, beat - beatsPerFrame, display)), b = corners(shapeAt(keys, beat, display));
  return Math.max(...a.map(([x, y], i) => Math.hypot(b[i][0] - x, b[i][1] - y)));
};

export const blurSamples = (speed: number): 4 | 8 => (speed > BLUR_FAST ? 8 : 4);
/** L'otturatore del blur della forma: mezzo fotogramma, come le scene (§8.3). */
export const SHAPE_SHUTTER = 180;
/**
 * Di quanto arretrare il fotogramma della scatola dentro `CameraMotionBlur` perché i suoi `n` campioni cadano in media
 * sul fotogramma. La 4.0.490 congela il campione i (1..n) a f − frazione·i/n + 1, cioè tutti DOPO f: senza correzione la
 * scatola anticipa di due terzi di fotogramma il contenuto e le scene, che non hanno blur (70 px nei tratti veloci).
 */
export const shutterCentre = (n: number): number => 1 - ((SHAPE_SHUTTER / 360) * (n + 1)) / (2 * n);

/** L'inchiostro del testo sulla forma: fra quello dei titoli e il bianco, il più contrastato (rapporto WCAG) sul colore. */
const luminance = (hex: string) => {
  const [r, g, b] = channels(hex).map((c) => (c / 255 <= 0.04045 ? c / 255 / 12.92 : ((c / 255 + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
export const inkOn = (color: string): string => {
  const l = luminance(color);
  return (l + 0.05) / (luminance(INK.text) + 0.05) > 1.05 / (l + 0.05) ? INK.text : "#ffffff";
};

const GESTURES = ["tap", "longPress", "swipe", "send"];
const half = (v: unknown): v is number => typeof v === "number" && v >= 0 && Number.isInteger(v * 2);

/**
 * I problemi della traccia, in italiano come quelli della scaletta; [] se va bene, e anche se la traccia non c'è (un film
 * senza forma). Non importa `timeline.ts`: la scaletta chiama questa, non il contrario.
 */
export const validateShape = (raw: unknown, total: number): string[] => {
  if (raw === undefined) return [];
  if (!Array.isArray(raw) || raw.length === 0) return ["la traccia deve essere un elenco di chiavi non vuoto"];
  const bad: string[] = [];
  const keys = raw as ShapeKey[];
  const isKey = (k: unknown): k is ShapeKey => typeof k === "object" && k !== null;
  const num = (v: unknown): v is number => typeof v === "number" && Number.isFinite(v);
  keys.forEach((k, i) => {
    if (!isKey(k)) { bad.push(`chiave ${i}: non è una chiave (${String(k)})`); return; }
    const say = (m: string) => bad.push(`chiave ${i} (battito ${k.at}): ${m}`);
    const prev = isKey(keys[i - 1]) ? keys[i - 1] : undefined;
    if (i === 0 && k.at !== 0) say("la prima chiave deve stare al battito 0");
    if (!half(k.at)) say("servono battiti o mezzi battiti");
    if (prev && !(k.at > prev.at)) say(`i battiti devono essere strettamente crescenti (la chiave prima è al ${prev.at})`);
    if (!(k.at < total)) say(`oltre la fine del film (${total} battiti)`);
    if (k.rect === undefined && k.anchor === undefined) say("serve «rect», «anchor» o tutti e due (un rect in unità del display)");
    if (k.anchor !== undefined && k.anchor !== "display") say(`anchor «${k.anchor}»: vale solo «display»`);
    if (k.rect !== undefined && !(Array.isArray(k.rect) && k.rect.length === 4 && k.rect.every(Number.isFinite) && k.rect[2] > 0 && k.rect[3] > 0)) say(`rect (${String(k.rect)}): servono quattro numeri, larghezza e altezza positive`);
    if (!(num(k.r) && k.r >= 0)) say(`raggio ${k.r}: serve un numero non negativo`);
    if (!(typeof k.color === "string" && /^#[0-9A-Fa-f]{6}$/.test(k.color))) say(`colore «${k.color}»: atteso #rrggbb`);
    if (k.content !== undefined && typeof k.content !== "string") say("il contenuto è un id di testo");
    if (k.len !== undefined && !(num(k.len) && k.len > 0)) say(`durata ${k.len}: serve un numero positivo`);
    if (k.ease !== undefined && k.ease !== "spring" && k.ease !== "settle") say(`molla «${k.ease}» sconosciuta: spring o settle`);
    if (k.zoom !== undefined && !(num(k.zoom) && k.zoom >= 1 && k.zoom <= ZOOM_MAX)) say(`zoom ${k.zoom} fuori da 1-${ZOOM_MAX}`);
    if (k.gesture !== undefined && !GESTURES.includes(k.gesture)) say(`gesto «${k.gesture}» sconosciuto: ${GESTURES.join(", ")}`);
    // una molla non finisce prima di quella della chiave prima: finché ogni cambio è più avanti del successivo, la somma
    // delle molle resta una miscela dei bersagli; al contrario la forma esce dai bersagli (2951 px su un campo di 2120,
    // revisione del 25/09) e le pinze di peso e camera nasconderebbero solo il sintomo
    if (prev && num(prev.at) && num(k.at) && k.at + (k.len ?? KEY_LEN) < prev.at + (prev.len ?? KEY_LEN)) say(`la molla finisce al ${k.at + (k.len ?? KEY_LEN)}, prima di quella della chiave prima (${prev.at + (prev.len ?? KEY_LEN)}): la forma uscirebbe dai bersagli`);
    // l'uscita del vecchio e l'entrata del nuovo devono finire prima della chiave: se no i testi si sovrappongono
    if (prev && k.content !== prev.content && k.at - prev.at < SWAP_OUT + SWAP_IN) say(`scambio di contenuto a ${k.at - prev.at} battiti dalla chiave prima: testi sovrapposti (servono ${SWAP_OUT + SWAP_IN})`);
  });
  return bad;
};
