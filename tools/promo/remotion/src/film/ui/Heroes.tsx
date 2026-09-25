import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import geo from "../mockup.geometry.json";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Pose } from "../moves.ts";
import type { Fx, Scene } from "../timeline.ts";
import { THEME } from "../theme.ts";
import { HERO_OPTION_PX, cardOutAt, gaugeHeroAt, isHero, optionsBuildAt, optionsRest, panelHeroAt, terminalPlaneAt } from "./heroes.ts";
import type { Flight } from "./heroes.ts";
import { Plane3D } from "./Plane3D.tsx";
import { UiCard } from "./UiCard.tsx";
import { UiGauge } from "./UiGauge.tsx";
import { UiOption } from "./UiOption.tsx";
import { UiBriefContext, UiBriefWork } from "./UiBrief.tsx";
import { UiClaudeCode } from "./UiClaudeCode.tsx";
import { CC } from "./claudeCode.ts";

/** Larghezza della card da protagonista, diametro del gauge, larghezza del tasto e della finestra del PC, nel quadro. */
export const HERO_CARD_PX = 900, HERO_GAUGE_PX = 640, HERO_TERMINAL_PX = 1700;   // HERO_OPTION_PX sta in heroes.ts, con il posto dei tasti
/** La scheda che si ferma grande al centro misura 804 px: la card di 427 unità a zoom `AROUND_ZOOM` (heroes.ts). */
export const HERO_CARD_CENTER_PX = 804;

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
  const { width } = useVideoConfig();
  const e = (scene.fx ?? []).find((f) => f.kind === "terminalPlane");
  if (!e || e.kind !== "terminalPlane") return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const c0 = terminalPlaneAt((frame - from) / len);
  // `keep`: il terminale NON si ritira a fine scena, perché la scena dopo lo riprende con le stesse righe e ci costruisce
  // attorno la finestra: spegnendolo si vedeva il testo sparire e ricomparire (Franz, 20/09 19:55)
  const c = e.keep ? { show: c0.show, exit: 0 } : c0;
  // Claude Code (Franz, 21/09 19:46): intestazione, prompt e casella ci sono dal primo fotogramma — il cerchio dell'invio
  // li rivela, e sul prompt si posa il testo dettato (`promptAt`). Le righe arrivano come prima.
  const cc = Boolean(e.prompt);
  if (c.show <= 0 && !cc) return null;
  const every = spanFrames(g, scene.at, e.every);
  // le righe che il display sta GIÀ mostrando quando il terminale compare, più quelle che arrivano dopo: il fondo e
  // l'orologio devono dire la stessa cosa nello stesso momento (Franz, 20/09 16:47).
  const start = e.start ?? 0;
  // non in tempo reale, ma sfalsato di poco (Franz, 20/09 17:14): la riga la scrive il PC e il polso la ripete sei
  // fotogrammi dopo. Perfettamente sincroni sembravano lo stesso oggetto, non due schermi collegati.
  const LEAD = 6;
  // Le righe già scritte non compaiono tutte insieme: si posano una alla volta mentre il terminale entra in quadro
  // (Franz, 20/09 17:39), dieci fotogrammi l'una. Quelle nuove arrivano invece al ritmo vero del polso, sei fotogrammi
  // prima di lui. I risultati «⎿» non contano nel ritmo: seguono la loro riga dieci fotogrammi dopo.
  const BACKFILL = 7;
  const main = e.lines.map((l) => !l.startsWith("⎿"));
  let n = -1;
  const idx = e.lines.map((_, i) => (main[i] ? ++n : n));
  // con `times` il PC scrive ogni riga al suo battito: il polso la ripete dopo, e prima non la mostra (Franz, 21/09 20:40:
  // «il testo del terminale viene prima»)
  const arrivo = (i: number) => {
    if (e.times) return spanFrames(g, scene.at, e.times[i]);
    const m = idx[i];
    const base = m < start ? from + m * BACKFILL : from + (m - start + 1) * every - LEAD;
    return main[i] ? base : base + 10;
  };
  const a = c.show * (1 - c.exit);
  // Come Claude Code sul PC: ogni frase nasce INTERA in fondo, sopra la casella, e cresce spingendo su le altre (Franz, 21/09
  // 20:24: «a blocchi di frasi … e salire verso l'alto»); prima uscivano due parole alla volta in una colonna che scendeva.
  // E chi è avanti è il fondo: il polso ripete dopo (`LEAD`).
  const clamp01 = (v: number) => Math.min(1, Math.max(0, v));
  // dodici fotogrammi per nascere: con sette sembrava uno scatto (Franz, 21/09 21:16)
  const rows = e.lines.map((l, i) => ({ text: l, on: frame < arrivo(i) ? 0 : clamp01((frame - arrivo(i) + 1) / 12) * (cc ? 1 : a) }));
  const promptOn = e.promptAt !== undefined ? clamp01((frame - spanFrames(g, scene.at, e.promptAt)) / 2) : 1;
  return (
    <div style={{ position: "absolute", left: THEME.leftMargin, top: 0, bottom: 0, width: width * CC.column }}>
      <UiClaudeCode width={width * CC.column} header={cc ? { path: e.path ?? "", model: e.model ?? "" } : undefined} chrome={cc ? 1 : a}
        prompt={e.prompt} promptOn={promptOn} rows={rows} status={e.status} frame={frame} />
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
          if (e.inShape) return null;   // li disegna la forma unica (ShapeContent): qui restano tempi, camera e fuoco
          const o = optionsBuildAt(p, e.pressAt !== undefined ? e.pressAt / e.len : undefined);
          if (o.alpha <= 0) return null;
          // i tasti ESCONO dal display: partono dal loro rettangolo vero (stessa misura, stesso posto) e si posano al
          // centro del quadro. L'orologio in questa scena sta fermo (`steady`), altrimenti il punto di partenza scivola.
          const rest = optionsRest(e.yes, e.no, HERO_OPTION_PX, width, height), k = rest.k;
          const lerp = (a: number, b: number, t: number) => a + (b - a) * t;
          const centre = (r: [number, number, number, number]) => [dx + (r[0] + r[2] / 2 - 240) * u, dy + (r[1] + r[3] / 2 - 240) * u] as const;
          const toYes: readonly [number, number] = [rest.yes.x, rest.yes.y];
          const toNo: readonly [number, number] = [rest.no.x, rest.no.y];
          const place = (r: [number, number, number, number], to: readonly [number, number]) => {
            const [x0, y0] = centre(r);
            return { x: lerp(x0, to[0], o.travel), y: lerp(y0, to[1], o.travel), zoom: lerp(u, k, o.travel) };
          };
          const pYes = place(e.yes, toYes), pNo = place(e.no, toNo);
          // il tasto si schiaccia sotto il dito, scatta quando l'anello si chiude, poi cresce fino a coprire il quadro
          // il tasto NON si ingrandisce da solo: scatta e basta. A portarlo a tutto quadro è il campo del takeover, con la
          // sua curva più lunga — prima crescevano tutti e due, uno in mezzo secondo (Franz, 19/09 13:51)
          const grow = (1 + 0.13 * o.pop) * (1 - 0.045 * o.press);   // lo scatto alla fine della pressione: +13 % (era 7: Franz, 21/09 17:18)
          return (
            <React.Fragment key={i}>
              <div style={{ position: "absolute", inset: 0, opacity: 0.55 * o.travel * (1 - o.fill), background: "radial-gradient(60% 60% at 40% 50%, rgba(0,0,0,0) 30%, rgba(0,0,0,.85) 100%)" }} />
              <div style={{ position: "absolute", left: pNo.x, top: pNo.y, width: 0, height: 0, opacity: 1 - o.fill }}>
                <div style={{ translate: "-50% -50%", width: e.no[2] * pNo.zoom, height: e.no[3] * pNo.zoom }}>
                  <div style={{ zoom: pNo.zoom }}><UiOption w={e.no[2]} h={e.no[3]} label={e.noLabel} build={Math.max(0, o.build - 0.06)} /></div>
                </div>
              </div>
              <div style={{ position: "absolute", left: pYes.x, top: pYes.y, width: 0, height: 0 }}>
                <div style={{ translate: "-50% -50%", width: e.yes[2] * pYes.zoom, height: e.yes[3] * pYes.zoom, scale: String(grow) }}>
                  <div style={{ zoom: pYes.zoom }}><UiOption w={e.yes[2]} h={e.yes[3]} label={e.yesLabel} primary build={o.build} ring={o.ring} /></div>
                </div>
                {/* il dito, come il «mostra tocchi» degli screencast Android (Franz, 21/09 17:19): un cerchio grigio
                    semitrasparente si posa sul tasto quando la pressione comincia, resta finché il dito preme e sparisce
                    al rilascio. Fuori dallo scatto del tasto: il dito non si gonfia con lui. A destra dell'etichetta. */}
                {o.press > 0.001 ? (
                  <div style={{ position: "absolute", left: e.yes[2] * pYes.zoom * 0.2, top: 0, width: e.yes[2] * pYes.zoom * 0.22, height: e.yes[2] * pYes.zoom * 0.22, translate: "-50% -50%",   /* un polpastrello sul tasto del polso: poco più di un quinto della sua larghezza */
                    borderRadius: "50%", background: "rgba(58,60,70,.38)", boxShadow: "0 0 0 2px rgba(255,255,255,.75)",
                    opacity: Math.min(1, o.press * 3), scale: String(0.9 + 0.1 * Math.min(1, o.press * 3)) }} />
                ) : null}
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
