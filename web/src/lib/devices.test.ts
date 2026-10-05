import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Session, type State } from './contract'
import type { Freshness } from './durations'
import { build, linked, type Tone } from './devices'

// Gli stessi casi di SettingsDevicesTest in Kotlin.
const now = 1_790_000_000
const s = (name: string, st: Session['state'] = 'idle'): Session => ({ id: name, name, account: 'personale', project: name, state: st, since: 0 })
const state: State = decodeState(JSON.stringify({
  v: 1, ts: now, host: 'penguin',
  sessions: [s('master'), s('app'), s('relay', 'busy'), s('old', 'gone')],
  quota: { professionale: { h5: 2, stale: true, kind: 'work' }, personale: { h5: 7, kind: 'personal' } },
}))
const fresh: Freshness = { stale: false }
const mk = (o: { host?: string | null; st?: State | null; fresh?: Freshness | null; watch?: string | null; pending?: boolean; reachable?: boolean | null } = {}) => {
  const { host = 'penguin', st = state, fresh: f = fresh, watch = 'watch-pixel5', pending = false, reachable = true } = o
  return build(host, st, f, now, 'Pixel 11 Pro XL', '0.2', true, watch, pending, reachable)
}

describe('SettingsDevices', () => {
  it('tutto vivo con stato fresco e orologio vicino', () => {
    const m = mk()
    expect([m.phone.tone, m.pc.tone, m.watch.tone]).toEqual(['live', 'live', 'live'])
    expect(m.pcLink).toBe('live'); expect(m.watchLink).toBe('live')
  })

  it('il PC conta le sessioni aperte come la home', () => {
    const pc = mk().pc
    expect(pc.open).toBe(2); expect(pc.host).toBe('penguin')
    expect(pc.accounts.map(a => a.account)).toEqual(['personale', 'professionale'])
    expect(pc.accounts.map(a => a.pct)).toEqual([7, 2]); expect(pc.accounts.map(a => a.stale)).toEqual([false, true])
  })

  it('uno stato vecchio rende arancio il PC e il suo filo', () => {
    const m = mk({ fresh: { stale: true, minutes: 12 } })
    expect([m.pc.tone, m.pcLink, m.pc.ageMinutes]).toEqual(['stale', 'stale', 12])
  })

  it('un orologio in attesa della chiave o fuori portata è arancio', () => {
    expect(mk({ pending: true }).watch.tone).toBe('stale')
    expect(mk({ reachable: false }).watch.tone).toBe('stale')
    expect(mk({ reachable: false }).watchLink).toBe('stale')
    expect(mk({ reachable: null }).watch.tone).toBe('live')
  })

  it('non abbinato è grigio ovunque tranne il telefono', () => {
    const m = mk({ host: null, st: null, fresh: null, watch: null, reachable: null })
    expect(m.paired).toBe(false); expect(m.phone.tone).toBe('live')
    expect([m.pc.tone, m.watch.tone, m.pcLink, m.watchLink]).toEqual<Tone[]>(['off', 'off', 'off', 'off'])
  })

  it('abbinato senza orologio lascia spento il suo filo', () => {
    const m = mk({ watch: null, reachable: null })
    expect(m.paired).toBe(true); expect(m.watch.tone).toBe('off'); expect(m.watchLink).toBe('off')
  })

  // Contratto 1.32: «questo» è il proprio uid; verde se ha letto da mezz'ora al massimo, arancio oltre, spento se mai.
  it('i dispositivi veri dallo stato', () => {
    const st = decodeState(readFileSync(new URL('../../../contract/state-2-idle.json', import.meta.url), 'utf8'))
    const t = st.devices![0].seen! + 10
    const l = linked(st, 'tabletUid0000000000000000000', t)!
    expect(l.map(d => d.name)).toEqual(['Pixel 9', 'Pixel Watch 5', 'Pixel Tablet', 'Chromebook', 'Pixel 7'])
    expect(l.map(d => d.self)).toEqual([false, false, true, false, false])
    expect(l[0].tone).toBe('live'); expect(l[3].tone).toBe('stale'); expect(l[4].tone).toBe('off'); expect(l[2].tone).toBe('live')
  })

  it('prima del contratto non c\'è l\'elenco', () => expect(linked(decodeState('{"v":1,"ts":0,"host":"pc"}'), 'u', 0)).toBeNull())
})
