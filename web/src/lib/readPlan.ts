// La cadenza delle letture della conversazione, la stessa del telefono (PhoneTerminal.kt): ogni 10 s se la sessione
// lavora, ogni minuto se è ferma, subito se il suo stato cambia, 21 s di attesa per una lettura rimasta senza risposta.
// Il 07/10 la web chiedeva una lettura per sessione a ogni stato, 888 all'ora, ed era il carico più grande sulla fila dei
// comandi del relay (piano prestazioni, Task 5).
export const BUSY_S = 10
export const IDLE_S = 60
export const LOST_S = 21

export type LastRead = { at: number; answered: boolean }

export function shouldRead(s: { state: string }, last: LastRead | undefined, changed: boolean, now: number): boolean {
  // La prima volta si legge sempre, anche una sessione chiusa: la sua colonna mostra la conversazione.
  if (!last) return true
  if (s.state === 'gone') return false
  if (!last.answered) return now - last.at >= LOST_S
  if (changed) return true
  const busy = s.state === 'busy' || s.state === 'awaiting'
  return now - last.at >= (busy ? BUSY_S : IDLE_S)
}
