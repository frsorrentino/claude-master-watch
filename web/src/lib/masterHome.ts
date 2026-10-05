import type { Session, State } from './contract'
import { parseSteps } from './nextSteps'
import { FINISHED_S, MASTER, finishedKey, type Sent } from './summary'

// La casa della master, porta di MasterHome.kt: l'ultimo esito in testa, «Per te» (chi ti aspetta, chi ha finito, il
// contesto dall'80 %) e il gruppo «al lavoro». Resoconto e notte, prossimi del recap e invii programmati arrivano con i
// campi del contratto che la web app ancora non legge.
export const MAX = 3
export const URGENT = 80
export type Kind = 'question' | 'finished' | 'context'
export type ForYouRow = { kind: Kind; title: string; detail?: string | null; session: string; key?: string; number?: number; at?: number }
export type ForYou = { rows: ForYouRow[]; more: number }
export type Hero = { headline: string; body: string; steps: string[]; at: number | null }
export type Entry = { role: string; text?: string | null; at: number }

export function forYou(state: State, sent: Sent[], now: number, read: Set<string> = new Set(), limit = MAX): ForYou {
  const live = state.sessions.filter(s => s.state !== 'gone')
  const all: ForYouRow[] = [
    ...live.filter(s => s.question).sort((a, b) => a.question!.asked_at - b.question!.asked_at)
      .map(s => ({ kind: 'question' as const, title: s.name, detail: s.question!.text, session: s.name, at: s.question!.asked_at })),
    ...live.filter(s => s.name !== MASTER && s.state === 'idle' && s.outcome)
      .filter(s => {
        const o = s.outcome!
        return now - o.at <= FINISHED_S && !read.has(finishedKey(s.name, o.at)) &&
          (s.followed || sent.some(x => x.session === s.name)) && !sent.some(x => x.session === s.name && x.sentAt > o.at)
      })
      .sort((a, b) => b.outcome!.at - a.outcome!.at)
      .map(s => ({ kind: 'finished' as const, title: s.name, detail: s.outcome!.full, session: s.name, key: finishedKey(s.name, s.outcome!.at), at: s.outcome!.at })),
    ...live.filter(s => s.context != null && s.context >= URGENT).sort((a, b) => b.context! - a.context!)
      .map(s => ({ kind: 'context' as const, title: s.name, session: s.name, number: s.context! })),
  ]
  return { rows: all.slice(0, limit), more: Math.max(0, all.length - limit) }
}

/** Con una domanda aperta «Per te» va prima dell'ultimo esito: il lavoro bloccato in vista senza scorrere. */
export const forYouFirst = (f: ForYou) => f.rows.some(r => r.kind === 'question')

/**
 * L'ultima risposta di Claude, o senza conversazione l'esito del relay. Titolo: la riga «Esito:», se no la riga «Watch:»,
 * se no la prima riga; la riga per l'orologio non si vede.
 */
export function hero(entries: Entry[], master: Session): Hero | null {
  const last = [...entries].reverse().find(e => e.role !== 'user' && e.role !== 'tool' && e.text?.trim())
  const raw = last?.text ?? master.outcome?.full
  if (raw == null) return null
  const at = last?.at ?? master.outcome?.at ?? null
  const parsed = parseSteps(raw)
  const all = parsed.text.split('\n')
  const isWatch = (l: string) => l.trimStart().startsWith('Watch:')
  const watch = all.find(isWatch)?.trim().slice('Watch:'.length).trim() || null
  const lines = all.filter(l => !isWatch(l))
  const outcome = lines.findIndex(l => l.trimStart().startsWith('Esito:'))
  if (outcome < 0 && watch) return { headline: watch, body: lines.join('\n').trim(), steps: parsed.steps, at }
  const head = outcome >= 0 ? outcome : lines.findIndex(l => l.trim())
  if (head < 0) return null
  const headline = lines[head].trim().replace(/^Esito:/, '').trim()
  return { headline, body: lines.filter((_, i) => i !== head).join('\n').trim(), steps: parsed.steps, at }
}

/** Il gruppo «al lavoro» dentro «Per te»: solo chi lavora adesso, senza la master, con l'ultimo esito intero. */
export function working(state: State): { session: Session; detail: string | null }[] {
  return state.sessions.filter(s => s.name !== MASTER && (s.state === 'busy' || s.state === 'awaiting'))
    .map(s => ({ session: s, detail: s.outcome?.full ?? null }))
}
