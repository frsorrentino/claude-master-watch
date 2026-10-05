// I testi visibili, in un posto solo come strings.xml dell'app Android.
const pl = (n: number, one: string, other: string) => `${n === 1 ? one : other} · ${n}`
export const t = {
  summary: {
    waiting: (n: number) => pl(n, 'Ti aspetta', 'Ti aspettano'), finished: (n: number) => pl(n, 'Ha finito', 'Hanno finito'),
    working: (n: number) => pl(n, 'Al lavoro', 'Al lavoro'), still: (n: number) => pl(n, 'Ferma', 'Ferme'),
  } as Record<string, (n: number) => string>,
  closed: (n: number) => pl(n, 'Chiusa', 'Chiuse'), closedCat: (n: number) => `Chiuse · ${n}`, closedMore: (n: number) => `e altre ${n}`,
  outsideTitle: 'Fuori dalle sessioni', outsideSub: 'Quello che non sta in una sessione aperta',
  goal: 'Obiettivo', open: 'Apri i passaggi', close: 'Chiudi i passaggi',
  groups: { waiting: 'Ti aspetta', working: 'Al lavoro', idle: 'Ferma', closed: 'Chiuse' } as Record<string, string>,
  states: { waiting: 'domanda', busy: 'al lavoro', awaiting: 'in attesa', idle: 'ferma', gone: 'chiusa' } as Record<string, string>,
  writeTo: (name: string) => `Scrivi a ${name}`,
  send: 'Invia', use: 'Usa', next: 'Prossimi', pick: 'Scegli una sessione dalla home',
  quota5h: (p: number) => `5h ${p}%`, ctx: (p: number) => `ctx ${p}%`, week: (p: number) => `sett. ${p}%`,
  stale: 'dato vecchio', demo: 'Dati di prova', updated: (h: string) => `${h} · aggiornato ora`,
  homeLast: (hm: string) => `Ultimo esito · ${hm}`, homeLastBare: 'Ultimo esito', listen: 'Ascolta',
  openConversation: 'Apri la conversazione', forYou: 'Per te', stepHint: 'Clic: nel campo · doppio clic: invia subito',
  fyContext: (name: string, p: number) => `Contesto di ${name} al ${p}%`, fyContextDetail: 'Conviene un handoff prima di continuare',
  fyHandoff: 'Handoff e riavvio', fyMore: (n: number) => `+${n} altre`,
  quota: (a: string, h5: number, w7: number) => `${a} ${h5}% · sett. ${w7}%`, quotaOld: (a: string) => `${a} · non aggiornata`,
  home: 'Casa', dockMaster: 'MASTER', conversation: 'Conversazione',
  enterSends: 'Invio manda · Maiusc+Invio a capo', back: 'Indietro',
}
