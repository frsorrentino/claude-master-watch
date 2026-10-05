import type { TranscriptEntry, TranscriptPage } from './contract'
import { SKEW_S, waiting, type Sent, type Status } from './chatRules'
import { short } from './toolText'

// La chat della scheda con la conversazione vera, porta di ChatFeed.kt: le voci di `transcript` più i messaggi mandati
// da qui, che prendono il posto della loro voce e portano il loro stato.
export type Item =
  /** Un messaggio mandato da qui; `entry` = la sua voce nella trascrizione, null finché non c'è. */
  | { type: 'mine'; sent: Sent; status: Status; entry: TranscriptEntry | null }
  | { type: 'user'; entry: TranscriptEntry }
  | { type: 'claude'; entry: TranscriptEntry }
  | { type: 'tool'; entry: TranscriptEntry }
  /** Due o più passaggi di fila fra due messaggi: una card chiusa che si apre al tocco. */
  | { type: 'steps'; entries: TranscriptEntry[] }
export type Page = 'fresh' | 'after' | 'before'

export const PAGE = 50

/** Quanti passaggi per strumento, dal più usato (a pari merito per nome); gli MCP col nome corto. */
export function counts(steps: Extract<Item, { type: 'steps' }>): [string, number][] {
  const m = new Map<string, number>()
  for (const e of steps.entries) { const k = e.tool != null ? short(e.tool) : '?'; m.set(k, (m.get(k) ?? 0) + 1) }
  return [...m].sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))
}

/** I passaggi consecutivi diventano un gruppo; uno solo resta una riga. */
export function group(items: Item[]): Item[] {
  const out: Item[] = []
  let run: TranscriptEntry[] = []
  const flush = () => {
    if (run.length === 1) out.push({ type: 'tool', entry: run[0] })
    else if (run.length > 1) out.push({ type: 'steps', entries: run })
    run = []
  }
  for (const it of items) { if (it.type === 'tool') run.push(it.entry); else { flush(); out.push(it) } }
  flush()
  return out
}

export const arg = (lastId: string | null): string => lastId == null ? `${PAGE}` : `${PAGE}:after=${lastId}`
export const olderArg = (firstId: string): string => `${PAGE}:before=${firstId}`

/**
 * Una pagina nuova sulla lista che c'è: `after` in coda, `before` in testa, `fresh` al posto. Una voce già presente si
 * sostituisce con la sua versione nuova: mai due voci con lo stesso id.
 */
export function append(old: TranscriptEntry[], page: TranscriptPage, mode: Page): TranscriptEntry[] {
  const byId = new Map(page.entries.map(e => [e.id, e]))
  const updated = old.map(e => byId.get(e.id) ?? e)
  const seen = new Set(old.map(e => e.id))
  const fresh = page.entries.filter(e => !seen.has(e.id))
  return mode === 'fresh' ? page.entries : mode === 'after' ? [...updated, ...fresh] : [...fresh, ...updated]
}

/** La rotella solo fino alla prima risposta del relay: una conversazione vuota è una risposta. */
export const loading = (entries: TranscriptEntry[], answered: boolean): boolean => entries.length === 0 && !answered

/** La conversazione di una pagina dello scorrimento: quella dal vivo solo se è la sessione aperta e le voci sono sue; se no l'ultima copia letta. */
export const pageEntries = (name: string, open: string | null, owner: string | null, live: TranscriptEntry[], cache: Record<string, TranscriptEntry[]>): TranscriptEntry[] =>
  name === open && owner === name ? live : cache[name] ?? []

/** Da dove ripartire con `after`: prima della prima voce in coda, se no dalla penultima. Null = prima lettura. */
export function anchor(entries: TranscriptEntry[]): string | null {
  if (entries.length === 0) return null
  const q = entries.findIndex(e => e.queued)
  const i = q >= 0 ? q - 1 : entries.length - 2
  return i < 0 ? entries[0].id : entries[i].id
}

const atOf = (i: Item): number =>
  i.type === 'mine' ? i.entry?.at ?? i.sent.sentAt : i.type === 'steps' ? i.entries[0].at ?? 0 : i.entry.at ?? 0

export function merge(entries: TranscriptEntry[], sent: [Sent, Status][], more = false): Item[] {
  const left = [...sent].sort((a, b) => a[0].sentAt - b[0].sentAt)
  const items: Item[] = entries.map(e => {
    if (e.role === 'user') {
      // Il relay antepone al prompt le sue istruzioni: basta che la voce finisca con il testo mandato.
      const t = (e.text ?? '').trim()
      const at = e.at ?? Number.MAX_SAFE_INTEGER
      // Solo le voci mandate da qui (web, e remote da un relay prima della 1.36), dal telefono o da un relay che non lo dice;
      // mai i messaggi falliti; fra più candidati il più vicino nel tempo.
      let hit: [Sent, Status] | null = null
      if (e.origin == null || e.origin === 'phone' || e.origin === 'web' || e.origin === 'remote') {
        for (const c of left) {
          const [m, st] = c
          if (st !== 'failed' && m.failed == null && !waiting(m) && m.text.trim() !== '' && t.endsWith(m.text.trim()) && at >= m.sentAt - SKEW_S &&
            (hit == null || Math.abs(at - m.sentAt) < Math.abs(at - hit[0].sentAt))) hit = c
        }
      }
      // La voce nella trascrizione prova che il messaggio è arrivato, anche senza il risultato del comando.
      if (hit) {
        left.splice(left.indexOf(hit), 1)
        const st: Status = hit[1] === 'uncertain' || hit[1] === 'sent' || hit[1] === 'sending' ? 'delivered' : hit[1]
        return { type: 'mine', sent: hit[0], status: st, entry: e }
      }
      return { type: 'user', entry: e }
    }
    return e.role === 'tool' ? { type: 'tool', entry: e } : { type: 'claude', entry: e }
  })
  // I messaggi non ancora nella trascrizione, al loro orario; con pagine più vecchie da caricare quelli più vecchi della pagina aspettano la loro.
  const first = entries[0]?.at
  for (const [m, st] of left.filter(([m]) => !(more && first != null && m.sentAt < first))) {
    const at = items.findIndex(i => atOf(i) > m.sentAt)
    const item: Item = { type: 'mine', sent: m, status: st, entry: null }
    if (at < 0) items.push(item); else items.splice(at, 0, item)
  }
  return items
}
