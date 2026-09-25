import React from "react";
import { AbsoluteFill, Easing, Sequence, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import raw from "./timeline.json";
import shortRaw from "./timeline.short.json";
import { beatToFrame, spanFrames } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { BLEED_LIFT, MOVE_BEATS, closingAt, displayUnit, poseAt, screenAt, watchPlace } from "./moves.ts";
import { validateTimeline, watchColumn } from "./timeline.ts";
import type { Fx, Scene, Timeline } from "./timeline.ts";
import { framesOf, gridOf } from "./cut.ts";
import { dollyAt } from "./dolly.ts";
import { Backdrop } from "./Backdrop.tsx";
import { PhotoWatch } from "./PhotoWatch.tsx";
import { SideWatch } from "./SideWatch.tsx";
import { Floating } from "./ui/Floating.tsx";
import { Aside } from "./ui/Aside.tsx";
import { WordMask } from "./WordMask.tsx";
import { THEME, actColors } from "./theme.ts";
import { useFilmFonts } from "./fonts.ts";
import { EndCard } from "./EndCard.tsx";
import { END_PACE, logoTrack } from "./endCard.ts";
import { LogoMark } from "./LogoMark.tsx";
import { AROUND_ZOOM, Heroes, TerminalBackdrop, cameraAt, heroState } from "./ui/Heroes.tsx";
import { AskDots } from "./ui/Dots.tsx";
import { TITLE_LEAD } from "./ui/dots.ts";
import { Blink } from "./ui/Blink.tsx";
import { Carry } from "./ui/Carry.tsx";
import { TAKEOVER_CUT, Takeover } from "./ui/Takeover.tsx";
import { takeoverAt } from "./ui/takeover.ts";
import { Flip } from "./ui/Flip.tsx";
import { FLIP_CUT } from "./ui/flip.ts";
import { sleepAt, sleepP, titleOnAt } from "./ui/sleep.ts";
import { beforeLids } from "./ui/blink.ts";
import { screenFadeAt } from "./ui/takeIn.ts";
import { TakeInFrame, TakeInFront, TakeInScreen } from "./ui/TakeIn.tsx";
import { Glow } from "./ui/Glow.tsx";
import { GLOW_CUT } from "./ui/glow.ts";
import { Bands } from "./ui/Bands.tsx";
import { Split } from "./ui/Split.tsx";
import { splitAt } from "./ui/split.ts";
import { Blinds } from "./ui/Blinds.tsx";
import { CameraMotionBlur } from "@remotion/motion-blur";
import { BLIND_CUT, blindBars, blindSoloAt } from "./ui/blinds.ts";
import geo from "./mockup.geometry.json";
import type { Key } from "./ui/carry.ts";
import { fxLayers } from "./Fx.tsx";
import { watchTextFor } from "./WatchText.tsx";
import { Soundtrack } from "./Soundtrack.tsx";
import { Whip } from "./ui/Whip.tsx";
import type { Stems } from "./Soundtrack.tsx";

/** Scaletta sbagliata = il film non parte: l'errore elenca tutti i problemi. */
export const FILM_TIMELINE = validateTimeline(raw);
/** La scaletta che il motore sta rendendo: la passa la composizione (il film lungo o il corto, 23/09). */
export const FilmTimeline = React.createContext<Timeline>(FILM_TIMELINE);
/** La sfocatura di movimento è accesa (`blur` della composizione): a false le scene con `blur` rendono come prima, per il confronto e per misurare il costo. */
export const FilmBlur = React.createContext<boolean>(true);
/** Quattro sotto-fotogrammi con otturatore a 180° (direttive di motion, 25/09: «4 subframes per frame … motion blur»): ogni strato
 *  si rende quattro volte a tempi diversi e si somma. Solo le scene che lo dichiarano (`blur` in scaletta), perché costa quattro volte. */
export const BLUR_SAMPLES = 4, BLUR_SHUTTER = 180;
const MB: React.FC<{ on?: boolean; children: React.ReactNode }> = ({ on, children }) => {
  const enabled = React.useContext(FilmBlur);
  return on && enabled ? <CameraMotionBlur samples={BLUR_SAMPLES} shutterAngle={BLUR_SHUTTER}>{children}</CameraMotionBlur> : <>{children}</>;
};
/** I fotogrammi sono assoluti: la musica parte dal fotogramma 0 e il battito 0 cade a offsetSeconds. */
export const filmFrames = (): number => framesOf(FILM_TIMELINE);
/** Il corto (design del 23/09): un'altra scaletta sullo stesso motore. */
export const SHORT_TIMELINE = validateTimeline(shortRaw);

export const SceneView: React.FC<{ scene: Scene; overlay?: React.ReactNode; around?: React.ReactNode }> = ({ scene, overlay, around }) => {
  const TIMELINE = React.useContext(FilmTimeline);
  const GRID: Grid = gridOf(TIMELINE);
  const frame = useCurrentFrame();
  const { width, height } = useVideoConfig();
  const total = spanFrames(GRID, scene.at, scene.len);
  const beat = spanFrames(GRID, scene.at, 1);
  const w = scene.watch;
  // il display che dorme (ui/sleep.ts): la finestra finisce sul taglio, e la scena dopo eredita il risveglio. La scena che
  // dorme è anche quella che si sposta: al buio la camera passa all'inquadratura della scena dopo.
  const idx = TIMELINE.scenes.indexOf(scene);
  const prev = TIMELINE.scenes[idx - 1];
  const next = TIMELINE.scenes[idx + 1];
  const nap = scene.sleep ?? prev?.sleep;
  const napOwn = Boolean(scene.sleep);
  const sleep = nap ? sleepAt(sleepP(frame, spanFrames(GRID, (napOwn ? scene : prev).at, nap.len), napOwn, total), nap.len, nap.breath !== false) : null;
  const light = sleep ? sleep.light : 1;
  const halo = sleep ? sleep.halo : 1;
  const mv = sleep && napOwn ? sleep.move : 0;
  // con `titleLead` la frase della scena dopo aspetta la notifica: la frase di chi dorme resta fino al taglio ed esce con la
  // sua uscita normale (corto, 23/09: il titolo apre il film in ambient). Senza, com'era nel film lungo
  const titleOn = sleep ? titleOnAt(sleep.title, napOwn, nap?.titleLead) : 1;
  const drift = sleep ? 1 - sleep.still : 1;
  // nella chiusura delle due finestre la frase cammina verso la colonna dei titoli, dove la scena dopo metterà la sua
  const lift = scene.text?.carry ? interpolate(frame - spanFrames(GRID, scene.at, scene.text.at ?? 0), [0, 14], [0, -THEME.title * 1.04], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: Easing.bezier(0.16, 1, 0.3, 1) }) : 0;
  const chiude = scene.split ? Math.min(1, Math.max(0, (0.5 - splitAt(frame / Math.max(1, total), scene.split.open, scene.split.hold, scene.split.close, total / beat).edge) / 0.5)) : 1;   // senza le due finestre la frase è già alla sua colonna   // la deriva si ferma nel sonno e torna piano dopo il risveglio
  const closing = scene.endCard ? closingAt(frame / beat, scene.strapBleed ? BLEED_LIFT : undefined) : null;
  // `exitBeats`: quanto dura il movimento d'uscita, quando deve accompagnare un tratto di musica invece di essere un
  // gesto breve — lo zoom della complication dura quanto il crescendo (Franz, 19/09 15:25: «zoom = crescendo»)
  // `enterAt`: l'orologio entra a scena iniziata — qui rientra mentre la frase cammina verso sinistra (Franz, 20/09 20:28)
  const eAt = w?.enterAt ? spanFrames(GRID, scene.at, w.enterAt) : 0;
  const pose = closing ? closing.pose : w ? poseAt(frame - eAt, total - eAt, beat * (w.exitBeats ?? MOVE_BEATS), w.enter, w.exit, frame + beatToFrame(GRID, scene.at), w.steady, drift, beat * (w.exitHold ?? 0)) : null;
  const cx = (watchColumn(scene) + (watchColumn(next) - watchColumn(scene)) * mv) * width;
  const textAt = spanFrames(GRID, scene.at, scene.text?.at ?? 0);
  // il titolo lascia il posto anche alle righe del terminale, che arrivano nella sua stessa colonna (Franz, 20/09 16:36)
  // se l'orologio esce (di lato o ingrandendosi) attraversa la colonna del testo: il testo se ne va prima;
  // e se una card esce dal display (piano 3) prende lei il centro sinistro: il titolo le lascia il posto un attimo prima che si stacchi
  const hero = (scene.fx ?? []).find((f) => f.kind === "cardOut" || f.kind === "gaugeHero" || f.kind === "optionsBuild" || f.kind === "terminalPlane");
  // quanto prima del taglio il titolo comincia ad andarsene. Con un'uscita lunga se ne va a metà del movimento; con una
  // rapida (sotto i due battiti) deve essere GIÀ fuori quando il movimento parte, se no la sua dissolvenza si mescola
  // allo zoom (Franz, 21/09 12:26: «That's fine» si sovrapponeva allo zoom dell'orologio)
  const exitLead = (moveF: number, b: number) => (moveF < 2 * b ? moveF + 8 : moveF * 0.55);
  // i dati della Panoramica stanno dove sta il titolo: il titolo se ne va prima che entri il primo (Franz, 18/09 21:42)
  const firstAside = (scene.fx ?? []).find((f) => f.kind === "aside");
  // con il battito di ciglia il titolo non se ne va: la sua parola in colore cresce e copre tutto (Blink, a livello del film)
  const leave0 = scene.out === "blink" ? total - BLINK_FRAMES : scene.text?.place === "top" ? total + 1000 : Math.min((w?.exit ? total - exitLead(beat * (w.exitBeats ?? MOVE_BEATS), beat) : total) - 8, hero ? spanFrames(GRID, scene.at, hero.at) - 6 : total + 1000, firstAside ? spanFrames(GRID, scene.at, firstAside.at) - 10 : total + 1000);
  // con le sole palpebre la frase è già uscita quando cominciano a chiudersi (revisione del 23/09)
  const leave = scene.out === "lids" && scene.text?.place !== "top" ? beforeLids(leave0, total) : leave0;
  // mentre la card è protagonista ci si avvicina all'orologio (come nel Canvas di Google a 31,5 s: il componente davanti, l'interfaccia
  // enorme, scura e sfocata dietro): il display cresce, si sfoca e si scurisce, e torna a fuoco al rientro
  const { zoom: zoom0, focus, watch: watchIn } = cameraAt(scene, GRID, frame);
  // `fadeOut`: l'orologio se ne va in dissolvenza PRIMA del taglio, sfalsato rispetto a quello che resta in quadro
  // (il terminale): sparendo insieme sembrava uno stacco, non un passaggio (Franz, 20/09 20:09)
  // `screenFade`: sfuma lo schermo, non l'orologio (piano 6): nel corto il terminale entra nello STESSO orologio
  const screenCover = w?.screenFade ? screenFadeAt(frame, total, spanFrames(GRID, scene.at, w.screenFade)) : 0;
  const fadeOut = w?.fadeOut ? 1 - Math.min(1, Math.max(0, (frame - (total - spanFrames(GRID, scene.at, w.fadeOut))) / spanFrames(GRID, scene.at, w.fadeOut))) : 1;
  // mentre il display dorme la camera torna anche alla misura della scena dopo: spostamento e scala si esauriscono al buio
  // il dolly della scena (23/09): la camera si avvicina o arretra; senza dolly vale 1 e la scena resta com'era
  const zoom = (zoom0 + ((next?.watch?.camera === "around" ? AROUND_ZOOM : 1) - zoom0) * mv) * dollyAt(w?.dolly, frame / Math.max(1, total));
  // quando l'orologio si materializza ATTORNO alla scheda ferma al centro, non è l'orologio a essere centrato: è la sua
  // scheda. Si sposta l'orologio di quanto la scheda dista dal centro del display, alla scala di quel momento.
  // dopo un battito di ciglia la scena che si riapre non ha la sua scheda (è un'altra schermata): tiene lo scarto di quella
  // di prima, se no l'orologio salta in verticale attraverso le palpebre (Franz, 21/09 04:23: «posizione identica»).
  const aroundCard = (() => {
    if (scene.watch?.camera !== "around") return undefined;
    for (let k = idx; k >= 0; k--) {
      const card = (TIMELINE.scenes[k].fx ?? []).find((f) => f.kind === "cardOut");
      if (card) return card;
      if (TIMELINE.scenes[k - 1]?.out !== "blink") return undefined;   // si risale solo attraverso le palpebre
    }
    return undefined;
  })();
  const aroundDy = aroundCard && aroundCard.kind === "cardOut"
    // `(1 - mv)`: quando la camera lascia la scheda e va nella colonna di destra, questo scarto si annulla con il
    // movimento. Senza, l'orologio restava 94 px più in basso per tutto il sonno e al risveglio saltava su (Franz, 22:20).
    ? (240 - (aroundCard.rect[1] + aroundCard.rect[3] / 2)) * (((THEME.frontGlassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR) / 480) * zoom * (1 - mv)
    : 0;
  // quando un momento forte prende il quadro (il tasto che diventa sfondo) il titolo se ne va: sul chiaro non si leggerebbe
  const over = heroState(scene, GRID, frame).exit;
  // mentre il takeover cresce, il componente sotto sparisce: il takeover È quel componente, non una copia sopra
  const takeStart = scene.takeover ? total - Math.round(spanFrames(GRID, scene.at, scene.takeover.len) * TAKEOVER_CUT) : Infinity;
  // il componente sotto sparisce quando il takeover lo ha COPERTO (metà della crescita), non appena parte: altrimenti fra
  // i tasti che se ne vanno e il campo che arriva si vede lo sfondo della scena (Franz, 19/09 11:03: «c'è un buco»)
  const flipStart = scene.flip ? total - Math.round(spanFrames(GRID, scene.at, scene.flip.len) * FLIP_CUT) : Infinity;
  const underFlip = frame >= flipStart ? 1 : 0;
  const underTakeover = scene.takeover && frame >= takeStart
    ? Math.min(1, Math.max(0, (takeoverAt((frame - takeStart) / Math.max(1, spanFrames(GRID, scene.at, scene.takeover.len))).grow - 0.3) / 0.25))
    : 0;
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
  // dove sta l'orologio: lo stesso conto per il suo transform e per il volo del terminale (piano 7)
  const place = pose ? watchPlace(cx, pose, width, height, shake, aroundDy, zoom) : null;
  // il volo del terminale (piani 4, 6, 7): dietro l'orologio, dentro lo schermo e davanti alla cornice, tutti dallo stesso piazzamento
  const tiFx = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "takeIn" }> => f.kind === "takeIn");
  const takeIn = tiFx && place && w?.view === "front" ? { e: tiFx, prev, g: GRID, f: frame - spanFrames(GRID, scene.at, tiFx.at), frames: spanFrames(GRID, scene.at + tiFx.at, tiFx.len),
    dx: place.x, dy: place.y, u: displayUnit(THEME.frontGlassPx, geo.front.glassR, geo.front.displayR, place.scale), width, height } : null;
  // posizione e scala dell'orologio in un solo transform 3D con will-change: così Chrome tiene la deriva lenta a sottopixel invece
  // di arrotondare left/top a pixel interi (misurato il 18/09: 40k pixel di differenza ogni tre fotogrammi, uno scatto visibile)
  const extraOut = interpolate(frame, [leave, leave + (scene.out === "blink" ? 2 : 8)], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill>
      <Backdrop act={scene.act} colors={actColors(scene.act, TIMELINE.palette)} light={halo} haloR={sleep ? sleep.haloR : 1} field={sleep ? sleep.field : 1} glowX={scene.text ? THEME.watchX : 0.5} from={scene.bgFrom} keep={scene.bgKeep} shadeIn={w?.enter ? beat * MOVE_BEATS : 0} fade={scene.bgFadeBeats ? spanFrames(GRID, scene.at, scene.bgFadeBeats) : undefined} />
      {scene.split ? <Split left={scene.split.left} right={scene.split.right} open={scene.split.open} hold={scene.split.hold} close={scene.split.close} total={total} beat={beat} /> : null}
      {scene.bands ? <Bands left={scene.bands.left} right={scene.bands.right} openFrames={scene.bands.open} winFrames={scene.bands.win} total={total} /> : null}
      <TerminalBackdrop scene={scene} g={GRID} />
      {/* il volo del terminale: la finestra DIETRO l'orologio, la sua copia dentro lo schermo (piano 6) */}
      {takeIn ? <TakeInFrame {...takeIn} /> : null}
      {w && pose && w.view === "side" ? (<>
        <div style={{ position: "absolute", width: 0, height: 0, left: 0, top: 0, transformOrigin: "0 0", willChange: "transform", transform: `translate3d(${width / 2 + pose.x * width}px, ${height * 0.70 + pose.y * height}px, 0) scale(${pose.scale})` }}>
          <SideWatch widthPx={THEME.sideCasePx} />
        </div>
        <div style={{ opacity: 1 - underTakeover }}><Floating scene={scene} g={GRID} glassY={height * 0.70} /></div>
        </>
      ) : w && pose && w.view !== "side" ? (
        <div style={{ position: "absolute", width: 0, height: 0, left: 0, top: 0, transformOrigin: "0 0", willChange: "transform", transform: `translate3d(${place!.x}px, ${place!.y}px, 0) scale(${place!.scale})`, opacity: watchIn * (1 - solo) * fadeOut * (frame >= eAt ? 1 : 0), filter: focus > 0 ? `blur(${8 * focus}px) brightness(${1 - 0.55 * focus})` : undefined }}>
          <PhotoWatch view={w.view} light={light} rim={sleep ? sleep.rim : 0} clip={w.clip} clipStart={w.clipStart} rate={w.rate} freeze={w.freeze} hold={w.hold ? spanFrames(GRID, scene.at, w.hold) : undefined} still={w.still} reveal={closing?.tilt} bodyOpacity={closing?.body} contentOpacity={closing?.logo} screenOpacity={closing && scene.logoCutout ? screenAt(closing, true) : undefined} bleed={scene.strapBleed} focus={closing?.focus} tilt={pose.tilt} overlay={closing ? <LogoMark draw={closing.draw} track={logoTrack(scene.endTone)} /> : screenCover > 0 ? <><div style={{ position: "absolute", inset: 0, background: scene.bgFrom ?? actColors(scene.act, TIMELINE.palette)[1], opacity: screenCover }} />{overlay}</> : takeIn ? <>{overlay}<TakeInScreen {...takeIn} /></> : overlay} around={around}
            glassPx={w.view === "threeQuarter" ? THEME.q34GlassPx : THEME.frontGlassPx} />
        </div>
      ) : null}
      {w?.view !== "side" && (scene.fx ?? []).some((f) => f.kind === "float") ? <div style={{ opacity: 1 - underTakeover }}><Floating scene={scene} g={GRID} /></div> : null}
      <Aside scene={scene} g={GRID} />
      {takeIn ? <TakeInFront {...takeIn} /> : null}
      {/* quando l'orologio si materializza attorno, la scheda ricostruita gli lascia il posto: dentro il display c'è la
          stessa scheda, nello stesso punto, e due copie sovrapposte si vedrebbero */}
      <div style={{ position: "absolute", inset: 0, opacity: (1 - underTakeover) * (1 - underFlip) * (scene.watch?.camera === "around" ? 1 - Math.max(0, (watchIn - 0.75) / 0.25) : 1) }}><Heroes scene={scene} g={GRID} watchCx={cx} pose={w?.view === "front" && pose ? { ...pose, scale: pose.scale * zoom } : null} glassPx={THEME.frontGlassPx} /></div>
      {w?.exit === "diveIn" ? <AbsoluteFill style={{ background: "#000", opacity: interpolate(frame, [total - beat * MOVE_BEATS * 0.55, total - 2], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }) }} /> : null}
      {scene.endCard ? <Sequence from={beat * END_PACE[scene.endPace ?? "normal"].start} layout="none"><EndCard beat={beat} pace={scene.endPace} tone={scene.endTone} /></Sequence> : null}
      {scene.text && !prev?.sleep ? (
        <Sequence from={textAt} layout="none">
          {/* con i due terminali la frase sta in ALTO e al centro: sotto ci sono le schede dei due account (Franz, 19:06) */}
          <div style={{ position: "absolute", opacity: (1 - Math.min(1, over * 2.5)) * (1 - solo) * titleOn, left: w ? THEME.leftMargin : 0, right: w ? undefined : 0, top: scene.split ? height * (0.66 - 0.266 * chiude) : scene.text.carry ? height * 0.394 : scene.text.place === "top" ? 110 : 0, bottom: 0, display: "flex", flexDirection: "column", alignItems: w ? "flex-start" : "center", justifyContent: scene.text.place === "top" || scene.split || scene.text.carry ? "flex-start" : "center" }}>
            {/* con l'orologio in scena il blocco è ancorato alla colonna dei titoli: la frase PARTE spostata al centro e
                torna al suo posto mentre le finestre si chiudono, arrivando dove la scena dopo mette il suo titolo */}
            {/* la frase arriva alla colonna prima che le finestre finiscano di chiudersi (Franz, 21:31): due terzi della corsa,
                con frenata morbida; e sale di una riga quando la terza riga entra, così il blocco resta dov'era */}
            <span style={{ display: "block", translate: `${(width / 2 - THEME.leftMargin - 340) * (1 - Easing.bezier(0.2, 0, 0, 1)(Math.min(1, chiude / 0.66)))}px ${lift}px` }}>
            <WordMask lines={scene.text.lines} accent={scene.text.accent} size={scene.text.place === "top" ? "service" : scene.text.size} sub={scene.text.sub} hideAccentFrom={scene.out === "blink" ? leave - textAt : undefined}   /* dentro la sequenza del testo i fotogrammi ripartono da zero */
              fadeFrom={scene.out === "blink" ? total - 30 - textAt : undefined}
              pauseFrames={scene.text.pause ? Math.round(beat * scene.text.pause) : undefined} perWordFrames={Math.round(beat / 2)}   /* mezzo battito a parola anche sui cartelli: la frase si compone in metà tempo e poi resta ferma (Franz, 19/09 14:22) */ exitAt={scene.text.place === "top" || scene.text.keep ? undefined : leave - textAt - (scene.out === "blink" ? 8 : 0)}   /* col blink le altre parole sono GIÀ uscite quando parte la card: se ne vanno negli 8 fotogrammi prima (Franz, 21/09 14:00) */ carry={scene.text.carry} align={w ? "left" : "center"} />
            </span>
            <div style={{ marginTop: 40, opacity: extraOut }}>{watchTextFor(scene, GRID, textAt)}</div>
          </div>
        </Sequence>
      ) : null}
    </AbsoluteFill>
  );
};

/** I battiti in cui cambia l'atto (senza apertura e chiusura, che hanno la loro cornice): lì passa la frustata. Non dove il
 *  passaggio lo racconta già l'invio della dettatura: la fascia passava proprio sullo scatto del ✓ (Franz, 21/09 20:16). */
export const actChanges = (scenes: Scene[]): number[] =>
  scenes.filter((s, i) => i > 0 && s.act !== scenes[i - 1].act && s.act !== "close" && scenes[i - 1].act !== "open" && scenes[i - 1].takeover?.body !== "screen").map((s) => s.at);

/** Una chiave nel display diventa una chiave nel quadro: il display frontale sta al centro dell'orologio (`watchX` se c'è testo),
 *  con 480 unità = 2·displayR·glassPx/(2·glassR) pixel, a riposo (posa senza movimenti né deriva: sui tagli l'orologio è fermo). */
const toFrame = (k: Key & { space?: "display" | "frame" }, scene: Scene): Key => {
  if (k.space === "frame") return k;
  const u = ((THEME.frontGlassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR) / 480;
  const cx = (scene.text ? THEME.watchX : 0.5) * 1920;
  return { ...k, x: cx + (k.x - 240) * u, y: 540 + (k.y - 240) * u, w: k.w * u, h: k.h * u, stroke: (k.stroke ?? 4) * u };
};
const BLINK_FRAMES = 49;   // 3 battiti (era 78, 2,6 s): la frase se ne va solo quando parte la card, e la parola corre in sincrono con lei (Franz, 21/09 14:00: la card copriva la frase ancora in quadro)
const CARRY_FRAMES = 22;   // 0,73 s: il passaggio si deve vedere (14 erano un lampo)

export const Film: React.FC<{ stems?: Stems; timeline?: Timeline; blur?: boolean }> = ({ stems, timeline = FILM_TIMELINE, blur = true }) => {
  useFilmFonts();
  const TIMELINE = timeline;
  const GRID: Grid = gridOf(timeline);
  return (
    <FilmTimeline.Provider value={timeline}>
    <FilmBlur.Provider value={blur}>
    <AbsoluteFill style={{ background: "#000" }}>
      <Soundtrack t={TIMELINE} g={GRID} stems={stems ?? "nosfx"} />
      {TIMELINE.scenes.map((s, i) => (
        <Sequence key={s.id} name={s.id} from={beatToFrame(GRID, s.at)} durationInFrames={spanFrames(GRID, s.at, s.len)}>
          <MB on={s.blur}><SceneView scene={s} {...fxLayers(s, GRID, TIMELINE.scenes[i - 1])} /></MB>
        </Sequence>
      ))}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.takeover || !next) return null;
        const k = s.takeover, frames = spanFrames(GRID, s.at, k.len);
        const startF = beatToFrame(GRID, next.at) - Math.round(frames * TAKEOVER_CUT);
        // l'invio della dettatura si posa sul prompt del terminale della scena dopo
        const term = (next.fx ?? []).find((f) => f.kind === "terminalPlane");
        const body = k.body === "card" ? { kind: "card" as const, text: k.text ?? "" } : k.body === "words" ? { kind: "words" as const, words: k.words ?? [], card: k.card } : k.body === "screen" ? { kind: "screen" as const, lines: k.words ?? [] } : { kind: "plain" as const };
        return <Sequence key={`take-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * TAKEOVER_CUT)} durationInFrames={frames + 1} layout="none"><MB on={s.blur}><Takeover x={k.x} y={k.y} w={k.w} h={k.h} r={k.r} tilt={k.tilt} color={k.color} toColor={k.toColor} tint={k.tint} frames={frames} body={body}
          press={k.press !== undefined ? beatToFrame(GRID, s.at + k.press) - startF : undefined} beat={spanFrames(GRID, s.at, 1)} land={term && term.kind === "terminalPlane" ? term.prompt : undefined} /></MB></Sequence>;
      })}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.carryOut || !next?.carryIn) return null;
        // centrato sul taglio: l'oggetto lascia la scena negli ultimi 7 fotogrammi e arriva nei primi 7 della dopo
        return <Sequence key={`carry-${s.id}`} from={beatToFrame(GRID, next.at) - CARRY_FRAMES / 2} durationInFrames={CARRY_FRAMES + 1} layout="none"><Carry from={toFrame(s.carryOut, s)} to={toFrame(next.carryIn, next)} frames={CARRY_FRAMES} /></Sequence>;
      })}
      {/* la luce della notifica: cresce dal display, copre, e sotto il quadro pieno l'inquadratura cambia */}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.glow || !next) return null;
        const frames = spanFrames(GRID, s.at, s.glow.len);
        return (
          <Sequence key={`glow-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * GLOW_CUT)} durationInFrames={frames + 1} layout="none">
            <Glow frames={frames} cx={s.glow.cx} cy={s.glow.cy} color={s.glow.color} />
          </Sequence>
        );
      })}
      {/* la scheda che si volta: sul retro c'è la domanda della scena dopo */}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.flip || !next) return null;
        const frames = spanFrames(GRID, s.at, s.flip.len);
        return (
          <Sequence key={`flip-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * FLIP_CUT)} durationInFrames={frames + 1} layout="none">
            <Flip frames={frames} w={s.flip.w} card={s.flip.card} question={s.flip.question} />
          </Sequence>
        );
      })}
      {/* il piccolo nero che chiude una scena sul suo movimento, appena prima che il brano riprenda */}
      {TIMELINE.scenes.filter((s) => s.blackOutFrames).map((s) => (
        <Sequence key={`black-${s.id}`} from={beatToFrame(GRID, s.at + s.len) - (s.blackOutFrames ?? 0)} durationInFrames={s.blackOutFrames} layout="none">
          <AbsoluteFill style={{ background: "#000" }} />
        </Sequence>
      ))}
      {/* la tapparella: chiusa a BLIND_CUT dell'arco, e lì cade il taglio con la scena dopo (revisione 3D, momento 1) */}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.blinds || !next) return null;
        const frames = spanFrames(GRID, s.at, s.blinds.len);
        return <Sequence key={`blinds-${s.id}`} from={beatToFrame(GRID, next.at) - Math.round(frames * BLIND_CUT)} durationInFrames={frames + 1} layout="none"><Blinds frames={frames} bars={blindBars(((s.fx ?? []).find((e) => e.kind === "aside" && e.out === "bars") as { rows?: unknown[] } | undefined)?.rows?.length ?? 2)} /></Sequence>;
      })}
      {/* il battito di ciglia dura quanto la crescita della card, non 30 fotogrammi: parola e scheda crescono INSIEME
          (Franz, 19/09 05:12). La parola resta dov'è e si ingrandisce; le palpebre si chiudono negli ultimi 7. */}
      {/* «lids»: le sole palpebre anche con una frase in scena, che esce con la sua uscita normale prima che si chiudano
          (corto, 23/09: la carrellata passa da una schermata del polso all'altra) */}
      {TIMELINE.scenes.filter((s) => s.out === "blink" || s.out === "lids").map((s) => (
        <Sequence key={`blink-${s.id}`} from={beatToFrame(GRID, s.at + s.len) - BLINK_FRAMES} durationInFrames={BLINK_FRAMES + 12} layout="none"><Blink word={s.out === "blink" ? s.text?.accent ?? "" : ""} cut={BLINK_FRAMES} from={[387, 597]} to={[684, 140]} /></Sequence>
      ))}
      {/* il titolo della scena dopo si scrive MENTRE la camera si sposta, prima della notifica (Franz, 19/09 18:14): è un
          solo disegno che attraversa il taglio, se no al taglio la frase ripartirebbe da capo. Perciò la scena che si
          risveglia non disegna il suo testo: lo disegna qui. */}
      {TIMELINE.scenes.map((s, i) => {
        const next = TIMELINE.scenes[i + 1];
        if (!s.sleep || !next?.text) return null;
        const frames = spanFrames(GRID, s.at, s.sleep.len);
        const pre = beatToFrame(GRID, next.at) - beatToFrame(GRID, next.at - (s.sleep.titleLead ?? TITLE_LEAD));   // la frase si scrive `titleLead` battiti prima della notifica (TITLE_LEAD nel film lungo)
        const cut = beatToFrame(GRID, next.at);
        const nextFrames = spanFrames(GRID, next.at, next.len);
        const beat = spanFrames(GRID, next.at, 1);
        return (
          <Sequence key={`title-${s.id}`} from={cut - pre} durationInFrames={pre + nextFrames} layout="none">
            <div style={{ position: "absolute", left: THEME.leftMargin, top: 0, bottom: 0, display: "flex", flexDirection: "column", alignItems: "flex-start", justifyContent: "center" }}>
              <WordMask lines={next.text.lines} accent={next.text.accent} size={next.text.size} sub={next.text.sub} perWordFrames={Math.round(beat / 2)} exitAt={pre + nextFrames - 8} align="left" />
              {/* l'attesa non è vuota: sotto la frase la sessione sta scrivendo (ui/dots.ts) */}
              <AskDots pre={pre} frames={frames} len={s.sleep.len} />
            </div>
          </Sequence>
        );
      })}
      {actChanges(TIMELINE.scenes).map((b) => (
        <Sequence key={`whip-${b}`} from={beatToFrame(GRID, b) - 3} durationInFrames={8} layout="none"><Whip width={1920} height={1080} /></Sequence>
      ))}
    </AbsoluteFill>
    </FilmBlur.Provider>
    </FilmTimeline.Provider>
  );
};

/** Il corto: la stessa macchina del film lungo, con la sua scaletta, la sua musica e la sua tavolozza. */
export const ShortFilm: React.FC<{ stems?: Stems; blur?: boolean }> = ({ stems, blur }) => <Film stems={stems} timeline={SHORT_TIMELINE} blur={blur} />;
