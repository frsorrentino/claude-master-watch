<script lang="ts">
  import type { Ring } from './overview'
  import { forecast, weekProjected } from './tablet'
  import { t } from './t'

  // Il pannello delle quote in fondo alla home della plancia (TabletQuotaPanel): la finestra di 5 ore con la previsione al
  // ritmo di adesso fino alla ripartenza (piena fino a ora, tratteggiata dopo, soglie 80 e 100), poi la settimana col
  // ritmo medio al rinnovo.
  let { ring, now, dataStale = false, timeZone }: { ring: Ring; now: number; dataStale?: boolean; timeZone?: string } = $props()
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit', timeZone })
  const dayTime = (at: number) => new Date(at * 1000).toLocaleString('it-IT', { weekday: 'long', hour: '2-digit', minute: '2-digit', timeZone }).replace(',', ' ')
  const f = $derived(ring.resetAt != null && ring.pace ? forecast(ring.pace, ring.resetAt, now) : null)
  const nowPct = $derived(ring.h5 ?? f?.points[f.points.length - 1]?.[1] ?? 0)
  const week = $derived(weekProjected(ring.w7, ring.weekResetAt, now))
  // Il grafico in un riquadro 300×96: margine a sinistra per le soglie, sotto per le ore.
  const L = 30, W = 262, T = 4, H = 76
  const x = (v: number) => L + W * v
  const y = (p: number) => T + H * (1 - Math.min(100, Math.max(0, p)) / 100)
</script>

<div class="panel">
  <div class="title"><span class="lab">{t.tabletQuotaLabel.toUpperCase()} · {ring.account.toUpperCase()}</span>{#if ring.h5 != null}<b>{ring.h5}%</b>{/if}</div>
  {#if f && ring.resetAt}
    {@const pts = f.points.map(([px, p]) => `${x(px)},${y(p)}`).join(' ')}
    <svg viewBox="0 0 300 96" class="chart" role="img" aria-label={f.projected != null ? t.tabletForecast(f.projected, hm(ring.resetAt)) : t.tabletForecastFlat(hm(ring.resetAt))}>
      {#each [[100, 'var(--line)'], [50, 'color-mix(in srgb, var(--line) 60%, transparent)']] as [p, c]}
        <line x1={L} y1={y(+p)} x2={L + W} y2={y(+p)} stroke={c} stroke-width="1" /><text x="0" y={y(+p) + 3} class="ax">{p}</text>
      {/each}
      <line x1={L} y1={y(80)} x2={L + W} y2={y(80)} stroke="var(--b-warn)" stroke-width="1" stroke-dasharray="6 6" /><text x="0" y={y(80) + 3} class="ax warn">80</text>
      <line x1={x(f.nowX)} y1={T} x2={x(f.nowX)} y2={T + H} stroke="var(--text2)" stroke-opacity=".6" stroke-dasharray="2 4" />
      {#if f.points.length}
        <polygon points={`${pts} ${x(f.points[f.points.length - 1][0])},${T + H} ${x(f.points[0][0])},${T + H}`} fill="var(--b-ring)" fill-opacity=".12" />
        <polyline points={pts} fill="none" stroke="var(--b-ring)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        {#if f.projected != null}
          <line x1={x(f.nowX)} y1={y(nowPct)} x2={x(1)} y2={y(f.projected)} stroke="var(--b-ring)" stroke-width="2" stroke-dasharray="8 6" />
          <circle cx={x(1)} cy={y(f.projected)} r="4.5" fill="var(--bg)" stroke="var(--b-ring)" stroke-width="2" />
          <text x={x(1) - 2} y={y(f.projected) - 8} class="ax val" text-anchor="end">{f.projected}%</text>
        {/if}
        <circle cx={x(f.nowX)} cy={y(nowPct)} r="3.5" fill="var(--b-ring)" />
      {/if}
      <text x={L} y="94" class="ax">{hm(f.start)}</text>
      <text x={Math.min(L + W - 30, Math.max(L + 30, x(f.nowX)))} y="94" class="ax val" text-anchor="middle">{t.tabletNowPct(nowPct)}</text>
      {#if x(f.nowX) + 60 < L + W - 30}<text x={L + W} y="94" class="ax" text-anchor="end">{hm(f.resetAt)}</text>{/if}
    </svg>
    <p class="txt">{f.projected != null ? t.tabletForecast(f.projected, hm(ring.resetAt)) : t.tabletForecastFlat(hm(ring.resetAt))}</p>
  {:else}
    {#if ring.h5 != null}<span class="mini"><i style="width:{Math.min(100, ring.h5)}%"></i></span>{/if}
    <p class="txt dim">{ring.resetAt == null ? t.ovNoQuota : dataStale || ring.stale ? t.tabletForecastStale(hm(ring.resetAt)) : t.tabletForecastNone(hm(ring.resetAt))}</p>
  {/if}
  <hr />
  <div class="title"><span class="lab">{t.tabletWeekLabel.toUpperCase()} · {ring.account.toUpperCase()}</span>{#if ring.w7 != null}<b>{ring.w7}%</b>{/if}</div>
  {#if ring.w7 != null}
    <span class="week">
      {#if week != null}<i class="proj" style="width:{week}%"></i>{/if}
      <i class="used" style="width:{Math.min(100, ring.w7)}%"></i><i class="mark"></i>
    </span>
    {#if ring.weekResetAt}<p class="txt">{week != null ? t.tabletWeekForecast(week, dayTime(ring.weekResetAt)) : t.tabletWeekReset(dayTime(ring.weekResetAt))}</p>{/if}
  {/if}
</div>

<style>
  .panel { border-radius: 16px; background: var(--low); border: 1px solid rgb(255 255 255 / .12); padding: 14px; display: flex; flex-direction: column; gap: 8px; }
  .title { display: flex; align-items: baseline; gap: 8px; flex-wrap: wrap; }
  .lab { flex: 1; font: 11px var(--mono); letter-spacing: .12em; color: var(--text2); }
  .title b { font: 500 14px var(--mono); }
  .chart { width: 100%; height: auto; overflow: visible; }
  .ax { font: 10px var(--mono); fill: var(--text2); }
  .ax.warn { fill: var(--b-warn); }
  .ax.val { fill: var(--text); }
  .txt { font-size: 12.5px; color: var(--text); }
  .txt.dim { color: var(--text2); }
  .mini { height: 4px; border-radius: 2px; background: var(--b-track); overflow: hidden; display: block; }
  .mini i { display: block; height: 100%; background: var(--b-ring); }
  hr { border: 0; height: 1px; background: var(--line); margin: 2px 0; }
  .week { position: relative; height: 10px; border-radius: 5px; background: var(--b-track); display: block; }
  .week i { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 5px; }
  .proj { background: color-mix(in srgb, var(--b-week) 35%, transparent); }
  .used { background: var(--b-week); }
  .mark { left: 80% !important; width: 1.5px; top: -2px !important; bottom: -2px !important; background: var(--b-warn); border-radius: 0 !important; }
</style>
