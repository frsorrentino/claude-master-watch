import type { Event, State } from './contract'
import { isPersonalQuota, type Tone } from './briefCards'
import { today, type Bar } from './dayBars'
import { shortModel } from './header'
import { pace, type Pace, type Sample } from './quotaHistory'
import * as work from './workPanel'

// La Panoramica del telefono, porta di PhoneOverview.kt: anelli per account, «Adesso», domande, contesto, «Oggi», notte
// e ora dell'aggiornamento.

/**
 * `pace` null senza almeno due campioni, senza l'ora della ripartenza o con un dato vecchio (si proietterebbe dal passato).
 * `weekResetAt`: quando si azzera la settimana, finché non è passata anche con un dato vecchio; `stale`: lettura vecchia.
 */
export type Ring = {
  account: string; personal: boolean; h5: number | null; w7: number | null; resetAt: number | null; pace: Pace | null
  weekResetAt: number | null; stale: boolean
}
/** Una riga del contesto: le soglie della card delle misure, più modello ed effort come li legge il PC. */
export type ContextRow = { name: string; pct: number; tone: Tone; model: string | null; effort: string | null }
export type Model = {
  rings: Ring[]; now: work.Now; questions: work.Questions | null; contexts: ContextRow[]
  today: Bar[]; nightQueued: number; updated: work.Updated
}

/** Porta di PhoneBoard.quotaRows: personale prima, poi alfabetico; l'ora del reset resta finché non è passata. */
const quotaRows = (state: State, now: number) => Object.entries(state.quota)
  .map(([account, q]) => ({ account, personal: isPersonalQuota(account, q), resetAt: q.reset_h5 != null && q.reset_h5 > now ? q.reset_h5 : null, stale: q.stale ?? false }))
  .sort((a, b) => Number(!a.personal) - Number(!b.personal) || (a.account < b.account ? -1 : a.account > b.account ? 1 : 0))

export function build(state: State, events: Event[], samples: Record<string, Sample[]>, now: number, timeZone: string | undefined, stale: boolean): Model {
  const rings = quotaRows(state, now).map((row): Ring => {
    const q = state.quota[row.account]
    const s = samples[row.account]
    const p = s && s.length >= 2 && row.resetAt != null && !stale && !row.stale ? pace(s, row.resetAt, now) : null
    return { account: row.account, personal: row.personal, h5: q.h5 ?? null, w7: q.w7 ?? null, resetAt: row.resetAt, pace: p,
      weekResetAt: q.reset_w7 != null && q.reset_w7 > now ? q.reset_w7 : null, stale: row.stale }
  })
  const contexts = work.contexts(state).map((c): ContextRow => {
    const s = state.sessions.find(x => x.name === c.name)!
    return { name: c.name, pct: c.pct, tone: c.tone, model: shortModel(s.model), effort: s.effort?.trim() || null }
  })
  return {
    rings, now: work.now(state), questions: work.questions(state, now), contexts,
    today: today(events, now, timeZone), nightQueued: state.night.queued,
    updated: { minutes: Math.max(0, Math.trunc((now - state.ts) / 60)), host: state.host, stale },
  }
}
