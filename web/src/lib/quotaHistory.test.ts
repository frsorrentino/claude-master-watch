import { describe, expect, it } from 'vitest'
import { pace, window, WINDOW_S, type Sample } from './quotaHistory'

// Gli stessi casi di QuotaHistoryTest in Kotlin.
const reset = 1789521000
const start = reset - WINDOW_S
const s = (offsetS: number, pct: number): Sample => ({ ts: start + offsetS, pct })

describe('ritmo della finestra', () => {
  it('la finestra tiene solo i campioni dopo il suo inizio', () => {
    const fuori = { ts: start - 60, pct: 90 }
    const dentro = [s(0, 4), s(3600, 12)]
    expect(window([fuori, ...dentro], reset)).toEqual(dentro)
  })
  it('i campioni restano in ordine di tempo', () => {
    expect(window([s(3600, 12), s(0, 4)], reset).map(x => x.pct)).toEqual([4, 12])
  })
  it('la proiezione estende il ritmo fino al reset', () => {
    // 10 % a inizio finestra, 20 % un'ora dopo: 10 punti all'ora; al reset mancano 4 ore: 20 + 40 = 60 %.
    const p = pace([s(0, 10), s(3600, 20)], reset, start + 3600)
    expect(p.projected).toBe(60)
    expect(p.at).toBe(reset)
  })
  it('la proiezione non supera il cento', () => {
    expect(pace([s(0, 40), s(3600, 80)], reset, start + 3600).projected).toBe(100)
  })
  it('senza consumo nessuna proiezione', () => {
    expect(pace([s(0, 12), s(3600, 12)], reset, start + 3600).projected).toBeNull()
  })
  it('con un solo campione nessuna proiezione', () => {
    const p = pace([s(0, 12)], reset, start + 600)
    expect(p.projected).toBeNull()
    expect(p.points.map(x => x.pct)).toEqual([12])
  })
  it('la proiezione usa solo la finestra corrente', () => {
    const vecchio = { ts: start - 7200, pct: 95 }
    expect(pace([vecchio, s(0, 10), s(3600, 20)], reset, start + 3600).projected).toBe(60)
  })
})
