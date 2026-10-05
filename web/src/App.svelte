<script lang="ts">
  import { demoChats, demoState, type Line } from './lib/demo'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import { t } from './lib/t'
  import MasterDock from './lib/MasterDock.svelte'
  import { MASTER } from './lib/summary'

  // Per ora i dati di prova (fixture del contratto); la strada locale del relay arriva con il contratto 1.35.
  const st = demoState
  let chats = $state<Record<string, Line[]>>(structuredClone(demoChats))
  // Sul telefono si parte dalla home; su desktop con la prima sessione che ti aspetta già aperta.
  // `#nome` apre quella sessione (link diretto, e i provini).
  const linked = decodeURIComponent(location.hash.slice(1))
  let open = $state<string | null>(st.sessions.some(s => s.name === linked) ? linked : window.innerWidth > 760 ? (st.sessions.find(s => s.state === 'waiting')?.name ?? null) : null)
  const session = $derived(st.sessions.find(s => s.name === open) ?? null)

  function send(text: string) {
    if (!open) return
    const now = Math.round(Date.now() / 1000)
    chats[open] = [...(chats[open] ?? []), { id: crypto.randomUUID(), role: 'user', text, at: now }]
  }
  const master = $derived(st.sessions.find(s => s.name === MASTER && s.state !== 'gone') ?? null)
  function speak(text: string) { speechSynthesis.cancel(); const u = new SpeechSynthesisUtterance(text); u.lang = 'it-IT'; speechSynthesis.speak(u) }
  // Il cambio di sessione con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  function pick(name: string) {
    const go = () => { open = name }
    if (document.startViewTransition) document.startViewTransition(go); else go()
  }
</script>

<div class="desk" class:chatOpen={open !== null}>
  <aside>
    <div class="list"><Home {st} selected={open} onPick={pick} onAnswer={(n, x) => { pick(n); send(String(x)) }} onStep={(n, x) => { pick(n); send(x) }} /></div>
    {#if master && open !== MASTER}<MasterDock {master} entries={chats[MASTER] ?? []} onOpen={() => pick(MASTER)} onSpeak={speak} />{/if}
  </aside>
  <main>
    {#if session}
      <Chat {st} s={session} lines={chats[session.name] ?? []} onSend={send} onBack={() => (open = null)} onPick={pick} onAnswer={(n, x) => { pick(n); send(String(x)) }} />
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
    .demo { bottom: auto; top: 4px; }
    .desk.chatOpen aside { display: none; }
    .desk:not(.chatOpen) main { display: none; }
  }
  @media (min-width: 761px) { :global(.back) { display: none; } }
</style>
