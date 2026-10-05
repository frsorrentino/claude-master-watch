import stateRaw from '../../../contract/state-1-question.json?raw'
import { decodeState } from './contract'

// I dati di prova: lo stato della fixture e una conversazione breve per sessione (finché non arriva il relay locale).
// In più la master, come nel provino della sua casa, e field-notes seguita, così «Per te» ha chi ha finito.
const base = decodeState(stateRaw)
export const demoState = {
  ...base,
  // Il recap di oggi con un progetto senza sessione, così «Per te» propone il suo prossimo passo.
  // La finestra delle 5 ore cominciata da due ore, così il ritmo ha letture da mostrare.
  quota: { ...base.quota, personal: { ...base.quota.personal, reset_h5: base.ts + 3 * 3600 } },
  recap: { ...base.recap, items: [...base.recap.items, { project: 'orbit-docs', done: 'API pages drafted', next: 'Review the API pages' }] },
  sessions: [
    { id: 'm', name: 'master', account: 'personal', project: 'personali/claude-master', state: 'idle' as const, since: base.ts - 3600, context: 34, attached: true,
      model: { id: 'claude-opus-5-5', label: 'Opus 5.5' }, effort: 'high', suggestion: 'commit the README changes and open a PR' },
    // Contesto al 64 %: la proposta «handoff, poi /clear» del contratto 1.37 si vede nella sua chat.
    ...base.sessions.map(s => (s.name === 'field-notes' ? { ...s, followed: true, context: 64, outcome: { ...s.outcome!, at: base.ts - 900 } } : s)),
  ],
}

import eventsRaw from '../../../contract/events-sample.json?raw'
import type { Event } from './contract'
export const demoEvents: Event[] = JSON.parse(eventsRaw)

import type { TranscriptEntry } from './contract'
import type { Sent, Status } from './chatRules'

// Le conversazioni di prova, come i provini della chat (SessionSheetTest: sheetTranscript, sheetTables).
const t0 = demoState.ts - 600
const said = (id: string, text: string, at: number, origin = 'pc'): TranscriptEntry => ({ id, role: 'user', text, at, origin })
const claude = (id: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'assistant', text, at, ...extra })
const tool = (id: string, tool: string, text: string, at: number, extra: Partial<TranscriptEntry> = {}): TranscriptEntry => ({ id, role: 'tool', tool, text, at, ...extra })
export const demoTranscripts: Record<string, TranscriptEntry[]> = {
  master: [
    said('u1', 'Lancia claude-master sulla fase 2.2', demoState.ts - 660, 'phone'),
    claude('a1', 'Lanciata claude-master sulla fase 2.2. Solo commit locali: push e release con il tuo ok.\n\nEsito: Fase 2.2 avviata su claude-master\nProssimi: distilla il confronto nella kb · prova la casa dal vivo', demoState.ts - 600),
  ],
  'ledger-api': [
    said('u1', 'Prepare the deploy of 2.4 and wait for my ok', t0 - 300),
    tool('a1', 'Bash', './gradlew assembleRelease', t0 - 250, { note: 'Build the release' }),
    tool('a2', 'Read', 'docs/migrations/2.4.md', t0 - 200),
    claude('a3', "The build is ready and the **migration notes** are checked. I'm waiting for the client's ok before the deploy.", t0 - 120, { turn: { started: t0 - 300, ended: t0 - 118, out: 412 } }),
  ],
  'atlas-shop': [
    said('u1.0', 'Add the Tuesday meeting notes to the draft', t0),
    claude('a1.0', "I'll read the draft first.", t0 + 5),
    tool('a1.1', 'Read', 'docs/draft.md', t0 + 5),
    tool('a3.0', 'Bash', 'grep -n Tuesday notes/*.md', t0 + 20, { note: 'Find the Tuesday notes', error: true }),
    claude('a4.0', 'The notes file was missing, so I added the Tuesday section to the draft by hand.', t0 + 40, { turn: { started: t0, ended: t0 + 45, in: 4020, out: 130 } }),
    tool('a6.0', 'Write', 'docs/cover.png', t0 + 152, { files: [{ path: '/w/field-notes/docs/cover.png', mime: 'image/png', size: 48_000 }] }),
    tool('a7.0', 'SendUserFile', 'Tuesday minutes', t0 + 155, { files: [{ path: '/w/field-notes/docs/minutes.pdf', mime: 'application/pdf', size: 212_000 }] }),
    said('p1.0', 'Also add the attendees list', t0 + 158, 'phone'),
  ],
  'field-notes': [
    said('u1', 'Dammi i numeri', t0, 'phone'),
    claude('a1', 'I file toccati:\n\n| file | righe |\n|---|---|\n| Repo.kt | 336 |\n| Slash.kt | 47 |\n\nI numeri\n\n| cosa | ora | a mezzogiorno |\n|---|---|---|\n| post del film (01/10) | 223 visualizzazioni, 4 repost | 121 visualizzazioni, 1 repost |\n| nuovi follower | 1 | — |\n\nDettagli in https://claude.ai/artifact/RGdmD5fx e nel file `docs/numeri.md`.\n\nEsito: README rewritten\nProssimi: apri la PR · aggiorna il changelog · tagga la v1.2', t0 + 60, { turn: { started: t0 + 5, ended: t0 + 60, out: 288 } }),
  ],
}
/** I messaggi mandati da qui, col loro stato. */
export const demoMine: [Sent, Status][] = [
  [{ id: 'c1', session: 'atlas-shop', text: 'Also add the attendees list', sentAt: t0 + 156 }, 'working'],
  [{ id: 'c2', session: 'field-notes', text: 'Dammi i numeri', sentAt: t0 - 2 }, 'done'],
]

/** Le letture della quota delle 5 ore che l'app tiene da sé, per il ritmo (QuotaHistory): qui tre dell'account personale. */
export const demoSamples = {
  personal: [{ ts: demoState.ts - 7200, pct: 4 }, { ts: demoState.ts - 3600, pct: 7 }, { ts: demoState.ts, pct: 11 }],
}

/**
 * Quello che il relay risponderebbe a `search` (contratto 1.27) cercando nelle conversazioni di prova: un punto trovato per
 * voce, la riga intorno (fino a 160 caratteri) e la parte trovata. Finto: serve finché la web app non ha il trasporto.
 */
import type { SearchPage } from './contract'
export function demoSearch(q: string): SearchPage {
  const needle = q.toLowerCase()
  const hits = Object.entries(demoTranscripts).flatMap(([session, entries]) => entries.flatMap(e => {
    const text = e.text ?? ''
    const line = text.split('\n').find(l => l.toLowerCase().includes(needle))
    if (!line) return []
    const snippet = line.slice(0, 160)
    const i = snippet.toLowerCase().indexOf(needle)
    return i < 0 ? [] : [{ session, live: demoState.sessions.some(s => s.name === session && s.state !== 'gone'), entry: e.id, role: e.role, at: e.at ?? null, snippet, match: [i, i + needle.length] }]
  }))
  return { hits, more: false }
}

/** La cronologia di oggi (contratto 1.29) come la manderebbe il relay, per i dettagli della plancia. */
import type { TimelinePage } from './contract'
const d0 = demoState.ts - 5 * 3600
export const demoTimeline: TimelinePage = { since: d0, sessions: [
  { session: 'ledger-api', live: true, events: [{ at: d0 + 600, kind: 'prompt', text: 'Prepare the deploy of 2.4', ref: 'ledger-api' }, { at: d0 + 4000, kind: 'commit', text: 'bump version to 2.4.0', ref: '4c1e9a2' }, { at: d0 + 9000, kind: 'test', text: 'migration suite', ok: true }] },
  { session: 'atlas-shop', live: true, events: [{ at: d0 + 1200, kind: 'commit', text: 'fix the cart totals', ref: '4be1c2a' }, { at: d0 + 2400, kind: 'prompt', text: 'ok, run the checkout tests', ref: 'atlas-shop' }, { at: d0 + 6000, kind: 'test', text: 'checkout suite', ok: true }, { at: d0 + 12000, kind: 'test', text: 'payment suite', ok: false }, { at: d0 + 15000, kind: 'outcome', text: 'release candidate tagged, payments to fix' }] },
] }
