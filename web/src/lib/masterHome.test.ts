import { describe, expect, it } from 'vitest'
import type { Event, Question, Session, State } from './contract'
import { finishedKey, FINISHED_S } from './summary'
import { forYou, forYouFirst, hero, nextKey, working, workingText, type Scheduled } from './masterHome'

// Gli stessi casi di MasterHomeTest in Kotlin (le righe che la web app già legge).
const at = (h: number, m = 0) => Date.UTC(2026, 9, 2, h - 2, m) / 1000
const s = (name: string, state: Session['state'] = 'idle', ctx: number | null = 10, q: Question | null = null): Session =>
  ({ id: name, name, account: 'personale', project: name, state, since: 0, context: ctx, question: q })
const q = (id: string, asked: number): Question => ({ id, kind: 'ask', text: 'Pubblico?', options: [], tier: 'low', asked_at: asked })
const st = (...ss: Session[]): State => ({ v: 1, ts: 0, host: 'pc', sessions: ss, quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } })
const zone = { timeZone: 'Europe/Rome' }
const fy = (state: State, now: number, o: { events?: Event[]; sent?: Scheduled[]; read?: Set<string>; limit?: number } = {}) =>
  forYou(state, o.events ?? [], o.sent ?? [], now, { ...zone, read: o.read, limit: o.limit })
const kinds = (f: ReturnType<typeof forYou>) => f.rows.map(r => r.kind)
const done = (name: string, t: number): Session => ({ ...s(name), followed: true, outcome: { short: 'Fatto', full: 'Test verdi.', at: t } })
const claude = (text: string, t: number) => ({ role: 'assistant', text, at: t })

describe('Per te', () => {
  it('niente da fare, niente righe', () => {
    expect(fy(st(s('kb')), at(15))).toEqual({ rows: [], more: 0 })
  })
  it('domande dalla più vecchia, poi il contesto', () => {
    const f = fy(st(s('a', 'waiting', 10, q('2', at(14))), s('b', 'waiting', 10, q('1', at(13))), s('master', 'idle', 83)), at(15))
    expect(f.rows.map(r => r.kind)).toEqual(['question', 'question', 'context'])
    expect(f.rows.map(r => r.session)).toEqual(['b', 'a', 'master'])
    expect(f.rows[2].number).toBe(83)
  })
  it('tagliate a tre con il conto delle altre', () => {
    const state = st(s('a', 'idle', 81), s('b', 'idle', 82), s('c', 'idle', 83), s('d', 'idle', 84))
    const f = fy(state, at(15))
    expect(f.rows.map(r => r.session)).toEqual(['d', 'c', 'b']); expect(f.more).toBe(1)
    expect(fy(state, at(15), { limit: Infinity }).rows).toHaveLength(4)
  })
  it('chi ha finito resta finché non lo leggi, non gli scrivi o passano 12 ore', () => {
    const b = done('b', at(14, 30))
    expect(kinds(fy(st(b), at(15)))).toEqual(['finished'])
    expect(fy(st(b), at(15), { sent: [{ session: 'b', sentAt: at(14, 40) }] }).rows).toEqual([])
    expect(fy(st(b), at(15), { read: new Set([finishedKey('b', at(14, 30))]) }).rows).toEqual([])
    expect(fy(st(b), at(14, 30) + FINISHED_S + 1).rows).toEqual([])
  })
  it('la master non sta mai nella sua lista', () => {
    expect(fy(st(done('master', at(14, 30))), at(15)).rows).toEqual([])
  })
  it('una domanda mette Per te prima', () => {
    expect(forYouFirst(fy(st(s('a', 'waiting', 10, q('1', at(14)))), at(15)))).toBe(true)
    expect(forYouFirst(fy(st(s('a', 'idle', 85)), at(15)))).toBe(false)
  })
})

describe('Per te: notte, recap e programmati', () => {
  const report: Event = { key: 'nr-1', kind: 'night_report', ts: at(6, 10), title: 'Stanotte', body: '3 lavori fatti' }
  it('resoconto della notte solo la mattina e finché non è letto', () => {
    expect(kinds(fy(st(s('kb')), at(7, 40), { events: [report] }))).toEqual(['night_report'])
    expect(fy(st(s('kb')), at(12, 30), { events: [report] }).rows).toEqual([])
    expect(fy(st(s('kb')), at(7, 40), { events: [report], read: new Set(['nr-1']) }).rows).toEqual([])
  })
  it('la notte solo dalle 20 e con la notte attiva', () => {
    const on = { ...st(s('kb')), night: { queued: 0, items: [] } }
    expect(fy(on, at(19, 59)).rows).toEqual([])
    const f = fy(on, at(20))
    expect(kinds(f)).toEqual(['night']); expect(f.rows[0].number).toBe(0)
    expect(fy({ ...st(s('kb')), night: { queued: 0, items: null } }, at(21)).rows).toEqual([])
  })
  const withRecap = (state: State, date: string, items: { project: string; done: string; next?: string }[], projects = state.projects) => ({ ...state, recap: { date, items }, projects })
  it('prossimo passo solo per i progetti senza sessione', () => {
    const items = [{ project: 'kb', done: 'nota scritta', next: 'distillare la nota' }, { project: 'atlas', done: 'test', next: 'pubblicare' }]
    const projects = [{ path: '/w/kb', name: 'kb', account: 'personale' }, { path: '/w/atlas', name: 'atlas', account: 'personale' }]
    expect(fy(withRecap(st(s('kb'), s('atlas', 'busy')), '2026-10-02', items, projects), at(15)).rows).toEqual([])
    const closed = fy(withRecap(st(), '2026-10-02', items, projects), at(15))
    expect(kinds(closed)).toEqual(['next_step', 'next_step'])
    expect([closed.rows[0].title, closed.rows[0].detail, closed.rows[0].project]).toEqual(['kb', 'distillare la nota', '/w/kb'])
    expect(closed.rows.map(r => r.session ?? null)).toEqual([null, null])
  })
  it('il nome della cartella e la master contano come sessione', () => {
    const items = ['claude-master-phone', 'master', 'chrome-bridge', 'fable-director'].map((project, i) => ({ project, done: 'x', next: ['merge', 'Store', 'Store', 'rilettura'][i] }))
    const state = withRecap(st({ ...s('claude-master-phone', 'busy'), project: 'personali/claude-master-phone' }, { ...s('master'), project: 'workspaces' }, { ...s('chrome-bridge', 'gone'), project: 'personali/chrome-bridge' }), '2026-10-03', items)
    expect(fy(state, at(15)).rows.map(r => r.title)).toEqual(['chrome-bridge', 'fable-director'])
  })
  it('un recap vecchio non conta, uno di ieri sì', () => {
    expect(fy(withRecap(st(), '2026-09-29', [{ project: 'kb', done: 'x', next: 'y' }]), at(15)).rows).toEqual([])
    expect(kinds(fy(withRecap(st(), '2026-10-01', [{ project: 'kb', done: 'x', next: 'y' }]), at(9)))).toEqual(['next_step'])
  })
  it('un prossimo passo avviato non torna', () => {
    const state = withRecap(st(), '2026-10-02', [{ project: 'kb', done: 'nota scritta', next: 'distillare la nota' }])
    expect(fy(state, at(15), { read: new Set([nextKey('kb', 'distillare la nota')]) }).rows).toEqual([])
  })
  it('i programmati: quanti e il primo', () => {
    const sent = [{ session: 'kb', sentAt: at(14), scheduledFor: at(16) }, { session: 'kb', sentAt: at(14), scheduledFor: at(15, 30) }]
    const f = fy(st(s('kb')), at(15), { sent })
    expect(kinds(f)).toEqual(['scheduled']); expect(f.rows[0].number).toBe(2); expect(f.rows[0].at).toBe(at(15, 30))
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
    expect(h).toEqual({ headline: 'Fase 2.2 avviata', body: 'Lanciata la sessione.', steps: ['distilla nella kb', 'prova la casa'], blocking: new Set(), at: at(6) })
  })
  it('i Prossimi col «!» sbloccano (contratto 1.38)', () => {
    const h = hero([claude('Fatto.\n\nEsito: piano pronto\nProssimi: !approva il piano · codice della 1.37', at(6))], s('master'))!
    expect(h.steps).toEqual(['approva il piano', 'codice della 1.37'])
    expect([...h.blocking]).toEqual(['approva il piano'])
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
  it('partito un turno dopo l\'esito, la card al lavoro dice cosa fa adesso (Franz, 08/10 14:03)', () => {
    const x = { ...s('a', 'awaiting'), outcome: { short: 'Fatto', full: 'Prossimi: ok, procedi con 1 e 2', at: 1000 }, turn_started: 1060, tool: 'Bash', tool_note: 'git merge' }
    expect(workingText(x)).toBe('Bash · git merge')
    expect(workingText({ ...x, tool: null, tool_note: null })).toBeNull()
    expect(workingText({ ...x, turn_started: 900 })).toBe('Prossimi: ok, procedi con 1 e 2')
  })
})
