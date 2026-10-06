<script lang="ts">
  import type { Session } from './contract'
  import { hero as heroOf, type Entry } from './masterHome'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import { breathes } from './badge'
  // La master nella home (MasterDock.kt): ridotta è la barra agganciata in fondo, «MASTER · ora · modello · contesto» e il
  // titolo dell'ultimo esito, ▶ per ascoltarlo e ▲ per espanderla; espansa la stessa barra sta in cima con ▼ e la riduce.
  let { master, entries, onToggle, onSpeak, expanded = false }: {
    master: Session; entries: Entry[]; onToggle: () => void; onSpeak: (text: string) => void; expanded?: boolean
  } = $props()
  const h = $derived(heroOf(entries, master))
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const label = $derived([t.dockMaster, h?.at ? hm(h.at) : null, master.model?.label, master.context != null ? t.ctx(master.context) : null].filter(Boolean).join(' · '))
</script>

<div class="dock" class:expanded role="button" tabindex="0" aria-expanded={expanded} onclick={onToggle} onkeydown={(e) => e.key === 'Enter' && onToggle()}>
  <Badge s={master} size={20} />
  <span class="col">
    <!-- La scintilla di Claude, l'unico colore della master: gira mentre lavora o aspetta un permesso, come il badge che respira. -->
    <span class="mono"><svg class="spark" class:spin={breathes(master.state)} viewBox="0 0 18 18" aria-hidden="true"><path d="M11.2 9H17.3M10.56 10.56L14.87 14.87M9 11.2V17.3M7.44 10.56L3.13 14.87M6.8 9H.7M7.44 7.44L3.13 3.13M9 6.8V.7M10.56 7.44L14.87 3.13" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" /></svg>{label}</span>
    {#if h}<b>{h.headline.replace(/\*\*|__|`/g, '')}</b>{/if}
  </span>
  {#if h}
    <button class="ib" aria-label={t.dockListen} onclick={(e) => { e.stopPropagation(); onSpeak([h.headline, h.body].filter(Boolean).join('\n')) }}>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>
    </button>
  {/if}
  <button class="ib" aria-label={expanded ? t.dockCollapse : t.dockConversation} title={expanded ? t.dockCollapse : t.dockConversation} onclick={(e) => { e.stopPropagation(); onToggle() }}>
    <svg viewBox="0 0 24 24" width="22" height="22"><path d={expanded ? 'M7 10l5 5 5-5' : 'M7 14l5-5 5 5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </button>
</div>

<style>
  /* La master è la superficie più alta dell'app, velata del celeste d'accento, piatta come il resto (Celeste velato, Franz
     06/10 21:23). Aperta resta una linguetta col verso di quando è chiusa, angoli tondi in alto (08:48). */
  .dock { display: flex; align-items: center; gap: 12px; padding: 10px 12px 8px 16px; border-radius: 26px 26px 0 0; cursor: pointer; outline: none; background: var(--master-tab); }
  .dock:hover, .dock:focus-visible { filter: brightness(1.08); }
  .spark { display: inline-block; width: 11px; height: 11px; margin: 0 7px 0 1px; vertical-align: -1px; color: var(--opus); }
  .spark.spin { animation: spin 2.4s linear infinite; }
  @keyframes spin { to { rotate: 360deg; } }
  @media (prefers-reduced-motion: reduce) { .spark.spin { animation: none; } }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .col > * { white-space: nowrap; overflow: hidden; }
  b { font-size: 16px; font-weight: 600; }
  .ib { flex: none; width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; background: var(--master-disc); color: var(--text); }
  .ib:hover { filter: brightness(1.15); }
</style>
