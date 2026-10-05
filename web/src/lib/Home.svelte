<script lang="ts">
  import type { State } from './contract'
  import { age, groups } from './summary'
  import { t } from './t'
  import Badge from './Badge.svelte'
  let { st, selected, onPick }: { st: State; selected: string | null; onPick: (name: string) => void } = $props()
  const now = $derived(st.ts)
</script>

<section class="home">
  <header>
    <div class="mono">{t.updated(st.host)}</div>
    <div class="quotas">
      {#each Object.entries(st.quota) as [acc, q]}
        <span class="quota" class:stale={q.stale}>{acc} · {q.h5 != null ? t.quota5h(q.h5) : t.stale}{q.w7 != null ? ` · ${t.week(q.w7)}` : ''}</span>
      {/each}
    </div>
  </header>
  {#each groups(st.sessions) as g}
    <h2 class="mono group {g.group}">{t.groups[g.group].toUpperCase()} · {g.sessions.length}</h2>
    <ul>
      {#each g.sessions as s (s.id)}
        <li>
          <button class="row" class:on={selected === s.name} onclick={() => onPick(s.name)}>
            <Badge {s} />
            <span class="body">
              <span class="name">{s.name}</span>
              <span class="sub">{s.question?.text ?? s.outcome?.short ?? `${t.states[s.state]} · ${age(s.since, now)}`}</span>
            </span>
            {#if s.context != null}<span class="mono">{t.ctx(s.context)}</span>{/if}
          </button>
        </li>
      {/each}
    </ul>
  {/each}
</section>

<style>
  .home { padding: 16px 12px 24px; display: flex; flex-direction: column; gap: 6px; overflow-y: auto; height: 100%; }
  header { padding: 4px 8px 10px; display: flex; flex-direction: column; gap: 8px; }
  .quotas { display: flex; flex-wrap: wrap; gap: 6px; }
  .quota { font: 12px var(--mono); background: var(--low); border-radius: 999px; padding: 4px 10px; color: var(--text2); }
  .quota.stale { color: var(--wait); }
  .group { margin: 12px 8px 4px; }
  .group.waiting { color: var(--wait); } .group.working { color: var(--icon); }
  ul { list-style: none; padding: 0; display: flex; flex-direction: column; gap: 2px; }
  .row { width: 100%; display: flex; align-items: center; gap: 12px; padding: 10px; border-radius: 14px; text-align: left; transition: background .15s; }
  .row:hover { background: var(--low); }
  .row.on { background: var(--high); }
  .body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
  .name { font-weight: 500; font-size: 16px; }
  .sub { color: var(--text2); font-size: 13px; overflow-wrap: anywhere; }
</style>
