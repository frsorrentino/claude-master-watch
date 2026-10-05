import type { Session, State } from './contract'
import { pace, type Sample } from './quotaHistory'

// L'avviso della quota sopra la barra di scrittura, porta di QuotaWarning.kt: la finestra di 5 ore dell'account della
// sessione è al 90 % o il ritmo la esaurisce prima del reset. Mai con un dato vecchio, senza lettura o senza reset futuro.
export const THRESHOLD = 90
// Il giudizio sul ritmo aspetta prove: campioni che coprono almeno mezz'ora e la finestra almeno al 20 %.
export const MIN_SPAN_S = 30 * 60
export const MIN_PCT = 20

export type Warn = { account: string; pct: number; resetAt: number; projected: boolean }

/** `samples`: i campioni delle 5 ore dell'account della sessione. */
export function of(state: State, session: Session, samples: Sample[], now: number): Warn | null {
  const entry = Object.entries(state.quota).find(([k]) => k.toLowerCase() === session.account.toLowerCase())
  if (!entry) return null
  const [name, q] = entry
  if (q.stale) return null
  const pct = q.h5
  if (pct == null) return null
  const reset = q.reset_h5 != null && q.reset_h5 > now ? q.reset_h5 : null
  if (reset == null) return null
  if (pct >= THRESHOLD) return { account: name, pct, resetAt: reset, projected: false }
  if (pct < MIN_PCT) return null
  const p = pace(samples, reset, now)
  const span = p.points.length >= 2 ? p.points[p.points.length - 1].ts - p.points[0].ts : 0
  if (span < MIN_SPAN_S) return null
  if (p.projected == null) return null
  return p.projected >= 100 ? { account: name, pct, resetAt: reset, projected: true } : null
}
