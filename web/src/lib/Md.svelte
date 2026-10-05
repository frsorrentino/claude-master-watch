<script lang="ts">
  import { parse } from './markdown'
  import { findLinks } from './links'
  // Il testo con il markdown dell'app (`linked` di SessionSheet.kt): grassetto, corsivo, codice, link con testo, poi i link
  // nudi che non stanno già dentro un link. Pezzi di testo semplice, niente HTML iniettato.
  let { text }: { text: string } = $props()
  type Piece = { s: string; bold?: boolean; italic?: boolean; code?: boolean; url?: string }
  const pieces = $derived.by(() => {
    const md = parse(text)
    const plain = md.text
    const marks: { start: number; end: number; set: Partial<Piece> }[] = md.spans.map(sp => ({
      start: sp.start, end: sp.end, set: sp.kind === 'link' ? { url: sp.url } : { [sp.kind]: true },
    }))
    const linked = md.spans.filter(sp => sp.kind === 'link')
    for (const [a, b] of findLinks(plain)) if (!linked.some(l => a < l.end && l.start < b)) marks.push({ start: a, end: b, set: { url: plain.slice(a, b) } })
    const cuts = [...new Set([0, plain.length, ...marks.flatMap(m => [m.start, m.end])])].sort((x, y) => x - y)
    const out: Piece[] = []
    for (let i = 0; i < cuts.length - 1; i++) {
      const [a, b] = [cuts[i], cuts[i + 1]]
      if (a === b) continue
      out.push(Object.assign({ s: plain.slice(a, b) }, ...marks.filter(m => m.start <= a && b <= m.end).map(m => m.set)))
    }
    return out
  })
</script>

{#each pieces as p}{#if p.url}<a href={p.url} target="_blank" rel="noopener" class:b={p.bold} class:i={p.italic} class:c={p.code}>{p.s}</a>{:else if p.bold || p.italic || p.code}<span class:b={p.bold} class:i={p.italic} class:c={p.code}>{p.s}</span>{:else}{p.s}{/if}{/each}

<style>
  a { color: var(--icon); text-decoration: underline; }
  .b { font-weight: 600; }
  .i { font-style: italic; }
  .c { font-family: var(--mono); font-size: .9em; background: var(--high); border-radius: 4px; padding: 0 3px; }
</style>
