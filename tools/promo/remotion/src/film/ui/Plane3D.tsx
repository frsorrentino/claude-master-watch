import React from "react";

/** Un piano inclinato nello spazio: prospettiva sul contenitore, rotazioni sul figlio. Senza 3D vero: la profondità è simulata. */
export const Plane3D: React.FC<{ rx?: number; ry?: number; rz?: number; perspective?: number; children: React.ReactNode }> = ({ rx = 0, ry = 0, rz = 0, perspective = 1400, children }) => (
  <div style={{ perspective: `${perspective}px` }}>
    <div style={{ transform: `rotateX(${rx}deg) rotateY(${ry}deg) rotateZ(${rz}deg)`, transformStyle: "preserve-3d" }}>{children}</div>
  </div>
);
