import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { wallAt, wallPlateAt } from "./wall.ts";
import { UI } from "./UiTokens.ts";

/** Le lastre: un account per lastra, con il suo colore e le sue righe. Il primo è quello della scena prima (il terminale). */
const ACCOUNTS = [
  { name: "payments-api · Pro", color: "#3C81F2", lines: ["$ pytest -q tests", "42 passed in 3.1s", "$ git tag v2.8.0"] },
  { name: "storefront · Usr", color: "#2ECC71", lines: ["$ npm run build", "built in 8.4s", "$ vercel deploy"] },
  { name: "blog · Usr", color: "#E3B341", lines: ["$ hugo --minify", "12 pages, 0 draft", "$ rsync -az public/"] },
  { name: "ios-app · Pro", color: "#B57EDC", lines: ["$ xcodebuild test", "** TEST SUCCEEDED **", "$ fastlane beta"] },
  { name: "infra · Pro", color: "#E06C5A", lines: ["$ terraform plan", "3 to add, 0 to change", "$ terraform apply"] },
];
const COLS = ACCOUNTS.length, ROWS = 2, PW = 520, PH = 330, GAP = 58;

/** La superficie di una lastra, disegnata una volta su un canvas: è una finestra di terminale come quella della scena prima.
 *  Il canvas diventa texture con l'elemento `<canvasTexture>` di React Three Fiber, così non serve importare three. */
const usePlateCanvas = (a: (typeof ACCOUNTS)[number], dim: number): HTMLCanvasElement =>
  React.useMemo(() => {
    const c = document.createElement("canvas");
    c.width = 1040;
    c.height = 660;
    const g = c.getContext("2d")!;
    g.fillStyle = UI.surface;
    g.fillRect(0, 0, c.width, c.height);
    g.fillStyle = "rgba(255,255,255,.05)";
    g.fillRect(0, 0, c.width, 96);
    g.fillStyle = a.color;
    g.beginPath();
    g.arc(52, 48, 17, 0, Math.PI * 2);
    g.fill();
    g.font = "500 34px Inter, sans-serif";
    g.fillStyle = UI.text2;
    g.fillText(a.name, 86, 60);
    g.font = "400 34px Cousine, monospace";
    a.lines.forEach((l, i) => {
      g.fillStyle = i === a.lines.length - 1 ? UI.text : UI.text2;
      g.fillText(l, 52, 190 + i * 66);
    });
    g.fillStyle = `rgba(4,6,12,${dim})`;
    g.fillRect(0, 0, c.width, c.height);
    return c;
  }, [a, dim]);

const Plate: React.FC<{ a: (typeof ACCOUNTS)[number]; x: number; y: number; z: number; alpha: number; hero: boolean }> = ({ a, x, y, z, alpha, hero }) => {
  const canvas = usePlateCanvas(a, hero ? 0 : 0.28);
  return (
    <mesh position={[x, y, z]} scale={[PW, PH, 1]}>
      <planeGeometry args={[1, 1]} />
      <meshBasicMaterial transparent opacity={alpha}>
        <canvasTexture attach="map" args={[canvas]} />
      </meshBasicMaterial>
    </mesh>
  );
};

/**
 * Il muro degli account (revisione 3D, momento 2): la finestra del terminale che riempie il quadro resta ferma, la camera
 * ARRETRA e si scopre che è una lastra fra tante — un account per lastra, su una parete appena curva — poi rientra in una
 * sola lastra. Serve il 3D vero: è parallasse e profondità, che in CSS diventano solo rettangoli più piccoli.
 * La foschia (`fog`) fa il lavoro della profondità di campo senza postprocessing.
 */
export const AccountWall: React.FC<{ frames: number; bg?: string }> = ({ frames, bg = "#0B1A1F" }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const w = wallAt(Math.min(1, Math.max(0, frame / Math.max(1, frames))));
  const unit = (height / 2) / Math.tan((16 * Math.PI) / 180);       // distanza a cui un'unità è un pixel
  // si parte dentro la lastra centrale (riempie il quadro), si arretra fino a vedere tutto il muro, poi si rientra
  const near = unit * (PW / (width * 1.08)), far = unit * ((COLS * (PW + GAP)) / (width * 0.92));
  const z = near + (far - near) * w.back - (far - near) * 0.82 * w.inAgain;
  const heroIdx = 0;
  return (
    <ThreeCanvas width={width} height={height} camera={{ fov: 32, position: [0, 0, z], near: 1, far: far * 4 }}>
      <fogExp2 attach="fog" args={[bg, 0.00022]} />
      <ambientLight intensity={1} />
      {ACCOUNTS.flatMap((a, i) =>
        Array.from({ length: ROWS }, (_, r) => {
          const alpha = r === 0 ? wallPlateAt(frame / Math.max(1, frames), i, COLS, heroIdx) : wallPlateAt(frame / Math.max(1, frames), i, COLS, -1) * 0.82;
          if (alpha <= 0.002) return null;
          const dx = (i - (COLS - 1) / 2) * (PW + GAP);
          return (
            <Plate key={`${i}-${r}`} a={ACCOUNTS[(i + r * 2) % COLS]} x={dx + w.drift * 300} y={(0.5 - r) * (PH + GAP)} z={-Math.pow(i - (COLS - 1) / 2, 2) * 46} alpha={alpha} hero={r === 0 && i === heroIdx} />
          );
        }),
      )}
    </ThreeCanvas>
  );
};
