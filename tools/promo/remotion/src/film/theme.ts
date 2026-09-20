import type { Act } from "./timeline.ts";

export const THEME = {
  white: "#F4F2EC",            // bianco caldo
  accent: "#9EBEFF",           // il blu pastello dell'app
  dim: "#AAB2CD",
  title: 110, service: 44, leftMargin: 150,
  watchX: 0.69,                // centro dell'orologio quando c'è testo a sinistra
  frontGlassPx: 740,           // diametro del vetro nelle scene di lettura: display = 0,86 × 740 = 636 px = 59 % di 1080
  q34GlassPx: 760,
  sideCasePx: 820,            // larghezza della cassa nella vista laterale: l'orologio riempie il quadro in basso
};

/** Inchiostro per le scritte quando la scena nasce su un campo chiaro (il celeste del takeover): bianco su celeste non si
 *  legge — misurato il 20/09, contrasto quasi nullo per un secondo e mezzo. */
export const INK = { text: "#14203A", accent: "#2C58C8" };

/** Fonde due colori esadecimali: `t` 0 = il primo, 1 = il secondo. Serve a far passare le scritte da inchiostro a bianco
 *  mentre il campo chiaro diventa il fondo scuro dell'atto. */
export const mix = (a: string, b: string, t: number): string => {
  const c = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [r1, g1, b1] = c(a), [r2, g2, b2] = c(b);
  const v = (x: number, y: number) => Math.round(x + (y - x) * Math.min(1, Math.max(0, t)));
  return `rgb(${v(r1, r2)}, ${v(g1, g2)}, ${v(b1, b2)})`;
};

/** Un colore per atto: centro, mezzo, bordo del gradiente, e l'alone dietro l'orologio (luce che si SOMMA al fondo: fusione «schermo»). */
export const ACT_BG: Record<Act, [string, string, string, string]> = {
  open: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
  know: ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
  // il viola tirato verso il blu di «know» (Franz, 20/09 10:38): con l'isola chiara in mezzo, cinque famiglie di colore
  // erano una di troppo, e quella senza significato era il viola. Ora: nero, blu, blu-violetto, isola chiara, verde-azzurro.
  act: ["#44416E", "#292949", "#15152B", "rgb(40,36,68)"],
  control: ["#1F5A5E", "#15393F", "#0C1F26", "rgb(18,52,54)"],
  close: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
};
