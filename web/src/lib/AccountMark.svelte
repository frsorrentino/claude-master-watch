<script lang="ts">
  import { t } from './t'
  // La forma dell'account (AccountMark dell'app): tondo il personale, quadrato il lavoro.
  let { personal, size = 14, color = 'var(--text2)' }: { personal: boolean; size?: number; color?: string } = $props()
</script>

<svg viewBox="0 0 14 14" width={size} height={size} role="img" aria-label={personal ? t.badgePersonal : t.badgeWork}>
  {#if personal}<circle cx="7" cy="7" r="5.5" fill="none" stroke={color} stroke-width="2" />{:else}<rect x="1" y="1" width="12" height="12" rx="3.2" fill="none" stroke={color} stroke-width="2" />{/if}
</svg>

<style>svg { flex: none; display: block; }</style>
