// La riga della quota nella home (QuotaLine): con almeno un account aggiornato, quelli non aggiornati non si mostrano
// e l'altro prende tutta la larghezza (segnalazione 05/10 20:54). Se sono tutti vecchi restano, con «non aggiornata».
export function shown<T extends { stale?: boolean }>(rows: [string, T][]): [string, T][] {
  const fresh = rows.filter(([, r]) => !r.stale)
  return fresh.length ? fresh : rows
}
