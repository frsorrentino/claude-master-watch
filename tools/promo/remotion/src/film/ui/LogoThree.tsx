import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { logoSpinAt } from "./logo3d.ts";

/** Il segno dell'app in tre dimensioni: stesse proporzioni di `LogoMark` (spazio 480 del display), portate a `R` pixel di raggio. */
const CORAL = "#D97757", TRACK = "#3A404C";
const U = 300 / 92, G = U * 0.72, STROKE = 7.2 * G;         // le stesse unità del segno piatto
const R480 = 40.2 * U, ARC = (280 * Math.PI) / 180, START = (130 * Math.PI) / 180;

/** Un tratto tondo da `a` a `b` (coordinate dello spazio 480, y in giù), come lo `strokeLinecap="round"` del segno piatto. */
const Bar: React.FC<{ a: [number, number]; b: [number, number]; k: number; r: number; glow: number }> = ({ a, b, k, r, glow }) => {
  const dx = (b[0] - a[0]) * k, dy = -(b[1] - a[1]) * k;
  const len = Math.hypot(dx, dy);
  return (
    <mesh position={[((a[0] + b[0]) / 2) * k, (-(a[1] + b[1]) / 2) * k, 0]} rotation={[0, 0, Math.atan2(dy, dx) - Math.PI / 2]}>
      <capsuleGeometry args={[r, len, 6, 16]} />
      <meshStandardMaterial color={CORAL} emissive={CORAL} emissiveIntensity={0.15 + 0.5 * glow} roughness={0.38} metalness={0.25} />
    </mesh>
  );
};

/**
 * La chiusura in 3D (revisione, momento 3): il segno arriva **di taglio** — si vede solo il suo spessore — gira e si
 * presenta di faccia mentre l'arco del quadrante si disegna. Sostituisce la comparsa piatta, che Franz leggeva come
 * slegata dalle immagini. Ogni movimento viene da `useCurrentFrame()`.
 */
export const LogoThree: React.FC<{ frames: number; size?: number }> = ({ frames, size = 190 }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const s = logoSpinAt(Math.min(1, Math.max(0, frame / Math.max(1, frames))));
  const k = size / R480;                                     // dallo spazio 480 ai pixel del quadro
  const tube = (STROKE / 2) * k, cy = -2 * U;                // il segno sta un filo sopra il centro, come nel piatto
  const dist = (height / 2) / Math.tan((16 * Math.PI) / 180);
  return (
    <ThreeCanvas width={width} height={height} camera={{ fov: 32, position: [0, 0, dist], near: 1, far: dist * 3 }}>
      <ambientLight intensity={0.5} />
      <directionalLight position={[-600, 800, 900]} intensity={1.6} />
      <directionalLight position={[700, -400, 500]} intensity={0.5} />
      <group rotation={[0, s.yaw, 0]}>
        {/* il quadrante: binario intero e arco corallo che si disegna fino al 70 % */}
        <mesh rotation={[0, 0, -START]}>
          <torusGeometry args={[size, tube, 18, 120, ARC]} />
          <meshStandardMaterial color={TRACK} roughness={0.6} metalness={0.1} />
        </mesh>
        {s.draw > 0.005 ? (
          <mesh rotation={[0, 0, -START]}>
            <torusGeometry args={[size, tube * 1.02, 18, 120, ARC * 0.7 * s.draw]} />
            <meshStandardMaterial color={CORAL} emissive={CORAL} emissiveIntensity={0.18 + 0.6 * s.glow} roughness={0.36} metalness={0.25} />
          </mesh>
        ) : null}
        {/* il «>» e il trattino, con le proporzioni del segno piatto */}
        <Bar a={[-22 * G, cy - 17 * G]} b={[-3 * G, cy]} k={k} r={tube} glow={s.glow} />
        <Bar a={[-3 * G, cy]} b={[-22 * G, cy + 17 * G]} k={k} r={tube} glow={s.glow} />
        <Bar a={[2 * G + STROKE / 2, cy + 13.4 * G + STROKE / 2]} b={[23 * G - STROKE / 2, cy + 13.4 * G + STROKE / 2]} k={k} r={tube} glow={s.glow} />
      </group>
    </ThreeCanvas>
  );
};
