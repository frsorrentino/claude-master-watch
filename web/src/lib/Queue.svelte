<script lang="ts">
  import type { State } from './contract'
  import { items as queueOf, next as nextOf, page as pageOf } from './queue'
  import { since } from './durations'
  import { CHAT_ARG } from './questionRules'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import QuestionCard from './QuestionCard.svelte'

  // «Ti aspettano» (QueueScreen dell'app): le domande aperte di tutte le sessioni in fila, dalla più vecchia, una per
  // pagina; dopo una risposta si passa alla successiva. Si scorre di lato (trascinando o con le frecce e i tasti ← →).
  let { st, now, onAnswer, onSession }: {
    st: State; now: number; onAnswer: (session: string, arg: string, op?: 'answer' | 'allow_all') => void; onSession: (name: string) => void
  } = $props()

  const list = $derived(queueOf(st))
  let strip: HTMLElement | undefined = $state()
  let current = $state(0)
  const go = (i: number) => strip?.children[i]?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' })
  function onScroll() { if (strip) current = Math.round(strip.scrollLeft / Math.max(1, strip.clientWidth)) }
  function answered(session: string, questionId: string, arg: string, op: 'answer' | 'allow_all' = 'answer') {
    // La pagina dopo, subito: la domanda sparisce dalla fila quando il PC la chiude.
    const after = nextOf(st, questionId)
    onAnswer(session, arg, op)
    const i = after && after.questionId !== questionId ? pageOf(list, after.questionId) : null
    if (i != null) go(i)
  }
  function key(e: KeyboardEvent) {
    if ((e.target as HTMLElement)?.closest('input, textarea')) return
    if (e.key === 'ArrowRight') go(Math.min(list.length - 1, current + 1))
    if (e.key === 'ArrowLeft') go(Math.max(0, current - 1))
  }
</script>

<svelte:window onkeydown={key} />

<div class="queue">
  {#if !list.length}
    <p class="empty">{t.queueEmpty}</p>
  {:else}
    <div class="strip" bind:this={strip} onscroll={onScroll}>
      {#each list as it (it.questionId)}
        {@const s = st.sessions.find(x => x.name === it.session)}
        {#if s?.question}
          <div class="page">
            <button class="who" onclick={() => onSession(s.name)}><Badge {s} size={26} /><b>{s.name}</b><span>{t.queueAge(since(s.question.asked_at, now))}</span></button>
            <QuestionCard q={s.question} source={s.name} onAnswer={(n) => answered(s.name, it.questionId, String(n))}
              onChat={() => answered(s.name, it.questionId, CHAT_ARG)} onAllowAll={() => answered(s.name, it.questionId, '', 'allow_all')} />
          </div>
        {/if}
      {/each}
    </div>
    <div class="nav">
      <button class="arrow" aria-label={t.queuePrev} disabled={current === 0} onclick={() => go(current - 1)}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
      <span>{t.queuePosition(Math.min(current + 1, list.length), list.length)}</span>
      <button class="arrow" aria-label={t.queueNext} disabled={current >= list.length - 1} onclick={() => go(current + 1)}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M9 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
    </div>
  {/if}
</div>

<style>
  .queue { height: 100%; display: flex; flex-direction: column; }
  .empty { color: var(--text2); padding: 0 20px; font-size: 16px; }
  .strip { flex: 1; display: flex; overflow-x: auto; scroll-snap-type: x mandatory; scrollbar-width: none; }
  .page { flex: 0 0 100%; scroll-snap-align: start; padding: 0 16px; display: flex; flex-direction: column; gap: 12px; overflow-y: auto; }
  .who { display: flex; align-items: center; gap: 10px; padding: 6px 0; text-align: left; border-radius: 8px; }
  .who b { flex: 1; font-size: 16px; font-weight: 500; white-space: nowrap; overflow: hidden; }
  .who span { font-size: 14px; color: var(--text2); font-weight: 500; }
  .nav { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 12px; font-size: 14px; font-weight: 500; color: var(--text2); }
  .arrow { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .arrow:not(:disabled):hover { background: var(--surface); }
  .arrow:disabled { opacity: .3; cursor: default; }
</style>
