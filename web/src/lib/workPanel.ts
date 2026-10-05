import type { State } from './contract'
import { since, type Freshness } from './durations'
import { tone } from './header'
import type { Tone } from './briefCards'

// La sezione Lavoro della pagina Quota, porta di WorkPanel.kt: i numeri, una grafica per dato; il disegno sta altrove.

/** Il segmento di una sessione viva, nei colori dei badge. */
export type Seg = 'waiting' | 'working' | 'idle'
export type Now = { segments: Seg[]; working: number; waiting: number; idle: number }
const SEG_ORDER: Seg[] = ['waiting', 'working', 'idle']

/** Prima chi aspetta te, poi chi lavora, poi chi è ferma: si legge da sinistra quello che chiede attenzione. */
export function now(state: State): Now {
  const segs = state.sessions.flatMap((s): Seg[] =>
    s.state === 'gone' ? []
      : s.question != null || s.state === 'waiting' ? ['waiting']
        : s.state === 'busy' || s.state === 'awaiting' ? ['working'] : ['idle'])
    .sort((a, b) => SEG_ORDER.indexOf(a) - SEG_ORDER.indexOf(b))
  const count = (x: Seg) => segs.filter(s => s === x).length
  return { segments: segs, working: count('working'), waiting: count('waiting'), idle: count('idle') }
}

export type Ctx = { name: string; pct: number; tone: Tone }
/** Quante righe entrano nella card senza farla diventare una lista: le più piene sono quelle da guardare. */
export const MAX_CONTEXT_ROWS = 4

/** Le sessioni vive con il contesto noto, dalla più piena, con le soglie della card delle misure. */
export const contexts = (state: State): Ctx[] =>
  state.sessions.filter(s => s.state !== 'gone' && s.context != null)
    .sort((a, b) => b.context! - a.context!)
    .slice(0, MAX_CONTEXT_ROWS)
    .map(s => ({ name: s.name, pct: s.context!, tone: tone(s.context) }))

export type Questions = { count: number; oldest: string; age: string }
/** Null senza domande: la card compare solo quando c'è qualcuno che aspetta. */
export function questions(state: State, nowTs: number): Questions | null {
  const con = state.sessions.filter(s => s.question)
  if (!con.length) return null
  const vecchia = con.reduce((a, b) => (b.question!.asked_at < a.question!.asked_at ? b : a))
  return { count: con.length, oldest: vecchia.name, age: since(vecchia.question!.asked_at, nowTs) }
}

/** `progress`: quanta coda è già passata, una in corso su quelle che restano; zero se non ne gira nessuna. */
export type NightQueue = { queued: number; running: string | null; progress: number }
export function night(state: State): NightQueue | null {
  const n = state.night
  const running = n.running ?? null
  if (n.queued <= 0 && running == null) return null
  return { queued: n.queued, running, progress: running == null ? 0 : 1 / (n.queued + 1) }
}

export type Updated = { minutes: number; host: string; stale: boolean }
/** Età dello stato in minuti e nome della macchina, per la riga in fondo; `stale` la fa diventare rossa. */
export const updated = (state: State, fresh: Freshness, nowTs: number): Updated =>
  ({ minutes: Math.max(0, Math.trunc((nowTs - state.ts) / 60)), host: state.host, stale: fresh.stale })
