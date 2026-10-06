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
  .slide { flex: 1; min-height: 0; display: flex; flex-direction: column; animation: up .3s cubic-bezier(.2, .8, .2, 1); }
  /* Il foglio della master chiusa: sta sopra la lista e le getta l'ombra (Profondità, 06/10 20:12). */
  .closed { position: relative; border-radius: 26px 26px 0 0; background: linear-gradient(180deg, var(--master-sheet) 0%, var(--master-sheet-low) 100%);
    box-shadow: 0 -2px 3px rgb(0 0 0 / .7), 0 -14px 28px -6px rgb(0 0 0 / .8), 0 -34px 56px -18px rgb(0 0 0 / .6); }
  .body { flex: 1; min-height: 0; display: flex; flex-direction: column; }
  /* La master sale dal basso, dalla sua barra. */
  @keyframes up { from { translate: 0 40px; opacity: 0; } }
  @media (prefers-reduced-motion: reduce) { .slide { animation: none; } }
</style>
