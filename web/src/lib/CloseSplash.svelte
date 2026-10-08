<script lang="ts">
  import { CLOSED_SHOW_MS, type Phase } from './closeSplash'
  import { t } from './t'
  // Il pannello di chiusura sopra la pagina della sessione (CloseSplash.kt; Franz, 08/10 18:29, variante A): la pagina resta
  // sotto, velata. «Si sta chiudendo» con i passi veri; «è chiusa» con l'ora, «Riapri» e «Torna alla home», e dopo 3 s il
  // ritorno da solo, con la barra che si consuma. In una colonna della plancia toglie la colonna.
  let { phase, onHome, onReopen = null, column = false }: { phase: Phase; onHome: () => void; onReopen?: (() => void) | null; column?: boolean } = $props()
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  $effect(() => {
    if (phase.kind !== 'closed') return
    const id = setTimeout(onHome, CLOSED_SHOW_MS)
    return () => clearTimeout(id)
  })
</script>

<div class="veil" role="presentation">
  <div class="card" role="status">
    <svg class="pw" class:off={phase.kind === 'closed'} viewBox="0 0 24 24" width="44" height="44" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 3v9" /><path d="M6.3 6.8a8 8 0 1 0 11.4 0" /></svg>
    {#if phase.kind === 'closing'}
      <h3>{t.splashClosing(phase.name)}</h3>
      <p>{t.splashSentAt(hm(phase.sentAt))}</p>
      <ul>
        <li class:done={phase.delivered}><i>{phase.delivered ? '✓' : '○'}</i>{t.splashDelivered}</li>
        <li><i>○</i>{t.splashConfirmed}</li>
      </ul>
      <div class="bar"><b class="run"></b></div>
      {#if phase.slow}
        <small class="slow">{t.splashSlow}</small>
        <button class="tx" onclick={onHome}>{column ? t.splashDrop : t.splashHome}</button>
      {:else}<small>{t.splashUsually}</small>{/if}
    {:else}
      <h3>{t.splashClosed(phase.name)}</h3>
      <p>{phase.byMe ? t.splashClosedLine(hm(phase.at)) : t.splashClosedOther(hm(phase.at))}</p>
      {#key phase}<div class="bar"><b class="left" style:animation-duration="{CLOSED_SHOW_MS}ms"></b></div>{/key}
      <small>{column ? t.splashDropSoon : t.splashBackSoon}</small>
      <div class="btns">
        {#if onReopen}<button class="tx" onclick={onReopen}>{t.splashReopen}</button>{/if}
        <button class="fill" onclick={onHome}>{column ? t.splashDrop : t.splashHome}</button>
      </div>
    {/if}
  </div>
</div>

<style>
  .veil { position: absolute; inset: 0; z-index: 15; display: grid; place-items: center; padding: 16px; background: rgb(0 0 0 / .62); animation: fade .2s; }
  .card { width: min(420px, 100%); background: var(--surface); border-radius: 28px; padding: 26px 22px 16px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 8px;
    animation: pop .25s cubic-bezier(.2, .8, .2, 1); }
  .pw { color: var(--icon); }
  .pw.off { color: var(--text2); }
  h3 { margin: 0; font-size: 22px; font-weight: 500; }
  p { margin: 0; color: var(--text2); font-size: 14px; }
  ul { list-style: none; margin: 6px 0 0; padding: 0; align-self: stretch; text-align: left; font-size: 14px; color: var(--text2); }
  li { display: flex; gap: 10px; margin: 6px 0; }
  li.done { color: var(--text); }
  li i { font-style: normal; width: 18px; text-align: center; }
  small { font-size: 12px; color: var(--text2); }
  small.slow { color: var(--wait); }
  .bar { align-self: stretch; height: 4px; border-radius: 2px; background: var(--high); overflow: hidden; margin-top: 8px; }
  .bar b { display: block; height: 100%; background: var(--icon); }
  .bar .run { width: 40%; animation: run 1.2s ease-in-out infinite; }
  .bar .left { width: 100%; transform-origin: left; animation: left linear forwards; }
  .btns { display: flex; gap: 8px; margin-top: 4px; }
  button { padding: 10px 18px; border-radius: 999px; font-weight: 500; font-size: 14px; }
  .tx { color: var(--icon); }
  .tx:hover { background: var(--high); }
  .fill { background: #D3E3FD; color: #0A2050; }
  .fill:hover { filter: brightness(1.05); }
  @keyframes run { from { translate: -100% 0; } to { translate: 250% 0; } }
  @keyframes left { to { scale: 0 1; } }
  @keyframes fade { from { opacity: 0; } }
  @keyframes pop { from { opacity: 0; scale: .96; } }
  @media (prefers-reduced-motion: reduce) { .veil, .card, .bar .run { animation: none; } }
</style>
