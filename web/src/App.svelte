<script lang="ts">
  import { demoEvents, demoMine, demoSamples, demoSearch, demoState, demoTimeline, demoTranscripts } from './lib/demo'
  import type { SearchPage } from './lib/contract'
  import type { CmdOp, TranscriptEntry } from './lib/contract'
  import type { Sent, Status } from './lib/chatRules'
  import { toggle } from './lib/speech.svelte'
  import Home from './lib/Home.svelte'
  import Chat from './lib/Chat.svelte'
  import Desk from './lib/Desk.svelte'
  import ColumnHead from './lib/ColumnHead.svelte'
  import { t } from './lib/t'
  import HomePane from './lib/HomePane.svelte'
  import AppBar, { type Page as PageName } from './lib/AppBar.svelte'
  import Page from './lib/Page.svelte'
  import Launch from './lib/Launch.svelte'
  import Diary from './lib/Diary.svelte'
  import Overview from './lib/Overview.svelte'
  import Search from './lib/Search.svelte'
  import Settings from './lib/Settings.svelte'
  import Queue from './lib/Queue.svelte'
  import QuotaPanel from './lib/QuotaPanel.svelte'
  import Inspector from './lib/Inspector.svelte'
  import { alert as elsewhereOf, key as alertKey, type Alert } from './lib/elsewhere'
  import { build as devicesOf, linked as linkedDevices } from './lib/devices'
  import { freshness } from './lib/durations'
  import { setRate, setVoice } from './lib/speech.svelte'
  import { build as overviewOf } from './lib/overview'
  import ReadingPill from './lib/ReadingPill.svelte'
  import { build, MASTER } from './lib/summary'
  import { add, columns, columnsFromPref, columnsPref, inspect, sharesFromPref, sharesPref, toggle as toggleCol, wide as isWide } from './lib/tablet'

  // Per ora i dati di prova (fixture del contratto); la strada locale del relay arriva con il contratto 1.35.
  const st = demoState
  let transcripts = $state<Record<string, TranscriptEntry[]>>(structuredClone(demoTranscripts))
  let mine = $state<[Sent, Status][]>(structuredClone(demoMine))
  // Quello che si è letto o avviato (turni finiti, resoconto, prossimi passi) e i messaggi mandati da qui, per «Per te».
  let read = $state(new Set<string>())
  const sent = $derived(mine.map(([m]) => m))
  const master = $derived(st.sessions.find(s => s.name === MASTER && s.state !== 'gone') ?? null)

  const load = (k: string) => { try { return localStorage.getItem(k) } catch { return null } }
  const save = (k: string, v: string) => { try { localStorage.setItem(k, v) } catch { /* resta per la sessione */ } }

  // Da 840 px la plancia con le colonne, come il tablet; sotto, una colonna sola come il telefono.
  let width = $state(window.innerWidth)
  const wide = $derived(isWide(width))
  // `#nome` apre quella sessione (link diretto, e i provini).
  const linked = decodeURIComponent(location.hash.slice(1))
  const known = (n: string) => st.sessions.some(s => s.name === n && s.state !== 'gone')

  // La master vive nella home: ridotta è la barra in fondo, espansa occupa la home (mai una colonna o una pagina a parte).
  let masterOpen = $state(linked === MASTER)
  // Telefono: la sessione aperta, o la home.
  let open = $state<string | null>(known(linked) && linked !== MASTER ? linked : null)
  const session = $derived(st.sessions.find(s => s.name === open) ?? null)

  // Plancia: le colonne scelte (le prime tre senza una scelta salvata), le larghezze in dodicesimi, il lato della home.
  // Le colonne sono le sessioni della lista, senza la master: una master rimasta fra le scelte salvate si toglie da sé.
  const summary = $derived(build(st, sent, st.ts, read))
  const live = $derived(summary.rows.map(r => r.session.name).filter(n => n !== MASTER))
  let pinned = $state<string[] | null>(columnsFromPref(load('cm.columns')))
  if (known(linked) && linked !== MASTER) pinned = add(pinned ?? columns(null, live), linked)
  const cols = $derived(columns(pinned, live))
  let shares = $state(sharesFromPref(load('cm.shares'), 0))
  $effect(() => { if (shares.length !== cols.length) shares = sharesFromPref(load('cm.shares'), cols.length) })
  let homeRight = $state(load('cm.home_right') === '1')
  // I dettagli della prima colonna accanto alle colonne: dalle Impostazioni, spenti di default (Franz, 04/10 14:40).
  let details = $state(load('cm.details') === '1')
  const rowOf = $derived(Object.fromEntries(summary.rows.map(r => [r.session.name, r])))

  // Il cambio con la transizione del browser, quando c'è (Chrome): niente ridisegni continui.
  const smooth = (go: () => void) => { if (document.startViewTransition) document.startViewTransition(go); else go() }
  function setCols(c: string[]) { smooth(() => { pinned = c; save('cm.columns', columnsPref(c)) }) }
  function setShares(s: number[]) { shares = s; save('cm.shares', sharesPref(s)) }
  // Aprire una sessione (da «Per te», da una ricerca, dal controller della lettura): la master si espande nella home,
  // le altre entrano in colonna sulla plancia o diventano la pagina sul telefono.
  function pick(name: string) {
    if (name === MASTER) smooth(() => { masterOpen = true; open = null })
    else if (wide) setCols(add(cols, name))
    else smooth(() => { open = name })
  }
  // Le pagine del menu: sul telefono al posto della home, sulla plancia al posto delle colonne.
  // `?page=diary` apre una pagina (link diretto, e i provini).
  const asked = new URLSearchParams(location.search).get('page')
  let page = $state<PageName | null>(asked && ['launch', 'diary', 'overview', 'search', 'settings', 'queue'].includes(asked) ? (asked as PageName) : null)
  const pageTitle: Record<PageName, string> = { launch: t.menuLaunch, diary: t.menuRegister, overview: t.menuQuadro, search: t.menuSearch, settings: t.settingsTitle, queue: t.queueTitle }
  const openPage = (p: PageName | null) => smooth(() => { page = p })
  // La quota per account, come la Panoramica; i campioni del ritmo arrivano col trasporto.
  const overview = $derived(overviewOf(st, demoEvents, demoSamples, st.ts, undefined, false))
  let nightDlg: HTMLDialogElement | undefined = $state()
  // La ricerca nelle conversazioni (contratto 1.27): per ora la risposta di prova, poi il comando `search` al relay.
  let searchPage = $state<SearchPage | null>(null)
  // Gli avvisi delle altre sessioni chiusi con ✕ (una domanda non si chiude: resta finché qualcuno risponde).
  let seenAlerts = $state(new Set<string>())
  // Sulla plancia niente avviso per una sessione che è già in una colonna accanto.
  function elsewhereFor(name: string) {
    const a = elsewhereOf(st, name, st.ts, new Set(sent.map(m => m.session)), seenAlerts)
    if (!a || !wide) return a
    return (a.type === 'waiting' ? a.sessions.every(n => cols.includes(n)) : cols.includes(a.session)) ? null : a
  }
  function openAlert(a: Alert) {
    if (a.type === 'finished') { seenAlerts = new Set([...seenAlerts, alertKey(a)]); pick(a.session) }
    else if (a.sessions.length === 1) pick(a.sessions[0])
    else openQueue()
  }
  const openQueue = () => openPage('queue')
  // Il tocco su una scheda della home: sulla plancia apre e chiude la sua colonna (Franz, 05/10 11:09), sul telefono apre.
  function card(name: string) { if (name !== MASTER && wide) setCols(toggleCol(cols, name)); else pick(name) }
  const slots = $derived<(string | null)[]>(wide ? [masterOpen ? MASTER : null, ...cols] : [session ? session.name : masterOpen ? MASTER : null])

  function sendTo(name: string, text: string) {
    // Senza trasporto il messaggio resta «inviato al PC»: lo stato vero arriva con il relay.
    mine = [...mine, [{ id: crypto.randomUUID(), session: name, text, sentAt: Math.round(Date.now() / 1000) }, 'sent']]
  }
  const cmd = (name: string) => (op: CmdOp, arg?: string, text?: string) => console.info('[cm] comando', name, op, arg, text)
  const answer = (n: string, x: number) => { pick(n); cmd(n)('answer', String(x)) }
</script>

<svelte:window bind:innerWidth={width} />

{#snippet chatOf(name: string, inColumn: boolean)}
  {@const s = st.sessions.find(x => x.name === name)!}
  <Chat {st} {s} entries={transcripts[name] ?? []} mine={mine.filter(([m]) => m.session === name)} onSend={(x) => sendTo(name, x)} onPick={pick}
    onAnswer={answer} onCmd={cmd(name)} events={demoEvents} {sent} {read} onRead={(k) => (read = new Set([...read, k]))} onPromptTo={sendTo}
    wide={false} {slots} elsewhere={elsewhereFor(name)} onElsewhere={() => { const a = elsewhereFor(name); if (a) openAlert(a) }}
    onElsewhereDismiss={() => { const a = elsewhereFor(name); if (a) seenAlerts = new Set([...seenAlerts, alertKey(a)]) }} onBack={inColumn ? undefined : () => smooth(() => { open = null })} />
{/snippet}

{#snippet homePane()}
  <HomePane {master} entries={transcripts[MASTER] ?? []} open={masterOpen} onToggle={(o) => smooth(() => { masterOpen = o })} onSpeak={(x) => toggle(x, MASTER)}>
    {#snippet list()}
      <AppBar {st} now={st.ts} openCount={summary.open} onPage={openPage} />
      <div class="list"><Home {st} selected={[]} onPick={card} onAnswer={answer} onStep={(n, x) => { pick(n); sendTo(n, x) }} footer={wide ? quotaPanels : undefined} /></div>
      <div class="reading"><ReadingPill {slots} here={null} onOpen={pick} /></div>
    {/snippet}
    {#snippet chat()}{@render chatOf(MASTER, true)}{/snippet}
  </HomePane>
{/snippet}

{#snippet pageView(p: PageName)}
  <Page title={pageTitle[p]} onBack={() => openPage(null)}>
    {#if p === 'launch'}
      <Launch {st} onSession={(n, reopen) => { if (reopen) cmd(n)('reopen'); else { openPage(null); pick(n) } }}
        onLaunch={(pr, first) => { cmd(pr.name)('launch', pr.path, first || undefined); openPage(null) }} />
    {:else if p === 'diary'}
      <Diary {st} events={demoEvents} rings={overview.rings} now={st.ts} onAdd={() => nightDlg?.showModal()} onRemove={(id) => cmd('')('night_remove', id)}
        onQuadro={() => openPage('overview')} onSession={(n) => { openPage(null); pick(n) }} />
    {:else if p === 'overview'}
      <Overview model={overview} onSession={(n) => { openPage(null); pick(n) }}
        onQuestion={() => openPage('queue')} />
    {:else if p === 'search'}
      <Search {sent} events={demoEvents} remote onQuery={(q) => (searchPage = demoSearch(q))} page={searchPage}
        known={new Set(st.sessions.map(x => x.name))} onOpen={(n) => { if (n) { openPage(null); pick(n) } else openPage('diary') }} />
    {:else if p === 'settings'}
      <Settings m={devicesOf(st.host, st, freshness(st.ts, st.ts), st.ts, '', __APP_VERSION__, true, null, false, null)}
        devices={linkedDevices(st, null, st.ts) ?? []} now={st.ts} channel={t.channelDemo} onRate={setRate} onVoice={setVoice}
        details={wide ? details : null} onDetails={(on) => { details = on; save('cm.details', on ? '1' : '0') }} />
    {:else if p === 'queue'}
      <Queue {st} now={st.ts} onAnswer={(n, arg, op = 'answer') => cmd(n)(op, arg || undefined)} onSession={(n) => { openPage(null); pick(n) }} />
    {:else}
      <p class="soon">{t.soon}</p>
    {/if}
  </Page>
{/snippet}

{#snippet quotaPanels()}{#each overview.rings as r (r.account)}<QuotaPanel ring={r} now={st.ts} />{/each}{/snippet}

{#snippet inspector()}
  {@const first = st.sessions.find(x => x.name === cols[0])}
  {#if first}<Inspector i={inspect(first, demoTimeline, st.ts)} quotaH5={st.quota[first.account]?.h5 ?? null} loading={false} />{/if}
{/snippet}

{#snippet deskPage()}{#if page}{@render pageView(page)}{/if}{/snippet}

{#if wide}
  <Desk {cols} {shares} {homeRight} onCols={setCols} onShares={setShares} override={page ? deskPage : null} details={details && cols.length && !page ? inspector : null}
    onHomeSide={() => smooth(() => { homeRight = !homeRight; save('cm.home_right', homeRight ? '1' : '0') })}>
    {#snippet home()}{@render homePane()}{/snippet}
    {#snippet column(name, grab)}
      <div class="column">
        {#if rowOf[name]}<ColumnHead r={rowOf[name]} now={st.ts} onClose={() => setCols(cols.filter(c => c !== name))} onGrab={grab} />{/if}
        <div class="cbody">{@render chatOf(name, true)}</div>
      </div>
    {/snippet}
    {#snippet empty()}<p>{t.tabletDeskEmpty}</p>{/snippet}
  </Desk>
{:else}
  <div class="phone">
    {#if page}{@render pageView(page)}{:else if session}{@render chatOf(session.name, false)}{:else}{@render homePane()}{/if}
  </div>
{/if}
<span class="demo mono">{t.demo}</span>

<!-- «Aggiungi alla notte»: lo stesso foglio di Lancia, solo progetti (LaunchSheet con night_add). -->
<dialog bind:this={nightDlg} class="sheet" onclick={(e) => e.target === e.currentTarget && nightDlg?.close()}>
  <h2>{t.nightAddTitle}</h2>
  <Launch {st} action={t.nightAddTitle} onLaunch={(pr, text) => { cmd(pr.name)('night_add', pr.path, text); nightDlg?.close() }} />
</dialog>

<style>
  .phone { height: 100%; display: flex; flex-direction: column; max-width: 760px; margin: 0 auto; }
  .list { flex: 1; min-height: 0; }
  .reading:empty { display: none; }
  .reading { padding: 8px 0; }
  .column { height: 100%; display: flex; flex-direction: column; }
  .cbody { flex: 1; min-height: 0; }
  .soon { color: var(--text2); padding: 24px; }
  .sheet { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 0 0; width: min(600px, 100vw); max-height: 90vh; border-radius: 28px; }
  .sheet::backdrop { background: rgb(0 0 0 / .55); }
  .sheet h2 { font-size: 22px; font-weight: 600; padding: 0 20px; }
  @media (max-width: 599px) { .sheet { margin: auto 0 0; max-width: 100vw; border-radius: 28px 28px 0 0; } }
  .demo { position: fixed; right: 12px; bottom: 6px; opacity: .6; pointer-events: none; }
  @media (max-width: 839px) { .demo { bottom: 0; right: 50%; transform: translateX(50%); font-size: 10px; } }
</style>
