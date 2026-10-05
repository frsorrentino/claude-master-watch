<script lang="ts">
  import type { Session, State } from './contract'
  import { MASTER } from './summary'
  import MasterHome from './MasterHome.svelte'
  import Header from './Header.svelte'
  import type { CmdOp, Event } from './contract'
  import type { Scheduled } from './masterHome'
  import type { TranscriptEntry } from './contract'
  import { merge, group } from './chatFeed'
  import type { Sent, Status } from './chatRules'
  import { toggle } from './speech.svelte'
  import Feed from './Feed.svelte'
  import ReadingPill from './ReadingPill.svelte'
  import ElsewherePill from './ElsewherePill.svelte'
  import type { Alert } from './elsewhere'
  import QuestionCard from './QuestionCard.svelte'
  import { CHAT_ARG } from './questionRules'
  import { append, box as boxOf, inDraft } from './composer'
  import Composer from './Composer.svelte'
  import PromptBox from './PromptBox.svelte'
  import { parseSteps } from './nextSteps'
  import { t } from './t'
  import { ctxNudge, decisionDraft, decisionProject, DECISION_MAX } from './masterService'
  let { st, s, entries, mine, onSend, onBack, onPick, onAnswer, onCmd, wide, events, sent, read, onRead, onPromptTo, slots, onAttach, onFile, onHandoff, onDecision, elsewhere = null, onElsewhere = () => {}, onElsewhereDismiss = () => {} }: {
    st: State; s: Session; entries: TranscriptEntry[]; mine: [Sent, Status][]; onSend: (text: string) => void; onBack?: () => void
    onPick: (name: string) => void; onAnswer: (session: string, n: number) => void
    onCmd: (op: CmdOp, arg?: string, text?: string) => void; wide: boolean; slots: (string | null)[]
    /** Allegati del «+»: prima /share, poi `report` (contratti 1.19 e 1.28). */
    onAttach: (files: File[], text: string) => void
    /** Un file della conversazione (contratto 1.24): il PC lo manda e si apre in una scheda. */
    onFile: (path: string) => void
    /** Contratto 1.37: «Handoff, poi /clear» (prompt e /clear a turno finito, li gestisce App) e «Salva come decisione». */
    onHandoff: () => void; onDecision: (text: string, project: string | null) => void
    /** L'avviso delle altre sessioni sotto la barra (Elsewhere). */
    elsewhere?: Alert | null; onElsewhere?: () => void; onElsewhereDismiss?: () => void
    events: Event[]; sent: Scheduled[]; read: Set<string>; onRead: (key: string) => void; onPromptTo: (session: string, text: string) => void
  } = $props()
  // La master si apre sulla sua casa; la conversazione è a un tocco (casa A).
  let conversation = $state(false)
  const home = $derived(s.name === MASTER && !conversation)

  let draft = $state('')
  // Una bozza per sessione (DraftStore dell'app): cambiando sessione la bozza resta dov'era.
  const drafts: Record<string, string> = {}
  let shown = s.name
  $effect.pre(() => { const name = s.name; if (name !== shown) { drafts[shown] = draft; draft = drafts[name] ?? ''; shown = name } })
  let list: HTMLElement | undefined = $state()
  const items = $derived(group(merge(entries, mine)))
  // I consigli dell'ultima risposta di Claude, se dopo non c'è altro che passaggi.
  const last = $derived.by(() => { const it = [...items].reverse().find(i => i.type !== 'tool' && i.type !== 'steps'); return it?.type === 'claude' ? it.entry : null })
  const steps = $derived(last ? parseSteps(last.text ?? '').steps : [])

  let composer: Composer | undefined = $state()
  function pick(step: string) { draft = append(draft, step, t.then); composer?.focus() }
  // I box sopra il campo, aperti o chiusi come li hai lasciati: Prossimi aperto, Ricorrenti chiuso.
  const saved = (k: string, d: boolean) => { try { const v = localStorage.getItem(k); return v == null ? d : v === '1' } catch { return d } }
  const save = (k: string, v: boolean) => { try { localStorage.setItem(k, v ? '1' : '0') } catch { /* senza memoria resta per la sessione */ } }
  let stepsOpen = $state(saved('cm.steps_open', true))
  let recurringOpen = $state(saved('cm.recurring_open', false))
  const idle = $derived(s.state === 'idle' && !s.question)
  const stepsBox = $derived(idle ? boxOf(steps, s.suggestion, draft) : { field: null, rows: [] })
  const recurringRows = $derived(s.name === MASTER ? (st.recurring ?? []).filter(r => !inDraft(draft, r.prompt)).map(r => ({ label: r.label, text: r.prompt, direct: !r.param && idle })) : [])
  // Contratto 1.37: la proposta del contesto pieno sopra il campo; chiusa, torna alla fascia dopo (70, 80).
  const loadDismissed = (): Record<string, number> => { try { return JSON.parse(localStorage.getItem('cm.ctx_dismissed') ?? '{}') } catch { return {} } }
  let dismissed = $state(loadDismissed())
  const nudge = $derived(home ? null : ctxNudge(s, dismissed[s.name] ?? null))
  function dismissNudge() {
    dismissed = { ...dismissed, [s.name]: nudge! }
    try { localStorage.setItem('cm.ctx_dismissed', JSON.stringify(dismissed)) } catch { /* resta per la sessione */ }
  }
  // «Salva come decisione»: la bozza dalla risposta di Claude (o vuota dal + della master), il progetto della sessione.
  const canDecide = $derived(!!st.ops?.includes('decision'))
  let decisionDlg: HTMLDialogElement | undefined = $state()
  // File trascinati sulla colonna: vanno nel campo come quelli scelti con il +.
  let dragging = $state(false)
  const hasFiles = (e: DragEvent) => !!e.dataTransfer?.types.includes('Files')
  function onDrop(e: DragEvent) {
    if (!hasFiles(e)) return
    e.preventDefault()
    dragging = false
    if (composer?.addFiles([...(e.dataTransfer?.files ?? [])])) composer.focus()
  }
  let decisionText = $state('')
  let decisionAll = $state(false)
  const project = $derived(s.name === MASTER ? null : decisionProject(s))
  function openDecision(text: string) { decisionText = decisionDraft(text); decisionAll = project == null; decisionDlg?.showModal() }
  function saveDecision() {
    const text = decisionText.trim()
    if (!text) return
    onDecision(text, decisionAll ? null : project)
    decisionDlg?.close()
  }
  // In fondo alla conversazione: subito all'apertura (e dopo che la colonna ha preso la sua misura), con l'animazione
  // quando arriva qualcosa di nuovo.
  let settled = false
  $effect.pre(() => { s.name; settled = false })
  $effect(() => {
    items.length; s.question
    if (home || !list) return
    const el = list
    if (!settled) { requestAnimationFrame(() => { el.scrollTop = el.scrollHeight; settled = true }); return }
    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
  })
  // Una colonna che cambia larghezza fa andare a capo i testi: resta in fondo se lo era.
  $effect(() => {
    if (!list) return
    const el = list
    let atEnd = true
    const onScroll = () => { atEnd = el.scrollHeight - el.scrollTop - el.clientHeight < 40 }
    const ro = new ResizeObserver(() => { if (atEnd && !home) el.scrollTop = el.scrollHeight })
    el.addEventListener('scroll', onScroll, { passive: true })
    ro.observe(el)
    return () => { el.removeEventListener('scroll', onScroll); ro.disconnect() }
  })
</script>

<section class="chat" class:dragging aria-label={s.name}
  ondragover={(e) => { if (hasFiles(e)) { e.preventDefault(); dragging = true } }} ondragleave={(e) => { if (e.currentTarget === e.target) dragging = false }} ondrop={onDrop}>
  {#if dragging}<div class="dropzone">{t.dropHere}</div>{/if}
  <Header {st} {s} wide={wide} {onBack} {onCmd} onPrompt={onSend} {onHandoff} />
  {#if elsewhere}<ElsewherePill alert={elsewhere} onOpen={onElsewhere} onDismiss={onElsewhereDismiss} />{/if}
  {#if s.name === MASTER && conversation}<button class="tohome" onclick={() => (conversation = false)}>{t.home}</button>{/if}

  <div class="lines" class:dots={home} bind:this={list}>
    {#if home}
      <MasterHome {st} master={s} {entries} onConversation={() => (conversation = true)} onStep={pick} onSendStep={onSend}
        {onAnswer} onSession={onPick} onSpeak={(x) => toggle(x, s.name)} {events} {sent} {read} {onRead} onPrompt={onPromptTo} {onCmd} />
    {:else}
    <Feed {s} {items} now={st.ts} {onFile} onDecision={canDecide ? openDecision : undefined} />
    {#if s.question}
      <QuestionCard q={s.question} source={s.name} onAnswer={(n) => onCmd('answer', String(n))} onChat={() => onCmd('answer', CHAT_ARG)} onAllowAll={() => onCmd('allow_all')} />
    {/if}
    {/if}
  </div>

  {#if stepsBox.rows.length && !home}
    <PromptBox title={t.next} rows={stepsBox.rows.map(x => ({ label: x, text: x, direct: true }))} open={stepsOpen}
      onOpen={(o) => { stepsOpen = o; save('cm.steps_open', o) }} draftBlank={!draft.trim()} onPick={(r) => pick(r.text)} onSend={(r) => onSend(r.text)} />
  {/if}
  {#if recurringOpen && recurringRows.length}
    <PromptBox title={t.recurring} rows={recurringRows} open={true} onOpen={() => { recurringOpen = false; save('cm.recurring_open', false) }} draftBlank={!draft.trim()}
      onPick={(r) => { draft = append(draft, r.direct ? r.text : r.text.trimEnd() + ' ', t.then); recurringOpen = false; save('cm.recurring_open', false); composer?.focus() }}
      onSend={(r) => { recurringOpen = false; save('cm.recurring_open', false); onSend(r.text) }} />
  {/if}
  {#if nudge != null}
    <div class="nudge" role="status">
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--b-warn)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" /></svg>
      <span class="nt"><b>{t.ctxNudge(s.context ?? nudge)}</b>{t.ctxNudgeSub}</span>
      <button class="go" onclick={onHandoff}>{t.ctxNudgeGo}</button>
      <button class="x" aria-label={t.closeWord} title={t.closeWord} onclick={dismissNudge}><svg viewBox="0 0 24 24" width="18" height="18"><path d="M6 6l12 12M18 6 6 18" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg></button>
    </div>
  {/if}
  <ReadingPill onOpen={onPick} {slots} here={s.name} />
  <Composer bind:this={composer} {st} {s} bind:draft field={stepsBox.field} toMaster={s.name === MASTER} {onSend}
    onAnswerText={(arg) => onCmd('answer', arg)} onSlash={(c, a) => onCmd('slash', c, a ?? undefined)} onStop={() => onCmd('interrupt')}
    onReopen={() => onCmd('reopen')} {onAttach}
    onRecurring={recurringRows.length ? () => { recurringOpen = !recurringOpen; save('cm.recurring_open', recurringOpen) } : null}
    onDecision={canDecide ? () => openDecision('') : null} />
</section>

<dialog bind:this={decisionDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && decisionDlg?.close()}>
  <h3>{t.decisionSave}</h3>
  <p class="sub">{t.decisionSub}</p>
  <textarea bind:value={decisionText} rows="4" maxlength={DECISION_MAX}></textarea>
  {#if project}
    <p class="lab">{t.decisionFor}</p>
    <div class="pick">
      <button class:on={!decisionAll} aria-pressed={!decisionAll} onclick={() => (decisionAll = false)}>{project}</button>
      <button class:on={decisionAll} aria-pressed={decisionAll} onclick={() => (decisionAll = true)}>{t.decisionAll}</button>
    </div>
  {/if}
  <div class="btns">
    <button class="text2" onclick={() => decisionDlg?.close()}>{t.cancel}</button>
    <button class="filled" disabled={!decisionText.trim()} onclick={saveDecision}>{t.decisionOk}</button>
  </div>
</dialog>

<style>
  .chat { display: flex; flex-direction: column; height: 100%; min-width: 0; position: relative; }
  .dropzone { position: absolute; inset: 8px; z-index: 20; display: grid; place-items: center; border: 2px dashed color-mix(in srgb, var(--icon) 60%, transparent); border-radius: 24px; background: rgb(0 0 0 / .6); color: var(--icon); font-weight: 500; pointer-events: none; }
  .tohome { align-self: flex-start; margin: 8px 12px 0; color: var(--icon); padding: 6px 12px; border-radius: 16px; }
  .tohome:hover { background: var(--surface); }
  /* La griglia di puntini dietro la casa della master (TechStyle.dotGrid): un'immagine ripetuta, niente ridisegni. */
  .lines.dots { padding: 16px max(10px, calc((100% - 760px) / 2)); }
  .dots { background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  .lines { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
  .nudge { display: flex; align-items: center; gap: 12px; margin: 0 12px 8px; padding: 10px 6px 10px 14px; border-radius: 20px; background: var(--high); }
  .nudge .nt { flex: 1; min-width: 0; display: flex; flex-direction: column; font-size: 14px; line-height: 1.3; color: var(--text2); }
  .nudge .nt b { font-weight: 500; font-size: 14.5px; color: var(--text); }
  .nudge .go { background: color-mix(in srgb, var(--primary) 16%, var(--high)); color: var(--primary); border-radius: 999px; padding: 9px 16px; font-weight: 500; }
  .nudge .x { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; }
  .sheet { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 20px 24px; width: min(560px, 100vw); max-height: 85vh; border-radius: 28px; }
  .sheet::backdrop { background: rgb(0 0 0 / .55); }
  @media (max-width: 599px) { .sheet { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  .sheet h3 { font-size: 22px; font-weight: 600; margin-bottom: 4px; }
  .sheet .sub { color: var(--text2); font-size: 14px; }
  .sheet textarea { width: 100%; margin-top: 12px; background: var(--high); color: var(--text); border: 0; border-radius: 16px; padding: 12px 14px; font: inherit; font-size: 15.5px; resize: vertical; }
  .sheet .lab { font-size: 12px; color: var(--text2); margin: 14px 0 6px; }
  .pick { display: flex; gap: 8px; flex-wrap: wrap; }
  .pick button { background: var(--high); border-radius: 999px; padding: 7px 14px; font-weight: 500; }
  .pick button.on { background: color-mix(in srgb, var(--primary) 22%, var(--high)); color: var(--primary); }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  .btns button { padding: 10px 16px; border-radius: 20px; font-weight: 500; }
  .text2 { color: var(--text2); }
  .filled { background: var(--primary); color: var(--on-primary); }
  .filled:disabled { opacity: .5; }
</style>
