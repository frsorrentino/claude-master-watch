<script lang="ts">
  import type { Session } from './contract'
  import { hero as heroOf, type Entry } from './masterHome'
  import { t } from './t'
  import Badge from './Badge.svelte'
  // La master nella home (MasterDock.kt): la barra agganciata in fondo, «MASTER · ora · modello · contesto» e il titolo
  // dell'ultimo esito, ▶ per ascoltarlo e ▲ per aprire la sua casa.
  let { master, entries, onOpen, onSpeak }: { master: Session; entries: Entry[]; onOpen: () => void; onSpeak: (text: string) => void } = $props()
  const h = $derived(heroOf(entries, master))
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const label = $derived([t.dockMaster, h?.at ? hm(h.at) : null, master.model?.label, master.context != null ? t.ctx(master.context) : null].filter(Boolean).join(' · '))
</script>

<div class="dock" role="button" tabindex="0" onclick={onOpen} onkeydown={(e) => e.key === 'Enter' && onOpen()}>
  <Badge s={master} size={20} />
  <span class="col">
    <span class="mono">{label}</span>
    {#if h}<b>{h.headline.replace(/\*\*|__|`/g, '')}</b>{/if}
  </span>
  {#if h}
    <button class="ib" aria-label={t.listen} onclick={(e) => { e.stopPropagation(); onSpeak([h.headline, h.body].filter(Boolean).join('\n')) }}>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>
    </button>
  {/if}
  <button class="ib" aria-label={t.openConversation} onclick={(e) => { e.stopPropagation(); onOpen() }}>
    <svg viewBox="0 0 24 24" width="22" height="22"><path d="M7 14l5-5 5 5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </button>
</div>

<style>
  .dock { display: flex; align-items: center; gap: 12px; padding: 10px 12px 8px 16px; background: var(--low); border-radius: 26px 26px 0 0; border-top: 1px solid var(--line); cursor: pointer; outline: none; }
  .dock:hover, .dock:focus-visible { filter: brightness(1.08); }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .col > * { white-space: nowrap; overflow: hidden; }
  b { font-size: 16px; font-weight: 600; }
  .ib { flex: none; width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; background: var(--high); color: var(--icon); }
  .ib:hover { filter: brightness(1.15); }
</style>
