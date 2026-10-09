<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { Session } from './contract'
  import type { Entry } from './masterHome'
  import MasterDock from './MasterDock.svelte'

  // La home come sul telefono (summaryPage di MainActivity): la lista con la barra della master in fondo; la master si
  // espande sul posto, dentro la home, con la sua barra in cima, e torna ridotta toccando la barra. Non diventa mai una
  // colonna o una pagina a parte (segnalazione di Franz, 05/10 17:45: si sganciava dalla home e non tornava più).
  // Da chiusa, sotto la barra, il campo della master: chi scrive apre la conversazione (Franz, 06/10 10:45).
  // Le misure (Franz, 08/10 18:15): sul telefono barra e tutto schermo, con una testata sola che porta il menu ≡ (`menu`);
  // sulla plancia (`halfStops`) anche la metà, con la home sopra e la conversazione sotto (`half`, cambiata da `onHalf`).
  // A tutta altezza la testata della home non si vede: il menu ≡ va nella barra della master, come sul telefono (09/10 21:26).
  let { master, entries, open, onToggle, onSpeak, quick, list, chat, menu, halfStops = false, half = false, onHalf = () => {} }: {
    master: Session | null; entries: Entry[]; open: boolean; onToggle: (open: boolean) => void; onSpeak: (text: string) => void
    menu?: Snippet; halfStops?: boolean; half?: boolean; onHalf?: (half: boolean) => void
    /** Il campo completo della master (lo stesso Composer della conversazione, con il +). */
    quick: Snippet
    list: Snippet; chat: Snippet
  } = $props()
</script>

<div class="pane">
  {#if master && open && halfStops}
    <!-- La plancia: la home resta sotto e il foglio cresce fino a metà o a tutta altezza; a metà la home sopra si tocca. -->
    <div class="body above" inert={!half}>{@render list()}</div>
    <div class="sheet" class:full={!half}>
      <MasterDock {master} {entries} expanded toggleUp={half} onToggle={() => (half ? onToggle(false) : onHalf(true))}
        onToggleKey={() => onHalf(!half)} onClose={half ? () => onToggle(false) : undefined} trailing={half ? undefined : menu} {onSpeak} />
      <div class="body">{@render chat()}</div>
    </div>
  {:else if master && open}
    <div class="slide">
      <MasterDock {master} {entries} expanded screen trailing={menu} onToggle={() => onToggle(false)} {onSpeak} />
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
  .above { overflow: hidden; }
  /* Il foglio della plancia: 55 % della colonna, tutta a schermo intero; cresce dalla barra quando si apre. */
  .sheet { flex: 0 0 55%; min-height: 0; display: flex; flex-direction: column; border-radius: 26px 26px 0 0; overflow: hidden;
    transition: flex-basis .45s var(--spring); animation: grow .45s var(--spring); }
  .sheet.full { flex-basis: 100%; }
  @keyframes grow { from { flex-basis: 64px; } }
  /* La master sale dal basso, dalla sua barra. */
  @keyframes up { from { translate: 0 40px; opacity: 0; } }
  @media (prefers-reduced-motion: reduce) { .slide, .sheet { animation: none; transition: none; } }
</style>
