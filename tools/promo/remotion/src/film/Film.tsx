import React from "react";
import { AbsoluteFill, Sequence, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import raw from "./timeline.json";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { MOVE_BEATS, closingAt, poseAt } from "./moves.ts";
import { totalBeats, validateTimeline } from "./timeline.ts";
import type { Scene } from "./timeline.ts";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";
import { SideWatch } from "./SideWatch.tsx";
import { Floating } from "./ui/Floating.tsx";
import { Aside } from "./ui/Aside.tsx";
import { WordMask } from "./WordMask.tsx";
import { THEME } from "./theme.ts";
import { useFilmFonts } from "./fonts.ts";
import { EndCard } from "./EndCard.tsx";
import { LogoMark } from "./LogoMark.tsx";
import { Heroes, TerminalBackdrop, cameraAt, heroState } from "./ui/Heroes.tsx";
import { Blink } from "./ui/Blink.tsx";
import { Carry } from "./ui/Carry.tsx";
import { TAKEOVER_CUT, Takeover } from "./ui/Takeover.tsx";
import { Blinds } from "./ui/Blinds.tsx";
import { BLIND_CUT, blindSoloAt } from "./ui/blinds.ts";
import geo from "./mockup.geometry.json";
import type { Key } from "./ui/carry.ts";
import { fxLayers } from "./Fx.tsx";
import { watchTextFor } from "./WatchText.tsx";
import { Soundtrack } from "./Soundtrack.tsx";
import { Whip } from "./ui/Whip.tsx";
import type { Stems } from "./Soundtrack.tsx";

/** Scaletta sbagliata = il film non parte: l'errore elenca tutti i problemi. */
export const TIMELINE = validateTimeline(raw);
export const GRID: Grid = { bpm: TIMELINE.bpm, fps: TIMELINE.fps, offsetSeconds: TIMELINE.offsetSeconds };
/** I fotogrammi sono assoluti: la musica parte dal fotogramma 0 e il battito 0 cade a offsetSeconds. */
export const filmFrames = (): number => beatToFrame(GRID, totalBeats(TIMELINE));

export const SceneView: React.FC<{ scene: Scene; overlay?: React.ReactNode; around?: React.ReactNode }> = ({ scene, overlay, around }) => {
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const total = spanFrames(GRID, scene.at, scene.len);
  const beat = spanFrames(GRID, scene.at, 1);
  const w = scene.watch;
  const closing = scene.endCard ? closingAt(frame / beat) : null;
  const pose = closing ? closing.pose : w ? poseAt(frame, total, beat * MOVE_BEATS, w.enter, w.exit, frame + beatToFrame(GRID, scene.at)) : null;
  // con la camera «around» l'orologio è CENTRATO sul quadro (è la scheda ferma al centro che detta il posto), non nella
  // colonna di destra: il titolo resta in alto a sinistra (Franz, 19/09 05:12)
  const cx = (scene.watch?.camera === "around" ? 0.5 : scene.text ? THEME.watchX : 0.5) * width;
  const textAt = spanFrames(GRID, scene.at, scene.text?.at ?? 0);
  // se l'orologio esce (di lato o ingrandendosi) attraversa la colonna del testo: il testo se ne va prima;
  // e se una card esce dal display (piano 3) prende lei il centro sinistro: il titolo le lascia il posto un attimo prima che si stacchi
  const hero = (scene.fx ?? []).find((f) => f.kind === "cardOut" || f.kind === "gaugeHero" || f.kind === "optionsBuild");
  // i dati della Panoramica stanno dove sta il titolo: il titolo se ne va prima che entri il primo (Franz, 18/09 21:42)
  const firstAside = (scene.fx ?? []).find((f) => f.kind === "aside");
  // con il battito di ciglia il titolo non se ne va: la sua parola in colore cresce e copre tutto (Blink, a livello del film)
  const leave = scene.out === "blink" ? total - BLINK_FRAMES : scene.text?.place === "top" ? total + 1000 : Math.min((w?.exit ? total - beat * MOVE_BEATS : total) - 8, hero ? spanFrames(GRID, scene.at, hero.at) - 6 : total + 1000, firstAside ? spanFrames(GRID, scene.at, firstAside.at) - 10 : total + 1000);
  // mentre la card è protagonista ci si avvicina all'orologio (come nel Canvas di Google a 31,5 s: il componente davanti, l'interfaccia
  // enorme, scura e sfocata dietro): il display cresce, si sfoca e si scurisce, e torna a fuoco al rientro
  const { zoom, focus, watch: watchIn } = cameraAt(scene, GRID, frame);
  // quando l'orologio si materializza ATTORNO alla scheda ferma al centro, non è l'orologio a essere centrato: è la sua
  // scheda. Si sposta l'orologio di quanto la scheda dista dal centro del display, alla scala di quel momento.
  const aroundCard = scene.watch?.camera === "around" ? (scene.fx ?? []).find((f) => f.kind === "cardOut") : undefined;
  const aroundDy = aroundCard && aroundCard.kind === "cardOut"
    ? (240 - (aroundCard.rect[1] + aroundCard.rect[3] / 2)) * (((THEME.frontGlassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR) / 480) * zoom
    : 0;
  // quando un momento forte prende il quadro (il tasto che diventa sfondo) il titolo se ne va: sul chiaro non si leggerebbe
  const over = heroState(scene, GRID, frame).exit;
  // mentre il takeover cresce, il componente sotto sparisce: il takeover È quel componente, non una copia sopra
  const takeStart = scene.takeover ? total - Math.round(spanFrames(GRID, scene.at, scene.takeover.len) * TAKEOVER_CUT) : Infinity;
  const underTakeover = frame >= takeStart ? Math.min(1, (frame - takeStart) / 4) : 0;
  // quando parte la tapparella il quadro si svuota e restano sole le due barre, che diventano i primi listelli:
  // è così che si vede il collegamento fra il grafico e la transizione (Franz, 18/09 22:28)
  const blindStart = scene.blinds ? total - Math.round(spanFrames(GRID, scene.at, scene.blinds.len) * BLIND_CUT) : Infinity;
  const blindFrames = scene.blinds ? spanFrames(GRID, scene.at, scene.blinds.len) : 1;
  // il fade parte insieme al riempimento delle barre, 16 fotogrammi prima dell'innesco: così la crescita non si ferma
  // ad aspettare che il quadro si svuoti, le due cose corrono insieme (Franz, 22:41)
  const solo = frame >= blindStart - 16 ? blindSoloAt((frame - (blindStart - 16)) / blindFrames) : 0;
  // vibrazione: la notifica arriva e l'orologio trema per 10 fotogrammi (Franz, 13:13)
  const shakeAt = (scene.fx ?? []).find((f) => f.kind === "shake");
  const sh = shakeAt ? frame - spanFrames(GRID, scene.at, shakeAt.at) : -1;
  const shake = sh >= 0 && sh < 10 ? 4 * (1 - sh / 10) * Math.sin(sh * 2.6) : 0;
  // posizione e scala dell'orologio in un solo transform 3D con will-change: così Chrome tiene la deriva lenta a sottopixel invece
  // di arrotondare left/top a pixel interi (misurato il 18/09: 40k pixel di differenza ogni tre fotogrammi, uno scatto visibile)
  const extraOut = interpolate(frame, [leave, leave + (scene.out === "blink" ? 6 : 8)], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill>
      <Backdrop act={scene.act} glowX={scene.text ? THEME.watchX : 0.5} />
      <TerminalBackdrop scene={scene} g={GRID} />
      {w && pose && w.view === "side" ? (<>
        <div style={{ position: "absolute", width: 0, height: 0, left: 0, top: 0, transformOrigin: "0 0", willChange: "transform", transform: `translate3d(${width / 2 + pose.x * width}px, ${height * 0.70 + pose.y * height}px, 0) scale(${pose.scale})` }}>
          <SideWatch widthPx={THEME.sideCasePx} />
        </div>
        <div style={{ opacity: 1 - underTakeover }}><Floating scene={scene} g={GRID} glassY={height * 0.70} /></div>
        </>
      ) : w && pose && w.view !== "side" ? (
        <div style={{ position: "absolute", width: 0, height: 0, left: 0, top: 0, transformOrigin: "0 0", willChange: "transform", transform: `translate3d(${cx + pose.x * width + shake}px, ${height / 2 + pose.y * height + aroundDy}px, 0) scale(${pose.scale * zoom})`, opacity: watchIn * (1 - solo), filter: focus > 0 ? `blur(${8 * focus}px) brightness(${1 - 0.55 * focus})` : undefined }}>
          <PhotoWatch view={w.view} clip={w.clip} clipStart={w.clipStart} rate={w.rate} freeze={w.freeze} still={w.still} reveal={closing?.tilt} bodyOpacity={closing?.body} contentOpacity={closing?.logo} focus={closing?.focus} tilt={pose.tilt} overlay={closing ? <LogoMark draw={closing.draw} /> : overlay} around={around}
            glassPx={w.view === "threeQuarter" ? THEME.q34GlassPx : THEME.frontGlassPx} />
        </div>
      ) : null}
      {w?.view !== "side" && (scene.fx ?? []).some((f) => f.kind === "float") ? <div style={{ opacity: 1 - underTakeover }}><Floating scene={scene} g={GRID} /></div> : null}
      <Aside scene={scene} g={GRID} />
      {/* quando l'orologio si materializza attorno, la scheda ricostruita gli lascia il posto: dentro il display c'è la
          stessa scheda, nello stesso punto, e due copie sovrapposte si vedrebbero */}
      <div style={{ position: "absolute", inset: 0, opacity: (1 - underTakeover) * (scene.watch?.camera === "around" ? 1 - Math.max(0, (watchIn - 0.75) / 0.25) : 1) }}><Heroes scene={scene} g={GRID} watchCx={cx} pose={w?.view === "front" && pose ? { ...pose, scale: pose.scale * zoom } : null} glassPx={THEME.frontGlassPx} /></div>
      {w?.exit === "diveIn" ? <AbsoluteFill style={{ background: "#000", opacity: interpolate(frame, [total - beat * MOVE_BEATS * 0.55, total - 2], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }) }} /> : null}
      {scene.endCard ? <Sequence from={beat * 7} layout="none"><EndCard beat={beat} /></Sequence> : null}
      {scene.text ? (
        <Sequence from={textAt} layout="none">
          <div style={{ position: "absolute", opacity: (1 - Math.min(1, over * 2.5)) * (1 - solo), left: w ? THEME.leftMargin : 0, right: w ? undefined : 0, top: scene.text.place === "top" ? 110 : 0, bottom: 0, display: "flex", flexDirection: "column", alignItems: w ? "flex-start" : "center", justifyContent: scene.text.place === "top" ? "flex-start" : "center" }}>
            <WordMask lines={scene.text.lines} accent={scene.text.accent} size={scene.text.place === "top" ? "service" : scene.text.size} sub={scene.text.sub}
              fadeFrom={scene.out === "blink" ? total - 30 - textAt : undefined}
              perWordFrames={w ? Math.round(beat / 2) : beat} exitAt={scene.text.place === "top" ? undefined : leave - textAt} align={w ? "left" : "center"} />
            <div style={{ marginTop: 40, opacity: extraOut }}>{watchTextFor(scene, GRID, textAt)}</div>
          </div>
        </Sequence>
      ) : null}
    </AbsoluteFill>
  );
};

/** I battiti in cui cambia l'atto (senza apertura e chiusura, che hanno la loro cornice): lì passa la frustata. */
export const actChanges = (scenes: Scene[]): number[] =>
  scenes.filter((s, i) => i > 0 && s.act !== scenes[i - 1].act && s.act !== "close" && scenes[i - 1].act !== "open").map((s) => s.at);

/** Una chiave nel display diventa una chiave nel quadro: il display frontale sta al centro dell'orologio (`watchX` se c'è testo),
 *  con 480 unità = 2·displayR·glassPx/(2·glassR) pixel, a riposo (posa senza movimenti né deriva: sui tagli l'orologio è fermo). */
const toFrame = (k: Key & { space?: "display" | "frame" }, scene: Scene): Key => {
  if (k.space === "frame") return k;
  const u = ((THEME.frontGlassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR) / 480;
  const cx = (scene.text ? THEME.watchX : 0.5) * 1920;
  return { ...k, x: cx + (k.x - 240) * u, y: 540 + (k.y - 240) * u, w: k.w * u, h: k.h * u, stroke: (k.stroke ?? 4) * u };
};
const BLINK_FRAMES = 78;   // 2,6 s: la parola cresce per tutta la crescita della card, non solo nell'ultimo secondo
const CARRY_FRAMES = 22;   // 0,73 s: il passaggio si deve vedere (14 erano un lampo)

export const Film: React.FC<{ stems?: Stems }> = ({ stems }) => {
  useFilmFonts();
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <Soundtrack t={TIMELINE} g={GRID} stems={stems ?? "nosfx"} />
      {TIMELINE.scenes.map((s) => (
        <Sequence key={s.id} name={s.id} from={beatToFrame(GRID, s.at)} durationInFrames={spanFrames(GRID, s.at, s.len)}>
          <SceneView scene={s} {...fxLayers(s, GRID)} />
        </Sequence>
      ))}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.takeover || !next) return null;
        const k = s.takeover, frames = spanFrames(GRID, s.at, k.len);
        const body = k.body === "card" ? { kind: "card" as const, text: k.text ?? "" } : k.body === "words" ? { kind: "words" as const, words: k.words ?? [], card: k.card } : { kind: "plain" as const };
        return <Sequence key={`take-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * TAKEOVER_CUT)} durationInFrames={frames + 1} layout="none"><Takeover x={k.x} y={k.y} w={k.w} h={k.h} r={k.r} color={k.color} toColor={k.toColor} frames={frames} body={body} /></Sequence>;
      })}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.carryOut || !next?.carryIn) return null;
        // centrato sul taglio: l'oggetto lascia la scena negli ultimi 7 fotogrammi e arriva nei primi 7 della dopo
        return <Sequence key={`carry-${s.id}`} from={beatToFrame(GRID, next.at) - CARRY_FRAMES / 2} durationInFrames={CARRY_FRAMES + 1} layout="none"><Carry from={toFrame(s.carryOut, s)} to={toFrame(next.carryIn, next)} frames={CARRY_FRAMES} /></Sequence>;
      })}
      {/* la tapparella: chiusa a BLIND_CUT dell'arco, e lì cade il taglio con la scena dopo (revisione 3D, momento 1) */}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.blinds || !next) return null;
        const frames = spanFrames(GRID, s.at, s.blinds.len);
        return <Sequence key={`blinds-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * BLIND_CUT)} durationInFrames={frames + 1} layout="none"><Blinds frames={frames} /></Sequence>;
      })}
      {/* il battito di ciglia dura quanto la crescita della card, non 30 fotogrammi: parola e scheda crescono INSIEME
          (Franz, 19/09 05:12). La parola resta dov'è e si ingrandisce; le palpebre si chiudono negli ultimi 7. */}
      {TIMELINE.scenes.filter((s) => s.out === "blink").map((s) => (
        <Sequence key={`blink-${s.id}`} from={beatToFrame(GRID, s.at + s.len) - BLINK_FRAMES} durationInFrames={BLINK_FRAMES + 12} layout="none"><Blink word={s.text!.accent!} cut={BLINK_FRAMES} from={[387, 555]} to={[684, 140]} /></Sequence>
      ))}
      {actChanges(TIMELINE.scenes).map((b) => (
        <Sequence key={`whip-${b}`} from={beatToFrame(GRID, b) - 3} durationInFrames={8} layout="none"><Whip width={1920} height={1080} /></Sequence>
      ))}
    </AbsoluteFill>
  );
};
