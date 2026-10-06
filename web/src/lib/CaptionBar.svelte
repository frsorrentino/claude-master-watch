<script lang="ts">
  import type { State } from './contract'
  import { t } from './t'
  import Badge from './Badge.svelte'

  // La barra del titolo nostra quando la web app è installata (Window Controls Overlay). Non è una striscia a parte: la
  // home e le colonne salgono fin in cima, e sopra ogni colonna c'è la sua scheda, larga e posta come la colonna, saldata
  // al bordo della colonna sotto (Franz, 06/10 08:15). È una linguetta, larga quanto il nome, all'inizio della sua colonna
  // (08:21): icona, nome e × stanno solo qui. Si trascina la finestra dai vuoti; i tasti di sistema restano liberi.
  let { st, cols, onFocus, onClose }: { st: State; cols: string[]; onFocus: (name: string) => void; onClose: (name: string) => void } = $props()

  let rects = $state<Record<string, { left: number; width: number }>>({})
  let right = $state(Infinity)

  // Le colonne si spostano per stile (trascinamento, larghezze) e con una transizione di 0,32 s: a ogni cambio si misura
  // per qualche fotogramma, poi si smette.
  function measure() {
    const next: Record<string, { left: number; width: number }> = {}
    for (const name of cols) {
      const el = document.querySelector(`[data-col="${CSS.escape(name)}"]`)?.closest('.col')
      if (el) { const r = el.getBoundingClientRect(); next[name] = { left: r.left, width: r.width } }
    }
    rects = next
    const wco = (navigator as Navigator & { windowControlsOverlay?: { getTitlebarAreaRect(): DOMRect } }).windowControlsOverlay
    const area = wco?.getTitlebarAreaRect()
    right = area && area.width ? area.right : Infinity
  }
  let until = 0
  function follow() {
    until = performance.now() + 400
    const tick = () => { measure(); if (performance.now() < until) requestAnimationFrame(tick) }
    requestAnimationFrame(tick)
  }
  $effect(() => {
    cols.length
    follow()
    const desk = document.querySelector('.cols')
    const mo = new MutationObserver(follow)
    if (desk) mo.observe(desk, { subtree: true, childList: true, attributes: true, attributeFilter: ['style', 'class'] })
    const ro = new ResizeObserver(follow)
    ro.observe(document.body)
    return () => { mo.disconnect(); ro.disconnect() }
  })
</script>

<div class="caption">
  {#each cols as name (name)}
    {@const s = st.sessions.find(x => x.name === name)}
    {@const r = rects[name]}
    {#if s && r}
      <span class="tab" style="left:{r.left}px;max-width:{Math.max(0, Math.min(r.left + r.width, right) - r.left)}px">
        <button class="tb" onclick={() => onFocus(name)}><Badge {s} size={16} /><span class="name">{name}</span></button>
        <button class="x" aria-label={t.tabletClose(name)} title={t.tabletClose(name)} onclick={() => onClose(name)}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
      </span>
    {/if}
  {/each}
</div>

<style>
  /* Solo le schede ricevono i tocchi: il resto della striscia lascia passare alla home e alle colonne sotto. */
  .caption { position: fixed; z-index: 30; top: 0; left: 0; right: 0; height: env(titlebar-area-height, 40px); pointer-events: none; }
  .tab { position: absolute; top: 6px; bottom: -1px; display: flex; align-items: center; gap: 4px; padding: 0 4px 0 12px; overflow: hidden;
    background: var(--bg); border: 1px solid rgb(255 255 255 / .12); border-bottom: 0; border-radius: 12px 12px 0 0;
    pointer-events: auto; app-region: no-drag; -webkit-app-region: no-drag; }
  .tb { min-width: 0; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; height: 100%; }
  .name { white-space: nowrap; overflow: hidden; }
  .x { flex: none; width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center; }
  .x:hover { background: var(--surface); }
</style>
