import { describe, expect, it } from 'vitest'
import { shouldRead } from './readPlan'

// Piano prestazioni, Task 5 (A4a): la stessa cadenza del telefono (PhoneTerminal.kt). Il 07/10 la web chiedeva 888
// letture all'ora, una per sessione a ogni stato.
describe('cadenza delle letture', () => {
  it('la prima lettura parte subito', () => expect(shouldRead({ state: 'idle' }, undefined, false, 100)).toBe(true))
  it('ferma: una al minuto', () => {
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 159)).toBe(false)
    expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, false, 160)).toBe(true)
  })
  it('al lavoro o in attesa del polso: ogni 10 s', () => {
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: true }, false, 109)).toBe(false)
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: true }, false, 110)).toBe(true)
    expect(shouldRead({ state: 'awaiting' }, { at: 100, answered: true }, false, 110)).toBe(true)
  })
  it('stato della sessione cambiato: subito', () => expect(shouldRead({ state: 'idle' }, { at: 100, answered: true }, true, 101)).toBe(true))
  it('lettura senza risposta: si riprova dopo 21 s, anche se lo stato è cambiato', () => {
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 120)).toBe(false)
    expect(shouldRead({ state: 'busy' }, { at: 100, answered: false }, true, 121)).toBe(true)
  })
  it('una sessione chiusa non si legge più', () => expect(shouldRead({ state: 'gone' }, { at: 100, answered: true }, true, 500)).toBe(false))
})
