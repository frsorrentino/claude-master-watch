import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import geo from "../mockup.geometry.json";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Pose } from "../moves.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { cardOutAt, gaugeHeroAt } from "./heroes.ts";
import type { Flight } from "./heroes.ts";
import { Plane3D } from "./Plane3D.tsx";
import { UiCard } from "./UiCard.tsx";
import { UiGauge } from "./UiGauge.tsx";

/** Larghezza della card da protagonista e diametro del gauge da protagonista, nel quadro. */
export const HERO_CARD_PX = 900, HERO_GAUGE_PX = 640;

/** Il momento forte di una scena in questo fotogramma: avanzamento `p` (prima di 0 non è iniziato, da 1 è finito) e quanto è
 *  «fuori» (`travel`). Senza momento forte: p = −1, travel = 0. */
export const heroState = (scene: Scene, g: Grid, frame: number): { p: number; travel: number } => {
  for (const e of scene.fx ?? []) {
    if (e.kind !== "cardOut" && e.kind !== "gaugeHero") continue;
    const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
    const p = (frame - from) / len;
    return { p, travel: p >= 0 && p < 1 ? (e.kind === "cardOut" ? cardOutAt(p) : gaugeHeroAt(p)).travel : 0 };
  }
  return { p: -1, travel: 0 };
};

/** La camera della scena (piano 4): `zoom` sull'orologio e `focus` (0 a fuoco, 1 sfocato e scuro dietro al protagonista). */
export const cameraAt = (scene: Scene, g: Grid, frame: number): { zoom: number; focus: number } => {
  const { p, travel } = heroState(scene, g, frame);
  if (scene.watch?.camera === "release") {
    // si parte vicini (dopo il battito di ciglia); mentre il componente esce la camera torna indietro e resta lì
    if (p < 0) return { zoom: 1.55, focus: 0 };
    if (p >= 1) return { zoom: 1, focus: 0 };
    return { zoom: 1 + 0.55 * (1 - travel), focus: 0 };
  }
  return { zoom: 1 + 0.55 * travel, focus: travel };
};

/** Un componente che vola dal display (rettangolo in unità del display, 480) al posto da protagonista (centro e scala nel quadro). */
const Flying: React.FC<{ c: Flight; from: [number, number, number, number]; u: number; dx: number; dy: number; to: [number, number]; toScale: number; children: React.ReactNode }> = ({ c, from, u, dx, dy, to, toScale, children }) => {
  const [rx, ry, rw, rh] = from;
  // dal display a protagonista in linea retta: è la crescita a dare il volo
  const x0 = dx + (rx + rw / 2 - 240) * u, y0 = dy + (ry + rh / 2 - 240) * u;
  const x = x0 + (to[0] - x0) * c.travel, y = y0 + (to[1] + 8 * c.drift - y0) * c.travel;
  const scale = u + (toScale - u) * c.travel;
  // un asse solo: attorno alla verticale, il lato destro (quello che si stacca per ultimo) più vicino; fuori resta appena girato
  const yaw = 3 * c.travel + 16 * c.swing + 1.2 * c.drift * c.travel;
  return (
    <>
      {/* il campo si pulisce: una vignetta scurisce i bordi finché il componente è fuori */}
      <div style={{ position: "absolute", inset: 0, opacity: 0.55 * c.travel, background: "radial-gradient(60% 60% at 40% 50%, rgba(0,0,0,0) 30%, rgba(0,0,0,.85) 100%)" }} />
      <div style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: c.alpha }}>
        {/* il componente è impaginato alla grandezza da protagonista (`zoom`) e poi solo rimpicciolito: Chrome rasterizza alla
            grandezza impaginata, e ingrandire con `scale` un elemento da 104 px lo sgrana */}
        <div style={{ translate: "-50% -50%", width: "max-content", scale: String(scale / toScale) }}>
          <Plane3D ry={yaw} perspective={2600 / scale}><div style={{ zoom: toScale }}>{children}</div></Plane3D>
        </div>
      </div>
    </>
  );
};

/** Il gauge da protagonista: gli anelli dell'app, il numero che conta dentro, la frase sotto. Tutto in unità del display
 *  (poi scalato con il gauge): numero e frase compaiono solo quando il gauge è fuori. */
const GaugeHeroView: React.FC<{ e: Extract<Fx, { kind: "gaugeHero" }>; value: number; show: number }> = ({ e, value, show }) => {
  const n = Math.round(e.value * value);
  return (
    <div style={{ position: "relative", width: e.size, height: e.size }}>
      <UiGauge size={e.size} outer={(e.value / 100) * value} inner={(e.week / 100) * value} />
      {/* a destra del gauge, come «82/100» accanto alla barra nel riferimento: il numero grande e la frase sotto */}
      <div style={{ position: "absolute", left: "100%", top: "50%", translate: `${e.size * 0.12}px -50%`, opacity: show, whiteSpace: "nowrap", fontFamily: "Inter", color: THEME.white }}>
        <div style={{ fontWeight: 600, fontSize: e.size * 0.42, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>
          {n}<span style={{ color: THEME.dim, fontSize: "0.5em", marginLeft: "0.08em", fontWeight: 500 }}>{e.suffix}</span>{/* come l'app: grigio chiaro a corpo minore, l'unico accento del film resta il corallo */}
        </div>
        <div style={{ marginTop: e.size * 0.05, fontWeight: 500, color: THEME.dim, fontSize: e.size * 0.1 }}>{e.phrase}</div>
      </div>
    </div>
  );
};

/**
 * I momenti forti di una scena, disegnati sopra l'orologio. Il display frontale sta a `watchCx + pose.x·width`,
 * `height/2 + pose.y·height`, con 480 unità = 2·displayR·glassPx/(2·glassR)·pose.scale pixel: così un componente in coordinate
 * del display (rettangolo della scaletta) parte esattamente da dove sta sul fotogramma vero.
 */
export const Heroes: React.FC<{ scene: Scene; g: Grid; watchCx: number; pose: Pose | null; glassPx: number }> = ({ scene, g, watchCx, pose, glassPx }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  if (!pose) return null;
  const u = ((glassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR * pose.scale) / 480;
  const dx = watchCx + pose.x * width, dy = height / 2 + pose.y * height;
  return (
    <>
      {(scene.fx ?? []).map((e, i) => {
        if (e.kind !== "cardOut" && e.kind !== "gaugeHero") return null;
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const p = (frame - from) / len;
        if (e.kind === "cardOut") {
          const c = cardOutAt(p);
          if (c.alpha <= 0) return null;
          return (
            <Flying key={i} c={c} from={e.rect} u={u} dx={dx} dy={dy} to={[THEME.leftMargin + HERO_CARD_PX / 2, height / 2]} toScale={HERO_CARD_PX / e.rect[2]}>
              <UiCard w={e.rect[2]} name={e.name} age={e.age} text={e.text} badge={e.badge} icon={e.icon} light={c.travel} />
            </Flying>
          );
        }
        const c = gaugeHeroAt(p);
        if (c.alpha <= 0) return null;
        return (
          <Flying key={i} c={c} from={[e.cx - e.size / 2, e.cy - e.size / 2, e.size, e.size]} u={u} dx={dx} dy={dy} to={[THEME.leftMargin + HERO_GAUGE_PX / 2, height / 2]} toScale={HERO_GAUGE_PX / e.size}>
            <GaugeHeroView e={e} value={c.value} show={c.label} />
          </Flying>
        );
      })}
    </>
  );
};
