import type { Session, SessionState } from './contract'

// I gruppi della home, come sul telefono: chi ti aspetta, chi lavora, chi è fermo, le chiuse in fondo.
export type Group = 'waiting' | 'working' | 'idle' | 'closed'
const ORDER: Group[] = ['waiting', 'working', 'idle', 'closed']
const OF: Record<SessionState, Group> = { waiting: 'waiting', busy: 'working', awaiting: 'working', idle: 'idle', gone: 'closed' }

export function groups(sessions: Session[]): { group: Group; sessions: Session[] }[] {
  return ORDER.map(group => ({ group, sessions: sessions.filter(s => OF[s.state] === group).sort((a, b) => b.since - a.since) }))
    .filter(g => g.sessions.length > 0)
}

/** «5 m», «3 h», «2 g» dall'istante `since` a `now` (epoch s). */
export function age(since: number, now: number): string {
  const s = Math.max(0, now - since)
  if (s < 3600) return `${Math.max(1, Math.round(s / 60))} m`
  if (s < 86400) return `${Math.round(s / 3600)} h`
  return `${Math.round(s / 86400)} g`
}
