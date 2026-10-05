import { describe, expect, it } from 'vitest'
import type { CmdResult, Outcome, Session, State } from './contract'
import { advance, CLAIM_S, due, KEEP_S, nightDir, prune, reason, status, type Sent, type Upload } from './chatRules'

// Gli stessi casi di ChatRulesTest in Kotlin.
const t = 1_000_000
const m: Sent = { id: 'c1', session: 'kb', text: 'run the tests', sentAt: t }
const s = (st: Session['state'], turn: number | null = null, outcome: Outcome | null = null): Session =>
  ({ id: '1', name: 'kb', account: 'personal', project: 'p', state: st, since: 0, turn_started: turn, outcome })
const ok: CmdResult = { id: 'c1', ok: true, text: 'delivered', at: t + 1 }

describe('ChatRules', () => {
  it('sending until the PC answers', () => expect(status(m, 'sending', null, s('idle'))).toBe('sending'))
  it('failed when rejected', () => expect(status(m, null, { ...ok, ok: false, text: 'kb is not running' }, s('gone'))).toBe('failed'))
  it('timeout is uncertain not failed', () => expect(status(m, 'failed', null, s('idle'))).toBe('uncertain'))
  it('delivered when idle', () => expect(status(m, null, ok, s('idle'))).toBe('delivered'))
  it('queued behind a running turn', () => expect(status(m, null, ok, s('busy', t - 600))).toBe('queued'))

  it('working when its turn starts', () => {
    const a = advance(m, s('busy', t + 3), t + 5)
    expect(a.startedAt).toBe(t + 3)
    expect(status(a, null, ok, s('busy', t + 3))).toBe('working')
  })
  it('done with its outcome', () => {
    const started = advance(m, s('busy', t + 3), t + 5)
    const out: Outcome = { short: 'Tests green', full: 'All 40 tests green.', at: t + 90 }
    const done = advance(started, s('idle', null, out), t + 95)
    expect(done.doneAt).toBe(t + 95)
    expect(done.outcomeFull).toBe('All 40 tests green.')
    expect(status(done, null, ok, s('idle', null, out))).toBe('done')
  })
  it('outcome of an earlier turn is not attached', () => {
    const started = advance(m, s('busy', t + 3), t + 5)
    const old: Outcome = { short: 'Old', full: 'Old turn.', at: t - 100 }
    const done = advance(started, s('idle', null, old), t + 95)
    expect(done.doneAt).not.toBeNull()
    expect(done.outcomeFull).toBeNull()
  })
  it('fast turn seen only by its outcome', () => {
    const out: Outcome = { short: 'Done', full: 'Done quickly.', at: t + 4 }
    const done = advance(m, s('idle', null, out), t + 30)
    expect(done.doneAt).toBe(t + 4)
    expect(done.outcomeFull).toBe('Done quickly.')
  })
  it('old outcome does not finish a new message', () => {
    const old: Outcome = { short: 'Old', full: 'Old turn.', at: t - 100 }
    const a = advance(m, s('idle', null, old), t + 30)
    expect(a.doneAt).toBeUndefined()
    expect(status(a, null, ok, s('idle', null, old))).toBe('delivered')
  })
  it('small clock skew still counts the turn', () => {
    const a = advance(m, s('busy', t - 5), t + 2)
    expect(status(a, null, ok, s('busy', t - 5))).toBe('working')
  })
  it('advance is idempotent once done', () => {
    const done = { ...m, startedAt: t + 1, doneAt: t + 9, outcomeFull: 'x' }
    expect(advance(done, s('busy', t + 50), t + 60)).toEqual(done)
  })
  it('prune after seven days', () => {
    expect(prune([m, { ...m, id: 'old', sentAt: t - KEEP_S - 1 }], t).map(x => x.id)).toEqual(['c1'])
  })

  it('uploading while the image goes up', () => expect(status(m, null, null, s('idle'), { type: 'going' })).toBe('uploading'))
  it('upload failure is failed with its reason', () => {
    const up: Upload = { type: 'failed', reason: 'image too large' }
    expect(status(m, null, null, s('idle'), up)).toBe('failed')
    expect(reason(null, null, up)).toBe('image too large')
  })
  it('sent once written on the bus', () => expect(status(m, 'sent', null, s('idle'))).toBe('sent'))
  it('offline waits for the network', () => expect(status(m, 'queued', null, s('idle'))).toBe('offline'))
  it('rejected carries the relay reason', () =>
    expect(reason(null, { ...ok, ok: false, text: 'kb is not running: nothing sent' }, null)).toBe('kb is not running: nothing sent'))
  it('lost has no reason from the PC', () => expect(reason('failed', null, null)).toBeNull())

  it('awaiting counts as started', () => expect(advance(m, s('awaiting'), t + 3).startedAt).toBe(t + 3))
  it('waiting keeps the turn open', () => {
    expect(advance({ ...m, startedAt: t + 1 }, s('waiting'), t + 60).doneAt).toBeUndefined()
  })
  it('failed messages do not advance', () => {
    const failed = { ...m, failed: 'kb is not running' }
    expect(advance(failed, s('busy', t + 30), t + 40)).toEqual(failed)
    expect(status(failed, null, null, s('idle'))).toBe('failed')
    expect(reason(null, null, null, failed)).toBe('kb is not running')
  })
  it('late turns are not claimed', () => {
    expect(advance(m, s('busy', t + CLAIM_S + 1), t + CLAIM_S + 5).startedAt).toBeUndefined()
  })

  const later = { ...m, scheduledFor: t + 3600 }
  it('scheduled is its own status', () => {
    expect(status(later, null, null, s('busy', t - 600))).toBe('scheduled')
    // Un turno che parte prima dell'invio non è suo.
    expect(advance(later, s('busy', t + 5), t + 10).startedAt).toBeUndefined()
  })
  it('scheduled sends once when late', () => {
    expect(due([later], t + 60)).toEqual([])
    expect(due([later, m], t + 3 * 3600).map(x => x.id)).toEqual(['c1'])
    const sent = { ...later, sentAt: t + 3 * 3600 }
    expect(due([sent], t + 4 * 3600)).toEqual([])
    expect(status(sent, 'sending', null, s('idle'))).not.toBe('scheduled')
  })
  it('night dir from the session project', () => {
    const st: State = {
      v: 1, ts: t, host: 'pc', sessions: [], quota: {}, night: { queued: 0 }, recap: { date: '', items: [] }, projects: [
        { path: '/home/demo/workspaces/personal/atlas-shop', name: 'atlas-shop', account: 'personal' },
        { path: '/home/demo/workspaces/work/clients/ledger-api', name: 'ledger-api', account: 'work' },
      ],
    }
    const ledger = { ...s('idle'), name: 'ledger-api', project: 'work/clients/ledger-api', account: 'work' }
    expect(nightDir(st, ledger)).toBe('/home/demo/workspaces/work/clients/ledger-api')
    expect(nightDir(st, { ...ledger, project: 'work/other' })).toBeNull()
  })
})
