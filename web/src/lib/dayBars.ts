import type { Event, EventKind } from './contract'

// «Oggi», porta di DayBars.kt: una barra per ora con il lavoro delle sessioni, dagli eventi. Le barre arrivano fino
// all'ora corrente, mai oltre: ore future vuote direbbero «non hai fatto niente» invece di «non è ancora successo».
export type Bar = { hour: number; count: number }

/** Gli eventi che dicono lavoro. La quota cambia da sola e non è lavoro di nessuno: fuori. */
const WORK: EventKind[] = ['launched', 'answered', 'outcome', 'question', 'resumed']

/** Mezzanotte locale del giorno di `t` nel fuso: l'offset si cerca due volte per reggere i cambi dell'ora legale. */
function startOfDay(t: number, timeZone?: string): number {
  const parts = (e: number) => Object.fromEntries(new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date(e * 1000)).map(x => [x.type, x.value]))
  const asUtc = (e: number) => { const p = parts(e); return Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second) / 1000 }
  const p = parts(t)
  const midnight = Date.UTC(+p.year, +p.month - 1, +p.day) / 1000
  let s = midnight
  for (let i = 0; i < 2; i++) s = midnight - (asUtc(s) - s)
  return s
}

export function today(events: Event[], now: number, timeZone?: string): Bar[] {
  const inizio = startOfDay(now, timeZone)
  const oraCorrente = Math.min(23, Math.max(0, Math.trunc((now - inizio) / 3600)))
  const conteggi = new Array<number>(24).fill(0)
  for (const e of events) {
    if (!WORK.includes(e.kind) || e.ts < inizio) continue
    const h = Math.trunc((e.ts - inizio) / 3600)
    if (h >= 0 && h <= oraCorrente) conteggi[h]++
  }
  return Array.from({ length: oraCorrente + 1 }, (_, hour) => ({ hour, count: conteggi[hour] }))
}

/** L'ora più piena: è la scala con cui si disegnano le barre. */
export const peak = (bars: Bar[]): number => Math.max(0, ...bars.map(b => b.count))
