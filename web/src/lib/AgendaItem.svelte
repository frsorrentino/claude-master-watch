<script lang="ts">
  import type { AgendaRow } from './contract'
  import { isOpen, mark } from './recapAgenda'
  import { untilLabel } from './agendaDates'
  import { t } from './t'
  // Una riga dell'agenda (tavole 1 e 2 del mockup): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o
  // lo stato se non è aperta. Il tocco apre «Approfondisci» (`onOpen`), il tasto tonale «Azioni» il menu (piano del 09/10).
  let { row, other = false, onOpen = null, onActions = null }: { row: AgendaRow; other?: boolean; onOpen?: (() => void) | null; onActions?: (() => void) | null } = $props()
  const sub = $derived(row.state.trim().toLowerCase() === 'sospeso' && row.until ? t.agendaUntil(untilLabel(row.until))
    : !isOpen(row) ? [row.state.trim(), (row.ref ?? '').trim()].filter(Boolean).join(' · ') : other ? t.recapBlocks(row.blocks.trim()) : (row.ref ?? '').trim())
</script>

<!-- svelte-ignore a11y_no_noninteractive_tabindex -->
<div class="row" class:tap={!!onOpen} role={onOpen ? 'button' : undefined} tabindex={onOpen ? 0 : undefined}
  onclick={() => onOpen?.()} onkeydown={(e) => onOpen && (e.key === 'Enter' || e.key === ' ') && e.target === e.currentTarget && (e.preventDefault(), onOpen())}>
  <i class="mark {mark(row.scope)}"></i>
  <div class="t"><b>{row.title}</b>{#if sub}<small>{sub}</small>{/if}</div>
  {#if onActions}<button class="do" onclick={(e) => { e.stopPropagation(); onActions() }}>{t.agendaActions}</button>{/if}
</div>

<style>
  .row { display: flex; gap: 12px; align-items: flex-start; background: var(--surface); border-radius: 18px; padding: 12px 14px; }
  .row.tap { cursor: pointer; }
  .row.tap:hover { box-shadow: inset 0 0 0 1px var(--line); }
  .t { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .t b { font-weight: 500; font-size: 16px; overflow-wrap: anywhere; }
  .t small { color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  /* Tondo personale, quadrato agenzia, rombo postazione. */
  .mark { width: 18px; height: 18px; flex: none; margin-top: 3px; border: 2px solid var(--text2); }
  .mark.personal { border-radius: 50%; }
  .mark.agency { border-radius: 5px; }
  .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); }
  .mark.none { border-color: transparent; }
  .do { background: var(--high); color: var(--primary); border-radius: 999px; padding: 7px 16px; font-weight: 500; white-space: nowrap; flex: none; }
  .do:hover { filter: brightness(1.15); }
</style>
