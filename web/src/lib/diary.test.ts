import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import type { Event } from './contract'
import { lastNightReport, minutes, night, parseRecap, personalIcon, recap, recaps } from './diary'

const events: Event[] = JSON.parse(readFileSync(new URL('../../../contract/events-sample.json', import.meta.url), 'utf8'))

// Gli stessi casi di RecapSessionsTest in Kotlin.
describe('RecapSessions', () => {
  const body = [
    'Recap 02/10/2026 · 4 progetti · 1 ferme su domanda', '', 'FERME SU UNA DOMANDA', '',
    '🟧 ledger-api (https://claude.ai/code/session_01A) · AskUserQuestion',
    "     Deploy ready, waiting for the client's ok.", '', 'APERTE', '',
    '⚪ master (https://claude.ai/code/session_01B): La sessione rino è avviata, nuova.',
    '     ↳ prossimo: prova la casa dal vivo', '',
    '🟢 claude-master (https://claude.ai/code/session_01C): Contratto 1.26 fatto.', '', 'CHIUSE OGGI', '',
    '✓ sito.⁠com: Pagina dei prezzi rifatta.', '', 'altro: kb, bozze', '', 'Progetti: master 12, claude-master 9',
  ].join('\n')

  it('le sessioni stanno nelle loro sezioni', () => {
    const v = parseRecap(body)
    expect(v.title).toBe('Recap 02/10/2026 · 4 progetti · 1 ferme su domanda')
    expect(v.sections.map(s => s.kind)).toEqual(['waiting', 'open', 'closed', 'other'])
    const waiting = v.sections[0].entries[0]
    expect(v.sections[0].entries).toHaveLength(1)
    expect([waiting.name, waiting.icon, waiting.tool]).toEqual(['ledger-api', '🟧', 'AskUserQuestion'])
    expect(waiting.url).toBe('https://claude.ai/code/session_01A'); expect(waiting.detail).toBe("Deploy ready, waiting for the client's ok.")
    const master = v.sections[1].entries[0]
    expect([master.name, master.text, master.next]).toEqual(['master', 'La sessione rino è avviata, nuova.', 'prova la casa dal vivo'])
    expect(v.sections[1].entries.map(e => e.name)).toEqual(['master', 'claude-master'])
    const closed = v.sections[2]
    expect(closed.entries).toHaveLength(1)
    expect([closed.entries[0].name, closed.entries[0].text]).toEqual(['sito.com', 'Pagina dei prezzi rifatta.'])
    expect(closed.lines).toEqual(['altro: kb, bozze'])
    expect(v.sections[3].lines).toEqual(['Progetti: master 12, claude-master 9'])
  })

  it('anche il formato vecchio in inglese', () => {
    const v = parseRecap("Recap 12/09/2026 · 3 projects · 1 waiting on a question\n\nWAITING ON A QUESTION\n⏳ ledger-api · since 14:15 · AskUserQuestion\n   Deploy ready, waiting for the client's ok.\n\nOPEN\n● atlas-shop · 6 turns · last 13:45\n   Migrations 008-011 applied, tests green")
    expect(v.sections.map(s => s.kind)).toEqual(['waiting', 'open'])
    expect(v.sections[0].entries[0].tool).toBe('since 14:15 · AskUserQuestion')
    expect(v.sections[1].entries[0].detail).toBe('Migrations 008-011 applied, tests green')
  })

  it('la forma viene dall\'emoji', () => {
    expect(personalIcon('🟢')).toBe(true); expect(personalIcon('⚪')).toBe(true)
    expect(personalIcon('🟧')).toBe(false); expect(personalIcon('🟩')).toBe(false)
  })
})

// Gli stessi casi di RegistroTest.
describe('Registro', () => {
  it('il resoconto della notte è una riga per lavoro, senza percorsi', () => {
    const r = night(events.find(e => e.kind === 'night_report')!.body!)
    expect(r).toHaveLength(2)
    expect(r[0]).toEqual({ ok: true, project: 'atlas-shop', seconds: 812, text: 'Fixed the three flaky tests, two were real.' })
    expect(r[1]).toEqual({ ok: false, project: 'ledger-api', seconds: 3600, text: 'timed out (night.item_timeout_s = 3600)' })
  })

  it('un resoconto sconosciuto non dà righe', () => expect(night('Notte tranquilla, niente da dire')).toEqual([]))

  it('il recap è una riga per progetto', () => {
    const rows = recap(events.find(e => e.kind === 'recap')!.body!)
    expect(rows.map(r => r.project)).toEqual(['ledger-api', 'atlas-shop', 'field-notes'])
    expect(rows[1].text).toBe('Migrations 008-011 applied, tests green')
  })

  it('i minuti vanno per eccesso', () => { expect(minutes(812)).toBe(14); expect(minutes(3600)).toBe(60) })
})

// Gli stessi casi di PhoneDiaryTest.
describe('PhoneDiary', () => {
  const e = (key: string, kind: Event['kind'], ts: number, ref: string | null, body = key): Event => ({ key, kind, ts, title: key, body, ref })
  const ev = [
    e('r1', 'recap', 100, '2026-09-27'), e('r2', 'recap', 200, '2026-09-28'), e('r2b', 'recap', 250, '2026-09-28', 'resent'),
    e('n1', 'night_report', 150, '2026-09-28'), e('n2', 'night_report', 300, '2026-09-29'), e('q', 'quota', 400, null),
  ]

  it('un recap per giorno, vince l\'ultimo invio, il giorno più nuovo prima', () => {
    const r = recaps(ev)
    expect(r.map(x => x.ref)).toEqual(['2026-09-28', '2026-09-27'])
    expect(r[0].body).toBe('resent')
  })
  it('l\'ultimo resoconto della notte', () => expect(lastNightReport(ev)!.key).toBe('n2'))
  it('senza eventi niente diario', () => { expect(recaps([])).toEqual([]); expect(lastNightReport([])).toBeNull() })
})
