import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { decodeState } from './contract'
import { contexts, night, now as nowSeg, questions, updated } from './workPanel'

// Gli stessi casi di WorkPanelTest in Kotlin. Fixture: ledger-api aspetta una risposta (ctx 62), atlas-shop lavora (18),
// field-notes è ferma (4), orbit-docs è sparita.
const state = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const now = state.ts

describe('pannello Lavoro', () => {
  it('Adesso ha un segmento per sessione viva', () => {
    const a = nowSeg(state)
    expect(a.segments).toEqual(['waiting', 'working', 'idle'])
    expect([a.working, a.waiting, a.idle]).toEqual([1, 1, 1])
  })
  it('awaiting conta come lavoro', () => {
    const s = { ...state, sessions: state.sessions.map(x => (x.name === 'field-notes' ? { ...x, state: 'awaiting' as const } : x)) }
    expect(nowSeg(s).working).toBe(2)
  })
  it('contesto ordinato dalla più piena', () => {
    const c = contexts(state)
    expect(c.map(x => x.name)).toEqual(['ledger-api', 'atlas-shop', 'field-notes'])
    expect(c[0].pct).toBe(62)
  })
  it('contesto al massimo quattro righe', () => {
    const molte = { ...state, sessions: [1, 2, 3, 4, 5, 6].map(i => ({ ...state.sessions[1], id: `s${i}`, name: `s${i}`, context: i * 10 })) }
    const c = contexts(molte)
    expect(c.length).toBe(4)
    expect(c[0].name).toBe('s6')
  })
  it('contesto con le soglie delle misure', () => {
    const s = { ...state, sessions: state.sessions.map(x => (x.name === 'ledger-api' ? { ...x, context: 91 } : x)) }
    expect(contexts(s)[0].tone).toBe('alert')
  })
  it('domande con la più vecchia', () => {
    const d = questions(state, now)!
    expect([d.count, d.oldest, d.age]).toEqual([1, 'ledger-api', '5 m'])
  })
  it('senza domande niente card', () => {
    expect(questions({ ...state, sessions: state.sessions.map(x => ({ ...x, question: null })) }, now)).toBeNull()
  })
  it('notte solo quando c\'è qualcosa', () => {
    expect(night(state)!.queued).toBe(2)
    expect(night({ ...state, night: { queued: 0 } })).toBeNull()
  })
  it('riga dell\'aggiornamento', () => {
    const fresco = updated(state, { stale: false }, now)
    expect([fresco.minutes, fresco.host, fresco.stale]).toEqual([0, 'crostini-demo', false])
    const fermo = updated(state, { stale: true, minutes: 7 }, now + 7 * 60)
    expect([fermo.minutes, fermo.stale]).toEqual([7, true])
  })
})
