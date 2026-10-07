import type { Question } from './contract'
import { parse as markdown } from './markdown'
import { outcomeForPhone } from './links'
import { parseSteps } from './nextSteps'
import { optionText } from './questionRules'

// La lettura a voce, porte di SpeechText.kt, SpeechRate.kt, VoiceRules.kt e ReadingBar.kt: il testo pulito per la voce,
// i pezzi tagliati a fine frase, la velocità della pillola, la voce dopo, la prima riga del mini-controller.

const FINE = '.!?:;,…'
/** Il markdown letto a voce diventa rumore e si toglie; il codice si annuncia soltanto. */
export function clean(text: string, codeLabel: string): string {
  let t = text.replace(/```[\s\S]*?(```|$)/g, `\n${codeLabel}.\n`)
  t = t.replace(/`([^`]*)`/g, '$1').replace(/!?\[([^\]]*)]\([^)]*\)/g, '$1').replace(/https?:\/\/\S+/g, '')
  t = t.split('\n').map(raw => {
    const line = raw.trim()
      .replace(/^#{1,6}\s*/, '').replace(/^[-*+]\s+/, '').replace(/^\|?\s*:?-{3,}.*$/, '').replace(/\s*\|\s*/g, ', ')
      .replace(/^[ ,]+|[ ,]+$/g, '')
      .replace(/(\*\*|\*)(?=\S)(.+?)(?<=\S)\1/g, '$2')
    // Una riga senza punteggiatura finale chiude con un punto: la voce fa la pausa.
    return !line || FINE.includes(line[line.length - 1]) ? line : `${line}.`
  }).join('\n')
  return t.replace(/[ \t]+/g, ' ').replace(/\n{2,}/g, '\n').trim()
}
/** Quello che legge il telefono: senza la riga dei consigli, con «Watch:» letto come esito, poi pulito. */
export const forPhone = (text: string, codeLabel: string, outcomeLabel = 'Esito') => clean(outcomeForPhone(parseSteps(text).text, outcomeLabel), codeLabel)

export const MAX_CHUNK = 3500
/** Pezzi da leggere di fila, ognuno entro `max`, tagliati a fine frase; una frase più lunga si taglia a uno spazio. */
export function chunks(text: string, max = MAX_CHUNK): string[] {
  const frasi = text.trim().split(/(?<=[.!?…:;])\s+|\n+/).map(f => f.trim()).filter(Boolean)
  const out: string[] = []
  let cur = ''
  const flush = () => { if (cur) { out.push(cur); cur = '' } }
  for (const f of frasi) {
    let rest = f
    while (rest.length > max) {
      flush()
      const sp = rest.lastIndexOf(' ', max)
      const cut = sp > 0 ? sp : max
      out.push(rest.slice(0, cut).trim())
      rest = rest.slice(cut).trim()
    }
    if (cur && cur.length + 1 + rest.length > max) flush()
    cur = cur ? `${cur} ${rest}` : rest
  }
  flush()
  return out
}
/** La domanda con le opzioni numerate: «Deploy now? 1, yes. 2, no.». */
export const question = (q: Question) => [q.text.trim(), ...q.options.map(o => `${o.n}, ${optionText(o.label)}.`)].join(' ')

// La velocità continua (SpeechRate.kt, slider approvato da Franz il 07/10 alle 22:06): da 0,5× a 2× a passi di 0,05,
// con uno scatto su 1× per ritrovare la velocità normale al tatto.
export const RATE_MIN = 0.5
export const RATE_MAX = 2
export const RATE_STEP = 0.05
export function snapRate(v: number): number {
  const c = Math.min(RATE_MAX, Math.max(RATE_MIN, v))
  if (Math.abs(c - 1) < 0.035) return 1
  return (Math.round(c / RATE_STEP) * 5) / 100
}
/** Il valore salvato portato sul passo; senza scelta o rovinato, 1. */
export function rateOf(saved: number | null | undefined): number {
  if (saved == null || !Number.isFinite(saved) || saved <= 0) return 1
  return snapRate(saved)
}
/** Dove sta `v` sulla corsa dello slider, da 0 a 1: per il segno di 1×. */
export const rateFraction = (v: number) => (v - RATE_MIN) / (RATE_MAX - RATE_MIN)

/** La voce dopo `current`: dalla predefinita alla prima, poi in ordine, dopo l'ultima di nuovo la predefinita (null). */
export function nextVoice(voices: string[], current: string | null): string | null {
  if (!voices.length) return null
  const i = current == null ? -1 : voices.indexOf(current)
  return current == null || i < 0 ? voices[0] : voices[i + 1] ?? null
}
export const voicePosition = (voices: string[], current: string | null) => (current == null ? 0 : voices.indexOf(current) + 1)

/** La prima riga del mini-controller: testo vero, senza markdown, senza «Esito:» e senza la riga per l'orologio. */
export function excerpt(text: string): string {
  const line = markdown(text).text.split('\n').map(l => l.trim()).find(l => l && !l.startsWith('Watch:'))
  return line ? line.replace(/^Esito:/, '').trim() : ''
}

/**
 * Le voci italiane dalla migliore (Franz, 05/10 16:54: «legge molto male»): senza una voce scelta Chrome prendeva eSpeak.
 * Prima la voce italiana predefinita dei servizi vocali Google, la stessa che usa l'app sul telefono, poi le altre Google
 * sul dispositivo, poi quelle di rete, poi le altre; eSpeak in fondo.
 */
export function rankVoices(names: string[]): string[] {
  const score = (n: string) => {
    const x = n.toLowerCase()
    if (x.includes('espeak')) return 9
    if (!x.includes('google')) return 5
    if (x.endsWith('-language')) return 0
    return x.includes('network') ? 2 : 1
  }
  return [...names].sort((a, b) => score(a) - score(b))
}
