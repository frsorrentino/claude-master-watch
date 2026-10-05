<script lang="ts">
  import type { State } from './contract'
  import { t } from './t'
  import Badge from './Badge.svelte'

  // La barra del titolo nostra quando la web app è installata (Window Controls Overlay), come quella del Pixel Tablet in
  // finestra (CaptionStrip): le colonne come schede a sinistra, la quota a destra; il resto si trascina per spostare la
  // finestra, e i tasti di sistema restano liberi (il browser dice dove, con titlebar-area-*).
  let { st, cols, onFocus, onClose }: { st: State; cols: string[]; onFocus: (name: string) => void; onClose: (name: string) => void } = $props()
</script>

<div class="caption">
  {#each cols as name (name)}
    {@const s = st.sessions.find(x => x.name === name)}
    {#if s}
      <span class="tab">
        <button class="tb" onclick={() => onFocus(name)}><Badge {s} size={16} /><span>{name}</span></button>
        <button class="x" aria-label={t.tabletClose(name)} title={t.tabletClose(name)} onclick={() => onClose(name)}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
      </span>
    {/if}
  {/each}
  {#if !cols.length}<span class="title">claude-master</span>{/if}
  <span class="sp"></span>
  {#each Object.entries(st.quota) as [acc, q]}
    {#if q.h5 != null}<span class="mono" class:stale={q.stale}>{acc} · {t.quota5h(q.h5)}</span>{/if}
  {/each}
</div>

<style>
  /* Il posto che il browser lascia all'app nella barra del titolo; fuori dalla finestra installata la barra non c'è. */
  .caption { position: fixed; z-index: 30; top: env(titlebar-area-y, 0); left: env(titlebar-area-x, 0); width: env(titlebar-area-width, 100%);
    height: env(titlebar-area-height, 0); display: flex; align-items: center; gap: 6px; padding: 0 8px; background: var(--bg); app-region: drag; -webkit-app-region: drag; overflow: hidden; }
  .tab { display: flex; align-items: center; border-radius: 10px; background: var(--surface); padding-left: 8px; app-region: no-drag; -webkit-app-region: no-drag; flex: none; }
  .tb { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 500; height: 26px; }
  .x { width: 28px; height: 28px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover, .tab:hover { filter: brightness(1.15); }
  .title { font-size: 13px; color: var(--text2); }
  .sp { flex: 1; }
  .mono { white-space: nowrap; }
  .stale { color: var(--wait); }
</style>
