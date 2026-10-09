import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { AgendaPage } from './contract'
import { agendaModel, doIt, mark } from './recapAgenda'

// Gli stessi casi di RecapAgendaTest in Kotlin.
describe('recapAgenda', () => {
  const root = JSON.parse(readFileSync(new URL('../../../contract/cmd-result-sample.json', import.meta.url), 'utf8'))
  const page: AgendaPage = JSON.parse(root.result.find((r: { id: string }) => r.id.endsWith('0386')).text)
  it('le aperte per chi deve muoversi, le altre in fondo', () => {
    const m = agendaModel(page)
    expect(m.you.map(r => r.title)).toEqual(['Confirm the 6 client ids with a candidate'])
    expect(m.claude.map(r => r.title)).toEqual(['Move the docs site to the new host'])
    expect(m.other).toEqual([])
    expect(m.rest.map(r => r.state)).toEqual(['sospeso', 'fatto'])
  })
  it('filtro per ambito, segni e valori sconosciuti', () => {
    const rows = [...page.rows, { state: 'chiuso', scope: 'personale', blocks: 'franz', title: 'Old thing', ref: '' },
      { state: 'aperto', scope: 'Agenzia', blocks: 'terzi', title: 'Client answer on the domain', ref: '' }, { state: 'stato', scope: 'ambito', blocks: 'blocca', title: 'titolo', ref: 'rif' }]
    const m = agendaModel({ rows, more: false }, 'agenzia')
    expect(m.other.map(r => r.title)).toEqual(['Client answer on the domain'])
    expect(m.rest.map(r => r.title)).toEqual(['Staging of atlas-shop not reachable from the CLI'])
    expect(agendaModel({ rows, more: false }).rest.map(r => r.state)).toEqual(['sospeso', 'fatto', 'chiuso'])
    expect([mark('Agenzia'), mark('postazione'), mark('personale'), mark('altro')]).toEqual(['agency', 'desk', 'personal', 'none'])
  })
  it('«Fallo» va alla master col rimando', () => {
    const a = doIt(agendaModel(page).claude[0])
    expect(a.to).toBe('master'); expect(a.viaMaster && a.agenda).toBe(true)
    expect(a.send).toBe("Fai questo lavoro dell'agenda: Move the docs site to the new host (orbit-docs)")
    expect(doIt({ state: 'aperto', scope: '', blocks: 'claude', title: 'X', ref: ' ' }).send).toBe("Fai questo lavoro dell'agenda: X")
  })
})

// Le Azioni sulle schede (piano approvato il 09/10, contratto 1.47): gli stessi casi di RecapAgendaActionsTest.
import { deepenText, menu, nextWeek, talkText, tomorrow, url } from './recapAgenda'
describe('azioni del recap', () => {
  const today = '2026-10-09'
  const row = (state = 'aperto', blocks = 'franz', ref = '', until: string | null = null) => ({ state, scope: 'agenzia', blocks, title: 'Revoke the API key', ref, detail: null, until })
  it('le rimandate tornano il loro giorno, le scartate spariscono', () => {
    const m = agendaModel({ rows: [row('sospeso', 'franz', '', '2026-10-09'), row('sospeso', 'franz', '', '2026-10-10'), row('sospeso'), row('scartato'), row('chiuso'), row('fatto')], more: false }, null, today)
    expect(m.you).toHaveLength(1)
    expect(m.rest.map(r => r.state)).toEqual(['sospeso', 'sospeso', 'chiuso', 'fatto'])
  })
  it('il menu ha le scritture solo con l\'op e Passa va dall\'altra parte', () => {
    expect(menu(row(), false)).toEqual(['deepen', 'do', 'talk'])
    expect(menu(row('aperto', 'franz', 'orbit-docs'), true)).toEqual(['deepen', 'do', 'talk', 'open_ref', 'done', 'postpone', 'pass_claude', 'remove'])
    expect(menu(row('aperto', 'claude'), true)).toContain('pass_me')
    expect(menu(row('fatto'), true)).toEqual(['deepen', 'talk', 'remove'])
  })
  it('testi, indirizzi e date', () => {
    expect(deepenText(row('aperto', 'franz', 'console.anthropic.com'))).toBe('Approfondisci: Revoke the API key (console.anthropic.com)')
    expect(talkText(row('aperto', 'franz', 'x.md'))).toBe('Sulla scheda «Revoke the API key» (x.md): ')
    expect(url('console.anthropic.com -> API Keys')).toBe('https://console.anthropic.com')
    expect([url('.claude/DA-DECIDERE-client-id.md'), url('orbit-docs'), url('tag-runtime.json')]).toEqual([null, null, null])
    expect([tomorrow(today), nextWeek(today), nextWeek('2026-10-12')]).toEqual(['2026-10-10', '2026-10-12', '2026-10-19'])
  })
})
