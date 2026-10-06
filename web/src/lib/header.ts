import type { Model, Session, State } from './contract'

// Le misure e i testi della testata della sessione, porte di ModelText, Tune, ContextActions, SessionMeters e SessionsText.

/** «Opus 5», «Haiku 4.5»: l'etichetta se c'è, se no dall'id; un id sconosciuto si mostra com'è. */
export function shortModel(m: Model | null | undefined): string | null {
  if (!m) return null
  if (m.label?.trim()) return m.label
  const id = m.id.split('[')[0].replace(/^claude-/, '').replace(/^-+|-+$/g, '')
  if (!id) return null
  const parts = id.split('-').filter(p => !(p.length === 8 && /^\d+$/.test(p)))
  const family = parts[0]
  if (!family) return null
  const nums = parts.slice(1).filter(p => /^\d+$/.test(p))
  return nums.length ? `${family[0].toUpperCase()}${family.slice(1)} ${nums.join('.')}` : parts.join('-')
}

/** «claude-opus-5-5[1m]» e «claude-opus-5-5» sono lo stesso modello. */
export const sameModel = (a?: string | null, b?: string | null) => a != null && b != null && a.split('[')[0] === b.split('[')[0]

/** La finestra da 1M dello stesso modello, se c'è fra le scelte e la sessione non la usa già. */
/** La famiglia del modello, per la riga che lo spiega nel pannello Modello ed effort; sconosciuta: null. */
export const modelKind = (id: string) => (['opus', 'fable', 'sonnet', 'haiku'] as const).find(k => id.includes(k)) ?? null

export function wider(s: Session, choices: State['choices']): Model | null {
  const id = s.model?.id
  if (!id || id.endsWith('[1m]')) return null
  return choices?.models.find(m => m.id.endsWith('[1m]') && sameModel(m.id, id)) ?? null
}

export type Tone = 'neutral' | 'warn' | 'alert'
/** Da tre quarti il contesto (e la quota) è da guardare, dal 90 % è il momento di chiudere il turno. */
export const tone = (pct: number | null | undefined): Tone => (pct == null ? 'neutral' : pct >= 90 ? 'alert' : pct >= 75 ? 'warn' : 'neutral')

/** L'ora dell'azzeramento, col giorno se non è oggi; niente se è già passata. */
export function resetLabel(resetAt: number | null | undefined, now: number, timeZone?: string): string | null {
  if (resetAt == null || resetAt <= now) return null
  const day = (t: number) => new Date(t * 1000).toLocaleDateString('it-IT', { timeZone })
  const opts: Intl.DateTimeFormatOptions = { hour: '2-digit', minute: '2-digit', timeZone }
  if (day(resetAt) !== day(now)) opts.weekday = 'short'
  return new Date(resetAt * 1000).toLocaleString('it-IT', opts).replace(',', '')
}

/** Le righe sotto la testata: obiettivo, bassa priorità, finestra chiusa con la sessione viva. */
export function notes(s: Session, t: { goal: string; lowPriority: string; lowPriorityOffered: string; noWindow: string }): string[] {
  const goal = s.goal?.text?.trim()
  const prio = s.low_priority?.trim().toLowerCase()
  return [
    goal ? `${t.goal}: ${goal}` : null,
    prio === 'active' ? t.lowPriority : prio === 'offered' ? t.lowPriorityOffered : null,
    !s.attached && s.state !== 'gone' ? t.noWindow : null,
  ].filter((x): x is string => x != null)
}
