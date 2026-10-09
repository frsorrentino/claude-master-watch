<script lang="ts">
  import { getContext } from 'svelte'
  import type { AgendaRow } from './contract'
  import type { AgendaNight } from './agendaNight'
  import { doIt, isClaude, mark } from './recapAgenda'
  import { t } from './t'
  // Le schede del Recap nel foglio «Aggiungi alla notte» (Franz, 09/10 20:42: «vorrei vedere anche i da fare dei recap
  // selezionabili»; scelta A delle 21:27), come NightAgendaPicker dell'app: si spuntano, e «Metti stanotte» le manda alla
  // master, un lavoro per scheda col testo di «Fallo». Prima quelle che può fare Claude; le tue chiuse sotto la loro riga.
  // Senza il contesto `agendaNight` (relay prima della 1.49) o senza schede non c'è.
  let { onDone = () => {} }: { onDone?: () => void } = $props()
  const night = getContext<AgendaNight | undefined>('agendaNight')
  const rows = $derived(night?.can ? night.rows : [])
  const id = (r: AgendaRow) => (r.key ?? '').trim() || r.title
  let picked = $state<string[]>([])
  let yoursOpen = $state(false)
  const claude = $derived(rows.filter(r => isClaude(r.blocks)))
  const yours = $derived(rows.filter(r => !isClaude(r.blocks)))
  const toggle = (r: AgendaRow) => { const k = id(r); picked = picked.includes(k) ? picked.filter(x => x !== k) : [...picked, k] }
  function queue() {
    night?.queue(rows.filter(r => picked.includes(id(r))).map(r => doIt(r, t.recapDoText).send))
    picked = []
    onDone()
  }
</script>

{#snippet pick(r: AgendaRow)}
  <label class="row">
    <i class="mark {mark(r.scope)}"></i>
    <span class="col"><b>{r.title}</b>{#if (r.ref ?? '').trim()}<small>{r.ref.trim()}</small>{/if}</span>
    <input type="checkbox" checked={picked.includes(id(r))} onchange={() => toggle(r)} />
  </label>
{/snippet}

{#if rows.length}
  <section>
    <div class="mono label">{t.nightFromRecap.toUpperCase()}</div>
    {#if claude.length}<div class="group">{t.recapClaude(claude.length)}</div>{/if}
    {#each claude as r (id(r))}{@render pick(r)}{/each}
    {#if yours.length}
      <button class="group fold" aria-expanded={yoursOpen} onclick={() => (yoursOpen = !yoursOpen)}>
        <span>{t.recapYou(yours.length)}</span>
        <svg viewBox="0 0 24 24" width="20" height="20"><path d={yoursOpen ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
      {#if yoursOpen}{#each yours as r (id(r))}{@render pick(r)}{/each}{/if}
    {/if}
    <!-- Tonale: il tasto pieno del foglio resta quello del progetto (un solo bottone pieno per schermata). -->
    {#if picked.length}<button class="tonal" onclick={queue}>{t.nightAgendaQueue(picked.length)}</button>{/if}
  </section>
{/if}

<style>
  section { display: flex; flex-direction: column; gap: 2px; margin: 0 0 12px; text-align: left; }
  .label { font-size: 12px; color: var(--text2); margin-bottom: 4px; }
  .group { display: flex; align-items: center; width: 100%; padding: 6px 8px; font-size: 14px; font-weight: 500; color: var(--text2); text-align: left; }
  .group span { flex: 1; }
  .fold { border-radius: 14px; }
  .fold:hover { background: var(--low); }
  .row { display: flex; align-items: flex-start; gap: 12px; padding: 6px 0 6px 8px; border-radius: 14px; cursor: pointer; }
  .row:hover { background: var(--low); }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  b { font-weight: 400; font-size: 16px; color: var(--text); }
  small { font-size: 13px; color: var(--text2); overflow-wrap: anywhere; }
  input { width: 20px; height: 20px; margin: 2px 12px 0 0; flex: none; accent-color: var(--primary); }
  .mark { width: 18px; height: 18px; flex: none; margin-top: 3px; border: 2px solid var(--text2); }
  .mark.personal { border-radius: 50%; } .mark.agency { border-radius: 5px; } .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); } .mark.none { border-color: transparent; }
  .tonal { margin-top: 8px; padding: 14px 20px; border-radius: 999px; background: var(--high); color: var(--primary); font-weight: 500; }
  .tonal:hover { filter: brightness(1.12); }
</style>
