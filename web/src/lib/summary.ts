import type { QuotaAccount, Session, State } from './contract'

// Il riepilogo unico della home, porta di Summary.kt: ogni sessione una volta, nell'ordine del bisogno (ti aspetta, ha
// finito, al lavoro, ferme), le chiuse a parte. La master compare nella lista solo quando aspetta.
export const MASTER = 'master'
export const FINISHED_S = 12 * 3600
export type Group = 'waiting' | 'finished' | 'working' | 'still'
export type Row = { group: Group; session: Session; text: string | null; at: number | null; key?: string; quota?: QuotaAccount }
export type Model = { rows: Row[]; closed: Session[]; open: number; master: Session | null }
/** Un messaggio mandato da questo dispositivo, per sapere a chi abbiamo scritto. */
export type Sent = { session: string; sentAt: number }

export const finishedKey = (session: string, at: number) => `finished:${session}@${at}`

export function build(state: State, sent: Sent[], now: number, read: Set<string>): Model {
  const live = state.sessions.filter(s => s.state !== 'gone')
  const master = live.find(s => s.name === MASTER) ?? null
  const others = live.filter(s => s.name !== MASTER)
  const waiting: Row[] = live.filter(s => s.question).sort((a, b) => a.question!.asked_at - b.question!.asked_at)
    .map(s => ({ group: 'waiting', session: s, text: s.question!.text, at: s.question!.asked_at }))
  const asking = new Set(waiting.map(r => r.session.name))
  // Chi ha finito resta finché non lo apri o non gli scrivi, per 12 ore; solo le seguite o quelle a cui hai scritto.
  const finished: Row[] = others.filter(s => s.state === 'idle' && s.outcome && !asking.has(s.name))
    .filter(s => {
      const o = s.outcome!
      return now - o.at <= FINISHED_S && !read.has(finishedKey(s.name, o.at)) &&
        (s.followed || sent.some(x => x.session === s.name)) && !sent.some(x => x.session === s.name && x.sentAt > o.at)
    })
    .sort((a, b) => b.outcome!.at - a.outcome!.at)
    .map(s => ({ group: 'finished', session: s, text: s.outcome!.full, at: s.outcome!.at, key: finishedKey(s.name, s.outcome!.at) }))
  const working: Row[] = others.filter(s => (s.state === 'busy' || s.state === 'awaiting') && !asking.has(s.name))
    .map(s => ({ group: 'working', session: s, text: s.outcome?.full ?? null, at: s.turn_started ?? s.since }))
  const taken = new Set([...waiting, ...finished, ...working].map(r => r.session.name))
  const still: Row[] = others.filter(s => !taken.has(s.name)).sort((a, b) => b.since - a.since)
    .map(s => ({ group: 'still', session: s, text: s.outcome?.full ?? null, at: s.since }))
  return {
    rows: [...waiting, ...finished, ...working, ...still].map(r => ({ ...r, quota: state.quota[r.session.account] })),
    closed: state.sessions.filter(s => s.state === 'gone'),
    open: others.length,
    master,
  }
}

/** «5 m», «3 h», «2 g» dall'istante `since` a `now` (epoch s). */
export function age(since: number, now: number): string {
  const s = Math.max(0, now - since)
  if (s < 3600) return `${Math.max(1, Math.round(s / 60))} m`
  if (s < 86400) return `${Math.round(s / 3600)} h`
  return `${Math.round(s / 86400)} g`
}
