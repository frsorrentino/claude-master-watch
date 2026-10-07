import { describe, expect, it } from 'vitest'
import { copyKind, inlineSafe } from './fileActions'

describe('azioni sul file', () => {
  it('copia il testo dei file di testo', () => {
    for (const m of ['text/plain', 'text/markdown', 'application/json', 'application/xml', 'image/svg+xml', 'application/javascript', 'text/csv; charset=utf-8']) expect(copyKind(m)).toBe('text')
  })
  it("copia l'immagine delle immagini raster", () => {
    for (const m of ['image/png', 'image/jpeg', 'image/webp']) expect(copyKind(m)).toBe('image')
  })
  it('degli altri copia il percorso', () => {
    for (const m of ['application/pdf', 'application/zip', 'video/mp4', 'application/octet-stream']) expect(copyKind(m)).toBe('path')
  })
  it('HTML, SVG e XML non si aprono nella pagina: leggerebbero il token', () => {
    for (const m of ['text/html', 'image/svg+xml', 'application/xml', 'application/xhtml+xml']) expect(inlineSafe(m)).toBe(false)
    for (const m of ['image/png', 'application/pdf', 'text/plain']) expect(inlineSafe(m)).toBe(true)
  })
})
