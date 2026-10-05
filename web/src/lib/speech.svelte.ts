import { chunks, forPhone, nextRate, nextVoice, rankVoices, rateOf } from './speechRules'
import { t } from './t'

// La lettura a voce, una per volta (Speech.kt): ▶ legge, lo stesso tasto sullo stesso testo la ferma. Il testo si pulisce
// e si legge a pezzi; velocità e voce si cambiano mentre legge e la lettura riparte dal pezzo che stava dicendo.
const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
const store = (k: string, v: string | null) => { try { if (v == null) localStorage.removeItem(k); else localStorage.setItem(k, v) } catch { /* resta per la sessione */ } }

export const speech = $state<{ text: string | null; source: string | null; rate: number; voice: string | null; voices: string[] }>({
  text: null, source: null, rate: rateOf(Number(load('cm.rate') ?? NaN)), voice: load('cm.voice'), voices: [],
})
let parts: string[] = []
let at = 0
let run = 0

function italian() {
  if (typeof speechSynthesis === 'undefined') return
  speech.voices = rankVoices(speechSynthesis.getVoices().filter(v => v.lang.toLowerCase().startsWith('it')).map(v => v.name))
}
if (typeof speechSynthesis !== 'undefined') { italian(); speechSynthesis.addEventListener('voiceschanged', italian) }

function play(from: number) {
  const id = ++run
  speechSynthesis.cancel()
  at = from
  const next = () => {
    if (id !== run) return
    if (at >= parts.length) { speech.text = null; speech.source = null; return }
    const u = new SpeechSynthesisUtterance(parts[at])
    u.lang = 'it-IT'
    u.rate = speech.rate
    // Senza una scelta la migliore italiana, non quella che sceglierebbe Chrome.
    const name = speech.voice ?? speech.voices[0]
    const v = speechSynthesis.getVoices().find(x => x.name === name)
    if (v) u.voice = v
    u.onend = () => { if (id === run) { at++; next() } }
    u.onerror = e => { if (id === run && e.error !== 'interrupted' && e.error !== 'canceled') { speech.text = null; speech.source = null } }
    speechSynthesis.speak(u)
  }
  next()
}

/** Legge `text` (grezzo, col markdown) o, se lo sta già leggendo, si ferma. `source`: la sessione, per il controller. */
export function toggle(text: string, source: string | null = null) {
  if (speech.text === text) { stop(); return }
  parts = chunks(forPhone(text, t.codeLabel))
  if (!parts.length) return
  speech.text = text
  speech.source = source
  play(0)
}
export function stop() { run++; speechSynthesis.cancel(); speech.text = null; speech.source = null }
export function cycleRate() {
  speech.rate = nextRate(speech.rate)
  store('cm.rate', String(speech.rate))
  if (speech.text) play(at)
}
export function cycleVoice() {
  speech.voice = nextVoice(speech.voices, speech.voice)
  store('cm.voice', speech.voice)
  if (speech.text) play(at)
}
