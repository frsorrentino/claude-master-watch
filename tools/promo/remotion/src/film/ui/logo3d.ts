/**
 * La chiusura (revisione 3D, momento 3): il segno dell'app come oggetto vero, non come disegno.
 * Nasce **di taglio** — si vede solo il suo spessore, una linea verticale — gira mostrandosi di faccia, e mentre gira
 * l'arco del quadrante si disegna. Alla fine resta fermo, appena inclinato, e il nome compare sotto (in HTML, non in 3D).
 *  - `yaw`    radianti: 90° all'inizio (di taglio), 0 alla fine (di faccia), con un filo di inclinazione residua;
 *  - `draw`   0-1: quanto dell'arco corallo è disegnato (arriva al 70 % del quadrante, come il segno piatto);
 *  - `glow`   0-1: la luce che passa sul segno nel momento in cui si presenta di faccia;
 *  - `name`   0-1: il nome sotto, che entra quando il segno si è fermato.
 */
const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ease = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);
const soft = (t: number) => 1 - Math.pow(1 - t, 3);

export type LogoSpin = { yaw: number; draw: number; glow: number; name: number };

/** L'inclinazione residua: il segno non resta perfettamente frontale, tiene 4° così si legge come oggetto. */
export const LOGO_REST = (4 * Math.PI) / 180;

export const logoSpinAt = (p: number): LogoSpin => {
  const turn = ease(clamp((p - 0.12) / 0.46));
  return {
    yaw: (Math.PI / 2) * (1 - turn) + LOGO_REST * turn,
    draw: soft(clamp((p - 0.3) / 0.42)),
    glow: Math.sin(Math.PI * clamp((p - 0.34) / 0.26)),
    name: soft(clamp((p - 0.66) / 0.2)),
  };
};
