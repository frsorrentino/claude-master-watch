import { describe, expect, it } from 'vitest'
import { blocks, cards, compact, parse, spoken, type SpanKind, type Table } from './markdown'

// Gli stessi casi di MarkdownTest e MarkdownTableTest in Kotlin.
const styled = (t: string, kind: SpanKind) => { const m = parse(t); return m.spans.filter(s => s.kind === kind).map(s => m.text.slice(s.start, s.end)) }

describe('Markdown', () => {
  it('bold loses its asterisks', () => {
    expect(parse('una **prova** qui').text).toBe('una prova qui')
    expect(styled('una **prova** qui', 'bold')).toEqual(['prova'])
  })
  it('italic and code', () => {
    expect(parse('a *b* `c`').text).toBe('a b c')
    expect(styled('a *b* `c`', 'italic')).toEqual(['b'])
    expect(styled('a *b* `c`', 'code')).toEqual(['c'])
  })
  it('snake case and lone asterisks stay', () => {
    expect(parse('tool_note e 2 * 3 = 6').text).toBe('tool_note e 2 * 3 = 6')
    expect(parse('mcp__chrome-bridge__click').text).toBe('mcp__chrome-bridge__click')
  })
  it('code keeps what is inside', () => {
    const m = parse('`**non grassetto**`')
    expect(m.text).toBe('**non grassetto**')
    expect(m.spans.filter(s => s.kind === 'bold')).toEqual([])
  })
  it('lists and headings', () => {
    const t = '## Titolo\n- **Uno**: primo\n* due'
    expect(parse(t).text).toBe('Titolo\n• Uno: primo\n• due')
    expect(styled(t, 'bold')).toEqual(['Titolo', 'Uno'])
  })
  it('link with text', () => {
    const m = parse('vedi [il mockup](https://claude.ai/artifact/X).')
    expect(m.text).toBe('vedi il mockup.')
    const link = m.spans.filter(s => s.kind === 'link')
    expect(link).toHaveLength(1)
    expect(m.text.slice(link[0].start, link[0].end)).toBe('il mockup')
    expect(link[0].url).toBe('https://claude.ai/artifact/X')
  })
  it('code block fences go', () => {
    const t = 'prima\n```bash\nls -la\n```\ndopo'
    expect(parse(t).text).toBe('prima\nls -la\ndopo')
    expect(styled(t, 'code')).toEqual(['ls -la'])
  })
  it('unclosed markers stay as they are', () => {
    expect(parse('un **solo').text).toBe('un **solo')
  })
})

const numeri = [
  'I numeri', '',
  '| cosa | ora | a mezzogiorno |',
  '|---|---|---|',
  '| post del film (01/10) | 223 visualizzazioni, 4 repost, 0 Mi piace | 121 visualizzazioni, 1 repost |',
  "| 6 risposte di oggi | 78 visualizzazioni in tutto (fra 3 e 28 l'una), 0 Mi piace, 0 risposte | — |",
  '| nuovi follower | 1 | — |', '',
  'Su GitHub nessuna visita arriva da X.',
].join('\n')

describe('MarkdownTable', () => {
  it('a table is its own block between the text', () => {
    const b = blocks(numeri)
    expect(b).toHaveLength(3)
    expect(b[0]).toEqual({ type: 'text', text: 'I numeri' })
    const t = b[1] as Table
    expect(t.header).toEqual(['cosa', 'ora', 'a mezzogiorno'])
    expect(t.rows).toHaveLength(3)
    expect(t.rows[2][1]).toBe('1')
    expect(b[2]).toEqual({ type: 'text', text: 'Su GitHub nessuna visita arriva da X.' })
  })
  it('pipes without the dashes line are text', () => {
    const src = '| a | b |\n| 1 | 2 |'
    expect(blocks(src)).toEqual([{ type: 'text', text: src }])
  })
  it('a narrow table stays a grid, a wide one becomes cards', () => {
    expect(compact({ type: 'table', header: ['file', 'righe'], rows: [['Repo.kt', '336'], ['Slash.kt', '47']] })).toBe(true)
    expect(compact(blocks(numeri)[1] as Table)).toBe(false)
  })
  it('cards one row each without empty cells', () => {
    const c = cards(blocks(numeri)[1] as Table)
    expect(c[0]).toEqual({ title: 'post del film (01/10)', lines: ['ora: 223 visualizzazioni, 4 repost, 0 Mi piace', 'a mezzogiorno: 121 visualizzazioni, 1 repost'] })
    expect(c[2]).toEqual({ title: 'nuovi follower', lines: ['ora: 1'] })
  })
  it('an escaped pipe stays in its cell', () => {
    const t = blocks('| a | b |\n|---|---|\n| x \\| y | z |')[0] as Table
    expect(t.rows[0]).toEqual(['x | y', 'z'])
  })
  it('spoken reads the cards', () => {
    const s = spoken(numeri)
    expect(s).toContain('post del film (01/10). ora: 223 visualizzazioni, 4 repost, 0 Mi piace. a mezzogiorno: 121 visualizzazioni, 1 repost.')
    expect(s).not.toContain('|')
    expect(s).not.toContain('---')
    expect(s.startsWith('I numeri')).toBe(true)
    expect(s.endsWith('arriva da X.')).toBe(true)
  })
})
