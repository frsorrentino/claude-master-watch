import type { Event } from './contract'

// Il Registro e il diario, porte di RecapSessions.kt, Registro.kt e PhoneDiary.kt.
const LINES = /\r\n|\r|\n/

// --- RecapSessions: il recap della giornata letto riga per riga (formato `render_short` del plugin e vecchio inglese).
export type RecapKind = 'waiting' | 'open' | 'closed' | 'other'
export type RecapEntry = { icon: string; name: string; url: string | null; text: string | null; tool: string | null; next: string | null; detail: string | null }
export type RecapSection = { kind: RecapKind; title: string; entries: RecapEntry[]; lines: string[] }
export type RecapView = { title: string; sections: RecapSection[] }

const HEADERS: Record<string, RecapKind> = {
  'FERME SU UNA DOMANDA': 'waiting', 'WAITING ON A QUESTION': 'waiting',
  'APERTE': 'open', 'OPEN': 'open',
  'CHIUSE OGGI': 'closed', 'CHIUSI OGGI': 'closed', 'CLOSED TODAY': 'closed',
}
const ENTRY = /^(\S+)\s+(.+)$/
const URL_ = /\s*\((https?:\/\/[^)\s]+)\)/
const OTHER_LINE = /^(altro|other)\s*:/i
const NEXT = /^↳\s*(?:prossimo|next)\s*:\s*/i
const SQUARES = new Set(['🟥', '🟧', '🟨', '🟩', '🟦', '🟪', '🟫', '⬛', '⬜', '◼', '◻', '▪', '▫'])

/** Le emoji tonde sono dell'account personale, le quadrate di quello di lavoro. */
export const personalIcon = (icon: string): boolean => !SQUARES.has(icon.trim())

const isEntry = (t: string): boolean => {
  const m = ENTRY.exec(t)
  return m != null && !/[\p{L}\p{N}]/u.test(m[1])
}
const isHeader = (t: string) => t.length > 2 && t === t.toUpperCase() && /\p{L}/u.test(t) && !isEntry(t)

function entry(t: string): RecapEntry {
  const m = ENTRY.exec(t)!
  const icon = m[1]
  const url = URL_.exec(m[2])?.[1] ?? null
  const rest = m[2].replace(new RegExp(URL_.source, 'g'), '')
  const colon = rest.indexOf(': ')
  const dot = rest.indexOf(' · ')
  const base = { icon, url, text: null, tool: null, next: null, detail: null }
  if (dot >= 0 && (colon < 0 || dot < colon)) return { ...base, name: rest.slice(0, dot).trim(), tool: rest.slice(dot + 3).trim() }
  if (colon >= 0) return { ...base, name: rest.slice(0, colon).trim(), text: rest.slice(colon + 2).trim() }
  return { ...base, name: rest.trim() }
}

export function parseRecap(body: string): RecapView {
  const lines = body.split(LINES)
  const title = lines.find(l => l.trim())?.trim() ?? ''
  const sections: RecapSection[] = []
  let kind: RecapKind = 'other', header = ''
  let entries: RecapEntry[] = [], other: string[] = []
  const close = () => { if (entries.length || other.length) sections.push({ kind, title: header, entries, lines: other }) }
  const open = (k: RecapKind, h: string) => { close(); kind = k; header = h; entries = []; other = [] }
  for (const raw of lines.slice(lines.findIndex(l => l.trim()) + 1)) {
    const line = raw.replace(/⁠/g, '')
    const t = line.trim()
    if (!t) continue
    if (HEADERS[t] != null || isHeader(t)) open(HEADERS[t] ?? 'other', t)
    // Una riga rientrata appartiene alla sessione sopra: il prossimo passo o la domanda.
    else if (line.startsWith(' ') && entries.length) {
      const last = entries[entries.length - 1]
      entries[entries.length - 1] = NEXT.test(t) ? { ...last, next: t.replace(NEXT, '') } : { ...last, detail: [last.detail, t].filter(x => x != null).join('\n') }
    } else if (kind !== 'other' && isEntry(t)) entries.push(entry(t))
    // «altro: a, b» (le chiuse senza sostanza) resta nella sua sezione.
    else if (kind !== 'other' && OTHER_LINE.test(t)) other.push(t)
    else {
      // Il resto (totali, «Tra le sessioni») fa una sezione di righe sole, in fondo.
      if (kind !== 'other') open('other', '')
      other.push(t)
    }
  }
  close()
  return { title, sections }
}

// --- Registro: il resoconto della notte e i diari come righe, una per lavoro o per progetto, senza percorsi.
export type Job = { ok: boolean; project: string; seconds: number | null; text: string }
export type Line = { project: string; text: string }

// «✓ atlas-shop (personal, 812 s, rc=0): Fixed the three flaky tests.»
const JOB = /^\s*([✓✗])\s+(\S+)\s+\((?:[^,()]*,\s*)?(\d+)\s*s(?:,[^)]*)?\):\s*(.*)$/
// «● atlas-shop · 6 turns · last 13:45», poi il testo rientrato sulla riga sotto.
const PROJECT = /^[^\p{L}\p{N}\s]+\s+([\p{L}\p{N}._-]+)(?:\s+·.*)?$/u

export function night(body: string): Job[] {
  return body.split(LINES).flatMap(l => {
    const m = JOB.exec(l)
    if (!m) return []
    const n = Number(m[3])
    return [{ ok: m[1] === '✓', project: m[2], seconds: n <= 2147483647 ? n : null, text: m[4].trim() }]
  })
}

export function recap(body: string): Line[] {
  const lines = body.split(LINES)
  return lines.flatMap((l, i) => {
    const m = PROJECT.exec(l)
    if (!m) return []
    const next = lines[i + 1]
    return [{ project: m[1], text: next?.startsWith(' ') ? next.trim() : '' }]
  })
}

/** I minuti di un lavoro, per eccesso: 812 s sono 14 minuti, non 13. */
export const minutes = (seconds: number): number => Math.floor((seconds + 59) / 60)

// --- PhoneDiary: un diario per giorno (l'ultimo invio vince), il resoconto più recente.
export function recaps(events: Event[]): Event[] {
  const days = new Map<string, Event>()
  for (const e of events) {
    if (e.kind !== 'recap' || e.ref == null) continue
    const best = days.get(e.ref)
    if (!best || e.ts > best.ts) days.set(e.ref, e)
  }
  return [...days.values()].sort((a, b) => (a.ref! < b.ref! ? 1 : a.ref! > b.ref! ? -1 : 0))
}

export function lastNightReport(events: Event[]): Event | null {
  return events.filter(e => e.kind === 'night_report').reduce<Event | null>((a, e) => (a == null || e.ts > a.ts ? e : a), null)
}
