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
export const isYou = (b: string, owner: string | null = null) => (owner ? norm(b) === norm(owner) : ['franz', 'owner'].includes(norm(b)))
export const isClaude = (b: string) => norm(b) === 'claude'
/** Aperta: `aperto`, o `sospeso` con `until` arrivato (contratto 1.47: la scheda rimandata torna quel giorno). `today` = AAAA-MM-GG. */
export function isOpen(r: AgendaRow, today: string | null = null): boolean {
  const st = norm(r.state)
  if (st === 'aperto') return true
  return st === 'sospeso' && !!today && /^\d{4}-\d{2}-\d{2}$/.test((r.until ?? '').trim()) && (r.until ?? '').trim() <= today
}

/** `scope` null = tutti gli ambiti; l'intestazione del TSV non è una riga; le `scartato` restano nel file ma qui non si vedono. */
export function agendaModel(page: AgendaPage | null | undefined, scope: string | null = null, today: string | null = null): AgendaModel {
  const rows = (page?.rows ?? []).filter(r => (r.title ?? '').trim() && !['stato', 'scartato'].includes(norm(r.state))).filter(r => scope == null || norm(r.scope) === norm(scope))
  const open = rows.filter(r => isOpen(r, today))
  const owner = page?.owner ?? null
  return { you: open.filter(r => isYou(r.blocks, owner)), claude: open.filter(r => isClaude(r.blocks)), other: open.filter(r => !isYou(r.blocks, owner) && !isClaude(r.blocks)), rest: rows.filter(r => !isOpen(r, today)) }
}

/**
 * Le voci del menu «Azioni» di una scheda, nell'ordine del menu (piano approvato il 09/10), come RecapAgenda.menu;
 * «Stanotte» dopo «Fallo» solo con un relay che mette la master nella notte (`canNight`, contratto 1.49).
 */
export type Item = 'deepen' | 'do' | 'night' | 'talk' | 'open_ref' | 'done' | 'postpone' | 'pass_claude' | 'pass_me' | 'remove'
export function menu(r: AgendaRow, canWrite: boolean, today: string | null = null, canNight = false): Item[] {
  const open = isOpen(r, today) || norm(r.state) === 'sospeso'
  const out: Item[] = ['deepen']
  if (open) out.push('do')
  if (open && canNight) out.push('night')
  out.push('talk')
  if ((r.ref ?? '').trim()) out.push('open_ref')
  // Senza key (relay 1.46) non si scrive: il relay non saprebbe quale riga.
  const write = canWrite && !!(r.key ?? '').trim()
  if (write && open) out.push('done', 'postpone', isClaude(r.blocks) ? 'pass_me' : 'pass_claude')
  if (write) out.push('remove')
  return out
}

/** Una scrittura nell'agenda chiesta dal menu: la fa il relay con l'op del contratto 1.47. «Stanotte» ha la stessa conferma. */
export type Edit = { item: 'done' | 'postpone' | 'remove' | 'pass_claude' | 'pass_me' | 'night'; row: AgendaRow; until?: string }

/** Contratto 1.49: `night_add` con questo `arg` mette il lavoro stanotte nella cartella della master. */
export const NIGHT_TARGET = 'master'

/**
 * Le schede da spuntare nel foglio «Aggiungi alla notte» (Franz, 09/10 20:42; scelta A delle 21:27), come RecapAgenda.night:
 * le aperte, prima quelle che può fare Claude e poi le tue; quelle ferme su altri no.
 */
export function night(page: AgendaPage | null | undefined, today: string | null = null): AgendaRow[] {
  const m = agendaModel(page, null, today)
  return [...m.claude, ...m.you]
}

/** Il comando `agenda_set` di una scrittura (contratto 1.47); null senza key. `owner` = tu nell'agenda. */
export function setCmd(e: Edit, owner: string | null): { key: string; action: string; until?: string; blocks?: string } | null {
  const key = (e.row.key ?? '').trim()
  if (!key || e.item === 'night') return null
  if (e.item === 'done') return { key, action: 'done' }
  if (e.item === 'postpone') return e.until ? { key, action: 'snooze', until: e.until } : null
  if (e.item === 'remove') return { key, action: 'remove' }
  return { key, action: 'pass', blocks: e.item === 'pass_claude' ? 'claude' : owner ?? 'franz' }
}

const withRef = (r: AgendaRow, base: string) => ((r.ref ?? '').trim() ? `${base} (${r.ref.trim()})` : base)
export const deepenText = (r: AgendaRow) => withRef(r, `Approfondisci: ${r.title}`)
export const talkText = (r: AgendaRow) => withRef(r, `Sulla scheda «${r.title}»`) + ': '
export const fileText = (r: AgendaRow) => `Mandami il file «${r.ref.trim()}» della scheda «${r.title}»`

const TLD = new Set(['com', 'it', 'net', 'org', 'io', 'dev', 'app', 'eu', 'ai', 'co'])
/** Il rimando come indirizzo da aprire, o null se è un file o un nome (allora lo manda la master). Vale il primo pezzo. */
export function url(ref: string): string | null {
  const first = ref.trim().split(' ')[0]
  if (first.startsWith('https://') || first.startsWith('http://')) return first
  const host = first.split('/')[0]
  return /^[a-z0-9-]+(\.[a-z0-9-]+)+$/i.test(host) && TLD.has(host.split('.').pop()!.toLowerCase()) ? `https://${first}` : null
}

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
const day = (s: string) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d) }
/** Rimanda: domani, o il lunedì della settimana dopo (date AAAA-MM-GG). */
export const tomorrow = (today: string) => { const d = day(today); d.setDate(d.getDate() + 1); return iso(d) }
export const nextWeek = (today: string) => { const d = day(today); d.setDate(d.getDate() + (((8 - d.getDay()) % 7) || 7)); return iso(d) }
export const todayIso = () => iso(new Date())

/** «Fallo» su un lavoro che può fare Claude: alla master, dal foglio di conferma. */
export function doIt(r: AgendaRow, text: (title: string, ref: string) => string = (t, ref) => `Fai questo lavoro dell'agenda: ${t}${ref ? ` (${ref})` : ''}`): RecapAction {
  return { text: r.title, send: text(r.title, (r.ref ?? '').trim()), to: MASTER, from: 'agenda', viaMaster: true, recap: false, agenda: true }
}
