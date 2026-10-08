// La pagina «Notte» dal rapporto (porta di NightPage.kt; specifica docs/proposte/2026-10-07-pagina-notte.md, approvata
// da Franz il 07/10 alle 21:50): solo i conti. Date e durate a parole le scrive la pagina con t.ts.
import type { Option } from './contract'

export type NightReport = {
  schema?: string; v?: number; generated_at?: number; date?: string
  window: { start: number; end: number; start_source?: string | null; last_message?: { at: number; session?: string | null; text: string } | null }
  attention?: {
    questions?: { session: string; project?: string | null; text: string; options?: Option[] }[]
    approvals?: { task: string; title: string; project?: string | null }[]
    unblock?: { session: string; project?: string | null; text: string }[]
  }
  timeline?: { kind: string; id: string; title: string; project?: string | null; start: number; end?: number | null; outcome: string
    detail?: string | null; report?: string | null; live?: boolean; counts?: { prompts: number; tests: number; commits: number } | null }[]
  projects?: { name: string; path?: string | null; parts?: { title: string; state: string }[]; parts_total?: number; waiting_on?: string[]
    next?: string | null; events?: { at: number; kind: string; text: string; ok?: boolean | null }[] }[]
}

/** Gli stessi significati di Telegram: ✓ ok, ✗ fermo o fallito, ▶ in corso, ❓ domanda aperta. */
export type Icon = 'ok' | 'stopped' | 'running' | 'question'
export const MIN_BAR = 0.012
export type Step = { at: number; kind: string; text: string; ok: boolean | null }
export type Card = {
  id: string; icon: Icon; title: string; folder: string | null; queue: boolean; start: number; end: number | null; durationS: number | null
  detail: string | null; from: number; to: number; chat: string | null; prompts: number | null; tests: number | null; commits: number | null; steps: Step[]
}
export type Need = { kind: 'question' | 'approval' | 'unblock'; session: string; text: string; options: Option[]; task?: string }
export type Project = { name: string; done: number; total: number; parts: string[]; waiting: string[]; next: string | null }
/** Quante voci per esito, per le pillole in testa (Franz, 08/10 12:30: «non capisco cosa è da fare o fatto»). */
export type Counts = { done: number; running: number; stopped: number; asking: number }
export type NightPage = {
  counts: Counts
  day: string; dayBefore: string; start: number; end: number; windowS: number; fromLastMessage: boolean
  needs: Need[]; cards: Card[]; axis: { label: string; at: number }[]; projects: Project[]
}

const ymd = (t: number, timeZone: string) => new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(t * 1000))
const hm = (t: number, timeZone: string) => new Intl.DateTimeFormat('it-IT', { timeZone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(t * 1000))

/** `now`: sessioni vive e approvazioni in attesa adesso; «Da fare per te» tiene solo quello ancora vero (Franz, 08/10 14:20). */
export function page(r: NightReport, timeZone: string, now?: { live: Set<string>; pending: Set<string> }): NightPage {
  const start = r.window.start
  const end = Math.max(r.window.end, start + 60)
  const span = end - start
  const frac = (t: number) => Math.min(1, Math.max(0, (t - start) / span))
  const asking = new Set((r.attention?.questions ?? []).map(q => q.session))
  const projects = r.projects ?? []
  const cards: Card[] = (r.timeline ?? []).map(i => {
    const queue = i.kind === 'night_job'
    const [folder, rest] = queue && i.title.includes(' — ') ? [i.title.split(' — ')[0], i.title.slice(i.title.indexOf(' — ') + 3)] : [null, i.title]
    let from = frac(i.start)
    let to = frac(i.end ?? end)
    if (to - from < MIN_BAR) { to = Math.min(1, from + MIN_BAR); from = Math.max(0, to - MIN_BAR) }
    const project = projects.find(p => p.path != null && p.path === i.project)
    const until = (i.end ?? end) + 60
    return {
      id: i.id,
      icon: i.kind === 'session' && asking.has(i.id) ? 'question' : i.outcome === 'ok' ? 'ok' : i.outcome === 'running' ? 'running' : 'stopped',
      title: queue ? shortTitle(rest) : i.title, folder, queue, start: i.start, end: i.end ?? null,
      durationS: i.end != null ? Math.max(0, i.end - i.start) : null, detail: i.detail ?? null, from, to,
      chat: i.kind === 'session' && i.live ? i.id : null,
      prompts: i.counts?.prompts ?? null, tests: i.counts?.tests ?? null, commits: i.counts?.commits ?? null,
      steps: (project?.events ?? []).filter(e => e.at >= i.start - 60 && e.at <= until).map(e => ({ at: e.at, kind: e.kind, text: e.text, ok: e.ok ?? null })),
    }
  })
  const axis: { label: string; at: number }[] = []
  for (let t = (Math.floor(start / 1800) + 1) * 1800; t < end; t += 1800) axis.push({ label: hm(t, timeZone), at: frac(t) })
  const needs: Need[] = [
    ...(r.attention?.questions ?? []).map(q => ({ kind: 'question' as const, session: q.session, text: q.text, options: q.options ?? [] })),
    ...(r.attention?.approvals ?? []).map(a => ({ kind: 'approval' as const, session: (a.project ?? '').split('/').pop() ?? '', text: a.title, options: [], task: a.task })),
    ...(r.attention?.unblock ?? []).map(u => ({ kind: 'unblock' as const, session: u.session, text: u.text, options: [] })),
  ]
    .filter(n => !now || (n.kind === 'approval' ? !!n.task && now.pending.has(n.task) : now.live.has(n.session)))
  const day = ymd(end, timeZone)
  const before = new Date(`${day}T12:00:00Z`); before.setUTCDate(before.getUTCDate() - 1)
  const counts: Counts = { done: 0, running: 0, stopped: 0, asking: 0 }
  for (const c of cards) counts[c.icon === 'ok' ? 'done' : c.icon === 'running' ? 'running' : c.icon === 'question' ? 'asking' : 'stopped']++
  return {
    counts, day, dayBefore: before.toISOString().slice(0, 10), start, end, windowS: end - start,
    fromLastMessage: r.window.start_source === 'last_message', needs, cards, axis,
    projects: projects.map(p => ({
      name: p.name, done: (p.parts ?? []).filter(x => x.state === 'done').length, total: Math.max(p.parts_total ?? 0, (p.parts ?? []).length),
      parts: (p.parts ?? []).map(x => x.state), waiting: (p.waiting_on ?? []).map(w => w.replace(/^(?:!|domanda|ok)\s*:\s*/, '')), next: p.next ?? null,
    })),
  }
}

/**
 * Il titolo di un lavoro della coda (mai «…» nel corpo dei testi): se è lungo o tagliato, la prima frase o il pezzo prima
 * dei due punti; altrimenti fino all'ultima parola intera. La correzione vera è un titolo breve dal relay (richiesta 4).
 */
export function shortTitle(raw: string): string {
  const t = raw.trim()
  const cut = t.endsWith('…')
  if (!cut && t.length <= 60) return t.replace(/\.+$/, '')
  const body = cut ? t.slice(0, -1) : t
  const m = /^(.{12,}?)(?:[.;:]\s|\.$)/.exec(body)
  if (m) return m[1].trim()
  if (!cut) return body.replace(/\.+$/, '')
  return body.slice(0, body.lastIndexOf(' ')).replace(/[,;: ]+$/, '')
}

/** Il riquadro Notte in home (mockup approvato l'08/10 alle 12:57): finché quella notte non è stata aperta. */
export const showHomeNight = (ref: { date: string } | null | undefined, opened: string | null): boolean => !!ref && ref.date !== opened
