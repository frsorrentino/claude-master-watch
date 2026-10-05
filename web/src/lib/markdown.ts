// La formattazione dei messaggi della chat, porta di Markdown.kt e MarkdownTable.kt.

// --- Markdown: il testo senza i segni e gli intervalli da disegnare ([start, end) come `start until end`) ---
export type SpanKind = 'bold' | 'italic' | 'code' | 'link'
export type Span = { kind: SpanKind; start: number; end: number; url?: string }
export type Styled = { text: string; spans: Span[] }

const LIST = /^(\s*)[-*]\s+/
const HEADING = /^#{1,6}\s+/
const LINK = /^\[([^\]\n]+)\]\((https?:\/\/[^)\s]+)\)/
const WS = /\s/
const ALNUM = /[\p{L}\p{N}]/u

/** `__` vale grassetto solo fuori da una parola: mcp__chrome-bridge__click resta com'è. */
const boundary = (s: string, at: number) => at < 0 || at >= s.length || !ALNUM.test(s[at])

export function parse(src: string): Styled {
  let out = ''
  const spans: Span[] = []
  const lines = src.split('\n')
  let i = 0
  let first = true
  const newline = () => { if (!first) out += '\n'; first = false }
  while (i < lines.length) {
    const line = lines[i]
    if (line.trimStart().startsWith('```')) {
      let end = -1
      for (let k = i + 1; k < lines.length; k++) if (lines[k].trimStart().startsWith('```')) { end = k; break }
      if (end >= 0) {
        const body = lines.slice(i + 1, end).join('\n')
        newline()
        const start = out.length
        out += body
        if (body.length > 0) spans.push({ kind: 'code', start, end: out.length })
        i = end + 1
        continue
      }
    }
    newline()
    let rest = line
    const heading = HEADING.exec(rest)
    if (heading) rest = rest.slice(heading[0].length)
    const list = LIST.exec(rest)
    if (list) { out += list[1] + '• '; rest = rest.slice(list[0].length) }
    const start = out.length
    out = inline(rest, out, spans)
    if (heading && out.length > start) spans.push({ kind: 'bold', start, end: out.length })
    i++
  }
  return { text: out, spans }
}

function inline(s: string, out: string, spans: Span[]): string {
  let i = 0
  while (i < s.length) {
    const c = s[i]
    if (c === '`') {
      const end = s.indexOf('`', i + 1)
      if (end > i + 1) { const start = out.length; out += s.slice(i + 1, end); spans.push({ kind: 'code', start, end: out.length }); i = end + 1; continue }
    }
    if (c === '[') {
      const m = LINK.exec(s.slice(i))
      if (m) {
        const start = out.length
        out = inline(m[1], out, spans)
        spans.push({ kind: 'link', start, end: out.length, url: m[2] })
        i += m[0].length
        continue
      }
    }
    if (s.startsWith('**', i) || (s.startsWith('__', i) && boundary(s, i - 1))) {
      const mark = s.slice(i, i + 2)
      const end = s.indexOf(mark, i + 2)
      if (end > i + 2 && !WS.test(s[i + 2]) && (mark === '**' || boundary(s, end + 2))) {
        const start = out.length
        out = inline(s.slice(i + 2, end), out, spans)
        spans.push({ kind: 'bold', start, end: out.length })
        i = end + 2
        continue
      }
    }
    if (c === '*' && i + 1 < s.length && !WS.test(s[i + 1]) && s[i + 1] !== '*') {
      const end = s.indexOf('*', i + 1)
      if (end > i + 1 && !WS.test(s[end - 1])) {
        const start = out.length
        out = inline(s.slice(i + 1, end), out, spans)
        spans.push({ kind: 'italic', start, end: out.length })
        i = end + 1
        continue
      }
    }
    out += c
    i++
  }
  return out
}

// --- Tabelle: griglia se stretta, una scheda per riga se larga ---
export type Block = { type: 'text'; text: string } | { type: 'table'; header: string[]; rows: string[][] }
export type Table = Extract<Block, { type: 'table' }>
export type Card = { title: string; lines: string[] }

/** Una griglia regge al massimo tre colonne di celle corte su un telefono. */
export const COMPACT_COLS = 3
export const COMPACT_CELL = 18

const SEPARATOR = /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)*\|?\s*$/
const PIPE = /(?<!\\)\|/
const EMPTY = new Set(['', '—', '–', '-'])

const isRow = (line: string) => { const t = line.trim(); return t.startsWith('|') && t.split('|').length - 1 >= 2 }

function cells(line: string): string[] {
  let t = line.trim()
  if (t.startsWith('|')) t = t.slice(1)
  if (t.endsWith('|')) t = t.slice(0, -1)
  return t.split(PIPE).map(c => c.trim().replaceAll('\\|', '|'))
}

export function blocks(src: string): Block[] {
  const lines = src.split('\n')
  const out: Block[] = []
  let text: string[] = []
  const flush = () => {
    const t = text.join('\n').replace(/^\n+|\n+$/g, '')
    if (t.trim() !== '') out.push({ type: 'text', text: t })
    text = []
  }
  let i = 0
  while (i < lines.length) {
    if (isRow(lines[i]) && i + 1 < lines.length && SEPARATOR.test(lines[i + 1])) {
      const header = cells(lines[i])
      const rows: string[][] = []
      let j = i + 2
      while (j < lines.length && isRow(lines[j])) {
        const r = cells(lines[j])
        rows.push(header.map((_, k) => r[k] ?? ''))
        j++
      }
      flush()
      out.push({ type: 'table', header, rows })
      i = j
    } else {
      text.push(lines[i])
      i++
    }
  }
  flush()
  return out
}

export const compact = (t: Table): boolean =>
  t.header.length <= COMPACT_COLS && [t.header, ...t.rows].every(r => r.every(c => c.length <= COMPACT_CELL))

export const cards = (t: Table): Card[] => t.rows.map(r => ({
  title: r[0] ?? '',
  lines: t.header.slice(1).flatMap((h, k) => { const v = r[k + 1]; return v !== undefined && !EMPTY.has(v) ? [`${h}: ${v}`] : [] }),
}))

/** Il testo da leggere a voce: le tabelle come le loro schede, una riga per riga. */
export const spoken = (src: string): string => blocks(src).map(b =>
  b.type === 'text' ? b.text : cards(b).map(c => [c.title, ...c.lines].filter(x => x.trim() !== '').join('. ') + '.').join('\n'),
).join('\n\n')
