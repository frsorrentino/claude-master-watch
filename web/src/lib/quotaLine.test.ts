import { describe, expect, it } from 'vitest'
import { shown } from './quotaLine'

describe('riga della quota nella home', () => {
  // Segnalazione 05/10 20:54: con un account non aggiornato si vede solo quello aggiornato, a tutta larghezza.
  it('toglie gli account non aggiornati se ce n\'è almeno uno aggiornato', () => {
    expect(shown([['personal', { stale: false }], ['work', { stale: true }]]).map(([n]) => n)).toEqual(['personal'])
  })
  it('se sono tutti vecchi li lascia', () => {
    const all: [string, { stale?: boolean }][] = [['personal', { stale: true }], ['work', { stale: true }]]
    expect(shown(all)).toEqual(all)
  })
  it('senza stale vale aggiornato', () => {
    expect(shown([['personal', {}], ['work', { stale: true }]]).map(([n]) => n)).toEqual(['personal'])
  })
})
