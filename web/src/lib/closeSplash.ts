import type { Session } from './contract'

// Il pannello di chiusura di una sessione (CloseSplash.kt; Franz, 08/10 18:06-18:29, variante A): la pagina non sparisce.
// Dopo /exit dice che si sta chiudendo, con i passi veri; poi che è chiusa e a che ora, e dopo 3 s si torna alla home.
export const CLOSED_SHOW_MS = 3_000
export const SLOW_S = 20

export type Leaving = { name: string; sentAt: number; delivered: boolean }
export type Phase =
  | { kind: 'closing'; name: string; sentAt: number; delivered: boolean; slow: boolean }
  | { kind: 'closed'; name: string; at: number; byMe: boolean }

/**
 * La fase per la pagina di `name`; null = niente pannello. `lastSeen` è la sessione com'era l'ultima volta nello stato.
 * Chiusa per conto suo ma ancora nello stato resta una pagina con «Riapri» (03/10 19:57): il pannello arriva quando esce.
 */
export function closeSplash(name: string, sessions: Session[], leaving: Leaving | null, lastSeen: Session | null, now: number): Phase | null {
  const s = sessions.find(x => x.name === name)
  const mine = leaving?.name === name
  // Per una gone `since` è l'ultimo avvistamento, cioè quando si è chiusa; per una viva no: allora vale adesso.
  const at = s?.since ?? (lastSeen?.state === 'gone' ? lastSeen.since : now)
  if (!s) return { kind: 'closed', name, at, byMe: mine }
  if (s.state === 'gone') return mine ? { kind: 'closed', name, at, byMe: true } : null
  if (mine) return { kind: 'closing', name, sentAt: leaving!.sentAt, delivered: leaving!.delivered, slow: now - leaving!.sentAt >= SLOW_S }
  return null
}
