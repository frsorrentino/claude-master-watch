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

/** Un colore per atto: centro, mezzo, bordo del gradiente, e l'alone dietro l'orologio (luce che si SOMMA al fondo: fusione «schermo»). */
export const ACT_BG: Record<Act, [string, string, string, string]> = {
  open: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
  know: ["#3A4468", "#242A42", "#14172A", "rgb(30,35,60)"],
  act: ["#4A3670", "#2C2148", "#17122A", "rgb(44,30,66)"],
  control: ["#1F5A5E", "#15393F", "#0C1F26", "rgb(18,52,54)"],
  close: ["#000000", "#000000", "#000000", "rgb(0,0,0)"],
};
