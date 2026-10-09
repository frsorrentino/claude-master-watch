<script lang="ts">
  import { flip } from 'svelte/animate'
  import type { Approval, Session, State } from './contract'
  import { cleanupOf } from './masterService'
  import { since } from './durations'
  import { build, type Group, type Row } from './summary'
  import { groups as deskGroups } from './tablet'
  import { summary as outcomeSummary } from './outcome'
  import { blockingOf, parseSteps } from './nextSteps'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import Options from './Options.svelte'
  import { recapActions, type RecapAction } from './recapActions'
  import { agendaModel, todayIso, type Edit } from './recapAgenda'
  import AgendaActions from './AgendaActions.svelte'
  import AgendaItem from './AgendaItem.svelte'
  import RecapSend from './RecapSend.svelte'

  // La home del telefono (SummaryList.kt): ogni sessione una card nei gruppi del bisogno; il tocco apre la sessione, ▼ la
  // apre sul posto. La domanda ha le opzioni subito, chi ha finito i consigli come tasti, sotto la barretta del contesto.
  import type { Snippet } from 'svelte'
  import type { NightPage } from './night'
  let { st, selected, onPick, onAnswer, onStep, withMaster = false, footer, onApprove = () => {}, onClose = () => {}, night = null, nightTitle = '', onNight = () => {}, usage, justClosed = null, onRecapAction = () => {}, agenda = null, onRecapPage = () => {}, canWrite = false, onTalk = () => {}, onEdit = () => {} }: {
    /** `selected`: le sessioni aperte, evidenziate; `withMaster`: la master nella lista come le altre (la plancia). */
    st: State; selected: string[]; onPick: (name: string) => void; withMaster?: boolean
    /** In fondo alla lista: sulla plancia i pannelli della quota (footer di SummaryList). */
    footer?: Snippet
    onAnswer: (session: string, n: number) => void; onStep: (session: string, text: string) => void
    /** Contratto 1.37: l'ok a un compito (`approve`) e «Chiudi» per una sessione finita o doppione (/exit). */
    onApprove?: (task: string, note: string) => void; onClose?: (name: string) => void
    /** Il riquadro Notte (mockup approvato l'08/10 alle 12:57): `undefined` = nascosto, null = rapporto in arrivo. */
    night?: NightPage | null | undefined; nightTitle?: string; onNight?: () => void
    /** «Utilizzo»: i pannelli della quota in una sezione richiudibile. */
    usage?: Snippet
    /** La sessione appena chiusa dal pannello di chiusura (Franz, 08/10 18:59): la sua card in cima alle sessioni è dove la
     *  pagina atterra tornando alla home (`view-transition-name: page-fly`), poi si richiude da sola. */
    justClosed?: { s: Session; line: string } | null
    /** La sezione Recap (mockup approvato l'08/10, Franz 20:41): l'azione confermata con «Manda». */
    onRecapAction?: (a: RecapAction) => void
    /** Contratto 1.46: le righe dell'agenda (null = non ancora arrivate); «Tutto il recap» apre la pagina. */
    agenda?: import('./contract').AgendaPage | null; onRecapPage?: () => void
    /** Le Azioni sulle schede (piano del 09/10): scritture con l'op del contratto 1.47, «Parlane» apre la master. */
    canWrite?: boolean; onTalk?: (text: string) => void; onEdit?: (e: Edit) => void
  } = $props()
  let acts: AgendaActions | undefined = $state()
  const today = todayIso()
  // Sezioni richiudibili (Franz, 08/10 12:30): restano come le hai lasciate, anche alla prossima apertura.
  const SEC = 'home-sections'
  let open = $state<Record<string, boolean>>((() => { try { return { night: true, sessions: true, usage: false, other: false, ...JSON.parse(localStorage.getItem(SEC) ?? '{}') } } catch { return { night: true, sessions: true, usage: false, other: false } } })())
  function toggleSec(k: string) { open = { ...open, [k]: !open[k] }; try { localStorage.setItem(SEC, JSON.stringify(open)) } catch { /* senza memoria resta per questa visita */ } }
  // La sezione Recap: le Azioni come tasti; il tocco apre «Mandare questa azione?» e «Manda» la fa partire.
  const actions = $derived(recapActions(st, t.recapResume))
  const recapDay = $derived(/^\d{4}-\d{2}-\d{2}$/.test(st.recap.date) ? `${st.recap.date.slice(8, 10)}/${st.recap.date.slice(5, 7)}` : '')
  let asking = $state<RecapAction | null>(null)
  let sentActions = $state(new Set<string>())
  const actionKey = (a: RecapAction) => `${a.to}\n${a.send}`
  function askSend(a: RecapAction) { asking = a }
  const ag = $derived(agendaModel(agenda, null, today))
  const hhmm = (s: number) => new Intl.DateTimeFormat('it-IT', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s * 1000))
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
  let confirming = $state<string | null>(null)
  // Il tasto di un passo toccato resta segnato «mandato» finché la sessione non cambia esito (Franz, 06/10 12:32): la card
  // risponde subito, poi la sessione al lavoro la aggiorna.
  let sentSteps = $state(new Set<string>())
  const stepKey = (s: { name: string; outcome?: { at: number } | null }, step: string) => `${s.name}@${s.outcome?.at ?? 0}:${step}`
  function askApprove(a: Approval) { pending = a; note = ''; approveDlg?.showModal() }
  const ctxTone = (p: number) => (p >= 90 ? 'var(--b-alert)' : p >= 75 ? 'var(--b-warn)' : 'var(--b-ring)')
  function text(r: Row) {
    const parsed = parseSteps(r.text ?? '')
    const shown = r.group === 'waiting' ? parsed.text : r.session.outcome ? outcomeSummary(r.session.outcome) : parsed.text
    return { body: shown.replace(/\*\*|__|`/g, '').trim(), steps: r.group === 'waiting' ? [] : parsed.steps, blocking: blockingOf(r.session, parsed.blocking) }
  }
</script>

{#snippet sec(key: string, title: string, summary: string | null = null)}
  <button class="sec" aria-expanded={open[key]} onclick={() => toggleSec(key)}>
    <svg viewBox="0 0 24 24" width="16" height="16"><path d={open[key] ? 'M6 9l6 6 6-6' : 'M9 6l6 6-6 6'} fill="none" stroke="var(--icon)" stroke-width="2.4" stroke-linecap="round" /></svg>
    <span>{title.toUpperCase()}</span><i></i>{#if summary && !open[key]}<small>{summary}</small>{/if}
  </button>
{/snippet}

<section class="home">
  {#if night !== undefined}
    {@render sec('night', t.homeNight)}
    {#if open.night}
      <button class="nightbox" onclick={onNight}>
        <b>{nightTitle}</b>
        {#if night}
          <small>{t.homeNightWindow(hhmm(night.start), hhmm(night.end), night.cards.length)}</small>
          <span class="nchips">
            {#if night.counts.done}<span style:color="var(--good)">{t.nightDone(night.counts.done)}</span>{/if}
            {#if night.counts.running}<span style:color="var(--icon)">{t.nightRunningN(night.counts.running)}</span>{/if}
            {#if night.counts.stopped}<span style:color="var(--gone-dim)">{t.nightStopped(night.counts.stopped)}</span>{/if}
            {#if night.counts.asking}<span style:color="var(--advice)">{t.nightAsking(night.counts.asking)}</span>{/if}
          </span>
          <span class="nrow"><small>{night.needs.length ? t.homeNightNeeds(night.needs.length, night.needs[0].session, night.needs[0].text) : ''}</small><span class="open">{t.homeNightOpen}</span></span>
        {:else}<small>{t.nightLoading}</small>{/if}
      </button>
    {/if}
  {/if}
  {@render sec('sessions', t.homeSessions(model.rows.length))}
  {#if justClosed}
    <div class="card just" style:view-transition-name="page-fly">
      <div class="jrow"><Badge s={justClosed.s} size={24} /><span class="jcol"><b>{justClosed.s.name}</b><small>{justClosed.line}</small></span></div>
    </div>
  {/if}
  {#if open.sessions}
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
      {@const c = cleanupOf(s, canExit, st.ts)}
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
            {#each x.steps as step}{@const k = stepKey(s, step)}<button class="chip" class:unblock={x.blocking.has(step)} class:sent={sentSteps.has(k)} disabled={sentSteps.has(k)} aria-label={sentSteps.has(k) ? t.stepSent(step) : undefined} onclick={(e) => { e.stopPropagation(); sentSteps = new Set([...sentSteps, k]); onStep(s.name, step) }}>{#if sentSteps.has(k)}<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--good)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5" /></svg>{:else if x.blocking.has(step)}<svg class="lock" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="var(--wait)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 9.9-1" /></svg>{/if}{step}</button>{/each}
          </div>
        {/if}
        {#if c}
          <div class="clean">
            <!-- Chi ha finito o è un doppione resta qui e dice di essere aperta (Franz, 06/10 10:57). -->
            <span>{#if s.attached}{t.cleanupAttached}{:else}<b class="alive">● {t.cleanupOpen}</b> · {c.kind === 'duplicate' ? t.cleanupDuplicate(c.of ?? '') : s.outcome?.at ? t.cleanupFinishedAt(new Date(s.outcome.at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })) : t.cleanupFinished}{/if}</span>
            {#if c.canClose && confirming !== s.name}
              <button class="close" onclick={(e) => { e.stopPropagation(); confirming = s.name }}>
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="var(--b-alert)" stroke-width="2" stroke-linecap="round"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>{t.cleanupClose(s.name)}
              </button>
            {/if}
          </div>
          <!-- La conferma sta nella card, senza finestre (Franz, 06/10 12:40): il secondo tocco, col nome, chiude. -->
          {#if c.canClose && confirming === s.name}
            <div class="confirm" role="group" aria-label={t.closeTitle(s.name)}>
              <span>{t.closeInline}</span>
              <button class="keep" onclick={(e) => { e.stopPropagation(); confirming = null }}>{t.closeKeep}</button>
              <button class="danger" onclick={(e) => { e.stopPropagation(); confirming = null; onClose(s.name) }}>{t.closeOk(s.name)}</button>
            </div>
          {/if}
        {/if}
        {#if s.context != null}
          <div class="track"><div class="fill" style="width:{Math.min(100, s.context)}%;background:{ctxTone(s.context)}"></div></div>
        {/if}
      </div>
    {/each}
  {/each}

  {/if}
  <!-- Il Recap (tavola 1): chiuso i conteggi (azioni · aspetta te · può farlo Claude); aperto le Azioni, le prime due cose
       che aspettano te, la prima che può fare Claude, e «Tutto il recap». -->
  {#if actions.length || ag.you.length || ag.claude.length}
    {@render sec('recap', t.homeRecap, `${actions.length} · ${ag.you.length} · ${ag.claude.length}`)}
    {#if open.recap}
      {#if actions.length}
        <h2 class="gh" style="--t:var(--text)"><span>{t.recapActions(actions.length).toUpperCase()}</span><i></i></h2>
        <div class="chips">
          {#each actions as a (actionKey(a))}
            {@const done = sentActions.has(actionKey(a))}
            <button class="act" class:sent={done} disabled={done} onclick={() => askSend(a)}>
              {#if done}<svg viewBox="0 0 24 24" width="16" height="16"><path d="M5 12l5 5 9-10" fill="none" stroke="var(--good)" stroke-width="2.4" stroke-linecap="round" /></svg>{/if}
              <span>{a.text}</span><small>{a.recap ? t.recapFrom(recapDay) : a.from}</small>
            </button>
          {/each}
        </div>
      {/if}
      {#if ag.you.length}
        <h2 class="gh" style="--t:var(--b-warn)"><span>{t.recapYou(ag.you.length).toUpperCase()}</span><i></i></h2>
        {#each ag.you.slice(0, 2) as r}<AgendaItem row={r} onOpen={() => acts?.deepen(r)} onActions={() => acts?.menuOf(r)} />{/each}
      {/if}
      {#if ag.claude.length}
        <h2 class="gh" style="--t:var(--icon)"><span>{t.recapClaude(ag.claude.length).toUpperCase()}</span><i></i></h2>
        <AgendaItem row={ag.claude[0]} onOpen={() => acts?.deepen(ag.claude[0])} onActions={() => acts?.menuOf(ag.claude[0])} />
      {/if}
      <button class="all" onclick={onRecapPage}><span>{t.recapAll}</span>
        <svg viewBox="0 0 24 24" width="20" height="20"><path d="M10 7l5 5-5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg></button>
    {/if}
  {/if}
  {#if usage}
    {@render sec('usage', t.homeUsage)}
    {#if open.usage}<div class="footer">{@render usage()}</div>{/if}
  {/if}
  {#if model.closed.length}
    {@render sec('other', t.homeOther, t.closedCat(model.closed.length))}
  {/if}
  {#if model.closed.length && open.other}
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

<AgendaActions bind:this={acts} {canWrite} {today} onSend={onRecapAction} {onTalk} {onEdit} />
<RecapSend action={asking} day={recapDay} onClose={() => (asking = null)} onSend={(a) => { sentActions = new Set([...sentActions, actionKey(a)]); onRecapAction(a) }} />

<style>
  .alive { color: var(--idle); font-weight: 500; }
  .home { padding: 12px 16px 24px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; height: 100%; }
  .gh { display: flex; align-items: center; gap: 8px; margin: 6px 8px 0; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--t); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--t) 25%, transparent); }
  .card.just { background: var(--surface); outline: 1.5px solid color-mix(in srgb, var(--icon) 70%, transparent); cursor: default; padding: 14px; }
  .jrow { display: flex; align-items: center; gap: 12px; }
  .jcol { display: flex; flex-direction: column; min-width: 0; }
  .jcol b { font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .jcol small { font-size: 14px; color: var(--text2); white-space: nowrap; overflow: hidden; }
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
  .confirm { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 13.5px; color: var(--text2); }
  .confirm span { flex: 1 1 160px; min-width: 0; }
  .confirm button { border-radius: 999px; padding: 8px 14px; font-weight: 500; flex: none; }
  .confirm .keep { color: var(--icon); }
  .confirm .keep:hover { background: var(--high); }
  .confirm .danger { background: var(--gone); color: #fff; }
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
  dialog .filled { background: var(--primary); color: var(--on-primary); }
  .chip { border-radius: 999px; padding: 8px 14px; font-size: 14px; background: color-mix(in srgb, var(--primary) 12%, transparent); }
  .chip:hover { filter: brightness(1.15); }
  /* Contratto 1.38, variante 2 (Franz, 05/10 22:06): il Prossimo che sblocca ha un filo ambra e il lucchetto aperto. */
  .chip.sent { display: inline-flex; align-items: center; gap: 6px; background: color-mix(in srgb, var(--good) 14%, transparent); color: var(--text2); cursor: default; }
  .chip.unblock { display: inline-flex; align-items: center; gap: 6px; box-shadow: inset 0 0 0 1.5px color-mix(in srgb, var(--wait) 65%, transparent); }
  .track { margin-right: 4px; height: 3px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .fill { height: 100%; }
  .sec { display: flex; align-items: center; gap: 8px; width: 100%; margin: 14px 0 4px; padding: 0 4px; font: 500 12px/1 var(--mono); letter-spacing: .14em; color: var(--text2); text-align: left; }
  .sec i { flex: 1; height: 1px; background: var(--line); }
  .sec small { font: 13px/1 var(--sans, inherit); letter-spacing: 0; }
  .nightbox { display: flex; flex-direction: column; gap: 10px; width: 100%; padding: 16px; border-radius: 24px; background: var(--surface); text-align: left; }
  .nightbox b { font-size: 17px; font-weight: 500; }
  .nightbox small { color: var(--text2); font-size: 14px; }
  .nchips { display: flex; gap: 8px; flex-wrap: wrap; }
  .nchips span { padding: 6px 12px; border-radius: 999px; background: var(--high); font-weight: 500; font-size: 14px; }
  .nrow { display: flex; align-items: center; gap: 12px; }
  .nrow small { flex: 1; }
  .open { padding: 10px 18px; border-radius: 22px; background: var(--primary); color: var(--on-primary); font-weight: 500; }
  .outside { margin: 18px 4px 0; }
  .oh { display: flex; align-items: center; gap: 12px; }
  .oh h3 { font-size: 16px; font-weight: 600; }
  .oh i { flex: 1; height: 1px; background: var(--line); }
  .osub { font-size: 12.5px; color: var(--stale); }
  .cat { margin-top: 6px; }
  .closed { display: flex; align-items: center; gap: 12px; padding: 14px; border-radius: 20px; background: var(--low); color: var(--text2); text-align: left; }
  .closed span { flex: 1; }
  .act { display: inline-flex; align-items: baseline; gap: 8px; border-radius: 999px; padding: 10px 16px; background: var(--low); color: var(--text); font-size: 15px; text-align: left; }
  .act:hover { filter: brightness(1.15); }
  .act small { font-size: 12.5px; color: var(--text2); }
  .act.sent { align-items: center; background: color-mix(in srgb, var(--good) 14%, transparent); color: var(--text2); cursor: default; }
  .act small { white-space: nowrap; }
  .all { display: flex; justify-content: space-between; align-items: center; padding: 12px 4px; color: var(--text2); font-size: 15px; text-align: left; }
  .footer { display: flex; flex-direction: column; gap: 12px; padding: 12px 0; }
</style>
