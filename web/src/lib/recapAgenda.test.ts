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
