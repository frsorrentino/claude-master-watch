import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState, type Session, type State } from './contract'
import { MASTER } from './summary'
import {
  adviceOf, approveText, canClear, cleanupOf, closeStateOf, clearDue, ctxBand, ctxNudge, decisionDraft, decisionProject, DECISION_MAX, handoffPrompt, tokens,
} from './masterService'

const st: State = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const atlas = st.sessions.find(s => s.name === 'atlas-shop')!
const field = st.sessions.find(s => s.name === 'field-notes')!
const at = atlas.advice!.at

describe('A. consiglio di modello ed effort', () => {
  it('consigliati, puntino e costo a metà lavoro', () => {
    expect(adviceOf(atlas, st.choices, at + 60)).toEqual({
      model: 'claude-fable-5-1', effort: 'high', reason: atlas.advice!.reason, dot: true, cost: 36000,
    })
  })
  it('a contesto fresco niente costo; senza differenza niente puntino', () => {
    const s = { ...atlas, advice: { ...atlas.advice!, when: 'now' as const, differs: false } }
    expect(adviceOf(s, st.choices, at)).toMatchObject({ dot: false, cost: null })
  })
  it('vecchio di 6 ore, o fuori dalle scelte: non si mostra', () => {
    expect(adviceOf(atlas, st.choices, at + 6 * 3600)).toBeNull()
    expect(adviceOf({ ...atlas, advice: { ...atlas.advice!, model: 'claude-x' } }, st.choices, at)).toBeNull()
    expect(adviceOf({ ...atlas, advice: { ...atlas.advice!, effort: 'turbo' } }, st.choices, at)).toBeNull()
    expect(adviceOf(field, st.choices, at)).toBeNull()
  })
  it('i token come si scrivono in italiano', () => {
    expect(tokens(36000)).toBe('36.000')
  })
})

describe('B. approvazioni', () => {
  it('senza nota vale «ok»', () => {
    expect(approveText('  ')).toBe('ok')
    expect(approveText(' vai pure ')).toBe('vai pure')
  })
})

describe('C. contesto pieno', () => {
  it('fasce 60, 70, 80', () => {
    expect([59, 60, 69, 70, 85, null].map(ctxBand)).toEqual([null, 60, 60, 70, 80, null])
  })
  it('proposta con sessione ferma oltre il 60 %, chiusa torna alla fascia dopo', () => {
    const s = { ...field, state: 'idle' as const, context: 64 }
    expect(ctxNudge(s, null)).toBe(60)
    expect(ctxNudge(s, 60)).toBeNull()
    expect(ctxNudge({ ...s, context: 72 }, 60)).toBe(70)
    expect(ctxNudge({ ...s, state: 'busy' as const }, null)).toBeNull()
    expect(ctxNudge({ ...s, context: 40 }, null)).toBeNull()
  })
  it('alla master la ricorrente master-handoff, alle altre il prompt di sempre', () => {
    const master: Session = { ...field, name: MASTER }
    const withRec = { ...st, recurring: [...(st.recurring ?? []), { id: 'master-handoff', label: 'Handoff', prompt: 'fai il master handoff' }] }
    expect(handoffPrompt(withRec, master, 'altro')).toBe('fai il master handoff')
    expect(handoffPrompt(withRec, field, 'altro')).toBe('altro')
    expect(handoffPrompt(st, master, 'altro')).toBe('altro')
  })
  it('/clear solo se permesso, e a turno finito con un esito più nuovo dell\'invio', () => {
    expect(canClear(st)).toBe(true)
    expect(canClear({ ...st, slash: ['compact'] })).toBe(false)
    const sent = 1000
    expect(clearDue({ ...field, state: 'busy', outcome: { short: 'x', full: 'x', at: 900 } }, sent)).toBe(false)
    expect(clearDue({ ...field, state: 'idle', outcome: { short: 'x', full: 'x', at: 900 } }, sent)).toBe(false)
    expect(clearDue({ ...field, state: 'idle', outcome: { short: 'x', full: 'x', at: 1100 } }, sent)).toBe(true)
    expect(clearDue(undefined, sent)).toBe(false)
  })
})

describe('D. salva come decisione', () => {
  it('il progetto è l\'ultimo pezzo del percorso', () => {
    expect(decisionProject(atlas)).toBe('atlas-shop')
  })
  it('la bozza toglie Prossimi e Watch, e sta nei 2000 caratteri senza puntini', () => {
    const d = decisionDraft('I prezzi includono l\'IVA.\n\nProssimi: a · b\nWatch: ok')
    expect(d).toBe('I prezzi includono l\'IVA.')
    const long = decisionDraft('parola '.repeat(400))
    expect(long.length).toBeLessThanOrEqual(DECISION_MAX)
    expect(long.endsWith('…')).toBe(false)
    expect(long.endsWith(' ')).toBe(false)
  })
})

describe('E. pulizia', () => {
  it('ferma col compito chiuso: «Chiudi» solo senza finestra', () => {
    expect(cleanupOf({ ...field, state: 'idle', attached: false }, true)).toEqual({ kind: 'finished', of: null, canClose: true })
    expect(cleanupOf({ ...field, state: 'idle', attached: true }, true)).toEqual({ kind: 'finished', of: null, canClose: false })
  })
  it('un doppione, anche senza compito chiuso', () => {
    expect(cleanupOf({ ...field, finished: false, duplicate_of: 'field-notes', name: 'field-notes-2', state: 'idle' }, true))
      .toEqual({ kind: 'duplicate', of: 'field-notes', canClose: true })
  })
  it('mai a metà turno, mai senza /exit permesso', () => {
    expect(cleanupOf({ ...field, state: 'busy' }, true)).toBeNull()
    expect(cleanupOf({ ...field, state: 'idle', attached: false }, false)).toEqual({ kind: 'finished', of: null, canClose: false })
    expect(cleanupOf({ ...atlas, finished: false }, true)).toBeNull()
  })
})

describe('closeStateOf', () => {
  const base = { name: 'rino', state: 'idle', finished: false, duplicate_of: null } as unknown as Session
  it('dice che chi ha finito è ancora aperta, con l\'ora del compito', () => {
    expect(closeStateOf({ ...base, finished: true, outcome: { at: 1000 } } as Session)).toEqual({ kind: 'finished', of: null, at: 1000 })
  })
  it('il doppione nomina la sessione originale', () => {
    expect(closeStateOf({ ...base, duplicate_of: 'rino-1' })).toEqual({ kind: 'duplicate', of: 'rino-1', at: null })
  })
  it('chi lavora o aspetta è al lavoro, prima di doppione e finita', () => {
    expect(closeStateOf({ ...base, state: 'busy', finished: true }).kind).toBe('working')
    expect(closeStateOf({ ...base, state: 'waiting' }).kind).toBe('working')
  })
  it('altrimenti ferma', () => { expect(closeStateOf(base).kind).toBe('still') })
})
