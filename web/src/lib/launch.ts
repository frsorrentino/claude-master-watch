import type { Project, QuotaAccount, Session, State } from './contract'

// Account e foglio «Lancia», porte di Accounts.kt e LaunchSuggest.kt.
export const PERSONAL = 'personal'

/** Lo dice il tipo che manda il relay (1.8); senza il tipo, un relay vecchio, il nome «personale». */
export const personal = (name: string, kind?: string | null): boolean =>
  kind != null ? kind === PERSONAL : name.toLowerCase() === 'personale'

export const isPersonal = (s: Session): boolean => personal(s.account, s.account_kind)
export const isPersonalQuota = (name: string, q: QuotaAccount): boolean => personal(name, q.kind)

/** Il nome dell'account personale fra quelli della quota, se c'è. */
export const personalQuota = (state: State): string | null =>
  Object.entries(state.quota).find(([k, q]) => isPersonalQuota(k, q))?.[0] ?? null

/** L'account scelto se esiste nella quota, altrimenti quello personale, altrimenti il primo in ordine. */
export function resolve(state: State, chosen: string): string | null {
  const keys = Object.keys(state.quota)
  return keys.find(k => k.toLowerCase() === chosen.toLowerCase()) ?? personalQuota(state) ?? keys.reduce<string | null>((a, k) => (a == null || k < a ? k : a), null)
}

const used = (p: Project) => p.last_used ?? -Infinity

/** I progetti dell'account scelto, per pezzo di nome, i più recenti prima. */
export function projects(state: State, account: string, typed: string, limit = 6): Project[] {
  const t = typed.trim().toLowerCase()
  return state.projects.filter(p => p.account === account && (!t || p.name.toLowerCase().includes(t)))
    .sort((a, b) => used(b) - used(a)).slice(0, limit)
}

/**
 * La ricerca con completamento: prima i nomi che iniziano con il testo, poi quelli che lo contengono, poi quelli che lo
 * hanno nella cartella; a pari merito il più recente. `account` null = tutti e due; `pool` = l'elenco completo (1.26).
 */
export function ranked(state: State, typed: string, { account = null, limit = 6, pool = state.projects }: { account?: string | null; limit?: number; pool?: Project[] } = {}): Project[] {
  const t = typed.trim().toLowerCase()
  const rank = (p: Project): number | null => !t || p.name.toLowerCase().startsWith(t) ? 0 : p.name.toLowerCase().includes(t) ? 1 : p.path.toLowerCase().includes(t) ? 2 : null
  return pool.filter(p => account == null || p.account === account)
    .flatMap(p => { const r = rank(p); return r == null ? [] : [{ r, p }] })
    .sort((a, b) => a.r - b.r || used(b.p) - used(a.p))
    .map(x => x.p).slice(0, limit)
}

/** I recenti a campo vuoto: solo i progetti con una data d'uso, i più recenti prima. */
export function recent(state: State, account: string | null, { limit = 6, pool = state.projects }: { limit?: number; pool?: Project[] } = {}): Project[] {
  return pool.filter(p => (account == null || p.account === account) && p.last_used != null)
    .sort((a, b) => b.last_used! - a.last_used!).slice(0, limit)
}

/** Le sessioni con il testo nel nome, prima quelle che cominciano così. */
export function sessions(state: State, typed: string, limit = 3): Session[] {
  const t = typed.trim().toLowerCase()
  if (!t) return []
  const first = (s: Session) => (s.name.toLowerCase().startsWith(t) ? 0 : 1)
  return state.sessions.filter(s => s.name.toLowerCase().includes(t))
    .sort((a, b) => first(a) - first(b) || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0)).slice(0, limit)
}
