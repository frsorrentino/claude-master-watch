<script lang="ts">
  import type { CmdOp, Event, Session, State } from './contract'
  import { forYou, forYouFirst, hero as heroOf, nextKey, working, type Entry, type ForYouRow, type Scheduled } from './masterHome'
  import { parseSteps } from './nextSteps'
  import { age } from './summary'
  import { PATHS } from './badge'
  import { t } from './t'
  import Options from './Options.svelte'

  // La casa della master (casa A, MasterHome.kt): l'ultimo esito in grande con i consigli, «Per te» con chi ti aspetta,
  // chi ha finito e chi lavora, la quota in due barre. Con una domanda aperta «Per te» va in testa.
  let { st, master, entries, events, sent, read, onRead, onConversation, onStep, onSendStep, onAnswer, onSession, onSpeak, onPrompt, onCmd }: {
    st: State; master: Session; entries: Entry[]; events: Event[]; sent: Scheduled[]; read: Set<string>; onRead: (key: string) => void
    onConversation: () => void; onStep: (text: string) => void; onSendStep: (text: string) => void
    onAnswer: (session: string, n: number) => void; onSession: (name: string) => void; onSpeak: (text: string) => void
    onPrompt: (session: string, text: string) => void; onCmd: (op: CmdOp, arg?: string, text?: string) => void
  } = $props()

  const h = $derived(heroOf(entries, master))
  const fy = $derived(forYou(st, events, sent, st.ts, { read }))
  // «+N altre»: tutte le righe in un foglio.
  const every = $derived(forYou(st, events, sent, st.ts, { read, limit: Infinity }))
  let allSheet: HTMLDialogElement | undefined = $state()
  let nightSheet: HTMLDialogElement | undefined = $state()
  let nightDir = $state('')
  let nightText = $state('')
  const busy = $derived(working(st))
  const first = $derived(forYouFirst(fy))
  let expanded = $state<string | null>(null)
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const strip = (x: string) => x.replace(/\*\*|__|`/g, '')
  const dot: Record<string, string> = { waiting: 'var(--wait)', busy: 'var(--busy)', awaiting: 'var(--busy)', idle: 'var(--idle)', gone: 'var(--stale)' }
  const FLAG = ['M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z', 'M4 22v-7']
  const tone = { waiting: 'var(--b-warn)', finished: 'var(--b-good)', working: 'var(--b-ring)' }
  type Variant = keyof typeof tone
  type Item = { id: string; variant: Variant; title: string; detail: string | null; at: number | null; session: string; context?: number | null }
  const isAttention = (r: ForYouRow) => (r.kind === 'question' || r.kind === 'finished') && !!r.session
  const toItem = (r: ForYouRow): Item => ({
    id: `${r.kind}:${r.session}`, variant: r.kind === 'question' ? 'waiting' : 'finished', title: r.title, detail: r.detail ?? null, at: r.at ?? null, session: r.session!,
  })
  const attention = $derived(fy.rows.filter(isAttention).map(toItem))
  const service = $derived(fy.rows.filter(r => !isAttention(r)))
  // Le righe di servizio: titolo, dettaglio e un tasto tonale (ForYouRow dell'app).
  function serviceText(r: ForYouRow): { title: string; detail: string | null; action: string } {
    const first = (x?: string | null) => x?.split('\n').find(l => l.trim()) ?? null
    switch (r.kind) {
      case 'context': return { title: t.fyContext(r.title, r.number ?? 0), detail: t.fyContextDetail, action: t.fyHandoff }
      case 'night_report': return { title: r.title, detail: first(r.detail), action: t.listen }
      case 'night': return { title: (r.number ?? 0) > 0 ? t.fyNightQueued(r.number!) : t.fyNightEmpty, detail: t.fyNightDetail, action: t.fyAdd }
      case 'next_step': return { title: t.fyNext(r.title), detail: r.detail ?? null, action: t.fyStart }
      case 'scheduled': return { title: t.fyScheduled(r.number ?? 1), detail: r.at ? t.fyScheduledDetail(hm(r.at)) : null, action: t.fySee }
      case 'question': return { title: t.fyQuestion(r.title), detail: r.detail ?? null, action: t.fyAnswer }
      case 'finished': return { title: t.fyFinished(r.title), detail: first(r.detail), action: t.fyOpen }
    }
  }
  function act(r: ForYouRow) {
    allSheet?.close()
    switch (r.kind) {
      case 'context': if (r.session) onPrompt(r.session, t.ctxHandoffPrompt); break
      case 'night_report': if (r.detail) onSpeak(r.detail); if (r.key) onRead(r.key); break
      case 'night': nightDir = st.projects[0]?.path ?? ''; nightText = ''; nightSheet?.showModal(); break
      case 'next_step':
        onRead(nextKey(r.title, r.detail ?? ''))
        if (r.session) onPrompt(r.session, r.detail ?? ''); else if (r.project) onCmd('launch', r.project, r.detail ?? undefined)
        break
      case 'finished': if (r.key) onRead(r.key); if (r.session) onSession(r.session); break
      default: if (r.session) onSession(r.session)
    }
  }
  const backdrop = (e: MouseEvent) => { if (e.target === e.currentTarget) (e.currentTarget as HTMLDialogElement).close() }
  const work = $derived(busy.map(({ session: s, detail }): Item => ({
    id: `work:${s.name}`, variant: 'working', title: s.name, detail, at: (s.turn_started ?? s.since) || null, session: s.name, context: s.context,
  })))
  const quota = $derived(Object.entries(st.quota))
  function ageOf(i: Item) { return i.at == null ? null : i.variant === 'finished' ? hm(i.at) : age(i.at, st.ts) }
</script>

{#snippet heroCard()}
  <div class="glass hero" role="button" tabindex="0" onclick={onConversation} onkeydown={(e) => e.key === 'Enter' && onConversation()}>
    <div class="label" style="--c:var(--idle)">
      <span>{(h?.at ? t.homeLast(hm(h.at)) : t.homeLastBare).toUpperCase()}</span><span class="sp"></span>
      <i class="led" style="background:{dot[master.state]}"></i>
    </div>
    {#if h}
      <h2>{strip(h.headline)}</h2>
      {#if h.body}<p class="hbody">{strip(h.body)}</p>{/if}
    {/if}
    <div class="acts">
      {#if h}
        <button class="round" aria-label={t.listen} onclick={(e) => { e.stopPropagation(); onSpeak(`${h.headline}. ${h.body}`) }}>
          <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>
        </button>
      {/if}
      <button class="pill" onclick={(e) => { e.stopPropagation(); onConversation() }}>
        {t.openConversation}
        <svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
    </div>
    {#if h?.steps.length}
      <hr />
      <div class="mono">{t.next}</div>
      {#each h.steps as step}
        <!-- Come sotto l'ultima risposta: clic = nel campo, doppio clic = invio subito. -->
        <button class="step" title={t.stepHint} onclick={(e) => { e.stopPropagation(); onStep(step) }} ondblclick={(e) => { e.stopPropagation(); onSendStep(step) }}>↳ {step}</button>
      {/each}
    {/if}
  </div>
{/snippet}

{#snippet row(i: Item)}
  {@const open = expanded === i.id}
  {@const p = parseSteps(i.detail ?? '')}
  {@const body = strip(p.text).trim()}
  {@const q = i.variant === 'waiting' ? st.sessions.find(s => s.name === i.session)?.question : null}
  {@const a = ageOf(i)}
  <div class="att" class:open style="--t:{tone[i.variant]}">
    <button class="atop" onclick={() => (expanded = open ? null : i.id)}>
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="var(--t)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        {#each i.variant === 'waiting' ? PATHS.hand : i.variant === 'working' ? PATHS.zap : FLAG as d}<path {d} />{/each}
      </svg>
      <span class="acol">
        <span class="aname"><b>{i.title}</b>{#if a}<span style="color:var(--t)">{` · ${a}`}</span>{/if}</span>
        {#if !open && body}<span class="aline">{body.split('\n').find(l => l.trim())}</span>{/if}
      </span>
      {#if i.context != null}<span class="mono" style="color:{i.context >= 75 ? 'var(--wait)' : 'var(--b-ring)'}">{i.context}%</span>{/if}
      <svg viewBox="0 0 24 24" width="22" height="22" aria-label={open ? t.close : t.open}><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    {#if open}
      <div class="abody">
        {#if body}<p>{body}</p>{/if}
        {#if q}<Options {q} onAnswer={(n) => onAnswer(i.session, n)} />{/if}
        {#if !q && p.steps.length}
          <div class="chips">{#each p.steps as step}<button class="chip" onclick={() => onSendStep(step)}>{step}</button>{/each}</div>
        {/if}
        <button class="link" onclick={() => onSession(i.session)}>{t.openConversation}</button>
      </div>
    {/if}
  </div>
{/snippet}

{#snippet svc(r: ForYouRow)}
  {@const x = serviceText(r)}
  <div class="svc">
    <span><span class="stitle">{x.title}</span>{#if x.detail}<span class="sdet">{x.detail}</span>{/if}</span>
    <button class="tonal" onclick={() => act(r)}>{x.action}</button>
  </div>
{/snippet}

{#snippet forYouCard()}
  {#if attention.length || work.length || service.length}
    <div class="glass foryou">
      <div class="label rule" style="--c:var(--wait)"><span>{t.forYou.toUpperCase()}</span><i></i></div>
      {#each attention as i (i.id)}{@render row(i)}{/each}
      {#if work.length}
        <div class="gh"><span>{t.summary.working(work.length).toUpperCase()}</span><i></i></div>
        {#each work as i (i.id)}{@render row(i)}{/each}
      {/if}
      {#each service as r}{@render svc(r)}{/each}
      {#if fy.more > 0}<button class="link" onclick={() => allSheet?.showModal()}>{t.fyMore(fy.more)}</button>{/if}
    </div>
  {/if}
{/snippet}

<div class="mhome">
  {#if first}{@render forYouCard()}{@render heroCard()}{:else}{@render heroCard()}{@render forYouCard()}{/if}
  {#if quota.length}
    <div class="quota">
      {#each quota as [name, r]}
        <button class="qcell">
          <span class="qtext" class:stale={r.stale}>{r.stale ? t.quotaOld(name) : t.quota(name, r.h5 ?? 0, r.w7 ?? 0)}</span>
          <span class="qtrack"><span style="width:{Math.min(100, Math.max(0, r.h5 ?? 0))}%;background:{r.stale ? 'var(--text2)' : 'var(--b-ring)'}"></span></span>
        </button>
      {/each}
    </div>
  {/if}
</div>

<dialog bind:this={allSheet} onclick={backdrop}>
  <div class="label rule" style="--c:var(--wait)"><span>{t.forYou.toUpperCase()}</span><i></i></div>
  <div class="sheetrows">{#each every.rows as r}{@render svc(r)}{/each}</div>
</dialog>

<dialog bind:this={nightSheet} onclick={backdrop}>
  <h3>{t.nightTitle}</h3>
  <form class="night" onsubmit={(e) => { e.preventDefault(); if (nightDir && nightText.trim()) { onCmd('night_add', nightDir, nightText.trim()); nightSheet?.close() } }}>
    <label>{t.nightProject}
      <select bind:value={nightDir}>{#each st.projects as p}<option value={p.path}>{p.name}</option>{/each}</select>
    </label>
    <textarea rows="4" bind:value={nightText} placeholder={t.nightPrompt}></textarea>
    <div class="btns">
      <button type="button" class="link" onclick={() => nightSheet?.close()}>{t.cancel}</button>
      <button type="submit" class="filled" disabled={!nightDir || !nightText.trim()}>{t.nightAdd}</button>
    </div>
  </form>
</dialog>

<style>
  .mhome { display: flex; flex-direction: column; gap: 20px; }
  /* GlassCard dell'app: superficie, riflesso in alto, bordo sottile (nel colore della card se ha una tinta). */
  .glass { --tint: transparent; border-radius: 16px; padding: 12px 14px; display: flex; flex-direction: column; gap: 10px; border: 1px solid rgb(255 255 255 / .14);
    background: linear-gradient(rgb(255 255 255 / .06), transparent 45%), var(--surface); }
  .foryou { border-color: color-mix(in srgb, var(--wait) 75%, transparent); background: linear-gradient(rgb(255 255 255 / .06), transparent 45%), color-mix(in srgb, var(--wait) 6%, var(--bg)); gap: 4px; }
  .hero { cursor: pointer; outline: none; }
  .hero:focus-visible { border-color: var(--icon); }
  .label { display: flex; align-items: center; gap: 10px; font-size: 11px; letter-spacing: 2px; font-weight: 700; color: var(--c); }
  .label i:not(.led) { flex: 1; height: 1px; background: color-mix(in srgb, var(--c) 40%, transparent); }
  .foryou .label { padding-bottom: 6px; }
  .sp { flex: 1; }
  .led { width: 8px; height: 8px; border-radius: 50%; }
  h2 { font-size: 17px; font-weight: 600; line-height: 1.4; }
  .hbody { color: var(--text2); font-size: 14.5px; white-space: pre-wrap; display: -webkit-box; -webkit-line-clamp: 4; line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
  .acts { display: flex; gap: 10px; align-items: center; padding-top: 4px; }
  .round, .pill, .tonal { background: var(--high); color: var(--text); }
  .round { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); }
  .pill { flex: 1; height: 44px; border-radius: 22px; display: flex; align-items: center; justify-content: center; gap: 4px; font-weight: 500; font-size: 14.5px; }
  .round:hover, .pill:hover, .tonal:hover { filter: brightness(1.15); }
  hr { border: 0; height: 1px; background: var(--line); margin: 2px 0; }
  .step { text-align: left; color: var(--icon); font-size: 16px; padding: 4px 0; border-radius: 8px; }
  .step:hover { background: rgb(255 255 255 / .04); }
  .gh { display: flex; align-items: center; gap: 10px; margin: 6px 8px 0; font: 12px var(--mono); letter-spacing: .06em; color: var(--b-ring); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--b-ring) 25%, transparent); }
  .att { border-radius: 20px; }
  .att.open { background: color-mix(in srgb, var(--t) 8%, transparent); }
  .atop { width: 100%; display: flex; align-items: center; gap: 12px; padding: 10px 8px; text-align: left; border-radius: 20px; }
  .atop:hover { background: rgb(255 255 255 / .03); }
  .atop > svg:first-child { flex: none; }
  .acol { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .aname, .aline { white-space: nowrap; overflow: hidden; }
  .aname { font-size: 16px; }
  .aname b { font-weight: 600; }
  .aline { font-size: 14px; color: var(--text2); }
  .abody { padding: 0 8px 12px 40px; display: flex; flex-direction: column; gap: 8px; }
  .abody p { font-size: 14px; white-space: pre-wrap; overflow-wrap: anywhere; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .chip { border: 1px solid color-mix(in srgb, var(--t) 50%, transparent); border-radius: 999px; padding: 8px 14px; font-size: 14px; }
  .chip:hover { filter: brightness(1.15); }
  .link { align-self: flex-start; color: var(--icon); padding: 8px 12px; border-radius: 20px; font-weight: 500; font-size: 14px; }
  .link:hover { background: rgb(255 255 255 / .05); }
  .svc { display: flex; align-items: center; gap: 12px; padding: 6px 8px; }
  .svc > span { flex: 1; display: flex; flex-direction: column; gap: 2px; }
  .stitle { font-size: 16px; }
  .sdet { font-size: 12.5px; color: var(--text2); }
  .tonal { border-radius: 20px; padding: 10px 16px; font-weight: 500; font-size: 14px; }
  .svc .stitle { color: var(--text); }
  .sdet { display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
  .tonal { flex: none; }
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 20px 24px; width: min(560px, 100vw); max-height: 85vh; border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  @media (max-width: 599px) { dialog { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  dialog h3 { font-size: 22px; font-weight: 600; margin-bottom: 12px; }
  .sheetrows { display: flex; flex-direction: column; gap: 14px; margin-top: 14px; }
  .night { display: flex; flex-direction: column; gap: 12px; }
  .night label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text2); }
  .night select, .night textarea { font: inherit; color: var(--text); background: var(--high); border: 1px solid var(--line); border-radius: 12px; padding: 10px 12px; }
  .night textarea { resize: vertical; }
  .night :focus-visible { outline: 2px solid var(--icon); outline-offset: 1px; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; }
  .filled { background: var(--primary); color: var(--on-primary); border-radius: 20px; padding: 10px 18px; font-weight: 500; }
  .filled:disabled { background: var(--high); color: var(--text2); cursor: default; }
  .quota { display: flex; gap: 10px; }
  .qcell { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6px; padding: 8px 10px; border-radius: 12px; background: var(--low); text-align: left; }
  .qcell:hover { filter: brightness(1.15); }
  .qtext { font-size: 12.5px; font-weight: 500; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .qtext.stale { color: var(--wait); }
  .qtrack { height: 4px; border-radius: 2px; background: var(--b-track); overflow: hidden; display: block; }
  .qtrack span { display: block; height: 100%; }
</style>
