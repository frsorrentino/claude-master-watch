import { describe, expect, it } from 'vitest'
import type { Outcome, Question, Session, State } from './contract'
import { FRESH_S, alert, key } from './elsewhere'

// Gli stessi casi di ElsewhereTest in Kotlin.
const now = 1_000_000
const s = (name: string, st: Session['state'], question: Question | null = null, outcome: Outcome | null = null, followed = false): Session =>
  ({ id: name, name, account: 'personal', project: 'p', state: st, since: 0, question, outcome, followed })
const q = (id: string, asked: number): Question => ({ id, kind: 'ask', text: 'Pubblico?', options: [], tier: 'low', asked_at: asked })
const done = (at: number): Outcome => ({ short: 'Fatto', full: 'Fatto tutto.', at })
const state = (...sessions: Session[]): State => ({ v: 1, ts: now, host: 'pc', sessions, quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } })
const none = new Set<string>()

describe('Elsewhere', () => {
  it('una domanda altrove viene prima', () => {
    const st = state(s('here', 'idle'), s('ledger', 'waiting', q('q1', now - 60)), s('atlas', 'idle', null, done(now - 30), true))
    expect(alert(st, 'here', now, none, none)).toEqual({ type: 'waiting', sessions: ['ledger'] })
  })

  it('la sessione aperta non avvisa se stessa', () => {
    expect(alert(state(s('ledger', 'waiting', q('q1', now - 60))), 'ledger', now, none, none)).toBeNull()
  })

  it('più in attesa, dalla più vecchia', () => {
    const st = state(s('b', 'waiting', q('q2', now - 10)), s('a', 'waiting', q('q1', now - 300)))
    expect(alert(st, 'here', now, none, none)).toEqual({ type: 'waiting', sessions: ['a', 'b'] })
  })

  // «ha finito» solo per chi segui o per chi hai scritto dal telefono.
  it('un turno finito solo da sessioni seguite o scritte', () => {
    const other = s('atlas', 'idle', null, done(now - 30))
    expect(alert(state(other), 'here', now, none, none)).toBeNull()
    expect(alert(state({ ...other, followed: true }), 'here', now, none, none)).toEqual({ type: 'finished', session: 'atlas', at: now - 30 })
    expect(alert(state(other), 'here', now, new Set(['atlas']), none)).toEqual({ type: 'finished', session: 'atlas', at: now - 30 })
  })

  it('un turno finito si mostra una volta', () => {
    const st = state(s('atlas', 'idle', null, done(now - 30), true))
    const first = alert(st, 'here', now, none, none)!
    expect(alert(st, 'here', now, none, new Set([key(first)]))).toBeNull()
  })

  it('un esito vecchio o una sessione al lavoro non sono novità', () => {
    expect(alert(state(s('atlas', 'idle', null, done(now - FRESH_S - 1), true)), 'here', now, none, none)).toBeNull()
    expect(alert(state(s('atlas', 'busy', null, done(now - 30), true)), 'here', now, none, none)).toBeNull()
  })

  it('prima il turno finito più recente', () => {
    const st = state(s('a', 'idle', null, done(now - 200), true), s('b', 'idle', null, done(now - 20), true))
    expect((alert(st, 'here', now, none, none) as { session: string }).session).toBe('b')
  })

  it('le chiavi come in Kotlin', () => {
    expect(key({ type: 'waiting', sessions: ['a', 'b'] })).toBe('w:a,b'); expect(key({ type: 'finished', session: 'x', at: 5 })).toBe('f:x@5')
  })
})
