<script lang="ts">
  import type { Model, Ring } from './overview'
  import { peak } from './dayBars'
  import { WINDOW_S, type Pace } from './quotaHistory'
  import type { Seg } from './workPanel'
  import type { Tone } from './briefCards'
  import { t } from './t'
  import AccountMark from './AccountMark.svelte'

  // Utilizzo e limiti (OverviewScreen dell'app): la quota col doppio anello (5 ore fuori, settimana dentro) e il ritmo,
  // poi il lavoro: «Adesso», chi ti aspetta, il contesto, «Oggi», la notte, l'ora dell'aggiornamento.
  let { model, staleMinutes = null, timeZone, onQuestion, onSession }: {
    model: Model; staleMinutes?: number | null; timeZone?: string; onQuestion: () => void; onSession: (name: string) => void
  } = $props()

  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit', timeZone })
  const dayTime = (at: number) => new Date(at * 1000).toLocaleString('it-IT', { weekday: 'short', hour: '2-digit', minute: '2-digit', timeZone }).replace(',', '')
  const segColour: Record<Seg, string> = { waiting: 'var(--b-warn)', working: 'var(--b-ring)', idle: 'var(--b-good)' }
  const toneColour = (x: Tone) => (x === 'alert' ? 'var(--b-alert)' : x === 'warn' ? 'var(--b-warn)' : 'var(--b-ring)')
  const pl = (n: number, one: string, other: string) => (n === 1 ? one : other)
  // Il doppio anello: archi con stroke-dasharray su un cerchio di lunghezza 100.
  const dash = (pct: number | null) => `${Math.max(0, Math.min(100, pct ?? 0))} 100`
  function paceLine(p: Pace, resetAt: number) {
    const start = resetAt - WINDOW_S
    const at = (ts: number, pct: number) => [((ts - start) / WINDOW_S) * 100, 22 * (1 - pct / 100)] as const
    const pts = p.points.map(s => at(s.ts, s.pct))
    const last = p.points[p.points.length - 1]
    const proj = last && p.projected != null && p.at != null ? [at(last.ts, last.pct), at(p.at, p.projected)] : null
    return { pts: pts.map(([x, y]) => `${x},${y}`).join(' '), proj }
  }
  const total = $derived(model.today.reduce((n, b) => n + b.count, 0))
  const top = $derived(Math.max(1, peak(model.today)))
</script>

{#snippet ringCard(r: Ring)}
  <div class="card">
    <span class="lab">{r.account}</span>
    <div class="rrow">
      <div class="ring">
        <svg viewBox="0 0 92 92" width="92" height="92">
          <g transform="rotate(-90 46 46)" fill="none" stroke-width="9" stroke-linecap="round">
            <circle cx="46" cy="46" r="41.5" stroke="var(--b-track)" />
            {#if r.h5 != null}<circle cx="46" cy="46" r="41.5" pathLength="100" stroke-dasharray={dash(r.h5)} stroke={r.h5 >= 90 ? 'var(--b-alert)' : 'var(--b-ring)'} class="arc" />{/if}
            <circle cx="46" cy="46" r="28.5" stroke="var(--b-track)" />
            {#if r.w7 != null}<circle cx="46" cy="46" r="28.5" pathLength="100" stroke-dasharray={dash(r.w7)} stroke="var(--b-week)" class="arc" />{/if}
          </g>
        </svg>
        <span class="mark"><AccountMark personal={r.personal} size={18} /></span>
      </div>
      <div class="nums">
        <span class="bigrow"><span class="big">{r.h5 != null ? `${r.h5}%` : '–'}</span><span class="unit">{t.ovFiveHours}</span></span>
        {#if r.w7 != null}<span class="week">{r.weekResetAt ? t.ovWeekReset(r.w7, dayTime(r.weekResetAt)) : t.ovWeek(r.w7)}</span>{/if}
        {#if r.resetAt}<span class="sec">{t.quotaResetsAt(hm(r.resetAt))}</span>{/if}
        {#if r.stale}<span class="old">{t.quotaOldWord}</span>{/if}
      </div>
    </div>
    {#if r.pace && r.resetAt}
      {@const l = paceLine(r.pace, r.resetAt)}
      <svg class="pace" viewBox="0 0 100 22" preserveAspectRatio="none" height="22">
        <line x1="0" y1="22" x2="100" y2="22" stroke="var(--b-track)" stroke-width="1" vector-effect="non-scaling-stroke" />
        <polyline points={l.pts} fill="none" stroke="var(--b-ring)" stroke-width="2.5" stroke-linecap="round" vector-effect="non-scaling-stroke" />
        {#if l.proj}<line x1={l.proj[0][0]} y1={l.proj[0][1]} x2={l.proj[1][0]} y2={l.proj[1][1]} stroke="var(--b-second)" stroke-width="1.5" stroke-dasharray="6 6" vector-effect="non-scaling-stroke" />{/if}
      </svg>
      {#if r.pace.projected != null && r.pace.at != null}<span class="sec">{t.ovPace(r.pace.projected, hm(r.pace.at))}</span>{/if}
    {/if}
  </div>
{/snippet}

<div class="ov">
  {#if staleMinutes != null}<p class="stale">{t.staleData(staleMinutes)}</p>{/if}
  <h2>{t.ovQuota}</h2>
  {#if !model.rings.length}<p class="sec">{t.ovNoQuota}</p>{/if}
  {#each model.rings as r (r.account)}{@render ringCard(r)}{/each}

  <h2>{t.ovWork}</h2>
  <div class="card">
    <span class="lab">{t.ovNow}</span>
    <span class="bigrow"><span class="big">{model.now.working}</span><span class="unit">{pl(model.now.working, 'lavora', 'lavorano')}</span></span>
    {#if !model.now.segments.length}
      <span class="sec">{t.ovNoneLive}</span>
    {:else}
      <div class="segs">{#each model.now.segments as s}<i style="background:{segColour[s]}"></i>{/each}</div>
      <div class="legend">
        {#if model.now.waiting}<span><i style="background:{segColour.waiting}"></i>{model.now.waiting} {pl(model.now.waiting, 'aspetta te', 'aspettano te')}</span>{/if}
        {#if model.now.working}<span><i style="background:{segColour.working}"></i>{model.now.working} {pl(model.now.working, 'lavora', 'lavorano')}</span>{/if}
        {#if model.now.idle}<span><i style="background:{segColour.idle}"></i>{model.now.idle} {pl(model.now.idle, 'ferma', 'ferme')}</span>{/if}
      </div>
    {/if}
  </div>

  {#if model.questions}
    <button class="card warn" onclick={onQuestion}>
      <span class="lab">{t.ovQuestions}</span>
      <span class="big">{model.questions.count}</span>
      <span class="qline">{model.questions.oldest} · da {model.questions.age}</span>
    </button>
  {/if}

  {#if model.contexts.length}
    <div class="card">
      <span class="lab">{t.ovContext}</span>
      {#each model.contexts as c (c.name)}
        <button class="ctx" onclick={() => onSession(c.name)}>
          <span class="crow"><b>{c.name}</b><span style="color:{toneColour(c.tone)}">{c.pct}%</span></span>
          {#if c.model || c.effort}<span class="sec">{[c.model, c.effort].filter(Boolean).join(' · ')}</span>{/if}
          <span class="ctrack"><i style="width:{Math.min(100, c.pct)}%;background:{toneColour(c.tone)}"></i></span>
        </button>
      {/each}
    </div>
  {/if}

  <div class="card">
    <span class="lab">{t.ovToday}</span>
    <span class="bigrow"><span class="big">{total}</span><span class="unit">{pl(total, 'evento', 'eventi')}</span></span>
    <div class="bars">
      {#each Array(24) as _, h}
        {@const b = model.today.find(x => x.hour === h)}
        {#if !b}<i class="dot"></i>{:else}<i class="bar" class:zero={b.count === 0} style="height:{b.count === 0 ? 3 : Math.max(3, (64 * b.count) / top)}px"></i>{/if}
      {/each}
    </div>
    <div class="hours"><span>0</span><span>6</span><span>12</span><span>18</span></div>
  </div>

  {#if model.nightQueued > 0}
    <div class="card"><span class="lab">{t.ovNight}</span><span class="bigrow"><span class="big">{model.nightQueued}</span><span class="unit">in coda</span></span></div>
  {/if}

  <p class="upd" class:alert={model.updated.stale}>
    {model.updated.stale ? t.ovStale(model.updated.minutes, model.updated.host) : model.updated.minutes <= 0 ? t.ovUpdatedNow(model.updated.host) : t.ovUpdatedAgo(model.updated.minutes, model.updated.host)}
  </p>
</div>

<style>
  .ov { display: flex; flex-direction: column; gap: 12px; padding: 8px 16px 96px; max-width: 760px; }
  h2 { font-size: 22px; font-weight: 600; padding: 8px 4px 0; }
  .card { background: var(--high); border-radius: 16px; padding: 18px 20px; display: flex; flex-direction: column; gap: 8px; text-align: left; }
  .lab { font-size: 14px; font-weight: 500; color: var(--b-label); }
  .rrow { display: flex; align-items: center; gap: 20px; }
  @container (max-width: 280px) { .rrow { flex-direction: column; } }
  .ring { position: relative; width: 92px; height: 92px; flex: none; }
  .ring .mark { position: absolute; inset: 0; display: grid; place-items: center; }
  .arc { animation: fill .9s cubic-bezier(.2, .8, .2, 1); }
  @keyframes fill { from { stroke-dasharray: 0 100; } }
  .nums { display: flex; flex-direction: column; gap: 4px; align-items: flex-start; }
  .bigrow { display: flex; align-items: flex-end; gap: 6px; }
  .big { font-size: 36px; font-weight: 600; color: #F4F4F4; line-height: 1.1; }
  .unit { font-size: 16px; color: var(--b-second); padding-bottom: 6px; }
  .week { background: var(--b-week); color: #1F1147; border-radius: 999px; padding: 3px 10px; font-size: 14px; font-weight: 500; }
  .sec { font-size: 14px; color: var(--b-second); }
  .old { font-size: 14px; color: var(--b-warn); }
  .pace { width: 100%; display: block; overflow: visible; }
  .segs { display: flex; gap: 4px; height: 12px; }
  .segs i { flex: 1; border-radius: 6px; animation: grow .9s cubic-bezier(.2, .8, .2, 1); transform-origin: left; }
  @keyframes grow { from { scale: 0 1; } }
  .legend { display: flex; flex-wrap: wrap; gap: 4px 16px; font-size: 14px; color: var(--b-second); }
  .legend span { display: flex; align-items: center; gap: 6px; }
  .legend i { width: 8px; height: 8px; border-radius: 50%; }
  .warn { background: var(--b-warn); color: #2A1A00; }
  .warn .lab, .warn .big, .warn .qline { color: #2A1A00; }
  .warn:hover { filter: brightness(1.05); }
  .qline { font-size: 16px; }
  .ctx { display: flex; flex-direction: column; gap: 4px; padding: 4px 0; text-align: left; }
  .crow { display: flex; font-size: 16px; font-weight: 500; }
  .crow b { flex: 1; font-weight: 500; color: #F4F4F4; }
  .ctrack { height: 6px; border-radius: 3px; background: var(--b-track); overflow: hidden; display: block; }
  .ctrack i { display: block; height: 100%; border-radius: 3px; }
  .bars { display: grid; grid-template-columns: repeat(24, 1fr); align-items: end; height: 64px; }
  .bars i { justify-self: center; width: 60%; border-radius: 99px; background: var(--b-ring); }
  .bars i.zero { background: var(--b-track); }
  .bars i.dot { width: 4px; height: 4px; background: var(--b-track); margin-bottom: 0; }
  .hours { display: grid; grid-template-columns: repeat(4, 1fr); font-size: 12.5px; color: var(--b-second); }
  .upd { text-align: center; font-size: 14px; color: var(--text2); padding: 8px 0; }
  .upd.alert { color: var(--b-alert); }
  .stale { background: var(--b-warn); color: #2A1A00; border-radius: 8px; padding: 10px 14px; font-size: 14px; font-weight: 500; }
  @media (prefers-reduced-motion: reduce) { .arc, .segs i { animation: none; } }
</style>
