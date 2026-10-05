import type { Session, SessionState } from './contract'

// Porte di Order.kt (contratto): l'ordine delle sessioni, la freschezza dello stato, le durate brevi. Fra numero e unità
// uno spazio non separabile (U+00A0): a 384 px «12 min» si spezzava fra due righe.
const NB = ' '

const rank: Record<SessionState, number> = { waiting: 0, busy: 1, awaiting: 1, idle: 2, gone: 3 }
/** waiting, poi busy e awaiting, idle, gone; a parità alfabetico. */
export const orderSessions = (list: Session[]) =>
  [...list].sort((a, b) => rank[a.state] - rank[b.state] || a.name.toLowerCase().localeCompare(b.name.toLowerCase()))

export const STALE_AFTER_S = 180
export type Freshness = { stale: false } | { stale: true; minutes: number }
/** Lo stato è vecchio da tre minuti senza aggiornamenti. */
export function freshness(stateTs: number, now: number): Freshness {
  const age = now - stateTs
  return age < STALE_AFTER_S ? { stale: false } : { stale: true, minutes: Math.floor(age / 60) }
}

/** «2 m», «1 h 05», «3 g»: da `from` a `now`, epoch in secondi. */
export function since(from: number, now: number, days = 'g'): string {
  const s = Math.max(0, now - from)
  if (s < 3600) return `${Math.floor(s / 60)}${NB}m`
  if (s < 86400) return `${Math.floor(s / 3600)}${NB}h${NB}${String(Math.floor((s % 3600) / 60)).padStart(2, '0')}`
  return `${Math.floor(s / 86400)}${NB}${days}`
}
