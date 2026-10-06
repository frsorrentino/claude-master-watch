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
  // Aperta, il modello e il contesto stanno già nella riga sotto la linguetta: qui resta l'ora. Chiusa, l'ora e il contesto,
  // che conta per l'handoff: il modello cambia di rado, e con lui la riga non stava nella larghezza (Franz, 06/10 22:27).
  const label = $derived((expanded ? [t.dockMaster, h?.at ? hm(h.at) : null] : [t.dockMaster, h?.at ? hm(h.at) : null, master.context != null ? t.ctx(master.context) : null]).filter(Boolean).join(' · '))
  // Il badge di stato solo quando la master ti aspetta o è chiusa; altrimenti la scintilla di Claude, che gira mentre lavora.
  // Il cerchio rosso era l'elemento più saturo della schermata e nel Material 3 il rosso è il ruolo «errore» (22:27).
  const attention = $derived(master.state === 'waiting' || master.state === 'gone')
</script>

<div class="dock" class:expanded role="button" tabindex="0" aria-expanded={expanded} onclick={onToggle} onkeydown={(e) => e.key === 'Enter' && onToggle()}>
  {#if attention}<Badge s={master} size={20} />{:else}<svg class="spark lead" class:spin={breathes(master.state)} viewBox="0 0 18 18" role="img" aria-label={master.state}><path d="M11.2 9H17.3M10.56 10.56L14.87 14.87M9 11.2V17.3M7.44 10.56L3.13 14.87M6.8 9H.7M7.44 7.44L3.13 3.13M9 6.8V.7M10.56 7.44L14.87 3.13" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" /></svg>{/if}
  <span class="col">
    <span class="mono">{label}</span>
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
  .dock { display: flex; align-items: center; gap: 12px; padding: 10px 12px 8px 16px; border-radius: 26px 26px 0 0; cursor: pointer; outline: none; background: var(--master-highest); }
  .dock:hover, .dock:focus-visible { filter: brightness(1.08); }
  .spark { flex: none; display: block; width: 20px; height: 20px; color: var(--opus); }
  .spark.spin { animation: spin 2.4s linear infinite; }
  @keyframes spin { to { rotate: 360deg; } }
  @media (prefers-reduced-motion: reduce) { .spark.spin { animation: none; } .ib { transition: none; } }
  .col { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
  .col > * { white-space: nowrap; overflow: hidden; }
  b { font-size: 16px; font-weight: 600; }
  /* Premuti, i tasti cambiano forma come quelli del Material 3 Expressive: da cerchio a quadrato smussato, con la molla. */
  .ib { flex: none; width: 44px; height: 44px; border-radius: 22px; display: grid; place-items: center; background: var(--master-key); color: var(--text);
    transition: border-radius .35s var(--spring); }
  .ib:active { border-radius: 12px; }
  .ib:hover { filter: brightness(1.15); }
</style>
