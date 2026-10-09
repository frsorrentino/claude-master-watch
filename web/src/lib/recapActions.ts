import type { State } from './contract'
import { MASTER } from './summary'

// Le Azioni della sezione Recap, come RecapActions.kt (mockup approvato da Franz l'08/10): quelle delle sessioni vive e i
// «prossimo» del recap del giorno, ognuna con a chi va. Un'azione di una sessione va a lei; una del recap va alla sessione
// viva del progetto, se c'è, altrimenti alla master come «Riprendi …». Senza doppioni, al massimo MAX.
export const MAX = 8

/** `text` = quello che si legge, `send` = quello che parte, `to` = chi lo riceve, `from` = da dove viene; `recap` = dal recap. */
export type RecapAction = {
  text: string; send: string; to: string; from: string; viaMaster: boolean; recap: boolean; agenda?: boolean
  /** La riga «Da …» del foglio quando quella predefinita non vale (Approfondisci, il file del rimando). */
  note?: string
}

const key = (x: string) => x.toLowerCase().replace(/[^\p{L}\p{N}]+/gu, ' ').trim()

/** `withSessions` = anche i «Prossimi:» delle sessioni; il Recap no (Franz, 09/10 17:21: senza il loro esito «non sono utili»). */
export function recapActions(st: State, resume: (project: string, next: string) => string = (p, n) => `Riprendi ${p}: ${n}`, withSessions = true): RecapAction[] {
  const live = st.sessions.filter(s => s.state !== 'gone' && s.name !== MASTER)
  const fromSessions = !withSessions ? [] : live.flatMap(s => (s.next_steps ?? []).map(n => ({ text: n.text, send: n.text, to: s.name, from: s.name, viaMaster: false, recap: false })))
  const fromRecap = st.recap.items.flatMap(r => {
    const next = r.next?.trim()
    if (!next) return []
    const s = live.find(x => x.name === r.project || x.project.split('/').pop() === r.project)
    return [s ? { text: next, send: next, to: s.name, from: r.project, viaMaster: false, recap: true }
      : { text: next, send: resume(r.project, next), to: MASTER, from: r.project, viaMaster: true, recap: true }]
  })
  const seen = new Set<string>()
  return [...fromSessions, ...fromRecap].filter(a => { const k = key(a.text); if (seen.has(k)) return false; seen.add(k); return true }).slice(0, MAX)
}
