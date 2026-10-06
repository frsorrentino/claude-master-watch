<script lang="ts">
  import type { Session } from './contract'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import type { Row } from './summary'
  import { columnState, columnTone } from './ColumnHead.svelte'

  // La linguetta di una colonna nella barra del titolo della web app installata (Window Controls Overlay): icona, nome e ×,
  // all'inizio della colonna e saldata al suo bordo (Franz, 06/10 08:15-08:21). La plancia la posa con la colonna, stessa
  // posizione e stessa animazione, così la segue quando la colonna si sposta o la home passa di lato (08:26). Accanto al nome lo
  // stato e da quanto (variante B, 08:34): la testata della colonna non c'è e la colonna si trascina dalla linguetta.
  let { s, r, now, onFocus, onClose, onGrab }: {
    s: Session; r?: Row; now: number; onFocus: () => void; onClose: () => void; onGrab: (e: PointerEvent) => void
  } = $props()
</script>

<!-- Il trascinamento cattura il puntatore sulla linguetta: il clic arriva a lei, non al nome. -->
<span class="tab" role="button" tabindex="-1" onpointerdown={(e) => { if (!(e.target as HTMLElement).closest('.x')) onGrab(e) }}
  onclick={(e) => { if (!(e.target as HTMLElement).closest('.x')) onFocus() }} onkeydown={(e) => e.key === 'Enter' && onFocus()}>
  <span class="tb"><Badge {s} size={16} /><span class="name">{s.name}</span>{#if r}<span class="st mono" style="color:{columnTone[r.group]}">{columnState(r, now).toLowerCase()}</span>{/if}</span>
  <button class="x" aria-label={t.tabletClose(s.name)} title={t.tabletClose(s.name)} onclick={onClose}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
</span>

<style>
  .tab { height: 100%; min-width: 0; display: flex; align-items: center; gap: 4px; padding: 0 4px 0 12px; overflow: hidden;
    background: var(--bg); border: 1px solid rgb(255 255 255 / .12); border-bottom: 0; border-radius: 12px 12px 0 0;
    app-region: no-drag; -webkit-app-region: no-drag; touch-action: none; cursor: grab; user-select: none; }
  .tb { min-width: 0; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; height: 100%; }
  .name { white-space: nowrap; flex: none; }
  .st { font-size: 10.5px; letter-spacing: .08em; white-space: nowrap; overflow: hidden; min-width: 0; margin-left: 2px; }
  .x { flex: none; width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover { background: var(--surface); }
</style>
