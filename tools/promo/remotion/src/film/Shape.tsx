import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { CameraMotionBlur } from "@remotion/motion-blur";
import { frameToBeat } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { SHAPE_SHUTTER, arcOf, blurSamples, contentAt, keyRect, shapeAt, shapeVisible, shutterCentre } from "./shape.ts";
import type { Display, ShapeKey } from "./shape.ts";
import { cameraAt, cameraCss, screenSpeed } from "./camera.ts";

/** Disegna il contenuto `id` alla misura della chiave che lo porta, non a quella corrente della forma. */
export type ShapeContent = (id: string, w: number, h: number) => React.ReactNode;
type Track = { keys: readonly ShapeKey[]; g: Grid; display: Display };
/** Da che battito a che battito la forma è accesa (`to` escluso; senza, fino alla fine). Fuori non c'è né forma né camera. */
export type ShapeSpan = { from: number; to?: number };
const inSpan = (span: ShapeSpan | undefined, beat: number) => !span || (beat >= span.from && (span.to === undefined || beat < span.to));

/** Lo strato che la camera sposta e scala: niente `will-change`, se no lo scalato resta una bitmap sgranata. `shift`:
 *  fotogrammi da togliere a quello corrente (la scatola dentro il blur); `span`: fuori, lo strato non trasforma. */
const CameraLayer: React.FC<Track & { shift?: number; span?: ShapeSpan; children: React.ReactNode }> = ({ keys, g, display, shift = 0, span, children }) => {
  const beat = frameToBeat(g, useCurrentFrame() - shift);
  return <AbsoluteFill style={inSpan(span, beat) ? { transform: cameraCss(cameraAt(keys, beat, display)), transformOrigin: "0 0" } : undefined}>{children}</AbsoluteFill>;
};

/** La scatola: riempimento e bordo, la sola cosa col blur. Stato e camera si leggono QUI, dentro il blur, perché il blur
 *  la ridisegna ai sotto-fotogrammi spostando il frame: calcolati fuori, i campioni sarebbero tutti uguali. Arretrati di
 *  `shutterCentre(n)`, perché i campioni cadano attorno al fotogramma e non davanti (shape.ts). */
const Box: React.FC<Track & { samples: number }> = ({ keys, g, display, samples }) => {
  const shift = shutterCentre(samples);
  const s = shapeAt(keys, frameToBeat(g, useCurrentFrame() - shift), display);
  const a = arcOf(s);
  // il tratto piegato si disegna come l'anello di LogoMark: un cerchio col solo tratto dell'arco (dasharray), girato
  // sul capo sinistro; sotto tutto l'arco nel colore del resto, sopra la parte `split` nel colore
  const ring = (len: number, color: string) => a ? (
    <circle cx={a.cx} cy={a.cy} r={a.R} fill="none" stroke={color} strokeWidth={a.stroke} strokeLinecap="round"
      strokeDasharray={`${len} ${2 * Math.PI * a.R}`} transform={`rotate(${a.start} ${a.cx} ${a.cy})`} />
  ) : null;
  return (
    <CameraLayer keys={keys} g={g} display={display} shift={shift}>
      {a ? (
        <svg width={1920} height={1080} style={{ position: "absolute", left: 0, top: 0, overflow: "visible" }}>
          {ring((a.R * a.sweep * Math.PI) / 180, s.track)}
          {s.split > 1e-3 ? ring((a.R * a.sweep * Math.PI * s.split) / 180, s.color) : null}
        </svg>
      ) : (
        <div style={{ position: "absolute", left: s.x, top: s.y, width: s.w, height: s.h, borderRadius: s.r, background: s.color }} />
      )}
    </CameraLayer>
  );
};

/** La forma unica sopra le scene: la scatola col blur, e sopra il contenuto senza blur, che non scala con la forma. */
export const Shape: React.FC<Track & { content?: ShapeContent; span?: ShapeSpan }> = ({ keys, g, display, content, span }) => {
  const frame = useCurrentFrame();
  const beat = frameToBeat(g, frame);
  if (!keys.length || !inSpan(span, beat) || !shapeVisible(keys, beat)) return null;
  const s = shapeAt(keys, beat, display);
  const c = content ? contentAt(keys, beat) : null;
  const [, , kw, kh] = c ? keyRect(c.key, display) : [0, 0, 0, 0];
  const samples = blurSamples(screenSpeed(keys, beat, display, g.bpm / 60 / g.fps));
  return (
    <AbsoluteFill>
      <CameraMotionBlur samples={samples} shutterAngle={SHAPE_SHUTTER}>
        <Box keys={keys} g={g} display={display} samples={samples} />
      </CameraMotionBlur>
      {c && content ? (
        <CameraLayer keys={keys} g={g} display={display}>
          <div style={{ position: "absolute", left: s.x, top: s.y, width: s.w, height: s.h, borderRadius: s.r, overflow: "hidden" }}>
            {/* la controscala: il blocco resta grande come la sua chiave e centrato, così l'a capo non si muove mentre la forma cambia */}
            <div style={{ position: "absolute", left: (s.w - kw) / 2, top: (s.h - kh) / 2, width: kw, height: kh, opacity: c.opacity, filter: c.blur > 0 ? `blur(${c.blur}px)` : undefined, transform: `translateY(${c.dy}px)` }}>
              {content(c.id, kw, kh)}
            </div>
          </div>
        </CameraLayer>
      ) : null}
    </AbsoluteFill>
  );
};

/** Le scene dentro la stessa camera della forma: scena e forma si muovono insieme (§4). Fuori da `span` lo strato resta
 *  ma non trasforma: cambiare l'albero sul battito di `from` rimonterebbe tutte le scene (e i loro video). */
export const CameraFrame: React.FC<Track & { span?: ShapeSpan; children: React.ReactNode }> = ({ keys, children, ...track }) =>
  keys.length ? <CameraLayer keys={keys} {...track}>{children}</CameraLayer> : <>{children}</>;
