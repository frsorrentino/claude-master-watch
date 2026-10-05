import { describe, expect, it } from 'vitest'
import type { Event, EventKind } from './contract'
import { peak, today } from './dayBars'

// Gli stessi casi di DayBarsTest in Kotlin.
const zone = 'Europe/Rome'
const ev = (kind: EventKind, ts: number, n = 0): Event => ({ key: `${kind}-${ts}-${n}`, kind, session: 'atlas-shop', ts, title: 'x' })
// 16/09/2026, 00:00 locali (22:00 UTC del 15)
const mezzanotte = Date.UTC(2026, 8, 15, 22) / 1000
const ore = (h: number, m = 0) => mezzanotte + h * 3600 + m * 60

describe('Oggi', () => {
  it('una barra per ogni ora fino a ora', () => {
    const out = today([], ore(9, 30), zone)
    expect(out.length).toBe(10)
    expect(out[0].hour).toBe(0)
    expect(out[out.length - 1].hour).toBe(9)
    expect(out.every(b => b.count === 0)).toBe(true)
  })
  it('conta gli eventi nell\'ora giusta', () => {
    const out = today([ev('launched', ore(8, 5)), ev('outcome', ore(8, 40), 1), ev('question', ore(9, 10))], ore(9, 30), zone)
    expect(out.find(b => b.hour === 8)!.count).toBe(2)
    expect(out.find(b => b.hour === 9)!.count).toBe(1)
    expect(out.find(b => b.hour === 7)!.count).toBe(0)
  })
  it('ieri non entra', () => {
    expect(today([ev('outcome', ore(8) - 24 * 3600)], ore(9), zone).every(b => b.count === 0)).toBe(true)
  })
  it('gli eventi di quota non sono lavoro', () => {
    expect(today([ev('quota', ore(8, 5))], ore(9), zone).find(b => b.hour === 8)!.count).toBe(0)
  })
  it('il più alto serve a scalare le barre', () => {
    const eventi = [ev('outcome', ore(8, 5)), ev('outcome', ore(8, 6), 1), ev('answered', ore(9, 1))]
    expect(peak(today(eventi, ore(9, 30), zone))).toBe(2)
  })
})
