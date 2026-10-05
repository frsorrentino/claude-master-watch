// La lettura a voce, una per volta (Speech.kt): ▶ legge, lo stesso tasto sullo stesso testo la ferma.
export const speech = $state<{ text: string | null }>({ text: null })

export function toggle(text: string) {
  speechSynthesis.cancel()
  if (speech.text === text) { speech.text = null; return }
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'it-IT'
  u.onend = u.onerror = () => { if (speech.text === text) speech.text = null }
  speech.text = text
  speechSynthesis.speak(u)
}
