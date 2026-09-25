import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { spanFrames } from "../beats.ts";
import type { Grid } from "../beats.ts";
import type { Fx, Scene } from "../timeline.ts";
import { laneSteps, laneY, railScrollVar } from "./heroes.ts";
import { UiCard } from "./UiCard.tsx";
import { UiDictation } from "./UiDictation.tsx";
import { UiBriefContext, UiBriefQuestions, UiBriefWork } from "./UiBrief.tsx";
import { UI } from "./UiTokens.ts";
import { TAKEOVER_CUT, takeoverAt } from "./takeover.ts";
import { INK, THEME, mix } from "../theme.ts";

/**
 * La corsia sopra l'orologio appoggiato a terra: una LISTA che scorre, come sul polso. Le schede stanno a passo costante
 * (`PITCH`), la corsia comincia SOPRA l'orologio senza toccarlo (`GAP`), la lista avanza di un passo alla volta e si sofferma
 * su ogni scheda (`railScroll`), e ogni scheda si deforma con la quota come nelle liste di Wear OS (`railAt`): stretta in
 * basso, larga al centro, stretta in cima. Le scritte scorrono nella stessa corsia, alternate alle schede, ma non si deformano.
 */
export const Floating: React.FC<{ scene: Scene; g: Grid; glassY?: number }> = ({ scene, g, glassY }) => {
  const frame = useCurrentFrame();
  // Quando la scena nasce sotto il campo chiaro del takeover, le scritte partono in inchiostro e diventano bianche mentre
  // il fondo scurisce: bianco su celeste non si legge (misurato il 20/09: contrasto nullo per un secondo e mezzo).
  const fadeF = scene.bgFrom ? spanFrames(g, scene.at, scene.bgFadeBeats ?? 2) : 0;
  const lit = scene.bgKeep ? 1 : scene.bgFrom ? 1 - (() => { const t = Math.min(1, frame / Math.max(1, fadeF)); return t * t * (3 - 2 * t); })() : 0;
  const inkText = mix(THEME.white, INK.text, lit);
  const inkAccent = mix(THEME.accent, INK.accent, lit);
  const { width, height, fps } = useVideoConfig();
  // la voce della scena, se c'è: la card che detta ne mostra le parole (Franz, 21/09 18:55)
  const voice = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "spoken" }> => f.kind === "spoken");
  const e = (scene.fx ?? []).find((f): f is Extract<Fx, { kind: "float" }> => f.kind === "float");
  if (!e) return null;
  const from = spanFrames(g, scene.at, e.at), len = spanFrames(g, scene.at + e.at, e.len);
  const p = (frame - from) / len;
  if (p < 0) return null;
  // dove sta la corsia: nella scena laterale sopra l'orologio, altrove dove dice la scaletta (colonna e fondo in frazioni di quadro)
  const W = (e.width ?? 560), cx = e.cx !== undefined ? e.cx * width : width / 2;
  const base = glassY ?? (e.bottom ?? 0.92) * height;
  const CLEAR = 150;                            // la corsia comincia sopra l'orologio, senza toccarlo
  const U = W / 427;                            // un'unità di display in pixel di quadro
  const GAP = 9 * U;                            // lo stacco vero fra due schede sul polso: nove unità, quasi a sfioro
  // ogni scheda è alta quanto il suo testo, come sul display: con un'altezza sola per tutte, quelle corte lasciavano un buco
  // le frasi occupano quanto una scheda immaginaria: 60 px d'aria sopra e sotto il blocco di testo (Franz, 20/09 16:00).
  // Con soli 10 px stavano appiccicate alle schede vicine e lo scorrimento sembrava irregolare.
  // altezze, soste e pesi dei passi: laneSteps (ui/heroes.ts), gli stessi conti che usano i test per i tempi delle card
  const { heights, holds, weights } = laneSteps(e.cards, W);
  const yBottom = base - (glassY ? CLEAR : 0);
  // la sosta la decide la scaletta, scheda per scheda: le card lunghe si leggono, le scritte passano più svelte
  // l'ultima scheda si ferma al centro e resta lì: da quella posizione parte l'ingrandimento della transizione
  // ogni passo pesa quanto è lungo: mezza voce di qui, mezza di là, più lo stacco — così la corsia scorre a velocità costante
  // sosta su un passo INTERO: la scheda in evidenza si ferma sempre con il bordo basso sullo stesso punto. Con la sosta a
  // 0,8 di passo il resto dipendeva dall'altezza della scheda dopo, e il punto si spostava ogni volta (Franz, 20/09 15:48).
  // ...e non oltre l'ultima: la corsia finisce con l'ultima scheda IN EVIDENZA, che è quella da cui parte l'ingrandimento
  const offset = Math.min(e.cards.length - 1, railScrollVar(p, holds, 1, weights));
  const flat = e.cards.map((c) => c.kind === "text");
  const lane = laneY(offset, heights, GAP, yBottom, flat);   // quote, scale e trasparenze di questo fotogramma
  // quando l'ultima scheda comincia a crescere (il takeover parte da lei), le scritte della corsia se ne vanno: la scheda
  // che si allarga passerebbe sopra la frase (Franz, 20/09 11:14). Le schede restano: quella che cresce È la stessa.
  const tk = scene.takeover;
  const tkFrames = tk ? spanFrames(g, scene.at, tk.len) : 1;
  const tkStart = tk ? spanFrames(g, scene.at, scene.len) - Math.round(tkFrames * TAKEOVER_CUT) : Infinity;
  const tkGrow = frame >= tkStart ? takeoverAt((frame - tkStart) / Math.max(1, tkFrames)).grow : 0;
  const wordsOut = Math.min(1, Math.max(0, tkGrow / 0.04));   // la frase se ne va in un paio di fotogrammi, prima che la scheda la superi
  // anche le schede se ne vanno appena parte il takeover: quella che cresce È la stessa, e due copie si vedevano sfalsate
  const laneOut = Math.min(1, Math.max(0, tkGrow / 0.03));   // sparisce in un paio di fotogrammi: due copie sfalsate si vedono
  return (
    <>
      {e.cards.map((c, i) => {
        const d = offset - i;                    // 0 = appena entrata in fondo alla corsia, SPAN = in cima
        const y = lane.y[i] ?? -9999;
        // la scheda non sparisce ai capi: resta piccola e appena trasparente finché non esce davvero dal quadro
        if (d <= -0.06 || lane.y[i] === null || y < -240) return null;
        const rail = { scale: lane.scale[i], alpha: lane.alpha[i] };
        // la scheda la disegna la forma unica (card1, card2, dict): la corsia tiene i suoi tempi e le sue scritte
        if (c.kind !== "text" && c.kind !== "brief" && c.inShape) return null;
        const isText = c.kind === "text";
        const s = isText ? 1 : rail.scale;                       // il testo non si deforma: sale liscio
        const a = rail.alpha * (isText ? 1 - wordsOut : 1) * (1 - laneOut);
        if (c.kind === "text") return (
          <div key={i} style={{ position: "absolute", left: cx, top: y, width: 0, height: 0, opacity: a, zIndex: 10 + i }}>
            <div style={{ translate: "-50% -50%", width: W + 220, textAlign: "center", scale: String(s), fontFamily: "Inter", fontWeight: 600, fontSize: 82, lineHeight: 1.08, letterSpacing: "-0.02em", color: inkText }}>
              {c.lines.map((l, j) => <div key={j}>{l.split(" ").map((wd, n) => <span key={n} style={{ color: wd === c.accent ? inkAccent : undefined }}>{wd}{n < l.split(" ").length - 1 ? " " : ""}</span>)}</div>)}
            </div>
          </div>
        );
        return (
          <div key={i} style={{ position: "absolute", left: cx, top: y, width: 0, height: 0, opacity: a, zIndex: 10 + i }}>
            {/* ombra sul vetro solo per la card più bassa: è quella appoggiata alla luce del display */}

            <div style={{ translate: "-50% -50%", width: W, scale: String(s), perspective: 1600 }}>
              <div style={{ transform: "rotateX(10deg)", transformOrigin: "50% 100%", position: "relative" }}>
                <div style={{ zoom: W / 427 }}>
                  {c.kind === "brief" ? (
                    c.panel === "work" ? <UiBriefWork w={427} n={c.n ?? 1} note={c.note ?? ""} bars={c.bars ?? [1, 1, 1]} />
                    : c.panel === "questions" ? <UiBriefQuestions w={427} n={c.n ?? 1} note={c.note ?? ""} />
                    : <UiBriefContext w={427} rows={c.rows ?? []} />
                  ) : c.dictation && voice ? (
                    <UiDictation w={427} name={c.name} words={voice.words} envelope={voice.voice.replace(/\.wav$/, ".env.json")}
                      t={frame / fps} tap={spanFrames(g, scene.at, c.dictation.tap) / fps} voice={spanFrames(g, scene.at, voice.at) / fps}
                      confirm={c.dictation.confirm !== undefined ? spanFrames(g, scene.at, c.dictation.confirm) / fps : 1e9} beat={60 / g.bpm} light={1} />
                  ) : (
                    <UiCard w={427} name={c.name} age={c.age} text={c.text} badge={c.badge} icon={c.icon === "bell" ? "check" : c.icon} light={1} />
                  )}
                </div>
                {c.kind !== "brief" && c.icon === "bell" ? <div style={{ position: "absolute", right: 42, top: 34, width: 34, height: 34, borderRadius: "50%", background: UI.followed }} /> : null}
              </div>
            </div>
          </div>
        );
      })}
    </>
  );
};
