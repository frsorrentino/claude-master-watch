<script lang="ts">
  import type { Ring } from './overview'
  import AccountMark from './AccountMark.svelte'
  // Il doppio anello del polso (Utilizzo unito, approvato da Franz il 09/10 alle 16:17), come QuotaRing nell'app: le 5 ore
  // fuori, azzurre (rosse dal 90%), la settimana dentro, lavanda; al centro il segno dell'account. Un dato vecchio non
  // disegna le 5 ore.
  let { ring, size = 64, stale = ring.stale }: { ring: Ring; size?: number; stale?: boolean } = $props()
  const w = $derived(size * 0.1)
  const r1 = $derived(size / 2 - w / 2)
  const r2 = $derived(r1 - w - 3)
  const c = $derived(size / 2)
  const len = (r: number) => 2 * Math.PI * r
  const dash = (r: number, p: number) => `${len(r) * Math.min(100, Math.max(0, p)) / 100} ${len(r)}`
</script>

<span class="ring" style:width="{size}px" style:height="{size}px">
  <svg width={size} height={size} viewBox="0 0 {size} {size}" aria-hidden="true">
    <circle cx={c} cy={c} r={r1} fill="none" stroke="var(--b-track)" stroke-width={w} />
    <circle cx={c} cy={c} r={r2} fill="none" stroke="var(--b-track)" stroke-width={w} />
    {#if ring.h5 != null && !stale}
      <circle cx={c} cy={c} r={r1} fill="none" stroke={ring.h5 >= 90 ? 'var(--b-alert)' : 'var(--b-ring)'} stroke-width={w} stroke-linecap="round" stroke-dasharray={dash(r1, ring.h5)} transform="rotate(-90 {c} {c})" />
    {/if}
    {#if ring.w7 != null}
      <circle cx={c} cy={c} r={r2} fill="none" stroke="var(--b-week)" stroke-width={w} stroke-linecap="round" stroke-dasharray={dash(r2, ring.w7)} transform="rotate(-90 {c} {c})" />
    {/if}
  </svg>
  <span class="mark"><AccountMark personal={ring.personal} size={Math.round(size * 0.2)} /></span>
</span>

<style>
  .ring { position: relative; display: inline-grid; place-items: center; flex: none; }
  .ring svg { position: absolute; inset: 0; }
  .mark { display: grid; place-items: center; }
</style>
