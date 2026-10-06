<script lang="ts">
  import type { Session } from './contract'
  import { closeStateOf } from './masterService'
  import { t } from './t'
  // La domanda prima di chiudere una sessione, uguale per il «Chiudi» della pulizia, «Chiudi la sessione» del menu e /exit
  // scritto nel campo (Franz, 06/10 10:57, mockup approvato 12:25): il nome nel titolo e nel tasto, lo stato, cosa succede.
  let { onConfirm }: { onConfirm: (name: string) => void } = $props()
  let dlg: HTMLDialogElement | undefined = $state()
  let s = $state<Session | null>(null)
  export function ask(session: Session) { s = session; dlg?.showModal() }
  const hm = (at: number) => new Date(at * 1000).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' })
  const line = $derived.by(() => {
    if (!s) return ''
    const c = closeStateOf(s)
    if (c.kind === 'working') return t.closeWorking(s.name)
    if (c.kind === 'duplicate') return t.closeDuplicate(s.name, c.of ?? '')
    if (c.kind === 'finished') return c.at ? t.closeFinishedAt(s.name, hm(c.at)) : t.closeFinished(s.name)
    return t.closeStill(s.name)
  })
</script>

<dialog bind:this={dlg} onclick={(e) => e.target === e.currentTarget && dlg?.close()}>
  {#if s}
    <h3>{t.closeTitle(s.name)}</h3>
    <p>{line}</p>
    <ul>{#each t.closeWhat as w}<li>{w}</li>{/each}</ul>
    <div class="btns">
      <button class="keep" onclick={() => dlg?.close()}>{t.closeKeep}</button>
      <button class="danger" onclick={() => { const n = s!.name; dlg?.close(); onConfirm(n) }}>{t.closeOk(s.name)}</button>
    </div>
  {/if}
</dialog>

<style>
  dialog { margin: auto; border: 0; color: var(--text); background: var(--surface); padding: 22px 24px 14px; width: min(420px, 92vw); border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .6); }
  h3 { font-size: 22px; font-weight: 500; margin: 0 0 10px; }
  p { color: var(--text2); font-size: 14px; margin: 0 0 8px; }
  ul { margin: 4px 0 6px; padding-left: 18px; color: var(--text2); font-size: 14px; }
  li { margin: 3px 0; }
  .btns { display: flex; justify-content: flex-end; gap: 6px; margin-top: 12px; }
  .btns button { padding: 10px 16px; border-radius: 999px; font-weight: 500; font-size: 14px; }
  .keep { color: var(--icon); }
  .keep:hover { background: var(--high); }
  .danger { background: var(--gone); color: #fff; }
  .danger:hover { filter: brightness(1.1); }
</style>
