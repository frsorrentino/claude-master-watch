import { type Group, type Model, type Row } from './summary'

// La plancia degli schermi larghi, porta di Tablet.kt: la colonna delle sessioni con la master al suo posto, le sessioni
// in colonne affiancate (al massimo quattro), le larghezze a scatti di un dodicesimo.

/** Da qui la plancia: finestra larga del Chromebook, tablet in orizzontale, desktop. */
export const WIDE_PX = 840
export const wide = (width: number) => width >= WIDE_PX

const ORDER: Group[] = ['waiting', 'finished', 'working', 'still']
/** I gruppi del riepilogo nell'ordine del bisogno, con la master come una sessione della lista; fra le ferme la più recente in alto. */
export function groups(summary: Model): [Group, Row[]][] {
  const m = summary.master
  let rows = summary.rows
  if (m && !rows.some(r => r.session.name === m.name)) {
    const group: Group = m.state === 'busy' || m.state === 'awaiting' ? 'working' : 'still'
    rows = [...rows, { group, session: m, text: m.outcome?.full ?? null, at: group === 'working' ? m.turn_started ?? m.since : m.since }]
  }
  return ORDER.map(g => {
    const inGroup = rows.filter(r => r.group === g)
    return [g, g === 'still' ? [...inGroup].sort((a, b) => b.session.since - a.session.since) : inGroup] as [Group, Row[]]
  }).filter(([, r]) => r.length > 0)
}

export const MAX_COLUMNS = 4
/** Le scelte ancora vive, nel loro ordine, fino a quattro; senza una scelta salvata (null) le prime tre. */
export const columns = (pinned: string[] | null, live: string[]) =>
  pinned ? [...new Set(pinned.filter(n => live.includes(n)))].slice(0, MAX_COLUMNS) : live.slice(0, 3)
/** Entra in fondo o esce; con quattro colonne prende il posto dell'ultima. */
export const toggle = (cols: string[], name: string) =>
  cols.includes(name) ? cols.filter(c => c !== name) : cols.length < MAX_COLUMNS ? [...cols, name] : [...cols.slice(0, -1), name]
/** Il clic su una scheda aggiunge; già in colonna niente; con quattro prende il posto dell'ultima. */
export const add = (cols: string[], name: string) =>
  cols.includes(name) ? cols : cols.length < MAX_COLUMNS ? [...cols, name] : [...cols.slice(0, -1), name]
/** Una colonna trascinata sopra un'altra: si scambiano di posto. */
export function swap(cols: string[], from: number, to: number): string[] {
  if (from === to || from < 0 || to < 0 || from >= cols.length || to >= cols.length) return cols
  const out = [...cols]
  out[from] = cols[to]; out[to] = cols[from]
  return out
}
export const columnsPref = (cols: string[]) => cols.join('\n')
export const columnsFromPref = (raw: string | null): string[] | null => (raw == null ? null : raw.split('\n').filter(x => x.trim()))

/** Le larghezze in dodicesimi, ogni colonna almeno due (un sesto). */
export const TOTAL = 12
export const MIN = 2
const equalOf = (total: number, n: number) => Array.from({ length: n }, (_, i) => Math.floor(total / n) + (i < total % n ? 1 : 0))
export const equal = (n: number) => (n <= 0 ? [] : equalOf(TOTAL, n))
/**
 * Un bordo trascinato fa crescere la colonna verso cui si sposta; le altre si dividono in parti uguali il resto, mai
 * sotto MIN (Franz, 05/10 12:30: allargarne una toglieva spazio solo alla vicina).
 */
export function drag(shares: number[], border: number, parts: number): number[] {
  if (border < 0 || border >= shares.length - 1 || parts === 0) return shares
  const grown = parts > 0 ? border : border + 1
  const size = Math.min(shares[grown] + Math.abs(parts), TOTAL - MIN * (shares.length - 1))
  if (size <= shares[grown]) return shares
  const rest = equalOf(TOTAL - size, shares.length - 1)
  let k = 0
  return shares.map((_, i) => (i === grown ? size : rest[k++]))
}
/** Come `kotlin.math.round`: a metà si va al pari. */
function roundEven(x: number): number {
  const r = Math.round(x)
  return Math.abs(x % 1) === 0.5 && r % 2 !== 0 ? r - 1 : r
}
/** I pixel trascinati in parti intere della larghezza delle colonne (lo scatto). */
export const parts = (dragPx: number, widthPx: number) => (widthPx <= 0 ? 0 : roundEven(dragPx / (widthPx / TOTAL)) || 0)
export const sharesPref = (shares: number[]) => shares.join(',')
export function sharesFromPref(raw: string | null, n: number): number[] {
  const s = raw?.split(',').map(x => Number.parseInt(x.trim(), 10)).filter(x => Number.isFinite(x))
  return s && s.length === n && s.reduce((a, b) => a + b, 0) === TOTAL && s.every(p => p >= MIN) ? s : equal(n)
}
