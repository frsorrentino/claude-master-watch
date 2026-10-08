import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { decodeState } from './contract'
import { chunks, clean, excerpt, rankVoices, nextVoice, nextRate, question, RATE_MAX, RATE_MIN, rateFraction, rateOf, snapRate, voicePosition } from './speechRules'

// Gli stessi casi di SpeechTextTest, SpeechRateTest, VoiceRulesTest e ReadingBarTest in Kotlin.
const code = 'segue un blocco di codice'
describe('testo per la voce', () => {
  it('il codice si annuncia e non si legge', () => {
    const t = clean('Ecco la patch:\n```kotlin\nval x = 1\n```\nFatto.', code)
    expect(t).not.toContain('```'); expect(t).not.toContain('val x'); expect(t).toContain(code); expect(t.endsWith('Fatto.')).toBe(true)
  })
  it('il markdown non si legge a simboli', () =>
    expect(clean('## Stato\n- **tile** rifatta\n- `quotaLine` nuova\nVedi [il piano](docs/plans/x.md).', code)).toBe('Stato.\ntile rifatta.\nquotaLine nuova.\nVedi il piano.'))
  it('le tabelle diventano elenchi', () => expect(clean('| conto | 5 ore |\n|---|---|\n| personale | 9 % |', code)).toBe('conto, 5 ore.\npersonale, 9 %.'))
  it('un testo corto è un pezzo solo', () => expect(chunks('Tutto a posto.')).toEqual(['Tutto a posto.']))
  it('i pezzi si tagliano a fine frase', () => expect(chunks('Uno due tre. Quattro cinque sei. Sette.', 30)).toEqual(['Uno due tre.', 'Quattro cinque sei. Sette.']))
  it('un testo lungo non perde niente', () => {
    const testo = Array.from({ length: 400 }, (_, i) => `Frase numero ${i + 1} della risposta.`).join(' ')
    const pezzi = chunks(testo, 500)
    expect(pezzi.length).toBeGreaterThan(1); expect(pezzi.every(p => p.length <= 500)).toBe(true); expect(pezzi.join(' ')).toBe(testo)
  })
  it('una frase senza punti si taglia a uno spazio', () => {
    const frase = Array.from({ length: 200 }, (_, i) => `parola${i + 1}`).join(' ')
    const pezzi = chunks(frase, 100)
    expect(pezzi.length).toBeGreaterThan(1); expect(pezzi.every(p => p.length <= 100)).toBe(true); expect(pezzi.join(' ')).toBe(frase)
  })
  it('la domanda con le opzioni numerate', () => {
    const q = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8')).sessions.find(s => s.question)!.question!
    expect(question(q)).toBe(`${q.text} ${q.options.map(o => `${o.n}, ${o.label}.`).join(' ')}`)
  })
})
describe('velocità', () => {
  // Dal 07/10 (approvata alle 22:06) è continua, come SpeechRate.kt: slider da 0,5× a 2× a passi di 0,05, scatto su 1×.
  it('senza scelta quella del motore, una scelta salvata resta', () => {
    expect(rateOf(null)).toBe(1); expect(rateOf(0.9)).toBe(0.9); expect(rateOf(1.35)).toBe(1.35); expect(rateOf(1.25)).toBe(1.25)
  })
  it('un tocco passa alla velocità dopo, e dopo 2× torna a 0,75×', () => {
    expect(nextRate(1)).toBe(1.25); expect(nextRate(1.3)).toBe(1.5); expect(nextRate(2)).toBe(0.75); expect(nextRate(0.5)).toBe(0.75); expect(nextRate(0.75)).toBe(1)
  })
  it('a passi di 0,05', () => { expect(snapRate(0.86)).toBe(0.85); expect(snapRate(1.41)).toBe(1.4); expect(rateOf(1.2)).toBe(1.2) })
  it('vicino a 1 si ferma su 1, 1,05 resta raggiungibile', () => { expect(snapRate(1.03)).toBe(1); expect(snapRate(0.97)).toBe(1); expect(snapRate(1.05)).toBe(1.05) })
  it('fuori dalla corsa o rovinato', () => { expect(rateOf(5)).toBe(RATE_MAX); expect(rateOf(0.1)).toBe(RATE_MIN); expect(rateOf(NaN)).toBe(1); expect(rateOf(-1)).toBe(1) })
  it('la corsa va da metà al doppio', () => { expect(RATE_MIN).toBe(0.5); expect(RATE_MAX).toBe(2); expect(rateFraction(1)).toBeCloseTo(1 / 3) })
})
describe('voce', () => {
  const v = ['it-it-x-itb-local', 'it-it-x-itc-local', 'it-it-x-itd-local']
  it('dalla predefinita alla prima, in ordine, poi di nuovo la predefinita', () => {
    expect(nextVoice(v, null)).toBe('it-it-x-itb-local'); expect(nextVoice(v, 'it-it-x-itc-local')).toBe('it-it-x-itd-local')
    expect(nextVoice(v, 'it-it-x-itd-local')).toBeNull(); expect(nextVoice(v, 'vecchia')).toBe('it-it-x-itb-local'); expect(nextVoice([], 'x')).toBeNull()
  })
  it('la posizione', () => { expect(voicePosition(v, null)).toBe(0); expect(voicePosition(v, 'it-it-x-itc-local')).toBe(2) })
})
describe('prima riga del controller', () => {
  it('senza markdown e senza Esito', () => {
    expect(excerpt('\n\n**Esito:** home A installata alle 22:56\nWatch: Home A installata')).toBe('home A installata alle 22:56')
    expect(excerpt('# Consiglio KMP\n\naltro')).toBe('Consiglio KMP')
  })
  it('salta la riga per l\'orologio', () => expect(excerpt('Watch: breve\nFatto')).toBe('Fatto'))
  it('vuoto dà vuoto', () => expect(excerpt('  \n ')).toBe(''))
})

describe('voci migliori', () => {
  it('prima la Google predefinita, poi le Google locali, quelle di rete, le altre, eSpeak in fondo', () => {
    expect(rankVoices(['eSpeak Italian', 'Android Speech Services by Google it-it-x-itd-network', 'Microsoft Elsa', 'Android Speech Services by Google it-it-x-itd-local', 'Android Speech Services by Google it-IT-language']))
      .toEqual(['Android Speech Services by Google it-IT-language', 'Android Speech Services by Google it-it-x-itd-local', 'Android Speech Services by Google it-it-x-itd-network', 'Microsoft Elsa', 'eSpeak Italian'])
  })
})
