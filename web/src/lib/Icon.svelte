<script lang="ts">
  import { ICON, type IconName } from './icons'
  let { name, size = 18, color = 'currentColor', label }: { name: IconName; size?: number; color?: string; label?: string } = $props()
</script>

<svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
  role={label ? 'img' : undefined} aria-label={label} aria-hidden={label ? undefined : true}>
  {#each ICON[name] as d}<path {d} />{/each}
</svg>

<style>svg { flex: none; display: block; }</style>
