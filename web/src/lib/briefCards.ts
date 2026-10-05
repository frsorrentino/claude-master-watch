import type { QuotaAccount, State } from './contract'
import { since, STALE_AFTER_S, type Freshness } from './durations'

// Contenuto della schermata «brief mattutino», porta di BriefCards.kt (e di Accounts.isPersonalQuota, QuotaText.fraction):
// una card per dato. Qui si decide cosa si vede e con che testo; le card a zero non si mostrano.
export type Tone = 'neutral' | 'good' | 'warn' | 'alert' | 'stale'
export type Glyph = 'time' | 'sessions' | 'question' | 'night' | 'sync'
/** Il segno dell'account davanti al titolo, come nella tile: cerchio personale, quadrato lavoro. */
export type Shape = 'circle' | 'square'

export type Card = {
  key: string; label: string; value: string
  unit: string | null; secondary: string | null; pill: string | null
  /** Riga sotto la pillolina. */
  note: string | null
  tone: Tone; progress: number | null
  /** Secondo valore per l'anello concentrico: nella quota, fuori le 5 ore e dentro la settimana. */
  progress2: number | null
  glyph: Glyph
  /** Tono dell'anello interno e della sua pillola: la settimana ha soglie sue. */
  tone2: Tone
  shape: Shape | null
}
// I default dei campi opzionali di Card in Kotlin.
const card = (c: Pick<Card, 'key' | 'label' | 'value'> & Partial<Card>): Card =>
  ({ unit: null, secondary: null, pill: null, note: null, tone: 'neutral', progress: null, progress2: null, glyph: 'time', tone2: 'neutral', shape: null, ...c })

export type Labels = {
  quota: string; week: string; resetAt: string; stale: string; none: string
  active: string; waitingPill: string; noQuestions: string; questions: string; oldest: string
  night: string; running: string; nothingRunning: string
  update: string; minutes: string; now: string; stopped: string
  /** «settimana»: la pillolina quando il numero grande è già la settimana. */
  weekOnly?: string
  quotaTitle?: string
}
// String.format dei testi: un solo segnaposto %s o %d.
const fmt = (pattern: string, arg: string | number) => pattern.replace(/%[sd]/, String(arg))

const fraction = (pct: number | null | undefined) => Math.min(100, Math.max(0, pct ?? 0)) / 100

/** Dal contratto 1.8 l'account personale lo dice il tipo, non il nome; senza tipo si ripiega sul nome «personale». */
export const isPersonalQuota = (name: string, q: QuotaAccount) =>
  q.kind != null ? q.kind === 'personal' : name.toLowerCase() === 'personale'

/** «gio» e «04:00» di un istante nel fuso, in italiano. */
function dayHm(epoch: number, timeZone?: string) {
  const p = Object.fromEntries(new Intl.DateTimeFormat('it-IT', { timeZone, weekday: 'short', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
    .formatToParts(new Date(epoch * 1000)).map(x => [x.type, x.value]))
  return { hm: `${p.hour}:${p.minute}`, weekday: `${p.weekday} ${p.hour}:${p.minute}` }
}

/** `personale` per primo, poi gli altri account in ordine: è l'account di Franz e lo guarda per primo. */
export function quota(state: State | null, l: Labels, timeZone?: string): Card[] {
  if (!state) return []
  const quota = state.quota
  const order = Object.keys(quota).sort((a, b) => {
    const pa = isPersonalQuota(a, quota[a]) ? 0 : 1
    const pb = isPersonalQuota(b, quota[b]) ? 0 : 1
    const la = a.toLowerCase(), lb = b.toLowerCase()
    return pa - pb || (la < lb ? -1 : la > lb ? 1 : 0)
  })
  return order.map(account => {
    const q = quota[account]
    // Senza lettura delle 5 ore (`h5: null`) il numero grande è la settimana, con il suo reset sotto.
    const soloSettimana = q.h5 == null && q.w7 != null
    const big = soloSettimana ? q.w7! : (q.h5 ?? null)
    const weekDay = q.reset_w7 != null ? dayHm(q.reset_w7, timeZone).weekday : null
    const personal = isPersonalQuota(account, q)
    return card({
      key: `quota-${account}`,
      // Il titolo è il tema, uguale in Panoramica e nella Scheda; l'account lo dice la forma.
      label: l.quotaTitle ?? 'Quota',
      shape: personal ? 'circle' : 'square',
      value: big != null ? String(big) : l.none,
      unit: big != null ? '%' : null,
      // Sotto le 5 ore la loro ripartenza; la settimanale sta con la settimana, per non sembrare il reset delle 5 ore.
      secondary: soloSettimana ? null : q.reset_h5 != null ? fmt(l.resetAt, dayHm(q.reset_h5, timeZone).hm) : null,
      pill: q.stale ? l.stale
        : soloSettimana ? (l.weekOnly ?? '') + (weekDay ? ` · ${weekDay}` : '')
          // La ripartenza settimanale dentro la pillola, a tutta larghezza sotto l'anello.
          : fmt(l.week, q.w7 != null ? `${q.w7} %` : l.none) + (weekDay ? ` · ${weekDay}` : ''),
      // Scala di allarme sulle 5 ore: dal 90 % ambra, esaurita rosso; per la sola settimana l'ambra dall'80 %.
      tone: q.stale ? 'stale' : (big ?? 0) >= 100 ? 'alert' : soloSettimana && (big ?? 0) >= 80 ? 'warn' : (q.h5 ?? 0) >= 90 ? 'warn' : 'neutral',
      // Sempre due anelli: fuori le 5 ore (vuoto senza lettura), dentro la settimana.
      progress: soloSettimana ? 0 : fraction(big),
      progress2: q.w7 != null ? fraction(q.w7) : null,
      glyph: 'time',
      // La settimana col suo tono: ambra dall'80 %, la soglia di stop; rosso esaurita.
      tone2: q.stale ? 'stale' : (q.w7 ?? 0) >= 100 ? 'alert' : (q.w7 ?? 0) >= 80 ? 'warn' : 'neutral',
    })
  })
}

/** Il lavoro: quante sessioni lavorano, quante domande aspettano, la coda della notte, quanto è fresco il PC. */
export function work(state: State | null, fresh: Freshness, now: number, l: Labels): Card[] {
  if (!state) return []
  const live = state.sessions.filter(s => s.state !== 'gone')
  const active = live.filter(s => s.state === 'waiting' || s.state === 'busy' || s.state === 'awaiting').length
  const questions = state.sessions.flatMap(s => (s.question ? [s.question] : []))
  const cards: Card[] = [card({
    key: 'active', label: l.active, value: String(active),
    pill: questions.length === 0 ? l.noQuestions : fmt(l.waitingPill, questions.length),
    tone: questions.length === 0 ? 'good' : 'warn',
    progress: live.length === 0 ? 0 : active / live.length,
    glyph: 'sessions',
  })]
  if (questions.length) {
    const oldest = questions.reduce((a, b) => (b.asked_at < a.asked_at ? b : a))
    cards.push(card({
      key: 'questions', label: l.questions, value: String(questions.length),
      pill: fmt(l.oldest, since(oldest.asked_at, now)), tone: 'warn',
      // Il gauge dice quante delle sessioni vive stanno aspettando te.
      progress: live.length === 0 ? 1 : questions.length / live.length,
      glyph: 'question',
    }))
  }
  const night = state.night
  if (night.queued > 0 || night.running != null) {
    cards.push(card({
      key: 'night', label: l.night, value: String(night.queued),
      pill: night.running != null ? fmt(l.running, night.running) : l.nothingRunning,
      tone: night.running != null ? 'neutral' : 'good',
      // Quanta coda è già passata: una in corso su quelle che restano.
      progress: night.running == null ? 0 : 1 / (night.queued + 1),
      glyph: 'night',
    }))
  }
  const age = Math.trunc((now - state.ts) / 60)
  cards.push(card({
    key: 'update', label: l.update,
    value: age <= 0 ? l.now : String(age),
    unit: age <= 0 ? null : l.minutes,
    secondary: fresh.stale ? l.stopped : null,
    pill: state.host.trim() ? state.host : null,
    tone: fresh.stale ? 'stale' : 'good',
    // Il gauge si riempie mentre il dato invecchia: pieno = PC fermo, e allora pulsa.
    progress: Math.min(1, Math.max(0, (now - state.ts) / STALE_AFTER_S)),
    glyph: 'sync',
  }))
  return cards
}
