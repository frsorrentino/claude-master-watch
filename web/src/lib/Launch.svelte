<script lang="ts">
  import type { Project, State } from './contract'
  import { isPersonalQuota, personal, ranked, recent, sessions as namedSessions } from './launch'
  import { t } from './t'
  import AccountMark from './AccountMark.svelte'
  import Badge from './Badge.svelte'

  // Il foglio «Lancia» (LaunchSheet dell'app), usato anche per «Aggiungi alla notte» (`action`): gli account in testa sono
  // un filtro facoltativo; sotto il campo una lista, a campo vuoto i recenti, scrivendo prima le sessioni con quel nome e
  // poi i progetti, la parte trovata in grassetto; il primo messaggio; il tasto.
  let { st, action = t.launch, onSession = null, onLaunch }: {
    st: State; action?: string
    /** Una sessione trovata per nome: aperta = la sua scheda, chiusa = «Riapri». Null = solo progetti (la notte). */
    onSession?: ((name: string, reopen: boolean) => void) | null
    onLaunch: (p: Project, first: string) => void
  } = $props()

  const accountPersonal = (a: string) => (st.quota[a] ? isPersonalQuota(a, st.quota[a]) : personal(a, null))
  const accounts = $derived([...new Set([...Object.keys(st.quota), ...st.projects.map(p => p.account)])]
    .sort((a, b) => Number(!accountPersonal(a)) - Number(!accountPersonal(b)) || a.localeCompare(b)))
  let filter = $state<string | null>(null)
  let typed = $state('')
  let chosen = $state<Project | null>(null)
  let first = $state('')
  const found = $derived(typed.trim() ? ranked(st, typed, { account: filter }) : recent(st, filter))
  const named = $derived(!onSession || chosen ? [] : namedSessions(st, typed))
  function parts(name: string) {
    const q = typed.trim()
    const i = q ? name.toLowerCase().indexOf(q.toLowerCase()) : -1
    return i < 0 ? [name, '', ''] : [name.slice(0, i), name.slice(i, i + q.length), name.slice(i + q.length)]
  }
  const folder = (p: Project) => p.path.split('/').slice(0, -1).pop() ?? ''
</script>

<div class="launch">
  {#if accounts.length > 1}
    <div class="chips">
      {#each accounts as a}
        <button class="chip" class:on={filter === a} aria-pressed={filter === a} onclick={() => { filter = filter === a ? null : a; chosen = null }}>
          <AccountMark personal={accountPersonal(a)} size={12} color={filter === a ? 'var(--on-primary)' : 'var(--text2)'} />{a}
        </button>
      {/each}
    </div>
  {/if}
  <label class="field"><span>{t.launchProject}</span>
    <input bind:value={typed} oninput={() => (chosen = null)} autocomplete="off" />
  </label>
  {#if !chosen}
    <div class="list">
      {#if !typed.trim() && found.length}<div class="lab">{t.launchRecent}</div>{/if}
      {#each named as s (s.name)}
        {@const closed = s.state === 'gone'}
        {@const [a, b, c] = parts(s.name)}
        <button class="row" onclick={() => onSession?.(s.name, closed)}>
          <Badge {s} size={18} /><span class="name">{a}<b>{b}</b>{c}</span><span class="act">{closed ? t.reopen : t.launchOpen}</span>
        </button>
      {/each}
      {#each found as p (p.path)}
        {@const [a, b, c] = parts(p.name)}
        <button class="row" onclick={() => { chosen = p; typed = p.name }}>
          <AccountMark personal={accountPersonal(p.account)} />
          <span class="col"><span class="name">{a}<b>{b}</b>{c}</span><small>{folder(p)}</small></span>
        </button>
      {/each}
    </div>
  {/if}
  <label class="field"><span>{t.launchFirst}</span>
    <textarea rows="3" bind:value={first}></textarea>
  </label>
  <button class="go" disabled={!chosen} onclick={() => chosen && onLaunch(chosen, first.trim())}>{action}</button>
</div>

<style>
  .launch { display: flex; flex-direction: column; gap: 16px; padding: 8px 20px 24px; max-width: 640px; }
  .chips { display: flex; gap: 8px; flex-wrap: wrap; }
  .chip { display: flex; align-items: center; gap: 8px; height: 32px; padding: 0 12px; border: 1px solid rgb(255 255 255 / .2); border-radius: 8px; font-size: 14px; font-weight: 500; }
  .chip.on { background: var(--primary); color: var(--on-primary); border-color: var(--primary); }
  .field { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: var(--text2); }
  .field input, .field textarea { font: inherit; font-size: 16px; color: var(--text); background: none; border: 1px solid rgb(255 255 255 / .25); border-radius: 12px; padding: 14px 16px; outline: none; resize: vertical; }
  .field input:focus, .field textarea:focus { border-color: var(--icon); border-width: 2px; padding: 13px 15px; }
  .list { display: flex; flex-direction: column; gap: 2px; }
  .lab { font-size: 14px; font-weight: 500; color: var(--text2); padding: 0 0 4px; }
  .row { display: flex; align-items: center; gap: 12px; padding: 8px; border-radius: 12px; text-align: left; }
  .row:hover { background: var(--surface); }
  .col { display: flex; flex-direction: column; }
  .name { font-size: 16px; }
  .name b { font-weight: 700; color: var(--icon); }
  small { font-size: 12.5px; color: var(--text2); }
  .act { margin-left: auto; color: var(--icon); font-size: 14px; font-weight: 500; }
  .go { height: 56px; border-radius: 28px; background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 500; }
  .go:disabled { background: rgb(255 255 255 / .12); color: rgb(255 255 255 / .38); cursor: default; }
</style>
