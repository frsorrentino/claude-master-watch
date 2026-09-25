import React from "react";
import { Sequence, useCurrentFrame } from "remotion";
import { beatToFrame, frameToBeat, spanFrames } from "./beats.ts";
import { gridOf } from "./cut.ts";
import type { Fx, Timeline } from "./timeline.ts";
import type { ShapeContent } from "./Shape.tsx";
import { SWAP_IN, SWAP_OUT, inkOn } from "./shape.ts";
import { laneCard, optionsEnterAt } from "./shapeContent.ts";
import { SpokenWords } from "./WatchText.tsx";
import { UiOption } from "./ui/UiOption.tsx";
import { UiWave } from "./ui/UiWave.tsx";
import { UiDictation } from "./ui/UiDictation.tsx";
import { SCREEN_ICONS } from "./ui/Takeover.tsx";
import { optionsBuildAt } from "./ui/heroes.ts";
import { sceneAt } from "./shapeDisplay.ts";
import { THEME } from "./theme.ts";
import { soft } from "./moves.ts";
import { UiCard } from "./ui/UiCard.tsx";
import { LIST_BODY, UI } from "./ui/UiTokens.ts";
import { BriefContext, BriefQuestions, BriefWork } from "./ui/Brief.tsx";

const clamp = (t: number) => Math.min(1, Math.max(0, t));
/** Il pannello si disegna dopo che il suo testo è entrato: barre e numeri nascono in tre quarti di battito, come `Aside`. */
const DRAW_BEATS = 0.75;
const PANEL_PAD = 30;   // pannello largo 820: dentro restano i 760 di Aside, e il bordo destro sta a 900, lontano dalla cassa
/** La notifica com'è sulla registrazione (n_speaks.mp4 a 12,5 s): la scaletta non la porta, la clip sì. */
const NOTICE = { name: "payments-api", age: "0 m", text: "Staging is green. Deploy 2.8.0?", badge: UI.badge };
/** Il tasto di UiOption col suo anello: l'anello sta 6 unità fuori dalla pillola, spesso 6 (9 in tutto per lato). */
const RING_OUT = 9;
type Options = Extract<Fx, { kind: "optionsBuild" }>;

/**
 * Cosa c'è dentro la forma, per id (la traccia `shape` del corto). I dati vengono dalle scene che una volta li
 * disegnavano da sole (la card ✓ della lista, i pannelli della Panoramica): una sola fonte, e la scena non li disegna più
 * dove c'è la forma. Il blocco è grande come la chiave che porta il contenuto (controscala, Shape.tsx): qui si impagina a
 * quella misura. Gli id dei tratti non ancora ribasati non hanno contenuto (passi 4-5).
 */
export const shapeContent = (t: Timeline): ShapeContent => (id, w, h) => <Content t={t} id={id} w={w} h={h} />;

const Content: React.FC<{ t: Timeline; id: string; w: number; h: number }> = ({ t, id, w, h }) => {
  const frame = useCurrentFrame();
  const beat = frameToBeat(gridOf(t), frame);
  const key = (t.shape ?? []).find((k) => k.content === id);
  if (!key) return null;
  const g = gridOf(t);
  const scene = sceneAt(t, key.at);
  const fx = scene.fx ?? [];
  const d = soft(clamp((beat - key.at - SWAP_OUT - SWAP_IN) / DRAW_BEATS));
  // i tempi dei componenti vecchi sono in fotogrammi della scena: si rifanno come li faceva chi li disegnava, così parole,
  // onda e anello cadono sugli stessi fotogrammi di prima
  const sceneStart = beatToFrame(g, scene.at);
  // le etichette dei tasti: stanno sulla domanda della scaletta (la scena della risposta), una sola in tutto il corto
  const question = t.scenes.flatMap((s) => s.fx ?? []).find((f): f is Options => f.kind === "optionsBuild");
  if (id === "notify") {
    // la notifica, come sulla registrazione: badge con «?», nome del progetto, età, la domanda; nell'inchiostro della forma
    const ink = inkOn(key.color), k = w / 720;
    return (
      <div style={{ position: "absolute", left: 0, top: 0, width: 720, height: h / k, transform: `scale(${k})`, transformOrigin: "0 0", boxSizing: "border-box", padding: "24px 32px", fontFamily: "Roboto", color: ink }}>
        <div style={{ display: "flex", alignItems: "center", gap: 16, height: 36 }}>
          <div style={{ width: 32, height: 32, borderRadius: 7.4, background: NOTICE.badge, display: "grid", placeItems: "center", color: "#FFFFFF", fontWeight: 500, fontSize: 24 }}>?</div>
          <div style={{ fontFamily: "Cousine", fontSize: 29, opacity: 0.7, flex: 1, whiteSpace: "nowrap" }}>{NOTICE.name}</div>
          <div style={{ fontSize: 33, opacity: 0.7, whiteSpace: "nowrap" }}>{NOTICE.age}</div>
        </div>
        <div style={{ marginTop: 16, fontSize: 42, lineHeight: "52px", letterSpacing: -0.6, whiteSpace: "nowrap" }}>{NOTICE.text}</div>
      </div>
    );
  }
  if (id === "voice") {
    // la domanda che parla: il ▶ toccato, le parole della voce mentre le dice, l'onda sotto, e in basso le due risposte
    const v = fx.find((f): f is Extract<Fx, { kind: "spoken" }> => f.kind === "spoken");
    if (!v) return null;
    const ink = inkOn(key.color), k = w / 720;
    const from = sceneStart + spanFrames(g, scene.at, v.at), frames = spanFrames(g, scene.at, v.at + v.len) - spanFrames(g, scene.at, v.at);
    return (
      <div style={{ position: "absolute", left: 0, top: 0, width: 720, height: h / k, transform: `scale(${k})`, transformOrigin: "0 0" }}>
        <div style={{ position: "absolute", left: 32, top: 32, width: 88, height: 88, borderRadius: "50%", background: ink, display: "grid", placeItems: "center" }}>
          <svg viewBox="0 0 24 24" width={44} height={44}><path d="M8.5 6v12l10-6z" fill={key.color} /></svg>
        </div>
        <Sequence from={from} layout="none">
          {/* `zoom` sul blocco interno: sullo stesso elemento scalerebbe anche left e top */}
          <div style={{ position: "absolute", left: 144, top: 32, width: 544 }}><div style={{ zoom: 0.85 }}><SpokenWords file={v.words} fps={g.fps} ink={ink} /></div></div>
          <div style={{ position: "absolute", left: 144, top: 214 }}><UiWave file={v.voice.replace(/\.wav$/, ".env.json")} width={544} height={80} frames={frames} /></div>
        </Sequence>
        {question ? (
          <div style={{ position: "absolute", left: 32, bottom: 32, display: "flex", gap: 16 }}>
            <div style={{ zoom: 0.55 }}><UiOption w={428} h={88} label={question.yesLabel} primary /></div>
            <div style={{ zoom: 0.55 }}><UiOption w={428} h={88} label={question.noLabel} /></div>
          </div>
        ) : null}
      </div>
    );
  }
  if (/^card\d+$/.test(id) || id === "dict") {
    // le schede della corsia (loop): la forma è la scheda che sale, la corsia tiene solo tempi e scritte (`inShape`)
    const lane = fx.find((f): f is Extract<Fx, { kind: "float" }> => f.kind === "float");
    const c = lane ? laneCard(lane.cards, id) : undefined;
    if (!c) return null;
    const voice = fx.find((f): f is Extract<Fx, { kind: "spoken" }> => f.kind === "spoken");
    // la dettatura sui tempi di Floating: secondi dall'inizio della scena, tocco sul microfono, voce, conferma
    const at = (b: number) => spanFrames(g, scene.at, b) / g.fps;
    return (
      <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div style={{ position: "relative", width: w }}>
          <div style={{ zoom: w / 428 }}>
            {c.dictation && voice ? (
              <UiDictation w={428} name={c.name} words={voice.words} envelope={voice.voice.replace(/\.wav$/, ".env.json")} t={(frame - sceneStart) / g.fps}
                tap={at(c.dictation.tap)} voice={at(voice.at)} confirm={c.dictation.confirm !== undefined ? at(c.dictation.confirm) : 1e9} beat={60 / g.bpm} />
            ) : (
              <UiCard w={428} name={c.name} age={c.age} text={c.text} badge={c.badge} icon={c.icon === "bell" ? "check" : c.icon} />
            )}
          </div>
          {/* la campanella: la sessione seguita, il puntino giallo in alto a destra, fuori dallo zoom come nella corsia */}
          {c.icon === "bell" && !c.dictation ? <div style={{ position: "absolute", right: 42, top: 34, width: 34, height: 34, borderRadius: "50%", background: UI.followed }} /> : null}
        </div>
      </div>
    );
  }
  if (id === "screen") {
    // la schermata dell'invio a tutto quadro, com'è a fine crescita del Takeover (corpo `screen`, u = 120/29): le righe
    // dettate, annulla e tastiera ai lati, il ✓ grande sotto. Posti di UiDictation, centro della schermata al centro
    const tk = scene.takeover;
    if (!tk?.words) return null;
    const u = 120 / 29;
    const place = (x0: number, y0: number, dd: number): React.CSSProperties => ({ position: "absolute", left: w / 2 + (x0 - 213.5) * u - (dd * u) / 2, top: h / 2 + (y0 - 106.5) * u - (dd * u) / 2, width: dd * u, height: dd * u });
    return (
      <>
        <div style={{ position: "absolute", left: w / 2, top: h / 2 + (71 - 106.5) * u, translate: "-50% -50%", whiteSpace: "nowrap", textAlign: "center", fontFamily: "Roboto", fontSize: 29 * u, lineHeight: `${36 * u}px`, letterSpacing: -0.2 * u, color: "#FFFFFF" }}>
          {tk.words.map((l, i) => <div key={i}>{l}</div>)}
        </div>
        <svg viewBox="0 0 24 24" style={place(85, 160, 34)}><path d={SCREEN_ICONS.undo} fill="#FFFFFF" /></svg>
        <svg viewBox="0 0 24 24" style={place(341, 160, 36)}><path d={SCREEN_ICONS.keyboard} fill="#FFFFFF" /></svg>
        <div style={{ ...place(213, 173, 64), borderRadius: "50%", background: "#FAF5E9", display: "grid", placeItems: "center" }}>
          <svg viewBox="0 0 24 24" width={32 * u} height={32 * u}><path d={SCREEN_ICONS.check} fill="#31302D" /></svg>
        </div>
      </>
    );
  }
  if (id === "check") {
    // il ✓ dell'invio: la forma è il suo cerchio, il segno nell'inchiostro che si legge sul colore della chiave
    return (
      <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center" }}>
        <svg viewBox="0 0 24 24" width={w / 2} height={h / 2}><path d={SCREEN_ICONS.check} fill={inkOn(key.color)} /></svg>
      </div>
    );
  }
  if (id === "options" || id === "yes") {
    const o = fx.find((f): f is Options => f.kind === "optionsBuild") ?? question;
    if (!o) return null;
    if (id === "yes") {
      // il solo «yes», a tutta scatola, con l'anello della pressione lunga: lo stesso conto di Heroes, sugli stessi fotogrammi
      const from = spanFrames(g, scene.at, o.at), len = spanFrames(g, scene.at + o.at, o.len);
      const ring = optionsBuildAt((frame - sceneStart - from) / len, o.pressAt !== undefined ? o.pressAt / o.len : undefined).ring;
      const k = Math.min(w / (o.yes[2] + 2 * RING_OUT), h / (o.yes[3] + 2 * RING_OUT));
      return (
        <div style={{ position: "absolute", left: RING_OUT * k, top: (h - o.yes[3] * k) / 2 }}>
          <div style={{ zoom: k }}><UiOption w={w / k - 2 * RING_OUT} h={o.yes[3]} label={o.yesLabel} primary ring={ring} /></div>
        </div>
      );
    }
    // i due tasti uno sotto l'altro, che entrano sfalsati con la molla ζ 0,7 (shapeContent.ts)
    const gap = 20, pad = 40;
    const k = Math.min((w - 2 * pad) / o.yes[2], (h - 2 * pad) / (o.yes[3] + gap + o.no[3]));
    const top0 = (h - (o.yes[3] + gap + o.no[3]) * k) / 2;
    return (
      <>
        {[{ r: o.yes, label: o.yesLabel, primary: true, y: top0 }, { r: o.no, label: o.noLabel, primary: false, y: top0 + (o.yes[3] + gap) * k }].map((b, i) => {
          const e = optionsEnterAt(beat - key.at, i);
          return (
            <div key={i} style={{ position: "absolute", left: (w - b.r[2] * k) / 2, top: b.y, opacity: clamp(e), transform: `translateY(${(1 - e) * 60}px)` }}>
              <div style={{ zoom: k }}><UiOption w={b.r[2]} h={b.r[3]} label={b.label} primary={b.primary} /></div>
            </div>
          );
        })}
      </>
    );
  }
  if (id === "done") {
    // la card vera della lista (UiCard nello spazio del display): la forma è il suo fondo, la card ci sta sopra identica
    const e = fx.find((f): f is Extract<Fx, { kind: "doneCard" }> => f.kind === "doneCard");
    if (!e) return null;
    return (
      <div style={{ width: 428, transform: `scale(${w / 428})`, transformOrigin: "0 0" }}>
        <UiCard w={428} name={e.name} age={e.age} text={e.text} badge={e.badge} icon="check" checkDraw={1} body={LIST_BODY} />
      </div>
    );
  }
  const a = fx.find((f): f is Extract<Fx, { kind: "aside" }> => f.kind === "aside" && f.panel === id);
  if (!a) return null;
  return (
    <div style={{ position: "absolute", left: PANEL_PAD, top: 0, width: w - 2 * PANEL_PAD, height: h, display: "flex", flexDirection: "column", justifyContent: "center", fontFamily: "Inter", color: THEME.white }}>
      {id === "work" ? <BriefWork n={a.n} note={a.note} bars={a.bars} d={d} /> : id === "questions" ? <BriefQuestions n={a.n} note={a.note} quote={a.quote} d={d} /> : <BriefContext rows={a.rows} d={d} />}
    </div>
  );
};
