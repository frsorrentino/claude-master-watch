<script lang="ts">
  import { demoChats, demoState, type Line } from './lib/demo'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import { t } from './lib/t'

  // Per ora i dati di prova (fixture del contratto); la strada locale del relay arriva con il contratto 1.35.
  const st = demoState
  let chats = $state<Record<string, Line[]>>(structuredClone(demoChats))
  let open = $state<string | null>(st.sessions.find(s => s.state === 'waiting')?.name ?? null)
  const session = $derived(st.sessions.find(s => s.name === open) ?? null)

  function send(text: string) {
    if (!open) return
    const now = Math.round(Date.now() / 1000)
    chats[open] = [...(chats[open] ?? []), { id: crypto.randomUUID(), role: 'user', text, at: now }]
  }
  // Il cambio di sessione con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  function pick(name: string) {
    const go = () => { open = name }
    if (document.startViewTransition) document.startViewTransition(go); else go()
  }
</script>

<div class="desk" class:chatOpen={open !== null}>
  <aside><Home {st} selected={open} onPick={pick} /></aside>
  <main>
    {#if session}
      <Chat s={session} lines={chats[session.name] ?? []} onSend={send} onBack={() => (open = null)} />
    {:else}
      <p class="empty">{t.pick}</p>
    {/if}
  </main>
  <span class="demo mono">{t.demo}</span>
</div>

<style>
  .desk { display: grid; grid-template-columns: 380px 1fr; height: 100%; }
  aside { border-right: 1px solid var(--line); min-height: 0; }
  main { min-height: 0; min-width: 0; }
  .empty { color: var(--text2); padding: 40px; }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; }
  /* Sul telefono una colonna sola: la home, e la chat al posto suo quando se ne apre una. */
  @media (max-width: 760px) {
    .desk { grid-template-columns: 1fr; }
    .desk.chatOpen aside { display: none; }
    .desk:not(.chatOpen) main { display: none; }
  }
  @media (min-width: 761px) { :global(.back) { display: none; } }
</style>
