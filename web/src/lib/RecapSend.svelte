<script lang="ts">
  import type { RecapAction } from './recapActions'
  import { t } from './t'
  // «Mandare questa azione?» (tavola 3): da dove viene, il testo che parte, a chi va. Un solo tasto pieno, «Manda».
  // Si apre quando `action` arriva; Annulla, il fondo o Esc lo chiudono con `onClose`.
  let { action, day, onClose, onSend }: { action: RecapAction | null; day: string; onClose: () => void; onSend: (a: RecapAction) => void } = $props()
  let dlg: HTMLDialogElement | undefined = $state()
  $effect(() => { if (action && dlg && !dlg.open) dlg.showModal(); else if (!action && dlg?.open) dlg.close() })
</script>

<dialog bind:this={dlg} class="sheet" onclose={onClose} onclick={(e) => e.target === e.currentTarget && dlg?.close()}>
  {#if action}
    <h3>{t.recapSendTitle}</h3>
    <p class="sub">{action.agenda ? t.recapSendFromAgenda : action.recap ? t.recapSendFromRecap(day, action.from) : t.recapSendFromSession(action.from)}</p>
    <p class="what">{action.send}</p>
    <p class="sub">{action.viaMaster ? t.recapGoesMaster : t.recapGoes} <b>{action.to}</b>{action.agenda ? ', ' + t.recapSendToMasterAgenda : action.viaMaster ? ': ' + t.recapSendToMaster : ', ' + t.recapSendToSession}</p>
    <div class="btns">
      <button class="text2" onclick={() => dlg?.close()}>{t.cancel}</button>
      <button class="filled" onclick={() => { const a = action!; onSend(a); dlg?.close() }}>{t.recapSend}</button>
    </div>
  {/if}
</dialog>

<style>
  dialog { margin: auto; border: 0; outline: none; color: var(--text); background: var(--surface); padding: 22px; width: min(420px, 92vw); border-radius: 28px; }
  dialog::backdrop { background: rgb(0 0 0 / .55); }
  /* Sul telefono il foglio sale dal basso, come quello dell'app. */
  @media (max-width: 600px) { dialog { margin: auto 0 0; width: 100%; max-width: none; border-radius: 28px 28px 0 0; } }
  h3 { font-size: 22px; font-weight: 600; }
  .sub { color: var(--text2); font-size: 14px; margin-top: 8px; }
  .sub b { color: var(--text); font-weight: 600; }
  .what { margin-top: 14px; padding: 14px 18px; border-radius: 16px; background: var(--low); font-size: 16px; }
  .btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
  .btns button { padding: 10px 18px; border-radius: 20px; font-weight: 500; }
  .text2 { color: var(--icon); }
  .filled { background: var(--primary); color: var(--on-primary); }
</style>
