import type { Event, SearchPage } from './contract'
import type { Sent } from './chatRules'

// La ricerca del telefono, porta di ChatSearch.kt: nei messaggi mandati, negli esiti e negli eventi del diario, senza
// accenti né maiuscole; il risultato è la riga trovata con la parte da mettere in grassetto, dalla più recente.
export type Kind = 'sent' | 'outcome' | 'event' | 'conversation'
/** `start`/`end`: la parte trovata dentro `line`. `ref`: unico per risultato. `live`: solo per i risultati del relay. */
export type Hit = { session: string | null; at: number; line: string; start: number; end: number; kind: Kind; ref: string; live?: boolean | null }

const clamp = (x: number, lo: number, hi: number) => Math.min(Math.max(x, lo), hi)

/** Contratto 1.27: i risultati del relay, più le voci del registro senza sessione. Dal più recente. */
export function withConversations(local: Hit[], page: SearchPage): Hit[] {
  const remote: Hit[] = (page.hits ?? []).map((h, i) => {
    const start = clamp(h.match?.[0] ?? 0, 0, h.snippet.length)
    const end = clamp(h.match?.[1] ?? start, start, h.snippet.length)
    return { session: h.session, at: h.at ?? 0, line: h.snippet, start, end, kind: 'conversation', ref: `CONV-${h.session}-${h.entry ?? i}-${i}`, live: h.live }
  })
  return [...remote, ...local.filter(h => h.kind === 'event' && h.session == null)].sort((a, b) => b.at - a.at)
}

export function find(query: string, sent: Sent[], events: Event[]): Hit[] {
  const q = fold(query.trim())
  if (!q) return []
  const hits: Hit[] = []
  const scan = (text: string | null | undefined, session: string | null | undefined, at: number, kind: Kind, ref: string) => {
    // La prima riga che contiene il testo cercato: un risultato per testo, non uno per riga.
    for (const raw of (text ?? '').split(/\r\n|\r|\n/)) {
      const line = raw.trim()
      const i = fold(line).indexOf(q)
      if (i >= 0) { hits.push({ session: session ?? null, at, line, start: i, end: i + q.length, kind, ref: `${kind.toUpperCase()}-${ref}` }); return }
    }
  }
  for (const m of sent) {
    scan(m.text, m.session, m.sentAt, 'sent', m.id)
    scan(m.outcomeFull, m.session, m.doneAt ?? m.sentAt, 'outcome', m.id)
  }
  events.forEach((e, i) => scan([e.title, e.body ?? ''].filter(x => x.trim()).join('\n'), e.session, e.ts, 'event', `${e.key}-${i}`))
  return hits.sort((a, b) => b.at - a.at)
}

/** Minuscole e senza segni diacritici, carattere per carattere: gli indici restano quelli del testo originale. */
export function fold(s: string): string {
  let out = ''
  for (let i = 0; i < s.length; i++) {
    const base = s[i].normalize('NFD')[0] ?? s[i]
    out += base.toLowerCase()[0] ?? base
  }
  return out
}
