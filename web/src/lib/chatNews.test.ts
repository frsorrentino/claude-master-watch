import { describe, expect, it } from 'vitest'
import type { Item } from './chatFeed'
import { at, firstNew, latest, rank } from './chatNews'

const claude = (id: string, t: number): Item => ({ type: 'claude', entry: { id, role: 'assistant', text: 'ok', at: t } as any })
const user = (id: string, t: number): Item => ({ type: 'user', entry: { id, role: 'user', text: 'go', at: t } as any })
const mine = (id: string, t: number): Item => ({ type: 'mine', sent: { id, session: 'x', text: 'fatto?', sentAt: t } as any, status: 'delivered', entry: null })
const steps = (...ts: number[]): Item => ({ type: 'steps', entries: ts.map((t, i) => ({ id: `s${i}`, role: 'tool', text: 'Bash', at: t }) as any) })
const feed = [claude('a', 100), user('b', 200), mine('c', 300), steps(310, 320), claude('d', 330)]

describe('apertura della sessione, come sul telefono', () => {
  it('i nuovi partono dopo l\'ultima visita e saltano i tuoi', () => {
    expect(firstNew(feed, 250)).toBe(3)
    expect(firstNew(feed, 150)).toBe(1)
    expect(firstNew(feed, 330)).toBeNull()
    expect(firstNew(feed, null)).toBeNull()
  })
  it('l\'ultimo momento si ricorda lasciando la chat', () => {
    expect(latest(feed)).toBe(330)
    expect(latest([])).toBeNull()
    expect(at(steps(310, 320))).toBe(320)
  })
  it('solo le ultime sei entrano a cascata, dall\'alto', () => {
    expect(Array.from({ length: 10 }, (_, i) => rank(i, 10))).toEqual([null, null, null, null, 0, 1, 2, 3, 4, 5])
    expect([0, 1, 2].map(i => rank(i, 3))).toEqual([0, 1, 2])
  })
})
