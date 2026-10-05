import { describe, expect, it } from 'vitest'
import type { CmdResult, Question, Session } from './contract'
import { append, box, button, mode, slashConfirm, slashPanel, slashParse, slashSuggest, suggestion, target } from './composer'

// Gli stessi casi di PhonePrimaryTest, NextStepsTest (box e append) e SlashTest in Kotlin.
const q: Question = { id: 'q1', kind: 'ask', text: 'Quale?', options: [{ n: 1, label: 'A' }], tier: 'medium', asked_at: 0 }
const s = (state: Session['state'], question: Question | null = null): Session => ({ id: '1', name: 'kb', account: 'personale', project: 'p', state, since: 0, question })
const ops = ['prompt', 'interrupt']

describe('tasto e destinazione', () => {
  it('campo vuoto niente tasto, tranne chiusa', () => { expect(button(s('idle'), '  ')).toBe('none'); expect(button(s('gone'), '')).toBe('reopen') })
  it('testo = invia', () => expect(button(s('idle'), 'vai')).toBe('send'))
  it('una chiusa non manda mai', () => { expect(button(s('gone'), 'vai')).toBe('reopen'); expect(target(s('gone'), 'vai')).toBeNull() })
  it('risposta solo finché la domanda c\'è', () => { expect(target(s('waiting', q), 'B')).toBe('answer_text'); expect(target(s('idle'), 'B')).toBe('prompt') })
  it('la prima opzione è il tasto pieno', () => expect(button(s('waiting', q), 'anche testo')).toBe('option'))
  it('domanda senza opzioni: invia', () => expect(button(s('waiting', { ...q, options: [] }), 'B')).toBe('send'))
  it('Stop mentre lavora col campo vuoto', () => expect(mode(s('busy'), '', ops)).toBe('stop'))
  it('scrivendo torna Invia', () => expect(mode(s('busy'), 'and also', ops)).toBe('send'))
  it('niente Stop senza il relay', () => { expect(mode(s('busy'), '', null)).toBe('none'); expect(mode(s('busy'), '', ['prompt'])).toBe('none') })
  it('Invia tonale con la prima opzione piena', () => expect(mode(s('waiting', q), 'B', ops)).toBe('send_tonal'))
  it('chiusa: Riapri', () => expect(mode(s('gone'), 'text', ops)).toBe('reopen'))
  it('ferma col campo vuoto: niente', () => expect(mode(s('idle'), ' ', ops)).toBe('none'))
  it('suggerimento solo da ferma col campo vuoto', () => {
    const idle = { ...s('idle'), suggestion: 'pubblicato, controlla' }
    expect(suggestion(idle, '')).toBe('pubblicato, controlla')
    expect(suggestion({ ...idle, state: 'busy' }, '')).toBeNull()
    expect(suggestion({ ...idle, state: 'awaiting' }, '')).toBeNull()
    expect(suggestion(idle, 'scrivo altro')).toBeNull()
    expect(suggestion({ ...idle, question: q, state: 'waiting' }, '')).toBeNull()
    expect(suggestion({ ...idle, suggestion: '  ' }, '')).toBeNull()
  })
})

describe('box Prossimi', () => {
  it('il primo va nel campo e la copia del terminale sparisce', () =>
    expect(box(['Prova dal vivo', 'Apri la CI', 'Scrivi il piano'], 'prova dal vivo.', '')).toEqual({ field: 'Prova dal vivo', rows: ['Apri la CI', 'Scrivi il piano'] }))
  it('un suggerito diverso è l\'ultima riga', () => expect(box(['Prova dal vivo', 'Apri la CI'], 'Committa', '').rows).toEqual(['Apri la CI', 'Committa']))
  it('senza consigli il suggerito resta nel campo', () => {
    expect(box([], 'Committa', '')).toEqual({ field: 'Committa', rows: [] })
    expect(box([], 'Committa', 'scrivo altro')).toEqual({ field: null, rows: [] })
  })
  it('una riga nel campo esce dal box e quella del campo torna', () =>
    expect(box(['Prova dal vivo', 'Apri la CI', 'Scrivi il piano'], null, 'Fai il merge e poi apri la CI')).toEqual({ field: null, rows: ['Prova dal vivo', 'Scrivi il piano'] }))
  it('una riga toccata si accoda con «e poi»', () => {
    expect(append('', 'Prova dal vivo')).toBe('Prova dal vivo')
    expect(append('Fai il merge.', 'Prova dal vivo')).toBe('Fai il merge e poi prova dal vivo')
    expect(append('Fai il merge', 'CI verde')).toBe('Fai il merge e poi CI verde')
  })
})

describe('slash', () => {
  const allowed = ['compact', 'clear', 'exit', 'context', 'cost']
  it('suggerisce scrivendo il nome', () => {
    expect(slashSuggest('/', allowed)).toEqual(allowed)
    expect(slashSuggest('/c', allowed).filter(c => c !== 'clear')).toEqual(['compact', 'context', 'cost'])
    expect(slashSuggest('/cl', allowed)).toEqual(['clear'])
    expect(slashSuggest('/compact tieni le decisioni', allowed)).toEqual([])
    expect(slashSuggest('ciao', allowed)).toEqual([])
    expect(slashSuggest('/', null)).toEqual([])
  })
  it('legge un comando consentito con gli argomenti', () => {
    expect(slashParse('/compact tieni le decisioni', allowed)).toEqual({ cmd: 'compact', args: 'tieni le decisioni' })
    expect(slashParse('  /exit  ', allowed)).toEqual({ cmd: 'exit', args: null })
  })
  it('tutto il resto resta un prompt', () => {
    expect(slashParse('/deploy subito', allowed)).toBeNull()
    expect(slashParse('usa /tmp per i file', allowed)).toBeNull()
    expect(slashParse('/compact', null)).toBeNull()
  })
  it('clear ed exit chiedono prima', () => { expect(slashConfirm('clear')).toBe(true); expect(slashConfirm('exit')).toBe(true); expect(slashConfirm('compact')).toBe(false) })
  const cost = { id: 'c1', session: 'kb', text: '/cost', sentAt: 1 }
  const ok = (text: string): CmdResult => ({ id: 'c1', ok: true, text, at: 2 })
  it('il pannello dopo la riga di invio', () => {
    expect(slashPanel(cost, ok('sent /cost to kb\n\n   Session\n   Total cost:            $0.42'))).toBe('Session\nTotal cost: $0.42')
    expect(slashPanel(cost, ok('sent /cost to kb\n\n   Session\n\n(the panel is still open on the PC)'))).toBe('Session\n(the panel is still open on the PC)')
  })
  it('il pannello senza quello che non serve', () => {
    const raw = 'sent /cost to kb\n\nSettings  Status   Config   Usage   Stats\n   Session\n   Total cost:            $0.42\n' +
      '   Prompt cache (main):   12 requests · 98% of input tokens from cache\n' +
      "   Plugin skill-listing footprint\n   What each plugin's skill descriptions add to the system prompt (cached input after the first turn).\n" +
      '   alpha                       7 skills · ~300 tok/turn\n   beta                        1 skill · ~52 tok/turn\n' +
      '   Total                       ~352 tok/turn\n   Current session\n                                          0% used\n' +
      '   Resets 9pm (Europe/Rome)\n                                                        ↓'
    expect(slashPanel(cost, ok(raw))).toBe('Session\nTotal cost: $0.42\nPrompt cache (main): 12 requests · 98% of input tokens from cache\nCurrent session\n0% used\nResets 9pm (Europe/Rome)')
  })
  it('niente pannello senza', () => {
    expect(slashPanel(cost, ok('sent /compact to kb'))).toBeNull()
    expect(slashPanel(cost, { id: 'c1', ok: false, text: 'kb is busy\n\nlater', at: 2 })).toBeNull()
    expect(slashPanel(cost, null)).toBeNull()
    expect(slashPanel({ ...cost, text: 'ciao' }, ok('delivered\n\nsomething'))).toBeNull()
  })
})
