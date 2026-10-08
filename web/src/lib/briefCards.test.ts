import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'
import { decodeState, type QuotaAccount } from './contract'
import { publishedTs, since, STALE_AFTER_S } from './durations'
import { quota, work, type Labels } from './briefCards'

// Gli stessi casi di BriefCardsTest, BriefQuotaAlertTest e BriefQuotaWeekToneTest in Kotlin.
const state = decodeState(readFileSync(new URL('../../../contract/state-1-question.json', import.meta.url), 'utf8'))
const labels: Labels = {
  quota: 'Quota %s', week: 'settimana %s', resetAt: 'reset %s', stale: 'dato vecchio', none: '—',
  active: 'Sessioni attive', waitingPill: '%d in attesa', noQuestions: 'nessuna domanda',
  questions: 'Domande aperte', oldest: 'più vecchia %s',
  night: 'Coda notte', running: 'in corso %s', nothingRunning: 'nessuna in corso',
  update: 'Ultimo aggiornamento', minutes: 'min', now: 'ora', stopped: 'PC fermo',
}
const rome = 'Europe/Rome'
const p0 = state.quota.personal
const only = (q: QuotaAccount) => ({ ...state, quota: { personal: q } })
const fresh = { stale: false } as const

describe('card della quota', () => {
  it('personale viene prima degli altri account', () => {
    const keys = quota(state, labels, rome).map(c => c.key)
    expect(keys[0]).toBe('quota-personal')
    expect(keys.length).toBe(Object.keys(state.quota).length)
  })
  it('porta percentuale, reset e settimana', () => {
    const cards = quota(state, labels, rome)
    const c = cards[0]
    // Titolo «Quota» uguale in Panoramica e nella Scheda; l'account lo dice la forma.
    expect(c.label).toBe('Quota')
    expect(c.shape).toBe('circle')
    expect(cards.find(x => x.key === 'quota-work')!.shape).toBe('square')
    expect(c.value).toBe(String(p0.h5)); expect(c.unit).toBe('%')
    // Sotto la percentuale la ripartenza delle 5 ore (reset_h5, le 18:00 a Roma), non quella settimanale.
    expect(c.secondary).toBe('reset 18:00')
    expect(c.pill).toBe(`settimana ${p0.w7} % · gio 04:00`)
    expect(c.note).toBeNull()
    expect(c.tone).toBe('neutral')
  })
  it('porta anche la frazione della settimana', () => {
    const c = quota(state, labels, rome)[0]
    expect(c.progress!).toBeCloseTo(p0.h5! / 100, 3)
    expect(c.progress2!).toBeCloseTo(p0.w7! / 100, 3)
  })
  it('senza cinque ore l\'anello esterno è vuoto e la settimana sta dentro', () => {
    const c = quota(only({ ...p0, h5: null, reset_h5: null }), { ...labels, weekOnly: 'settimana' }, rome)[0]
    expect(c.progress!).toBeCloseTo(0, 3)
    expect(c.progress2!).toBeCloseTo(p0.w7! / 100, 3)
  })
  it('senza ripartenza delle cinque ore la riga del reset non si disegna', () => {
    expect(quota(only({ ...p0, reset_h5: null }), labels, rome)[0].secondary).toBeNull()
  })
  // Franz, 15/09 15:34: senza lettura delle 5 ore il numero grande diventa la settimana, con il suo reset nella pillola.
  it('senza cinque ore il numero grande è la settimana', () => {
    const c = quota(only({ ...p0, h5: null, reset_h5: null }), { ...labels, weekOnly: 'settimana' }, rome)[0]
    expect(c.value).toBe(String(p0.w7)); expect(c.unit).toBe('%')
    expect(c.secondary).toBeNull()
    expect(c.pill).toBe('settimana · gio 04:00')
    expect(c.note).toBeNull()
    expect(c.tone).toBe('neutral')
  })
  it('senza cinque ore la settimana all\'ottanta per cento è da guardare', () => {
    const c = quota(only({ ...p0, h5: null, reset_h5: null, w7: 85 }), { ...labels, weekOnly: 'settimana' }, rome)[0]
    expect(c.tone).toBe('warn')
  })
  it('quota vecchia dice dato vecchio e diventa grigia', () => {
    const stale = { ...state, quota: Object.fromEntries(Object.entries(state.quota).map(([k, v]) => [k, { ...v, stale: true }])) }
    const c = quota(stale, labels)[0]
    expect(c.pill).toBe('dato vecchio'); expect(c.tone).toBe('stale')
  })
})

describe('card del lavoro', () => {
  it('le attive contano solo chi lavora e le pillole seguono le domande', () => {
    // fixture 1: ledger-api waiting con domanda, atlas-shop busy, field-notes idle, orbit-docs gone
    const active = work(state, fresh, state.ts + 30, labels).find(c => c.key === 'active')!
    expect(active.value).toBe('2')
    expect(active.pill).toBe('1 in attesa'); expect(active.tone).toBe('warn')
  })
  it('senza domande la pillolina è buona', () => {
    const calme = { ...state, sessions: state.sessions.map(s => ({ ...s, question: null })) }
    const cards = work(calme, fresh, calme.ts, labels)
    const active = cards.find(c => c.key === 'active')!
    expect(active.pill).toBe('nessuna domanda'); expect(active.tone).toBe('good')
    expect(cards.some(c => c.key === 'questions')).toBe(false)
  })
  it('la card delle domande dice la più vecchia', () => {
    const c = work(state, fresh, state.ts + 600, labels).find(x => x.key === 'questions')!
    const asked = Math.min(...state.sessions.flatMap(s => (s.question ? [s.question.asked_at] : [])))
    expect(c.value).toBe('1')
    expect(c.pill).toBe('più vecchia ' + since(asked, state.ts + 600))
  })
  it('aggiornamento in minuti e grigio quando il PC è fermo', () => {
    const fresco = work(state, fresh, state.ts, labels).find(c => c.key === 'update')!
    expect(fresco.value).toBe('ora'); expect(fresco.unit).toBeNull(); expect(fresco.tone).toBe('good')
    const fermo = work(state, { stale: true, minutes: 12 }, publishedTs(state) + 12 * 60, labels).find(c => c.key === 'update')!
    expect(fermo.value).toBe('12'); expect(fermo.unit).toBe('min')
    expect(fermo.secondary).toBe('PC fermo'); expect(fermo.tone).toBe('stale')
  })
  it('1.43: col PC lento la card resta verde, con l\'età contata dalla pubblicazione', () => {
    const lento = { ...state, published_at: state.ts + 45 }
    const c = work(lento, { stale: false, slowS: 45 }, state.ts + 45 + 20, labels).find(x => x.key === 'update')!
    expect(c.value).toBe('ora'); expect(c.secondary).toBeNull(); expect(c.tone).toBe('good')
    expect(c.progress).toBeCloseTo(20 / STALE_AFTER_S, 3)
  })
  it('la coda della notte compare solo quando c\'è qualcosa', () => {
    expect(work({ ...state, night: { queued: 0 } }, fresh, state.ts, labels).some(c => c.key === 'night')).toBe(false)
    const c = work({ ...state, night: { queued: 3, running: 'atlas-shop' } }, fresh, state.ts, labels).find(x => x.key === 'night')!
    expect(c.value).toBe('3'); expect(c.pill).toBe('in corso atlas-shop')
  })
})

describe('allarme sulla quota', () => {
  const tono = (pct: number) => quota(only({ ...p0, h5: pct, stale: false }), labels)[0].tone
  it('scala di allarme sulla finestra di cinque ore', () => {
    expect(tono(50)).toBe('neutral'); expect(tono(89)).toBe('neutral')
    expect(tono(90)).toBe('warn'); expect(tono(100)).toBe('alert')
  })
})

// Franz, 16/09 11:52: settimana all'82 % e anello interno senza avviso. Ambra dall'80 %, rosso esaurita, spento se vecchio.
describe('tono della settimana', () => {
  const card = (w7: number, stale = false) => quota(only({ ...p0, h5: 9, w7, stale }), labels)[0]
  it('la settimana ha il suo tono', () => {
    expect(card(79).tone2).toBe('neutral'); expect(card(82).tone2).toBe('warn')
    expect(card(100).tone2).toBe('alert'); expect(card(82, true).tone2).toBe('stale')
  })
  it('i due toni sono indipendenti', () => {
    expect(card(82).tone).toBe('neutral')
  })
})
