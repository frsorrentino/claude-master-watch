<script lang="ts">
  import { flip } from 'svelte/animate'
  import type { Approval, State } from './contract'
  import { cleanupOf } from './masterService'
  import { since } from './durations'
  import { build, type Group, type Row } from './summary'
  import { groups as deskGroups } from './tablet'
  import { summary as outcomeSummary } from './outcome'
  import { parseSteps } from './nextSteps'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import Options from './Options.svelte'

  // La home del telefono (SummaryList.kt): ogni sessione una card nei gruppi del bisogno; il tocco apre la sessione, ▼ la
  // apre sul posto. La domanda ha le opzioni subito, chi ha finito i consigli come tasti, sotto la barretta del contesto.
  import type { Snippet } from 'svelte'
  let { st, selected, onPick, onAnswer, onStep, withMaster = false, footer, onApprove = () => {}, onClose = () => {} }: {
    /** `selected`: le sessioni aperte, evidenziate; `withMaster`: la master nella lista come le altre (la plancia). */
    st: State; selected: string[]; onPick: (name: string) => void; withMaster?: boolean
    /** In fondo alla lista: sulla plancia i pannelli della quota (footer di SummaryList). */
    footer?: Snippet
    onAnswer: (session: string, n: number) => void; onStep: (session: string, text: string) => void
    /** Contratto 1.37: l'ok a un compito (`approve`) e «Chiudi» per una sessione finita o doppione (/exit). */
    onApprove?: (task: string, note: string) => void; onClose?: (name: string) => void
  } = $props()
  const model = $derived(build(st, [], st.ts, new Set()))
  let expanded = $state<string | null>(null)
  const tone: Record<Group, string> = { waiting: 'var(--b-warn)', finished: 'var(--b-good)', working: 'var(--b-ring)', still: 'var(--text2)' }
  const groups = $derived(withMaster ? deskGroups(model) : (['waiting', 'finished', 'working', 'still'] as Group[]).map(g => [g, model.rows.filter(r => r.group === g)] as [Group, Row[]]).filter(([, rows]) => rows.length > 0))
  // Contratto 1.37: «Da approvare» prima di «Ti aspetta»; con un'approvazione le opzioni delle domande restano tonali,
  // così sulla schermata c'è un solo bottone pieno.
  const approvals = $derived(st.approvals ?? [])
  const canExit = $derived(!!st.slash?.includes('exit'))
  let pending = $state<Approval | null>(null)
  let note = $state('')
  let approveDlg: HTMLDialogElement | undefined = $state()
  let closing = $state<string | null>(null)
  let closeDlg: HTMLDialogElement | undefined = $state()
  function askApprove(a: Approval) { pending = a; note = ''; approveDlg?.showModal() }
  function askClose(name: string) { closing = name; closeDlg?.showModal() }
  const ctxTone = (p: number) => (p >= 90 ? 'var(--b-alert)' : p >= 75 ? 'var(--b-warn)' : 'var(--b-ring)')
  function text(r: Row) {
    const parsed = parseSteps(r.text ?? '')
    const shown = r.group === 'waiting' ? parsed.text : r.session.outcome ? outcomeSummary(r.session.outcome) : parsed.text
    return { body: shown.replace(/\*\*|__|`/g, '').trim(), steps: r.group === 'waiting' ? [] : parsed.steps }
  }
</script>

<section class="home">
  {#if approvals.length}
    <h2 class="gh" style="--t:var(--advice)"><span>{t.approvalsGroup(approvals.length).toUpperCase()}</span><i></i></h2>
    {#each approvals as a (a.task)}
      <div class="card appr">
        <div class="top">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="var(--advice)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z" /><path d="m9 12 2 2 4-4" /></svg>
          <span class="name">{a.title}</span>
          {#if a.deploy}<span class="prod">{t.approvalProd.toUpperCase()}</span>{/if}
        </div>
        <div class="kv">
          <span>{t.approvalWhat}</span><span>{a.what}</span>
          <span>{t.approvalWhere}</span><span>{a.where}</span>
          <span>{t.approvalAsked}</span><span>{since(a.requested_at, st.ts)}</span>
        </div>
        <button class="approve" onclick={() => askApprove(a)}>{t.approve}</button>
      </div>
    {/each}
  {/if}
  {#each groups as [group, rows] (group)}
    <h2 class="gh" style="--t:{tone[group]}"><span>{t.summary[group](rows.length).toUpperCase()}</span><i></i></h2>
    {#each rows as r (r.session.name)}
      {@const s = r.session}
      {@const x = text(r)}
      {@const open = expanded === s.name}
      {@const c = cleanupOf(s, canExit)}
      <div class="card" class:waiting={group === 'waiting'} class:on={selected.includes(s.name)} animate:flip={{ duration: 250 }}
        role="button" tabindex="0" onclick={() => onPick(s.name)} onkeydown={(e) => e.key === 'Enter' && onPick(s.name)}>
        <div class="top">
          <Badge {s} size={24} />
          <span class="name">{s.name}</span>
          {#if s.duplicate_of}<span class="dup">{t.cleanupDupTag.toUpperCase()}</span>{/if}
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
          <Options q={s.question} onAnswer={(n) => onAnswer(s.name, n)} firstFilled={!approvals.length} tonal="color-mix(in srgb, var(--primary) 12%, var(--surface))" />
        {/if}
        {#if x.steps.length}
          <div class="chips">
            {#each x.steps as step}<button class="chip" onclick={(e) => { e.stopPropagation(); onStep(s.name, step) }}>{step}</button>{/each}
          </div>
        {/if}
        {#if c}
          <div class="clean">
            <span>{c.kind === 'duplicate' ? t.cleanupDuplicate(c.of ?? '') : s.attached ? t.cleanupAttached : t.cleanupFinished}</span>
            {#if c.canClose}
              <button class="close" onclick={(e) => { e.stopPropagation(); askClose(s.name) }}>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--b-alert)" stroke-width="2" stroke-linecap="round"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>{t.cleanupClose}
              </button>
            {/if}
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
  {#if footer}<div class="footer">{@render footer()}</div>{/if}
</section>

<dialog bind:this={approveDlg} class="alert" onclick={(e) => e.target === e.currentTarget && approveDlg?.close()}>
  {#if pending}
    <h3>{pending.deploy ? t.approveProdTitle : t.approveTitle(pending.title)}</h3>
    <p class="sub">{t.approveText(pending.title, pending.what, pending.where)}</p>
    <label class="lab">{t.approveNote}<input bind:value={note} placeholder="ok" /></label>
    <div class="btns">
      <button class="text2" onclick={() => approveDlg?.close()}>{t.cancel}</button>
      <button class="filled" onclick={() => { onApprove(pending!.task, note); approveDlg?.close() }}>{t.approve}</button>
    </div>
  {/if}
</dialog>

<dialog bind:this={closeDlg} class="alert" onclick={(e) => e.target === e.currentTarget && closeDlg?.close()}>
  {#if closing}
    <h3>{t.exitTitle(closing)}</h3>
    <p class="sub">{t.exitText}</p>
    <div class="btns">
      <button class="text2" onclick={() => closeDlg?.close()}>{t.cancel}</button>
      <button class="link" onclick={() => { onClose(closing!); closeDlg?.close() }}>{t.exitOk}</button>
    </div>
  {/if}
</dialog>

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
  .appr { cursor: default; padding: 12px 14px 14px; gap: 8px; }
  .prod, .dup { font: 600 11px/1.4 var(--mono); letter-spacing: .08em; border-radius: 999px; padding: 3px 10px; flex: none; }
  .prod { color: var(--prod); background: color-mix(in srgb, var(--prod) 16%, transparent); }
  .dup { color: var(--b-warn); background: color-mix(in srgb, var(--b-warn) 14%, transparent); }
  .kv { display: grid; grid-template-columns: auto 1fr; gap: 3px 10px; font-size: 14px; }
  .kv span:nth-child(odd) { color: var(--text2); }
  .approve { align-self: stretch; margin-top: 4px; background: var(--primary); color: var(--on-primary); border-radius: 999px; padding: 11px 20px; font-weight: 500; }
  .clean { display: flex; align-items: center; gap: 10px; font-size: 13.5px; color: var(--text2); }
  .clean span { flex: 1; min-width: 0; }
  .close { display: flex; align-items: center; gap: 8px; background: var(--high); border-radius: 999px; padding: 8px 14px; color: var(--text); font-weight: 500; flex: none; }
  dialog.alert { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 22px; width: min(400px, 92vw); border-radius: 28px; }
  dialog.alert::backdrop { background: rgb(0 0 0 / .55); }
  dialog h3 { font-size: 22px; font-weight: 600; }
  dialog .sub { color: var(--text2); font-size: 14px; margin-top: 6px; }
  dialog .lab { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: var(--text2); margin-top: 14px; }
  dialog input { background: var(--high); color: var(--text); border: 0; border-radius: 14px; padding: 11px 14px; font: inherit; font-size: 15px; }
  dialog .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  dialog .btns button { padding: 10px 16px; border-radius: 20px; font-weight: 500; }
  dialog .text2 { color: var(--text2); }
  dialog .link { color: var(--icon); }
  dialog .filled { background: var(--primary); color: var(--on-primary); }
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
  .footer { display: flex; flex-direction: column; gap: 12px; padding: 12px 0; }
</style>
