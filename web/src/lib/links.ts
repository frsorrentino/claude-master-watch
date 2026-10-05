// Testi della chat: i link toccabili e la riga per l'orologio, porte di Links.kt e OutcomeLine.kt.

const URL = /https?:\/\/[^\s<>"«»[\]{}]+/g
const TRAILING = ".,;:!?'’\"»)"

/** I link `http(s)://` come [inizio, fine); la punteggiatura in coda resta fuori, una ) chiusa da una ( interna no. */
export function findLinks(text: string): [number, number][] {
  const out: [number, number][] = []
  for (const m of text.matchAll(URL)) {
    const start = m.index!
    let end = start + m[0].length - 1
    while (end > start && TRAILING.includes(text[end])) {
      const url = text.slice(start, end + 1)
      if (text[end] === ')' && url.split('(').length >= url.split(')').length) break
      end--
    }
    if (end - start >= 'http://x'.length - 1) out.push([start, end + 1])
  }
  return out
}

/**
 * La riga «Watch: …» si legge come «Esito»; se la risposta ha già la sua riga «Esito:», quella per l'orologio sparisce.
 */
export function outcomeForPhone(text: string, label = 'Esito'): string {
  const lines = text.split('\n')
  const isWatch = (l: string) => l.trimStart().startsWith('Watch:')
  if (!lines.some(isWatch)) return text
  const hasOutcome = lines.some(l => l.trimStart().startsWith('Esito:'))
  return lines.flatMap(l => (!isWatch(l) ? [l] : hasOutcome ? [] : [`${label}:${l.trimStart().slice('Watch:'.length)}`])).join('\n').trimEnd()
}
