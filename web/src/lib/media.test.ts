import { describe, expect, it } from 'vitest'
import { autoPreview, isVideo } from './media'

describe('anteprime che arrivano da sole, come sul telefono', () => {
  it('immagini fino a 10 MB', () => {
    expect(autoPreview('image/png', 24_645)).toBe(true)
    expect(autoPreview('image/png', null)).toBe(true)
    expect(autoPreview('image/jpeg', 10_000_001)).toBe(false)
  })
  it('video fino a 25 MB, non col risparmio dati e non senza misura', () => {
    expect(autoPreview('video/mp4', 7_211_071)).toBe(true)
    expect(autoPreview('video/mp4', 26_214_400)).toBe(true)
    expect(autoPreview('video/mp4', 31_291_332)).toBe(false)
    expect(autoPreview('video/mp4', 7_211_071, false)).toBe(false)
    expect(autoPreview('video/mp4', null)).toBe(false)
  })
  it('gli altri file aspettano il tocco', () => {
    expect(autoPreview('application/pdf', 1_000)).toBe(false)
    expect(autoPreview(undefined, 1_000)).toBe(false)
    expect(isVideo('video/webm')).toBe(true)
  })
})
