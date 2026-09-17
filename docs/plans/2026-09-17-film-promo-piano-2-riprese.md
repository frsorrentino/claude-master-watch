# Film promozionale, piano 2 di 2: le riprese (con l'orologio)

Nasce dall'anteprima del 17/09 sera. Giudizio di Franz: «si vedono perlopiù immagini dell'orologio statiche», «più video e
meno screenshot», «ogni scena parta da un punto per arrivare a un altro ben definito, pertinente a quello che scriviamo»;
▶ deve diventare ■ e si deve sentire la voce vera dell'app; in chiusura l'app, possibilmente la tile, non il quadrante.
Il motore, la musica e la scaletta (piano 1) restano: qui cambiano le clip.

## 1. Regole di ripresa

- **Ogni clip è un'azione con un inizio e una fine dichiarati** (tabella sotto). Niente fermi oltre 1,5 s, tranne la lettura
  vocale, dove lo schermo è fermo per natura e a sinistra scorrono le parole dette.
- Il movimento comincia entro 0,5 s dall'inizio della clip e la clip dura almeno un battito più della scena (margine per
  `clipStart`). `npm run check` rifiuta già una scena più lunga della sua clip.
- Scorrimenti lenti e continui: `input swipe x1 y1 x2 y2 900` (900 ms), mai i colpi secchi di stamattina.
- Una clip per scena, registrata da sola: la connessione cade (tre volte in una notte) e ogni clip si rifà senza le altre.
- Dopo ogni gesto il copione legge l'albero dell'interfaccia e verifica il testo atteso (modalità prova prima della ripresa).
- Lingua di sistema in inglese, demo pilotata, nessun dato personale a schermo; alla fine sempre `teardown`.

## 2. Elenco delle riprese (110 battiti al minuto: 1 battito = 0,545 s)

| Scena · scritta | Battiti | Da | Gesto | A |
|---|---|---|---|---|
| open-3 · «That's fine.» | 6 | quadrante in ambiente (scuro) | `WAKEUP` | quadrante acceso, complication «payments-api ▶»; il film entra nella complication di sinistra |
| list · «Every session. One glance.» | 8 | quadrante | tocco sulla complication | lista delle sessioni che scorre lenta dalla prima alla terza card |
| asks · «It asks.» | 8 | lista ferma | passo demo QUESTION | la notifica con la domanda entra e si apre (anelli di vibrazione sull'arrivo) |
| speaks · «It speaks.» | 18 | schermata della domanda con ▶ | tocco su ▶ | ▶ diventa ■, l'orologio legge (audio vero), a fine lettura ■ torna ▶ |
| answer · «You answer.» | 6 | opzioni «1 yes / 2 no» in vista | pressione lunga su «yes» | conferma dell'invio, poi la sessione torna ▶ in lista |
| follow · «Follow what matters.» | 6 | card payments-api senza campanella | pressione lunga sulla card | campanella accesa sulla card |
| done · «Know when it's done.» | 6 | lista | passo demo DEPLOYED | notifica dell'esito che entra, tocco, schermata «Deployed 2.8.0…» |
| say · «Say what's next.» | 8 | fondo dell'esito, tasto «Write» | tocco, dettatura demo | il testo dettato nel campo, poi «Sent» |
| watch · «Watch it work.» | 8 | card al lavoro | tocco sulla card | terminale con le righe che arrivano (TICK ogni 0,5 s) e lo scorrimento che le segue |
| limits · «Know your limits.» | 10 | panoramica, card «Quota 11 %» | scorrimento lento | grafico del ritmo a cinque ore |
| new · «Start something new.» | 14 | fondo della lista, «New session» | tocco, scelta di storefront, dettatura | la lista con storefront ▶ al lavoro |
| close · cartello finale | 16 | quadrante | uno scorrimento laterale | la tile di Claude Master |

Tre di queste dipendono dai difetti aperti del blocco A (piano 1, progetto §5): scelta del progetto in «New session»,
pressione lunga su «yes» con l'opzione mezza fuori dal bordo, conferma dopo il messaggio dettato. Si indagano con
`superpowers:systematic-debugging` prima di registrare; se uno resta aperto, quella scena si gira fin dove l'app arriva
e la scritta si adegua, senza finti risultati.

## 3. La voce vera, senza toccare app né orologio

L'app legge con il sintetizzatore di sistema e la voce scelta nelle impostazioni. Due vie, in quest'ordine:

1. **`scrcpy` ≥ 2.0 registra schermo e audio interno insieme, via adb**, senza installare nulla sull'orologio: la ripresa di
   «It speaks.» esce già con la voce sincronizzata sul fotogramma in cui ▶ diventa ■. Da verificare sul Pixel Watch: Wear
   OS può rifiutare la cattura dell'audio. `scrcpy` qui non è installato (né nei pacchetti della macchina): va compilato
   o scaricato, con il sì di Franz.
2. **Microfono accanto all'altoparlante dell'orologio**, in una stanza silenziosa (registratore del telefono, 10 cm, tre
   prese): è la voce che si sente davvero dal polso, e il carattere «piccolo altoparlante» è autentico invece che simulato.
   Si allinea alla clip sul fotogramma ▶→■ e si misura come le altre voci (parole ritrovate, cresta, rumore di fondo).

Scartata la via più pulita sulla carta (far scrivere all'app la lettura su file): Franz non vuole modifiche ad app e
orologio per il film. La voce Gemini di stasera resta solo come segnaposto per giudicare abbassamento e parole a schermo.

## 4. Serve Franz (giorno di ripresa, ~40 minuti, orologio fuori dal caricatore)

Lingua di sistema in inglese · debug wireless acceso · quadrante con la nostra complication · **la tile di Claude Master
spostata al primo posto** dopo il quadrante (tra le due oggi ci sono le tile di salute, che nel film non devono passare) ·
volume della voce a metà e stanza silenziosa per la presa audio.

## 5. Ordine di lavoro

1. Difetti del blocco A, uno per volta, con prova al polso.
2. `record.sh`: una funzione per riga della tabella, con le asserzioni sull'albero dell'interfaccia; modalità prova.
3. Prova di `scrcpy` sull'orologio (schermo + audio); se l'audio non passa, presa con il microfono.
4. Riprese, una scena per volta; tavola a 2 fotogrammi al secondo di ogni clip per verificare che ci sia movimento
   dall'inizio alla fine; `teardown`.
5. Clip nella scaletta (`clipStart` sui punti d'inizio), `npm run check`, consegna con `deliver.sh`.
6. Verifiche del film finito (progetto §6): tavola a 1 fotogramma al secondo, lettura ottica contro parole italiane e dati
   personali, bordi a 3-4×, misure audio, dimensioni; poi derivato muto e taglio corto.
