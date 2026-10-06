<script lang="ts">
  import { t } from './t'
  // Il campo della master sotto la sua barra anche da chiusa (Franz, 06/10 10:45): Invio manda, Maiusc+Invio va a capo, e
  // chi lo usa vede la conversazione aprirsi con il messaggio appena mandato.
  let { onSend }: { onSend: (text: string) => void } = $props()
  let text = $state('')
  let area = $state<HTMLTextAreaElement | null>(null)
  function send() {
    const x = text.trim()
    if (!x) return
    text = ''
    onSend(x)
  }
  // Cresce con il testo fino a quattro righe.
  $effect(() => { text; if (area) { area.style.height = 'auto'; area.style.height = Math.min(area.scrollHeight, 4 * 22 + 16) + 'px' } })
</script>

<form class="quick" onsubmit={(e) => { e.preventDefault(); send() }}>
  <textarea bind:this={area} bind:value={text} rows="1" placeholder={t.masterPlaceholder} aria-label={t.masterPlaceholder}
    onkeydown={(e) => { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); send() } }}></textarea>
  <button type="submit" class="round" class:on={!!text.trim()} disabled={!text.trim()} aria-label={t.send}>
    <svg viewBox="0 0 24 24" width="20" height="20"><path d="M3.4 20.4 21 12 3.4 3.6 3.4 10.2 15 12 3.4 13.8z" fill="currentColor" /></svg>
  </button>
</form>

<style>
  .quick { display: flex; align-items: flex-end; gap: 4px; margin: 0 12px 12px; padding: 4px 4px 4px 14px; border: 2px solid rgb(217 119 87 / .75); border-radius: 24px; background: var(--bg); }
  .quick:focus-within { border-color: var(--opus); }
  textarea { flex: 1; min-width: 0; resize: none; border: 0; outline: none; background: none; color: var(--text); font: inherit; line-height: 22px; padding: 8px 0; max-height: 104px; }
  textarea::placeholder { color: var(--text2); }
  .round { flex: none; width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; color: var(--text2); }
  .round.on { background: var(--opus); color: #1A0F0A; }
</style>
