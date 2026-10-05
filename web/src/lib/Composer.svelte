<script lang="ts">
  import type { Session, State } from './contract'
  import { mode as modeOf, slashConfirm, slashParse, slashSuggest } from './composer'
  import { textArg } from './questionRules'
  import { t } from './t'
  import Icon from './Icon.svelte'

  // La barra di scrittura (Composer dell'app): + per allegati (e le azioni ricorrenti nella master), il suggerimento
  // attenuato nel campo vuoto con «Usa», i comandi slash scrivendo «/», a destra Invia, Invia tonale o Stop.
  let { st, s, draft = $bindable(), field, toMaster, onSend, onAnswerText, onSlash, onStop, onReopen, onAttach, onRecurring }: {
    st: State; s: Session; draft: string; field: string | null; toMaster: boolean
    onSend: (text: string) => void; onAnswerText: (arg: string) => void; onSlash: (cmd: string, args: string | null) => void
    onStop: () => void; onReopen: () => void; onAttach: (files: File[], text: string) => void; onRecurring: (() => void) | null
  } = $props()

  const MAX_FILES = 5
  let files = $state<{ file: File; url: string | null }[]>([])
  const md = $derived(files.length && modeOf(s, draft, st.ops) !== 'reopen' ? (s.question?.options.length ? 'send_tonal' : 'send') : modeOf(s, draft, st.ops))
  const hints = $derived(s.question ? [] : slashSuggest(draft, st.slash))
  const canFiles = $derived(!!st.share?.any)
  let confirm = $state<{ cmd: string; args: string | null } | null>(null)
  let confirmDlg: HTMLDialogElement | undefined = $state()
  let menu = $state(false)
  let images: HTMLInputElement | undefined = $state()
  let camera: HTMLInputElement | undefined = $state()
  let any: HTMLInputElement | undefined = $state()
  let area: HTMLTextAreaElement | undefined = $state()

  function picked(e: Event) {
    const list = [...((e.currentTarget as HTMLInputElement).files ?? [])]
    files = [...files, ...list.map(file => ({ file, url: file.type.startsWith('image/') ? URL.createObjectURL(file) : null }))].slice(0, MAX_FILES)
    ;(e.currentTarget as HTMLInputElement).value = ''
  }
  function drop(i: number) { const f = files[i]; if (f.url) URL.revokeObjectURL(f.url); files = files.filter((_, j) => j !== i) }
  function send() {
    const text = draft.trim()
    // Un testo che comincia con un comando consentito parte come comando (contratto 1.25); clear ed exit chiedono prima.
    const cmd = !files.length && !s.question ? slashParse(draft, st.slash) : null
    if (cmd && slashConfirm(cmd.cmd)) { confirm = cmd; confirmDlg?.showModal(); return }
    if (cmd) onSlash(cmd.cmd, cmd.args)
    else if (files.length) { onAttach(files.map(f => f.file), text); files.forEach(f => f.url && URL.revokeObjectURL(f.url)); files = [] }
    else if (!text) return
    else if (s.question) onAnswerText(textArg(text))
    else onSend(text)
    draft = ''
  }
  // Invio manda, Maiusc+Invio va a capo, come sul tablet con la tastiera fisica.
  function key(e: KeyboardEvent) {
    if (e.key !== 'Enter' || e.shiftKey || e.isComposing) return
    e.preventDefault()
    if (md === 'send' || md === 'send_tonal') send()
  }
  export function focus() { area?.focus(); area?.setSelectionRange(draft.length, draft.length) }
  // In un campo stretto (le colonne della plancia) il suggerimento accanto a «Usa» e all'invio andava a frammenti: lì sta
  // su una riga sua sopra il campo, intero, e il clic lo mette nel campo (come l'app sotto i 320 dp).
  let barW = $state(0)
  const narrow = $derived(barW > 0 && barW < 320)
  const inField = $derived(narrow ? null : field)
  const placeholder = $derived(inField ?? (s.question ? t.answerFree : toMaster ? t.masterPlaceholder : t.writeTo(s.name)))
</script>

{#if md === 'reopen'}
  <div class="bar"><button class="reopen" onclick={onReopen}>{t.reopen}</button></div>
{:else}
  <div class="bar" bind:clientWidth={barW}>
    {#if files.length}
      <div class="thumbs">
        {#each files as f, i}
          <div class="thumb">
            {#if f.url}<img src={f.url} alt={f.file.name} />{:else}<span class="fname"><Icon name="file" color="var(--icon)" />{f.file.name}</span>{/if}
            <button class="x" aria-label={t.removeImage} onclick={() => drop(i)}><svg viewBox="0 0 24 24" width="16" height="16"><path d="M18 6 6 18M6 6l12 12" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg></button>
          </div>
        {/each}
      </div>
    {/if}
    {#if hints.length}
      <div class="chips">{#each hints as c}<button class="chip" onclick={() => { draft = `/${c} `; area?.focus() }}>/{c}</button>{/each}</div>
    {/if}
    {#if narrow && field && !draft.trim()}
      <button type="button" class="sugrow" onclick={() => { draft = field ?? ''; area?.focus() }}><i>{field}</i><span>{t.use}</span></button>
    {/if}
    <form class="field" onsubmit={(e) => { e.preventDefault(); send() }}>
      <span class="anchor">
        <button type="button" class="plus" aria-label={t.attach} aria-expanded={menu} onclick={() => (menu = !menu)}>
          <svg viewBox="0 0 24 24" width="24" height="24"><path d="M12 5v14M5 12h14" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" /></svg>
        </button>
        {#if menu}
          <button type="button" class="scrim" aria-label={t.close} onclick={() => (menu = false)}></button>
          <div class="menu" role="menu">
            <button type="button" role="menuitem" onclick={() => { menu = false; images?.click() }}><Icon name="image" color="var(--icon)" size={22} />{t.attachGallery}</button>
            <button type="button" role="menuitem" onclick={() => { menu = false; camera?.click() }}><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3z" /><circle cx="12" cy="13" r="3" /></svg>{t.attachCamera}</button>
            {#if canFiles}<button type="button" role="menuitem" onclick={() => { menu = false; any?.click() }}><Icon name="file" color="var(--icon)" size={22} />{t.attachFile}</button>{/if}
            {#if onRecurring}
              <hr />
              <button type="button" role="menuitem" onclick={() => { menu = false; onRecurring?.() }}><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8M3 3v5h5M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16M16 16h5v5" /></svg>{t.recurringOpen}</button>
            {/if}
          </div>
        {/if}
      </span>
      <input bind:this={images} type="file" accept="image/*" multiple hidden onchange={picked} />
      <input bind:this={camera} type="file" accept="image/*" capture="environment" hidden onchange={picked} />
      <input bind:this={any} type="file" hidden onchange={picked} />
      <textarea bind:this={area} rows="1" class:sug={!!inField} bind:value={draft} onkeydown={key} {placeholder}></textarea>
      {#if inField && !draft.trim()}<button type="button" class="use" onclick={() => { draft = field ?? ''; area?.focus() }}>{t.use}</button>{/if}
      {#if md === 'stop'}
        <button type="button" class="round filled" aria-label={t.stop} title={t.stop} onclick={onStop}><svg viewBox="0 0 24 24" width="20" height="20"><rect x="6.5" y="6.5" width="11" height="11" rx="2" fill="currentColor" /></svg></button>
      {:else}
        <button type="submit" class="round" class:filled={md === 'send'} class:tonal={md === 'send_tonal'} disabled={md === 'none'} aria-label={t.send}>
          <svg viewBox="0 0 24 24" width="22" height="22"><path d="M3.4 20.4 21 12 3.4 3.6 3.4 10.2 15 12 3.4 13.8z" fill="currentColor" /></svg>
        </button>
      {/if}
    </form>
    {#if !narrow}<div class="keys mono">{t.enterSends}</div>{/if}
  </div>
{/if}

<dialog bind:this={confirmDlg} onclose={() => (confirm = null)}>
  {#if confirm}
    <h3>{t.slashConfirmTitle(confirm.cmd, s.name)}</h3>
    <p>{confirm.cmd === 'exit' ? t.exitText : t.slashConfirmClear}</p>
    <div class="btns">
      <button class="text2" onclick={() => confirmDlg?.close()}>{t.cancel}</button>
      <button class="link" onclick={() => { if (confirm) onSlash(confirm.cmd, confirm.args); draft = ''; confirmDlg?.close() }}>{t.exitOk}</button>
    </div>
  {/if}
</dialog>

<style>
  .bar { padding: 8px 12px 4px; display: flex; flex-direction: column; gap: 8px; }
  .reopen { height: 56px; border-radius: 28px; background: var(--primary); color: var(--on-primary); font-weight: 500; font-size: 15px; margin-bottom: 8px; }
  .thumbs, .chips { display: flex; gap: 8px; overflow-x: auto; }
  .thumb { position: relative; flex: none; }
  .thumb img { width: 72px; height: 72px; object-fit: cover; border-radius: 14px; display: block; }
  .fname { display: flex; align-items: center; gap: 6px; height: 72px; padding: 0 30px 0 12px; border-radius: 14px; background: var(--surface); font-size: 13px; max-width: 220px; }
  .x { position: absolute; top: 2px; right: 2px; width: 26px; height: 26px; border-radius: 50%; background: rgb(0 0 0 / .6); color: #fff; display: grid; place-items: center; }
  .chip { font: 14px var(--mono); padding: 6px 12px; border-radius: 8px; border: 1px solid var(--line); white-space: nowrap; }
  .chip:hover { background: var(--surface); }
  .field { display: flex; align-items: center; gap: 4px; border: 1px solid rgb(255 255 255 / .25); border-radius: 28px; padding: 0 8px 0 4px; min-height: 56px; }
  .field:focus-within { border-color: var(--icon); border-width: 2px; padding: 0 7px 0 3px; }
  .anchor { position: relative; flex: none; }
  .plus { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; }
  .plus:hover { background: var(--surface); }
  .scrim { position: fixed; inset: 0; z-index: 9; cursor: default; }
  .menu { position: absolute; bottom: 52px; left: 0; z-index: 10; min-width: 240px; background: var(--surface); border-radius: 12px; padding: 8px 0; box-shadow: 0 12px 40px rgb(0 0 0 / .5); border: 1px solid var(--line); }
  .menu button { display: flex; align-items: center; gap: 12px; width: 100%; padding: 12px 16px; font-size: 15px; text-align: left; }
  .menu button:hover { background: var(--high); }
  .menu hr { border: 0; height: 1px; background: var(--line); margin: 4px 0; }
  textarea { flex: 1; resize: none; background: none; border: 0; outline: 0; color: var(--text); font: inherit; font-size: 16px; letter-spacing: .03em; field-sizing: content; max-height: 5lh; padding: 14px 0 14px 12px; min-width: 0; }
  .field:has(.anchor) textarea { padding-left: 4px; }
  textarea::placeholder { color: var(--text2); }
  /* In corsivo e attenuato solo il suggerimento, come dopo ❯ nel terminale. */
  textarea.sug::placeholder { font-style: italic; color: var(--stale); }
  .sugrow { display: flex; align-items: center; gap: 10px; padding: 6px 12px; border-radius: 12px; text-align: left; }
  .sugrow i { flex: 1; color: var(--stale); }
  .sugrow span { color: var(--icon); font-weight: 500; font-size: 14px; }
  .sugrow:hover { background: var(--surface); }
  .use { color: var(--icon); padding: 8px 10px; font-weight: 500; border-radius: 16px; }
  .use:hover { background: var(--surface); }
  .round { width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; flex: none; background: var(--surface); color: var(--text2); }
  .round.filled { background: var(--primary); color: var(--on-primary); }
  .round.tonal { background: var(--high); color: var(--icon); }
  .round:disabled { cursor: default; }
  .keys { padding: 0 22px 6px; }
  @media (max-width: 760px) { .keys { display: none; } .bar { padding-bottom: 12px; } }
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 22px; width: min(400px, 92vw); border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  dialog h3 { font-size: 22px; font-weight: 600; margin-bottom: 8px; }
  dialog p { color: var(--text2); font-size: 14px; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
  .btns button { padding: 10px 14px; border-radius: 20px; font-weight: 500; }
  .text2 { color: var(--text2); }
  .link { color: var(--icon); }
  .btns button:hover { background: var(--high); }
</style>
