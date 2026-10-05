import type { Option, Question } from './contract'

// Le regole della domanda, porta di QuestionRules.kt: pressione lunga per il rischio alto, «Consenti tutto» mai per high,
// le opzioni brevi su una riga in parti uguali, l'etichetta senza il riquadro disegnato a caratteri.
export const needsLongPress = (tier: Question['tier']) => tier === 'high'
/** Senza opzioni lette non c'è «non chiedere più»: il relay rifiuterebbe allow_all. */
export const allowAllVisible = (q: Question) => q.kind === 'permission' && q.tier !== 'high' && q.options.length > 0

/** L'etichetta senza il riquadro (U+2500–U+259F): la prima riga che dice qualcosa. */
export function optionText(label: string): string {
  return label.split('\n').map(l => l.replace(/[─-▟]/g, ' ').replace(/\s+/g, ' ').trim()).find(l => l) ?? label.trim()
}
export const optionLabel = (o: Option) => `${o.n} · ${optionText(o.label)}`

export const INLINE_CHARS = 14
/** Due o tre opzioni brevi su una riga, in parti uguali; altrimenti una sotto l'altra. */
export const inline = (options: Option[]) => options.length >= 2 && options.length <= 3 && options.every(o => optionText(o.label).length <= INLINE_CHARS)

/** Contratto 1.10: la risposta a parole, gli a capo diventano spazi. */
export const textArg = (text: string) => `text:${text.replace(/\s*\n\s*/g, ' ').trim()}`
/** Contratto 1.10: «Parliamone», la domanda rifiutata e la sessione in attesa di un messaggio. */
export const CHAT_ARG = 'chat'
