# brag e HyperFrames per la pipeline promo — verdetto (25/09/2026)

Chiesto da Franz via master (14:32): valutare la skill `brag` per i demo dei comandi e HyperFrames accanto a Remotion.
Solo lettura (la macchina era satura: niente render di prova). Fonti: README di
[latent-spaces/brag](https://github.com/latent-spaces/brag) e di [heygen-com/hyperframes](https://github.com/heygen-com/hyperframes),
letti il 25/09 alle 18:40.

## Cosa sono

**HyperFrames** (HeyGen, Apache 2.0, ~53k stelle). Un video è una cartella con `index.html`: HTML, CSS e media con
attributi `data-*` per tempi e tracce; le animazioni passano da adattatori (GSAP, keyframe CSS, Anime.js, WAAPI, Lottie,
Three.js) resi «seek-safe». Il renderer posiziona ogni fotogramma in Chrome headless e codifica con FFmpeg: stesso input,
stesso MP4. CLI: `npx hyperframes init | lint | check | snapshot | preview | render | publish | doctor`; render in cloud
su HeyGen (con account) o AWS Lambda. Node 22+, FFmpeg. Un «media OS» risolve musica, effetti, icone, voce, LUT da un
catalogo e, se manca, li **genera** con modelli TTS/musica/immagini (quindi chiavi e servizi esterni). Skill per Claude
Code, Codex, Cursor, Gemini CLI. Nessuna sfocatura di movimento né sotto-fotogrammi nella documentazione.

**brag** (latent-spaces, MIT). Una skill: `/brag` dentro un progetto produce in `brag-output/` un piano, un brief di
composizione, il testo per i social e `brag.mp4`, «music, motion, and share copy included», con `--tone` e `--voice`.
Rende con la CLI di HyperFrames; Node 22+, FFmpeg. Installazione: `/plugin marketplace add latent-spaces/brag`.

## Verdetto

**brag: no per i demo dei comandi, forse per i lanci brevi.** Fa un «launch video» del progetto (motion grafica
generica, musica, copy), non una registrazione del terminale: per mostrare `claude-master sessions|launch|talk` servono
VHS o asciinema, oppure i loop che già abbiamo in `tools/promo/remotion/src/loops/` (8 s, 1280×800, terminale disegnato
con i testi veri). Una clip `/brag` sarebbe di forma decisa dalla skill, cioè «anything that looks like a template», il
primo divieto delle direttive di motion; e la musica/voce generate dal media OS di HyperFrames passano da servizi esterni,
contro il vincolo «solo open source locale». Da riprovare solo se serve una clip di lancio da 20 s per una pagina plugin
dove la barra è bassa, con `--voice` spento e musica nostra.

**HyperFrames: non migrare; da tenere per il caso «un solo HTML».** Il film e il corto vivono in un motore Remotion con
scaletta JSON, funzioni pure testate con `node --test`, molle in forma chiusa (`spring.ts`), tavola per battito e
consegna audio separata: HyperFrames non aggiunge niente a questo, e toglie la sfocatura di movimento (`@remotion/motion-blur`
la fa; con HyperFrames andrebbe rifatta con `ffmpeg tmix` come nel post di Aubry). Dove conviene: un pezzo che nasce
come singolo `index.html` senza bundler (i loop per le pagine del sito, un embed, un video che qualcun altro deve poter
aprire nel browser), perché lì il bundling di Remotion è puro attrito e la licenza Apache 2.0 è più pulita della
source-available di Remotion. Le direttive di motion (molle, un fotogramma per battito, niente tempi morti) sono
indipendenti dal motore e valgono in tutti e due.

## Se si riprende in mano

- Prova da 60 s, quando la macchina è libera: `npx hyperframes init` in una cartella di scratch, portare `LoopClaudeMaster`
  (terminale + statusline, 8 s) in HTML con GSAP, `render`, confrontare con `out/consegna/loop-claude-master.mp4` per
  fedeltà dei testi e tempo di resa. Verdetto sul loop, non sul film.
- `brag` solo come esperimento su una pagina plugin, con `--voice` spento; giudicare il risultato contro i divieti del post.
