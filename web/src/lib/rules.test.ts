import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { age, groups } from './summary'
import { parseSteps } from './nextSteps'

const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))

describe('home', () => {
  it('raggruppa come il telefono', () => {
    expect(groups(st.sessions).map(g => [g.group, g.sessions.map(s => s.name)])).toEqual([
      ['waiting', ['ledger-api']], ['working', ['atlas-shop']], ['idle', ['field-notes']], ['closed', ['orbit-docs']],
    ])
  })
  it('scrive le età corte', () => {
    expect(age(0, 300)).toBe('5 m'); expect(age(0, 3 * 3600)).toBe('3 h'); expect(age(0, 2 * 86400)).toBe('2 g')
  })
})

// Gli stessi casi di NextStepsTest in Kotlin.
describe('Prossimi', () => {
  it("l'ultima riga diventa consigli", () => {
    const r = parseSteps("Ho pushato.\n\nEsito: push fatto.\n\nProssimi: apri l'app · scrivi il piano · correggi Lancia")
    expect(r.text).toBe('Ho pushato.\n\nEsito: push fatto.')
    expect(r.steps).toEqual(["apri l'app", 'scrivi il piano', 'correggi Lancia'])
  })
  it('al massimo tre e non troppo lunghi', () => {
    expect(parseSteps(`Fatto.\nProssimi: uno · ${'x'.repeat(41)} · due · tre · quattro`).steps).toEqual(['uno', 'due', 'tre'])
  })
  it('senza riga niente consigli', () => {
    expect(parseSteps('Fatto.\n\nEsito: tutto ok.')).toEqual({ text: 'Fatto.\n\nEsito: tutto ok.', steps: [] })
  })
})
