import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { age, build } from './summary'
import type { Session, State } from './contract'
import { parseSteps } from './nextSteps'
import { t } from './t'

const st = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))

describe('home', () => {
  it('scrive le età corte', () => {
    expect(age(0, 300)).toBe('5 m'); expect(age(0, 3 * 3600)).toBe('3 h'); expect(age(0, 2 * 86400)).toBe('2 g')
  })
})

// Gli stessi casi di NextStepsTest in Kotlin.
describe('Prossimi', () => {
  it("l'ultima riga diventa consigli", () => {
    const r = parseSteps("Ho pushato.\n\nEsito: push fatto.\n\nProssimi: apri l'app · scrivi il piano · correggi Lancia")
    expect(r.text).toBe('Ho pushato.\n\nEsito: push fatto.')
    expect(r.steps).toEqual(["apri l'app", 'scrivi il piano', 'correggi Lancia'])
  })
  it('al massimo tre e non troppo lunghi', () => {
    expect(parseSteps(`Fatto.\nProssimi: uno · ${'x'.repeat(41)} · due · tre · quattro`).steps).toEqual(['uno', 'due', 'tre'])
    // Contratto 1.38: «!» = sblocca; si toglie dal testo e non conta nei 40 caratteri.
    const b = parseSteps(`Fatto.\n\nProssimi: !ok release claude-master 0.6.9 · aggiorna il changelog · !${'x'.repeat(40)}`)
    expect(b.steps).toEqual(['ok release claude-master 0.6.9', 'aggiorna il changelog', 'x'.repeat(40)])
    expect([...b.blocking]).toEqual(['ok release claude-master 0.6.9', 'x'.repeat(40)])
  })
  it('senza riga niente consigli', () => {
    expect(parseSteps('Fatto.\n\nEsito: tutto ok.')).toEqual({ text: 'Fatto.\n\nEsito: tutto ok.', steps: [], blocking: new Set() })
  })
})

// Gli stessi casi di SummaryTest in Kotlin.
describe('riepilogo', () => {
  const at = (h: number, m = 0) => Date.UTC(2026, 9, 3, h - 2, m) / 1000
  const s = (name: string, state: Session['state'] = 'idle', since = at(9)): Session => ({ id: name, name, account: 'personale', project: name, state, since })
  const q = (asked: number) => ({ id: '1', kind: 'ask' as const, text: 'Pubblico?', options: [], tier: 'low' as const, asked_at: asked })
  const done = (t: number) => ({ short: 'Fatto', full: 'Test verdi.\nProssimi: tagga · apri la PR', at: t })
  const st = (...ss: Session[]): State => ({ v: 1, ts: at(15), host: 'pc', sessions: ss, quota: {}, projects: [], night: { queued: 0 }, recap: { date: '', items: [] } })
  const b = (state: State) => build(state, [], at(15), new Set())

  it("nell'ordine del bisogno", () => {
    const m = b(st(s('idle'), s('busy', 'busy'), { ...s('asks', 'waiting'), question: q(at(14)) }, { ...s('fin'), outcome: done(at(14, 50)), followed: true }))
    expect(m.rows.map(r => r.group)).toEqual(['waiting', 'finished', 'working', 'still'])
    expect(m.rows.map(r => r.session.name)).toEqual(['asks', 'fin', 'busy', 'idle'])
  })
  it('ogni riga porta la quota del suo account', () => {
    const m = b({ ...st(s('idle'), { ...s('cli', 'busy'), account: 'professionale' }), quota: { personale: { h5: 4 }, professionale: { h5: 62, stale: true } } })
    expect(m.rows.map(r => r.quota?.h5)).toEqual([62, 4])
  })
  it('chi ha finito non è anche ferma', () => expect(b(st({ ...s('fin'), outcome: done(at(14, 50)), followed: true })).rows.map(r => r.group)).toEqual(['finished']))
  it('la master solo quando chiede', () => {
    expect(b(st(s('master', 'busy'))).rows).toEqual([])
    expect(b(st({ ...s('master', 'waiting'), question: q(at(14)) })).rows.map(r => r.session.name)).toEqual(['master'])
  })
  it('le chiuse a parte', () => {
    const m = b(st(s('a'), s('x', 'gone'), s('y', 'gone')))
    expect(m.closed.map(x => x.name)).toEqual(['x', 'y'])
  })
  it('aperte senza la master', () => expect(b(st(s('a'), s('b'), s('master'), s('x', 'gone'))).open).toBe(2))
  it('ferme dalla più recente', () => expect(b(st(s('old', 'idle', at(8)), s('new', 'idle', at(12)))).rows.map(r => r.session.name)).toEqual(['new', 'old']))
  it('in attesa senza domanda resta in lista', () => expect(b(st(s('w', 'waiting'))).rows.map(r => r.session.name)).toEqual(['w']))
  it('al lavoro con una domanda compare una volta', () => expect(b(st({ ...s('b', 'busy'), question: q(at(14)) })).rows.map(r => r.group)).toEqual(['waiting']))
})

// Gli stessi casi di PrepositionTest in Kotlin.
describe('preposizione', () => {
  it('ad solo davanti alla a', () => {
    expect(['atlas-shop', 'Atlas', 'àncora'].map(t.writeTo)).toEqual(['Scrivi ad atlas-shop', 'Scrivi ad Atlas', 'Scrivi ad àncora'])
    expect(['ledger-api', 'orbit-docs', 'Euro'].map(t.writeTo)).toEqual(['Scrivi a ledger-api', 'Scrivi a orbit-docs', 'Scrivi a Euro'])
    expect(t.writeTo('master')).toBe('Scrivi alla master')
  })
})
