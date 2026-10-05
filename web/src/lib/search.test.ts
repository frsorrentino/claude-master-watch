import { describe, expect, it } from 'vitest'
import type { Event, SearchPage } from './contract'
import type { Sent } from './chatRules'
import { find, withConversations, type Hit } from './search'

// Gli stessi casi di ChatSearchTest in Kotlin.
const sent: Sent[] = [
  { id: 'a', session: 'kb', text: 'Perché i test falliscono?', sentAt: 100, outcomeFull: 'Mancava la fixture.\nOra i test passano.', doneAt: 160 },
  { id: 'b', session: 'atlas', text: 'deploy staging', sentAt: 300 },
]
const events: Event[] = [
  { key: 'e1', kind: 'outcome', session: 'kb', ts: 200, title: 'kb', body: 'Migrazione del database finita' },
  { key: 'e2', kind: 'recap', ts: 400, title: 'Diario', body: 'Oggi: deploy di atlas e test di kb' },
]

describe('ChatSearch', () => {
  it('trova nei mandati e negli esiti', () => {
    const hits = find('fixture', sent, [])
    expect(hits).toHaveLength(1)
    expect([hits[0].session, hits[0].line, hits[0].kind]).toEqual(['kb', 'Mancava la fixture.', 'outcome'])
    expect(hits[0].line.slice(hits[0].start, hits[0].end)).toBe('fixture')
    expect(find('staging', sent, [])[0].kind).toBe('sent')
  })

  it('trova negli eventi del diario', () => {
    const hits = find('database', sent, events)
    expect(hits).toHaveLength(1); expect(hits[0].kind).toBe('event'); expect(hits[0].at).toBe(200)
  })

  it('accenti e maiuscole ignorati, grassetto sul testo originale', () => {
    const h = find('PERCHE', sent, events)[0]
    expect(h.line.slice(h.start, h.end)).toBe('Perché')
  })

  it('dal più recente', () => expect(find('deploy', sent, events).map(h => h.at)).toEqual([400, 300]))
  it('il vuoto non trova nulla', () => expect(find('  ', sent, events)).toEqual([]))

  it('la stessa riga due volte ha ref diversi', () => {
    const twice: Sent[] = [{ id: 'x1', session: 'kb', text: 'continua', sentAt: 500 }, { id: 'x2', session: 'kb', text: 'continua', sentAt: 500 }]
    expect(new Set(find('continua', twice, []).map(h => h.ref)).size).toBe(2)
  })

  // Contratto 1.27: le conversazioni prendono il posto dei messaggi locali; restano le voci del registro senza sessione.
  it('le conversazioni sostituiscono i messaggi locali', () => {
    const local: Hit[] = [
      { session: 'kb', at: 300, line: 'deploy di kb', start: 0, end: 6, kind: 'sent', ref: 'SENT-1' },
      { session: 'kb', at: 200, line: 'deploy finito', start: 0, end: 6, kind: 'event', ref: 'EVENT-e1' },
      { session: null, at: 400, line: 'Oggi: deploy di atlas', start: 6, end: 12, kind: 'event', ref: 'EVENT-e2' },
    ]
    const page: SearchPage = { hits: [
      { session: 'atlas', live: true, entry: 'a1.0', at: 500, snippet: 'il deploy è partito', match: [3, 9] },
      { session: 'old', live: false, entry: 'o1.0', at: 100, snippet: 'deploy', match: [0, 6] },
    ] }
    const hits = withConversations(local, page)
    expect(hits.map(h => h.session)).toEqual(['atlas', null, 'old'])
    expect(hits.map(h => h.kind)).toEqual(['conversation', 'event', 'conversation'])
    expect(hits.map(h => h.live ?? null)).toEqual([true, null, false])
    expect(hits[0].line.slice(hits[0].start, hits[0].end)).toBe('deploy')
  })

  it('una corrispondenza fuori dal testo si accorcia', () => {
    const h = withConversations([], { hits: [{ session: 'a', live: true, at: 1, snippet: 'abc', match: [2, 9] }] })[0]
    expect([h.start, h.end]).toEqual([2, 3])
  })
})
