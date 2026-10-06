// Contratto 1.37, la master al servizio dell'app: le regole delle cinque funzioni (mockup approvati da Franz il 05/10
// alle 21:07, docs/mockup/2026-10-05-contratto-1-37/). Le stesse in MasterService.kt.
import type { Model, Session, State } from './contract'
import { sameModel } from './header'
import { parseSteps } from './nextSteps'
import { MASTER } from './summary'

/** A. Il consiglio nel foglio Modello ed effort: `dot` sulla pillola, `cost` (token di cache) solo a metà lavoro. */
export type AdviceView = { model: string; effort: string; reason: string; dot: boolean; cost: number | null }
/** Oltre 6 ore il consiglio non vale più (il relay lo toglie; qui anche con uno stato vecchio). */
const ADVICE_MAX_AGE_S = 6 * 3600

export function adviceOf(s: Session, choices: State['choices'], now: number): AdviceView | null {
  const a = s.advice
  if (!a || now - a.at >= ADVICE_MAX_AGE_S) return null
  if (!choices?.models.some(m => sameModel(m.id, a.model)) || !choices.efforts.includes(a.effort)) return null
  return { model: a.model, effort: a.effort, reason: a.reason, dot: a.differs, cost: a.differs && a.when === 'next_task' && a.switch_cost_tokens > 0 ? a.switch_cost_tokens : null }
}

/** A. «Usa il consiglio»: solo quello che cambia rispetto alla scelta attuale; il modello con l'id della lista (`[1m]`
 *  compreso), quello che il PC sa applicare. */
export function adviceSteps(a: AdviceView, choices: State['choices'], model: string | null | undefined, effort: string | null | undefined): { model: Model | null; effort: string | null } {
  const m = sameModel(model, a.model) ? null : choices?.models.find(x => sameModel(x.id, a.model)) ?? null
  return { model: m, effort: a.effort === effort ? null : a.effort }
}

/** «36.000»: i token del costo del cambio, come si scrivono in italiano. */
export const tokens = (n: number) => n.toLocaleString('it-IT')

/** B. La nota dell'approvazione; vuota vale «ok», come il default del relay. */
export const approveText = (note: string) => note.trim() || 'ok'

/** C. Le fasce del contesto in cui la proposta torna dopo essere stata chiusa. */
export const CTX_BANDS = [60, 70, 80]

export function ctxBand(ctx: number | null | undefined): number | null {
  let band: number | null = null
  for (const b of CTX_BANDS) if (ctx != null && ctx >= b) band = b
  return band
}

/** La proposta «handoff, poi /clear» sopra il campo: sessione ferma, contesto oltre il 60 %, non chiusa in questa fascia. */
export function ctxNudge(s: Session, dismissed: number | null): number | null {
  if (s.state !== 'idle') return null
  const band = ctxBand(s.context)
  if (band == null || (dismissed != null && dismissed >= band)) return null
  return band
}

/** Alla master la sua ricorrente `master-handoff`, se c'è; alle altre il prompt di sempre. */
export function handoffPrompt(st: State, s: Session, fallback: string): string {
  if (s.name === MASTER) {
    const r = st.recurring?.find(x => x.id === 'master-handoff')
    if (r) return r.prompt
  }
  return fallback
}

export const canClear = (st: State) => !!st.slash?.includes('clear')

/** /clear a turno finito: la sessione è di nuovo ferma con un esito più nuovo dell'handoff. */
export function clearDue(s: Session | undefined, sentAt: number): boolean {
  return !!s && s.state === 'idle' && (s.outcome?.at ?? 0) >= sentAt
}

/** D. Il testo di una decisione: al massimo 2000 caratteri, come accetta il relay. */
export const DECISION_MAX = 2000

/** Il progetto a cui vale la decisione: l'ultimo pezzo del percorso della sessione. */
export const decisionProject = (s: Session): string | null => s.project.split('/').filter(Boolean).at(-1) ?? null

/** La bozza da una risposta di Claude: senza le righe «Prossimi:» e «Watch:», tagliata a fine parola, senza puntini. */
export function decisionDraft(text: string): string {
  const body = parseSteps(text).text.split('\n').filter(l => !l.trimStart().startsWith('Watch:')).join('\n').trim()
  if (body.length <= DECISION_MAX) return body
  const cut = body.slice(0, DECISION_MAX)
  const space = cut.search(/\s\S*$/)
  return (space > 0 ? cut.slice(0, space) : cut).trimEnd()
}

/** E. La pulizia: compito chiuso o doppione, solo a sessione ferma; «Chiudi» solo senza finestra e con /exit permesso. */
export type Cleanup = { kind: 'finished' | 'duplicate'; of: string | null; canClose: boolean }

/**
 * Prima di chiudere una sessione, cosa sta facendo (Franz, 06/10 10:57 e 12:25): nessuna chiusura senza una domanda col
 * nome, e la domanda dice che la sessione è aperta. `at` è l'ora dell'ultimo esito per chi ha finito il compito.
 */
export type CloseState = { kind: 'working' | 'finished' | 'duplicate' | 'still'; of: string | null; at: number | null }
export function closeStateOf(s: Session): CloseState {
  if (s.state === 'busy' || s.state === 'awaiting' || s.state === 'waiting') return { kind: 'working', of: null, at: null }
  if (s.duplicate_of) return { kind: 'duplicate', of: s.duplicate_of, at: null }
  if (s.finished) return { kind: 'finished', of: null, at: s.outcome?.at ?? null }
  return { kind: 'still', of: null, at: null }
}

/** Chi ha finito il compito si propone di chiudere solo dopo dieci minuti ferma: prima la si può ancora usare (Franz, 06/10 12:40-12:45). */
export const CLEANUP_QUIET_S = 10 * 60

export function cleanupOf(s: Session, canExit: boolean, now: number): Cleanup | null {
  if (s.state !== 'idle') return null
  const canClose = canExit && !s.attached
  if (s.duplicate_of) return { kind: 'duplicate', of: s.duplicate_of, canClose }
  if (s.finished && now - s.since >= CLEANUP_QUIET_S) return { kind: 'finished', of: null, canClose }
  return null
}
