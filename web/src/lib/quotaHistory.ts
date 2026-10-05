// «Ritmo della finestra», porta di QuotaHistory.kt: dai campioni della quota la linea della finestra di 5 ore e la
// proiezione al reset. Niente numeri inventati: con meno di due campioni, o senza consumo, la proiezione non c'è.
export const WINDOW_S = 5 * 3600

export type Sample = { ts: number; pct: number }
/** `points`: la linea da disegnare. `projected`: dove si arriva al reset con il ritmo attuale, se si può dire. */
export type Pace = { points: Sample[]; projected: number | null; at: number | null }

/** Solo i campioni della finestra corrente, in ordine di tempo: quelli di una finestra passata gonfierebbero il ritmo. */
export function window(samples: Sample[], resetAt: number): Sample[] {
  const start = resetAt - WINDOW_S
  return samples.filter(s => s.ts >= start && s.ts <= resetAt).sort((a, b) => a.ts - b.ts)
}

/** Ritmo medio fra il primo e l'ultimo campione della finestra, esteso fino al reset; si ferma a 100. */
export function pace(samples: Sample[], resetAt: number, now: number): Pace {
  const points = window(samples, resetAt)
  if (points.length < 2) return { points, projected: null, at: null }
  const first = points[0]
  const last = points[points.length - 1]
  const secondi = last.ts - first.ts
  const punti = last.pct - first.pct
  // Fermi o in calo (una finestra nuova riparte da zero): nessuna proiezione.
  if (secondi <= 0 || punti <= 0) return { points, projected: null, at: null }
  const restano = Math.max(0, resetAt - Math.max(now, last.ts))
  const proiettato = Math.min(100, Math.max(0, Math.trunc(last.pct + (punti / secondi) * restano)))
  return { points, projected: proiettato, at: resetAt }
}
