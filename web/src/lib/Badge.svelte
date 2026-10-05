<script lang="ts">
  import type { Session } from './contract'
  // Il segno della sessione come sul telefono: il colore dice lo stato; chi aspetta te respira piano, solo con il CSS
  // (la GPU anima l'opacità senza ridisegnare la pagina).
  let { s, size = 22 }: { s: Session; size?: number } = $props()
  const tone = $derived({ waiting: 'var(--wait)', busy: 'var(--good)', awaiting: 'var(--icon)', idle: 'var(--text2)', gone: '#596273' }[s.state])
</script>

<span class="badge" class:breathe={s.state === 'waiting'} style="--t:{tone};width:{size}px;height:{size}px" aria-hidden="true">
  {#if s.state === 'waiting'}✋{:else if s.state === 'busy'}⚡{:else if s.state === 'gone'}✕{:else}❙❙{/if}
</span>

<style>
  .badge { display: inline-grid; place-items: center; border-radius: 50%; background: var(--t); color: #0B0F14; font-size: 11px; font-weight: 700; flex: none; }
  .breathe { animation: breathe 3s ease-in-out infinite; }
  @keyframes breathe { 50% { opacity: .55; } }
  @media (prefers-reduced-motion: reduce) { .breathe { animation: none; } }
</style>
