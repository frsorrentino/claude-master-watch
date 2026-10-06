<script lang="ts">
  import type { Snippet } from 'svelte'
  import type { CmdOp, Model, Session, State } from './contract'
  import { notes as notesOf, resetLabel, sameModel, shortModel, tone, wider as widerOf, type Tone } from './header'
  import { MASTER } from './summary'
  import { t } from './t'
  import Badge from './Badge.svelte'
  import { adviceOf, ctxBand, tokens } from './masterService'

  // La testata della sessione (SheetHeader.kt): una riga con modello · effort in una pillola, la quota delle 5 ore con l'ora
  // dell'azzeramento, il contesto ad anello e il menu ⋮; sotto obiettivo, priorità e finestra. Su desktop a sinistra anche
  // badge e nome, come la plancia del tablet.
  let { st, s, wide, onBack, onCmd, onPrompt, onHandoff, status }: {
    /** `wide`: badge e nome a sinistra (la testata unica del desktop); in una colonna della plancia li ha la colonna. */
    /** `status`: lo stato della colonna in testa alla riga (web app installata, la testata della colonna non c'è). */
    status?: Snippet
    st: State; s: Session; wide: boolean; onBack?: () => void
    onCmd: (op: CmdOp, arg?: string) => void; onPrompt: (text: string) => void
    /** Contratto 1.37: «Handoff, poi /clear»; lo gestisce App (prompt e /clear a turno finito). */
    onHandoff: () => void
  } = $props()

  // La scelta resta in vista finché il PC non la conferma (Tune.model): qui finché si resta sulla sessione.
  let picked = $state<{ model?: Model; effort?: string; followed?: boolean }>({})
  $effect(() => { s.name; picked = {} })
  const model = $derived(picked.model ?? s.model)
  const effort = $derived(picked.effort ?? s.effort)
  const followed = $derived(picked.followed ?? !!s.followed)
  const tunable = $derived(!!st.choices && s.state !== 'gone')
  const quota = $derived(st.quota[s.account])
  const reset = $derived(resetLabel(quota?.reset_h5, st.ts))
  const notes = $derived(notesOf(s, t.notes))
  const wider = $derived(widerOf({ ...s, model }, st.choices))
  const canExit = $derived(!!st.slash?.includes('exit') && s.state !== 'gone')
  // Contratto 1.37: il consiglio di fable-director, dentro il foglio; il puntino solo se la scelta attuale è diversa.
  const advice = $derived(adviceOf(s, st.choices, st.ts))
  // L'ora dell'azzeramento accanto alla percentuale quando la testata è larga (480 px, come l'app), sotto se è stretta.
  let width = $state(0)
  const inline = $derived(width >= 480)
  // Una colonna molto stretta: «62%» senza «ctx», così il menu ⋮ resta in vista.
  const compact = $derived(width > 0 && width < 300)
  const ring: Record<Tone, string> = { neutral: 'var(--b-ring)', warn: 'var(--b-warn)', alert: 'var(--b-alert)' }
  const arc = (pct: number) => `${Math.max(0, Math.min(100, pct)) * 0.4712} 100`

  let menu = $state(false)
  let tune: HTMLDialogElement | undefined = $state()
  let ctx: HTMLDialogElement | undefined = $state()
  let exit: HTMLDialogElement | undefined = $state()
  function close(d?: HTMLDialogElement) { d?.close() }
  function setModel(m: Model) { picked.model = m; onCmd('model', m.id); close(tune); close(ctx) }
  function setEffort(e: string) { picked.effort = e; onCmd('effort', e); close(tune) }
  // Clic sul fondo del dialogo: si chiude, come il foglio dell'app.
  const backdrop = (e: MouseEvent) => { if (e.target === e.currentTarget) (e.currentTarget as HTMLDialogElement).close() }
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && (menu = false)} />

<header bind:clientWidth={width}>
  <div class="row">
    {#if onBack}<button class="ib back" onclick={onBack} aria-label={t.back}><svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 6l-6 6 6 6" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>{/if}
    {#if status}{@render status()}{/if}
    {#if wide}
      <span class="lead"><Badge {s} size={26} /><h1>{s.name}</h1></span>
    {/if}
    <button class="pill" disabled={!tunable} onclick={() => tune?.showModal()}>
      {[shortModel(model) ?? t.modelTitle, effort].filter(Boolean).join(' · ')}
      {#if tunable}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M7 10l5 5 5-5z" fill="var(--text2)" /></svg>{/if}
      {#if tunable && advice?.dot}<span class="dot" title={t.adviceDot} aria-label={t.adviceDot}></span>{/if}
    </button>
    {#if !wide}<span class="sp"></span>{/if}
    <!-- In una colonna stretta la quota lascia il posto a modello, contesto e menu (è anche nella home). -->
    {#if quota?.h5 != null && (width === 0 || width >= 340)}
      <span class="meter" title={reset ? t.quotaResetDesc(quota.h5, reset) : undefined}>
        <svg viewBox="0 0 20 20" width="20" height="20"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(quota.h5)} style="stroke:{quota.stale ? 'var(--wait)' : ring[tone(quota.h5)]}" class="arc" /></svg>
        <span class="mcol" class:inline>
          <span class="ml" class:stale={quota.stale}>{t.quota5h(quota.h5)}</span>
          {#if reset}<span class="reset"><svg viewBox="0 0 24 24" width="12" height="12"><path d="M21 12a9 9 0 1 1-3-6.7L21 8M21 3v5h-5M12 7v5l3 2" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{reset}</span>{/if}
        </span>
      </span>
    {/if}
    {#if s.context != null}
      <button class="meter ctxb" disabled={!tunable} onclick={() => ctx?.showModal()}>
        <svg viewBox="0 0 20 20" width="20" height="20"><circle cx="10" cy="10" r="7.5" class="trk" /><circle cx="10" cy="10" r="7.5" pathLength="47.12" stroke-dasharray={arc(s.context)} style="stroke:{ring[tone(s.context)]}" class="arc" /></svg>
        <span class="ml">{compact ? `${s.context}%` : t.ctx(s.context)}</span>
      </button>
    {/if}
    <span class="anchor">
      <button class="ib" aria-label={t.more} aria-expanded={menu} onclick={() => (menu = !menu)}>
        <svg viewBox="0 0 24 24" width="22" height="22"><g fill="var(--text2)"><circle cx="12" cy="5" r="2" /><circle cx="12" cy="12" r="2" /><circle cx="12" cy="19" r="2" /></g></svg>
      </button>
      {#if menu}
        <button class="scrim" aria-label={t.closeWord} onclick={() => (menu = false)}></button>
        <div class="panel" role="menu">
          <div class="ph"><Badge {s} size={20} /><b>{s.name}</b></div>
          <button class="mi" role="menuitem" onclick={() => { picked.followed = !followed; onCmd(followed ? 'unfollow' : 'follow'); menu = false }}>
            <svg viewBox="0 0 24 24" width="22" height="22"><path d={followed ? 'M8.7 3A6 6 0 0 1 18 8a21 21 0 0 0 .6 5M17 17H3s3-2 3-9a4.7 4.7 0 0 1 .3-1.7M10.3 21a1.9 1.9 0 0 0 3.4 0M2 2l20 20' : 'M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.9 1.9 0 0 0 3.4 0'} /></svg>
            <span><span class="mt">{followed ? t.unfollow : t.follow}</span><span class="ms">{followed ? t.unfollowSub : t.followSub}</span></span>
          </button>
          {#if s.name !== MASTER && st.sessions.some(x => x.name === MASTER && x.state !== 'gone')}
            <button class="mi" role="menuitem" onclick={() => { onCmd('prompt', t.askMasterPrompt(s.name)); menu = false }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" /></svg>
              <span><span class="mt">{t.askMaster}</span><span class="ms">{t.askMasterSub}</span></span>
            </button>
          {/if}
          {#if s.link?.trim()}
            <a class="mi" role="menuitem" href={s.link} target="_blank" rel="noopener" onclick={() => (menu = false)}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M15 3h6v6M10 14 21 3M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /></svg>
              <span><span class="mt">{t.openInClaude}</span><span class="ms">{t.openInClaudeSub}</span></span>
            </a>
          {/if}
          {#if canExit}
            <hr />
            <button class="mi danger" role="menuitem" onclick={() => { menu = false; exit?.showModal() }}>
              <svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg>
              <span><span class="mt">{t.menuExit}</span><span class="ms">{t.menuExitSub}</span></span>
            </button>
          {/if}
        </div>
      {/if}
    </span>
  </div>
  {#each notes as n}<div class="note">{n}</div>{/each}
</header>

<dialog bind:this={tune} onclick={backdrop}>
  <p class="sub">{t.choiceThisSession}</p>
  {#each [['model', t.modelTitle], ['effort', t.effortTitle]] as [kind, title]}
    <h3>{title}</h3>
    {#if kind === 'model'}
      {#each st.choices?.models ?? [] as m}
        {@const rec = !!advice && sameModel(m.id, advice.model)}
        <label class="radio"><input type="radio" name="model" checked={sameModel(m.id, model?.id)} onchange={() => setModel(m)} />{m.label ?? shortModel(m) ?? m.id}{#if rec}<span class="rec">{t.adviceTag}</span>{/if}</label>
        {#if rec}<p class="why">{advice!.reason}</p>{/if}
      {/each}
    {:else}
      {#each st.choices?.efforts ?? [] as e}
        <label class="radio"><input type="radio" name="effort" checked={e === effort} onchange={() => setEffort(e)} />{e}{#if advice?.effort === e}<span class="rec">{t.adviceTag}</span>{/if}</label>
      {/each}
    {/if}
  {/each}
  {#if advice?.cost}
    <p class="cost"><svg viewBox="0 0 24 24" width="18" height="18"><path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0zM12 9v4M12 17h.01" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>{t.adviceCost(tokens(advice.cost))}</p>
  {/if}
</dialog>

<dialog bind:this={ctx} onclick={backdrop}>
  {#if s.context != null}
    <h3>{t.ctxTitle(s.context)}</h3>
    <div class="col">
      <button class:filled={ctxBand(s.context) != null} class="tonal opt" onclick={() => { onHandoff(); close(ctx) }}>{t.ctxHandoffClear}<small>{t.ctxHandoffClearSub}</small></button>
      <button class="tonal opt" onclick={() => { onPrompt('/compact'); close(ctx) }}>{t.ctxCompact}<small>{t.ctxCompactSub}</small></button>
      {#if wider}<button class="tonal opt" onclick={() => setModel(wider!)}>{t.ctxWider}<small>{t.ctxWiderSub}</small></button>{/if}
    </div>
  {/if}
</dialog>

<dialog bind:this={exit} onclick={backdrop} class="alert">
  <h3>{t.exitTitle(s.name)}</h3>
  <p class="sub">{t.exitText}</p>
  <div class="btns">
    <button class="text2" onclick={() => close(exit)}>{t.cancel}</button>
    <button class="link" onclick={() => { onCmd('slash', 'exit'); close(exit) }}>{t.exitOk}</button>
  </div>
</dialog>

<style>
  header { border-bottom: 1px solid var(--line); padding: 4px 0 8px; background: var(--bg); }
  .row { display: flex; align-items: center; gap: 8px; padding: 0 0 0 16px; min-height: 52px; }
  .row:has(.back) { padding-left: 2px; gap: 6px; }
  .lead { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; }
  h1 { font-size: 19px; font-weight: 600; white-space: nowrap; overflow: hidden; }
  .sp { flex: 1; }
  .ib { width: 44px; height: 44px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); flex: none; }
  .ib:hover { background: var(--surface); }
  .back { color: var(--text); width: 36px; }
  .pill { display: flex; align-items: center; min-width: min-content; overflow: hidden; background: var(--surface); border-radius: 999px; padding: 6px 6px 6px 12px; font-size: 14px; font-weight: 500; white-space: nowrap; flex: 0 1 auto; }
  .pill { position: relative; }
  .pill svg { flex: none; }
  /* Il puntino del consiglio, in alto a destra della pillola (mockup 1.37). */
  .dot { position: absolute; top: 2px; right: 2px; width: 9px; height: 9px; border-radius: 50%; background: var(--advice); border: 2px solid var(--bg); }
  .rec { margin-left: auto; font: 600 11px/1.4 var(--mono); letter-spacing: .06em; text-transform: uppercase; color: var(--advice); background: color-mix(in srgb, var(--advice) 14%, transparent); border-radius: 999px; padding: 3px 9px; }
  .why { margin: -6px 0 4px 38px; font-size: 13px; color: var(--text2); }
  .cost { display: flex; gap: 10px; align-items: flex-start; margin-top: 12px; padding: 10px 12px; border-radius: 16px; font-size: 13.5px; color: var(--b-warn); background: color-mix(in srgb, var(--b-warn) 10%, transparent); }
  .cost svg { flex: none; margin-top: 1px; }
  .opt { display: flex; flex-direction: column; align-items: flex-start; text-align: left; }
  .opt small { font-size: 12.5px; font-weight: 400; opacity: .8; margin-top: 2px; }
  .pill:disabled { padding-right: 12px; cursor: default; }
  .pill:not(:disabled):hover { filter: brightness(1.2); }
  .meter { display: flex; align-items: center; gap: 6px; padding: 4px; border-radius: 8px; flex: none; }
  .ctxb:not(:disabled):hover { background: var(--surface); }
  .ctxb:disabled { cursor: default; }
  .trk, .arc { fill: none; stroke-width: 3; }
  .trk { stroke: var(--b-track); }
  .arc { stroke-linecap: round; transform: rotate(-90deg); transform-origin: center; }
  .mcol { display: flex; flex-direction: column; line-height: 1.2; }
  /* Su desktop l'ora dell'azzeramento sta accanto alla percentuale (segnalazione 05/10 16:41). */
  .mcol.inline { flex-direction: row; align-items: center; gap: 8px; }
  .ml { font-size: 14px; font-weight: 500; color: var(--text2); white-space: nowrap; }
  .ml.stale { color: var(--wait); }
  .reset { display: flex; align-items: center; gap: 2px; font-size: 11px; color: var(--text2); }
  .note { padding: 0 16px; font-size: 12px; font-weight: 500; color: var(--b-label); letter-spacing: .03em; }
  .anchor { position: relative; }
  .scrim { position: fixed; inset: 0; z-index: 9; cursor: default; }
  /* Il menu a pannello dell'app (AppMenuPanel): 380 px su desktop, voci con la spiegazione sotto. */
  .panel { position: absolute; right: 4px; top: 46px; z-index: 10; width: min(380px, calc(100vw - 24px)); background: var(--surface); border-radius: 20px; padding: 8px; box-shadow: 0 12px 40px rgb(0 0 0 / .5); border: 1px solid var(--line); }
  .ph { display: flex; align-items: center; gap: 12px; padding: 10px 12px 12px; font-size: 16px; }
  .mi { display: flex; gap: 14px; align-items: flex-start; width: 100%; padding: 12px; border-radius: 14px; text-align: left; color: var(--text); text-decoration: none; }
  .mi:hover { background: var(--high); }
  .mi svg { flex: none; fill: none; stroke: var(--icon); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
  .mi > span { display: flex; flex-direction: column; gap: 2px; }
  .mt { font-size: 15.5px; }
  .ms { font-size: 13px; color: var(--text2); }
  .danger svg { stroke: var(--b-alert); }
  .danger .mt { color: var(--b-alert); }
  hr { border: 0; height: 1px; background: var(--line); margin: 4px 8px; }
  /* I fogli: in basso sul telefono, una scheda al centro da 600 px (fogli adattivi dell'app). */
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 20px 24px; width: min(560px, 100vw); max-height: 85vh; border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  dialog :focus-visible { outline: 2px solid var(--icon); outline-offset: 2px; }
  @media (max-width: 599px) { dialog { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  dialog.alert { width: min(400px, 92vw); margin: auto; border-radius: 28px; }
  dialog h3 { font-size: 22px; font-weight: 600; margin: 12px 0 4px; }
  dialog h3:first-child { margin-top: 0; }
  .sub { color: var(--text2); font-size: 14px; }
  .radio { display: flex; align-items: center; gap: 14px; padding: 12px 4px; border-radius: 12px; cursor: pointer; font-size: 16px; }
  .radio:hover { background: var(--high); }
  .radio input { width: 20px; height: 20px; accent-color: var(--primary); margin: 0; }
  .col { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
  .tonal { background: var(--high); border-radius: 999px; padding: 12px 20px; font-weight: 500; }
  .tonal.filled { background: var(--primary); color: var(--on-primary); }
  .tonal:hover { filter: brightness(1.12); }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 20px; }
  .btns button { padding: 10px 14px; border-radius: 20px; font-weight: 500; }
  .text2 { color: var(--text2); }
  .link { color: var(--icon); }
  .btns button:hover { background: var(--high); }
</style>
