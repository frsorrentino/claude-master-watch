import { describe, expect, it } from 'vitest'
import type { Session, SessionState } from './contract'
import { closeSplash, SLOW_S } from './closeSplash'

// Gli stessi casi di CloseSplashTest.kt.
const s = (n: string, state: SessionState, since = 100): Session => ({ id: n, name: n, account: 'personale', project: n, state, since })
const sent = (delivered = false) => ({ name: 'kb', sentAt: 1_000, delivered })

describe('closeSplash', () => {
  it('a live session without exit shows nothing', () => expect(closeSplash('kb', [s('kb', 'idle')], null, null, 1_010)).toBeNull())
  it('after exit the session is closing until the PC confirms', () => {
    expect(closeSplash('kb', [s('kb', 'idle')], sent(), null, 1_002)).toEqual({ kind: 'closing', name: 'kb', sentAt: 1_000, delivered: false, slow: false })
    expect(closeSplash('kb', [s('kb', 'idle')], sent(true), null, 1_004)).toEqual({ kind: 'closing', name: 'kb', sentAt: 1_000, delivered: true, slow: false })
  })
  it('a closing that takes longer than usual says so', () =>
    expect(closeSplash('kb', [s('kb', 'idle')], sent(true), null, 1_000 + SLOW_S)).toMatchObject({ kind: 'closing', slow: true }))
  it('gone after my exit is closed by me at its last sighting', () =>
    expect(closeSplash('kb', [s('kb', 'gone', 1_003)], sent(), null, 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_003, byMe: true }))
  it('vanished after my exit is closed by me', () =>
    expect(closeSplash('kb', [], sent(), s('kb', 'gone', 1_003), 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_003, byMe: true }))
  it('gone on its own stays as a page', () => expect(closeSplash('kb', [s('kb', 'gone')], null, null, 1_005)).toBeNull())
  it('vanished on its own is closed not by me, now', () =>
    expect(closeSplash('kb', [], null, s('kb', 'idle', 900), 1_005)).toEqual({ kind: 'closed', name: 'kb', at: 1_005, byMe: false }))
  it('a question after exit shows the question, not the panel', () =>
    expect(closeSplash('kb', [{ ...s('kb', 'waiting'), question: { id: 'q1', kind: 'ask', text: 'Background tasks are running', options: [{ n: 3, label: 'Stay' }], tier: 'low', asked_at: 1_010 } }], sent(true), null, 1_030)).toBeNull())
  it("another session's exit does not count", () => expect(closeSplash('atlas', [s('atlas', 'idle')], sent(), null, 1_005)).toBeNull())
})
