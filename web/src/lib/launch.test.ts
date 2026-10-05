import { describe, expect, it } from 'vitest'
import { decodeState, type Project, type Session, type State } from './contract'
import { isPersonal, isPersonalQuota, personal, projects, ranked, recent, resolve, sessions } from './launch'

const st = (o: Partial<State>): State => decodeState(JSON.stringify({ v: 1, ts: 0, host: 'pc', ...o }))
const p = (path: string, name: string, account: string, last_used: number | null): Project => ({ path, name, account, last_used })

// Gli stessi casi di AccountsTest.
describe('Accounts', () => {
  it('il tipo vince sul nome', () => { expect(personal('home', 'personal')).toBe(true); expect(personal('personale', 'work')).toBe(false) })
  it('senza tipo si ripiega sul nome', () => {
    expect(personal('personale', null)).toBe(true); expect(personal('Personale', null)).toBe(true); expect(personal('agenzia', null)).toBe(false)
  })
  it('un tipo sconosciuto non è personale', () => expect(personal('home', 'family')).toBe(false))
  it('il contratto porta il tipo di sessione e quota', () => {
    const s = st({
      sessions: [{ id: 'a', name: 'x', account: 'home', account_kind: 'personal', project: 'p', state: 'idle', since: 1 }],
      quota: { home: { h5: 10, kind: 'personal' }, office: { h5: 20, kind: 'work' } },
    })
    expect(isPersonal(s.sessions[0])).toBe(true)
    expect(isPersonalQuota('home', s.quota.home)).toBe(true)
    expect(isPersonalQuota('office', s.quota.office)).toBe(false)
  })
  // `resolve` non ha test in Kotlin: qui solo il ripiego descritto nel suo commento.
  it('resolve: scelto, poi personale, poi il primo', () => {
    const s = st({ quota: { zeta: { kind: 'work' }, Home: { kind: 'personal' }, alfa: { kind: 'work' } } })
    expect(resolve(s, 'ZETA')).toBe('zeta'); expect(resolve(s, 'x')).toBe('Home')
    expect(resolve(st({ quota: { zeta: { kind: 'work' }, alfa: { kind: 'work' } } }), 'x')).toBe('alfa')
    expect(resolve(st({}), 'x')).toBeNull()
  })
})

const base = st({ projects: [p('/a/kb', 'kb', 'personale', 50), p('/a/docs', 'docs', 'personale', 90), p('/w/kb', 'kb', 'lavoro', 99), p('/a/old', 'old', 'personale', null)] })
const many = st({ projects: [
  p('/p/atlas-shop', 'atlas-shop', 'personal', 10), p('/p/data-tools', 'data-tools', 'personal', 99),
  p('/w/clients/ledger-api', 'ledger-api', 'work', 50), p('/w/field-notes', 'field-notes', 'work', 70),
] })
const names = (l: { name: string }[]) => l.map(x => x.name)
const paths = (l: Project[]) => l.map(x => x.path)

// Gli stessi casi di LaunchSuggestTest.
describe('LaunchSuggest', () => {
  it('solo quell\'account, il più recente prima', () => expect(paths(projects(base, 'personale', ''))).toEqual(['/a/docs', '/a/kb', '/a/old']))
  it('il testo filtra per pezzo di nome', () => expect(paths(projects(base, 'personale', 'K'))).toEqual(['/a/kb']))
  it('il prefisso prima del contenuto', () => expect(names(ranked(many, 'at'))).toEqual(['atlas-shop', 'data-tools']))
  it('il percorso per ultimo', () => expect(names(ranked(many, 'clients'))).toEqual(['ledger-api']))
  it('senza filtro entrambi gli account', () => expect(new Set(ranked(many, '').map(x => x.account))).toEqual(new Set(['personal', 'work'])))
  it('il filtro per account regge', () => expect(names(ranked(many, '', { account: 'work' }))).toEqual(['field-notes', 'ledger-api']))
  it('vuoto dà i più recenti', () => expect(names(ranked(many, '', { limit: 2 }))).toEqual(['data-tools', 'field-notes']))
  it('maiuscole e spazi ignorati', () => expect(names(ranked(many, '  Atlas '))).toEqual(['atlas-shop']))

  it('i recenti hanno solo chi ha una data', () => {
    expect(paths(recent(base, 'personale'))).toEqual(['/a/docs', '/a/kb'])
    expect(paths(recent(base, null))).toEqual(['/w/kb', '/a/docs', '/a/kb'])
  })

  it('le sessioni per nome', () => {
    const s = (id: string, name: string, project: string, state: Session['state']): Session => ({ id, name, account: 'personale', project, state, since: 0 })
    const x = st({ sessions: [s('1', 'claude-master', 'claude-master', 'idle'), s('2', 'master', 'workspaces', 'gone'), s('3', 'kb', 'kb', 'idle')] })
    expect(names(sessions(x, 'master'))).toEqual(['master', 'claude-master'])
    expect(names(sessions(x, ''))).toEqual([])
  })

  it('ricerca e recenti usano l\'elenco completo se c\'è', () => {
    const full = [...many.projects, p('/w/zeta-site', 'zeta-site', 'work', 5)]
    expect(names(ranked(many, 'zeta', { pool: full }))).toEqual(['zeta-site'])
    expect(recent(many, null, { pool: full }).at(-1)!.name).toBe('zeta-site')
    expect(names(ranked(many, 'zeta'))).toEqual([])
  })
})
