import { describe, expect, it } from 'vitest'
import type { Question, Session, State } from './contract'
import { finishedKey, FINISHED_S } from './summary'
import { forYou, forYouFirst, hero, working } from './masterHome'

// Gli stessi casi di MasterHomeTest in Kotlin (le righe che la web app già legge).
const at = (h: number, m = 0) => Date.UTC(2026, 9, 2, h - 2, m) / 1000
const s = (name: string, state: Session['state'] = 'idle', ctx: number | null = 10, q: Question | null = null): Session =>
  ({ id: name, name, account: 'personale', project: name, state, since: 0, context: ctx, question: q })
const q = (id: string, asked: number): Question => ({ id, kind: 'ask', text: 'Pubblico?', options: [], tier: 'low', asked_at: asked })
const st = (...ss: Session[]): State => ({ v: 1, ts: 0, host: 'pc', sessions: ss, quota: {} })
const done = (name: string, t: number): Session => ({ ...s(name), followed: true, outcome: { short: 'Fatto', full: 'Test verdi.', at: t } })
const claude = (text: string, t: number) => ({ role: 'assistant', text, at: t })

describe('Per te', () => {
  it('niente da fare, niente righe', () => {
    expect(forYou(st(s('kb')), [], at(15))).toEqual({ rows: [], more: 0 })
  })
  it('domande dalla più vecchia, poi il contesto', () => {
    const f = forYou(st(s('a', 'waiting', 10, q('2', at(14))), s('b', 'waiting', 10, q('1', at(13))), s('master', 'idle', 83)), [], at(15))
    expect(f.rows.map(r => r.kind)).toEqual(['question', 'question', 'context'])
    expect(f.rows.map(r => r.session)).toEqual(['b', 'a', 'master'])
    expect(f.rows[2].number).toBe(83)
  })
  it('tagliate a tre con il conto delle altre', () => {
    const state = st(s('a', 'idle', 81), s('b', 'idle', 82), s('c', 'idle', 83), s('d', 'idle', 84))
    const f = forYou(state, [], at(15))
    expect(f.rows.map(r => r.session)).toEqual(['d', 'c', 'b']); expect(f.more).toBe(1)
    expect(forYou(state, [], at(15), new Set(), Infinity).rows).toHaveLength(4)
  })
  it('chi ha finito resta finché non lo leggi, non gli scrivi o passano 12 ore', () => {
    const b = done('b', at(14, 30))
    expect(forYou(st(b), [], at(15)).rows.map(r => r.kind)).toEqual(['finished'])
    expect(forYou(st(b), [{ session: 'b', sentAt: at(14, 40) }], at(15)).rows).toEqual([])
    expect(forYou(st(b), [], at(15), new Set([finishedKey('b', at(14, 30))])).rows).toEqual([])
    expect(forYou(st(b), [], at(14, 30) + FINISHED_S + 1).rows).toEqual([])
  })
  it('la master non sta mai nella sua lista', () => {
    expect(forYou(st(done('master', at(14, 30))), [], at(15)).rows).toEqual([])
  })
  it('una domanda mette Per te prima', () => {
    expect(forYouFirst(forYou(st(s('a', 'waiting', 10, q('1', at(14)))), [], at(15)))).toBe(true)
    expect(forYouFirst(forYou(st(s('a', 'idle', 85)), [], at(15)))).toBe(false)
  })
})

describe('al lavoro', () => {
  it('solo chi lavora, senza la master', () => {
    const state = st(s('a', 'busy'), s('b', 'awaiting'), s('c', 'waiting', 10, q('1', at(14))), s('d'), s('master', 'busy'))
    expect(new Set(working(state).map(r => r.session.name))).toEqual(new Set(['a', 'b']))
  })
  it("l'ultimo esito, non il comando", () => {
    const busy = { ...s('a', 'busy'), tool: 'Bash', tool_note: 'cat /tmp/x', outcome: { short: 'Fatto', full: 'Test verdi.\nProssimi: tagga · apri la PR', at: at(14) } }
    expect(working(st(busy))[0].detail).toBe('Test verdi.\nProssimi: tagga · apri la PR')
    expect(working(st({ ...s('a', 'busy'), tool: 'Bash' }))[0].detail).toBeNull()
  })
})

describe('ultimo esito', () => {
  it('la riga Esito fa da titolo', () => {
    const h = hero([claude('vecchia', at(5)), claude('Lanciata la sessione.\n\nEsito: Fase 2.2 avviata\nProssimi: distilla nella kb · prova la casa\nWatch: avviata', at(6))], s('master'))!
    expect(h).toEqual({ headline: 'Fase 2.2 avviata', body: 'Lanciata la sessione.', steps: ['distilla nella kb', 'prova la casa'], at: at(6) })
  })
  it('senza Esito la riga Watch', () => {
    const h = hero([claude('Ha senso in parte. Conviene tenere il riepilogo e togliere il resto.\n\nWatch: Riepilogo sì, il resto no', at(6))], s('master'))!
    expect(h.headline).toBe('Riepilogo sì, il resto no')
    expect(h.body).toBe('Ha senso in parte. Conviene tenere il riepilogo e togliere il resto.')
  })
  it('senza Esito né Watch la prima riga', () => {
    const h = hero([claude('Fatto il push.\nCI verde.', at(6))], s('master'))!
    expect([h.headline, h.body]).toEqual(['Fatto il push.', 'CI verde.'])
  })
  it("senza conversazione l'esito del relay", () => {
    expect(hero([], { ...s('master'), outcome: { short: 'corto', full: 'Esito: dal relay', at: at(7) } })!.headline).toBe('dal relay')
    expect(hero([], s('master'))).toBeNull()
  })
})
