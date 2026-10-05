<script lang="ts">
  import { excerpt } from './speechRules'
  import { cycleRate, cycleVoice, speech, stop } from './speech.svelte'
  import { t } from './t'
  // Il mini-controller della lettura (ReadingPill dell'app): le barrette che si muovono, da dove arriva il testo e la sua
  // prima riga (il clic riporta lì), la velocità, la voce e ■ per fermare. Stretto restano solo i tasti.
  let { onOpen }: { onOpen?: (source: string) => void } = $props()
  const rate = $derived(`${speech.rate.toLocaleString('it-IT', { maximumFractionDigits: 2 })}×`)
</script>

{#if speech.text}
  <div class="pill" role="region" aria-label={t.readingNow}>
    <span class="bars" aria-hidden="true"><i></i><i></i><i></i></span>
    <button class="what" disabled={!speech.source || !onOpen} aria-label={speech.source ? t.readingOpen(speech.source) : undefined}
      onclick={() => speech.source && onOpen?.(speech.source)}>
      <span class="mono">{speech.source ?? t.readingNow}</span>
      <span class="line">{excerpt(speech.text)}</span>
    </button>
    <button class="chip" aria-label={t.rateChange(rate)} title={t.rateChange(rate)} onclick={cycleRate}>{rate}</button>
    {#if speech.voices.length}
      <button class="chip" aria-label={t.voiceChange(speech.voice ?? t.voiceDefault)} title={t.voiceChange(speech.voice ?? t.voiceDefault)} onclick={cycleVoice}>
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="4" /><path d="M2 21a7 7 0 0 1 14 0M17.5 4.5a5 5 0 0 1 0 7M20.5 2a9 9 0 0 1 0 12.5" /></svg>
      </button>
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
  .chip:hover, .stop:hover { filter: brightness(1.2); }
  /* In una colonna stretta restano i tasti: la riga del testo si vede già nella colonna. */
  @container (max-width: 300px) { .what { visibility: hidden; } }
</style>
