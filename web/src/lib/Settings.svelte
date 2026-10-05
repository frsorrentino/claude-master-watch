<script lang="ts">
  import type { DevicesModel, Linked, Tone } from './devices'
  import { RATE_CHOICES } from './speechRules'
  import { speech, toggle } from './speech.svelte'
  import { t } from './t'

  // Impostazioni (SettingsScreen dell'app, mockup A): sezioni con l'etichetta e il filo. Collegamento: lo schema dei
  // dispositivi da toccare (schema B, contratto 1.32); Lettura ad alta voce: voce e velocità; Informazioni: la versione.
  let { m, devices, now, channel, onRate, onVoice, timeZone, details = null, onDetails = () => {} }: {
    m: DevicesModel; devices: Linked[]; now: number; channel: string
    onRate: (r: number) => void; onVoice: (v: string | null) => void; timeZone?: string
    /** Sulla plancia: i dettagli della sessione accanto alle colonne, spenti di default; null altrove. */
    details?: boolean | null; onDetails?: (on: boolean) => void
  } = $props()

  const PC = 'pc'
  let selected = $state(PC)
  let voiceDlg: HTMLDialogElement | undefined = $state()
  const toneColor = (x: Tone) => (x === 'live' ? 'var(--b-good)' : x === 'stale' ? 'var(--wait)' : 'var(--b-track)')
  const kindLabel = (k: string | null) => ({ phone: t.devPhone, watch: t.devWatch, tablet: t.devTablet, chromebook: t.devChromebook } as Record<string, string>)[k ?? ''] ?? t.devOther
  const ICONS: Record<string, string[]> = {
    phone: ['M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z', 'M12 18h.01'],
    watch: ['M12 9v3l1.5 1.5', 'M16.51 17.35l-.35 3.83a2 2 0 0 1-2 1.82H9.83a2 2 0 0 1-2-1.82l-.35-3.83m.01-10.7.35-3.83A2 2 0 0 1 9.83 1h4.35a2 2 0 0 1 2 1.82l.35 3.83', 'M18 12a6 6 0 1 1-12 0 6 6 0 0 1 12 0'],
    tablet: ['M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z', 'M12 18h.01'],
    chromebook: ['M20 16V7a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v9m16 0H4m16 0 1.28 2.55a1 1 0 0 1-.9 1.45H3.62a1 1 0 0 1-.9-1.45L4 16'],
    pc: ['M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z', 'M8 21h8', 'M12 17v4'],
    other: ['M10 4H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h4', 'M14 8h6a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-6a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z', 'M4 20h6'],
  }
  const icon = (k: string | null) => ICONS[k ?? ''] ?? ICONS.other
  function seenShort(seen: number | null) {
    if (seen == null) return t.devSeenNever
    const s = Math.max(0, now - seen)
    return s < 120 ? t.devSeenNow : s < 3600 ? `${Math.floor(s / 60)} min` : s < 86400 ? `${Math.floor(s / 3600)} h` : `${Math.floor(s / 86400)} g`
  }
  const dayTime = (at: number) => new Date(at * 1000).toLocaleString('it-IT', { weekday: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', timeZone }).replace(',', '')
  const anyLive = $derived(devices.some(d => d.tone !== 'off'))
  const pick = $derived(devices.find(d => d.uid === selected) ?? null)
  const rate = (r: number) => `${r.toLocaleString('it-IT', { maximumFractionDigits: 2 })}×`
</script>

{#snippet node(kind: string, size: number, dot: string, ring: string, dashed: boolean, name: string, status: string, onClick: () => void)}
  <button class="node" style="--s:{size}px" onclick={onClick} aria-label={t.devDetails(name)}>
    <span class="circle" class:dashed style="border-color:{dashed ? 'transparent' : ring}">
      <svg viewBox="0 0 24 24" width={size * 0.42} height={size * 0.42} fill="none" stroke={dashed ? 'var(--stale)' : 'var(--icon)'} stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{#each icon(kind) as d}<path {d} />{/each}</svg>
      {#if !dashed}<i class="dot" style="background:{dot}"></i>{/if}
    </span>
    <b class:dim={dashed}>{name}</b>
    {#if status}<small>{status}</small>{/if}
  </button>
{/snippet}

<div class="settings">
  <div class="gh"><span>{t.secLink.toUpperCase()}</span><i></i></div>
  <div class="scheme">
    <div class="pc">{@render node('pc', 84, toneColor(m.pc.tone), selected === PC ? 'var(--icon)' : 'var(--line)', false, m.pc.host ?? t.devPc,
      [m.pc.ageMinutes != null ? t.devUpdatedAgo(m.pc.ageMinutes) : t.devUpdatedNow, t.devCount(devices.length)].join(' · '), () => (selected = PC))}</div>
    {#if devices.length}
      <svg class="wires" viewBox="0 0 {devices.length * 100} 34" preserveAspectRatio="none" height="34">
        <line x1={devices.length * 50} y1="0" x2={devices.length * 50} y2="12" stroke={toneColor(anyLive ? 'live' : 'off')} stroke-width="2" vector-effect="non-scaling-stroke" />
        {#if devices.length > 1}<line x1="50" y1="12" x2={devices.length * 100 - 50} y2="12" stroke={toneColor(anyLive ? 'live' : 'off')} stroke-width="2" vector-effect="non-scaling-stroke" />{/if}
        {#each devices as d, i}<line x1={i * 100 + 50} y1="12" x2={i * 100 + 50} y2="34" stroke={toneColor(d.tone)} stroke-width="2" stroke-dasharray={d.tone === 'off' ? '8 6' : undefined} vector-effect="non-scaling-stroke" />{/each}
      </svg>
      <div class="row" style="grid-template-columns: repeat({devices.length}, 1fr)">
        {#each devices as d (d.uid)}
          {@render node(d.kind ?? 'other', 60, toneColor(d.tone), d.self || selected === d.uid ? 'var(--icon)' : 'var(--line)', d.tone === 'off', kindLabel(d.kind), d.self ? t.devThis : seenShort(d.seen), () => (selected = d.uid))}
        {/each}
      </div>
    {/if}
    <div class="card">
      {#if pick}
        {@const chip = pick.self ? t.devThis : pick.seen == null ? t.devSeenNeverLong : t.devSeenAgo(seenShort(pick.seen))}
        <div class="ctop"><b>{pick.name}</b><span class="chip" style="color:{toneColor(pick.tone)};background:color-mix(in srgb, {toneColor(pick.tone)} 14%, transparent)">{chip}</span></div>
        <div class="fact"><span>{t.factKind}</span><span>{kindLabel(pick.kind)}</span></div>
        <div class="fact"><span>{t.factSeen}</span><span>{pick.seen != null ? dayTime(pick.seen) : t.devSeenNeverLong}</span></div>
        <div class="fact"><span>{t.factPairedTo}</span><span>{m.pc.host ?? '–'}</span></div>
      {:else}
        <div class="ctop"><b>{m.pc.host ?? t.devPc}</b><span class="chip" style="color:{toneColor(m.pc.tone)};background:color-mix(in srgb, {toneColor(m.pc.tone)} 14%, transparent)">{m.pc.tone === 'live' ? t.chipConnected : t.quotaOldWord}</span></div>
        <div class="fact"><span>{t.factUpdated}</span><span>{m.pc.ageMinutes != null ? t.devUpdatedAgo(m.pc.ageMinutes) : t.factUpdatedNow}</span></div>
        <div class="fact"><span>{t.factOpen}</span><span>{m.pc.open}</span></div>
        {#each m.pc.accounts as a}
          {@const q = a.pct != null ? t.quota5h(a.pct) : null}
          <div class="fact"><span>{a.account}</span><span>{q == null ? (a.stale ? t.quotaOldWord : t.factQuotaNone) : a.stale ? t.factQuotaStale(q) : q}</span></div>
        {/each}
        <div class="fact"><span>{t.factChannel}</span><span>{channel}</span></div>
      {/if}
    </div>
    <p class="hint">{t.devHint}</p>
  </div>

  {#if details != null}
    <div class="sec">
      <label class="srow">
        <span class="sic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><path d="M12 16v-4M12 8h.01" /></svg></span>
        <span class="st"><b>{t.tabletDetails}</b><small>{t.tabletDetailsSub}</small></span>
        <input class="switch" type="checkbox" role="switch" checked={details} onchange={(e) => onDetails((e.currentTarget as HTMLInputElement).checked)} />
      </label>
    </div>
  {/if}

  <div class="gh"><span>{t.secReading.toUpperCase()}</span><i></i></div>
  <div class="sec">
    <button class="srow" onclick={() => voiceDlg?.showModal()}>
      <span class="sic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><path d="M2 10v3M6 6v11M10 3v18M14 8v7M18 5v13M22 10v3" /></svg></span>
      <span class="st"><b>{t.voice}</b><small>{speech.voice ?? speech.voices[0] ?? t.voiceDefault}</small></span>
      <svg viewBox="0 0 24 24" width="22" height="22"><path d="M10 7l5 5-5 5" fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
    <div class="srow plain">
      <span class="sic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><path d="m12 14 4-4M3.34 19a10 10 0 1 1 17.32 0" /></svg></span>
      <span class="st"><b>{t.speechRate}</b><small>{t.rateHint}</small></span>
    </div>
    <div class="rates">
      {#each RATE_CHOICES as r}<button class="rate" class:on={r === speech.rate} aria-pressed={r === speech.rate} onclick={() => onRate(r)}>{rate(r)}</button>{/each}
    </div>
  </div>

  <div class="gh"><span>{t.secInfo.toUpperCase()}</span><i></i></div>
  <div class="sec">
    <div class="srow plain">
      <span class="sic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10" /><path d="M12 16v-4M12 8h.01" /></svg></span>
      <span class="st"><b>{t.versionTitle}</b><small>{t.webVersion(__APP_VERSION__)}</small></span>
    </div>
  </div>
</div>

<dialog bind:this={voiceDlg} onclick={(e) => e.target === e.currentTarget && voiceDlg?.close()}>
  <h3>{t.voice}</h3>
  <div class="voices">
    {#each [null, ...speech.voices] as v}
      <label class="radio"><input type="radio" name="voice" checked={v === speech.voice} onchange={() => onVoice(v)} />{v ?? t.voiceDefault}</label>
    {/each}
  </div>
  <div class="btns">
    <button class="link" onclick={() => toggle(t.voiceSample)}>{t.voiceTry}</button>
    <button class="link" onclick={() => voiceDlg?.close()}>{t.closeWord}</button>
  </div>
</dialog>

<style>
  .settings { display: flex; flex-direction: column; gap: 14px; padding: 0 16px 24px; max-width: 760px; }
  .gh { display: flex; align-items: center; gap: 10px; margin: 6px 8px 0; font: 12px var(--mono); letter-spacing: .06em; color: var(--text2); }
  .gh i { flex: 1; height: 1px; background: color-mix(in srgb, var(--text2) 25%, transparent); }
  .scheme { border-radius: 24px; background: var(--low); padding: 20px 10px 16px; display: flex; flex-direction: column; gap: 16px;
    background-image: radial-gradient(rgb(255 255 255 / .07) 1px, transparent 1.4px); background-size: 16px 16px; }
  .pc { display: flex; justify-content: center; }
  .pc .node { width: 260px; }
  .wires { width: 100%; display: block; margin-bottom: -16px; }
  .row { display: grid; container-type: inline-size; }
  /* In fila stretta un nome lungo («Chromebook») scende di corpo finché entra, invece di tagliarsi. */
  .row .node b { font-size: clamp(9px, 2.9cqi, 13px); }
  .row .node small { font-size: clamp(9px, 2.6cqi, 12px); }
  .node { display: flex; flex-direction: column; align-items: center; gap: 6px; min-width: 0; }
  .circle { position: relative; width: var(--s); height: var(--s); border-radius: 50%; background: var(--surface); border: 2px solid; display: grid; place-items: center; }
  .circle.dashed { background: none; outline: 2px dashed var(--b-track); outline-offset: -2px; }
  .dot { position: absolute; top: 3px; right: 3px; width: 14px; height: 14px; border-radius: 50%; border: 3px solid var(--low); }
  .node b { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; max-width: 100%; }
  .pc .node b { font-size: 16px; }
  .node b.dim { color: var(--text2); }
  .node small { font-size: 12px; color: var(--text2); white-space: nowrap; overflow: hidden; max-width: 100%; }
  .card { background: var(--surface); border-radius: 20px; padding: 16px; display: flex; flex-direction: column; gap: 12px; }
  .ctop { display: flex; align-items: center; gap: 12px; }
  .ctop b { flex: 1; font-size: 16px; font-weight: 600; }
  .chip { border-radius: 12px; padding: 4px 10px; font-size: 12.5px; font-weight: 500; }
  .fact { display: flex; gap: 16px; font-size: 14.5px; }
  .fact span:first-child { color: var(--text2); }
  .fact span:last-child { flex: 1; text-align: right; }
  .hint { text-align: center; font-size: 12.5px; color: var(--text2); }
  .sec { background: var(--low); border-radius: 20px; padding: 6px 0; display: flex; flex-direction: column; }
  .srow { display: flex; align-items: center; gap: 14px; min-height: 64px; padding: 10px 16px; text-align: left; width: 100%; }
  button.srow:hover { background: var(--surface); }
  .sic { width: 40px; height: 40px; border-radius: 50%; background: var(--surface); display: grid; place-items: center; flex: none; }
  .st { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
  .st b { font-size: 16px; font-weight: 500; }
  .st small { font-size: 12.5px; color: var(--text2); }
  label.srow { cursor: pointer; }
  label.srow:hover { background: var(--surface); }
  .switch { appearance: none; width: 52px; height: 32px; border-radius: 16px; background: var(--high); border: 2px solid var(--stale); position: relative; cursor: pointer; flex: none; transition: background .15s; }
  .switch::after { content: ''; position: absolute; top: 6px; left: 6px; width: 16px; height: 16px; border-radius: 50%; background: var(--stale); transition: all .15s; }
  .switch:checked { background: var(--icon); border-color: var(--icon); }
  .switch:checked::after { left: 24px; top: 2px; width: 24px; height: 24px; background: var(--on-primary); }
  .rates { display: flex; flex-wrap: wrap; gap: 6px; padding: 0 16px 14px 70px; }
  .rate { min-width: 44px; height: 36px; padding: 0 10px; border-radius: 18px; background: var(--low); border: 1px solid var(--line); font-size: 14px; font-weight: 500; }
  .rate.on { background: var(--icon); border-color: var(--icon); color: var(--on-primary); }
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 20px 22px; width: min(440px, 92vw); max-height: 80vh; border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  dialog h3 { font-size: 22px; font-weight: 600; margin-bottom: 8px; }
  .voices { display: flex; flex-direction: column; max-height: 50vh; overflow-y: auto; }
  .radio { display: flex; align-items: center; gap: 12px; padding: 10px 4px; cursor: pointer; font-size: 15px; }
  .radio input { width: 20px; height: 20px; accent-color: var(--primary); margin: 0; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
  .link { color: var(--icon); padding: 10px 14px; border-radius: 20px; font-weight: 500; }
  .link:hover { background: var(--high); }
</style>
