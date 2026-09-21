/**
 * Il terminale di Claude Code ricostruito (Franz, 21/09 19:46: «il terminale deve essere più simile a un terminale di Claude
 * Code»): intestazione con la mascotte, il prompt evidenziato, le frasi di Claude col punto bianco, le chiamate agli
 * strumenti col punto verde e il nome in grassetto, i risultati «⎿» attenuati, e in fondo la casella «>» con la riga di
 * stato. Qui le misure e le regole pure; il disegno è `UiClaudeCode`.
 */
export const CC = { font: 36, row: 54, top: 84, column: 0.4, bottom: 26 };   // `column`: larghezza della colonna in frazione del quadro, che finisca prima dell'orologio

export type CcKind = "says" | "tool" | "result";
export const ccKind = (l: string): CcKind => (l.startsWith("⏺") ? "says" : l.startsWith("⎿") ? "result" : "tool");
export const ccTool = (l: string): { name: string; args: string } => {
  const m = l.match(/^(\w+)(\(.*\))$/);
  return m ? { name: m[1], args: m[2] } : { name: l, args: "" };
};
/** A capo per parole in una colonna larga `width` (Cousine: ogni carattere 0,6 em): `lead` caratteri davanti alla prima riga
 *  («● », «> », «⎿  » più il rientro), e le righe dopo rientrano sotto il testo. */
const wrap = (text: string, width: number, lead: number, font: number): number => {
  const cap = Math.max(1, Math.floor(width / (0.6 * font)) - lead);
  let lines = 1, used = 0;
  for (const w of text.split(" ")) {
    if (used === 0) used = w.length;
    else if (used + 1 + w.length <= cap) used += 1 + w.length;
    else { lines++; used = w.length; }
  }
  return lines;
};
/** Quante righe occupa una riga del terminale: le frasi e gli strumenti hanno «● » davanti, i risultati «⎿  » più due di rientro. */
export const ccLines = (line: string, width: number, font = CC.font): number => {
  const k = ccKind(line);
  return k === "result" ? wrap(line.replace(/^⎿\s*/, ""), width, 5, font) : wrap(k === "says" ? line.replace(/^⏺\s*/, "") : line, width, 2, font);
};
/** Quante righe occupa il prompt («> » davanti, le righe dopo rientrano di due). */
export const ccRows = (prompt: string, width: number, font = CC.font): number => wrap(prompt, width, 2, font);
/** La riga del prompt: sotto l'intestazione (tre righe) e mezza riga d'aria. È lì che si posa il testo dettato. */
export const ccPromptY = (top = CC.top): number => top + 3.5 * CC.row;

/**
 * Ancorato in basso, come Claude Code (Franz, 21/09 20:24: «il testo deve nascere dal basso a blocchi di frasi e salire verso
 * l'alto»): la casella «>» sta in fondo, la conversazione si appoggia sopra e ogni frase nuova nasce lì, spingendo su il resto.
 * La casella: la riga di sopra (con il nome dell'account, se c'è), la riga «>», una linea, la riga di stato.
 */
export const ccBoxHeight = (row = CC.row): number => 3 * row + 1.5;
/** Dove finisce in basso la pila (intestazione, prompt, righe): mezza riga sopra la casella. */
export const ccStackBottom = (height: number, row = CC.row): number => height - CC.bottom - ccBoxHeight(row) - row / 2;
/** Il bordo alto del prompt quando Claude non ha ancora risposto: è l'ultima cosa della pila. Lì si posa il testo dettato. */
export const ccPromptTop = (height: number, prompt: string, width: number, row = CC.row): number => ccStackBottom(height, row) - ccRows(prompt, width) * row;
