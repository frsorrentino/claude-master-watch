<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterDock from './MasterDock.svelte'

  // La home come sul telefono (summaryPage di MainActivity): la lista con la barra della master in fondo; la master si
  // espande sul posto, dentro la home, con la sua barra in cima, e torna ridotta toccando la barra. Non diventa mai una
  // colonna o una pagina a parte (segnalazione di Franz, 05/10 17:45: si sganciava dalla home e non tornava più).
  // Da chiusa, sotto la barra, il campo della master: chi scrive apre la conversazione (Franz, 06/10 10:45).
  let { master, entries, open, onToggle, onSpeak, quick, list, chat }: {
    master: Session | null; entries: Entry[]; open: boolean; onToggle: (open: boolean) => void; onSpeak: (text: string) => void
    /** Il campo completo della master (lo stesso Composer della conversazione, con il +). */
    quick: Snippet
    list: Snippet; chat: Snippet
  } = $props()
</script>

<div class="pane">
  {#if master && open}
    <div class="slide">
      <MasterDock {master} {entries} expanded onToggle={() => onToggle(false)} {onSpeak} />
      <div class="body">{@render chat()}</div>
    </div>
  {:else}
    <div class="body">{@render list()}</div>
    {#if master}<div class="closed"><MasterDock {master} {entries} onToggle={() => onToggle(true)} {onSpeak} />{@render quick()}</div>{/if}
  {/if}
</div>

<style>
  .pane { height: 100%; display: flex; flex-direction: column; min-height: 0; }
  .slide { flex: 1; min-height: 0; display: flex; flex-direction: column; animation: up .45s var(--spring); }
  /* La master chiusa: linguetta e campo su un solo fondo, velato di celeste (Celeste velato, 06/10 21:23). */
  .closed { border-radius: 26px 26px 0 0; background: var(--master-high); }
  .body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
  /* La master sale dal basso, dalla sua barra. */
  @keyframes up { from { translate: 0 40px; opacity: 0; } }
  @media (prefers-reduced-motion: reduce) { .slide { animation: none; } }
</style>
