import type { Outcome } from './contract'
import { parseSteps } from './nextSteps'

// Titolo e testo dell'esito, porta di OutcomeText.kt: il titolo è l'ultima riga «Esito:» o «Watch:», o la prima frase intera
// di `full` se comincia come `short`, o `short`; sotto solo quello che il titolo non dice già.
const RIGA = /^[\s*_>-]*(?:Esito|Watch)\s*:\s*[*_]*\s*(.+?)[*_\s]*$/i
const FINE = /[.!?…](\s|$)/g
const MAX = 220

function riga(o: Outcome): string | null {
  const found = o.full.split('\n').map(l => RIGA.exec(l.trim())?.[1]?.trim()).filter((x): x is string => !!x)
  return found.length ? found[found.length - 1] : null
}

export function headline(o: Outcome): string {
  const r = riga(o); if (r) return r
  const base = o.short.replace(/[…\s]+$/, '')
  const full = o.full.trim()
  if (base && full.startsWith(base)) {
    FINE.lastIndex = Math.min(base.length, full.length)
    const f = FINE.exec(full)
    const frase = (f ? full.slice(0, f.index + 1) : full).trim()
    if (frase.length <= MAX) return frase
  }
  return o.short
}

export function body(o: Outcome): string | null {
  const titolo = headline(o)
  const full = o.full.trim()
  const resto = (riga(o) != null ? full.split('\n').filter(l => !RIGA.test(l.trim())).join('\n') : full.startsWith(titolo) ? full.slice(titolo.length) : full).trim()
  return resto && resto !== titolo ? resto : null
}

/** Il testo di una card del riepilogo: l'esito senza etichetta, poi il resto, senza la riga «Prossimi». */
export function summary(o: Outcome): string {
  const clean = { ...o, full: parseSteps(o.full).text }
  return [headline(clean), body(clean)].filter(Boolean).join('\n')
}
