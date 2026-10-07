import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { MIN_BAR, page as pageOf, shortTitle, type NightReport } from './night'

// La pagina Notte dal rapporto (specifica del 07/10, approvata alle 21:50): gli stessi casi di NightPageTest.kt.
const report: NightReport = JSON.parse(readFileSync(new URL('../../../contract/night-report-sample.json', import.meta.url), 'utf8'))
const p = pageOf(report, 'Europe/Rome')

describe('pagina Notte', () => {
  it('la notte prende il nome dai suoi due giorni', () => {
    expect([p.dayBefore, p.day]).toEqual(['2026-10-06', '2026-10-07'])
    expect(p.fromLastMessage).toBe(true)
    expect(p.windowS).toBe(2 * 3600 + 3 * 60)
  })
  it('una card per voce, nell\'ordine del file', () =>
    expect(p.cards.map(c => c.id)).toEqual(['master', 'ledger-api', 'atlas-shop', 'a1b2c3d4', 'e5f6a7b8', 'c9d0e1f2']))
  it('l\'icona dice l\'esito come Telegram', () =>
    expect(p.cards.map(c => c.icon)).toEqual(['running', 'question', 'ok', 'stopped', 'ok', 'running']))
  it('una voce in corso non ha fine e arriva in fondo all\'asse', () => {
    const m = p.cards[0]
    expect(m.end).toBeNull(); expect(m.durationS).toBeNull(); expect(m.from).toBeCloseTo(0); expect(m.to).toBeCloseTo(1)
  })
  it('una voce chiusa sta sull\'asse con la sua durata', () => {
    const a = p.cards[2]
    expect(a.durationS).toBe(64 * 60); expect(a.from).toBeCloseTo(3 / 123, 6); expect(a.to).toBeCloseTo(67 / 123, 6)
  })
  it('un lavoro di un minuto resta un punto', () => expect(p.cards[3].to - p.cards[3].from).toBeGreaterThanOrEqual(MIN_BAR - 1e-9))
  it('i titoli della coda senza cartella e senza la fine tagliata', () => {
    const j = p.cards[3]
    expect(j.queue).toBe(true); expect(j.title).toBe('Prima esecuzione del lavoro notturno sul checkout'); expect(j.folder).toBe('atlas-shop-notte')
    expect(j.title).not.toContain('…')
    expect(p.cards[4].title).toBe('Dati del registro dei pagamenti per la pagina mensile')
  })
  it('le sessioni tengono il nome e aprono la chat solo se vive', () => {
    expect(p.cards[1].title).toBe('ledger-api'); expect(p.cards[1].folder).toBeNull(); expect(p.cards[1].chat).toBe('ledger-api')
    expect(p.cards[2].chat).toBeNull(); expect(p.cards[3].chat).toBeNull()
  })
  it('i passi sono gli eventi del progetto dentro la voce', () => expect(p.cards[1].steps.map(s => s.kind)).toEqual(['commit', 'test', 'outcome']))
  it('l\'asse sulle ore piene e mezze', () => expect(p.axis.map(t => t.label)).toEqual(['01:00', '01:30', '02:00', '02:30']))
  it('quello che serve a Franz viene prima, con la domanda', () => {
    expect(p.needs.map(n => n.kind)).toEqual(['question', 'approval', 'unblock']); expect(p.needs[2].session).toBe('field-notes')
  })
  it('i progetti con le parti e quello che aspettano', () => {
    expect([p.projects[0].done, p.projects[0].total]).toEqual([3, 4])
    expect(p.projects[0].waiting).toEqual(['Pubblico la release 1.4 adesso?', 'Release 1.4 di ledger-api'])
    expect(p.projects[1].waiting).toEqual(['/clear'])
  })
  it('i titoli corti restano', () => expect(shortTitle('Riordino delle note')).toBe('Riordino delle note'))
  it('un titolo tagliato finisce all\'ultima frase o parola intera', () => {
    expect(shortTitle('Seconda esecuzione della voce d17d7b00. Alle 02:01 la prima si è fermata perché le fonti stavano fuori dal worktree e i permessi ne negavano la lett…')).toBe('Seconda esecuzione della voce d17d7b00')
    expect(shortTitle('Un titolo lungo senza punti che il relay ha tagliato a metà di una parola lungh…')).toBe('Un titolo lungo senza punti che il relay ha tagliato a metà di una parola')
  })
})
