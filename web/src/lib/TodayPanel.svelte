<script lang="ts">
  import { peak, type Bar } from './dayBars'
  import { t } from './t'
  // «Oggi» (dalla Panoramica tolta il 09/10, l'unica statistica che in home non c'era), come TodayPanel nell'app: gli eventi
  // della giornata per ora, le ore non ancora arrivate a puntino, il totale a destra.
  let { bars }: { bars: Bar[] } = $props()
  const total = $derived(bars.reduce((n, b) => n + b.count, 0))
  const top = $derived(Math.max(1, peak(bars)))
  const STEP = 300 / 24, BW = STEP * 0.6, H = 56
</script>

<div class="panel">
  <div class="title"><span class="lab">{t.todayLabel.toUpperCase()}</span><b>{total}</b></div>
  <svg viewBox="0 0 300 {H}" class="chart" role="img" aria-label={t.todayLabel}>
    {#each Array.from({ length: 24 }, (_, h) => h) as h}
      {@const b = bars.find(x => x.hour === h)}
      {@const x = h * STEP + (STEP - BW) / 2}
      {#if !b}<circle cx={x + BW / 2} cy={H - 2} r="2" fill="var(--b-track)" />
      {:else}{@const tall = b.count === 0 ? 3 : (H * b.count) / top}
        <rect {x} y={H - tall} width={BW} height={tall} rx={BW / 2} fill={b.count === 0 ? 'var(--b-track)' : 'var(--b-ring)'} />{/if}
    {/each}
  </svg>
  <div class="hours">{#each ['0', '6', '12', '18'] as h}<span>{h}</span>{/each}</div>
</div>

<style>
  .panel { border-radius: 16px; background: var(--low); border: 1px solid rgb(255 255 255 / .12); padding: 14px; display: flex; flex-direction: column; gap: 8px; }
  .title { display: flex; align-items: baseline; gap: 8px; }
  .lab { flex: 1; font: 11px var(--mono); letter-spacing: .12em; color: var(--text2); }
  .title b { font: 500 14px var(--mono); }
  .chart { width: 100%; height: auto; }
  .hours { display: flex; font: 10px var(--mono); color: var(--text2); }
  .hours span { flex: 1; }
</style>
