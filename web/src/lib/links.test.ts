import { describe, expect, it } from 'vitest'
import { findLinks, outcomeForPhone } from './links'

// Gli stessi casi di LinksTest e OutcomeLineTest in Kotlin.
const found = (t: string) => findLinks(t).map(([a, b]) => t.slice(a, b))
describe('link', () => {
  it('trova il link', () => expect(found('Mockup: https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz')).toEqual(['https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz']))
  it('la punteggiatura in coda resta fuori', () => {
    for (const t of ['vedi https://a.it/x.', '(https://a.it/x)', '«https://a.it/x»,', '[link](https://a.it/x)']) expect(found(t)).toEqual(['https://a.it/x'])
  })
  it('parentesi interne e query restano', () => {
    expect(found('https://it.wikipedia.org/wiki/Roma_(città) qui')).toEqual(['https://it.wikipedia.org/wiki/Roma_(città)'])
    expect(found('http://a.it/p?q=1&r=2#s')).toEqual(['http://a.it/p?q=1&r=2#s'])
  })
  it('più link, e niente senza schema', () => {
    expect(findLinks('https://a.it e https://b.it')).toHaveLength(2)
    expect(findLinks('nessun link, solo claude.ai senza schema')).toHaveLength(0)
  })
})
describe('riga per l\'orologio', () => {
  it('Watch diventa Esito', () => expect(outcomeForPhone('Fatto il push.\n\nWatch: release avviata')).toBe('Fatto il push.\n\nEsito: release avviata'))
  it('con Esito già presente sparisce', () => expect(outcomeForPhone('Fatto.\n\nEsito: push di 3d353f4 fatto, CI in corso.\n\nWatch: push fatto, CI in corso')).toBe('Fatto.\n\nEsito: push di 3d353f4 fatto, CI in corso.'))
  it('solo a inizio riga', () => {
    expect(outcomeForPhone("Il watch: segna l'ora")).toBe("Il watch: segna l'ora")
    expect(outcomeForPhone('niente')).toBe('niente')
  })
})
