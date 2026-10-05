<script lang="ts">
  import { t } from './t'
  // Un box sopra il campo, «Prossimi» o «Ricorrenti» (PromptBox dell'app): una riga per voce; il clic la porta nel campo,
  // a campo vuoto ↗ la manda subito, con del testo nel campo + la accoda. L'intestazione apre e chiude; chiuso resta una
  // riga col numero delle voci, e si apre verso l'alto.
  export type Row = { label: string; text: string; direct: boolean }
  let { title, rows, open, onOpen, draftBlank, onPick, onSend }: {
    title: string; rows: Row[]; open: boolean; onOpen: (open: boolean) => void; draftBlank: boolean
    onPick: (r: Row) => void; onSend: (r: Row) => void
  } = $props()
</script>

<div class="box">
  <button class="head" aria-expanded={open} aria-label={open ? t.boxClose(title) : t.boxOpen(title)} onclick={() => onOpen(!open)}>
    <span class="mono">{title.toUpperCase()} · {rows.length}</span>
    <svg viewBox="0 0 24 24" width="20" height="20"><path d={open ? 'M7 10l5 5 5-5' : 'M7 14l5-5 5 5'} fill="none" stroke="var(--text2)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg>
  </button>
  {#if open}
    {#each rows as r (r.text)}
      <div class="row">
        <button class="label" onclick={() => onPick(r)}>{r.label}</button>
        {#if draftBlank && r.direct}
          <button class="act" aria-label={t.send} title={t.send} onclick={() => onSend(r)}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M7 17 17 7M8 7h9v9" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
        {:else}
          <button class="act" aria-label={t.boxAppend} title={t.boxAppend} onclick={() => onPick(r)}><svg viewBox="0 0 24 24" width="20" height="20"><path d="M12 5v14M5 12h14" fill="none" stroke="var(--icon)" stroke-width="2" stroke-linecap="round" /></svg></button>
        {/if}
      </div>
    {/each}
  {/if}
</div>

<style>
  .box { margin: 0 12px; background: var(--low); border: 1px solid var(--line); border-radius: 18px; overflow: hidden; }
  .head { width: 100%; display: flex; align-items: center; padding: 8px 12px 8px 14px; }
  .head .mono { flex: 1; text-align: left; }
  .row { display: flex; align-items: center; border-top: 1px solid var(--line); }
  .label { flex: 1; text-align: left; padding: 10px 0 10px 14px; font-size: 16px; letter-spacing: .03em; }
  .act { width: 48px; height: 44px; display: grid; place-items: center; }
  .head:hover, .row:hover { background: rgb(255 255 255 / .03); }
</style>
