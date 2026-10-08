<script lang="ts">
  import type { Card, NightPage } from './night'
  import { t } from './t'
  // La pagina «Notte» (specifica docs/proposte/2026-10-07-pagina-notte.md, approvata da Franz il 07/10 alle 21:50): la
  // finestra della notte, quello che serve a te, una card per lavoro o sessione che si apre al tocco, i progetti.
  // Un solo tasto pieno: il primo di «Serve a te».
  let { model, error = null, loading = false, onRefresh, onChat, onAnswer, onApprove, onSend }: {
    model: NightPage | null; error?: string | null; loading?: boolean; onRefresh: () => void
    onChat: (session: string) => void; onAnswer: (session: string, n: number) => void
    onApprove: (task: string) => void; onSend: (session: string, text: string) => void
  } = $props()
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone
  const hm = (s: number) => new Intl.DateTimeFormat('it-IT', { timeZone: zone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
  let opened = $state<string | null>(null)
  const times = (c: Card) => (c.end == null ? t.nightSince(hm(c.start)) : `${hm(c.start)}–${hm(c.end)} · ${t.nightShort(c.durationS ?? 0)}`)
  const tone: Record<string, string> = { ok: 'var(--good)', stopped: 'var(--gone-dim)', running: 'var(--icon)', question: 'var(--advice)' }
  // Sezioni richiudibili (Franz, 08/10 12:30): «Progetti» parte chiusa.
  let needsOpen = $state(true)
  let itemsOpen = $state(true)
  let projectsOpen = $state(false)
</script>

<div class="night">
  {#if model}
    <div class="win">
      <div class="wt">
        <p>{t.nightWindow(hm(model.start), hm(model.end), model.windowS)}{#if model.fromLastMessage}<br />{t.nightFromLast}{/if}</p>
        <div class="chips">
          {#if model.counts.done}<span style:color="var(--good)">{t.nightDone(model.counts.done)}</span>{/if}
          {#if model.counts.running}<span style:color="var(--icon)">{t.nightRunningN(model.counts.running)}</span>{/if}
          {#if model.counts.stopped}<span style:color="var(--gone-dim)">{t.nightStopped(model.counts.stopped)}</span>{/if}
          {#if model.counts.asking}<span style:color="var(--advice)">{t.nightAsking(model.counts.asking)}</span>{/if}
        </div>
      </div>
      <button class="ib" aria-label={t.nightRefresh} title={t.nightRefresh} disabled={loading} onclick={onRefresh}>
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.64-6.36L21 8M21 3v5h-5" /></svg>
      </button>
    </div>

    {#snippet section(title: string, explain: string, open: boolean, toggle: () => void)}
      <button class="sec" aria-expanded={open} onclick={toggle}>
        <span class="h2"><svg viewBox="0 0 24 24" width="16" height="16"><path d={open ? 'M6 9l6 6 6-6' : 'M9 6l6 6-6 6'} fill="none" stroke="var(--icon)" stroke-width="2.4" stroke-linecap="round" /></svg>{title}</span>
        <small>{explain}</small>
      </button>
    {/snippet}

    {#if model.needs.length}
      {@render section(t.nightNeedsTitle(model.needs.length), t.nightNeedsExpl, needsOpen, () => (needsOpen = !needsOpen))}
      {#if needsOpen}
      {#each model.needs as n, i}
        <div class="need">
          <span class="ni" style:color={n.kind === 'question' ? 'var(--advice)' : 'var(--wait)'}>{n.kind === 'question' ? '?' : '!'}</span>
          <span class="nt"><b>{n.session}</b><small>{n.text}</small></span>
          <span class="na">
            {#if n.kind === 'question'}
              {#each n.options as o, k}<button class="nb" class:filled={i === 0 && k === 0} onclick={() => onAnswer(n.session, o.n)}>{o.n} · {o.label}</button>{/each}
            {:else if n.kind === 'approval'}
              <button class="nb" class:filled={i === 0} onclick={() => n.task && onApprove(n.task)}>{t.nightApprove}</button>
            {:else}
              <button class="nb" class:filled={i === 0} onclick={() => onSend(n.session, n.text)}>{t.nightSend(n.text)}</button>
            {/if}
          </span>
        </div>
      {/each}
      {/if}
    {/if}

    {@render section(t.nightItemsTitle(model.cards.length), t.nightItemsExpl, itemsOpen, () => (itemsOpen = !itemsOpen))}
    {#if itemsOpen}
    {#each model.cards as c (c.id)}
      {@const open = opened === c.id}
      <button class="card" aria-expanded={open} onclick={() => (opened = open ? null : c.id)}>
        <span class="hd"><b class="tt">{c.title}</b><i class="st" style:color={tone[c.icon]}>{t.nightState[c.icon]}</i></span>
        <small>{c.queue ? t.nightKindJob : t.nightKindSession} · {times(c)}</small>
        {#if c.folder}<small>{c.folder}</small>{/if}
        {#if c.detail}<span class="out">{c.detail}</span>{/if}
        {#if c.commits != null}<small>{t.nightCount(c.commits, 'commit')} · {t.nightCount(c.tests ?? 0, 'test')} · {t.nightCount(c.prompts ?? 0, 'prompt')}</small>{/if}
        {#if open}
          {#if c.steps.length}
            <ol class="steps">{#each c.steps as s}<li><span class="mono">{hm(s.at)}</span> {s.text}</li>{/each}</ol>
          {/if}
          {#if c.chat}
            <span class="tonal" role="button" tabindex="0" onclick={(e) => { e.stopPropagation(); if (c.chat) onChat(c.chat) }} onkeydown={(e) => { if (e.key === 'Enter' && c.chat) onChat(c.chat) }}>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
              {t.nightConversation}
            </span>
          {/if}
        {/if}
      </button>
    {/each}
    {/if}

    {#if model.projects.length}
      {@render section(t.nightProjectsTitle(model.projects.length), t.nightProjectsExpl, projectsOpen, () => (projectsOpen = !projectsOpen))}
      {#if projectsOpen}
      {#each model.projects as p}
        <div class="proj">
          <div class="ph"><b>{p.name}</b><small>{p.total ? t.nightParts(p.done, p.total) : t.nightNoParts}</small></div>
          {#if p.parts.length}<div class="segs">{#each p.parts as st}<i class={st}></i>{/each}</div>{/if}
          {#if p.waiting.length}<small>{t.nightWaiting}: {p.waiting.join(' · ')}</small>{/if}
          {#if p.next}<small>{t.nightNext}: {p.next}</small>{/if}
        </div>
      {/each}
      {/if}
    {/if}
  {:else if error}
    <p class="msg">{error}</p>
  {:else}
    <p class="msg">{t.nightLoading}</p>
  {/if}
</div>

<style>
  .night { padding: 0 12px 24px; display: flex; flex-direction: column; gap: 10px; max-width: 760px; }
  .win { display: flex; align-items: flex-start; gap: 8px; padding: 0 4px 0 8px; color: var(--text2); font-size: 15px; }
  .wt { flex: 1; display: flex; flex-direction: column; gap: 12px; }
  .chips { display: flex; gap: 8px; flex-wrap: wrap; }
  .chips span { padding: 6px 12px; border-radius: 999px; background: var(--high); font-weight: 500; font-size: 14px; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; flex: none; margin-top: -10px; }
  .ib:hover { background: var(--surface); }
  .sec { display: flex; flex-direction: column; gap: 4px; margin: 14px 0 2px; padding: 0 8px; text-align: left; }
  .sec .h2 { display: flex; align-items: center; gap: 6px; font: 500 12px/1 var(--mono); letter-spacing: .14em; text-transform: uppercase; color: var(--text2); }
  .sec small { color: var(--text2); font-size: 14px; }
  .need { display: flex; align-items: center; gap: 14px; padding: 14px; border-radius: 24px; background: var(--surface); }
  .ni { width: 40px; height: 40px; flex: none; border-radius: 50%; display: grid; place-items: center; background: var(--high); font-weight: 700; }
  .nt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .nt b { font-size: 16px; font-weight: 500; }
  .nt small { color: var(--text2); font-size: 14px; }
  .na { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
  .nb { padding: 10px 16px; border-radius: 22px; background: var(--high); color: var(--icon); font-weight: 500; font-size: 14.5px; white-space: nowrap; }
  .nb.filled { background: var(--primary); color: var(--on-primary); }
  .nb:hover { filter: brightness(1.12); }
  .card { display: flex; flex-direction: column; gap: 6px; width: 100%; padding: 14px 18px; border-radius: 24px; background: var(--surface); text-align: left; }
  .card:hover { filter: brightness(1.06); }
  .hd { display: flex; align-items: center; gap: 8px; }
  .tt { flex: 1; font-size: 17px; font-weight: 500; overflow-wrap: anywhere; }
  .st { font-style: normal; font-weight: 600; font-size: 13px; padding: 4px 10px; border-radius: 999px; background: color-mix(in srgb, currentColor 16%, transparent); white-space: nowrap; }
  .card small { color: var(--text2); font-size: 14px; }
  .out { font-size: 15.5px; line-height: 1.45; }
  .steps { list-style: none; margin: 0; padding: 0 0 0 12px; border-left: 2px solid var(--line); display: flex; flex-direction: column; gap: 6px; font-size: 15px; }
  .steps .mono { color: var(--text2); }
  .tonal { align-self: flex-start; display: flex; align-items: center; gap: 10px; padding: 10px 18px; border-radius: 22px; background: var(--high); color: var(--icon); font-weight: 500; }
  .tonal:hover { filter: brightness(1.12); }
  .proj { display: flex; flex-direction: column; gap: 6px; padding: 14px 18px; border-radius: 24px; background: var(--surface); }
  .ph { display: flex; justify-content: space-between; gap: 10px; align-items: baseline; }
  .ph b { font-size: 17px; font-weight: 500; }
  .proj small { color: var(--text2); font-size: 14px; }
  .segs { display: flex; gap: 4px; }
  .segs i { flex: 1; height: 8px; border-radius: 3px; background: var(--high); }
  .segs i.done { background: var(--good); }
  .segs i.doing { background: var(--icon); }
  .msg { padding: 24px 16px; color: var(--text2); }
</style>
