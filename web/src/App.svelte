<script lang="ts">
  import { demoEvents, demoMine, demoState, demoTranscripts } from './lib/demo'
  import type { TranscriptEntry } from './lib/contract'
  import type { Sent, Status } from './lib/chatRules'
  import { toggle } from './lib/speech.svelte'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import { t } from './lib/t'
  import MasterDock from './lib/MasterDock.svelte'
  import { MASTER } from './lib/summary'

  // Per ora i dati di prova (fixture del contratto); la strada locale del relay arriva con il contratto 1.35.
  const st = demoState
  let transcripts = $state<Record<string, TranscriptEntry[]>>(structuredClone(demoTranscripts))
  let mine = $state<[Sent, Status][]>(structuredClone(demoMine))
  // Sul telefono si parte dalla home; su desktop con la prima sessione che ti aspetta già aperta.
  // `#nome` apre quella sessione (link diretto, e i provini).
  const linked = decodeURIComponent(location.hash.slice(1))
  let open = $state<string | null>(st.sessions.some(s => s.name === linked) ? linked : window.innerWidth > 760 ? (st.sessions.find(s => s.state === 'waiting')?.name ?? null) : null)
  const session = $derived(st.sessions.find(s => s.name === open) ?? null)

  function sendTo(name: string, text: string) {
    // Senza trasporto il messaggio resta «inviato al PC»: lo stato vero arriva con il relay.
    mine = [...mine, [{ id: crypto.randomUUID(), session: name, text, sentAt: Math.round(Date.now() / 1000) }, 'sent']]
  }
  function send(text: string) { if (open) sendTo(open, text) }
  // Quello che si è letto o avviato (turni finiti, resoconto, prossimi passi) e i messaggi mandati da qui, per «Per te».
  let read = $state(new Set<string>())
  const sent = $derived(mine.map(([m]) => m))
  // Desktop o telefono: la testata mostra nome e badge solo dove la home non sta accanto con la sessione evidenziata.
  let width = $state(window.innerWidth)
  const wide = $derived(width > 760)
  const master = $derived(st.sessions.find(s => s.name === MASTER && s.state !== 'gone') ?? null)
  // Il cambio di sessione con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  function pick(name: string) {
    const go = () => { open = name }
    if (document.startViewTransition) document.startViewTransition(go); else go()
  }
</script>

<svelte:window bind:innerWidth={width} />

<div class="desk" class:chatOpen={open !== null}>
  <aside>
    <div class="list"><Home {st} selected={open} onPick={pick} onAnswer={(n, x) => { pick(n); send(String(x)) }} onStep={(n, x) => { pick(n); send(x) }} /></div>
    {#if master && open !== MASTER}<MasterDock {master} entries={transcripts[MASTER] ?? []} onOpen={() => pick(MASTER)} onSpeak={toggle} />{/if}
  </aside>
  <main>
    {#if session}
      <Chat {st} s={session} entries={transcripts[session.name] ?? []} mine={mine.filter(([m]) => m.session === session.name)} onSend={send} onPick={pick} onAnswer={(n, x) => { pick(n); send(String(x)) }}
        onCmd={(op, arg, text) => console.info('[cm] comando', op, arg, text)}
        events={demoEvents} {sent} {read} onRead={(k) => (read = new Set([...read, k]))} onPromptTo={sendTo} {wide} onBack={wide ? undefined : () => (open = null)} />
    {:else}
      <p class="empty">{t.pick}</p>
    {/if}
  </main>
  <span class="demo mono">{t.demo}</span>
</div>

<style>
  .desk { display: grid; grid-template-columns: 380px 1fr; height: 100%; }
  aside { border-right: 1px solid var(--line); min-height: 0; display: flex; flex-direction: column; }
  .list { flex: 1; min-height: 0; }
  main { min-height: 0; min-width: 0; }
  .empty { color: var(--text2); padding: 40px; }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; }
  /* Sul telefono una colonna sola: la home, e la chat al posto suo quando se ne apre una. */
  @media (max-width: 760px) {
    .desk { grid-template-columns: minmax(0, 1fr); }
    .demo { bottom: 0; right: 50%; transform: translateX(50%); font-size: 10px; }
    .desk.chatOpen aside { display: none; }
    .desk:not(.chatOpen) main { display: none; }
  }
</style>
