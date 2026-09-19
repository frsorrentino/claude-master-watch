import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import geo from "../mockup.geometry.json";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Pose } from "../moves.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { cardOutAt, gaugeHeroAt, optionsBuildAt, panelHeroAt, terminalPlaneAt } from "./heroes.ts";
import type { Flight } from "./heroes.ts";
import { Plane3D } from "./Plane3D.tsx";
import { UiCard } from "./UiCard.tsx";
import { UiGauge } from "./UiGauge.tsx";
import { UiOption } from "./UiOption.tsx";
import { UiTerminalPanel } from "./UiTerminalPanel.tsx";
import { UiBriefContext, UiBriefWork } from "./UiBrief.tsx";

/** Larghezza della card da protagonista, diametro del gauge, larghezza del tasto e della finestra del PC, nel quadro. */
export const HERO_CARD_PX = 900, HERO_GAUGE_PX = 640, HERO_OPTION_PX = 760, HERO_TERMINAL_PX = 1700;
/** La scheda che si ferma grande al centro (`toCenter`) e l'orologio che poi le compare attorno devono COMBACIARE: a
 *  zoom `AROUND_ZOOM` la card di 427 unità dentro il display misura 427 · 1,3258 · 1,42 = 804 px. Franz, 19/09 05:12:
 *  «la scheda esattamente nella stessa posizione grande centrata di prima». */
export const HERO_CARD_CENTER_PX = 804, AROUND_ZOOM = 1.42;
type Hero = Extract<Fx, { kind: "cardOut" | "gaugeHero" | "optionsBuild" | "panelHero" }>;
const isHero = (e: Fx): e is Hero => e.kind === "cardOut" || e.kind === "gaugeHero" || e.kind === "optionsBuild" || e.kind === "panelHero";

/** Il momento forte di una scena in questo fotogramma: avanzamento `p` (prima di 0 non è iniziato, da 1 è finito), quanto è
 *  «fuori» (`travel`) e quanto si sta consegnando alla scena dopo (`exit`). Senza momento forte: p = −1. */
export const heroState = (scene: Scene, g: Grid, frame: number): { p: number; travel: number; exit: number } => {
  for (const e of scene.fx ?? []) {
    if (!isHero(e)) continue;
    const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
    const p = (frame - from) / len;
    if (p < 0 || p >= 1) return { p, travel: 0, exit: p >= 1 ? 1 : 0 };
    if (e.kind === "cardOut") { const c = cardOutAt(p, e.fromOut, e.toCenter); return { p, travel: c.travel, exit: c.exit }; }
    if (e.kind === "gaugeHero") { const c = gaugeHeroAt(p); return { p, travel: c.travel, exit: c.exit }; }
    if (e.kind === "optionsBuild") { const c = optionsBuildAt(p); return { p, travel: c.travel, exit: c.fill }; }
    if (e.kind === "panelHero") { const c = panelHeroAt(p); return { p, travel: c.travel, exit: c.exit }; }
  }
  return { p: -1, travel: 0, exit: 0 };
};

/** La camera della scena (piano 4): `zoom` sull'orologio, `focus` (0 a fuoco, 1 sfocato e scuro dietro al protagonista),
 *  `watch` (opacità dell'orologio: con la camera `release` si materializza attorno alla card già fuori). */
export const cameraAt = (scene: Scene, g: Grid, frame: number): { zoom: number; focus: number; watch: number } => {
  const { p, travel, exit } = heroState(scene, g, frame);
  if (scene.watch?.camera === "around") {
    // la card è ferma al centro dal battito di ciglia: l'orologio compare attorno, grande e centrato, e resta
    const t = Math.min(1, Math.max(0, frame / 30));
    return { zoom: AROUND_ZOOM, focus: 0, watch: t * t * (3 - 2 * t) };
  }
  if (scene.watch?.camera === "release") {
    // dopo il battito di ciglia: card già al centro, l'orologio compare attorno e la camera torna indietro piano
    const t = Math.min(1, Math.max(0, frame / 40));
    const ease = t * t * (3 - 2 * t);
    return { zoom: 1.35 - 0.35 * ease, focus: 0.35 * (1 - exit), watch: Math.min(1, frame / 24) };
  }
  if (p < 0 || p >= 1) return { zoom: 1, focus: 0, watch: 1 };
  const near = travel * (1 - exit);
  return { zoom: 1 + 0.55 * near, focus: near, watch: 1 };
};

/**
 * Un componente che vola dal display (rettangolo in unità del display, 480) al posto da protagonista (centro e scala nel quadro)
 * e da lì si consegna alla scena dopo: si ritira verso il posto del titolo prossimo (`exitTo`) rimpicciolendo, e sparisce.
 */
const Flying: React.FC<{ c: Flight; from: [number, number, number, number]; u: number; dx: number; dy: number; to: [number, number]; toScale: number; exitTo: [number, number]; children: React.ReactNode }> = ({ c, from, u, dx, dy, to, toScale, exitTo, children }) => {
  const [rx, ry, rw, rh] = from;
  // dal display a protagonista in linea retta: è la crescita a dare il volo
  const x0 = dx + (rx + rw / 2 - 240) * u, y0 = dy + (ry + rh / 2 - 240) * u;
  const x1 = x0 + (to[0] - x0) * c.travel, y1 = y0 + (to[1] + 8 * c.drift - y0) * c.travel;
  const x = x1 + (exitTo[0] - x1) * c.exit, y = y1 + (exitTo[1] - y1) * c.exit;
  const scale = (u + (toScale - u) * c.travel) * (1 - 0.72 * c.exit);
  // un asse solo: attorno alla verticale, il lato destro (quello che si stacca per ultimo) più vicino; fuori resta appena girato
  const yaw = 3 * c.travel + 16 * c.swing + 1.2 * c.drift * c.travel;
  return (
    <>
      {/* il campo si pulisce: una vignetta leggera sui bordi finché il componente è protagonista */}
      <div style={{ position: "absolute", inset: 0, opacity: 0.3 * c.travel * (1 - c.exit), background: "radial-gradient(60% 60% at 40% 50%, rgba(0,0,0,0) 30%, rgba(0,0,0,.85) 100%)" }} />
      <div style={{ position: "absolute", left: x, top: y, width: 0, height: 0, opacity: c.alpha }}>
        {/* impaginato alla grandezza da protagonista (`zoom`) e poi solo rimpicciolito: ingrandire con `scale` un elemento da
            104 px lo sgrana; il contenitore ha la misura del rettangolo di partenza, così il centro è quello del rettangolo */}
        <div style={{ translate: "-50% -50%", width: rw * toScale, height: rh * toScale, scale: String(scale / toScale) }}>
          <Plane3D ry={yaw} perspective={2600 / scale}><div style={{ zoom: toScale }}>{children}</div></Plane3D>
        </div>
      </div>
    </>
  );
};

/** Il gauge da protagonista: gli anelli dell'app, il numero che conta a destra, la frase sotto (unità del display, poi zoom). */
const GaugeHeroView: React.FC<{ e: Extract<Fx, { kind: "gaugeHero" }>; value: number; show: number }> = ({ e, value, show }) => {
  const n = Math.round(e.value * value);
  return (
    <div style={{ position: "relative", width: e.size, height: e.size }}>
      <UiGauge size={e.size} outer={(e.value / 100) * value} inner={(e.week / 100) * value} />
      <div style={{ position: "absolute", left: "100%", top: "50%", translate: `${e.size * 0.12}px -50%`, opacity: show, whiteSpace: "nowrap", fontFamily: "Inter", color: THEME.white }}>
        <div style={{ fontWeight: 600, fontSize: e.size * 0.42, lineHeight: 1, letterSpacing: "-0.03em", fontVariantNumeric: "tabular-nums" }}>
          {n}<span style={{ color: THEME.dim, fontSize: "0.5em", marginLeft: "0.08em", fontWeight: 500 }}>{e.suffix}</span>
        </div>
        <div style={{ marginTop: e.size * 0.05, fontWeight: 500, color: THEME.dim, fontSize: e.size * 0.1 }}>{e.phrase}</div>
      </div>
    </div>
  );
};

/**
 * Il terminale del PC dietro l'orologio (Franz, 13:13): la finestra ricostruita compare in dissolvenza a tutto sfondo, con le
 * stesse righe che arrivano sul display; alla fine si ritira. Va disegnato SOTTO l'orologio: `SceneView` lo mette prima.
 */
export const TerminalBackdrop: React.FC<{ scene: Scene; g: Grid }> = ({ scene, g }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const e = (scene.fx ?? []).find((f) => f.kind === "terminalPlane");
  if (!e || e.kind !== "terminalPlane") return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const c = terminalPlaneAt((frame - from) / len);
  if (c.show <= 0) return null;
  const every = spanFrames(g, scene.at, e.every);
  const shown = 3 + Math.max(0, (frame - from - len * 0.2) / every);
  const k = HERO_TERMINAL_PX / e.rect[2];
  const a = c.show * (1 - c.exit);
  return (
    <div style={{ position: "absolute", left: width / 2, top: height / 2, width: 0, height: 0, opacity: a, filter: `blur(${1.5 * (1 - c.show) + 2 * c.exit}px)` }}>
      <div style={{ translate: "-50% -50%", width: e.rect[2] * k, scale: String(0.94 + 0.06 * c.show - 0.05 * c.exit) }}>
        <div style={{ zoom: k }}>
          <UiTerminalPanel w={e.rect[2]} header={e.header} title={e.title} lines={e.lines} shown={shown} morph={1} />
        </div>
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
  // dove nascerà il titolo della scena dopo: lì il componente si consegna
  const exitTo: [number, number] = [THEME.leftMargin + 120, height / 2 - 30];
  return (
    <>
      {(scene.fx ?? []).map((e, i) => {
        if (!isHero(e)) return null;
        const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
        const p = (frame - from) / len;
        if (e.kind === "optionsBuild") {
          const o = optionsBuildAt(p);
          if (o.alpha <= 0) return null;
          const k = HERO_OPTION_PX / e.yes[2], gap = (e.no[1] - (e.yes[1] + e.yes[3])) * k;
          const top = height / 2 - (e.yes[3] * k + gap + e.no[3] * k) / 2;
          const left = width / 2 - (e.yes[2] * k) / 2;                 // tasti al centro del quadro (Franz, 18/09 18:22)
          // dopo la pressione il tasto «yes» si gonfia e poi cresce fino a coprire il quadro: è lui lo sfondo della scena dopo
          const grow = 1 + 0.06 * o.pop + 12 * o.fill;
          return (
            <React.Fragment key={i}>
              <div style={{ position: "absolute", inset: 0, opacity: 0.55 * o.travel * (1 - o.fill), background: "radial-gradient(60% 60% at 40% 50%, rgba(0,0,0,0) 30%, rgba(0,0,0,.85) 100%)" }} />
              <div style={{ position: "absolute", left, top, opacity: 1 - o.fill }}>
                <div style={{ zoom: k }}>
                  <div style={{ height: e.yes[3] }} />
                  <div style={{ height: gap / k }} />
                  <UiOption w={e.no[2]} h={e.no[3]} label={e.noLabel} build={Math.max(0, o.build - 0.06)} />
                </div>
              </div>
              <div style={{ position: "absolute", left: left + (e.yes[2] * k) / 2, top: top + (e.yes[3] * k) / 2, width: 0, height: 0 }}>
                <div style={{ translate: "-50% -50%", width: e.yes[2] * k, height: e.yes[3] * k, scale: String(grow) }}>
                  <div style={{ zoom: k }}><UiOption w={e.yes[2]} h={e.yes[3]} label={e.yesLabel} primary build={o.build} ring={o.ring} /></div>
                </div>
              </div>
            </React.Fragment>
          );
        }
        if (e.kind === "cardOut") {
          const c = cardOutAt(p, e.fromOut, e.toCenter);
          if (c.alpha <= 0) return null;
          const to: [number, number] = e.toCenter ? [width / 2, height / 2] : e.fromOut ? [width * 0.42, height / 2] : [THEME.leftMargin + HERO_CARD_PX / 2, height / 2];
          return (
            <Flying key={i} c={c} from={e.rect} u={u} dx={dx} dy={dy} to={to} toScale={(e.toCenter ? HERO_CARD_CENTER_PX : HERO_CARD_PX) / e.rect[2]} exitTo={exitTo}>
              <UiCard w={e.rect[2]} name={e.name} age={e.age} text={e.text} badge={e.badge} icon={e.icon} light={c.travel} />
            </Flying>
          );
        }
        if (e.kind === "panelHero") {
          const c = panelHeroAt(p);
          if (c.alpha <= 0) return null;
          const K = 980 / e.rect[2];
          return (
            <Flying key={i} c={c} from={e.rect} u={u} dx={dx} dy={dy} to={[THEME.leftMargin + 980 / 2, height / 2]} toScale={K} exitTo={exitTo}>
              {e.panel === "work"
                ? <UiBriefWork w={e.rect[2]} n={e.n ?? 1} note={e.note ?? ""} bars={e.bars ?? [1, 1, 1]} draw={c.draw} />
                : <UiBriefContext w={e.rect[2]} rows={e.rows ?? []} draw={c.draw} />}
            </Flying>
          );
        }
        const c = gaugeHeroAt(p);
        if (c.alpha <= 0) return null;
        return (
          <Flying key={i} c={c} from={[e.cx - e.size / 2, e.cy - e.size / 2, e.size, e.size]} u={u} dx={dx} dy={dy} to={[THEME.leftMargin + HERO_GAUGE_PX / 2, height / 2]} toScale={HERO_GAUGE_PX / e.size} exitTo={exitTo}>
            <GaugeHeroView e={e} value={c.value} show={c.label} />
          </Flying>
        );
      })}
    </>
  );
};
