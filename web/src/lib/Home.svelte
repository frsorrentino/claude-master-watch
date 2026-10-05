<script lang="ts">
  import { flip } from 'svelte/animate'
  import type { State } from './contract'
  import { build, type Group, type Row } from './summary'
  import { summary as outcomeSummary } from './outcome'
  import { parseSteps } from './nextSteps'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import Options from './Options.svelte'

  // La home del telefono (SummaryList.kt): ogni sessione una card nei gruppi del bisogno; il tocco apre la sessione, ▼ la
  // apre sul posto. La domanda ha le opzioni subito, chi ha finito i consigli come tasti, sotto la barretta del contesto.
  let { st, selected, onPick, onAnswer, onStep }: {
    st: State; selected: string | null; onPick: (name: string) => void
    onAnswer: (session: string, n: number) => void; onStep: (session: string, text: string) => void
  } = $props()
  const model = $derived(build(st, [], st.ts, new Set()))
  let expanded = $state<string | null>(null)
  const tone: Record<Group, string> = { waiting: 'var(--b-warn)', finished: 'var(--b-good)', working: 'var(--b-ring)', still: 'var(--text2)' }
  const groups = $derived((['waiting', 'finished', 'working', 'still'] as Group[]).map(g => [g, model.rows.filter(r => r.group === g)] as [Group, Row[]]).filter(([, rows]) => rows.length > 0))
  const ctxTone = (p: number) => (p >= 90 ? 'var(--b-alert)' : p >= 75 ? 'var(--b-warn)' : 'var(--b-ring)')
  function text(r: Row) {
    const parsed = parseSteps(r.text ?? '')
    const shown = r.group === 'waiting' ? parsed.text : r.session.outcome ? outcomeSummary(r.session.outcome) : parsed.text
    return { body: shown.replace(/\*\*|__|`/g, '').trim(), steps: r.group === 'waiting' ? [] : parsed.steps }
  }
</script>

<section class="home">
  {#each groups as [group, rows] (group)}
    <h2 class="gh" style="--t:{tone[group]}"><span>{t.summary[group](rows.length).toUpperCase()}</span><i></i></h2>
    {#each rows as r (r.session.name)}
      {@const s = r.session}
      {@const x = text(r)}
      {@const open = expanded === s.name}
      <div class="card" class:waiting={group === 'waiting'} class:on={selected === s.name} animate:flip={{ duration: 250 }}
        role="button" tabindex="0" onclick={() => onPick(s.name)} onkeydown={(e) => e.key === 'Enter' && onPick(s.name)}>
        <div class="top">
          <Badge {s} size={24} />
          <span class="name">{s.name}</span>
          {#if s.context != null}<span class="mono">{t.ctx(s.context)}</span>{/if}
          {#if r.quota?.h5 != null}<span class="mono" class:stale={r.quota.stale}>{t.quota5h(r.quota.h5)}</span>{/if}
          <button class="chev" aria-label={open ? t.close : t.open} onclick={(e) => { e.stopPropagation(); expanded = open ? null : s.name }}>
            <svg viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
          </button>
        </div>
        {#if s.goal?.text}<div class="goal">{t.goal}: {s.goal.text}</div>{/if}
        {#if x.body}<p class="body" class:full={open} class:strong={group === 'waiting' || open}>{x.body}</p>{/if}
        {#if open}<div class="details">{[s.model?.label, s.effort, s.account].filter(Boolean).join(' · ')}</div>{/if}
        {#if group === 'waiting' && s.question}
          <Options q={s.question} onAnswer={(n) => onAnswer(s.name, n)} tonal="color-mix(in srgb, var(--primary) 12%, var(--surface))" />
        {/if}
        {#if x.steps.length}
          <div class="chips">
            {#each x.steps as step}<button class="chip" onclick={(e) => { e.stopPropagation(); onStep(s.name, step) }}>{step}</button>{/each}
          </div>
        {/if}
        {#if s.context != null}
          <div class="track"><div class="fill" style="width:{Math.min(100, s.context)}%;background:{ctxTone(s.context)}"></div></div>
        {/if}
      </div>
    {/each}
  {/each}

  {#if model.closed.length}
    <div class="outside">
      <div class="oh"><h3>{t.outsideTitle}</h3><i></i></div>
      <div class="osub">{t.outsideSub}</div>
    </div>
    <h2 class="gh cat" style="--t:var(--text2)">
      <svg viewBox="0 0 24 24" width="16" height="16"><path d="M4 5h16v4H4zM5 9h14v10H5zM10 13h4" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" /></svg>
      <span>{t.closedCat(model.closed.length).toUpperCase()}</span>
    </h2>
    <button class="closed">
      <span>{model.closed.slice(0, 3).map(s => s.name).join(', ')}{model.closed.length > 3 ? ` ${t.closedMore(model.closed.length - 3)}` : ''}</span>
      <svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
  {/if}
</section>

<style>
  .home { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; }
  .gh { display: flex; align-items: center; gap: 8px; margin: 6px 8px 0; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--t); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--t) 25%, transparent); }
  .card { background: var(--low); border-radius: 20px; padding: 8px 10px 12px 14px; display: flex; flex-direction: column; gap: 6px; cursor: pointer; transition: background .15s, box-shadow .15s; outline: none; }
  .card.waiting { background: color-mix(in srgb, var(--low) 94%, var(--b-warn)); }
  .card:hover, .card:focus-visible { box-shadow: inset 0 0 0 1px var(--line); }
  .card.on { box-shadow: inset 0 0 0 1.5px var(--b-ring); }
  .top { display: flex; align-items: center; gap: 10px; }
  .name { flex: 1; min-width: 0; font-weight: 600; font-size: 16px; white-space: nowrap; overflow: hidden; }
  .stale { color: var(--wait); }
  .chev { width: 32px; height: 32px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .chev:hover { background: var(--surface); }
  .goal { font-size: 12.5px; color: var(--b-label); }
  .body { font-size: 14px; color: var(--text2); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: clip; }
  .body.full { display: block; }
  .body.strong { color: var(--text); }
  .details { font-size: 12.5px; color: var(--text2); }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .chip { border-radius: 999px; padding: 8px 14px; font-size: 14px; background: color-mix(in srgb, var(--primary) 12%, transparent); }
  .chip:hover { filter: brightness(1.15); }
  .track { margin-right: 4px; height: 3px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .fill { height: 100%; }
  .outside { margin: 18px 4px 0; }
  .oh { display: flex; align-items: center; gap: 12px; }
  .oh h3 { font-size: 16px; font-weight: 600; }
  .oh i { flex: 1; height: 1px; background: var(--line); }
  .osub { font-size: 12.5px; color: var(--stale); }
  .cat { margin-top: 6px; }
  .closed { display: flex; align-items: center; gap: 12px; padding: 14px; border-radius: 20px; background: var(--low); color: var(--text2); text-align: left; }
  .closed span { flex: 1; }
</style>
