import type { AgendaRow } from './contract'

/**
 * La notte dal Recap (contratto 1.49, scelta A di Franz del 09/10 21:27), come LocalAgendaNight dell'app: App la mette nel
 * contesto `agendaNight` quando il relay manda la master nella notte. `rows` = le schede da spuntare (`night()`),
 * `queue` manda i testi, un lavoro per scheda.
 */
export type AgendaNight = { readonly can: boolean; readonly rows: AgendaRow[]; queue: (prompts: string[]) => void }
