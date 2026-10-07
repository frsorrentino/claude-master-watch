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
  const iconColor = { ok: 'var(--good)', stopped: 'var(--gone-dim)', running: 'var(--icon)', question: 'var(--advice)' }
  const barColor = { ok: 'var(--good)', stopped: 'var(--gone-dim)', running: 'var(--icon)', question: 'var(--advice)' }
</script>

<div class="night">
  {#if model}
    <div class="win">
      <p>{t.nightWindow(hm(model.start), hm(model.end), model.windowS, model.fromLastMessage)}</p>
      <button class="ib" aria-label={t.nightRefresh} title={t.nightRefresh} disabled={loading} onclick={onRefresh}>
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-2.64-6.36L21 8M21 3v5h-5" /></svg>
      </button>
    </div>

    {#if model.needs.length}
      <h2>{t.nightNeeds}</h2>
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

    <h2>{t.nightItems}</h2>
    <div class="axis" aria-hidden="true">{#each model.axis as a}<span style:left="{a.at * 100}%">{a.label}</span>{/each}</div>
    {#each model.cards as c (c.id)}
      {@const open = opened === c.id}
      <div class="card" class:open>
        <button class="head" aria-expanded={open} onclick={() => (opened = open ? null : c.id)}>
          <span class="ic" style:color={iconColor[c.icon]}>
            {#if c.icon === 'ok'}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M20 6 9 17l-5-5" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" /></svg>
            {:else if c.icon === 'stopped'}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg>
            {:else if c.icon === 'running'}<svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" /></svg>
            {:else}<b>?</b>{/if}
          </span>
          <span class="tx">
            <span class="tt">{c.title}{#if c.queue}<i class="tag">{t.nightQueueTag}</i>{/if}</span>
            <small>{times(c)}</small>
            {#if open || !c.detail}{#if c.folder}<small>{c.folder}</small>{/if}{/if}
            {#if !open && c.detail}<span class="line">{c.detail}</span>{/if}
            <span class="track"><i style:left="{c.from * 100}%" style:width="{(c.to - c.from) * 100}%" style:background={barColor[c.icon]}></i></span>
          </span>
          <svg class="chev" viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg>
        </button>
        {#if open}
          <div class="more">
            {#if c.detail}<p>{c.detail}</p>{/if}
            {#if c.commits != null}
              <div class="counts"><span>{t.nightCount(c.commits, 'commit')}</span><span>{t.nightCount(c.tests ?? 0, 'test')}</span><span>{t.nightCount(c.prompts ?? 0, 'prompt')}</span></div>
            {/if}
            {#if c.steps.length}
              <ol class="steps">{#each c.steps as s}<li><span class="mono">{hm(s.at)}</span> {s.text}</li>{/each}</ol>
            {/if}
            {#if c.chat}
              <button class="tonal" onclick={() => c.chat && onChat(c.chat)}>
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" /></svg>
                {t.nightConversation}
              </button>
            {/if}
          </div>
        {/if}
      </div>
    {/each}

    {#if model.projects.length}
      <h2>{t.nightProjects}</h2>
      {#each model.projects as p}
        <div class="proj">
          <div class="ph"><b>{p.name}</b><small>{p.total ? t.nightParts(p.done, p.total) : t.nightNoParts}</small></div>
          {#if p.parts.length}<div class="segs">{#each p.parts as st}<i class={st}></i>{/each}</div>{/if}
          {#if p.waiting.length}<small>{t.nightWaiting}: {p.waiting.join(' · ')}</small>{/if}
          {#if p.next}<small>{t.nightNext}: {p.next}</small>{/if}
        </div>
      {/each}
    {/if}
  {:else if error}
    <p class="msg">{error}</p>
  {:else}
    <p class="msg">{t.nightLoading}</p>
  {/if}
</div>

<style>
  .night { padding: 0 12px 24px; display: flex; flex-direction: column; gap: 10px; max-width: 760px; }
  .win { display: flex; align-items: flex-start; gap: 8px; padding: 0 4px 0 44px; color: var(--text2); font-size: 15px; }
  .win p { flex: 1; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; flex: none; margin-top: -10px; }
  .ib:hover { background: var(--surface); }
  h2 { margin: 14px 8px 2px; font: 500 12px/1 var(--mono); letter-spacing: .14em; text-transform: uppercase; color: var(--text2); }
  .need { display: flex; align-items: center; gap: 14px; padding: 14px; border-radius: 24px; background: var(--surface); }
  .ni { width: 40px; height: 40px; flex: none; border-radius: 50%; display: grid; place-items: center; background: var(--high); font-weight: 700; }
  .nt { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .nt b { font-size: 16px; font-weight: 500; }
  .nt small { color: var(--text2); font-size: 14px; }
  .na { display: flex; gap: 6px; flex-wrap: wrap; justify-content: flex-end; }
  .nb { padding: 10px 16px; border-radius: 22px; background: var(--high); color: var(--icon); font-weight: 500; font-size: 14.5px; white-space: nowrap; }
  .nb.filled { background: var(--primary); color: var(--on-primary); }
  .nb:hover { filter: brightness(1.12); }
  .axis { position: relative; height: 18px; margin: 0 52px 0 64px; font: 12px/1 var(--mono); color: var(--text2); }
  .axis span { position: absolute; transform: translateX(-50%); white-space: nowrap; }
  .card { border-radius: 24px; background: var(--surface); }
  .head { display: flex; align-items: flex-start; gap: 14px; width: 100%; padding: 14px 10px 14px 14px; text-align: left; border-radius: 24px; }
  .head:hover { background: rgb(255 255 255 / .03); }
  .ic { width: 40px; height: 40px; flex: none; border-radius: 50%; display: grid; place-items: center; background: var(--high); }
  .tx { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
  .tt { font-size: 17px; font-weight: 500; overflow-wrap: anywhere; }
  .tag { font: 600 11px/1 var(--mono); font-style: normal; letter-spacing: .08em; color: var(--advice); background: color-mix(in srgb, var(--advice) 16%, transparent); border-radius: 999px; padding: 4px 8px; margin-left: 8px; vertical-align: 3px; }
  .tx small { color: var(--text2); font-size: 14px; }
  .line { color: var(--text2); font-size: 14.5px; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .track { position: relative; height: 5px; margin-top: 8px; border-radius: 3px; background: var(--high); }
  .track i { position: absolute; top: 0; bottom: 0; border-radius: 3px; min-width: 5px; }
  .chev { flex: none; margin-top: 6px; }
  .more { padding: 0 18px 16px 68px; display: flex; flex-direction: column; gap: 12px; }
  .more p { font-size: 16px; line-height: 1.5; }
  .counts { display: flex; gap: 8px; flex-wrap: wrap; }
  .counts span { padding: 6px 12px; border-radius: 10px; background: var(--high); color: var(--text2); font-size: 14px; }
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
