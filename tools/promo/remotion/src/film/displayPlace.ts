/**
 * Dove sta l'orologio della scena, e il suo display nel quadro, come funzioni pure del fotogramma. Era il blocco di conti di
 * `SceneView`: spostato qui così com'è perché la forma unica si aggancia al display VERO (deriva, dolly, tremito, zoom
 * della scena), e una seconda copia di quei conti prima o poi divergerebbe. La scena lo chiama per disegnarsi, la forma
 * per agganciarsi: un solo conto.
 */
import geo from "./mockup.geometry.json" with { type: "json" };
import { beatToFrame, spanFrames } from "./beats.ts";
import { gridOf } from "./cut.ts";
import { dollyAt } from "./dolly.ts";
import { applyH, homography } from "./homography.ts";
import type { Quad } from "./homography.ts";
import { BLEED_LIFT, MOVE_BEATS, closingAt, displayUnit, poseAt, watchPlace } from "./moves.ts";
import type { Closing, Pose } from "./moves.ts";
import type { Rect } from "./shape.ts";
import { THEME } from "./theme.ts";
import { watchColumn } from "./timeline.ts";
import type { Scene, Timeline } from "./timeline.ts";
import { AROUND_ZOOM, sceneCameraAt } from "./ui/heroes.ts";
import { sleepAt, sleepP } from "./ui/sleep.ts";

/** Centro del vetro nel quadro e scala dell'orologio. */
export type Place = { x: number; y: number; scale: number };
export type WatchState = {
  place: Place | null; pose: Pose | null; closing: Closing | null; zoom: number; focus: number; watchIn: number; mv: number;
  sleep: ReturnType<typeof sleepAt> | null; aroundDy: number; shake: number; cx: number;
  eAt: number;   // il fotogramma della scena in cui l'orologio entra (`enterAt`): prima non c'è
};

/** Lo stato dell'orologio nel fotogramma `frame` della scena (relativo al suo inizio). */
export const watchStateAt = (t: Timeline, scene: Scene, frame: number, W: number, H: number): WatchState => {
  const GRID = gridOf(t);
  const total = spanFrames(GRID, scene.at, scene.len);
  const beat = spanFrames(GRID, scene.at, 1);
  const w = scene.watch;
  // il display che dorme (ui/sleep.ts): la finestra finisce sul taglio, e la scena dopo eredita il risveglio. La scena che
  // dorme è anche quella che si sposta: al buio la camera passa all'inquadratura della scena dopo.
  const idx = t.scenes.indexOf(scene);
  const prev = t.scenes[idx - 1];
  const next = t.scenes[idx + 1];
  const nap = scene.sleep ?? prev?.sleep;
  const napOwn = Boolean(scene.sleep);
  const sleep = nap ? sleepAt(sleepP(frame, spanFrames(GRID, (napOwn ? scene : prev).at, nap.len), napOwn, total), nap.len, nap.breath !== false) : null;
  const mv = sleep && napOwn ? sleep.move : 0;
  const drift = sleep ? 1 - sleep.still : 1;   // la deriva si ferma nel sonno e torna piano dopo il risveglio
  const closing = scene.endCard ? closingAt(frame / beat, scene.strapBleed ? BLEED_LIFT : undefined) : null;
  // `exitBeats`: quanto dura il movimento d'uscita, quando deve accompagnare un tratto di musica invece di essere un
  // gesto breve — lo zoom della complication dura quanto il crescendo (Franz, 19/09 15:25: «zoom = crescendo»)
  // `enterAt`: l'orologio entra a scena iniziata — qui rientra mentre la frase cammina verso sinistra (Franz, 20/09 20:28)
  const eAt = w?.enterAt ? spanFrames(GRID, scene.at, w.enterAt) : 0;
  const pose = closing ? closing.pose : w ? poseAt(frame - eAt, total - eAt, beat * (w.exitBeats ?? MOVE_BEATS), w.enter, w.exit, frame + beatToFrame(GRID, scene.at), w.steady, drift, beat * (w.exitHold ?? 0)) : null;
  const cx = (watchColumn(scene) + (watchColumn(next) - watchColumn(scene)) * mv) * W;
  // mentre la card è protagonista ci si avvicina all'orologio (come nel Canvas di Google a 31,5 s: il componente davanti, l'interfaccia
  // enorme, scura e sfocata dietro): il display cresce, si sfoca e si scurisce, e torna a fuoco al rientro
  const { zoom: zoom0, focus, watch: watchIn } = sceneCameraAt(scene, GRID, frame);
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
      const card = (t.scenes[k].fx ?? []).find((f) => f.kind === "cardOut");
      if (card) return card;
      if (t.scenes[k - 1]?.out !== "blink") return undefined;   // si risale solo attraverso le palpebre
    }
    return undefined;
  })();
  const aroundDy = aroundCard && aroundCard.kind === "cardOut"
    // `(1 - mv)`: quando la camera lascia la scheda e va nella colonna di destra, questo scarto si annulla con il
    // movimento. Senza, l'orologio restava 94 px più in basso per tutto il sonno e al risveglio saltava su (Franz, 22:20).
    ? (240 - (aroundCard.rect[1] + aroundCard.rect[3] / 2)) * (((THEME.frontGlassPx / (2 * geo.front.glassR)) * 2 * geo.front.displayR) / 480) * zoom * (1 - mv)
    : 0;
  // vibrazione: la notifica arriva e l'orologio trema per 10 fotogrammi (Franz, 13:13)
  const shakeAt = (scene.fx ?? []).find((f) => f.kind === "shake");
  const sh = shakeAt ? frame - spanFrames(GRID, scene.at, shakeAt.at) : -1;
  const shake = sh >= 0 && sh < 10 ? 4 * (1 - sh / 10) * Math.sin(sh * 2.6) : 0;
  // dove sta l'orologio: lo stesso conto per il suo transform e per il volo del terminale (piano 7)
  const place = pose ? watchPlace(cx, pose, W, H, shake, aroundDy, zoom) : null;
  return { place, pose, closing, zoom, focus, watchIn, mv, sleep, aroundDy, shake, cx, eAt };
};

/**
 * Il quadrato 0-480 del display nel quadro, al fotogramma assoluto `frame` (anche fra due fotogrammi: la deriva è
 * continua); `null` senza orologio o con la vista laterale. Vale a inclinazione 0 della posa (la prospettiva di
 * `PhotoWatch` è l'identità) e, di tre quarti, col display piatto: con `closing.tilt` > 0 è il piatto, un'approssimazione,
 * e la forma lì non deve essere agganciata.
 */
export const displayRectAt = (t: Timeline, frame: number, W = 1920, H = 1080): Rect | null => {
  const g = gridOf(t);
  // prima del film vale la prima scena, dopo l'ultima: la forma c'è anche fuori
  const scene = t.scenes.find((s) => frame < beatToFrame(g, s.at + s.len)) ?? t.scenes[t.scenes.length - 1];
  const view = scene.watch?.view;
  if (view !== "front" && view !== "threeQuarter") return null;
  const st = watchStateAt(t, scene, frame - beatToFrame(g, scene.at), W, H);
  if (!st.place) return null;
  const { x, y, scale } = st.place;
  if (view === "front") {
    const u = displayUnit(THEME.frontGlassPx, geo.front.glassR, geo.front.displayR, scale);
    return [x - 240 * u, y - 240 * u, 480 * u, 480 * u];
  }
  // la matrice `flat` di `ThreeQuarter`: un quadrato dritto centrato dove cade il centro del display, lato medio del
  // quadrilatero della foto; poi la scala del vetro `k`, l'origine che `focus` sposta dal vetro al display, il piazzamento
  const Q = geo.q34, q = Q.quad as Quad;
  const k = THEME.q34GlassPx / (2 * Q.b);
  const [qx, qy] = applyH(homography(480, q), [240, 240]);
  const side = (Math.hypot(q[1][0] - q[0][0], q[1][1] - q[0][1]) + Math.hypot(q[2][0] - q[3][0], q[2][1] - q[3][1]) + Math.hypot(q[3][0] - q[0][0], q[3][1] - q[0][1]) + Math.hypot(q[2][0] - q[1][0], q[2][1] - q[1][1])) / 4;
  const focus = st.closing?.focus ?? 0;
  const ox = Q.cx + (qx - Q.cx) * focus, oy = Q.cy + (qy - Q.cy) * focus;
  const s = scale * k;
  return [x + (qx - side / 2 - ox) * s, y + (qy - side / 2 - oy) * s, side * s, side * s];
};
