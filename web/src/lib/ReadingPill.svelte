<script lang="ts">
  import { excerpt, nextRate } from './speechRules'
  import { cycleVoice, pause, resume, setRate, speech, stop } from './speech.svelte'
  import RateSlider from './RateSlider.svelte'
  import { t } from './t'
  // Il mini-controller della lettura (ReadingPill dell'app): le barrette che si muovono, da dove arriva il testo e la sua
  // prima riga (il clic riporta lì), la velocità, la voce e ■ per fermare. Stretto restano solo i tasti.
  // Un controller solo per schermata: `slots` sono i posti in vista (le colonne, o la home = null), `here` questo. Sta sopra
  // il campo della sessione che legge, se è in vista; altrimenti nel primo posto.
  let { onOpen, slots, here }: { onOpen?: (source: string) => void; slots: (string | null)[]; here: string | null } = $props()
  const mineHere = $derived(speech.source != null && slots.includes(speech.source) ? speech.source === here : slots[0] === here)
  const rate = $derived(`${speech.rate.toLocaleString('it-IT', { maximumFractionDigits: 2 })}×`)
  // La velocità aperta nella barra (Franz, 07/10, approvata alle 22:06): senza tocchi per 3 s torna pillola.
  let rateOpen = $state(false)
  let draft = $state(1)
  let closer: ReturnType<typeof setTimeout> | undefined
  function touch() { clearTimeout(closer); closer = setTimeout(() => (rateOpen = false), 3_000) }
  function openRate() { draft = speech.rate; rateOpen = true; touch() }
  // Il tocco sulla pillola passa alla velocità dopo, la pressione lunga apre lo slider (Franz, 08/10 19:50).
  let holdTimer: ReturnType<typeof setTimeout> | undefined
  let held = false
  function holdStart() { held = false; clearTimeout(holdTimer); holdTimer = setTimeout(() => { held = true; navigator.vibrate?.(15); openRate() }, 450) }
  function holdEnd() { clearTimeout(holdTimer) }
  function chipClick() { if (held) { held = false; return } setRate(nextRate(speech.rate)) }
  // Mentre si trascina la voce cambia velocità quando il dito si ferma un attimo: il motore non cambia velocità a metà
  // frase, quindi riparte dal pezzo che sta dicendo.
  let live: ReturnType<typeof setTimeout> | undefined
  function liveRate(v: number) { clearTimeout(live); live = setTimeout(() => { if (v !== speech.rate) setRate(v) }, 300) }
  const draftLabel = $derived(`${draft.toLocaleString('it-IT', { maximumFractionDigits: 2 })}×`)
</script>

{#if speech.text && mineHere}
  <div class="pill" class:open={rateOpen} role="region" aria-label={t.readingNow}>
    {#if rateOpen}
      <button class="chip val" aria-label={t.rateChange(draftLabel)} onclick={() => (rateOpen = false)}>{draftLabel}</button>
      <RateSlider value={speech.rate} onInput={(v) => { draft = v; touch(); liveRate(v) }} onDone={(v) => { clearTimeout(live); if (v !== speech.rate) setRate(v); touch() }} />
    {:else}
      <span class="bars" class:still={speech.paused} aria-hidden="true"><i></i><i></i><i></i></span>
      <button class="what" disabled={!speech.source || !onOpen} aria-label={speech.source ? t.readingOpen(speech.source) : undefined}
        onclick={() => speech.source && onOpen?.(speech.source)}>
        <span class="mono">{speech.paused ? t.readingPaused : speech.source ?? t.readingNow}</span>
        <span class="line">{excerpt(speech.text)}</span>
      </button>
      <button class="chip" aria-label={t.rateChange(rate)} title={t.rateChange(rate)} onclick={chipClick}
        onpointerdown={holdStart} onpointerup={holdEnd} onpointerleave={holdEnd} oncontextmenu={(e) => { e.preventDefault(); holdEnd(); held = true; openRate() }}>{rate}</button>
      {#if speech.voices.length}
        <button class="chip" aria-label={t.voiceChange(speech.voice ?? t.voiceDefault)} title={t.voiceChange(speech.voice ?? t.voiceDefault)} onclick={cycleVoice}>
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="4" /><path d="M2 21a7 7 0 0 1 14 0M17.5 4.5a5 5 0 0 1 0 7M20.5 2a9 9 0 0 1 0 12.5" /></svg>
        </button>
      {/if}
    {/if}
    {#if speech.paused}
      <button class="pp play" aria-label={t.readingResume} title={t.readingResume} onclick={resume}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" /></svg></button>
    {:else}
      <button class="pp" aria-label={t.readingPause} title={t.readingPause} onclick={pause}><svg viewBox="0 0 24 24" width="22" height="22"><rect x="6.5" y="5" width="4" height="14" rx="1.2" fill="currentColor" /><rect x="13.5" y="5" width="4" height="14" rx="1.2" fill="currentColor" /></svg></button>
    {/if}
    <button class="stop" aria-label={t.stopReading} title={t.stopReading} onclick={stop}><svg viewBox="0 0 24 24" width="20" height="20"><rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor" /></svg></button>
  </div>
{/if}

<style>
  .pill { container-type: inline-size; display: flex; align-items: center; gap: 10px; min-height: 60px; padding: 6px 8px 6px 16px; margin: 0 12px; border-radius: 28px; background: var(--high); box-shadow: 0 6px 18px rgb(0 0 0 / .45); }
  .bars { display: flex; gap: 3px; align-items: flex-end; height: 20px; flex: none; }
  .bars i { width: 4px; height: 60%; border-radius: 2px; background: var(--icon); animation: bar 520ms ease-in-out infinite alternate; }
  .bars i:nth-child(2) { animation-delay: 180ms; }
  .bars i:nth-child(3) { animation-delay: 360ms; }
  @keyframes bar { from { height: 35%; } to { height: 100%; } }
  @media (prefers-reduced-motion: reduce) { .bars i { animation: none; } }
  .what { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; text-align: left; padding: 4px 6px; border-radius: 12px; }
  .what:not(:disabled):hover { background: rgb(255 255 255 / .05); }
  .what:disabled { cursor: default; }
  .what .mono { white-space: nowrap; overflow: hidden; }
  .line { font-size: 14.5px; white-space: nowrap; overflow: hidden; }
  .chip { flex: none; min-width: 40px; height: 34px; padding: 0 10px; border-radius: 17px; background: var(--surface); color: var(--text); font-weight: 500; font-size: 14px; display: grid; place-items: center; }
  .stop { flex: none; width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); }
  .pill.open { padding-left: 8px; }
  .val { background: var(--primary); color: var(--on-primary); font-weight: 600; min-width: 64px; }
  .pp { flex: none; width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); background: #33405A; }
  .pp.play { background: var(--primary); color: var(--on-primary); }
  .pp:hover { filter: brightness(1.15); }
  .bars.still i { animation: none; height: 45%; background: var(--text2); }
  .chip:hover, .stop:hover { filter: brightness(1.2); }
  /* In una colonna stretta restano i tasti: la riga del testo si vede già nella colonna. */
  @container (max-width: 300px) { .what { visibility: hidden; } }
</style>
