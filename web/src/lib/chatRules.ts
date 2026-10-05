import type { CmdResult, Session, State } from './contract'

// Lo stato dei messaggi della chat, porta di ChatRules.kt: dalla conferma del comando e dai turni della sessione.
/** Un messaggio mandato da questo dispositivo (campi come `Sent` di Kotlin; compatibile con `Sent` di summary.ts). */
export type Sent = {
  id: string; session: string; text: string; sentAt: number
  startedAt?: number | null; doneAt?: number | null; outcomeShort?: string | null; outcomeFull?: string | null
  attachment?: string | null
  /** Il motivo per cui non è stato consegnato; null finché va bene. */
  failed?: string | null
  /** Invio programmato: finché `sentAt` è prima di quest'ora il messaggio aspetta. */
  scheduledFor?: number | null
  /** Il pannello che un comando slash ha aperto sul PC. */
  panel?: string | null
}
export type Status = 'scheduled' | 'offline' | 'uploading' | 'sending' | 'sent' | 'uncertain' | 'failed' | 'delivered' | 'queued' | 'working' | 'done'
/** Come `PendingStatus` di Repo.kt. */
export type PendingStatus = 'sending' | 'sent' | 'queued' | 'failed'
export type Upload = { type: 'going' } | { type: 'failed'; reason: string }

/** Scarto tollerato fra l'orologio del telefono (`sentAt`) e quello del PC (`turn_started`, esito). */
export const SKEW_S = 10
export const KEEP_S = 7 * 86_400
/** Oltre questo tempo dall'invio un turno nuovo non è più di quel messaggio. */
export const CLAIM_S = 1_800

/** Programmato e non ancora partito. */
export const waiting = (m: Sent): boolean => m.scheduledFor != null && m.sentAt < m.scheduledFor

export function status(m: Sent, pending: PendingStatus | null, result: CmdResult | null, s: Session | null, upload: Upload | null = null): Status {
  if (m.failed != null || upload?.type === 'failed' || result?.ok === false) return 'failed'
  if (waiting(m)) return 'scheduled'
  if (upload?.type === 'going') return 'uploading'
  if (m.doneAt != null) return 'done'
  if (m.startedAt != null) return 'working'
  // Nessuna risposta in 20 s: spesso il messaggio è arrivato lo stesso.
  if (pending === 'failed' && result == null) return 'uncertain'
  if (result == null && pending === 'queued') return 'offline'
  if (result == null && pending === 'sent') return 'sent'
  if (result == null && pending != null) return 'sending'
  if (s?.state === 'busy' && (s.turn_started ?? Number.MAX_SAFE_INTEGER) < m.sentAt - SKEW_S) return 'queued'
  return 'delivered'
}

/** Perché non è stato consegnato; null se il PC non ha risposto in tempo. */
export function reason(_pending: PendingStatus | null, result: CmdResult | null, upload: Upload | null, m: Sent | null = null): string | null {
  if (m?.failed != null) return m.failed
  if (upload?.type === 'failed') return upload.reason
  if (result?.ok === false) return result.text.trim() !== '' ? result.text : null
  return null
}

export function advance(m: Sent, s: Session | null, now: number): Sent {
  if (m.doneAt != null || m.failed != null || s == null || waiting(m)) return m
  const from = m.sentAt - SKEW_S
  const until = m.sentAt + CLAIM_S
  // Una domanda di permesso a metà turno non lo chiude.
  const running = s.state === 'busy' || s.state === 'awaiting' || s.state === 'waiting'
  const out = s.outcome
  const turn = s.turn_started ?? -Infinity
  if (m.startedAt == null && s.state === 'busy' && turn >= from && turn <= until) return { ...m, startedAt: s.turn_started }
  // Il relay mette la sessione in «awaiting» appena consegna il prompt: il turno è suo da lì.
  if (m.startedAt == null && s.state === 'awaiting' && now <= until) return { ...m, startedAt: now }
  if (m.startedAt != null && !running) {
    const mine = out && out.at >= m.startedAt ? out : null
    return { ...m, doneAt: now, outcomeShort: mine?.short ?? null, outcomeFull: mine?.full ?? null }
  }
  // Turno partito e finito fra due stati: lo dice solo l'esito, più nuovo dell'invio.
  if (m.startedAt == null && !running && out && out.at >= from && out.at <= until)
    return { ...m, startedAt: m.sentAt, doneAt: out.at, outcomeShort: out.short, outcomeFull: out.full }
  return m
}

/** I programmati da mandare adesso: l'ora è passata, anche da molto. */
export const due = (list: Sent[], now: number): Sent[] => list.filter(m => waiting(m) && m.scheduledFor! <= now)

/** «Manda stanotte»: la cartella del progetto della sessione fra quelle del PC; null se non c'è. */
export const nightDir = (state: State, s: Session): string | null =>
  state.projects.find(p => p.path === s.project || p.path.endsWith('/' + s.project))?.path ?? null

export const prune = (list: Sent[], now: number): Sent[] => list.filter(m => m.sentAt >= now - KEEP_S)
