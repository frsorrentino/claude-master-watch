import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { CameraMotionBlur } from "@remotion/motion-blur";
import { frameToBeat } from "./beats.ts";
import type { Grid } from "./beats.ts";
import { SHAPE_SHUTTER, blurSamples, contentAt, keyRect, shapeAt, shapeSpeed } from "./shape.ts";
import type { Display, ShapeKey } from "./shape.ts";
import { cameraAt, cameraCss } from "./camera.ts";

/** Disegna il contenuto `id` alla misura della chiave che lo porta, non a quella corrente della forma. */
export type ShapeContent = (id: string, w: number, h: number) => React.ReactNode;
type Track = { keys: readonly ShapeKey[]; g: Grid; display: Display };

/** Lo strato che la camera sposta e scala: niente `will-change`, se no lo scalato resta una bitmap sgranata. */
const CameraLayer: React.FC<Track & { children: React.ReactNode }> = ({ keys, g, display, children }) => {
  const beat = frameToBeat(g, useCurrentFrame());
  return <AbsoluteFill style={{ transform: cameraCss(cameraAt(keys, beat, display)), transformOrigin: "0 0" }}>{children}</AbsoluteFill>;
};

/** La scatola: riempimento e bordo, la sola cosa col blur. Stato e camera si leggono QUI, dentro il blur, perché il blur
 *  la ridisegna ai sotto-fotogrammi spostando il frame: calcolati fuori, i campioni sarebbero tutti uguali. */
const Box: React.FC<Track> = ({ keys, g, display }) => {
  const s = shapeAt(keys, frameToBeat(g, useCurrentFrame()), display);
  return (
    <CameraLayer keys={keys} g={g} display={display}>
      <div style={{ position: "absolute", left: s.x, top: s.y, width: s.w, height: s.h, borderRadius: s.r, background: s.color }} />
    </CameraLayer>
  );
};

/** La forma unica sopra le scene: la scatola col blur, e sopra il contenuto senza blur, che non scala con la forma. */
export const Shape: React.FC<Track & { content?: ShapeContent }> = ({ keys, g, display, content }) => {
  const frame = useCurrentFrame();
  if (!keys.length) return null;
  const beat = frameToBeat(g, frame);
  const s = shapeAt(keys, beat, display);
  const c = content ? contentAt(keys, beat) : null;
  const [, , kw, kh] = c ? keyRect(c.key, display) : [0, 0, 0, 0];
  return (
    <AbsoluteFill>
      <CameraMotionBlur samples={blurSamples(shapeSpeed(keys, beat, display, g.bpm / 60 / g.fps))} shutterAngle={SHAPE_SHUTTER}>
        <Box keys={keys} g={g} display={display} />
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

/** Le scene dentro la stessa camera della forma: scena e forma si muovono insieme (§4). */
export const CameraFrame: React.FC<Track & { children: React.ReactNode }> = ({ keys, children, ...track }) =>
  keys.length ? <CameraLayer keys={keys} {...track}>{children}</CameraLayer> : <>{children}</>;
