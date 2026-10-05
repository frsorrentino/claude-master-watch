import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Session } from './contract'
import { freshness, orderSessions, since } from './durations'

// Gli stessi casi di OrderTest in Kotlin.
const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const NB = ' '
describe('ordine, freschezza, durate', () => {
  it('waiting, busy, idle, gone', () => {
    const shuffled = [...st.sessions].reverse()
    expect(orderSessions(shuffled).map(s => s.name)).toEqual(['ledger-api', 'atlas-shop', 'field-notes', 'orbit-docs'])
  })
  it('a parità di stato alfabetico', () => {
    const a = { ...st.sessions[2], name: 'zeta' }, b = { ...st.sessions[2], name: 'alpha' }
    expect(orderSessions([a, b]).map(s => s.name)).toEqual(['alpha', 'zeta'])
  })
  it('awaiting conta come busy', () => {
    const awaiting: Session = { ...st.sessions[1], name: 'aaa-await', state: 'awaiting' }
    expect(orderSessions([...st.sessions, awaiting]).map(s => s.name)).toEqual(['ledger-api', 'aaa-await', 'atlas-shop', 'field-notes', 'orbit-docs'])
  })
  it('vecchio da tre minuti', () => {
    expect(freshness(1000, 1179)).toEqual({ stale: false })
    expect(freshness(1000, 1180)).toEqual({ stale: true, minutes: 3 })
    expect(freshness(1000, 1000 + 65 * 60 + 5)).toEqual({ stale: true, minutes: 65 })
  })
  it('le durate', () => {
    expect(since(100, 130)).toBe(`0${NB}m`); expect(since(100, 250)).toBe(`2${NB}m`)
    expect(since(0, 65 * 60)).toBe(`1${NB}h${NB}05`); expect(since(0, 3 * 86400 + 100)).toBe(`3${NB}g`)
  })
})
