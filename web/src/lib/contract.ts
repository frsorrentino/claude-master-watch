// Il contratto con il PC (contract/README.md), lo stesso JSON che legge l'app Android: qui solo i tipi e la lettura.
// I test leggono le fixture di ../contract, come quelli Kotlin.

export type SessionState = 'waiting' | 'busy' | 'idle' | 'awaiting' | 'gone'
export type Option = { n: number; label: string }
export type Question = { id: string; kind: 'permission' | 'ask' | 'plan'; text: string; options: Option[]; tier: 'low' | 'medium' | 'high'; asked_at: number }
export type Outcome = { short: string; full: string; at: number }
export type Model = { id: string; label?: string | null }
export type Goal = { text: string; since: number; met?: boolean | null }

export type Session = {
  id: string; name: string; account: string; project: string; state: SessionState; since: number
  turn_started?: number | null; tool?: string | null; link?: string; tool_note?: string | null
  attached?: boolean; followed?: boolean; question?: Question | null; outcome?: Outcome | null
  next?: string | null; next_at?: number | null; color?: string | null; icon?: string | null; account_kind?: string | null
  model?: Model | null; effort?: string | null; context?: number | null; low_priority?: string | null
  goal?: Goal | null; suggestion?: string | null
  /** Contratto 1.37: assenti nello stato quando non valorizzati; la lettura mette null / false / null. */
  advice?: Advice | null; finished?: boolean; duplicate_of?: string | null
  /** Contratto 1.38: i Prossimi dell'ultimo esito, col «!» tolto; `blocking` = sblocca un lavoro fermo. */
  next_steps?: NextStep[] | null
}

export type NextStep = { text: string; blocking: boolean }

/** Contratto 1.37: il consiglio di fable-director; `when` "now" o "next_task", `differs` = il puntino sul tasto. */
export type Advice = { model: string; effort: string; reason: string; switch_cost_tokens: number; at: number; source: string; when: 'now' | 'next_task'; differs: boolean }
/** Contratto 1.37: un compito che aspetta l'ok; `deploy` = esce in produzione. */
export type Approval = { task: string; title: string; what: string; where: string; deploy: boolean; requested_at: number }

export type QuotaAccount = { h5?: number | null; w7?: number | null; reset_w7?: number | null; reset_h5?: number | null; stale?: boolean; kind?: string | null }
export type Device = { uid: string; name: string; kind?: string | null; seen?: number | null }
export type Recurring = { id: string; label: string; prompt: string; param?: boolean }

export type Project = { path: string; name: string; account: string; last_used?: number | null }
export type NightItem = { id: string; dir: string; name: string; prompt: string; added: number; started?: number | null }
export type Night = { queued: number; running?: string | null; items?: NightItem[] | null }
export type RecapItem = { project: string; done: string; next?: string | null }
export type Recap = { date: string; items: RecapItem[] }
export type EventKind = 'question' | 'answered' | 'outcome' | 'gone' | 'launched' | 'quota' | 'resumed' | 'recap' | 'night_report' | 'restart_failed'
export type Event = { key: string; kind: EventKind; session?: string | null; account?: string | null; ts: number; title: string; body?: string; ref?: string | null }

// Trascrizione (contratto 1.22): `in` comprende la cache; `queued` = scritta a turno in corso e non ancora presa; `cut` = accorciata dal PC.
export type TranscriptTurn = { started?: number | null; ended?: number | null; in?: number | null; out?: number | null }
export type TranscriptFile = { path: string; mime?: string | null; size?: number | null }
export type TranscriptEntry = {
  id: string; role: string; text?: string | null; at?: number | null; tool?: string | null; note?: string | null
  error?: boolean | null; cut?: boolean; turn?: TranscriptTurn | null; files?: TranscriptFile[] | null
  origin?: string | null; queued?: boolean
}
export type TranscriptPage = { entries: TranscriptEntry[]; more?: boolean }

// Ricerca (contratto 1.27): `match` = [inizio, fine] dentro `snippet`.
export type SearchHit = { session: string; live: boolean; project?: string | null; entry?: string | null; role?: string | null; at?: number | null; snippet: string; match?: number[] }
export type SearchPage = { hits?: SearchHit[]; more?: boolean }

/** Contratto 1.29: la cronologia di oggi; `live` distingue la stessa cartella viva e chiusa. */
export type TimelineEvent = { at: number; kind: string; text: string; ok?: boolean | null; ref?: string | null }
export type TimelineSession = { session: string; live: boolean; project?: string | null; events: TimelineEvent[] }
export type TimelinePage = { since: number; sessions: TimelineSession[]; more?: boolean }

export type State = {
  v: number; ts: number; host: string
  /** 1.43: epoch s della pubblicazione (decimali ammessi); assente con un relay precedente. */
  published_at?: number | null
  sessions: Session[]; quota: Record<string, QuotaAccount>
  projects: Project[]; night: Night; recap: Recap
  ops?: string[] | null; slash?: string[] | null
  devices?: Device[] | null; recurring?: Recurring[] | null
  choices?: { models: Model[]; efforts: string[] } | null
  share?: { max_bytes: number; any?: boolean } | null
  approvals?: Approval[]
}

export type CmdOp = 'answer' | 'prompt' | 'launch' | 'follow' | 'unfollow' | 'resume' | 'screen' | 'allow_all' | 'last' | 'reopen'
  | 'model' | 'effort' | 'night_add' | 'night_remove' | 'report' | 'interrupt' | 'transcript' | 'file' | 'slash' | 'projects'
  | 'search' | 'timeline' | 'pair_add' | 'approve' | 'decision' | 'unpair' | 'night'
export type Cmd = { id: string; op: CmdOp; session?: string | null; arg?: string | null; issued: number; by: string; text?: string; device?: string; parts?: boolean }
export type CmdResult = { id: string; ok: boolean; text: string; at: number; session?: string | null; job?: string | null }

/** Lo stato come arriva: i campi che mancano prendono i valori di default del contratto. */
export function decodeState(raw: string): State {
  const s = JSON.parse(raw)
  if (typeof s?.v !== 'number' || typeof s?.ts !== 'number' || typeof s?.host !== 'string') throw new Error('not a state')
  const sessions = (s.sessions ?? []).map((x: Session) => ({ ...x, advice: x.advice ?? null, finished: x.finished ?? false, duplicate_of: x.duplicate_of ?? null }))
  return { ...s, sessions, approvals: s.approvals ?? [], quota: s.quota ?? {}, projects: s.projects ?? [], night: { queued: 0, ...s.night }, recap: { date: '', items: [], ...s.recap } }
}
