import { describe, expect, it } from 'vitest'
import type { Session } from './contract'
import { modelKind, notes, resetLabel, sameModel, shortModel, tone, wider } from './header'

// Gli stessi casi di ModelTextTest, TuneTest, ContextActionsTest, SessionMetersTest e SessionsTextTest in Kotlin.
describe('modello', () => {
  it('famiglia, per la riga che lo spiega nel pannello', () => {
    expect(['claude-opus-5-5[1m]', 'claude-fable-5-1', 'claude-sonnet-5', 'claude-haiku-4-5', 'claude-x'].map(modelKind))
      .toEqual(['opus', 'fable', 'sonnet', 'haiku', null])
  })
  it('nome corto', () => {
    expect(shortModel({ id: 'claude-opus-5[1m]', label: 'Opus 5' })).toBe('Opus 5')
    expect(shortModel({ id: 'claude-opus-5' })).toBe('Opus 5')
    expect(shortModel({ id: 'claude-sonnet-5' })).toBe('Sonnet 5')
    expect(shortModel({ id: 'claude-haiku-4-5-20251001' })).toBe('Haiku 4.5')
    expect(shortModel({ id: 'claude-opus-5[1m]' })).toBe('Opus 5')
    expect(shortModel({ id: 'claude-qualcosa-nuovo' })).toBe('qualcosa-nuovo')
    expect(shortModel(null)).toBeNull()
  })
  it('stesso modello con e senza finestra', () => {
    expect(sameModel('claude-opus-5-5[1m]', 'claude-opus-5-5')).toBe(true)
    expect(sameModel('claude-sonnet-5', 'claude-opus-5-5')).toBe(false)
    expect(sameModel(null, 'claude-opus-5-5')).toBe(false)
  })
  it('la finestra da 1M solo quando non è già in uso', () => {
    const choices = { models: [{ id: 'claude-opus-5-5[1m]', label: 'Opus 5.5' }, { id: 'claude-sonnet-5', label: 'Sonnet 5' }], efforts: ['low', 'max'] }
    const s = (id: string): Session => ({ id: 'kb', name: 'kb', account: 'personal', project: 'p', state: 'idle', since: 0, model: { id }, context: 83 })
    expect(wider(s('claude-opus-5-5'), choices)?.id).toBe('claude-opus-5-5[1m]')
    expect(wider(s('claude-opus-5-5[1m]'), choices)).toBeNull()
    expect(wider(s('claude-sonnet-5'), choices)).toBeNull()
    expect(wider(s('claude-opus-5-5'), null)).toBeNull()
  })
})

describe('misure', () => {
  it('i toni del contesto e della quota', () => {
    expect([43, 74, 75, 90, null].map(tone)).toEqual(['neutral', 'neutral', 'warn', 'alert', 'neutral'])
  })
  const rome = (d: number, h: number, m = 0) => Date.UTC(2026, 9, d, h - 2, m) / 1000
  it("l'azzeramento di oggi senza giorno, dopo mezzanotte col giorno", () => {
    expect(resetLabel(rome(4, 23), rome(4, 20, 43), 'Europe/Rome')).toBe('23:00')
    expect(resetLabel(rome(5, 1), rome(4, 22, 30), 'Europe/Rome')).toBe('lun 01:00')
    expect(resetLabel(rome(4, 20), rome(4, 20, 43), 'Europe/Rome')).toBeNull()
  })
})

describe('righe sotto la testata', () => {
  const t = { goal: 'Obiettivo', lowPriority: 'bassa priorità', lowPriorityOffered: 'bassa priorità proposta', noWindow: 'senza finestra' }
  const s = (x: Partial<Session>): Session => ({ id: 'a', name: 'a', account: 'personal', project: 'p', state: 'idle', since: 0, attached: true, ...x })
  it('obiettivo, priorità e finestra', () => {
    expect(notes(s({ goal: { text: ' Test verdi ', since: 0 }, low_priority: 'active' }), t)).toEqual(['Obiettivo: Test verdi', 'bassa priorità'])
    expect(notes(s({ low_priority: 'offered', attached: false }), t)).toEqual(['bassa priorità proposta', 'senza finestra'])
    expect(notes(s({ goal: { text: '  ', since: 0 }, low_priority: 'off', attached: false, state: 'gone' }), t)).toEqual([])
  })
})
