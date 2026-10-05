import stateRaw from '../../../contract/state-1-question.json?raw'
import { decodeState } from './contract'

// I dati di prova: lo stato della fixture e una conversazione breve per sessione (finché non arriva il relay locale).
export const demoState = decodeState(stateRaw)

export type Line = { id: string; role: 'user' | 'assistant'; text: string; at: number }
const t0 = demoState.ts - 900
export const demoChats: Record<string, Line[]> = {
  'ledger-api': [
    { id: 'u1', role: 'user', text: 'Prepare the deploy of 2.4 and wait for my ok', at: t0 },
    { id: 'a1', role: 'assistant', text: 'The build is ready and the migration notes are checked. I\'m waiting for the client\'s ok before the deploy.', at: t0 + 120 },
  ],
  'atlas-shop': [
    { id: 'u1', role: 'user', text: 'Tag the release candidate and run the checkout tests', at: t0 },
    { id: 'a1', role: 'assistant', text: 'Checkout suite green: 48 passed. The release candidate is tagged as 2.4.0-rc1; the payment suite still has two failures in the refund path.\n\nEsito: release candidate tagged, payments to fix\nProssimi: fix the refund path · run the payment suite', at: t0 + 400 },
  ],
  'field-notes': [
    { id: 'u1', role: 'user', text: 'Rewrite the README with the three sections', at: t0 },
    { id: 'a1', role: 'assistant', text: 'README rewritten with the three sections asked for.\n\nEsito: README rewritten\nProssimi: apri la PR · aggiorna il changelog · tagga la v1.2', at: t0 + 60 },
  ],
}
