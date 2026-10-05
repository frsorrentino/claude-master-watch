<script lang="ts">
  import { demoEvents, demoMine, demoState, demoTranscripts } from './lib/demo'
  import type { CmdOp, TranscriptEntry } from './lib/contract'
  import type { Sent, Status } from './lib/chatRules'
  import { toggle } from './lib/speech.svelte'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import Desk from './lib/Desk.svelte'
  import ColumnHead from './lib/ColumnHead.svelte'
  import { t } from './lib/t'
  import MasterDock from './lib/MasterDock.svelte'
  import ReadingPill from './lib/ReadingPill.svelte'
  import { build, MASTER } from './lib/summary'
  import { add, columns, columnsFromPref, columnsPref, groups, sharesFromPref, sharesPref, wide as isWide } from './lib/tablet'

  // Per ora i dati di prova (fixture del contratto); la strada locale del relay arriva con il contratto 1.35.
  const st = demoState
  let transcripts = $state<Record<string, TranscriptEntry[]>>(structuredClone(demoTranscripts))
  let mine = $state<[Sent, Status][]>(structuredClone(demoMine))
  // Quello che si è letto o avviato (turni finiti, resoconto, prossimi passi) e i messaggi mandati da qui, per «Per te».
  let read = $state(new Set<string>())
  const sent = $derived(mine.map(([m]) => m))
  const master = $derived(st.sessions.find(s => s.name === MASTER && s.state !== 'gone') ?? null)

  const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
  const save = (k: string, v: string) => { try { localStorage.setItem(k, v) } catch { /* resta per la sessione */ } }

  // Da 840 px la plancia con le colonne, come il tablet; sotto, una colonna sola come il telefono.
  let width = $state(window.innerWidth)
  const wide = $derived(isWide(width))
  // `#nome` apre quella sessione (link diretto, e i provini).
  const linked = decodeURIComponent(location.hash.slice(1))
  const known = (n: string) => st.sessions.some(s => s.name === n && s.state !== 'gone')

  // Telefono: la sessione aperta, o la home.
  let open = $state<string | null>(known(linked) ? linked : null)
  const session = $derived(st.sessions.find(s => s.name === open) ?? null)

  // Plancia: le colonne scelte (le prime tre senza una scelta salvata), le larghezze in dodicesimi, il lato della home.
  const live = $derived(groups(build(st, sent, st.ts, read)).flatMap(([, rows]) => rows.map(r => r.session.name)))
  let pinned = $state<string[] | null>(columnsFromPref(load('cm.columns')))
  if (known(linked)) pinned = add(pinned ?? columns(null, live), linked)
  const cols = $derived(columns(pinned, live))
  let shares = $state(sharesFromPref(load('cm.shares'), 0))
  $effect(() => { if (shares.length !== cols.length) shares = sharesFromPref(load('cm.shares'), cols.length) })
  let homeRight = $state(load('cm.home_right') === '1')
  const rowOf = $derived(Object.fromEntries(groups(build(st, sent, st.ts, read)).flatMap(([, rows]) => rows.map(r => [r.session.name, r]))))

  // Il cambio con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  const smooth = (go: () => void) => { if (document.startViewTransition) document.startViewTransition(go); else go() }
  function setCols(c: string[]) { smooth(() => { pinned = c; save('cm.columns', columnsPref(c)) }) }
  function setShares(s: number[]) { shares = s; save('cm.shares', sharesPref(s)) }
  function pick(name: string) { if (wide) setCols(add(cols, name)); else smooth(() => { open = name }) }

  function sendTo(name: string, text: string) {
    // Senza trasporto il messaggio resta «inviato al PC»: lo stato vero arriva con il relay.
    mine = [...mine, [{ id: crypto.randomUUID(), session: name, text, sentAt: Math.round(Date.now() / 1000) }, 'sent']]
  }
  const cmd = (name: string) => (op: CmdOp, arg?: string, text?: string) => console.info('[cm] comando', name, op, arg, text)
  const answer = (n: string, x: number) => { pick(n); cmd(n)('answer', String(x)) }
</script>

<svelte:window bind:innerWidth={width} />

{#snippet chat(name: string, inColumn: boolean)}
  {@const s = st.sessions.find(x => x.name === name)!}
  <Chat {st} {s} entries={transcripts[name] ?? []} mine={mine.filter(([m]) => m.session === name)} onSend={(x) => sendTo(name, x)} onPick={pick}
    onAnswer={answer} onCmd={cmd(name)} events={demoEvents} {sent} {read} onRead={(k) => (read = new Set([...read, k]))} onPromptTo={sendTo}
    wide={false} slots={wide ? cols : [name]} onBack={inColumn ? undefined : () => smooth(() => { open = null })} />
{/snippet}

{#if wide}
  <Desk {cols} {shares} {homeRight} onCols={setCols} onShares={setShares}
    onHomeSide={() => smooth(() => { homeRight = !homeRight; save('cm.home_right', homeRight ? '1' : '0') })}>
    {#snippet home()}
      <div class="list"><Home {st} selected={[]} onPick={pick} onAnswer={answer} onStep={(n, x) => { pick(n); sendTo(n, x) }} withMaster /></div>
      {#if !cols.length}<div class="reading"><ReadingPill slots={[null]} here={null} onOpen={pick} /></div>{/if}
    {/snippet}
    {#snippet column(name, grab)}
      <div class="column">
        {#if rowOf[name]}<ColumnHead r={rowOf[name]} now={st.ts} onClose={() => setCols(cols.filter(c => c !== name))} onGrab={grab} />{/if}
        <div class="cbody">{@render chat(name, true)}</div>
      </div>
    {/snippet}
    {#snippet empty()}<p>{t.tabletDeskEmpty}</p>{/snippet}
  </Desk>
{:else}
  <div class="phone">
    {#if session}
      {@render chat(session.name, false)}
    {:else}
      <div class="list"><Home {st} selected={[]} onPick={pick} onAnswer={answer} onStep={(n, x) => { pick(n); sendTo(n, x) }} /></div>
      <div class="reading"><ReadingPill slots={[null]} here={null} onOpen={pick} /></div>
      {#if master}<MasterDock {master} entries={transcripts[MASTER] ?? []} onOpen={() => pick(MASTER)} onSpeak={(x) => toggle(x, MASTER)} />{/if}
    {/if}
  </div>
{/if}
<span class="demo mono">{t.demo}</span>

<style>
  .phone { height: 100%; display: flex; flex-direction: column; max-width: 760px; margin: 0 auto; }
  .list { flex: 1; min-height: 0; }
  .reading:empty { display: none; }
  .reading { padding: 8px 0; }
  .column { height: 100%; display: flex; flex-direction: column; }
  .cbody { flex: 1; min-height: 0; }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; pointer-events: none; }
  @media (max-width: 839px) { .demo { bottom: 0; right: 50%; transform: translateX(50%); font-size: 10px; } }
</style>
