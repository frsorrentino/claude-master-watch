import React from "react";
import { UI } from "./UiTokens.ts";

/** Arco da `a` a `b` gradi (senso orario, 0 = ore 3) su raggio `r`, come `CircularProgressIndicator` di Wear M3. */
const arc = (cx: number, cy: number, r: number, a: number, b: number): string => {
  const P = (d: number) => [cx + r * Math.cos((d * Math.PI) / 180), cy + r * Math.sin((d * Math.PI) / 180)];
  const [x0, y0] = P(a), [x1, y1] = P(b);
  return `M ${x0} ${y0} A ${r} ${r} 0 ${b - a > 180 ? 1 : 0} 1 ${x1} ${y1}`;
};

/** Angoli del gauge dell'app (`Gauge.kt`): apertura di 31° centrata sulle ore 6, il binario corre in senso orario da 105,5° per 329°.
 *  L'indicatore di Wear M3 lascia uno stacco dal binario e tiene le estremità tonde dentro l'arco: misurato sul fotogramma della
 *  quota (18/09), l'arco visibile parte 4,5° dopo l'inizio del binario e vale 2,75° per punto percentuale. */
export const GAUGE_START = 105.5, GAUGE_SWEEP = 329, GAUGE_INSET = 4.5, GAUGE_DEG_PER_PCT = 2.75;

/**
 * Il gauge doppio della quota (`Gauge.kt` con `second`), ricostruito nello spazio del display (1 dp = 2 px): 52 dp, tratti da
 * 6 dp con 2 dp di stacco, estremità tonde, ogni binario tinto del suo colore al 22 %. Fuori le 5 ore, dentro la settimana.
 * `outer` e `inner` 0-1; `ghost` disegna solo i binari (il posto lasciato sul display).
 */
export const UiGauge: React.FC<{ size?: number; outer: number; inner: number; ghost?: boolean }> = ({ size = 104, outer, inner, ghost = false }) => {
  const stroke = 12, gap = 4, c = size / 2;
  const rOut = c - stroke / 2, rIn = c - stroke - gap - stroke / 2;
  const ring = (r: number, v: number, ink: string) => {
    const cap = ((stroke / 2) / r) * (180 / Math.PI);                      // l'estremità tonda sporge di mezzo tratto: l'arco disegnato la tiene dentro
    const a = GAUGE_START + GAUGE_INSET + cap, b = GAUGE_START + GAUGE_INSET + Math.min(1, v) * 100 * GAUGE_DEG_PER_PCT - cap;
    return (
      <>
        <path d={arc(c, c, r, GAUGE_START + cap, GAUGE_START + GAUGE_SWEEP - cap)} fill="none" stroke={ink} strokeOpacity={0.22} strokeWidth={stroke} strokeLinecap="round" />
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
