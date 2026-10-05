import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { badge } from './badge'
import { decodeState } from './contract'

// Gli stessi casi di BadgeTest in Kotlin.
describe('badge', () => {
  it('la forma segue l\'account', () => {
    expect(badge('personale', null, 'busy').square).toBe(false)
    expect(badge('agenzia', null, 'busy').square).toBe(true)
    expect(badge('professionale', null, 'idle').square).toBe(true)
  })
  it('il colore della sessione, o grigio', () => {
    expect(badge('personale', '#4C7DFF', 'idle').fill).toBe('#4C7DFF')
    expect(badge('personale', null, 'idle').fill).toBe('#9B9B9B')
    expect(badge('personale', 'rosso', 'idle').fill).toBe('#9B9B9B')
  })
  it('l\'emoji dà il colore quando manca, il colore vince', () => {
    expect(badge('personale', null, 'idle', '🟦').fill).toBe('#3B82F6')
    expect(badge('personale', '#E74C3C', 'idle', '🟢').fill).toBe('#E74C3C')
  })
  it('le fixture del contratto', () => {
    const s = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8')).sessions[0]
    expect(badge(s.account, s.color, s.state, s.icon).fill).toBe('#3B82F6')
    expect(badge(s.account, s.color, s.state).square).toBe(true)
  })
  it('un glifo per stato', () => {
    expect(badge('personale', null, 'busy').glyph).toBe('zap')
    expect(badge('personale', null, 'awaiting').glyph).toBe('zap')
    expect(badge('personale', null, 'idle').glyph).toBe('pause')
    expect(badge('personale', null, 'waiting').glyph).toBe('hand')
    expect(badge('personale', null, 'gone').glyph).toBe('cross')
  })
})
