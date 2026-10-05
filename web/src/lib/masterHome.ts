import type { Event, Session, State } from './contract'
import { parseSteps } from './nextSteps'
import { FINISHED_S, MASTER, finishedKey, type Sent } from './summary'

// La casa della master, porta di MasterHome.kt: l'ultimo esito in testa, «Per te» e il gruppo «al lavoro». «Per te» in
// ordine di urgenza: domande, turni finiti, contesto dall'80 %, resoconto della notte (la mattina, finché non è letto), notte
// (dalle 20), prossimi passi del recap dei progetti senza sessione, invii programmati.
export const MAX = 3
export const URGENT = 80
export const MORNING_END = 12
export const EVENING = 20
export type Kind = 'question' | 'finished' | 'context' | 'night_report' | 'night' | 'next_step' | 'scheduled'
export type ForYouRow = {
  kind: Kind; title: string; detail?: string | null; session?: string | null; project?: string | null
  key?: string; number?: number; at?: number
}
export type ForYou = { rows: ForYouRow[]; more: number }
export type Hero = { headline: string; body: string; steps: string[]; at: number | null }
export type Entry = { role: string; text?: string | null; at?: number | null }
/** Un messaggio programmato da questo dispositivo: aspetta finché `sentAt` è prima di `scheduledFor`. */
export type Scheduled = Sent & { scheduledFor?: number | null }

/** La chiave di un prossimo passo avviato: ricordata, il passo non torna. */
export const nextKey = (project: string, next: string) => `next:${project}:${next}`

/** Giorno ISO e ora locali di un istante, nel fuso dato (quello del browser se manca). */
export function local(epoch: number, timeZone?: string): { day: string; hour: number } {
  const p = Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date(epoch * 1000)).map(x => [x.type, x.value]))
  return { day: `${p.year}-${p.month}-${p.day}`, hour: Number(p.hour) }
}
const dayBefore = (iso: string) => new Date(Date.parse(`${iso}T12:00:00Z`) - 86400000).toISOString().slice(0, 10)

export function forYou(
  state: State, events: Event[], sent: Scheduled[], now: number,
  { read = new Set<string>(), limit = MAX, timeZone }: { read?: Set<string>; limit?: number; timeZone?: string } = {},
): ForYou {
  const { day: today, hour } = local(now, timeZone)
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
  if (hour < MORNING_END) {
    const report = events.filter(e => e.kind === 'night_report').sort((a, b) => b.ts - a.ts)[0]
    if (report && local(report.ts, timeZone).day === today && !read.has(report.key))
      all.push({ kind: 'night_report', title: report.title, detail: report.body ?? '', key: report.key })
  }
  if (hour >= EVENING && state.night.items != null) all.push({ kind: 'night', title: '', number: state.night.queued })
  // Il recap di oggi o di ieri: quello delle 20 serve anche la mattina dopo. Mai a un progetto con una sessione aperta: il
  // progetto della sessione è un percorso relativo, quello del recap il nome della cartella.
  if (state.recap.date && state.recap.date >= dayBefore(today)) for (const item of state.recap.items) {
    const next = item.next?.trim()
    if (!next) continue
    if (live.some(s => s.name === item.project || s.project.split('/').pop() === item.project)) continue
    if (read.has(nextKey(item.project, next))) continue
    all.push({ kind: 'next_step', title: item.project, detail: next, project: state.projects.find(p => p.name === item.project)?.path ?? null })
  }
  const waiting = sent.filter(m => m.scheduledFor != null && m.sentAt < m.scheduledFor)
  if (waiting.length) {
    const first = waiting.reduce((a, b) => (b.scheduledFor! < a.scheduledFor! ? b : a))
    all.push({ kind: 'scheduled', title: '', session: first.session, number: waiting.length, at: first.scheduledFor! })
  }
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
