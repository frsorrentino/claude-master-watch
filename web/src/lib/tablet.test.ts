import { describe, expect, it } from 'vitest'
import type { Session, State } from './contract'
import { build } from './summary'
import { add, columns, forecast, inspect, timelineArg, weekProjected, columnsFromPref, columnsPref, drag, equal, groups, parts, sharesFromPref, sharesPref, swap, toggle, wide } from './tablet'

// Gli stessi casi di TabletTest in Kotlin (colonne e plancia).
const at = (h: number, m = 0) => Date.UTC(2026, 9, 4, h - 2, m) / 1000
const s = (name: string, state: Session['state'] = 'idle', since = at(9)): Session => ({ id: name, name, account: 'personale', project: name, state, since })
const st = (...ss: Session[]): State => ({ v: 1, ts: at(15), host: 'pc', sessions: ss, quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } })
const summary = (state: State) => build(state, [], at(15), new Set())

describe('plancia', () => {
  it('da 840', () => { expect(wide(839)).toBe(false); expect(wide(840)).toBe(true); expect(wide(1280)).toBe(true) })
  it('la master al suo posto fra le ferme', () => {
    const g = groups(summary(st(s('master'), s('busy', 'busy'), s('idle', 'idle', at(10)))))
    expect(g.map(([x]) => x)).toEqual(['working', 'still'])
    expect(g[1][1].map(r => r.session.name)).toEqual(['idle', 'master'])
  })
  it('la master al lavoro va con chi lavora', () => {
    const g = groups(summary(st({ ...s('master', 'busy'), turn_started: at(14, 50) }, s('idle'))))
    expect(g.find(([x]) => x === 'working')![1].map(r => r.session.name)).toEqual(['master'])
  })
})

describe('colonne', () => {
  it('le vive, nel loro ordine', () => {
    const live = ['a', 'b', 'c', 'd', 'e']
    expect(columns(['c', 'gone', 'a'], live)).toEqual(['c', 'a'])
    expect(columns(['a', 'b', 'c', 'd', 'e'], live)).toEqual(['a', 'b', 'c', 'd'])
  })
  it('senza scelta le prime tre, tolte tutte nessuna', () => {
    expect(columns(null, ['x', 'y', 'z', 'w'])).toEqual(['x', 'y', 'z']); expect(columns([], ['x', 'y'])).toEqual([])
  })
  it('toggle aggiunge, toglie, sostituisce l\'ultima', () => {
    expect(toggle(['a'], 'b')).toEqual(['a', 'b']); expect(toggle(['a', 'b'], 'a')).toEqual(['b']); expect(toggle(['a', 'b', 'c', 'd'], 'e')).toEqual(['a', 'b', 'c', 'e'])
  })
  it('le preferenze su una riga', () => {
    expect(columnsPref(['a', 'b'])).toBe('a\nb'); expect(columnsFromPref('a\nb\n')).toEqual(['a', 'b'])
    expect(columnsFromPref(null)).toBeNull(); expect(columnsFromPref('')).toEqual([])
  })
  it('il clic su una scheda aggiunge', () => {
    expect(add(['a'], 'b')).toEqual(['a', 'b']); expect(add(['a', 'b'], 'a')).toEqual(['a', 'b']); expect(add(['a', 'b', 'c', 'd'], 'e')).toEqual(['a', 'b', 'c', 'e'])
  })
  it('lo scambio', () => {
    expect(swap(['a', 'b', 'c'], 0, 2)).toEqual(['c', 'b', 'a']); expect(swap(['a', 'b', 'c'], 1, 1)).toEqual(['a', 'b', 'c']); expect(swap(['a', 'b', 'c'], 0, 5)).toEqual(['a', 'b', 'c'])
  })
})

describe('larghezze a scatti', () => {
  it('uguali', () => { expect(equal(1)).toEqual([12]); expect(equal(2)).toEqual([6, 6]); expect(equal(3)).toEqual([4, 4, 4]); expect(equal(4)).toEqual([3, 3, 3, 3]) })
  it('il bordo sposta parti intere, nessuna sotto 2', () => {
    expect(drag([4, 4, 4], 0, 2)).toEqual([6, 3, 3]); expect(drag([4, 4, 4], 0, 5)).toEqual([8, 2, 2])
    expect(drag([4, 4, 4], 1, -3)).toEqual([3, 2, 7]); expect(drag([3, 3, 3, 3], 0, 1)).toEqual([4, 3, 3, 2])
    expect(drag([6, 6], 0, 2)).toEqual([8, 4]); expect(drag([6, 6], 3, 2)).toEqual([6, 6])
  })
  it('i pixel diventano parti', () => { expect(parts(190, 1200)).toBe(2); expect(parts(-60, 1200)).toBe(-1); expect(parts(40, 1200)).toBe(0) })
  it('le preferenze', () => {
    expect(sharesPref([6, 2, 4])).toBe('6,2,4'); expect(sharesFromPref('6,2,4', 3)).toEqual([6, 2, 4]); expect(sharesFromPref('6,6', 3)).toEqual([4, 4, 4])
    expect(sharesFromPref('11,1', 2)).toEqual([6, 6]); expect(sharesFromPref(null, 4)).toEqual([3, 3, 3, 3])
  })
})

describe('ispettore e previsione', () => {
  const rome = (d: number, h: number, m = 0) => Date.UTC(2026, 9, d, h - 2, m) / 1000
  const at3 = (h: number, m = 0) => rome(3, h, m)
  it('la cronologia di oggi della sessione viva', () => {
    const ses: Session = { ...s('phone', 'busy', at3(10, 2)), turn_started: at3(14, 54), tool: 'Bash', context: 61 }
    const page = { since: at3(9), sessions: [
      { session: 'phone', live: false, events: [{ at: at3(11), kind: 'commit', text: 'old', ref: 'aaa' }] },
      { session: 'phone', live: true, events: [
        { at: rome(2, 23), kind: 'commit', text: 'ieri', ref: 'b' }, { at: at3(12), kind: 'commit', text: 'menus close again', ref: '7fe65be' },
        { at: at3(13), kind: 'prompt', text: 'ok A', ref: 'phone' }, { at: at3(13, 30), kind: 'test', text: 'CI verify', ok: true },
        { at: at3(14), kind: 'commit', text: 'outside the sessions', ref: '2703acf' },
      ] },
    ] }
    const i = inspect(ses, page, at3(15), 'Europe/Rome')
    expect(i.openedAt).toBe(at3(10, 2)); expect(i.openFor).toBe(4 * 3600 + 58 * 60); expect(i.turn).toBe(6 * 60)
    expect(i.today.map(e => e.text)).toEqual(['menus close again', 'ok A', 'CI verify', 'outside the sessions'])
    expect(i.commits).toBe(2); expect(i.prompts).toBe(1)
  })
  it('senza cronologia niente giornata', () => {
    const i = inspect(s('x'), null, at3(15), 'Europe/Rome')
    expect(i.turn).toBeNull(); expect(i.today).toEqual([]); expect(i.commits).toBeNull()
  })
  it('la richiesta da mezzanotte locale', () => expect(timelineArg(at3(15), 'Europe/Rome')).toBe(String(at3(0))))
  it('il grafico della finestra di 5 ore', () => {
    const reset = at3(16)
    const f = forecast({ points: [{ ts: at3(12), pct: 2 }, { ts: at3(14), pct: 6 }], projected: 10, at: reset }, reset, at3(14))
    expect(f.points[0][0]).toBeCloseTo(0.2, 3); expect(f.points[1][0]).toBeCloseTo(0.6, 3); expect(f.nowX).toBeCloseTo(0.6, 3)
    expect(f.projected).toBe(10); expect(f.start).toBe(at3(11))
  })
  it('la settimana al ritmo medio', () => {
    const reset = at3(15) + 4 * 86400
    expect(weekProjected(24, reset, at3(15))).toBe(56); expect(weekProjected(80, reset, at3(15))).toBe(100)
    expect(weekProjected(null, reset, at3(15))).toBeNull(); expect(weekProjected(1, at3(15) + 7 * 86400 - 600, at3(15))).toBeNull()
  })
})
