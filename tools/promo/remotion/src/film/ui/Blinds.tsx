import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { blindAt, blindDark } from "./blinds.ts";
import { UI } from "./UiTokens.ts";

/** Quanti listelli e quanto sono spessi: dodici su 1080 px fanno 90 px l'uno, spessore 22 px — abbastanza per prendere luce di taglio. */
export const BLIND_N = 12, BLIND_H = 90, BLIND_T = 22;
/** I listelli che ERANO le barre del Context: stessa riga, stessa larghezza, stesso colore. Le barre stanno a 122 px l'una
 *  dall'altra e il loro blocco resta centrato su y = −69 qualunque sia il numero di righe (misurate: con due a −8 e −130, con
 *  tre a 53, −69 e −191 — fotogramma 1868 del 21/09). */
const BAR = { w: 760, h: 16, left: -810, step: 122, mid: -69 };   // `left` = bordo sinistro in coordinate di quadro (x 150 px)
const barY = (k: number, n: number) => BAR.mid + BAR.step * ((n - 1) / 2 - k);

const mixRgb = (a: string, b: string, t: number): number[] => {
  const c = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [x, y] = [c(a), c(b)], k = Math.min(1, Math.max(0, t));
  return x.map((v, i) => Math.round(v + (y[i] - v) * k));
};
/** Il colore del listello, scurito verso il nero di `dark` 0-1 (con 0 è quello di sempre, arrotondato allo stesso modo). */
const shade = (rgb: number[], dark: number): string => `rgb(${(dark > 0 ? rgb.map((v) => Math.round(v * (1 - dark))) : rgb).join(",")})`;

/**
 * La tapparella (revisione 3D, momento 1): le due barre del Context diventano i primi due listelli, la tapparella si chiude
 * sul quadro e si volta sul proprio asse scoprendo la scena dopo. In Three vero, non in CSS, per due motivi: i listelli hanno
 * spessore e prendono la luce mentre girano, e la camera resta ferma davanti a un oggetto che ruota (in CSS si schiaccerebbe).
 * Ogni movimento è guidato da `useCurrentFrame()` (regola della skill: niente `useFrame`, o il rendering sfarfalla).
 */
export const Blinds: React.FC<{ frames: number; bars?: number[]; to?: "black" }> = ({ frames, bars = [6, 7], to }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const p = Math.min(1, Math.max(0, frame / Math.max(1, frames)));
  const dist = (height / 2) / Math.tan((16 * Math.PI) / 180);        // fov 32°: a z = 0 il quadro è alto esattamente `height`
  const dark = blindDark(p, to);
  return (
    <ThreeCanvas width={width} height={height} camera={{ fov: 32, position: [0, 0, dist], near: 1, far: dist * 3 }}>
      <ambientLight intensity={0.42} />
      <directionalLight position={[-500, 1400, 260]} intensity={1.9} />
      <directionalLight position={[700, -900, 500]} intensity={0.6} />
      {Array.from({ length: BLIND_N }, (_, i) => {
        const b = blindAt(p, i, BLIND_N, bars);
        if (b.alpha <= 0.001 || b.born <= 0.001) return null;
        const isBar = bars.includes(i);
        const yGrid = (BLIND_N / 2 - i - 0.5) * BLIND_H;
        // la barra parte dove stava sul quadro e cresce fino al suo posto nella griglia; gli altri nascono già a posto e si stendono
        const w = isBar ? BAR.w + (width * 1.15 - BAR.w) * b.spread : width * 1.15 * b.born;
        // la barra cresce dal suo bordo sinistro (com'è sul quadro) e solo alla fine il listello è centrato
        const h = isBar ? BAR.h + (BLIND_H - BAR.h) * b.spread : BLIND_H;
        const x = isBar ? (BAR.left + w / 2) * (1 - Math.pow(b.spread, 1.6)) : 0;   // il bordo sinistro resta fermo, poi il listello si centra
        const y0 = barY(bars.indexOf(i), bars.length);
        const y = isBar ? y0 + (yGrid - y0) * b.settle : yGrid;
        // i due colori della quota: blu al centro (le barre) e viola verso i capi, così la tapparella è fatta dei dati appena visti
        const far = Math.min(...bars.map((k) => Math.abs(i - k))) / (BLIND_N - 1);
        return (
          <mesh key={i} position={[x, y, 0]} rotation={[b.rot, 0, 0]} scale={[w, h, BLIND_T]}>
            <boxGeometry args={[1, 1, 1]} />
            <meshStandardMaterial color={shade(mixRgb(UI.briefRing, UI.briefWeek, far * 1.2), dark)} roughness={0.42} metalness={0.18} transparent opacity={b.alpha} />
          </mesh>
        );
      })}
    </ThreeCanvas>
  );
};
