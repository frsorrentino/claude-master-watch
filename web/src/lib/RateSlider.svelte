<script lang="ts">
  import { RATE_MAX, RATE_MIN, RATE_STEP, rateFraction, snapRate } from './speechRules'
  import { t } from './t'
  // Lo slider della velocità (SpeechRate.kt, approvato da Franz il 07/10 alle 22:06): da 0,5× a 2× a passi di 0,05, il
  // segno di 1× sotto; `onDone` al rilascio, così la voce riparte una volta sola. Lo usano la barra di lettura e le Impostazioni.
  let { value, onInput = () => {}, onDone }: { value: number; onInput?: (v: number) => void; onDone: (v: number) => void } = $props()
  const label = (r: number) => `${r.toLocaleString('it-IT', { maximumFractionDigits: 2 })}×`
  let draft = $state(0)
  $effect(() => { draft = value })
</script>

<div class="rs">
  <input type="range" min={RATE_MIN} max={RATE_MAX} step={RATE_STEP} value={draft} aria-label={t.rateChange(label(draft))}
    oninput={(e) => { draft = snapRate(Number((e.currentTarget as HTMLInputElement).value)); onInput(draft) }}
    onchange={() => onDone(draft)} />
  <div class="labs" aria-hidden="true">
    <span style="left:0">{label(RATE_MIN)}</span>
    <span style="left:{rateFraction(1) * 100}%;transform:translateX(-50%)">{label(1)}</span>
    <span style="right:0">{label(RATE_MAX)}</span>
  </div>
</div>

<style>
  .rs { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  input { width: 100%; accent-color: var(--icon); margin: 0; height: 24px; cursor: pointer; }
  .labs { position: relative; height: 14px; margin: 0 6px; font-size: 11px; color: var(--text2); }
  .labs span { position: absolute; top: 0; white-space: nowrap; }
</style>
