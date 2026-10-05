<script lang="ts">
  import type { Event, Session, State } from './contract'
  import type { Ring } from './overview'
  import { lastNightReport, minutes, night, parseRecap, personalIcon, recap as recapLines, recaps, type RecapKind, type RecapView } from './diary'
  import { speech, toggle } from './speech.svelte'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import Md from './Md.svelte'

  // Il Registro (DiaryScreen dell'app): blocchi uguali, uno sotto l'altro. Stanotte (coda e «Aggiungi»), la Notte (un
  // lavoro per riga), i giorni (Oggi aperto, i precedenti chiusi, un blocco per sessione), la quota con gli avvisi.
  let { st, events, rings, now, timeZone, onAdd, onRemove, onQuadro, onSession }: {
    st: State; events: Event[]; rings: Ring[]; now: number; timeZone?: string
    onAdd: () => void; onRemove: (id: string) => void; onQuadro: () => void; onSession: (name: string) => void
  } = $props()

  const quotaEvents = $derived(events.filter(e => e.kind === 'quota').sort((a, b) => b.ts - a.ts))
  const history = $derived(recaps(events).filter(e => e.ref !== st.recap.date))
  const report = $derived(lastNightReport(events))
  const items = $derived(st.night.items)
  const empty = $derived(!st.recap.items.length && st.night.queued === 0 && !st.night.running && !quotaEvents.length && !items?.length && !history.length && !report)
  const day = (at: number) => new Intl.DateTimeFormat('en-CA', { timeZone }).format(new Date(at * 1000))
  const todayIso = $derived(day(now))
  const yesterdayIso = $derived(day(now - 86400))
  function title(ref: string | null | undefined) {
    if (ref === todayIso) return t.regToday
    if (ref === yesterdayIso) return t.regYesterday
    const d = ref ? new Date(`${ref}T12:00:00Z`) : null
    return d && !Number.isNaN(d.getTime()) ? d.toLocaleDateString('it-IT', { day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' }) : (ref ?? '')
  }
  const todayView: RecapView = $derived({
    title: '',
    sections: [{ kind: 'open', title: '', lines: [], entries: st.recap.items.map(i => ({ icon: '', name: i.project, url: null, text: i.done, tool: null, next: i.next ?? null, detail: null })) }],
  })
  const todayRaw = $derived(st.recap.items.map(i => `${i.project}: ${i.done}`).join('\n'))
  let open = $state<Record<string, boolean>>({})
  let reportOpen = $state(false)
  const tone: Record<RecapKind, string> = { waiting: 'var(--b-warn)', open: 'var(--b-ring)', closed: 'var(--text2)', other: 'var(--text2)' }
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit', timeZone })
  // Senza la sessione viva il badge si ricava dal recap: forma dall'emoji, glifo dalla sezione.
  function badgeOf(name: string, icon: string, kind: RecapKind): Session {
    const live = st.sessions.find(s => s.name === name && s.state !== 'gone')
    if (live) return live
    const p = personalIcon(icon)
    return { id: name, name, account: p ? 'personale' : 'lavoro', project: name, since: 0, state: kind === 'waiting' ? 'waiting' : kind === 'closed' ? 'gone' : 'idle', icon, account_kind: p ? 'personal' : 'work' }
  }
  const alive = (name: string) => st.sessions.some(s => s.name === name && s.state !== 'gone')
</script>

{#snippet play(raw: string)}
  {@const reading = speech.text === raw}
  <button class="play" aria-label={reading ? t.stopReading : t.readAloud} onclick={(e) => { e.stopPropagation(); toggle(raw) }}>
    {#if reading}<svg viewBox="0 0 24 24" width="18" height="18"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
    {:else}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
  </button>
{/snippet}

{#snippet dayCard(key: string, label: string, view: RecapView, raw: string, startOpen: boolean)}
  {@const count = view.sections.reduce((n, s) => n + s.entries.length, 0)}
  {@const isOpen = open[key] ?? startOpen}
  <div class="card">
    <button class="dhead" aria-expanded={isOpen} onclick={() => (open[key] = !isOpen)}>
      <span class="dt">{label}</span><span class="cnt">{t.regProjects(count)}</span>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d={isOpen ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    {#if isOpen}
      <div class="rtitle"><span>{view.title}</span>{@render play(raw)}</div>
      {#each view.sections as sec}
        {#if sec.title.trim()}<div class="gh" style="--c:{tone[sec.kind]}"><span>{sec.title.toUpperCase()}</span><i></i></div>{/if}
        {#each sec.entries as e}
          {@const live = alive(e.name)}
          <svelte:element this={live ? 'button' : 'div'} class="entry" class:live role={live ? undefined : 'group'} onclick={live ? () => onSession(e.name) : undefined}>
            <span class="etop"><Badge s={badgeOf(e.name, e.icon, sec.kind)} size={22} /><b>{e.name}</b>{#if e.tool}<span class="mono">{e.tool}</span>{/if}</span>
            {#if e.text}<span class="et"><Md text={e.text} /></span>{/if}
            {#if e.detail}<span class="et" class:strong={sec.kind === 'waiting'}><Md text={e.detail} /></span>{/if}
            {#if e.next}<span class="next">↳ {e.next}</span>{/if}
          </svelte:element>
        {/each}
        {#each sec.lines as l}<p class="line">{l}</p>{/each}
      {/each}
    {/if}
  </div>
{/snippet}

{#snippet plainDay(key: string, label: string, raw: string)}
  {@const lines = recapLines(raw)}
  {@const isOpen = open[key] ?? false}
  <div class="card">
    <button class="dhead" aria-expanded={isOpen} onclick={() => (open[key] = !isOpen)}>
      <span class="dt">{label}</span>{#if lines.length}<span class="cnt">{t.regProjects(lines.length)}</span>{/if}
      <svg viewBox="0 0 24 24" width="22" height="22"><path d={isOpen ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    {#if isOpen}
      {#if !lines.length}<div class="rtitle"><span class="raw">{raw}</span>{@render play(raw)}</div>{/if}
      {#each lines as l}<button class="pline" onclick={() => onSession(l.project)}><b>{l.project}</b><span>{l.text}</span></button>{/each}
    {/if}
  </div>
{/snippet}

<div class="diary">
  {#if empty}
    <div class="empty">
      <p>{t.diaryEmpty}</p>
      {#if items}<button class="tonal" onclick={onAdd}>{t.regAdd}</button>{/if}
    </div>
  {:else}
    <div class="card">
      <div class="trow">
        <span class="tcol"><span class="lab">{t.regTonight.toUpperCase()}</span><span class="big">{st.night.queued > 0 ? t.regQueued(st.night.queued) : t.regQueueEmpty}</span></span>
        {#if items}<button class="tonal" onclick={onAdd}>{t.regAdd}</button>{/if}
      </div>
      {#if st.night.running}<p class="running">{t.nightRunning(st.night.running)}</p><div class="wave"><i></i></div>{/if}
      {#if !items}<p class="muted">{t.nightUpdatePc}</p>{/if}
      {#each items ?? [] as job (job.id)}
        <div class="job">
          <span class="jcol"><b>{job.name}</b><span>{job.prompt}</span>{#if job.started}<span class="started">{t.nightStarted}</span>{/if}</span>
          <button class="link" disabled={job.started != null} onclick={() => onRemove(job.id)}>{t.nightRemove}</button>
        </div>
      {/each}
    </div>

    {#if report}
      {@const jobs = night(report.body ?? '')}
      <div class="card" role="button" tabindex="0" onclick={() => (reportOpen = !reportOpen)} onkeydown={(e) => e.key === 'Enter' && (reportOpen = !reportOpen)}>
        <div class="trow"><span class="lab grow">{report.title.toUpperCase()}</span>{@render play(report.body ?? '')}</div>
        {#if !jobs.length || reportOpen}
          <p class="muted pre">{report.body}</p>
        {:else}
          {#each jobs as j, i}
            {#if i > 0}<hr />{/if}
            <div class="njob">
              <span class="mark" class:ok={j.ok}>{j.ok ? '✓' : '✗'}</span>
              <span class="jcol"><span><b>{j.project}</b>{#if j.seconds != null}<span class="mono">{t.regMinutes(minutes(j.seconds))}</span>{/if}</span><span>{j.text}</span></span>
            </div>
          {/each}
        {/if}
      </div>
    {/if}

    {#if st.recap.items.length}{@render dayCard('today', t.regToday, todayView, todayRaw, true)}{/if}
    {#each history as e (e.key)}
      {@const view = parseRecap(e.body ?? '')}
      {#if view.sections.some(s => s.entries.length)}{@render dayCard(e.key, title(e.ref), view, e.body ?? '', false)}{:else}{@render plainDay(e.key, title(e.ref), e.body ?? '')}{/if}
    {/each}

    {#if rings.length || quotaEvents.length}
      <div class="card">
        <span class="lab">{t.regQuota.toUpperCase()}</span>
        {#each rings as r}
          <button class="qcell" onclick={onQuadro}>
            <span class="qt" class:stale={r.stale}>{r.stale ? t.quotaOld(r.account) : t.quota(r.account, r.h5 ?? 0, r.w7 ?? 0)}</span>
            <span class="qtrack"><i style="width:{Math.min(100, Math.max(0, r.h5 ?? 0))}%;background:{r.stale ? 'var(--text2)' : 'var(--b-ring)'}"></i></span>
          </button>
        {/each}
        {#each quotaEvents.slice(0, 5) as e (e.key)}<div class="qev"><span>{e.title}</span><span class="mono">{hm(e.ts)}</span></div>{/each}
      </div>
    {/if}
  {/if}
</div>

<style>
  .diary { display: flex; flex-direction: column; gap: 14px; padding: 12px 16px 96px; max-width: 760px; }
  .card { background: var(--low); border-radius: 20px; padding: 12px 16px; display: flex; flex-direction: column; gap: 8px; }
  .lab { font: 12px var(--mono); letter-spacing: .15em; color: var(--b-label); }
  .grow { flex: 1; }
  .trow { display: flex; align-items: center; gap: 8px; }
  .tcol { flex: 1; display: flex; flex-direction: column; gap: 4px; }
  .big { font-size: 16px; }
  .tonal { background: var(--high); border-radius: 20px; padding: 10px 20px; font-weight: 500; font-size: 14px; }
  .tonal:hover, .play:hover { filter: brightness(1.15); }
  .running { color: var(--busy); font-size: 14px; }
  .wave { height: 4px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .wave i { display: block; height: 100%; width: 40%; background: var(--busy); border-radius: 2px; animation: run 1.6s ease-in-out infinite; }
  @keyframes run { from { translate: -100% 0; } to { translate: 250% 0; } }
  .muted { color: var(--text2); font-size: 14px; }
  .pre { white-space: pre-wrap; }
  .job, .njob { display: flex; align-items: center; gap: 10px; }
  .njob { align-items: flex-start; }
  .jcol { flex: 1; display: flex; flex-direction: column; gap: 2px; font-size: 14px; }
  .jcol > span:not(:first-child) { color: var(--text2); font-size: 13px; }
  .jcol b { font-weight: 600; margin-right: 6px; }
  .started { color: var(--busy) !important; }
  .link { color: var(--icon); padding: 8px 12px; border-radius: 16px; font-weight: 500; font-size: 14px; }
  .link:disabled { color: var(--text2); opacity: .5; cursor: default; }
  .mark { width: 22px; height: 22px; border-radius: 50%; display: grid; place-items: center; font-size: 12px; flex: none; margin-top: 2px; background: #3a1c1c; color: var(--b-alert); }
  .mark.ok { background: #1c3a26; color: var(--b-good); }
  hr { border: 0; height: 1px; background: var(--line); }
  .dhead { display: flex; align-items: center; gap: 8px; width: 100%; text-align: left; border-radius: 8px; }
  .dt { flex: 1; font-size: 16px; font-weight: 500; }
  .cnt { font-size: 12.5px; color: var(--text2); font-weight: 500; }
  .rtitle { display: flex; align-items: center; gap: 8px; color: var(--text2); font-size: 14px; }
  .rtitle span { flex: 1; }
  .raw { white-space: pre-wrap; }
  .play { width: 36px; height: 36px; border-radius: 50%; background: var(--high); color: var(--icon); display: grid; place-items: center; flex: none; }
  .gh { display: flex; align-items: center; gap: 10px; margin: 6px 8px 0; font: 12px var(--mono); letter-spacing: .06em; color: var(--c); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--c) 25%, transparent); }
  .entry { display: flex; flex-direction: column; gap: 4px; padding: 10px 12px; border-radius: 16px; background: var(--surface); text-align: left; }
  .entry.live:hover { filter: brightness(1.1); }
  .etop { display: flex; align-items: center; gap: 10px; }
  .etop b { flex: 1; font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .et { font-size: 14.5px; color: var(--text2); }
  .et.strong { color: var(--text); }
  .next { color: var(--icon); font-size: 14.5px; }
  .line { font-size: 13px; color: var(--text2); padding: 0 4px; }
  .pline { display: flex; gap: 10px; padding: 4px 0; text-align: left; font-size: 14px; }
  .pline b { width: 128px; flex: none; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .pline span { color: var(--text2); white-space: nowrap; overflow: hidden; }
  .qcell { display: flex; flex-direction: column; gap: 6px; padding: 8px 10px; border-radius: 12px; background: var(--bg); text-align: left; }
  .qt { font-size: 12.5px; font-weight: 500; color: var(--text2); }
  .qt.stale { color: var(--wait); }
  .qtrack { height: 4px; border-radius: 2px; background: var(--b-track); overflow: hidden; display: block; }
  .qtrack i { display: block; height: 100%; }
  .qev { display: flex; gap: 8px; font-size: 14px; }
  .qev span:first-child { flex: 1; white-space: nowrap; overflow: hidden; }
  .empty { min-height: 60vh; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: var(--text2); }
  @media (prefers-reduced-motion: reduce) { .wave i { animation: none; } }
</style>
