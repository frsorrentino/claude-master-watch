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
  import { CHAT_ARG, textArg } from './questionRules'
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
  let list: HTMLElement | undefined = $state()
  const items = $derived(group(merge(entries, mine)))
  const last = $derived([...entries].reverse().find(e => e.role === 'assistant' && e.text?.trim()))
  const steps = $derived(last ? parseSteps(last.text ?? '').steps : [])

  function send() {
    const text = draft.trim()
    if (!text) return
    // Con una domanda aperta il campo risponde a parole (contratto 1.10).
    if (s.question) onCmd('answer', textArg(text)); else onSend(text)
    draft = ''
  }
  // Invio manda, Maiusc+Invio va a capo, come sul tablet con la tastiera fisica.
  function key(e: KeyboardEvent) { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send() } }
  function pick(step: string) { draft = draft.trim() ? `${draft.trim().replace(/[.,;:]$/, '')} e poi ${step}` : step }
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

  {#if steps.length && !s.question && !home}
    <div class="steps">
      <div class="mono">{t.next.toUpperCase()} · {steps.length}</div>
      {#each steps as step}
        <div class="step"><button class="pick" onclick={() => pick(step)}>{step}</button><button class="go" aria-label={t.send} onclick={() => onSend(step)}>↗</button></div>
      {/each}
    </div>
  {/if}

  <form class="composer" onsubmit={(e) => { e.preventDefault(); send() }}>
    <textarea rows="1" class:hint={!s.question && !!s.suggestion} bind:value={draft} onkeydown={key} placeholder={s.question ? t.answerFree : s.suggestion ?? t.writeTo(s.name)}></textarea>
    {#if !draft && s.suggestion && !s.question}<button type="button" class="use" onclick={() => (draft = s.suggestion ?? '')}>{t.use}</button>{/if}
    <button type="submit" class="sendbtn" disabled={!draft.trim()} aria-label={t.send}>➤</button>
  </form>
  <div class="hint mono">{t.enterSends}</div>
</section>

<style>
  .chat { display: flex; flex-direction: column; height: 100%; min-width: 0; }
  .tohome { align-self: flex-start; margin: 8px 12px 0; color: var(--icon); padding: 6px 12px; border-radius: 16px; }
  .tohome:hover { background: var(--surface); }
  /* La griglia di puntini dietro la casa della master (TechStyle.dotGrid): un'immagine ripetuta, niente ridisegni. */
  .lines.dots { padding: 16px max(10px, calc((100% - 760px) / 2)); }
  .dots { background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  .lines { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
  .me { align-self: flex-end; max-width: 75%; display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
  .me p { background: var(--surface); border-radius: 20px 20px 6px 20px; padding: 10px 14px; }
  .claude { display: flex; flex-direction: column; gap: 4px; max-width: 760px; }
  p { white-space: pre-wrap; overflow-wrap: anywhere; }
  .steps { margin: 0 16px; background: var(--low); border: 1px solid var(--line); border-radius: 18px; padding: 8px 0; }
  .steps .mono { padding: 2px 14px 6px; }
  .step { display: flex; align-items: center; border-top: 1px solid var(--line); }
  .pick { flex: 1; text-align: left; padding: 10px 14px; }
  .go { padding: 10px 14px; color: var(--icon); }
  .pick:hover, .go:hover { background: var(--surface); }
  .composer { margin: 10px 16px 4px; display: flex; align-items: center; gap: 8px; border: 1px solid var(--line); border-radius: 28px; padding: 6px 6px 6px 18px; }
  .composer:focus-within { border-color: var(--icon); }
  textarea { flex: 1; resize: none; background: none; border: 0; outline: 0; color: var(--text); font: inherit; field-sizing: content; max-height: 8lh; padding: 8px 0; }
  textarea::placeholder { color: var(--text2); }
  /* In corsivo solo il suggerimento di Claude, come nell'app. */
  textarea.hint::placeholder { font-style: italic; }
  .use { color: var(--icon); padding: 6px 10px; }
  .sendbtn { width: 40px; height: 40px; border-radius: 50%; background: var(--primary); color: var(--on-primary); }
  .sendbtn:disabled { background: var(--surface); color: var(--text2); cursor: default; }
  .hint { padding: 0 34px 12px; }
  @media (max-width: 760px) { .hint { display: none; } .composer { margin-bottom: 14px; } }
</style>
