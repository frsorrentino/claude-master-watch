import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type State } from './contract'
import { items, next, page } from './queue'

// Gli stessi casi di AttentionQueueTest in Kotlin.
const base = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const q = base.sessions.find(s => s.question)!

/** Tre sessioni con una domanda ciascuna, chieste a 300, 100 e 200. */
const three = (): State => ({
  ...base, sessions: [
    { ...q, id: 'a', name: 'a', question: { ...q.question!, id: 'qa', asked_at: 300 } },
    { ...q, id: 'b', name: 'b', question: { ...q.question!, id: 'qb', asked_at: 100 } },
    { ...q, id: 'c', name: 'c', question: { ...q.question!, id: 'qc', asked_at: 200 } },
    { ...q, id: 'd', name: 'd', question: null },
  ],
})
const without = (st: State, name: string): State => ({ ...st, sessions: st.sessions.map(s => (s.name === name ? { ...s, question: null } : s)) })

describe('AttentionQueue', () => {
  it('dalla più vecchia', () => {
    expect(items(three()).map(i => i.questionId)).toEqual(['qb', 'qc', 'qa'])
    expect(items(three())[0].session).toBe('b')
  })
  it('la domanda dopo quella corrente', () => {
    expect(next(three(), 'qb')?.questionId).toBe('qc'); expect(next(three(), null)?.questionId).toBe('qb')
  })
  it('dopo l\'ultima viene la più vecchia', () => expect(next(three(), 'qa')?.questionId).toBe('qb'))
  it('risposta al PC: la corrente esce dalla coda', () => {
    const s = without(three(), 'c')
    expect(next(s, 'qc')?.questionId).toBe('qb')
    expect(items(s).some(i => i.questionId === 'qc')).toBe(false)
  })
  it('senza domande la coda è vuota', () => {
    const s: State = { ...base, sessions: base.sessions.map(x => ({ ...x, question: null })) }
    expect(items(s)).toEqual([]); expect(next(s, null)).toBeNull()
  })
  it('la pagina dopo una risposta segue la domanda per id', () => {
    const before = three()
    const target = next(before, 'qb')!.questionId
    const after = without(before, 'b')
    expect(page(items(after), target)).toBe(0)
    expect(page(items(after), 'gone')).toBeNull()
  })
})
