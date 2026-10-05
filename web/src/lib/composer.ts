import type { CmdResult, Session } from './contract'
import type { Sent } from './chatRules'

// Il campo di scrittura, porte di PhonePrimary.kt, NextSteps.box e Slash.kt: il tasto a destra, dove va il testo, i box
// «Prossimi» e «Ricorrenti» sopra il campo, i comandi slash.

export type Button = 'option' | 'send' | 'reopen' | 'none'
export type Target = 'answer_text' | 'prompt'
export type Mode = 'send' | 'send_tonal' | 'stop' | 'reopen' | 'none'

/** Il solo bottone pieno della scheda: con una domanda che ha opzioni è la prima opzione, e «Invia» resta tonale. */
export function button(s: Session, draft: string): Button {
  if (s.state === 'gone') return 'reopen'
  if (s.question?.options.length) return 'option'
  return draft.trim() ? 'send' : 'none'
}
/** Risposta libera se la domanda c'è ancora, altrimenti prompt; null se non si può mandare. */
export function target(s: Session, draft: string): Target | null {
  if (s.state === 'gone' || !draft.trim()) return null
  return s.question ? 'answer_text' : 'prompt'
}
/** Come in Claude Code: Stop mentre la sessione lavora col campo vuoto, se il relay sa fermare; scrivendo torna Invia. */
export function mode(s: Session, draft: string, ops: string[] | null | undefined): Mode {
  if (s.state === 'gone') return 'reopen'
  if (draft.trim()) return button(s, draft) === 'option' ? 'send_tonal' : 'send'
  if ((s.state === 'busy' || s.state === 'awaiting') && ops?.includes('interrupt')) return 'stop'
  return 'none'
}
/** Il prompt suggerito del terminale (contratto 1.23): solo per una sessione ferma, senza domanda, col campo vuoto. */
export const suggestion = (s: Session, draft: string): string | null =>
  s.suggestion?.trim() && s.state === 'idle' && !s.question && !draft.trim() ? s.suggestion : null

const norm = (x: string) => x.toLowerCase().replace(/[^\p{L}\p{N}]+/gu, ' ').trim()
/** Un testo già nel campo, a meno di maiuscole, spazi e punteggiatura. */
export const inDraft = (draft: string, text: string) => { const n = norm(text); return n !== '' && norm(draft).includes(n) }

export type Box = { field: string | null; rows: string[] }
/**
 * I consigli prima del suggerito del terminale, che spesso ne copia uno: il primo fa da suggerimento nel campo vuoto, gli
 * altri stanno nel box senza doppioni, il suggerito del terminale entra solo se diverso. Una riga già nel campo esce dal box.
 */
export function box(steps: string[], terminal: string | null | undefined, draft: string, max = 3): Box {
  const t = terminal?.trim() || null
  if (!steps.length) return { field: t && !draft.trim() ? t : null, rows: [] }
  const seen = new Set<string>()
  const all = [...steps, ...(t ? [t] : [])].filter(x => { const k = norm(x); if (seen.has(k)) return false; seen.add(k); return true })
  const field = draft.trim() ? null : all[0]
  return { field, rows: all.filter(x => x !== field && !inDraft(draft, x)).slice(0, max) }
}
/** Una riga toccata col campo già scritto si accoda: «fai X e poi prova dal vivo»; minuscola tranne le sigle. */
export function append(draft: string, step: string, then = 'e poi'): string {
  const head = draft.trimEnd().replace(/[.,;:]+$/, '').trimEnd()
  if (!head) return step
  const tail = step.length > 1 && /\p{Lu}/u.test(step[0]) && /\p{Ll}/u.test(step[1]) ? step[0].toLowerCase() + step.slice(1) : step
  return `${head} ${then} ${tail}`
}

// I comandi slash: il relay dice quali consente (`state.slash`, senza «/»; null = non li supporta).
export function slashSuggest(draft: string, allowed: string[] | null | undefined): string[] {
  if (!allowed || !draft.startsWith('/') || draft.includes(' ')) return []
  const typed = draft.slice(1)
  const hits = allowed.filter(c => c.startsWith(typed) && c !== typed)
  return hits.length ? hits : typed === '' ? allowed : []
}
export function slashParse(draft: string, allowed: string[] | null | undefined): { cmd: string; args: string | null } | null {
  const t = draft.trim()
  if (!allowed || !t.startsWith('/')) return null
  const sp = t.indexOf(' ')
  const name = t.slice(1, sp < 0 ? undefined : sp)
  if (!allowed.includes(name)) return null
  return { cmd: name, args: sp < 0 ? null : t.slice(sp + 1).trim() || null }
}
/** clear ed exit svuotano o chiudono la sessione: prima si chiede. */
export const slashConfirm = (cmd: string) => cmd === 'clear' || cmd === 'exit'

const NOISE = [/^Settings\b.*\bStatus\b.*\bConfig\b/, /^[↑↓]+$/, /^Plugin skill-listing footprint$/, /^What each plugin's skill descriptions/, /tok\/turn$/]
/** Il pannello che il comando ha aperto sul PC (/cost, /usage), dopo «sent /<cmd> to <name>», senza le righe che non servono. */
export function slashPanel(m: Sent, result: CmdResult | null | undefined): string | null {
  if (result?.ok !== true || !m.text.startsWith('/')) return null
  const i = result.text.indexOf('\n\n')
  if (i < 0) return null
  const out = result.text.slice(i + 2).split('\n').map(l => l.trim().replace(/\s{2,}/g, ' ')).filter(l => l && !NOISE.some(n => n.test(l))).join('\n')
  return out || null
}
