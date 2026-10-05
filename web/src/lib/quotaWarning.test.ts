import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { decodeState, type QuotaAccount, type State } from './contract'
import { of } from './quotaWarning'

// Gli stessi casi di QuotaWarningTest in Kotlin. Fixture: atlas-shop è personale (h5 11, reset fra 5 ore), ledger-api è
// di lavoro (h5 assente, stale).
const base = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const now = base.ts
const p0 = base.quota.personal
const reset = p0.reset_h5!
const atlas = base.sessions.find(s => s.name === 'atlas-shop')!
const personal = (q: QuotaAccount): State => ({ ...base, quota: { ...base.quota, personal: q } })

describe('avviso della quota', () => {
  it('avvisa al novanta', () => {
    const w = of(personal({ ...p0, h5: 94 }), atlas, [], now)!
    expect([w.account, w.pct, w.resetAt, w.projected]).toEqual(['personal', 94, reset, false])
  })
  it('nessun avviso sotto il novanta con ritmo regolare', () => {
    expect(of(personal({ ...p0, h5: 40 }), atlas, [], now)).toBeNull()
  })
  // La finestra è appena ripartita: un'ora dopo è al 40 %, 30 punti in un'ora; al reset si va oltre il 100 %.
  it('avvisa quando il ritmo si esaurisce', () => {
    const w = of(personal({ ...p0, h5: 40 }), atlas, [{ ts: now, pct: 10 }, { ts: now + 3600, pct: 40 }], now + 3600)!
    expect(w.projected).toBe(true)
    expect(w.pct).toBe(40)
  })
  it('nessun avviso senza una lettura fresca', () => {
    expect(of(personal({ ...p0, h5: 95, stale: true }), atlas, [], now)).toBeNull()
    expect(of(personal({ ...p0, h5: null }), atlas, [], now)).toBeNull()
    expect(of(personal({ ...p0, h5: 95, reset_h5: null }), atlas, [], now)).toBeNull()
    expect(of(personal({ ...p0, h5: 95, reset_h5: now - 60 }), atlas, [], now)).toBeNull()
  })
  it('un altro account non avvisa', () => {
    // Personale al 99 %, lavoro fresco al 50 %: la sessione di lavoro non vede l'avviso dell'altro account.
    const st = personal({ ...p0, h5: 99 })
    st.quota = { ...st.quota, work: { ...st.quota.work, h5: 50, stale: false } }
    expect(of(st, base.sessions.find(s => s.name === 'ledger-api')!, [], now)).toBeNull()
  })
  // Segnalazione 01/10 21:58: all'1 % avvisava già. Il giudizio aspetta mezz'ora di campioni e il 20 %.
  it('nessun avviso sul ritmo con poche prove', () => {
    expect(of(personal({ ...p0, h5: 1 }), atlas, [{ ts: now, pct: 0 }, { ts: now + 120, pct: 1 }], now + 120)).toBeNull()
    expect(of(personal({ ...p0, h5: 30 }), atlas, [{ ts: now, pct: 10 }, { ts: now + 600, pct: 30 }], now + 600)).toBeNull()
    expect(of(personal({ ...p0, h5: 15 }), atlas, [{ ts: now, pct: 2 }, { ts: now + 3600, pct: 15 }], now + 3600)).toBeNull()
  })
})
