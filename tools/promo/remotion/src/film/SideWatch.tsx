import React from "react";
import { Img, staticFile } from "remotion";
import geo from "./mockup.geometry.json";

/**
 * L'orologio visto di taglio (mockup `side`, dalla foto laterale del 18/09): display verso l'alto, in scorcio. Di taglio il display non si vede
 * (la cupola lo copre): l'orologio resta com'è nella foto; sopra
 * il vetro galleggia ciò che la scena mette in `above` (card e badge ricostruiti, in prospettiva coerente: piano quasi
 * orizzontale, base sull'ellisse del display). `widthPx`: larghezza della cassa nel quadro.
 */
export const SideWatch: React.FC<{ widthPx: number; above?: React.ReactNode }> = ({ widthPx, above }) => {
  const G = geo.side;
  const k = widthPx / (G.caseX1 - G.caseX0);
  return (
    <div style={{ width: G.width * k, height: G.height * k, translate: `${-G.displayCx * k}px ${-G.displayCy * k}px`, position: "relative",
      /* la foto finisce prima del quadro: il cinturino sfuma nel buio ai due lati, come il tre quarti */
      maskImage: "linear-gradient(90deg, rgba(0,0,0,0) 0%, #000 9%, #000 91%, rgba(0,0,0,0) 100%)" }}>
      <div style={{ width: G.width, height: G.height, position: "relative", transformOrigin: "0 0", scale: String(k) }}>
        {/* ombra a terra: la cassa poggia sul cinturino, la luce viene dall'alto */}
        <div style={{ position: "absolute", left: G.caseX0 - 60, top: G.bottom - 30, width: G.caseX1 - G.caseX0 + 120, height: 70, borderRadius: "50%", background: "rgba(0,0,0,.75)", filter: "blur(28px)" }} />
        <Img src={staticFile("mockup/side_body.png")} style={{ position: "absolute", inset: 0 }} />
        {above ? <div style={{ position: "absolute", left: G.displayCx, top: G.displayCy, width: 0, height: 0 }}>{above}</div> : null}
      </div>
    </div>
  );
};
