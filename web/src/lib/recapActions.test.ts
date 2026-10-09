import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { recapActions } from './recapActions'

// Gli stessi casi di RecapActionsTest in Kotlin.
describe('recapActions', () => {
  const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
  it('le azioni delle sessioni vanno a loro, quelle del recap alla sessione viva del progetto o alla master', () => {
    const withGone = { ...st, recap: { ...st.recap, items: [...st.recap.items, { project: 'orbit-docs', done: 'Pricing page drafted', next: 'Publish the pricing page' }] } }
    const a = recapActions(withGone)
    const orbit = a.filter(x => x.from === 'orbit-docs')
    expect(orbit).toHaveLength(1)
    expect(orbit[0].to).toBe('master'); expect(orbit[0].send).toBe('Riprendi orbit-docs: Publish the pricing page')
    expect(a.find(x => x.text === 'Review the seeds and the admin page')?.to).toBe('atlas-shop')
    expect(new Set(a.map(x => x.text.toLowerCase())).size).toBe(a.length)
    expect(orbit[0].recap).toBe(true); expect(a.find(x => x.text === 'Review the seeds and the admin page')?.recap).toBe(true)
    expect(a.find(x => x.text === 'ok to deploy on staging')?.recap).toBe(false)
  })
  it('nel Recap solo i «prossimo» del recap del giorno (Franz, 09/10 17:21)', () => {
    const a = recapActions(st, undefined, false)
    expect(a.length).toBeGreaterThan(0)
    expect(a.every(x => x.recap)).toBe(true)
    expect(a.find(x => x.text === 'ok to deploy on staging')).toBeUndefined()
  })
})
