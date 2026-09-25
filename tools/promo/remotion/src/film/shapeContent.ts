/** I tempi dei contenuti della forma (ShapeContent.tsx), come funzioni pure del battito: qui si provano senza Remotion. */
import { springEase } from "./spring.ts";
import { SWAP_OUT } from "./shape.ts";

/**
 * I tasti della risposta entrano con una molla ζ 0,7 (§8.4): un solo scavalco del 4,6 %, il «po' di bump» del 19/09 senza
 * l'oscillazione vietata. Il secondo un quarto di battito dopo il primo; il primo appena il contenuto di prima è uscito.
 */
export const OPTION_ZETA = 0.7, OPTION_STAGGER = 0.25, OPTION_ENTER = 1;
export const optionsEnterAt = (sinceKey: number, i: number): number =>
  springEase((sinceKey - SWAP_OUT - i * OPTION_STAGGER) / OPTION_ENTER, OPTION_ZETA);
