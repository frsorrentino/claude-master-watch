<script lang="ts">
  import type { Question } from './contract'
  import { inline as isInline, needsLongPress, optionLabel } from './questionRules'
  import { t } from './t'
  // Le opzioni di una domanda, uguali dovunque (QuestionOptions dell'app): la prima piena se è l'azione della schermata,
  // brevi su una riga in parti uguali, lunghe una sotto l'altra; col rischio alto risponde solo la pressione lunga, e il
  // clic breve lo ricorda.
  let { q, onAnswer, firstFilled = true, tonal = 'var(--surface)' }: { q: Question; onAnswer: (n: number) => void; firstFilled?: boolean; tonal?: string } = $props()
  const long = $derived(needsLongPress(q.tier))
  const row = $derived(isInline(q.options))
  let hint = $state(false)
  let held: ReturnType<typeof setTimeout> | null = null
  let fired = false
  const HOLD_MS = 600
  function down(n: number, e: PointerEvent) {
    if (!long || e.button !== 0) return
    fired = false
    held = setTimeout(() => { fired = true; onAnswer(n) }, HOLD_MS)
  }
  function up() { if (held) clearTimeout(held); held = null }
  function click(n: number, e: MouseEvent) {
    e.stopPropagation()
    if (!long) onAnswer(n); else if (!fired) hint = true
  }
</script>

{#if long}<p class="warn" class:hint>{hint ? t.questionHold : t.questionHighRisk}</p>{/if}
<div class="opts" class:row>
  {#each q.options as o, i}
    <button class="opt" class:filled={i === 0 && firstFilled} class:long style="--tonal:{tonal}"
      onpointerdown={(e) => down(o.n, e)} onpointerup={up} onpointerleave={up} onpointercancel={up} onclick={(e) => click(o.n, e)}
      oncontextmenu={(e) => long && e.preventDefault()}>{optionLabel(o)}</button>
  {/each}
</div>

<style>
  .warn { font-size: 14px; font-weight: 500; color: var(--b-warn); }
  .warn.hint { color: var(--wait); }
  .opts { display: flex; flex-direction: column; gap: 6px; }
  .opts.row { flex-direction: row; gap: 2px; }
  .opt { min-height: 44px; padding: 10px 18px; border-radius: 22px; background: var(--tonal); color: var(--text); font-size: 16px; font-weight: 500; text-align: left; }
  .row .opt { flex: 1; text-align: center; white-space: nowrap; overflow: hidden; border-radius: 6px; }
  .row .opt:first-child { border-radius: 22px 6px 6px 22px; }
  .row .opt:last-child { border-radius: 6px 22px 22px 6px; }
  .row .opt:only-child { border-radius: 22px; }
  .opt.filled { background: var(--primary); color: var(--on-primary); }
  .opt:hover { filter: brightness(1.12); }
  /* La pressione lunga si vede: il tasto si riempie mentre lo tieni. */
  .opt.long:active { background-image: linear-gradient(90deg, rgb(255 255 255 / .18) 0 0); background-size: 0 100%; background-repeat: no-repeat; animation: hold 600ms linear forwards; }
  @keyframes hold { to { background-size: 100% 100%; } }
</style>
