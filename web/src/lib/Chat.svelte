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
  import QuestionCard from './QuestionCard.svelte'
  import { CHAT_ARG } from './questionRules'
  import { append, box as boxOf, inDraft } from './composer'
  import Composer from './Composer.svelte'
  import PromptBox from './PromptBox.svelte'
  import { parseSteps } from './nextSteps'
  import { t } from './t'
  let { st, s, entries, mine, onSend, onBack, onPick, onAnswer, onCmd, wide, events, sent, read, onRead, onPromptTo }: {
    st: State; s: Session; entries: TranscriptEntry[]; mine: [Sent, Status][]; onSend: (text: string) => void; onBack?: () => void
    onPick: (name: string) => void; onAnswer: (session: string, n: number) => void
    onCmd: (op: CmdOp, arg?: string, text?: string) => void; wide: boolean
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
  $effect(() => { items.length; if (!home) list?.scrollTo({ top: list.scrollHeight, behavior: 'smooth' }) })
</script>

<section class="chat">
  <Header {st} {s} wide={wide} {onBack} {onCmd} onPrompt={onSend} />
  {#if s.name === MASTER && conversation}<button class="tohome" onclick={() => (conversation = false)}>{t.home}</button>{/if}

  <div class="lines" class:dots={home} bind:this={list}>
    {#if home}
      <MasterHome {st} master={s} {entries} onConversation={() => (conversation = true)} onStep={pick} onSendStep={onSend}
        {onAnswer} onSession={onPick} onSpeak={toggle} {events} {sent} {read} {onRead} onPrompt={onPromptTo} {onCmd} />
    {:else}
    <Feed {s} {items} now={st.ts} />
    {#if s.question}
      <QuestionCard q={s.question} onAnswer={(n) => onCmd('answer', String(n))} onChat={() => onCmd('answer', CHAT_ARG)} onAllowAll={() => onCmd('allow_all')} />
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
  <Composer bind:this={composer} {st} {s} bind:draft field={stepsBox.field} toMaster={s.name === MASTER} {onSend}
    onAnswerText={(arg) => onCmd('answer', arg)} onSlash={(c, a) => onCmd('slash', c, a ?? undefined)} onStop={() => onCmd('interrupt')}
    onReopen={() => onCmd('reopen')} onAttach={(fs, text) => onCmd('report', fs.map(f => f.name).join(', '), text)}
    onRecurring={recurringRows.length ? () => { recurringOpen = !recurringOpen; save('cm.recurring_open', recurringOpen) } : null} />
</section>

<style>
  .chat { display: flex; flex-direction: column; height: 100%; min-width: 0; }
  .tohome { align-self: flex-start; margin: 8px 12px 0; color: var(--icon); padding: 6px 12px; border-radius: 16px; }
  .tohome:hover { background: var(--surface); }
  /* La griglia di puntini dietro la casa della master (TechStyle.dotGrid): un'immagine ripetuta, niente ridisegni. */
  .lines.dots { padding: 16px max(10px, calc((100% - 760px) / 2)); }
  .dots { background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  .lines { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
</style>
