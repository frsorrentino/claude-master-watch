<script lang="ts">
  import type { Session } from './contract'
  import { t } from './t'
  import Badge from './Badge.svelte'

  // La linguetta di una colonna nella barra del titolo della web app installata (Window Controls Overlay): icona, nome e ×,
  // all'inizio della colonna e saldata al suo bordo (Franz, 06/10 08:15-08:21). La plancia la posa con la colonna, stessa
  // posizione e stessa animazione, così la segue quando la colonna si sposta o la home passa di lato (08:26).
  let { s, onFocus, onClose }: { s: Session; onFocus: () => void; onClose: () => void } = $props()
</script>

<span class="tab">
  <button class="tb" onclick={onFocus}><Badge {s} size={16} /><span class="name">{s.name}</span></button>
  <button class="x" aria-label={t.tabletClose(s.name)} title={t.tabletClose(s.name)} onclick={onClose}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
</span>

<style>
  .tab { height: 100%; min-width: 0; display: flex; align-items: center; gap: 4px; padding: 0 4px 0 12px; overflow: hidden;
    background: var(--bg); border: 1px solid rgb(255 255 255 / .12); border-bottom: 0; border-radius: 12px 12px 0 0;
    app-region: no-drag; -webkit-app-region: no-drag; }
  .tb { min-width: 0; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; height: 100%; }
  .name { white-space: nowrap; overflow: hidden; }
  .x { flex: none; width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover { background: var(--surface); }
</style>
