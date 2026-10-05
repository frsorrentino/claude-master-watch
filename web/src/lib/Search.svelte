<script lang="ts">
  import type { Event, SearchPage } from './contract'
  import type { Sent } from './chatRules'
  import { find, withConversations, type Hit } from './search'
  import { t } from './t'

  // Cerca (SearchScreen dell'app): un campo e i risultati, ognuno con sessione, ora e la riga trovata con la parte cercata
  // in grassetto. Con il relay (contratto 1.27) cerca anche nelle conversazioni di tutte le sessioni degli ultimi 7 giorni,
  // partendo da sola quando smetti di scrivere; un evento senza sessione apre il Registro.
  let { sent, events, remote, onQuery, page = null, loading = false, known, onOpen, timeZone }: {
    sent: Sent[]; events: Event[]; remote: boolean; onQuery: (q: string) => void; page?: SearchPage | null; loading?: boolean
    known: Set<string>; onOpen: (session: string | null) => void; timeZone?: string
  } = $props()

  let query = $state('')
  const local = $derived(find(query, sent, events))
  const hits = $derived<Hit[]>(remote && page ? withConversations(local, page) : local)
  let field: HTMLInputElement | undefined = $state()
  $effect(() => { field?.focus() })
  // Quando smetti di scrivere: ogni ricerca costa al relay da mezzo secondo a dieci.
  $effect(() => {
    const q = query.trim()
    if (!remote || q.length < 2) return
    const id = setTimeout(() => onQuery(q), 700)
    return () => clearTimeout(id)
  })
  const when = (at: number) => new Date(at * 1000).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', timeZone }).replace(',', '')
</script>

<div class="search">
  <label class="field">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round"><path d="m21 21-4.34-4.34" /><circle cx="11" cy="11" r="8" /></svg>
    <input bind:this={field} bind:value={query} placeholder={remote ? t.searchHintAll : t.searchHint} aria-label={t.search} />
  </label>
  {#if loading}<div class="bar"><i></i></div>{/if}
  {#if query.trim() && !hits.length && !loading}<p class="none">{t.searchNone}</p>{/if}
  <div class="hits">
    {#each hits as h (h.ref)}
      {@const canOpen = h.kind !== 'conversation' || h.live === true || (h.session != null && known.has(h.session))}
      <svelte:element this={canOpen ? 'button' : 'div'} class="hit" class:open={canOpen} role={canOpen ? undefined : 'group'} onclick={canOpen ? () => onOpen(h.session) : undefined}>
        <span class="top">
          <span class="who" class:link={canOpen}>{h.session ?? t.menuRegister}</span>
          {#if h.live === false}<span class="closed">{t.searchClosed}</span>{/if}
          <span class="at">{when(h.at)}</span>
        </span>
        <span class="line">{h.line.slice(0, h.start)}<b>{h.line.slice(h.start, h.end)}</b>{h.line.slice(h.end)}</span>
      </svelte:element>
    {/each}
    {#if remote && page?.more}<p class="more">{t.searchMore}</p>{/if}
  </div>
</div>

<style>
  .search { display: flex; flex-direction: column; min-height: 100%; max-width: 760px; }
  .field { display: flex; align-items: center; gap: 12px; margin: 12px 16px; padding: 0 18px; min-height: 56px; border: 1px solid rgb(255 255 255 / .25); border-radius: 28px; }
  .field:focus-within { border-color: var(--icon); border-width: 2px; padding: 0 17px; }
  input { flex: 1; font: inherit; font-size: 16px; color: var(--text); background: none; border: 0; outline: 0; }
  .bar { height: 4px; margin: 0 20px; border-radius: 2px; background: var(--b-track); overflow: hidden; }
  .bar i { display: block; height: 100%; width: 35%; background: var(--icon); animation: run 1.2s ease-in-out infinite; }
  @keyframes run { from { translate: -100% 0; } to { translate: 300% 0; } }
  .none { color: var(--text2); padding: 0 20px; }
  .hits { display: flex; flex-direction: column; padding: 4px 8px 24px; }
  .hit { display: flex; flex-direction: column; gap: 2px; padding: 10px 12px; text-align: left; border-radius: 12px; }
  .hit.open:hover { background: var(--surface); }
  .top { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 500; }
  .who { color: var(--text2); white-space: nowrap; overflow: hidden; }
  .who.link { color: var(--icon); }
  .closed { font-size: 12.5px; color: var(--text2); }
  .at { margin-left: auto; font-size: 12.5px; color: var(--text2); white-space: nowrap; }
  .line { font-size: 14.5px; color: var(--text2); }
  .line b { color: var(--text); font-weight: 700; }
  .more { font-size: 13px; color: var(--text2); padding: 12px; }
  @media (prefers-reduced-motion: reduce) { .bar i { animation: none; } }
</style>
