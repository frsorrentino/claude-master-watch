// Quali file della chat si scaricano da soli per l'anteprima, come MediaPreview sul telefono: le immagini fino a 10 MB,
// i video fino a 25 MB (Franz, 10/10 15:24: «Ok anteprime video»). Nel browser non c'è il contatore della rete del
// telefono: vale `saveData`, il risparmio dati chiesto dall'utente.
export const IMAGE_AUTO_MAX = 10_000_000
/** Il tetto dei file a pezzi dal PC (contratto 1.34): oltre, il PC li rifiuta comunque. */
export const VIDEO_AUTO_MAX = 26_214_400

export const isVideo = (mime?: string | null) => !!mime?.startsWith('video/')
export const isImage = (mime?: string | null) => !!mime?.startsWith('image/')

export function autoPreview(mime: string | null | undefined, size: number | null | undefined, unmetered = true): boolean {
  if (isImage(mime)) return (size ?? 0) <= IMAGE_AUTO_MAX
  if (isVideo(mime)) return unmetered && size != null && size <= VIDEO_AUTO_MAX
  return false
}
