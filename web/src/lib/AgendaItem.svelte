<script lang="ts">
  import { getContext } from 'svelte'
  import type { AgendaRow } from './contract'
  import type { AgendaNight } from './agendaNight'
  import { isOpen, mark, menu, type Item } from './recapAgenda'
  import { untilLabel } from './agendaDates'
  import { t } from './t'
  // Una riga dell'agenda (tavole 1 e 2 del mockup): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o
  // lo stato se non è aperta. Come le card delle sessioni (Franz, 10/10 09:24, variante A, al posto del tasto «Azioni»): ˅ la
  // apre sul posto, con il dettaglio salvato, le azioni come pillole («Fallo» piena) e il riferimento; il tocco sul titolo
  // apre «Approfondisci» (`onOpen`). Senza `onItem` niente ˅.
  let { row, other = false, onOpen = null, onItem = null, canWrite = false, today = null }: {
    row: AgendaRow; other?: boolean; onOpen?: (() => void) | null; onItem?: ((item: Item) => void) | null
    canWrite?: boolean; today?: string | null
  } = $props()
  const night = getContext<AgendaNight | undefined>('agendaNight')
  const items = $derived(onItem ? menu(row, canWrite, today, !!night?.can) : [])
  const chips = $derived(items.filter(i => i !== 'deepen' && i !== 'open_ref'))
  let open = $state(false)
  const label: Record<Item, string> = {
    deepen: t.agendaDeepen, do: t.recapDo, night: t.agendaNight, talk: t.agendaTalk, open_ref: t.agendaOpenRef, done: t.agendaDone,
    postpone: t.agendaPostpone, pass_claude: t.agendaPassClaude, pass_me: t.agendaPassMe, remove: t.agendaRemove,
  }
  const sub = $derived(row.state.trim().toLowerCase() === 'sospeso' && row.until ? t.agendaUntil(untilLabel(row.until))
    : !isOpen(row) ? [row.state.trim(), (row.ref ?? '').trim()].filter(Boolean).join(' · ') : other ? t.recapBlocks(row.blocks.trim()) : (row.ref ?? '').trim())
  const detail = $derived((row.detail ?? '').trim())
</script>

<div class="card">
  <div class="head">
    <i class="mark {mark(row.scope)}"></i>
    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
    <div class="t" class:tap={!!onOpen} role={onOpen ? 'button' : undefined} tabindex={onOpen ? 0 : undefined}
      onclick={() => onOpen?.()} onkeydown={(e) => onOpen && (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), onOpen())}>
      <b>{row.title}</b>{#if sub}<small>{sub}</small>{/if}
    </div>
    {#if items.length}
      <button class="chev" aria-expanded={open} aria-label={open ? t.agendaCardClose : t.agendaCardOpen} title={open ? t.agendaCardClose : t.agendaCardOpen} onclick={() => (open = !open)}>
        <svg viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
    {/if}
  </div>
  {#if open && items.length}
    <div class="more">
      <p class:none={!detail}>{detail || t.agendaNoDetail}</p>
      <div class="chips">
        {#each chips as item}<button class="chip" class:do={item === 'do'} class:danger={item === 'remove'} onclick={() => onItem?.(item)}>{label[item]}</button>{/each}
      </div>
      {#if items.includes('open_ref')}<button class="ref" onclick={() => onItem?.('open_ref')}>{t.agendaOpenRef} ↗</button>{/if}
    </div>
  {/if}
</div>

<style>
  .card { background: var(--surface); border-radius: 18px; padding: 12px 6px 12px 14px; }
  .head { display: flex; gap: 12px; align-items: flex-start; }
  .t { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; border-radius: 10px; }
  .t.tap { cursor: pointer; }
  .t.tap:hover { box-shadow: 0 0 0 4px var(--high); background: var(--high); }
  .t b { font-weight: 500; font-size: 16px; overflow-wrap: anywhere; }
  .t small { color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  /* Tondo personale, quadrato agenzia, rombo postazione. */
  .mark { width: 18px; height: 18px; flex: none; margin-top: 3px; border: 2px solid var(--text2); }
  .mark.personal { border-radius: 50%; }
  .mark.agency { border-radius: 5px; }
  .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); }
  .mark.none { border-color: transparent; }
  .chev { flex: none; width: 40px; height: 40px; margin: -8px 0 0; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .chev:hover { background: var(--high); }
  .more { display: flex; flex-direction: column; gap: 12px; padding: 10px 10px 0 0; margin-top: 10px; border-top: 1px solid var(--line); }
  .more p { margin: 0; font-size: 15px; white-space: pre-line; }
  .more p.none { color: var(--text2); }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .chip { border: 1px solid #3A4150; border-radius: 999px; padding: 7px 14px; font-weight: 500; font-size: 14px; color: var(--text); }
  .chip.do { background: var(--primary); border-color: var(--primary); color: var(--on-primary); }
  .chip.danger { color: var(--gone); }
  .chip:hover { filter: brightness(1.15); }
  .ref { align-self: flex-start; color: var(--icon); font-size: 15px; padding: 4px 0; }
</style>
