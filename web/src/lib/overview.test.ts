import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { decodeState } from './contract'
import { build } from './overview'
import type { Sample } from './quotaHistory'

// Gli stessi casi di PhoneOverviewTest in Kotlin.
const state = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const now = state.ts + 60
const mk = (samples: Record<string, Sample[]> = {}, stale = false) => build(state, [], samples, now, 'Europe/Rome', stale)

describe('Panoramica del telefono', () => {
  it('anelli: personale prima, con la settimana', () => {
    const rings = mk().rings
    expect(rings.map(r => r.account)).toEqual(['personal', 'work'])
    expect(rings[0].personal).toBe(true)
    expect(rings[0].w7).toBe(36)
  })
  it('senza campioni niente ritmo ma l\'anello resta', () => {
    const ring = mk().rings[0]
    expect(ring.pace).toBeNull()
    expect(ring.h5).toBe(11)
  })
  // Franz, 02/10 16:51: il reset resta, il ritmo no (si proietterebbe da dati vecchi).
  it('con dato vecchio il reset resta ma non il ritmo', () => {
    const samples = { personal: [{ ts: now - 1800, pct: 5 }, { ts: now - 60, pct: 11 }] }
    expect(mk(samples).rings[0].pace).not.toBeNull()
    const ring = mk(samples, true).rings[0]
    expect(ring.resetAt).not.toBeNull()
    expect(ring.pace).toBeNull()
  })
  it('i conteggi di Adesso tornano con la bacheca', () => {
    const n = mk().now
    expect(n.waiting + n.working + n.idle).toBe(state.sessions.filter(s => s.state !== 'gone').length)
  })
  it('le righe del contesto portano modello ed effort', () => {
    const row = mk().contexts.find(c => c.name === 'ledger-api')!
    expect([row.pct, row.model, row.effort]).toEqual([62, 'Opus 5', 'high'])
  })
  it('l\'aggiornamento porta età e macchina', () => {
    const u = mk({}, true).updated
    expect([u.minutes, u.host, u.stale]).toEqual([1, 'crostini-demo', true])
  })
  it('la pillola della settimana porta il suo reset', () => {
    expect(mk().rings[0].weekResetAt).toBe(1789610400)
    expect(mk({}, true).rings[0].weekResetAt).toBe(1789610400)
  })
  it('l\'account vecchio è segnalato', () => {
    const rings = mk().rings
    expect(rings.find(r => r.account === 'personal')!.stale).toBe(false)
    expect(rings.find(r => r.account === 'work')!.stale).toBe(true)
  })
})
