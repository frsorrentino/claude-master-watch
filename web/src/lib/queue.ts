import type { State } from './contract'

// La coda «Ti aspettano», porta di AttentionQueue.kt: le domande aperte di tutte le sessioni, da rispondere in fila.
export type QueueItem = { session: string; questionId: string; askedAt: number }

/** Le domande aperte, dalla più vecchia. */
export const items = (state: State): QueueItem[] =>
  state.sessions.flatMap(s => (s.question ? [{ session: s.name, questionId: s.question.id, askedAt: s.question.asked_at }] : []))
    .sort((a, b) => a.askedAt - b.askedAt)

/** La domanda dopo `current`; dopo l'ultima, o se `current` non c'è più, si riparte dalla più vecchia. */
export function next(state: State, current: string | null): QueueItem | null {
  const all = items(state)
  const i = all.findIndex(x => x.questionId === current)
  return i < 0 ? all[0] ?? null : all[i + 1] ?? all[0] ?? null
}

/** La pagina della domanda `questionId` nella fila di adesso; null se non c'è più. */
export function page(list: QueueItem[], questionId: string | null): number | null {
  const i = list.findIndex(x => x.questionId === questionId)
  return i >= 0 ? i : null
}
