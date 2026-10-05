// La riga «Prossimi: a · b · c» in fondo alla risposta, come NextSteps.parse dell'app Android: si toglie dal testo e diventa
// al massimo 3 consigli di non più di 40 caratteri. Conta l'ultima riga non vuota, saltando quella per l'orologio.
export const MAX = 3
export const MAX_CHARS = 40

export function parseSteps(text: string): { text: string; steps: string[] } {
  const lines = text.split('\n')
  let idx = -1
  for (let i = lines.length - 1; i >= 0; i--) if (lines[i].trim() && !lines[i].trimStart().startsWith('Watch:')) { idx = i; break }
  if (idx < 0 || !lines[idx].trim().startsWith('Prossimi:')) return { text, steps: [] }
  const steps = lines[idx].trim().slice('Prossimi:'.length).split('·').map(s => s.trim()).filter(s => s && s.length <= MAX_CHARS).slice(0, MAX)
  return { text: [...lines.slice(0, idx), ...lines.slice(idx + 1)].join('\n').trimEnd(), steps }
}
