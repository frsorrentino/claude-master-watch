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

  it('porta consiglio, approvazioni e sessioni finite della 1.37, con i default quando mancano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    const atlas = s.sessions.find(x => x.name === 'atlas-shop')!
    expect(atlas.advice).toMatchObject({ model: 'claude-fable-5-1', effort: 'high', switch_cost_tokens: 36000, when: 'next_task', differs: true })
    expect(s.sessions.find(x => x.name === 'field-notes')!.finished).toBe(true)
    expect(atlas.finished).toBe(false)
    expect(atlas.duplicate_of).toBeNull()
    expect(s.sessions.find(x => x.name === 'field-notes')!.advice).toBeNull()
    expect(s.approvals.map(a => [a.task, a.deploy])).toEqual([['atlas-release-2-4', true]])
    expect(decodeState(fixture('state-3-stale.json')).approvals).toEqual([])
    expect(s.ops).toEqual(expect.arrayContaining(['approve', 'decision']))
  })

  it('porta i Prossimi della 1.38, con le voci che sbloccano', () => {
    const s = decodeState(fixture('state-1-question.json'))
    expect(s.sessions.find(x => x.name === 'atlas-shop')!.next_steps).toEqual([
      { text: 'ok to deploy on staging', blocking: true }, { text: 'review the test seeds', blocking: false },
    ])
    expect(s.sessions.find(x => x.name === 'field-notes')!.next_steps).toBeUndefined()
  })

  it('rifiuta quello che non è uno stato', () => {
    expect(() => decodeState('{"x":1}')).toThrow()
  })
})
