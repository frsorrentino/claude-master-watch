import { describe, expect, it } from 'vitest'
import type { Session } from './contract'
import { of } from './sessionsMenu'

// Gli stessi casi di SessionsMenuTest in Kotlin.
const s = (name: string, st: Session['state'] = 'idle', q = false): Session => ({
  id: name, name, account: 'personale', project: name, state: st, since: 0,
  question: q ? { id: '1', kind: 'ask', text: '?', options: [], tier: 'low', asked_at: 0 } : null,
})

describe('SessionsMenu', () => {
  it('i gruppi in ordine di bisogno, senza la master e le chiuse', () => {
    const m = of([s('idle'), s('busy', 'busy'), s('asks', 'waiting', true), s('master'), s('x', 'gone')])
    expect(m.groups.map(g => g[0])).toEqual(['waiting', 'working', 'still'])
    expect(m.groups.flatMap(g => g[1].map(x => x.name))).toEqual(['asks', 'busy', 'idle'])
  })
  it('le chiuse sono solo contate', () => expect(of([s('a'), s('x', 'gone'), s('y', 'gone')]).closed).toBe(2))
  it('una sessione al lavoro con una domanda aspetta', () => expect(of([s('b', 'busy', true)]).groups.map(g => g[0])).toEqual(['waiting']))
  it('l\'ordine dentro un gruppo resta', () => expect(of([s('b'), s('a')]).groups[0][1].map(x => x.name)).toEqual(['b', 'a']))
})
