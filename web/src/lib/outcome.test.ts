import { describe, expect, it } from 'vitest'
import { body, headline, summary } from './outcome'

// Gli stessi casi di OutcomeTextTest in Kotlin.
const corto = 'lavoro del relay chiuso e attivo, in attesa della prova dal vivo…'
const o = (short: string, full: string) => ({ short, full, at: 0 })
describe('esito', () => {
  it('il titolo è la riga di esito intera', () =>
    expect(headline(o(corto, 'Ho chiuso il relay.\nEsito: lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.'))).toBe('lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.'))
  it('vale anche Watch e il grassetto', () => expect(headline(o('tile rifatta', 'testo\n**Watch:** tile rifatta, quota su una riga.'))).toBe('tile rifatta, quota su una riga.'))
  it("con più righe vale l'ultima", () => expect(headline(o('x', 'Esito: prima.\naltro\nEsito: seconda.'))).toBe('seconda.'))
  it('la descrizione non ripete il titolo', () =>
    expect(body(o(corto, 'Ho chiuso il relay e i test sono verdi.\nEsito: lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.'))).toBe('Ho chiuso il relay e i test sono verdi.'))
  it('senza riga il titolo arriva a fine frase', () => {
    const x = o(corto, 'lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz. Poi il resto del messaggio.')
    expect(headline(x)).toBe('lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.')
    expect(body(x)).toBe('Poi il resto del messaggio.')
  })
  it('se resta solo il titolo niente descrizione', () => expect(body(o(corto, 'lavoro del relay chiuso e attivo, in attesa della prova dal vivo di Franz.'))).toBeNull())
  it('senza riga resta il testo corto', () => expect(headline(o(corto, 'solo testo senza riga finale'))).toBe(corto))
  it('la card mette l\'esito per primo', () =>
    expect(summary(o('migrations', 'Esito: migrations 008-011 applied, tests green.\nThe test seeds are still to review.\nProssimi: seeds · admin'))).toBe('migrations 008-011 applied, tests green.\nThe test seeds are still to review.'))
  it('senza riga di esito tiene il testo', () => expect(summary(o('Test verdi', 'Test verdi, 40 su 40.\nProssimi: tagga'))).toBe('Test verdi, 40 su 40.'))
})
