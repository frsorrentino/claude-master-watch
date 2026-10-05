// I testi visibili, in un posto solo come strings.xml dell'app Android.
export const t = {
  groups: { waiting: 'Ti aspetta', working: 'Al lavoro', idle: 'Ferma', closed: 'Chiuse' } as Record<string, string>,
  states: { waiting: 'domanda', busy: 'al lavoro', awaiting: 'in attesa', idle: 'ferma', gone: 'chiusa' } as Record<string, string>,
  writeTo: (name: string) => `Scrivi a ${name}`,
  send: 'Invia', use: 'Usa', next: 'Prossimi', pick: 'Scegli una sessione dalla home',
  quota5h: (p: number) => `5h ${p}%`, ctx: (p: number) => `ctx ${p}%`, week: (p: number) => `sett. ${p}%`,
  stale: 'dato vecchio', demo: 'Dati di prova', updated: (h: string) => `${h} · aggiornato ora`,
  enterSends: 'Invio manda · Maiusc+Invio a capo', back: 'Indietro',
}
