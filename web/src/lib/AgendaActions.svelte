<script lang="ts">
  import type { AgendaRow } from './contract'
  import type { RecapAction } from './recapActions'
  import { deepenText, doIt, fileText, menu, mark, nextWeek, talkText, tomorrow, url, type Edit, type Item } from './recapAgenda'
  import { untilLabel } from './agendaDates'
  import { MASTER } from './summary'
  import { t } from './t'
  import RecapSend from './RecapSend.svelte'
  // Le Azioni sulle schede del Recap (piano approvato da Franz il 09/10, «Approvo, prosegui»), come AgendaActions.kt: il
  // menu, «Approfondisci» col dettaglio salvato, la scelta del giorno di «Rimanda», la conferma di ogni testo che parte e
  // di ogni scrittura nell'agenda. Il genitore chiama `menuOf(row)` e `deepen(row)`.
  let { canWrite, today, onSend, onTalk, onEdit, onSpeak = null }: {
    canWrite: boolean; today: string; onSend: (a: RecapAction) => void; onTalk: (text: string) => void; onEdit: (e: Edit) => void
    /** ▶ sul dettaglio (Franz, 09/10 16:31: «serve tasto play di lettura anche per le schede»). */
    onSpeak?: ((text: string) => void) | null
  } = $props()
  let menuFor = $state<AgendaRow | null>(null)
  let deepenFor = $state<AgendaRow | null>(null)
  let postponeFor = $state<AgendaRow | null>(null)
  let pickDate = $state('')
  let send = $state<RecapAction | null>(null)
  let edit = $state<Edit | null>(null)
  export function menuOf(r: AgendaRow) { menuFor = r }
  export function deepen(r: AgendaRow) { deepenFor = r }

  const toMaster = (r: AgendaRow, text: string): RecapAction => ({ text: r.title, send: text, to: MASTER, from: 'agenda', viaMaster: true, recap: false, agenda: true, note: t.agendaFromScope(r.scope.trim()) })
  const label: Record<Item, string> = {
    deepen: t.agendaDeepen, do: t.recapDo, talk: t.agendaTalk, open_ref: t.agendaOpenRef, done: t.agendaDone, postpone: t.agendaPostpone,
    pass_claude: t.agendaPassClaude, pass_me: t.agendaPassMe, remove: t.agendaRemove,
  }
  const icon: Record<Item, string> = {
    deepen: 'M12 8h.01M11 12h1v5h1M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z', do: 'M8 5v14l11-7z', talk: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z',
    open_ref: 'M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6', done: 'M9 12l2 2 4-4M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z',
    postpone: 'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', pass_claude: 'M12 8V4H8M4 8h16v12H4zM9 13h.01M15 13h.01',
    pass_me: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z', remove: 'M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14',
  }
  function pick(r: AgendaRow, item: Item) {
    menuFor = null
    if (item === 'deepen') deepenFor = r
    else if (item === 'do') send = doIt(r, t.recapDoText)
    else if (item === 'talk') onTalk(talkText(r))
    else if (item === 'open_ref') { const u = url(r.ref); if (u) window.open(u, '_blank', 'noopener'); else send = toMaster(r, fileText(r)) }
    else if (item === 'postpone') { postponeFor = r; pickDate = tomorrow(today) }
    else edit = { item, row: r }
  }
  const editText = (e: Edit) => ({
    done: [t.agendaDoneTitle, t.agendaDoneChange, t.agendaDone],
    postpone: [t.agendaPostponeConfirm, t.agendaPostponeChange(untilLabel(e.until)), t.agendaPostpone],
    remove: [t.agendaRemoveTitle, t.agendaRemoveChange, t.agendaRemove],
    pass_claude: [t.agendaPassClaudeTitle, t.agendaPassChange('claude'), t.agendaPass],
    pass_me: [t.agendaPassMeTitle, t.agendaPassChange('franz'), t.agendaPass],
  })[e.item]

  // Nei gestori: prima il foglio nuovo, poi si chiude il vecchio. `{@const r = …}` segue il suo stato: chiuso prima, `r`
  // diventa null e il foglio dopo non parte («Chiedilo alla master» non apriva la conferma, prova del 09/10).
  // Un <dialog> per foglio: si apre quando il suo stato arriva, Esc o il fondo lo chiudono.
  function modal(node: HTMLDialogElement, close: () => void) {
    node.showModal()
    // Il focus sul foglio, non sul primo tasto: aperto col dito, nessun tasto deve sembrare già scelto.
    node.tabIndex = -1
    node.focus()
    const onClose = () => close()
    node.addEventListener('close', onClose)
    return { destroy() { node.removeEventListener('close', onClose); if (node.open) node.close() } }
  }
  const backdrop = (e: MouseEvent) => e.target === e.currentTarget && (e.currentTarget as HTMLDialogElement).close()
</script>

{#if menuFor}
  {@const r = menuFor}
  <dialog use:modal={() => (menuFor = null)} onclick={backdrop}>
    <div class="head"><i class="mark {mark(r.scope)}"></i><h3>{r.title}</h3></div>
    {#each menu(r, canWrite, today) as item, i}
      {#if i > 0 && item === 'done'}<hr />{/if}
      <button class="item" class:danger={item === 'remove'} onclick={() => pick(r, item)}>
        <svg viewBox="0 0 24 24" width="22" height="22"><path d={icon[item]} fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
        <span>{label[item]}</span>
      </button>
    {/each}
  </dialog>
{/if}

{#if deepenFor}
  {@const r = deepenFor}
  {@const u = url(r.ref ?? '')}
  <dialog class="tall" use:modal={() => (deepenFor = null)} onclick={backdrop}>
    <div class="head"><i class="mark {mark(r.scope)}"></i><h2>{r.title}</h2></div>
    <p class="sub">{[r.scope, r.state, r.blocks && t.recapBlocks(r.blocks.trim()), r.until && t.agendaUntil(untilLabel(r.until))].filter(Boolean).join(' · ')}</p>
    {#if (r.ref ?? '').trim()}{#if u}<a class="ref" href={u} target="_blank" rel="noopener">{r.ref.trim()}</a>{:else}<p class="sub">{r.ref.trim()}</p>{/if}{/if}
    {#if (r.detail ?? '').trim()}
      <div class="detail"><p>{r.detail!.trim()}</p>
        {#if onSpeak}<button class="play" aria-label={t.readAloud} title={t.readAloud} onclick={() => onSpeak!(`${r.title.trim()}.\n${r.detail!.trim()}`)}>
          <svg viewBox="0 0 24 24" width="22" height="22"><path d="M8 5v14l11-7z" fill="currentColor" /></svg></button>{/if}
      </div>
    {:else}<p class="none">{t.agendaNoDetail}</p>{/if}
    <div class="btns">
      <button class="text" onclick={() => { menuFor = r; deepenFor = null }}>{t.agendaActions}</button>
      {#if !(r.detail ?? '').trim()}<button class="filled" onclick={() => { send = toMaster(r, deepenText(r)); deepenFor = null }}>{t.agendaAskDetail}</button>{/if}
    </div>
  </dialog>
{/if}

{#if postponeFor}
  {@const r = postponeFor}
  <dialog use:modal={() => (postponeFor = null)} onclick={backdrop}>
    <h3>{t.agendaPostponeTitle}</h3>
    <button class="item" onclick={() => { edit = { item: 'postpone', row: r, until: tomorrow(today) }; postponeFor = null }}><span>{t.agendaPostponeTomorrow(untilLabel(tomorrow(today)))}</span></button>
    <button class="item" onclick={() => { edit = { item: 'postpone', row: r, until: nextWeek(today) }; postponeFor = null }}><span>{t.agendaPostponeWeek(untilLabel(nextWeek(today)))}</span></button>
    <label class="item date"><span>{t.agendaPostponeDate}</span><input type="date" min={tomorrow(today)} bind:value={pickDate} /></label>
    <div class="btns">
      <button class="text" onclick={() => (postponeFor = null)}>{t.cancel}</button>
      <button class="filled" disabled={!pickDate || pickDate <= today} onclick={() => { edit = { item: 'postpone', row: r, until: pickDate }; postponeFor = null }}>{t.agendaPostpone}</button>
    </div>
  </dialog>
{/if}

{#if edit}
  {@const e = edit}
  {@const [title, change, button] = editText(e)}
  <dialog use:modal={() => (edit = null)} onclick={backdrop}>
    <h3>{title}</h3>
    <p class="sub">{t.agendaFromScope(e.row.scope.trim())}</p>
    <p class="what">{e.row.title}</p>
    <p class="sub">{change}</p>
    <div class="btns">
      <button class="text" onclick={() => (edit = null)}>{t.cancel}</button>
      <button class="filled" class:danger={e.item === 'remove'} onclick={() => { onEdit(e); edit = null }}>{button}</button>
    </div>
  </dialog>
{/if}

<RecapSend action={send} day="" onClose={() => (send = null)} onSend={(a) => onSend(a)} />

<style>
  dialog { margin: auto; border: 0; outline: none; color: var(--text); background: var(--surface); padding: 20px 0 16px; width: min(440px, 92vw); border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  @media (max-width: 600px) { dialog { margin: auto 0 0; width: 100%; max-width: none; border-radius: 28px 28px 0 0; } }
  dialog.tall { padding: 22px 0 18px; max-height: 88vh; overflow-y: auto; }
  .head { display: flex; gap: 12px; align-items: flex-start; padding: 0 24px 10px; }
  h2 { font-size: 22px; font-weight: 500; }
  h3 { font-size: 18px; font-weight: 500; padding: 0 24px 8px; }
  .head h3 { padding: 0; }
  .sub { color: var(--text2); font-size: 14px; padding: 4px 24px 0; }
  .ref { display: block; color: var(--icon); font-size: 14px; padding: 6px 24px 0; overflow-wrap: anywhere; }
  .detail { margin: 14px 24px 0; padding: 14px 8px 14px 18px; border-radius: 16px; background: var(--low); font-size: 16px; display: flex; gap: 6px; align-items: flex-start; }
  .detail p { flex: 1; white-space: pre-wrap; }
  .play { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; color: var(--icon); flex: none; }
  .play:hover { background: var(--high); }
  .none { color: var(--text2); font-size: 16px; padding: 14px 24px 0; }
  .what { margin: 14px 24px 0; padding: 14px 18px; border-radius: 16px; background: var(--low); font-size: 16px; font-weight: 500; }
  .item { display: flex; align-items: center; gap: 16px; width: 100%; padding: 14px 24px; font-size: 16px; color: var(--text); text-align: left; }
  .item svg { color: var(--icon); flex: none; }
  .item:hover, .item:focus-visible { background: var(--high); outline: none; }
  .item.danger svg { color: var(--gone); }
  .date { justify-content: space-between; }
  .date input { background: var(--high); color: var(--text); border: 0; border-radius: 12px; padding: 8px 10px; font: inherit; color-scheme: dark; }
  hr { border: 0; border-top: 1px solid var(--line); margin: 4px 24px; }
  .mark { width: 18px; height: 18px; flex: none; margin-top: 4px; border: 2px solid var(--text2); }
  .mark.personal { border-radius: 50%; } .mark.agency { border-radius: 5px; } .mark.desk { border-radius: 3px; transform: rotate(45deg) scale(.8); } .mark.none { border-color: transparent; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; padding: 0 24px; }
  .btns button { padding: 10px 18px; border-radius: 20px; font-weight: 500; }
  .text { color: var(--icon); }
  .filled { background: var(--primary); color: var(--on-primary); }
  .filled.danger { background: var(--gone); color: #fff; }
  .filled:disabled { opacity: .45; }
</style>
