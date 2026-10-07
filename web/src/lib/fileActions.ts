// Le quattro azioni su un file della conversazione (Franz, 07/10 15:32): apri, scarica, copia, condividi.
export type FileAct = 'open' | 'download' | 'copy' | 'share'
export type Fetched = { blob: Blob; name: string; mime: string }

const base = (mime: string) => mime.split(';')[0].trim().toLowerCase()

// Gli appunti del browser prendono solo testo e PNG: il testo e le immagini si copiano, degli altri file il percorso.
export function copyKind(mime: string): 'text' | 'image' | 'path' {
  const m = base(mime)
  if (m.startsWith('text/') || /\/(json|xml|javascript|x-sh|x-yaml|yaml|toml)$|\+(xml|json)$/.test(m)) return 'text'
  if (m.startsWith('image/')) return 'image'
  return 'path'
}

// HTML, SVG e XML aperti dalla stessa origine della pagina leggerebbero il token: si scaricano invece di aprirsi.
export const inlineSafe = (mime: string) => !/html|svg|xml/.test(base(mime))

export function download(f: Fetched) {
  const url = URL.createObjectURL(f.blob)
  Object.assign(document.createElement('a'), { href: url, download: f.name }).click()
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

export function open(f: Fetched) {
  if (!inlineSafe(f.mime)) { download(f); return }
  const url = URL.createObjectURL(f.blob)
  window.open(url, '_blank')
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

// Le immagini non PNG passano da un canvas: gli appunti accettano solo image/png.
async function png(blob: Blob): Promise<Blob> {
  if (base(blob.type) === 'image/png') return blob
  const bmp = await createImageBitmap(blob)
  const c = Object.assign(document.createElement('canvas'), { width: bmp.width, height: bmp.height })
  c.getContext('2d')?.drawImage(bmp, 0, 0)
  return new Promise((ok, ko) => c.toBlob(b => (b ? ok(b) : ko(new Error('png'))), 'image/png'))
}

/** Copia il file negli appunti; dice che cosa ha copiato. */
export async function copy(f: Fetched, path: string): Promise<'text' | 'image' | 'path'> {
  const k = copyKind(f.mime)
  if (k === 'text') await navigator.clipboard.writeText(await f.blob.text())
  else if (k === 'image') await navigator.clipboard.write([new ClipboardItem({ 'image/png': png(f.blob) })])
  else await navigator.clipboard.writeText(path)
  return k
}

export const canShare = () => typeof navigator !== 'undefined' && typeof navigator.share === 'function'

/** Il foglio di condivisione del sistema con il file; dove i file non si condividono, il nome e il percorso. */
export async function share(f: Fetched, path: string) {
  const file = new File([f.blob], f.name, { type: f.mime })
  if (navigator.canShare?.({ files: [file] })) await navigator.share({ files: [file], title: f.name })
  else await navigator.share({ title: f.name, text: path })
}
