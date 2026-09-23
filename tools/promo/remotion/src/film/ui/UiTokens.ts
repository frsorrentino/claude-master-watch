/** I colori dell'app (`CmColors` in wear/.../ui/theme/Theme.kt): i componenti ricostruiti nel film usano questi e solo questi. */
export const UI = {
  bg: "#000000", surfaceLow: "#1B1F26", surface: "#23272E", surfaceHigh: "#292F3A", line: "#2A2E35",
  text: "#F2F4F7", text2: "#B0B8C4", accent: "#4C7DFF", primary: "#D3E3FD", onPrimary: "#0A2050",
  waiting: "#FFB020", followed: "#FFE08A", busy: "#7FA1FF", idle: "#34C759", coral: "#D97757",
  badge: "#3C81F2",
  briefRing: "#8BB4F7", briefWeek: "#B9A6F5", briefTrack: "#455165",   // anelli della quota: 5 ore, settimana, binario
  track: "#3A404C",                                                       // il binario del segno (LogoMark)
  briefGood: "#65C581", briefLabel: "#BCE4C7",                             // verde del brief (intestazione del terminale)            // il blu dell'account nel badge della card (misurato sul fotogramma della lista)
};

/** Il corpo del testo di una card di sessione (UiCard), nello spazio del display: nella schermata della sessione (misurato il
 *  19/09: il corpo sale di 9,5 unità sotto l'intestazione) e nella lista delle sessioni, dove l'app lo scrive più piccolo e
 *  più stretto (misurato il 23/09 su n_list.mp4 a 11,6 s: linee di base a 66 e 108 unità dall'alto del badge). */
export type CardBody = { size: number; line: number; shift: number };
export const CARD_BODY: CardBody = { size: 36, line: 46, shift: -9.5 };
export const LIST_BODY: CardBody = { size: 34, line: 42, shift: -1.5 };   // con −0,5 le basi cadevano a 67 e 108,7: un'unità sotto
