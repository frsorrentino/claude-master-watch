import type { State } from './contract'
import type { Freshness } from './durations'
import { isPersonalQuota } from './launch'
import { MASTER } from './summary'

// Lo schema dei dispositivi nelle Impostazioni, porta di SettingsDevices.kt. Verde = vivo, arancio = dato vecchio o orologio
// non pronto, grigio = non abbinato.
export type Tone = 'live' | 'stale' | 'off'
/** Come `PhoneBoard.QuotaRow` di Kotlin (solo quello che serve qui). */
export type QuotaRow = { account: string; personal: boolean; pct: number | null; resetAt: number | null; stale: boolean }
export type Phone = { model: string; version: string; notifications: boolean; tone: Tone }
/** `ageMinutes`: da quanto è vecchio lo stato, null se fresco; `open`: le sessioni aperte come nella home. */
export type Pc = { host: string | null; tone: Tone; ageMinutes: number | null; open: number; accounts: QuotaRow[] }
/** `reachable`: il telefono vede l'orologio adesso; null = non ancora saputo. */
export type Watch = { name: string | null; keyDelivered: boolean; reachable: boolean | null; tone: Tone }
export type DevicesModel = { paired: boolean; phone: Phone; pc: Pc; watch: Watch; pcLink: Tone; watchLink: Tone }
/** Contratto 1.32: un dispositivo accoppiato; `self` = quello che si sta usando. */
export type Linked = { uid: string; name: string; kind: string | null; seen: number | null; tone: Tone; self: boolean }

/** Una lettura nell'ultima mezz'ora è viva; più vecchia, arancio; mai arrivata, spenta. */
export const LIVE_S = 30 * 60

/** Personale prima, poi alfabetico (`PhoneBoard.quotaRows`). L'ora dell'azzeramento resta finché non è passata. */
export const quotaRows = (state: State, now: number): QuotaRow[] =>
  Object.entries(state.quota)
    .map(([account, q]): QuotaRow => ({ account, personal: isPersonalQuota(account, q), pct: q.h5 ?? null, resetAt: q.reset_h5 != null && q.reset_h5 > now ? q.reset_h5 : null, stale: q.stale ?? false }))
    .sort((a, b) => Number(!a.personal) - Number(!b.personal) || (a.account < b.account ? -1 : a.account > b.account ? 1 : 0))

/** I dispositivi veri, nell'ordine del PC; null con un relay prima della 1.32. */
export const linked = (state: State | null, ownUid: string | null, now: number): Linked[] | null =>
  state?.devices?.map(d => {
    const self = d.uid === ownUid
    const seen = d.seen ?? null
    const tone: Tone = self ? 'live' : seen == null ? 'off' : now - seen <= LIVE_S ? 'live' : 'stale'
    return { uid: d.uid, name: d.name, kind: d.kind ?? null, seen, tone, self }
  }) ?? null

export function build(
  host: string | null, state: State | null, freshness: Freshness | null, now: number,
  phoneModel: string, version: string, notifications: boolean,
  watchName: string | null, watchPending: boolean, watchReachable: boolean | null,
): DevicesModel {
  const paired = host != null
  const pcTone: Tone = !paired ? 'off' : state == null || freshness?.stale ? 'stale' : 'live'
  const open = state?.sessions.filter(s => s.state !== 'gone' && s.name !== MASTER).length ?? 0
  const pc: Pc = { host, tone: pcTone, ageMinutes: freshness?.stale ? freshness.minutes : null, open, accounts: state ? quotaRows(state, now) : [] }
  const watchTone: Tone = !paired || watchName == null ? 'off' : watchPending || watchReachable === false ? 'stale' : 'live'
  const watch: Watch = { name: watchName, keyDelivered: watchName != null && !watchPending, reachable: watchReachable, tone: watchTone }
  return { paired, phone: { model: phoneModel, version, notifications, tone: 'live' }, pc, watch, pcLink: pcTone, watchLink: watchTone }
}
