import type { Session, SessionState } from './contract'

// Porte di Order.kt (contratto): l'ordine delle sessioni, la freschezza dello stato, le durate brevi. Fra numero e unità
// uno spazio non separabile (U+00A0): a 384 px «12 min» si spezzava fra due righe.
const NB = ' '

const rank: Record<SessionState, number> = { waiting: 0, busy: 1, awaiting: 1, idle: 2, gone: 3 }
/** waiting, poi busy e awaiting, idle, gone; a parità alfabetico. */
export const orderSessions = (list: Session[]) =>
  [...list].sort((a, b) => rank[a.state] - rank[b.state] || a.name.toLowerCase().localeCompare(b.name.toLowerCase()))

export const STALE_AFTER_S = 180
export const SLOW_LAG_S = 30
/** `slowS` (contratto 1.43): stato arrivato da poco, ma raccolto tanti secondi prima della pubblicazione, «PC lento». */
export type Freshness = { stale: false; slowS?: number } | { stale: true; minutes: number }
type Stamped = { ts: number; published_at?: number | null }
/** Da quando lo stato vale per il PC: la pubblicazione (1.43), o `ts` con un relay precedente. */
export const publishedTs = (s: Stamped) => Math.trunc(s.published_at ?? s.ts)
/** Lo stato è vecchio da tre minuti senza pubblicazioni; col PC lento resta fresco, e il menu lo dice. */
export function freshness(s: Stamped, now: number): Freshness {
  const pub = publishedTs(s)
  if (now - pub >= STALE_AFTER_S) return { stale: true, minutes: Math.floor((now - pub) / 60) }
  const lag = pub - s.ts
  return s.published_at != null && lag >= SLOW_LAG_S ? { stale: false, slowS: lag } : { stale: false }
}

export const WAITING_PC_MS = 10_000
/** Da quanti secondi una lettura aspetta il PC, con la chat già piena e oltre 10 s; null se non c'è niente da dire. */
export const waitingPc = (hasEntries: boolean, askedAtMs: number | null, nowMs: number): number | null =>
  askedAtMs == null || !hasEntries || nowMs - askedAtMs <= WAITING_PC_MS ? null : Math.floor((nowMs - askedAtMs) / 1000)

/** «2 m», «1 h 05», «3 g»: da `from` a `now`, epoch in secondi. */
export function since(from: number, now: number, days = 'g'): string {
  const s = Math.max(0, now - from)
  if (s < 3600) return `${Math.floor(s / 60)}${NB}m`
  if (s < 86400) return `${Math.floor(s / 3600)}${NB}h${NB}${String(Math.floor((s % 3600) / 60)).padStart(2, '0')}`
  return `${Math.floor(s / 86400)}${NB}${days}`
}
