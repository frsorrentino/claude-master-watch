<script lang="ts">
  import { getContext } from 'svelte'
  import type { AgendaRow } from './contract'
  import type { AgendaNight } from './agendaNight'
  import { isOpen, mark, menu, type Item } from './recapAgenda'
  import { untilLabel } from './agendaDates'
  import { t } from './t'
  // Una riga dell'agenda (tavole 1 e 2 del mockup): segno, titolo, sotto il rimando — o «blocca: …» se è ferma su altro, o
  // lo stato se non è aperta. Come le card delle sessioni (Franz, 10/10 09:24, variante A, al posto del tasto «Azioni»): ˅ la
  // apre sul posto, con il dettaglio salvato, le azioni come pillole («Fallo» piena) e il riferimento; il tocco sul titolo
  // apre «Approfondisci» (`onOpen`). Senza `onItem` niente ˅.
  let { row, other = false, onOpen = null, onItem = null, canWrite = false, today = null }: {
    row: AgendaRow; other?: boolean; onOpen?: (() => void) | null; onItem?: ((item: Item) => void) | null
    canWrite?: boolean; today?: string | null
  } = $props()
  const night = getContext<AgendaNight | undefined>('agendaNight')
  const items = $derived(onItem ? menu(row, canWrite, today, !!night?.can) : [])
  // I tasti (Franz, 10/10 10:36, variante A + C): una barra di icone con l'etichetta sotto, uguali, «Fallo» pieno, per le
  // azioni più usate; «Altro» apre qui sotto l'elenco delle altre. Senza «Fallo» la barra comincia da quello che c'è.
  const bar = $derived((['do', 'night', 'talk', 'done'] as Item[]).filter(i => items.includes(i)))
  const rest = $derived(items.filter(i => i !== 'deepen' && !bar.includes(i)))
  let open = $state(false)
  let more = $state(false)
  const icon: Record<Item, string> = {
    deepen: 'M12 8h.01M11 12h1v5h1M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z', do: 'M8 5v14l11-7z', night: 'M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z',
    talk: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z', open_ref: 'M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6',
    done: 'M5 12.5l4.5 4.5L19 7.5', postpone: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 7.5V12l3 2',
    pass_claude: 'M12 8V4H8M4 8h16v12H4zM9 13h.01M15 13h.01', pass_me: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z', remove: 'M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14',
  }
  const label: Record<Item, string> = {
    deepen: t.agendaDeepen, do: t.recapDo, night: t.agendaNight, talk: t.agendaTalk, open_ref: t.agendaOpenRef, done: t.agendaDone,
    postpone: t.agendaPostpone, pass_claude: t.agendaPassClaude, pass_me: t.agendaPassMe, remove: t.agendaRemove,
  }
  const sub = $derived(row.state.trim().toLowerCase() === 'sospeso' && row.until ? t.agendaUntil(untilLabel(row.until))
    : !isOpen(row) ? [row.state.trim(), (row.ref ?? '').trim()].filter(Boolean).join(' · ') : other ? t.recapBlocks(row.blocks.trim()) : (row.ref ?? '').trim())
  const detail = $derived((row.detail ?? '').trim())
</script>

<div class="card">
  <div class="head">
    <i class="mark {mark(row.scope)}"></i>
    <!-- svelte-ignore a11y_no_noninteractive_tabindex -->
    <div class="t" class:tap={!!onOpen} role={onOpen ? 'button' : undefined} tabindex={onOpen ? 0 : undefined}
      onclick={() => onOpen?.()} onkeydown={(e) => onOpen && (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), onOpen())}>
      <b>{row.title}</b>{#if sub}<small>{sub}</small>{/if}
    </div>
    {#if items.length}
      <button class="chev" aria-expanded={open} aria-label={open ? t.agendaCardClose : t.agendaCardOpen} title={open ? t.agendaCardClose : t.agendaCardOpen} onclick={() => (open = !open)}>
        <svg viewBox="0 0 24 24" width="22" height="22"><path d={open ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
      </button>
    {/if}
  </div>
  {#if open && items.length}
    <div class="more">
      <p class:none={!detail}>{detail || t.agendaNoDetail}</p>
      <div class="bar">
        {#each bar as item}
          <button class="key" class:do={item === 'do'} onclick={() => onItem?.(item)}>
            <i><svg viewBox="0 0 24 24" width="24" height="24"><path d={item === 'done' ? 'M5 12.5l4.5 4.5L19 7.5' : icon[item]} fill={item === 'do' ? 'currentColor' : 'none'} stroke="currentColor" stroke-width={item === 'do' ? 0 : 2.2} stroke-linecap="round" stroke-linejoin="round" /></svg></i>
            <span>{item === 'talk' ? t.agendaTalkShort : label[item]}</span>
          </button>
        {/each}
        {#if rest.length}
          <button class="key" class:lit={more} aria-expanded={more} onclick={() => (more = !more)}>
            <i><svg viewBox="0 0 24 24" width="24" height="24"><circle cx="5" cy="12" r="2" fill="currentColor" /><circle cx="12" cy="12" r="2" fill="currentColor" /><circle cx="19" cy="12" r="2" fill="currentColor" /></svg></i>
            <span>{t.agendaMoreShort}</span>
          </button>
        {/if}
        {#each { length: Math.max(0, 5 - bar.length - (rest.length ? 1 : 0)) } as _}<span class="key"></span>{/each}
      </div>
      {#if rest.length && more}
        <div class="list">
          {#each rest as item}
            <button class:danger={item === 'remove'} onclick={() => onItem?.(item)}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d={icon[item]} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{label[item]}
            </button>
          {/each}
        </div>
      {/if}
    </div>
  {/if}
</div>

<style>
  .card { background: var(--surface); border-radius: 18px; padding: 12px 6px 12px 14px; }
  .head { display: flex; gap: 12px; align-items: flex-start; }
  .t { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; border-radius: 10px; }
  .t.tap { cursor: pointer; }
  .t.tap:hover { box-shadow: 0 0 0 4px var(--high); background: var(--high); }
  .t b { font-weight: 500; font-size: 16px; overflow-wrap: anywhere; }
  .t small { color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  /* Tondo personale, quadrato agenzia, rombo postazione. */
  .mark { width: 18px; height: 18px; flex: none; margin-top: 3px; border: 2px solid var(--text2); }
  .mark.personal { border-radius: 50%; }
  .mark.agency { border-radius: 5px; }
  .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); }
  .mark.none { border-color: transparent; }
  .chev { flex: none; width: 40px; height: 40px; margin: -8px 0 0; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .chev:hover { background: var(--high); }
  .more { display: flex; flex-direction: column; gap: 12px; padding: 10px 10px 0 0; margin-top: 10px; border-top: 1px solid var(--line); }
  .more p { margin: 0; font-size: 15px; white-space: pre-line; }
  .more p.none { color: var(--text2); }
  .bar { display: grid; grid-template-columns: repeat(5, 1fr); }
  .key { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 4px 0; border-radius: 16px; color: var(--text); font-size: 12.5px; }
  .key i { width: 48px; height: 48px; border-radius: 16px; background: var(--high); color: var(--icon); display: grid; place-items: center; }
  .key.do i { background: var(--primary); color: var(--on-primary); }
  .key.lit i { background: #3A4A66; color: var(--primary); }
  button.key:hover i { filter: brightness(1.12); }
  .list { display: flex; flex-direction: column; background: var(--low); border-radius: 16px; padding: 6px 0; }
  .list button { display: flex; align-items: center; gap: 14px; padding: 12px 16px; font-size: 16px; text-align: left; color: var(--text); }
  .list button svg { color: var(--icon); flex: none; }
  .list button.danger, .list button.danger svg { color: var(--gone); }
  .list button:hover { background: var(--high); }
</style>
