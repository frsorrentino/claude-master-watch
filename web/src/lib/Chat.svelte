<script lang="ts">
  import type { Session, State } from './contract'
  import { MASTER } from './summary'
  import MasterHome from './MasterHome.svelte'
  import type { Line } from './demo'
  import { parseSteps } from './nextSteps'
  import { t } from './t'
  import Badge from './Badge.svelte'
  let { st, s, lines, onSend, onBack, onPick, onAnswer }: {
    st: State; s: Session; lines: Line[]; onSend: (text: string) => void; onBack?: () => void
    onPick: (name: string) => void; onAnswer: (session: string, n: number) => void
  } = $props()
  // La master si apre sulla sua casa; la conversazione è a un tocco (casa A).
  let conversation = $state(false)
  const home = $derived(s.name === MASTER && !conversation)
  function speak(text: string) { speechSynthesis.cancel(); const u = new SpeechSynthesisUtterance(text); u.lang = 'it-IT'; speechSynthesis.speak(u) }

  let draft = $state('')
  let list: HTMLElement | undefined = $state()
  const last = $derived([...lines].reverse().find(l => l.role === 'assistant'))
  const steps = $derived(last ? parseSteps(last.text).steps : [])
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })

  function send() {
    const text = draft.trim()
    if (!text) return
    onSend(text); draft = ''
  }
  // Invio manda, Maiusc+Invio va a capo, come sul tablet con la tastiera fisica.
  function key(e: KeyboardEvent) { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send() } }
  function pick(step: string) { draft = draft.trim() ? `${draft.trim().replace(/[.,;:]$/, '')} e poi ${step}` : step }
  $effect(() => { lines.length; if (!home) list?.scrollTo({ top: list.scrollHeight, behavior: 'smooth' }) })
</script>

<section class="chat">
  <header>
    {#if onBack}<button class="back" onclick={onBack} aria-label={t.back}>←</button>{/if}
    <Badge {s} size={26} />
    <h1>{s.name}</h1>
    <span class="mono">{[s.model?.label, s.effort].filter(Boolean).join(' · ')}</span>
    <span class="sp"></span>
    {#if s.context != null}<span class="mono">{t.ctx(s.context)}</span>{/if}
    {#if s.name === MASTER && conversation}<button class="tohome" onclick={() => (conversation = false)}>{t.home}</button>{/if}
  </header>

  <div class="lines" class:dots={home} bind:this={list}>
    {#if home}
      <MasterHome {st} master={s} entries={lines} onConversation={() => (conversation = true)} onStep={pick} onSendStep={onSend}
        {onAnswer} onSession={onPick} onSpeak={speak} />
    {:else}
    {#each lines as l (l.id)}
      {#if l.role === 'user'}
        <div class="me"><p>{l.text}</p><span class="mono">{hm(l.at)}</span></div>
      {:else}
        <div class="claude"><p>{parseSteps(l.text).text}</p><span class="mono">{hm(l.at)}</span></div>
      {/if}
    {/each}
    {#if s.question}
      <div class="question">
        <p>{s.question.text}</p>
        <div class="options">
          {#each s.question.options as o, i}
            <button class:first={i === 0} onclick={() => onSend(String(o.n))}>{o.n} · {o.label}</button>
          {/each}
        </div>
      </div>
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
    <textarea rows="1" bind:value={draft} onkeydown={key} placeholder={s.suggestion ?? t.writeTo(s.name)}></textarea>
    {#if !draft && s.suggestion}<button type="button" class="use" onclick={() => (draft = s.suggestion ?? '')}>{t.use}</button>{/if}
    <button type="submit" class="sendbtn" disabled={!draft.trim()} aria-label={t.send}>➤</button>
  </form>
  <div class="hint mono">{t.enterSends}</div>
</section>

<style>
  .chat { display: flex; flex-direction: column; height: 100%; min-width: 0; }
  header { display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-bottom: 1px solid var(--line); }
  h1 { font-size: 19px; font-weight: 600; }
  .sp { flex: 1; }
  .tohome { color: var(--icon); padding: 6px 12px; border-radius: 16px; }
  .tohome:hover { background: var(--surface); }
  /* La griglia di puntini dietro la casa della master (TechStyle.dotGrid): un'immagine ripetuta, niente ridisegni. */
  .lines.dots { padding: 16px max(10px, calc((100% - 760px) / 2)); }
  .dots { background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  .back { font-size: 20px; padding: 4px 8px; }
  .lines { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
  .me { align-self: flex-end; max-width: 75%; display: flex; flex-direction: column; align-items: flex-end; gap: 4px; }
  .me p { background: var(--surface); border-radius: 20px 20px 6px 20px; padding: 10px 14px; }
  .claude { display: flex; flex-direction: column; gap: 4px; max-width: 760px; }
  p { white-space: pre-wrap; overflow-wrap: anywhere; }
  .question { border: 2px solid var(--wait); background: var(--high); border-radius: 24px; padding: 16px; display: flex; flex-direction: column; gap: 12px; max-width: 760px; }
  .options { display: flex; flex-wrap: wrap; gap: 8px; }
  .options button { background: var(--surface); border-radius: 999px; padding: 8px 16px; }
  .options button.first { background: var(--primary); color: var(--on-primary); font-weight: 500; }
  .steps { margin: 0 16px; background: var(--low); border: 1px solid var(--line); border-radius: 18px; padding: 8px 0; }
  .steps .mono { padding: 2px 14px 6px; }
  .step { display: flex; align-items: center; border-top: 1px solid var(--line); }
  .pick { flex: 1; text-align: left; padding: 10px 14px; }
  .go { padding: 10px 14px; color: var(--icon); }
  .pick:hover, .go:hover { background: var(--surface); }
  .composer { margin: 10px 16px 4px; display: flex; align-items: center; gap: 8px; border: 1px solid var(--line); border-radius: 28px; padding: 6px 6px 6px 18px; }
  .composer:focus-within { border-color: var(--icon); }
  textarea { flex: 1; resize: none; background: none; border: 0; outline: 0; color: var(--text); font: inherit; field-sizing: content; max-height: 8lh; padding: 8px 0; }
  textarea::placeholder { color: var(--text2); font-style: italic; }
  .use { color: var(--icon); padding: 6px 10px; }
  .sendbtn { width: 40px; height: 40px; border-radius: 50%; background: var(--primary); color: var(--on-primary); }
  .sendbtn:disabled { background: var(--surface); color: var(--text2); cursor: default; }
  .hint { padding: 0 34px 12px; }
  @media (max-width: 760px) { .hint { display: none; } .composer { margin-bottom: 14px; } }
</style>
