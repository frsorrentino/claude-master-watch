<script lang="ts">
  import type { AgendaPage } from './contract'
  import type { RecapAction } from './recapActions'
  import { agendaModel, SCOPES, todayIso, type Edit } from './recapAgenda'
  import AgendaActions from './AgendaActions.svelte'
  import { t } from './t'
  import AgendaItem from './AgendaItem.svelte'
  import RecapSend from './RecapSend.svelte'
  // La pagina Recap (tavola 2 del mockup approvato l'08/10): dal menu ≡ o da «Tutto il recap». Filtri per ambito in cima;
  // le Azioni; poi chi deve muoversi; in fondo, richiuso, fatto e sospeso. `agenda` null = in arrivo.
  let { actions, day, agenda, error, loading, onSend, canWrite = false, onTalk = () => {}, onEdit = () => {}, today = todayIso() }: {
    actions: RecapAction[]; day: string; agenda: AgendaPage | null; error: string | null; loading: boolean; onSend: (a: RecapAction) => void
    /** Le Azioni sulle schede (piano del 09/10): scritture con l'op del contratto 1.47, «Parlane» apre la master. */
    canWrite?: boolean; onTalk?: (text: string) => void; onEdit?: (e: Edit) => void; today?: string
  } = $props()
  let acts: AgendaActions | undefined = $state()
  let scope = $state<string | null>(null)
  let restOpen = $state(false)
  let asking = $state<RecapAction | null>(null)
  let sent = $state(new Set<string>())
  const key = (a: RecapAction) => `${a.to}\n${a.send}`
  const m = $derived(agendaModel(agenda, scope, today))
  const scopeLabel: Record<string, string> = { agenzia: t.recapScopeAgency, personale: t.recapScopePersonal, postazione: t.recapScopeDesk }
</script>

<div class="recap">
  <div class="filt">
    <button class:on={scope === null} onclick={() => (scope = null)}>{t.recapFilterAll}</button>
    {#each SCOPES as sc}<button class:on={scope === sc} onclick={() => (scope = sc)}>{scopeLabel[sc]}</button>{/each}
  </div>
  {#if actions.length}
    <h2 class="gh" style="--t:var(--text)"><span>{t.recapActions(actions.length).toUpperCase()}</span><i></i></h2>
    <div class="chips">
      {#each actions as a (key(a))}
        {@const done = sent.has(key(a))}
        <button class="act" class:sent={done} disabled={done} onclick={() => (asking = a)}><span>{a.text}</span><small>{a.recap ? t.recapFrom(day) : a.from}</small></button>
      {/each}
    </div>
  {/if}
  {#if error}<p class="note">{error}</p>{:else if !agenda && loading}<p class="note">{t.recapLoading}</p>{/if}
  {#if m.you.length}
    <h2 class="gh" style="--t:var(--b-warn)"><span>{t.recapYou(m.you.length).toUpperCase()}</span><i></i></h2>
    {#each m.you as r}<AgendaItem row={r} onOpen={() => acts?.deepen(r)} onActions={() => acts?.menuOf(r)} />{/each}
  {/if}
  {#if m.claude.length}
    <h2 class="gh" style="--t:var(--icon)"><span>{t.recapClaude(m.claude.length).toUpperCase()}</span><i></i></h2>
    {#each m.claude as r}<AgendaItem row={r} onOpen={() => acts?.deepen(r)} onActions={() => acts?.menuOf(r)} />{/each}
  {/if}
  {#if m.other.length}
    <h2 class="gh" style="--t:var(--text2)"><span>{t.recapOther(m.other.length).toUpperCase()}</span><i></i></h2>
    {#each m.other as r}<AgendaItem row={r} other onOpen={() => acts?.deepen(r)} onActions={() => acts?.menuOf(r)} />{/each}
  {/if}
  {#if m.rest.length}
    <button class="fold" aria-expanded={restOpen} onclick={() => (restOpen = !restOpen)}>
      <span>{t.recapRest(m.rest.length)}</span>
      <svg viewBox="0 0 24 24" width="20" height="20"><path d={restOpen ? 'M6 15l6-6 6 6' : 'M6 9l6 6 6-6'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
    {#if restOpen}{#each m.rest as r}<AgendaItem row={r} onOpen={() => acts?.deepen(r)} onActions={() => acts?.menuOf(r)} />{/each}{/if}
  {/if}
  {#if agenda && !actions.length && !m.you.length && !m.claude.length && !m.other.length && !m.rest.length}<p class="note">{t.recapEmpty}</p>{/if}
</div>

<AgendaActions bind:this={acts} {canWrite} {today} {onSend} {onTalk} {onEdit} />
<RecapSend action={asking} {day} onClose={() => (asking = null)} onSend={(a) => { sent = new Set([...sent, key(a)]); onSend(a) }} />

<style>
  .recap { padding: 0 16px 24px; display: flex; flex-direction: column; gap: 8px; }
  .filt { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; }
  .filt button { border: 1px solid var(--line); border-radius: 999px; padding: 7px 14px; font-size: 14px; color: var(--text2); white-space: nowrap; }
  .filt button.on { background: var(--primary); color: var(--on-primary); border-color: transparent; }
  .gh { display: flex; align-items: center; gap: 8px; margin: 12px 4px 0; font: 500 12px/1.4 var(--mono); letter-spacing: .08em; color: var(--t); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--t) 25%, transparent); }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; }
  .act { display: inline-flex; align-items: baseline; gap: 8px; border-radius: 999px; padding: 10px 16px; background: var(--low); color: var(--text); font-size: 15px; text-align: left; }
  .act small { font-size: 12.5px; color: var(--text2); white-space: nowrap; }
  .act.sent { background: color-mix(in srgb, var(--good) 14%, transparent); color: var(--text2); cursor: default; }
  .note { color: var(--text2); font-size: 14px; padding: 8px 4px; }
  .fold { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding: 12px 4px; border-top: 1px solid var(--line); color: var(--text2); font-size: 15px; text-align: left; }
</style>
