import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { CmdResult, TranscriptEntry, TranscriptPage } from './contract'
import type { Sent, Status } from './chatRules'
import { anchor, append, arg, counts, group, loading, merge, olderArg, pageEntries, type Item } from './chatFeed'

// Gli stessi casi di ChatFeedTest in Kotlin.
const results: CmdResult[] = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8')).result
const first: TranscriptPage = { entries: [], ...JSON.parse(results[19].text) }
const after: TranscriptPage = { entries: [], ...JSON.parse(results[20].text) }
const sent = (id: string, text: string, sentAt: number, o: Partial<Sent> = {}): Sent => ({ id, session: 'field-notes', text, sentAt, ...o })
const entry = (id: string, role: string, o: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role, ...o })
const mines = (feed: Item[]) => feed.filter((i): i is Extract<Item, { type: 'mine' }> => i.type === 'mine')
const D: Status = 'delivered'

describe('ChatFeed', () => {
  it('parses the fixture page', () => {
    expect(first.entries).toHaveLength(17)
    expect(first.more ?? false).toBe(false)
    expect(first.entries.find(e => e.id === 'a6.0')!.files![0].mime).toBe('image/png')
    expect(first.entries.find(e => e.id === 'a4.0')!.turn!.out).toBe(130)
  })
  it('phone message becomes mine with its status', () => {
    const p1 = first.entries.find(e => e.id === 'p1.0')!
    const feed = merge(first.entries, [[sent('c1', 'Also add the attendees list', p1.at! - 2), 'done']])
    const mine = mines(feed)
    expect(mine).toHaveLength(1)
    expect(mine[0].entry!.id).toBe('p1.0')
    expect(mine[0].status).toBe('done')
    expect(feed.some(i => i.type === 'user' && i.entry.id === 'p1.0')).toBe(false)
  })
  it('text typed at the PC is never mine', () => {
    const feed = merge(first.entries, [[sent('c3', 'Also fix the typo in the title', 1789207318), D]])
    expect(feed.some(i => i.type === 'user' && i.entry.id === 'u2.0')).toBe(true)
    expect(mines(feed)[0].entry).toBeNull()
  })
  it('prefixed user entry still matches', () => {
    const e = entry('u9.0', 'user', { text: "Dall'utente via polso (watch). Chiudi con Watch. run the tests", at: 100 })
    expect(merge([e], [[sent('c9', 'run the tests', 99), D]])[0].type).toBe('mine')
  })
  it('unmatched sent goes by time', () => {
    const feed = merge(first.entries, [[sent('c2', 'one more thing', 1789207400), 'sending']])
    const last = feed[feed.length - 1] as Extract<Item, { type: 'mine' }>
    expect(last.type).toBe('mine')
    expect(last.entry).toBeNull()
    expect(last.sent.id).toBe('c2')
  })
  it('roles map to items', () => {
    const feed = merge(first.entries, [])
    expect(feed[0].type).toBe('user')
    expect(feed.find(i => i.type === 'tool' && i.entry.id === 'a3.0')).toBeDefined()
    expect(feed[feed.length - 1].type).toBe('claude')
  })
  it('after page appends without duplicates', () => {
    expect(append(first.entries.slice(0, 5), after, 'after').map(e => e.id)).toEqual(first.entries.map(e => e.id))
  })
  it('before page prepends', () => {
    const older: TranscriptPage = { entries: [entry('u0.0', 'user', { text: 'earlier', at: 1 })], more: false }
    expect(append(first.entries, older, 'before')[0].id).toBe('u0.0')
  })
  it('arg for the polls', () => {
    expect(arg(null)).toBe('50')
    expect(arg('a5.0')).toBe('50:after=a5.0')
    expect(olderArg('u1.0')).toBe('50:before=u1.0')
  })
  // Revisione 30/09: una risposta corta ripetuta va al messaggio più vicino nel tempo.
  it('nearest message wins', () => {
    const e = entry('u5.0', 'user', { text: 'sì', at: 1000, origin: 'phone' })
    const old: [Sent, Status] = [sent('a', 'sì', 100), D]
    const nw: [Sent, Status] = [sent('b', 'sì', 998), D]
    expect(mines(merge([e], [old, nw])).find(i => i.sent.id === 'b')!.entry?.id).toBe('u5.0')
  })
  it('watch entries are not mine', () => {
    const e = entry('u6.0', 'user', { text: 'status?', at: 1000, origin: 'watch' })
    expect(merge([e], [[sent('c', 'status?', 999), D]]).some(i => i.type === 'user')).toBe(true)
  })
  it('failed messages do not match', () => {
    const e = entry('u7.0', 'user', { text: 'deploy', at: 1000, origin: 'phone' })
    expect(merge([e], [[sent('d', 'deploy', 999, { failed: 'x' }), 'failed']]).some(i => i.type === 'user')).toBe(true)
  })
  it('leftovers older than the page hide while more', () => {
    const e = entry('u8.0', 'user', { text: 'now', at: 1000, origin: 'pc' })
    expect(merge([e], [[sent('e', 'long ago', 10), 'done']], true).some(i => i.type === 'mine')).toBe(false)
  })
  it('queued entry updates in place', () => {
    const q = first.entries.find(e => e.queued)!
    const merged = append(first.entries, { entries: [{ ...q, queued: false }], more: false }, 'after')
    expect(merged).toHaveLength(first.entries.length)
    expect(merged.find(e => e.id === q.id)!.queued).toBe(false)
  })
  it('reads anchor before the first queued', () => {
    const firstQueued = first.entries.findIndex(e => e.queued)
    expect(anchor(first.entries)).toBe(first.entries[firstQueued - 1].id)
    const settled = first.entries.map(e => ({ ...e, queued: false }))
    expect(anchor(settled)).toBe(settled[settled.length - 2].id)
    expect(anchor([])).toBeNull()
  })
  it('matched entry proves delivery', () => {
    const e = entry('p9.0', 'user', { text: 'check the logs', at: 1000, origin: 'phone' })
    expect(mines(merge([e], [[sent('f', 'check the logs', 990), 'uncertain']]))[0].status).toBe('delivered')
  })
  it('scheduled is never matched', () => {
    const p1 = first.entries.find(e => e.id === 'p1.0')!
    const feed = merge(first.entries, [[sent('c1', 'Also add the attendees list', p1.at! - 2, { scheduledFor: p1.at! + 3600 }), 'scheduled']])
    expect(mines(feed)[0].status).toBe('scheduled')
    expect(mines(feed)[0].entry).toBeNull()
  })

  const tool = (id: string, name: string, text: string): Item => ({ type: 'tool', entry: entry(id, 'tool', { tool: name, text, at: 1 }) })
  const claude = (id: string): Item => ({ type: 'claude', entry: entry(id, 'assistant', { text: 'ok', at: 1 }) })
  const steps = (items: Item[]) => group(items).find((i): i is Extract<Item, { type: 'steps' }> => i.type === 'steps')!

  it('consecutive tools become one group', () => {
    const out = group([claude('c1'), tool('t1', 'Bash', 'ls'), tool('t2', 'Read', 'a.md'), tool('t3', 'Bash', 'pwd'), claude('c2')])
    expect(out).toHaveLength(3)
    expect((out[1] as Extract<Item, { type: 'steps' }>).entries.map(e => e.id)).toEqual(['t1', 't2', 't3'])
  })
  it('single tool stays a row', () => {
    expect(group([claude('c1'), tool('t1', 'Bash', 'ls'), claude('c2')])[1].type).toBe('tool')
  })
  it('steps count by tool most first', () => {
    expect(counts(steps([tool('t1', 'Read', 'a'), tool('t2', 'Bash', 'b'), tool('t3', 'Bash', 'c')]))).toEqual([['Bash', 2], ['Read', 1]])
  })
  it('steps count mcp tools by short name', () => {
    expect(counts(steps([tool('t1', 'mcp__chrome-bridge__click', ''), tool('t2', 'mcp__chrome-bridge__click', '')]))).toEqual([['click', 2]])
  })
  it('spinner only until the first answer', () => {
    expect(loading([], false)).toBe(true)
    expect(loading([], true)).toBe(false)
    expect(loading(first.entries, false)).toBe(false)
  })
  it('live page entries only for their own session', () => {
    const a = [entry('a1', 'assistant', { text: 'di A' })]
    const b = [entry('b1', 'assistant', { text: 'di B' })]
    const cache = { B: b }
    expect(pageEntries('B', 'B', 'A', a, cache)).toEqual(b)
    expect(pageEntries('A', 'A', 'A', a, cache)).toEqual(a)
    expect(pageEntries('B', 'A', 'A', a, cache)).toEqual(b)
    expect(pageEntries('C', 'C', 'A', a, cache)).toEqual([])
  })
})
