import React from "react";
import { UI } from "./UiTokens.ts";

/** Arco da `a` a `b` gradi (senso orario, 0 = ore 3) su raggio `r`, come `CircularProgressIndicator` di Wear M3. */
const arc = (cx: number, cy: number, r: number, a: number, b: number): string => {
  const P = (d: number) => [cx + r * Math.cos((d * Math.PI) / 180), cy + r * Math.sin((d * Math.PI) / 180)];
  const [x0, y0] = P(a), [x1, y1] = P(b);
  return `M ${x0} ${y0} A ${r} ${r} 0 ${b - a > 180 ? 1 : 0} 1 ${x1} ${y1}`;
};

/** Il colore del binario com'è sul display: l'inchiostro al 22 % sopra la superficie della card (`surfaceHigh`), già fuso, così
 *  disegnato sopra la card vera non si somma al binario vero e fuori dal display resta il colore dell'app. */
const onCard = (hex: string, alpha = 0.22): string => {
  const c = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [r, g, b] = c(hex).map((v, i) => Math.round(v * alpha + c(UI.surfaceHigh)[i] * (1 - alpha)));
  return `rgb(${r},${g},${b})`;
};

/** Angoli del gauge dell'app (`Gauge.kt`): apertura di 31° centrata sulle ore 6, il binario corre in senso orario da 105,5° per 329°.
 *  L'indicatore di Wear M3 parte 5° dopo l'inizio del binario, vale 3,09° per punto percentuale e accorcia l'arco di uno stacco
 *  fisso in pixel (18,4 px del display, cioè un angolo che cresce sull'anello interno). Misurato sui due anelli del fotogramma
 *  della quota (18/09): esterno 11 % visibile 110-136°, interno 36 % visibile 112-211°; i due punti fissano costante e stacco. */
export const GAUGE_START = 105.5, GAUGE_SWEEP = 329, GAUGE_INSET = 5, GAUGE_DEG_PER_PCT = 3.09, GAUGE_GAP_PX = 18.4;

/**
 * Il gauge doppio della quota (`Gauge.kt` con `second`), ricostruito nello spazio del display (1 dp = 2 px): 52 dp, tratti da
 * 6 dp con 2 dp di stacco, estremità tonde, ogni binario tinto del suo colore al 22 % sopra la card. Fuori le 5 ore, dentro la settimana.
 * `outer` e `inner` 0-1; `ghost` disegna solo i binari (il posto lasciato sul display).
 */
export const UiGauge: React.FC<{ size?: number; outer: number; inner: number; ghost?: boolean }> = ({ size = 104, outer, inner, ghost = false }) => {
  const stroke = 12, gap = 4, c = size / 2;
  const rOut = c - stroke / 2, rIn = c - stroke - gap - stroke / 2;
  const ring = (r: number, v: number, ink: string) => {
    const deg = 180 / Math.PI, cap = ((stroke / 2) / r) * deg;             // l'estremità tonda sporge di mezzo tratto: l'arco disegnato la tiene dentro
    const a = GAUGE_START + GAUGE_INSET + cap, b = a + Math.min(1, v) * 100 * GAUGE_DEG_PER_PCT - (GAUGE_GAP_PX / r) * deg;
    return (
      <>
        <path d={arc(c, c, r, GAUGE_START + cap, GAUGE_START + GAUGE_SWEEP - cap)} fill="none" stroke={onCard(ink)} strokeWidth={stroke} strokeLinecap="round" />
        {v > 0 && !ghost ? <path d={arc(c, c, r, a, Math.max(a + 0.01, b))} fill="none" stroke={ink} strokeWidth={stroke} strokeLinecap="round" /> : null}
      </>
    );
  };
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ display: "block" }}>
      {ring(rOut, outer, UI.briefRing)}
      {ring(rIn, inner, UI.briefWeek)}
    </svg>
  );
};
