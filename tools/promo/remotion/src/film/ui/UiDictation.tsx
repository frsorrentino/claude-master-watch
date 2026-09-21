import React, { useEffect, useState } from "react";
import { continueRender, delayRender, staticFile } from "remotion";
import { dictationAt } from "./dictation.ts";
import type { Word } from "./dictation.ts";
import { UI } from "./UiTokens.ts";

/**
 * La dettatura sulla card (Franz, 21/09 18:55), nello spazio del display come `UiCard`: prima i tre tasti che Wear OS propone
 * per scrivere a una sessione («Message for payments-api»: emoji, microfono, tastiera, come nella registrazione `n_say`), il
 * dito sul microfono; poi la schermata di dettatura com'è sull'orologio (Franz, 19:22: niente titolo della sessione, i tasti di
 * Wear OS): fondo nero, le parole al centro mentre la voce le dice, sotto ↶ · microfono · tastiera, e il microfono che diventa
 * ✓ appena c'è testo; il dito su ✓. Da questa schermata parte il takeover verso il terminale della scena dopo (Franz, 19:33:
 * la card della sessione non serve più). Misure prese sul fotogramma di `n_say` (480×480) e riportate nella card, alta quanto
 * la card della sessione (213 unità) perché la corsia la misura così.
 */
const MIC = "M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z";
const MOOD = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z";
const KEYS = "M20 5H4c-1.1 0-1.99.9-1.99 2L2 17c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm-9 3h2v2h-2V8zm0 3h2v2h-2v-2zM8 8h2v2H8V8zm0 3h2v2H8v-2zm-1 2H5v-2h2v2zm0-3H5V8h2v2zm9 7H8v-2h8v2zm0-4h-2v-2h2v2zm0-3h-2V8h2v2zm3 3h-2v-2h2v2zm0-3h-2V8h2v2z";
const UNDO = "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z";
const CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
const HEIGHT = 213;                      // la card finita (tre righe), `cardUnits` del testo dettato

const useJson = <T,>(file: string, fallback: T): T => {
  const [data, setData] = useState<T>(fallback);
  const [handle] = useState(() => delayRender(`dettatura ${file}`));
  useEffect(() => { fetch(staticFile(`audio/${file}`)).then((r) => r.json()).then((d: T) => { setData(d); continueRender(handle); }); }, [file, handle]);
  return data;
};

const Finger: React.FC<{ on: number }> = ({ on }) => (on > 0.001 ? (
  <div style={{ position: "absolute", left: "50%", top: "50%", width: 80, height: 80, translate: "-50% -50%", borderRadius: "50%",
    background: "rgba(58,60,70,.38)", boxShadow: "0 0 0 2px rgba(255,255,255,.75)", opacity: Math.min(1, on * 3), scale: String(0.9 + 0.1 * Math.min(1, on * 3)) }} />
) : null);

const Pill: React.FC<{ d: string; pressed?: number; finger?: number }> = ({ d, pressed = 0, finger = 0 }) => (
  <div style={{ position: "relative", width: 116, height: 84, borderRadius: 42, display: "grid", placeItems: "center",
    background: `color-mix(in srgb, #9FB3D6 ${Math.round(45 * pressed)}%, ${UI.primary})` }}>
    <svg viewBox="0 0 24 24" width="42" height="42"><path d={d} fill={UI.onPrimary} /></svg>
    {/* il dito come nella risposta: il cerchio grigio del «mostra tocchi»; 80 unità, così su ✓ resta dentro la card */}
    <Finger on={finger} />
  </div>
);

export const UiDictation: React.FC<{
  w: number; name: string; words: string; envelope: string; t: number; tap: number; voice: number; confirm: number; beat: number; light?: number;
}> = ({ w, name, words: wordsFile, envelope, t, tap, voice, confirm, beat, light = 0 }) => {
  const words = useJson<Word[]>(wordsFile, []);
  const env = useJson<{ env: number[] }>(envelope, { env: [] }).env;
  const d = dictationAt(t, tap, voice, words, beat, confirm);
  const level = env[Math.round((t - voice) * 30)] ?? 0;
  const blink = Math.floor((t - tap) / 0.5) % 2 === 0 ? 1 : 0;
  const shadow = `inset 1px 1px 0 rgba(235,244,255,${0.16 * light}), ${16 * light}px ${24 * light}px ${44 * light}px ${-6 * light}px rgba(4,5,12,${0.62 * light})`;
  return (
    <div style={{ position: "relative", width: w, height: HEIGHT }}>
      {/* la schermata dell'orologio: nera come la vera, con i bordi della card */}
      <div style={{ position: "absolute", inset: 0, borderRadius: 42, background: "#000", overflow: "hidden", boxShadow: shadow, fontFamily: "Roboto", color: "#FFFFFF" }}>
        {d.chooser > 0.001 ? (
          <div style={{ position: "absolute", inset: 0, padding: "24px 24px 24.5px", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "space-between", opacity: d.chooser }}>
            <div style={{ fontSize: 30, lineHeight: "36px", textAlign: "center" }}>Message for<br />{name}</div>
            <div style={{ display: "flex", gap: 13 }}>
              <Pill d={MOOD} />
              <Pill d={MIC} pressed={d.press} finger={d.press} />
              <Pill d={KEYS} />
            </div>
          </div>
        ) : null}
        <div style={{ position: "absolute", inset: 0, opacity: d.listen }}>
          {/* le parole al centro, e il cursore dopo l'ultima (senza occupare posto) */}
          <div style={{ position: "absolute", left: 30, right: 30, top: 16, height: 110, display: "flex", alignItems: "center", justifyContent: "center", textAlign: "center", fontSize: 29, lineHeight: "36px", letterSpacing: -0.2 }}>
            <div>
              {words.slice(0, d.words).map((wd, i) => <React.Fragment key={i}>{i ? " " : ""}{wd.word}</React.Fragment>)}
              <span style={{ position: "relative" }}><span style={{ position: "absolute", left: 2, top: -1, width: 2.5, height: 34, background: "#FFFFFF", opacity: blink * (1 - d.pressOk) }} /></span>
            </div>
          </div>
          {/* ↶ e tastiera ai lati, un po' più in alto del tasto centrale, come sul vetro tondo */}
          <svg viewBox="0 0 24 24" width="34" height="34" style={{ position: "absolute", left: 213 - 128 - 17, top: 160 - 17 }}><path d={UNDO} fill="#FFFFFF" /></svg>
          <svg viewBox="0 0 24 24" width="36" height="36" style={{ position: "absolute", left: 213 + 128 - 18, top: 160 - 18 }}><path d={KEYS} fill="#FFFFFF" /></svg>
          <div style={{ position: "absolute", left: 213 - 32, top: 141, width: 64, height: 64 }}>
            <div style={{ position: "absolute", inset: 0, borderRadius: "50%", background: "#1E2021", display: "grid", placeItems: "center", opacity: 1 - d.ok,
              boxShadow: `0 0 0 ${7 * level * (1 - d.ok)}px rgba(245,245,235,.18)` }}>
              <svg viewBox="0 0 24 24" width="30" height="30"><path d={MIC} fill="#F5F5EB" /></svg>
            </div>
            <div style={{ position: "absolute", inset: 0, borderRadius: "50%", background: "#FAF5E9", display: "grid", placeItems: "center", opacity: d.ok }}>
              <svg viewBox="0 0 24 24" width="32" height="32"><path d={CHECK} fill="#31302D" /></svg>
            </div>
            <Finger on={d.pressOk} />
          </div>
        </div>
      </div>
    </div>
  );
};
