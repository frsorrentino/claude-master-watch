<script lang="ts">
  import type { Inspector } from './tablet'
  import { since } from './durations'
  import { shortModel } from './header'
  import { t } from './t'

  // I dettagli della sessione accanto alle colonne (TabletInspector): la sessione (progetto, account, aperta da, turno),
  // tre numeri (contesto, prompt e commit di oggi), l'obiettivo e la cronologia di oggi (contratto 1.29).
  let { i, quotaH5, loading, timeZone }: { i: Inspector; quotaH5: number | null; loading: boolean; timeZone?: string } = $props()
  const s = $derived(i.session)
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit', timeZone })
  const prio = $derived(s.low_priority?.trim().toLowerCase() === 'active' ? t.tabletPriorityActive : s.low_priority?.trim().toLowerCase() === 'offered' ? t.tabletPriorityOffered : null)
  const toneOf = (kind: string, ok?: boolean | null) => kind === 'test' && ok === false ? 'var(--b-alert)' : kind === 'test' || (kind === 'task' && ok === true) ? 'var(--b-good)' : kind === 'outcome' ? 'var(--b-label)' : 'var(--b-ring)'
</script>

<div class="insp">
  <span class="lab">{t.tabletInspTitle.toUpperCase()}</span>
  <div class="kv"><span>{t.tabletInspProject}</span><span>{s.project}</span></div>
  <div class="kv"><span>{t.tabletInspAccount}</span><span>{[s.account, quotaH5 != null ? t.quota5h(quotaH5) : null].filter(Boolean).join(' · ')}</span></div>
  <div class="kv"><span>{t.tabletInspOpened}</span><span>{hm(i.openedAt)} ({since(i.openedAt, i.openedAt + i.openFor)})</span></div>
  <div class="kv"><span>{t.tabletInspTurn}</span><span>{i.turn != null ? [since(0, i.turn), s.tool].filter(Boolean).join(' · ') : t.tabletInspTurnNone}</span></div>
  {#if shortModel(s.model)}<div class="kv"><span>{t.tabletInspModel}</span><span>{[shortModel(s.model), s.effort].filter(Boolean).join(' · ')}</span></div>{/if}
  {#if prio}<div class="kv"><span>{t.tabletInspPriority}</span><span>{prio}</span></div>{/if}
  <div class="stats">
    <div class="stat"><b>{s.context != null ? `${s.context}%` : '–'}</b><span>{t.tabletInspContext.toUpperCase()}</span></div>
    <div class="stat"><b>{i.prompts ?? '–'}</b><span>{t.tabletInspPrompts.toUpperCase()}</span></div>
    <div class="stat"><b>{i.commits ?? '–'}</b><span>{t.tabletInspCommits.toUpperCase()}</span></div>
  </div>
  {#if s.goal?.text}<span class="lab">{t.goal.toUpperCase()}</span><p class="goal">{s.goal.text}</p>{/if}
  <div class="gh"><span>{t.tabletInspToday.toUpperCase()}</span><i></i></div>
  {#if i.today.length}
    {#each i.today as e}
      {@const tone = toneOf(e.kind, e.ok)}
      <div class="ev"><span class="at">{hm(e.at)}</span><span class="kind" style="color:{tone};border-color:{tone}">{(t.tabletKind[e.kind] ?? e.kind).toUpperCase()}</span><span class="tx">{e.kind === 'commit' && e.ref ? `${e.ref} ${e.text}` : e.text}</span></div>
    {/each}
  {:else}
    <p class="dim">{loading ? t.tabletInspTodayLoading : t.tabletInspTodayNone}</p>
  {/if}
</div>

<style>
  .insp { height: 100%; overflow-y: auto; padding: 16px 18px; display: flex; flex-direction: column; gap: 10px; }
  .lab { font: 11px var(--mono); letter-spacing: .12em; color: var(--text2); }
  .kv { display: flex; gap: 16px; font: 13px var(--mono); color: var(--text2); }
  .kv span:last-child { flex: 1; text-align: right; color: var(--text); overflow-wrap: anywhere; }
  .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding-top: 4px; }
  .stat { border-radius: 12px; background: var(--low); border: 1px solid rgb(255 255 255 / .1); padding: 10px 12px; display: flex; flex-direction: column; }
  .stat b { font: 500 22px var(--mono); }
  .stat span { font: 10px var(--mono); letter-spacing: .06em; color: var(--text2); }
  .goal { font-size: 14px; color: var(--b-label); }
  .gh { display: flex; align-items: center; gap: 10px; margin-top: 8px; font: 11px var(--mono); letter-spacing: .12em; color: var(--text2); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--text2) 40%, transparent); }
  .ev { display: flex; gap: 10px; align-items: flex-start; }
  .at { font: 13px var(--mono); color: var(--text2); padding-top: 2px; }
  .kind { font: 10px var(--mono); letter-spacing: .06em; border: 1px solid; border-radius: 5px; padding: 2px 6px; white-space: nowrap; margin-top: 2px; }
  .tx { flex: 1; font-size: 14px; }
  .dim { font-size: 12.5px; color: var(--text2); }
</style>
