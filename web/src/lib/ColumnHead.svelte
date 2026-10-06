<script lang="ts">
  import type { Group, Row } from './summary'
  import { age } from './summary'
  import { t } from './t'
  import Badge from './Badge.svelte'
  // La testata di una colonna (TabletColumnHeader): l'icona della sessione, il nome, × per toglierla; sotto lo stato e da
  // quanto, nel colore del gruppo. Si prende per trascinare la colonna sopra un'altra.
  // `bare`: nella web app installata icona, nome e × stanno nella linguetta sopra la colonna (ColumnTab) e lo stato va nella
  // riga di modello, quota e contesto (Franz, 06/10 08:24): qui resta solo lo stato, da cui si trascina ancora la colonna.
  let { r, now, onClose, onGrab, bare = false }: { r: Row; now: number; onClose: () => void; onGrab: (e: PointerEvent) => void; bare?: boolean } = $props()
  const tone: Record<Group, string> = { waiting: 'var(--b-warn)', finished: 'var(--b-good)', working: 'var(--b-ring)', still: 'var(--text2)' }
  const state = $derived([t.tabletState[r.group], (r.group === 'working' || r.group === 'waiting') && r.at ? age(r.at, now) : null].filter(Boolean).join(' · '))
</script>

{#if bare}
<span class="state mono bare" role="presentation" style="color:{tone[r.group]}" onpointerdown={onGrab}>{state.toUpperCase()}</span>
{:else}
<div class="head" role="presentation" onpointerdown={(e) => { if (!(e.target as HTMLElement).closest('button')) onGrab(e) }}>
  <div class="top">
    <Badge s={r.session} size={24} />
    <b>{r.session.name}</b>
    <button class="x" aria-label={t.tabletClose(r.session.name)} title={t.tabletClose(r.session.name)} onclick={onClose}>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
  </div>
  <div class="state mono" style="color:{tone[r.group]}">{state.toUpperCase()}</div>
</div>
{/if}

<style>
  .head { background: var(--bg); border-bottom: 1px solid var(--line); cursor: grab; touch-action: none; user-select: none; }
  .head:active { cursor: grabbing; }
  .top { display: flex; align-items: center; gap: 10px; padding: 6px 4px 0 14px; }
  b { flex: 1; min-width: 0; font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .x { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); cursor: pointer; }
  .x:hover { background: var(--surface); }
  .state { padding: 0 16px 10px; font-size: 11px; letter-spacing: .1em; }
  .bare { padding: 0; white-space: nowrap; flex: none; cursor: grab; touch-action: none; user-select: none; }
</style>
