<script lang="ts">
  import type { Session, TranscriptEntry, TranscriptFile } from './contract'
  import { counts, type Item } from './chatFeed'
  import type { Status } from './chatRules'
  import { blocks, cards, compact, type Table } from './markdown'
  import { kind, row } from './toolText'
  import { outcomeForPhone } from './links'
  import { parseSteps } from './nextSteps'
  import { speech, toggle } from './speech.svelte'
  import type { IconName } from './icons'
  import { t } from './t'
  import Icon from './Icon.svelte'
  import { canShare, type FileAct } from './fileActions'
  import { autoPreview, isImage, isVideo } from './media'
  import { media, mediaKey } from './mediaCache.svelte'
  import Md from './Md.svelte'

  // La conversazione della scheda (SessionSheet.kt): i messaggi a destra con lo stato, Claude a tutta larghezza con Copia
  // e ▶, i passaggi raccolti in una card, i file come chip, il costo del turno, e in fondo la riga dal vivo.
  let { s, items, now, onFile = () => {}, onDecision }: { s: Session; items: Item[]; now: number; onFile?: (path: string, act: FileAct | 'prepare' | 'preview') => void; onDecision?: (text: string) => void } = $props()

  // Un file: sotto il nome i tasti apri, scarica, copia e condividi (condividi solo dove il browser lo sa fare; Franz, 07/10
  // 15:48). Il file si chiede al PC appena il puntatore ci arriva, così il tocco trova i byte pronti.
  const fileActs: [FileAct, string, IconName][] = [['open', t.fileOpen, 'external'], ['download', t.fileDownload, 'download'], ['copy', t.fileCopy, 'copy'], ...(canShare() ? [['share', t.fileShare, 'share'] as [FileAct, string, IconName]] : [])]

  // Immagini e video nascono già in anteprima, come sul telefono (Franz, 10/10 15:24): il file si chiede al PC appena la
  // card è a schermo, se `autoPreview` lo vuole; col risparmio dati del browser i video aspettano il tocco.
  function preview(_node: HTMLElement, f: TranscriptFile) {
    const saveData = (navigator as Navigator & { connection?: { saveData?: boolean } }).connection?.saveData === true
    if (!media[mediaKey(s.name, f.path)] && autoPreview(f.mime, f.size, !saveData)) onFile(f.path, 'preview')
  }

  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const copy = (x: string) => navigator.clipboard?.writeText(x)
  const toolIcon = (e: TranscriptEntry): IconName => (e.error ? 'error' : kind(e.tool))
  const fileIcon = (f: TranscriptFile): IconName =>
    f.mime?.startsWith('image/') ? 'image' : f.mime === 'application/pdf' ? 'pdf' : f.mime?.startsWith('video/') ? 'film' : f.mime?.startsWith('audio/') ? 'audio' : 'file'
  const size = (b: number) => (b >= 1_000_000 ? `${(b / 1_000_000).toFixed(1).replace('.', ',')} MB` : b >= 1000 ? `${Math.floor(b / 1000)} KB` : `${b} B`)
  const since = (d: number) => (d < 3600 ? `${Math.floor(d / 60)} m` : d < 86400 ? `${Math.floor(d / 3600)} h ${String(Math.floor((d % 3600) / 60)).padStart(2, '0')}` : `${Math.floor(d / 86400)} g`)
  function cost(e: TranscriptEntry): string | null {
    const tr = e.turn
    if (!tr) return null
    const secs = (tr.ended ?? 0) - (tr.started ?? 0)
    const parts = [tr.started != null && tr.ended != null && secs > 0 ? (secs < 60 ? `${secs} s` : since(secs)) : null, tr.out != null ? t.turnTokens(tr.out) : null]
    return parts.filter(Boolean).join(' · ') || null
  }
  const status: Record<Status, [IconName, string]> = {
    scheduled: ['alarm', 'var(--wait)'], offline: ['offline', 'var(--stale)'], uploading: ['upload', 'var(--text2)'], sending: ['clock', 'var(--text2)'],
    sent: ['check', 'var(--text2)'], uncertain: ['hourglass', 'var(--wait)'], failed: ['error', 'var(--gone)'], delivered: ['checks', 'var(--text2)'],
    queued: ['checks', 'var(--stale)'], working: ['checks', 'var(--busy)'], done: ['checks', 'var(--icon)'],
  }
  let open = $state<Record<string, boolean>>({})

  // Il tempo del turno al secondo, mentre la sessione lavora; fermo altrimenti.
  // Il contatore parte dall'ora dello stato (`now`) e avanza col tempo passato da quando la chat è aperta.
  const opened = Date.now()
  let tick = $state(0)
  const live = $derived(s.state === 'busy' || s.state === 'awaiting')
  $effect(() => { if (!live) return; const id = setInterval(() => (tick = Math.round((Date.now() - opened) / 1000)), 1000); return () => clearInterval(id) })
  const elapsed = (x: number) => (x < 60 ? `${x} s` : x < 3600 ? `${Math.floor(x / 60)} min ${x % 60} s` : `${Math.floor(x / 3600)} h ${Math.floor((x % 3600) / 60)} min`)
  const liveText = $derived([s.turn_started != null ? elapsed(Math.max(0, now + tick - s.turn_started)) : null, s.tool_note?.trim() || s.tool?.trim() || t.thinking].filter(Boolean).join(' · '))
</script>

{#snippet actions(text: string, at: number | null | undefined)}
  {@const reading = speech.text === text}
  <div class="acts">
    <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(text)}><Icon name="copy" /></button>
    <button class="play" aria-label={reading ? t.stopReading : t.readAloud} onclick={() => toggle(text, s.name)}>
      {#if reading}<svg viewBox="0 0 24 24" width="18" height="18"><rect x="7" y="7" width="10" height="10" rx="1.5" fill="currentColor" /></svg>
      {:else}<svg viewBox="0 0 24 24" width="18" height="18"><path d="M8 5.5v13l10.5-6.5z" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" /></svg>{/if}
    </button>
    {#if onDecision}<button class="sm" aria-label={t.decisionSave} title={t.decisionSave} onclick={() => onDecision(text)}><svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z" /></svg></button>{/if}
    {#if at}<span class="time">{hm(at)}</span>{/if}
  </div>
{/snippet}

{#snippet table(tb: Table)}
  {#if compact(tb)}
    <div class="grid" style="--n:{tb.header.length}">
      {#each tb.header as h}<span class="th"><Md text={h} /></span>{/each}
      {#each tb.rows as r}{#each r as c}<span class="td"><Md text={c} /></span>{/each}{/each}
    </div>
  {:else}
    <div class="cards">
      {#each cards(tb) as c}
        <div class="tcard">
          {#if c.title.trim()}<b><Md text={c.title} /></b>{/if}
          {#each c.lines as line}{@const i = line.indexOf(': ')}<span><span class="k">{`${i < 0 ? line : line.slice(0, i)}: `}</span><Md text={i < 0 ? '' : line.slice(i + 2)} /></span>{/each}
        </div>
      {/each}
    </div>
  {/if}
{/snippet}

{#snippet claude(e: TranscriptEntry)}
  {@const text = parseSteps(e.text ?? '').text}
  {@const shown = outcomeForPhone(text, t.outcomeLabel)}
  {@const c = cost(e)}
  <div class="claude">
    <div class="ctext">
      {#each blocks(shown) as b}
        {#if b.type === 'text'}<p><Md text={b.text} /></p>{:else}{@render table(b)}{/if}
      {/each}
      {#if e.cut}<span class="meta">{t.textCut}</span>{/if}
      {#if c}<span class="cost">{c}</span>{/if}
    </div>
    {@render actions(text, e.at)}
  </div>
{/snippet}

{#snippet files(fs: TranscriptFile[])}
  <div class="files">
    {#each fs as f}
      {@const url = media[mediaKey(s.name, f.path)]}
      <div class="fcard" role="group" aria-label={f.path.split('/').pop()} use:preview={f} onpointerenter={() => onFile(f.path, 'prepare')} onfocusin={() => onFile(f.path, 'prepare')}>
        <button class="fchip" title={f.path} onclick={() => onFile(f.path, 'open')}><Icon name={fileIcon(f)} color="var(--icon)" /><span class="fname">{f.path.split('/').pop()}</span>{#if f.size != null}<span class="fsize">{size(f.size)}</span>{/if}</button>
        <div class="acts">{#each fileActs as [a, label, icon]}<button class="sm" aria-label={label} title={label} onclick={() => onFile(f.path, a)}><Icon name={icon} /></button>{/each}</div>
        {#if url && isImage(f.mime)}<button class="fthumb" aria-label={t.fileOpen} onclick={() => onFile(f.path, 'open')}><img src={url} alt="" /></button>{/if}
        {#if url && isVideo(f.mime)}<!-- svelte-ignore a11y_media_has_caption --><video class="fvideo" src={url} controls preload="metadata" playsinline></video>{/if}
      </div>
    {/each}
  </div>
{/snippet}

{#snippet tool(e: TranscriptEntry, withFiles: boolean)}
  {@const [main, detail] = row(e.tool, e.text, e.note)}
  <div class="tool">
    <div class="trow">
      <Icon name={toolIcon(e)} color={e.error ? 'var(--gone)' : 'var(--text2)'} label={e.tool ?? undefined} />
      <span class="tcol"><span class:bad={e.error}>{main}</span>{#if detail}<code>{detail}</code>{/if}</span>
    </div>
    {#if withFiles && e.files?.length}<div class="indent">{@render files(e.files)}</div>{/if}
  </div>
{/snippet}

{#snippet steps(g: Extract<Item, { type: 'steps' }>)}
  {@const id = g.entries[0].id}
  {@const failed = g.entries.filter(e => e.error).length}
  {@const cs = counts(g)}
  {@const last = g.entries[g.entries.length - 1]}
  {@const fs = g.entries.flatMap(e => e.files ?? [])}
  <div class="steps" class:fail={failed > 0}>
    <button class="shead" aria-expanded={!!open[id]} onclick={() => (open[id] = !open[id])}>
      <span class="ssum">{[t.stepsCount(g.entries.length), cs[0] ? `${cs[0][1]} ${cs[0][0]}` : null, cs.length > 1 ? `+${cs.length - 1}` : null].filter(Boolean).join(' · ')}{#if failed}{' · '}<b class="sfail">{t.stepsFailed(failed)}</b>{/if}</span>
      <span class="dots">{#each g.entries.slice(0, 12) as e}<i class:bad={e.error}></i>{/each}{#if g.entries.length > 12}<span>+{g.entries.length - 12}</span>{/if}</span>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d={open[id] ? 'M7 14l5-5 5 5' : 'M7 10l5 5 5-5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
    </button>
    {#if open[id]}
      {#each g.entries as e (e.id)}{@render tool(e, false)}{/each}
    {:else}
      {@const [main] = row(last.tool, last.text, last.note)}
      <div class="slast"><Icon name={toolIcon(last)} size={16} color={last.error ? 'var(--gone)' : 'var(--text2)'} /><span class:bad={last.error}>{main}</span></div>
    {/if}
    {#if fs.length}{@render files(fs)}{/if}
  </div>
{/snippet}

{#snippet bubble(text: string, low: boolean)}
  <div class="bubble" class:low><p><Md {text} /></p></div>
{/snippet}

{#each items as it (it.type === 'steps' ? `s-${it.entries[0].id}` : it.type === 'mine' ? `m-${it.sent.id}` : `${it.type[0]}-${it.entry.id}`)}
  {#if it.type === 'mine'}
    {@const [ic, tone] = status[it.status]}
    <div class="me">
      {#if it.sent.attachment}<span class="att"><Icon name="file" size={16} />{it.sent.attachment}</span>{/if}
      {#if it.sent.text}{@render bubble(it.sent.text, false)}{/if}
      <div class="mrow">
        <span class="mark" style="color:{tone}"><Icon name={ic} size={16} color={tone} />{it.status === 'scheduled' && it.sent.scheduledFor ? t.chatStatus.scheduled(hm(it.sent.scheduledFor)) : t.chatStatus[it.status]('')}</span>
        {#if it.status !== 'scheduled'}<span class="time">{hm(it.sent.sentAt)}</span>{/if}
        <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(it.sent.text)}><Icon name="copy" /></button>
      </div>
    </div>
  {:else if it.type === 'user'}
    <div class="me">
      {@render bubble(it.entry.text ?? '', true)}
      <div class="mrow">
        {#if it.entry.origin && t.origin[it.entry.origin]}<span class="meta">{t.origin[it.entry.origin]}</span>{/if}
        {#if it.entry.queued}<span class="meta stale">· {t.chatStatus.queued('')}</span>{/if}
        {#if it.entry.at}<span class="time">{hm(it.entry.at)}</span>{/if}
        <button class="sm" aria-label={t.copy} title={t.copy} onclick={() => copy(it.entry.text ?? '')}><Icon name="copy" /></button>
      </div>
    </div>
  {:else if it.type === 'claude'}
    {@render claude(it.entry)}
  {:else if it.type === 'tool'}
    {@render tool(it.entry, true)}
  {:else}
    {@render steps(it)}
  {/if}
{/each}

{#if live}
  <div class="live">
    <svg class="star" viewBox="0 0 18 18" width="18" height="18" aria-hidden="true">
      {#each Array(8) as _, i}{@const a = (i * Math.PI) / 4}<line x1={9 + Math.cos(a) * 2.2} y1={9 + Math.sin(a) * 2.2} x2={9 + Math.cos(a) * 8.5} y2={9 + Math.sin(a) * 8.5} />{/each}
    </svg>
    <span>{liveText}</span>
  </div>
{/if}

<style>
  p { white-space: pre-wrap; overflow-wrap: anywhere; }
  .me { align-self: stretch; padding-left: 40px; display: flex; flex-direction: column; align-items: flex-end; }
  .att { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; margin-bottom: 4px; border-radius: 14px; background: var(--surface); color: var(--text2); font-size: 14px; overflow-wrap: anywhere; }
  .bubble { background: var(--high); border-radius: 20px 20px 6px 20px; padding: 12px 16px; font-size: 16px; letter-spacing: .03em; max-width: 760px; }
  .bubble.low { background: var(--surface); }
  .mrow { display: flex; align-items: center; gap: 2px; }
  .mark { display: flex; align-items: center; gap: 4px; font-size: 12.5px; font-weight: 500; }
  .time { font-size: 12.5px; color: var(--text2); padding: 0 4px; font-weight: 500; }
  .meta { font-size: 12.5px; color: var(--text2); font-weight: 500; }
  .stale { color: var(--stale); }
  .sm { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .sm:hover { background: var(--surface); }
  .claude { display: flex; flex-direction: column; max-width: 760px; }
  .ctext { padding: 4px; display: flex; flex-direction: column; gap: 10px; font-size: 16px; line-height: 1.5; letter-spacing: .03em; }
  .cost { font-size: 12.5px; font-weight: 500; color: var(--b-second); }
  .acts { display: flex; align-items: center; gap: 2px; }
  .play { width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; background: var(--high); color: var(--icon); margin-left: 2px; }
  .play:hover, .fchip:hover { filter: brightness(1.15); }
  .grid { display: grid; grid-template-columns: repeat(var(--n), 1fr); }
  .th, .td { padding-right: 12px; }
  .th { font-size: 14px; font-weight: 600; color: var(--text2); padding: 6px 0; }
  .td { font-size: 14.5px; padding: 8px 0; border-top: 1px solid var(--line); }
  .cards { display: flex; flex-direction: column; gap: 8px; }
  .tcard { background: var(--surface); border-radius: 12px; padding: 10px 14px; display: flex; flex-direction: column; gap: 3px; font-size: 14.5px; }
  .tcard b { font-size: 16px; font-weight: 600; }
  .k { color: var(--text2); }
  .tool { display: flex; flex-direction: column; gap: 6px; padding-left: 4px; }
  .trow { display: flex; gap: 10px; align-items: flex-start; }
  .trow :global(svg) { margin-top: 2px; }
  .tcol { display: flex; flex-direction: column; gap: 2px; font-size: 14.5px; min-width: 0; }
  .bad { color: var(--gone-dim); }
  code { font: 12.5px/1.4 var(--mono); color: var(--stale); white-space: pre-wrap; overflow-wrap: anywhere; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
  .indent { padding-left: 28px; }
  .files { display: flex; flex-wrap: wrap; gap: 8px; }
  .fcard { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; max-width: 100%; }
  .fchip { display: flex; align-items: center; gap: 8px; background: var(--surface); border-radius: 12px; padding: 8px 12px; text-align: left; max-width: 100%; }
  .fthumb { padding: 0; border-radius: 14px; overflow: hidden; background: none; }
  .fthumb img { display: block; max-width: 240px; max-height: 180px; object-fit: cover; }
  .fvideo { display: block; max-width: min(100%, 360px); max-height: 320px; border-radius: 14px; background: #000; }
  .steps .fchip { background: rgb(255 255 255 / .06); }
  .fname { font-size: 14px; font-weight: 500; overflow-wrap: anywhere; }
  .fsize { font-size: 12.5px; color: var(--text2); white-space: nowrap; }
  .steps { background: var(--steps-bg); border-radius: 14px; padding: 6px 4px 8px 12px; display: flex; flex-direction: column; gap: 4px; }
  .steps.fail { background: var(--steps-fail); }
  .shead { display: flex; align-items: center; width: 100%; text-align: left; border-radius: 8px; }
  .ssum { flex: 1; min-width: 0; font-size: 14px; font-weight: 500; color: var(--text2); white-space: nowrap; overflow: hidden; }
  .sfail { color: var(--b-alert); font-weight: 600; }
  .dots { display: flex; gap: 3px; align-items: center; padding: 0 4px 0 8px; font-size: 11px; color: var(--text2); }
  .dots i { width: 5px; height: 5px; border-radius: 50%; background: var(--steps-dot); }
  .dots i.bad { background: var(--b-alert); }
  .slast { display: flex; align-items: center; gap: 8px; font-size: 14.5px; white-space: nowrap; overflow: hidden; }
  .steps .tool { padding: 4px 0; }
  .live { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 14.5px; color: var(--text2); }
  .star { stroke: var(--opus); stroke-width: 2; stroke-linecap: round; animation: spin 2.4s linear infinite, pulse 1.4s ease-in-out infinite alternate; }
  @keyframes spin { to { rotate: 360deg; } }
  @keyframes pulse { from { scale: .75; opacity: .6; } to { scale: 1.1; opacity: 1; } }
  @media (prefers-reduced-motion: reduce) { .star { animation: pulse 1.2s ease-in-out infinite alternate; } }
</style>
