import type { AgendaPage, AgendaRow } from './contract'
import { MASTER } from './summary'
import type { RecapAction } from './recapActions'

// Le righe dell'agenda nel Recap, come RecapAgenda.kt (contratto 1.46, mockup approvato l'08/10): le aperte per chi deve
// muoversi — tu, Claude (con «Fallo», che va alla master), altri — e in fondo le altre, con lo stato com'è.
export type Mark = 'personal' | 'agency' | 'desk' | 'none'
export type AgendaModel = { you: AgendaRow[]; claude: AgendaRow[]; other: AgendaRow[]; rest: AgendaRow[] }
export const SCOPES = ['agenzia', 'personale', 'postazione'] as const

const norm = (s: string | null | undefined) => (s ?? '').trim().toLowerCase()
export const mark = (scope: string): Mark => ({ personale: 'personal', agenzia: 'agency', postazione: 'desk' } as Record<string, Mark>)[norm(scope)] ?? 'none'
/** `blocks` è testo libero: «franz» (e «owner» delle fixture) sei tu, «claude» è Claude, il resto è fermo su altro. */
export const isYou = (b: string) => ['franz', 'owner'].includes(norm(b))
export const isClaude = (b: string) => norm(b) === 'claude'
export const isOpen = (r: AgendaRow) => norm(r.state) === 'aperto'

/** `scope` null = tutti gli ambiti; la riga d'intestazione del TSV, se arriva, non è una riga. */
export function agendaModel(page: AgendaPage | null | undefined, scope: string | null = null): AgendaModel {
  const rows = (page?.rows ?? []).filter(r => (r.title ?? '').trim() && norm(r.state) !== 'stato').filter(r => scope == null || norm(r.scope) === norm(scope))
  const open = rows.filter(isOpen)
  return { you: open.filter(r => isYou(r.blocks)), claude: open.filter(r => isClaude(r.blocks)), other: open.filter(r => !isYou(r.blocks) && !isClaude(r.blocks)), rest: rows.filter(r => !isOpen(r)) }
}

/** «Fallo» su un lavoro che può fare Claude: alla master, dal foglio di conferma. */
export function doIt(r: AgendaRow, text: (title: string, ref: string) => string = (t, ref) => `Fai questo lavoro dell'agenda: ${t}${ref ? ` (${ref})` : ''}`): RecapAction {
  return { text: r.title, send: text(r.title, (r.ref ?? '').trim()), to: MASTER, from: 'agenda', viaMaster: true, recap: false, agenda: true }
}
