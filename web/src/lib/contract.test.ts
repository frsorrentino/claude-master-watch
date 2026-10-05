import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'

const fixture = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')

describe('contratto', () => {
  it('legge i tre stati delle fixture', () => {
    for (const n of ['state-1-question.json', 'state-2-idle.json', 'state-3-stale.json']) {
      const s = decodeState(fixture(n))
      expect(s.v).toBe(1)
      expect(Array.isArray(s.sessions)).toBe(true)
    }
  })

  it('porta i ricorrenti della 1.33 e i dispositivi della 1.32', () => {
    const s = decodeState(fixture('state-2-idle.json'))
    expect(s.recurring?.map(r => r.id)).toEqual(['x-posts', 'release-changelog', 'plugin-rivals'])
    expect(s.devices?.map(d => d.kind)).toEqual(['phone', 'watch', 'tablet', 'chromebook', null])
  })

  it('rifiuta quello che non è uno stato', () => {
    expect(() => decodeState('{"x":1}')).toThrow()
  })
})
