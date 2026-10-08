<script lang="ts">
  import type { AgendaRow } from './contract'
  import { isOpen, mark } from './recapAgenda'
  import { t } from './t'
  // Una riga dell'agenda (tavole 1 e 2 del mockup): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o
  // lo stato se non è aperta; con `onDo` il tasto tonale «Fallo».
  let { row, other = false, onDo = null }: { row: AgendaRow; other?: boolean; onDo?: (() => void) | null } = $props()
  const sub = $derived(!isOpen(row) ? [row.state.trim(), (row.ref ?? '').trim()].filter(Boolean).join(' · ') : other ? t.recapBlocks(row.blocks.trim()) : (row.ref ?? '').trim())
</script>

<div class="row">
  <i class="mark {mark(row.scope)}"></i>
  <div class="t"><b>{row.title}</b>{#if sub}<small>{sub}</small>{/if}</div>
  {#if onDo}<button class="do" onclick={onDo}>{t.recapDo}</button>{/if}
</div>

<style>
  .row { display: flex; gap: 12px; align-items: flex-start; background: var(--surface); border-radius: 18px; padding: 12px 14px; }
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
