import { useEffect, useState } from "react";
import { continueRender, delayRender, staticFile } from "remotion";

const FACES: [string, string, string][] = [
  ["Inter", "fonts/Inter-SemiBold.ttf", "600"],
  ["Inter", "fonts/Inter-Medium.ttf", "500"],
  ["Noto Sans Mono", "fonts/NotoSansMono-Regular.ttf", "400"],
  ["Roboto", "fonts/Roboto-Regular.ttf", "400"],           // il carattere dell'interfaccia dell'orologio, per i componenti ricostruiti
  ["Roboto", "fonts/Roboto-Medium.ttf", "500"],
  ["Cousine", "fonts/Cousine-Regular.ttf", "400"],       // il monospazio dell'orologio (Droid Sans Mono) ha Cousine come gemello
];

/** Il render aspetta i caratteri: senza, i primi fotogrammi escono con il carattere di ripiego. */
export const useFilmFonts = (): void => {
  const [handle] = useState(() => delayRender("caratteri"));
  useEffect(() => {
    Promise.all(FACES.map(([family, file, weight]) => new FontFace(family, `url(${staticFile(file)})`, { weight }).load().then((f) => document.fonts.add(f))))
      .then(() => continueRender(handle));
  }, [handle]);
};
