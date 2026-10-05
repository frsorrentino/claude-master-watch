import type { Session, TimelineEvent, TimelinePage } from './contract'
import { type Group, type Model, type Row } from './summary'
import { WINDOW_S, type Pace } from './quotaHistory'

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

/** Mezzanotte locale del giorno di `now`, nel fuso dato (quello del browser se manca). */
function midnight(now: number, timeZone?: string): number {
  const day = new Intl.DateTimeFormat('en-CA', { timeZone }).format(new Date(now * 1000))
  // L'istante di mezzanotte di quel giorno: si parte da mezzanotte UTC e si corregge dello scarto del fuso, due volte per
  // reggere il cambio dell'ora legale.
  let t = Date.parse(`${day}T00:00:00Z`) / 1000
  for (let i = 0; i < 2; i++) {
    const p = Object.fromEntries(new Intl.DateTimeFormat('en-GB', { timeZone, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
      .formatToParts(new Date(t * 1000)).map(x => [x.type, x.value]))
    const local = Date.parse(`${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}:00Z`) / 1000
    t -= local - Date.parse(`${day}T00:00:00Z`) / 1000
  }
  return t
}

/**
 * L'ispettore a destra: aperta da, da quanto, il turno in corso, e la giornata dalla cronologia (contratto 1.29) della
 * sessione viva. `commits` e `prompts` null finché la cronologia non c'è: un numero inventato direbbe zero.
 */
export type Inspector = { session: Session; openedAt: number; openFor: number; turn: number | null; today: TimelineEvent[]; commits: number | null; prompts: number | null }
export function inspect(session: Session, page: TimelinePage | null, now: number, timeZone?: string): Inspector {
  const from = midnight(now, timeZone)
  const mine = page?.sessions.filter(x => x.session === session.name && x.live) ?? null
  const today = (mine ?? []).flatMap(x => x.events).filter(e => e.at >= from).sort((a, b) => a.at - b.at)
  const working = session.state === 'busy' || session.state === 'awaiting'
  const turn = working && session.turn_started != null ? Math.max(0, now - session.turn_started) : null
  return {
    session, openedAt: session.since, openFor: Math.max(0, now - session.since), turn, today,
    commits: mine ? today.filter(e => e.kind === 'commit').length : null, prompts: mine ? today.filter(e => e.kind === 'prompt').length : null,
  }
}
/** L'argomento di `timeline`: da mezzanotte di oggi, come epoch. */
export const timelineArg = (now: number, timeZone?: string) => String(midnight(now, timeZone))

/** Il grafico della quota 5h: la finestra da 5 ore prima della ripartenza alla ripartenza, x da 0 a 1. */
export type Forecast = { start: number; resetAt: number; points: [number, number][]; nowX: number; projected: number | null }
export function forecast(p: Pace, resetAt: number, now: number): Forecast {
  const start = resetAt - WINDOW_S
  const x = (ts: number) => Math.min(1, Math.max(0, (ts - start) / WINDOW_S))
  return { start, resetAt, points: p.points.map(s => [x(s.ts), s.pct]), nowX: x(now), projected: p.projected }
}

const WEEK_S = 7 * 86400
/** La settimana: il ritmo medio da quando è ripartita, esteso al rinnovo, al massimo 100; nella prima ora non si dice. */
export function weekProjected(w7: number | null | undefined, resetAt: number | null | undefined, now: number): number | null {
  if (w7 == null || resetAt == null || resetAt <= now) return null
  const elapsed = now - (resetAt - WEEK_S)
  if (elapsed < 3600) return null
  return Math.min(100, Math.max(0, Math.trunc(w7 + (w7 / elapsed) * (resetAt - now))))
}
