<script lang="ts">
  import type { Session } from './contract'
  import { badge, breathes, PATHS } from './badge'
  // Il badge come sul telefono e sull'orologio (SessionBadge.kt): glifo in un riquadro di 0,6 del diametro, tratto 3 su 24.
  // Il respiro è CSS sull'opacità: la GPU lo fa senza ridisegnare la pagina.
  let { s, size = 22 }: { s: Session; size?: number } = $props()
  const b = $derived(badge(s.account, s.color, s.state, s.icon, s.account_kind))
</script>

<svg width={size} height={size} viewBox="0 0 24 24" class:breathe={breathes(s.state)} role="img" aria-label={s.state}>
  {#if b.square}<rect width="24" height="24" rx="5.5" fill={b.fill} />{:else}<circle cx="12" cy="12" r="12" fill={b.fill} />{/if}
  <g transform="translate(4.8 4.8) scale(0.6)" fill="none" stroke={b.ink} stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
    {#each PATHS[b.glyph] as d}<path {d} />{/each}
  </g>
</svg>

<style>
  svg { flex: none; display: block; }
  .breathe { animation: breathe 1.5s ease-in-out infinite alternate; }
  @keyframes breathe { to { opacity: .55; } }
  @media (prefers-reduced-motion: reduce) { .breathe { animation: none; } }
</style>
