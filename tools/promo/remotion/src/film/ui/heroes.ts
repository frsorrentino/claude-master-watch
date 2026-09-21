/** Coreografie dei momenti forti (piano 3): funzioni pure dell'avanzamento 0-1, come `moves.ts`. */
import { bezier, bump, soft } from "../moves.ts";

/** Strappo: un oggetto che si stacca prende velocità per un attimo e poi frena a lungo (la curva `soft` parte troppo secca: un
 *  quarto della strada nei primi tre fotogrammi, e senza sfocatura di movimento sembra un taglio). */
const pull = bezier(0.35, 0, 0.15, 1);
/** Lo stesso strappo, ma l'arrivo scavalca di poco il posto e ci si assesta: dà peso alla scheda che si ferma al centro. */
const pullBump = bump(0.05);

const clamp = (t: number) => Math.min(1, Math.max(0, t));
const ramp = (v: number, a: number, b: number) => clamp((v - a) / (b - a));

/**
 * Il volo comune ai componenti che lasciano il display e ci tornano: strappo e atterraggio morbido nel posto da protagonista
 * (0-`outEnd`), sosta con deriva lenta, rientro morbido sul proprio rettangolo (`backFrom`-`backTo`), dissolvenza sull'originale.
 * - `travel` 0-1: dov'è tra il display (0) e il posto da protagonista (1); a 0 combacia con l'originale sul fotogramma.
 * - `swing` 0-1: quanto è girato attorno all'asse verticale: solo in volo, all'arrivo è quasi frontale.
 * - `drift` −1…1: respiro lento mentre è fuori (posizione e inclinazione di pochi pixel e gradi).
 * - `patch` 0-1: quanto è coperto l'originale sul display (un fantasma della superficie che resta al suo posto); il fantasma
 *   sparisce prima del componente (0,95-0,97), così sotto la dissolvenza c'è già l'originale.
 * - `alpha` 0-1: visibilità del componente ricostruito: a 0 è già disegnato, combaciante (il confronto di fedeltà si fa lì);
 *   si dissolve solo dopo essere atterrato.
 */
export type Flight = { travel: number; swing: number; drift: number; patch: number; alpha: number; exit: number };
/**
 * Franz, 18/09 13:13: il componente NON torna mai sul display. Esce (0-`outEnd`, `outEnd` 0 = è già fuori dal primo
 * fotogramma, dopo un battito di ciglia), resta protagonista, e da `exitFrom` si consegna alla scena dopo (`exit` 0-1: si
 * ritira verso il posto del titolo prossimo rimpicciolendo, e lì diventa l'elemento grafico che quella scena riprende).
 */
export const flightAt = (p: number, outEnd: number, exitFrom: number): Flight => {
  const out = outEnd <= 0 ? (p >= 0 ? 1 : 0) : (exitFrom >= 1 ? pullBump : pull)(ramp(p, 0, outEnd));
  // `exitFrom` oltre la fine = il componente NON se ne va: resta protagonista fino al taglio (la sosta di `cardOutAt`).
  // Senza questo caso `ramp(p, 2, 1)` ha gli estremi invertiti e restituisce 1: la card spariva invece di restare.
  const exit = exitFrom >= 1 ? 0 : soft(ramp(p, exitFrom, 1));
  return {
    travel: out,
    swing: outEnd <= 0 ? 0 : Math.sin(Math.PI * out),
    drift: Math.sin(2 * Math.PI * ramp(p, outEnd * 0.7, exitFrom + 0.1)),
    patch: ramp(p, 0, 0.03) * (1 - exit),
    alpha: p < 0 || p >= 1 ? 0 : 1 - exit,
    exit,
  };
};

/** La card lascia il display (0-0,42), resta fuori, rientra (0,62-0,95) e si dissolve sulla card vera (0,95-1). Su 3,5
 *  battiti (57 fotogrammi) uscita e rientro durano 800 e 630 ms. */
export type CardOut = Flight;
/** `fromOut`: la card è già fuori (dopo il battito di ciglia) e l'orologio si materializza attorno. */
export const cardOutAt = (p: number, fromOut = false, stay = false): CardOut => flightAt(p, fromOut ? 0 : 0.42, stay ? 1 : 0.86);

/**
 * Il gauge della quota lascia la card (0-0,3) svuotandosi mentre vola, fuori si disegna da zero al suo valore (0,34-0,62,
 * molla lenta come nell'app) con il numero che conta, resta, e rientra pieno (0,72-0,95) com'era sul display.
 * `value` 0-1: frazione del valore vero mostrata dagli archi e dal contatore; agli estremi è 1, cioè il display.
 * `label` 0-1: numero e frase accanto al gauge, solo mentre è fermo fuori (compaiono con il disegno, se ne vanno prima del rientro).
 */
export type GaugeHero = Flight & { value: number; label: number };
export const gaugeHeroAt = (p: number): GaugeHero => {
  const f = flightAt(p, 0.3, 0.84);
  const value = p < 0.32 ? 1 - pull(ramp(p, 0, 0.3)) : soft(ramp(p, 0.34, 0.62));
  const label = ramp(p, 0.33, 0.38) * (1 - ramp(p, 0.8, 0.84));
  return { ...f, value, label };
};

/**
 * I tasti della domanda nascono come contorno fuori dal display (0-0,2), si riempiono (0,2-0,3: «1 · yes» pieno, «2 · no»
 * scuro), l'anello corallo della pressione lunga corre attorno a «1 · yes» in tempo con la pressione vera (0,3-0,68), poi
 * i tasti restano un attimo e si dissolvono (0,86-1) mentre sul display arriva «Sent». Non lasciano il display: sono l'eco.
 */
export type OptionsBuild = { build: number; ring: number; alpha: number; travel: number; pop: number; fill: number; press: number };
/** Dopo la pressione (`ring` 1) il tasto si gonfia un attimo (`pop`) e poi cresce fino a diventare lo sfondo (`fill`): la
 *  transizione alla scena dopo è il tasto stesso (Franz, 13:13). */
/**
 * `pressFrom` 0-1: dove, nell'arco dei tasti, comincia la pressione lunga. Di suo a 0,34; si sposta quando la pressione
 * deve cadere dopo che la voce ha finito di leggere le due risposte (Franz, 20/09 08:57). Tutte le fasi che seguono —
 * anello, scatto, riempimento — si spostano con lei, così la coreografia resta la stessa, solo più in là.
 */
export const optionsBuildAt = (p: number, pressFrom = 0.34): OptionsBuild => ({
  // i tasti ESCONO dall'orologio già fatti e si posano al centro con un filo di rimbalzo (Franz, 19/09 09:33), poi la
  // pressione lunga corre e la selezione si chiude: tutto più svelto di prima (l'anello durava un quinto della scena)
  // pieni dal primo fotogramma: con il contorno che si riempiva, al decollo si vedevano due coppie di tasti sovrapposte,
  // quella vera ferma sul display e quella che partiva (Franz, 21/09 18:00: «un movimento dei tasti nell'orologio»)
  build: p < 0 ? 0 : 1,
  ring: ramp(p, pressFrom, pressFrom + 0.12),
  pop: Math.sin(Math.PI * ramp(p, pressFrom + 0.12, pressFrom + 0.19)),
  // il tasto si schiaccia sotto il dito per tutta la corsa dell'anello e scatta quando si chiude (Franz, 19/09 11:03)
  press: Math.min(1, ramp(p, pressFrom - 0.04, pressFrom + 0.02) * 1.2) * (1 - ramp(p, pressFrom + 0.12, pressFrom + 0.16)),
  fill: soft(ramp(p, pressFrom + 0.19, pressFrom + 0.26)),
  alpha: p < 0 || p >= 1 ? 0 : 1,
  travel: p < 0 || p >= 1 ? 0 : bump(0.05)(ramp(p, 0, 0.22)),
});

/** Il terminale lascia il display (0-0,25), resta fuori come finestra del PC mentre le righe arrivano, rientra (0,78-0,95).
 *  `morph` segue il volo: com'è sul display quando è a casa, finestra del PC quando è fuori. */
/** Il terminale del PC compare in dissolvenza a tutto sfondo dietro l'orologio (Franz, 13:13: niente uscita e rientro): `show`
 *  0-1 in 0-0,2, resta, e negli ultimi 15 % si ritira verso la scena dopo. */
export type TerminalPlane = { show: number; exit: number };
export const terminalPlaneAt = (p: number): TerminalPlane => ({ show: p < 0 ? 0 : soft(ramp(p, 0, 0.2)), exit: soft(ramp(p, 0.85, 1)) });

/**
 * Un pannello della Panoramica che esce e si anima (Franz, 18/09 20:06: «animazioni dei suoi elementi come hai fatto col
 * gauge»): esce dal display (0-0,3), fuori il suo contenuto si disegna da zero — barre che si riempiono, percentuali che
 * contano (0,34-0,66) — resta, e si consegna alla scena dopo (0,84-1).
 */
export type PanelHero = Flight & { draw: number };
export const panelHeroAt = (p: number): PanelHero => {
  const f = flightAt(p, 0.3, 0.84);
  return { ...f, draw: p < 0.32 ? 0 : soft(ramp(p, 0.34, 0.66)) };
};

/**
 * Lo scorrimento della corsia (vista laterale): una LISTA vera, non elementi indipendenti (Franz, 18/09 19:03). Le schede
 * stanno a passo costante e la lista scorre di un passo alla volta, **soffermandosi su ogni scheda**: dentro ogni passo il
 * primo tratto è movimento (curva morbida), il resto è sosta. `steps` = tempo in passi; il risultato è l'offset della lista.
 */
export const railScroll = (steps: number, dwell = 0.58): number => {
  const k = Math.floor(steps), f = steps - k;
  const m = clamp(f / (1 - dwell));
  return k + m * m * (3 - 2 * m);
};

/**
 * Lo stesso scorrimento ma con **sosta diversa per scheda** (Franz, 18/09 19:21: «poteva soffermarsi su una card un po' di più
 * per farla leggere»): `holds[i]` è quanto la lista resta ferma sulla scheda `i`, nella stessa unità del movimento (`MOVE`).
 * `p` 0-1 è il tempo della scena; il risultato è l'offset della lista.
 */
/** Il passo della lista: parte piano, accelera e frena a lungo (più «easing» di uno smoothstep, Franz 18/09 19:25). */
const railEase = bezier(0.45, 0, 0.22, 1);
export const RAIL_MOVE = 0.66;          // il movimento occupa più tempo della sosta: scorre, non scatta
export const railScrollVar = (p: number, holds: number[], center = 0.8, weights: number[] = []): number => {
  // `weights[i]`: quanto è LUNGO in pixel il passo i rispetto alla media. Con le altezze vere un passo vale il doppio di
  // un altro: a tempo uguale la lista strisciava sulle scritte e correva sulle schede (Franz, 20/09 14:40). Dando a ogni
  // passo un tempo proporzionale alla sua lunghezza, la velocità in pixel resta la stessa.
  const w = (i: number) => (weights.length ? (weights[i] ?? 1) : 1);
  const dur = holds.map((h, i) => RAIL_MOVE * w(i) + Math.max(0, h));
  const total = dur.reduce((a, b) => a + b, 0);
  let t = clamp(p) * total, i = 0;
  while (i < dur.length && t >= dur[i]) { t -= dur[i]; i++; }
  // `center`: la sosta cade quando la scheda è al centro della corsia, non sul passo intero
  if (i >= dur.length) return dur.length - (1 - center);
  return i + railEase(clamp(t / (RAIL_MOVE * w(i)))) - (1 - center);
};

/**
 * La deformazione delle liste di Wear OS (`SurfaceTransformation`, Franz 18/09 18:40): una scheda entra piccola in basso, è
 * più larga al centro e torna piccola salendo; la scala dipende dalla QUOTA, non dal tempo. `t` 0-1 dal bordo basso della
 * corsia (0, dove la scheda nasce) alla cima (1, dove esce). Il testo non si deforma: sale liscio.
 * Le schede ai capi NON spariscono (Franz, 18/09 19:19): restano piccole e appena trasparenti, come nella lista vera.
 */
export const railAt = (t: number): { scale: number; alpha: number } => {
  const bell = Math.sin(Math.PI * clamp(t));
  return { scale: 0.62 + 0.38 * bell, alpha: 0.45 + 0.55 * Math.min(1, 1.5 * bell) };
};

/**
 * L'altezza di una scheda della lista, in unità di display, dal suo testo: riempimento 24 sopra e 24,5 sotto, intestazione
 * 36, e il corpo che va a capo ogni `perLine` caratteri (23, misurati sul fotogramma della card vera) con interlinea 46 (il corpo sale di 9,5 sotto l'intestazione).
 * Misurata sulla card vera: 3 righe = 213 unità, come il rettangolo preso sul fotogramma. Serve alla corsia laterale, dove
 * le schede devono stare vicine come sul polso: con un'altezza fissa per tutte, quelle a due righe lasciavano mezzo buco
 * (Franz, 20/09 10:38: «quasi a sfioro»).
 */
export const cardUnits = (text: string, perLine = 23): number => {
  let n = 1, len = 0;
  for (const w of text.split(" ")) {
    if (len === 0) { len = w.length; continue; }
    if (len + 1 + w.length <= perLine) len += 1 + w.length; else { n++; len = w.length; }
  }
  return 24 + 36 + (n * 46 - 9.5) + 24.5;
};


/**
 * Le quote delle schede della corsia, in pixel, per il fotogramma corrente: una catena dal basso verso l'alto in cui fra i
 * bordi di due schede vicine resta SEMPRE `gap`, qualunque sia la scala con cui sono disegnate in quell'istante. È il modo
 * esatto: calcolando il passo sugli indici (e non sulla posizione vera) le schede si deformavano scorrendo e finivano una
 * sull'altra (Franz, 20/09 11:39).
 * - `offset`: avanzamento della lista in passi (0 = la prima scheda appoggiata in fondo alla corsia);
 * - `heights[i]`: altezza della scheda i alla scala piena; `flat[i]`: non si deforma (le scritte);
 * - il risultato è il CENTRO di ogni scheda, dall'alto del quadro; `null` per quelle non ancora entrate.
 */
/**
 * La deformazione della corsia sopra l'orologio (Franz, 20/09 15:24): chi arriva sta in primo piano, pieno e opaco; quella
 * prima scivola sopra, **più piccola e più trasparente**; quella ancora prima **sparisce**. In quadro non ce ne sono mai
 * più di due. `d` = quanti passi sono passati da quando la voce era davanti (0 = è lei).
 */
export const stackAt = (d: number): { scale: number; alpha: number } => {
  const x = Math.max(0, Math.min(2, d));
  // la voce sopra resta visibile ma staccata (Franz, 20/09 16:23): 80 % dopo un passo, e si spegne solo
  // nell'ultimo quinto, appena prima di uscire.
  return { scale: 1 - 0.26 * (x / 2), alpha: x <= 1.6 ? 1 - 0.2 * x : Math.max(0, (0.68 * (2 - x)) / 0.4) };
};

/**
 * Le quote delle voci della corsia, in pixel, per il fotogramma corrente: una catena dal basso verso l'alto in cui fra i
 * bordi di due voci vicine resta SEMPRE `gap`. La scala viene da `stackAt`, cioè da quanti passi sono passati, non dalla
 * quota: così la voce davanti è sempre piena e quella sopra sempre più piccola, qualunque sia l'altezza delle schede.
 * - `offset`: avanzamento della lista in passi (0 = la prima voce appoggiata in fondo alla corsia);
 * - `heights[i]`: altezza della voce i alla scala piena; `flat[i]`: non si deforma (le scritte restano a grandezza piena);
 * - il risultato dà centro, scala e trasparenza di ognuna; `y` è `null` per quelle fuori quadro.
 */
export const laneY = (offset: number, heights: number[], gap: number, yBottom: number, flat: boolean[] = []): { y: (number | null)[]; scale: number[]; alpha: number[] } => {
  const n = heights.length;
  const iB = Math.max(0, Math.min(n - 1, Math.floor(offset)));      // la voce appoggiata in fondo
  const f = Math.max(0, Math.min(1, offset - iB));                   // quanto è già salita verso la prossima
  const st = heights.map((_, i) => stackAt(offset - i));
  const sc = st.map((x, i) => (flat[i] ? 1 : x.scale));
  const hOf = (i: number) => heights[Math.max(0, Math.min(n - 1, i))] * sc[Math.max(0, Math.min(n - 1, i))];
  const y: (number | null)[] = new Array(n).fill(null);
  // la voce in fondo sale di quanto occupa quella che ARRIVA sotto di lei: al cambio di turno la quota combacia
  y[iB] = yBottom - hOf(iB) / 2 - f * (hOf(iB + 1) + gap);
  for (let i = iB - 1; i >= 0; i--) y[i] = (y[i + 1] as number) - (hOf(i + 1) + hOf(i)) / 2 - gap;
  const alpha = st.map((x, i) => (y[i] === null ? 0 : x.alpha));
  for (let i = 0; i < n; i++) if (alpha[i] <= 0) y[i] = null;        // sparita: non si disegna più
  return { y, scale: sc, alpha };
};

/**
 * Quanto è salita dal fondo della corsia, in pixel, una scheda che sta a `v` passi dalla più bassa (Franz, 18/09 19:47: «le
 * schede nella realtà sono molto più vicine»). Non basta un passo fisso: con la deformazione le schede fuori centro sono più
 * piccole e lo stacco apparente cresce. Qui la quota si ottiene **impilando le altezze vere**: fra due schede a un passo di
 * distanza restano sempre `gap` pixel, come sul display (card 209 px, stacco 9).
 */
export const railStackPx = (v: number, cardH: number, gap: number, span: number, steps = 96): number => {
  if (v <= 0) return v * (cardH + gap);
  const h = v / steps;
  let acc = 0;
  for (let k = 0; k < steps; k++) acc += (cardH * railAt(((k + 0.5) * h) / span).scale + gap) * h;
  return acc;
};
