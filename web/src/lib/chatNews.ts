import type { Item } from './chatFeed'

// L'apertura di una sessione, come ChatNews sul telefono (Franz, 10/10 16:01: «per il testo della conversazione va bene
// a+b»): le ultime sei voci entrano a cascata dall'alto (A); quelle arrivate dall'ultima visita sono «Nuovi», con una riga
// sopra la prima e un fondo che sfuma (B). L'ultima visita sta nel browser, una per sessione.
export const CASCADE = 6

/** Il momento di una voce: la trascrizione, o l'invio per un messaggio non ancora trascritto. */
export function at(it: Item): number | null {
  if (it.type === 'mine') return it.entry?.at ?? it.sent.sentAt
  if (it.type === 'steps') return it.entries.reduce<number | null>((m, e) => (e.at != null && (m == null || e.at > m) ? e.at : m), null)
  return it.entry.at ?? null
}

/** L'ultimo momento della conversazione, da ricordare lasciando la chat. */
export function latest(items: Item[]): number | null {
  return items.reduce<number | null>((m, it) => { const a = at(it); return a != null && (m == null || a > m) ? a : m }, null)
}

/** L'indice della prima voce nuova, non tua; null senza novità o se la sessione non era mai stata aperta. */
export function firstNew(items: Item[], seenAt: number | null): number | null {
  if (seenAt == null) return null
  const i = items.findIndex(it => it.type !== 'mine' && (at(it) ?? 0) > seenAt)
  return i < 0 ? null : i
}

/** Il posto nella cascata, dall'alto, delle ultime `CASCADE` voci; null per le altre. */
export function rank(i: number, n: number): number | null {
  const r = i - Math.max(0, n - CASCADE)
  return r >= 0 && i < n ? r : null
}

const KEY = 'chat_seen'
function seenMap(): Record<string, number> {
  try { return JSON.parse(localStorage.getItem(KEY) ?? '{}') ?? {} } catch { return {} }
}
export function readSeen(session: string): number | null {
  const v = seenMap()[session]
  return typeof v === 'number' ? v : null
}
export function saveSeen(session: string, when: number | null) {
  if (when == null) return
  try { localStorage.setItem(KEY, JSON.stringify({ ...seenMap(), [session]: when })) } catch { /* il browser non salva: pazienza */ }
}
