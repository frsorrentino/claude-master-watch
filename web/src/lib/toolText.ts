// Da nome di strumento a frase e riga di passaggio, porta di ToolText.kt.
export type ToolKind = 'run' | 'read' | 'edit' | 'write' | 'search' | 'web' | 'message' | 'delegate' | 'plan' | 'other'
export type Labels = { run: string; read: string; edit: string; write: string; search: string; web: string; message: string; delegate: string; plan: string; other: string }

const before = (s: string, c: string) => { const i = s.indexOf(c); return i < 0 ? s : s.slice(0, i) }
const afterLast = (s: string, c: string) => { const i = s.lastIndexOf(c); return i < 0 ? s : s.slice(i + c.length) }
const beforeLast = (s: string, c: string) => { const i = s.lastIndexOf(c); return i < 0 ? '' : s.slice(0, i) }
const removePrefix = (s: string, p: string) => s.startsWith(p) ? s.slice(p.length) : s
const removeSuffix = (s: string, p: string) => s.endsWith(p) ? s.slice(0, s.length - p.length) : s
// Il «%1$s» delle etichette (String.format su Android); nessun altro formato.
const fmt = (t: string, a: string) => t.replace('%1$s', () => a)

function plain(n: string): ToolKind {
  switch (n) {
    case 'bash': case 'shell': case 'run': return 'run'
    case 'read': case 'notebookread': case 'view': return 'read'
    case 'edit': case 'multiedit': case 'notebookedit': case 'update': return 'edit'
    case 'write': case 'create': return 'write'
    case 'grep': case 'glob': case 'search': case 'find': return 'search'
    case 'webfetch': case 'websearch': case 'fetch': return 'web'
    case 'sendmessage': case 'listagents': case 'senduserfile': return 'message'
    case 'task': case 'agent': case 'workflow': return 'delegate'
    case 'todowrite': case 'exitplanmode': case 'enterplanmode': return 'plan'
    default: return 'other'
  }
}

/** Il tipo di strumento, per l'icona; gli MCP del browser sono web. */
export function kind(tool: string | null | undefined): ToolKind {
  if (tool == null) return 'other'
  const n = before(tool.trim(), ' ').toLowerCase()
  if (n.startsWith('mcp__')) return n.includes('chrome') || n.includes('browser') ? 'web' : 'other'
  return plain(n)
}

/** «mcp__chrome-bridge__execute_js» → «execute_js»; gli altri restano. */
export const short = (tool: string): string => tool.startsWith('mcp__') ? afterLast(tool, '__') : tool

/** Le due righe di un passaggio: in chiaro sopra, il dettaglio sotto (descrizione+comando; nome+cartella per i file). */
export function row(tool: string | null | undefined, text: string | null | undefined, note: string | null | undefined): [string, string | null] {
  const t = (text ?? '').trim()
  const n = note?.trim() || null
  const k = kind(tool)
  if (k === 'read' || k === 'edit' || k === 'write') return [n ?? afterLast(t, '/'), n != null ? t : (beforeLast(t, '/') || null)]
  // Gli MCP arrivano senza testo né nota: mai una riga vuota.
  if (n != null) return [n, t || null]
  return [t || short(tool?.trim() || '?'), null]
}

/** Un percorso si riduce al nome del file. */
function breve(arg: string): string {
  if (!arg) return ''
  const primo = before(arg, ' ')
  return primo.includes('/') && !arg.includes(' ') ? afterLast(primo, '/') : arg
}

/** Frase per la tile e per le righe di stato, null se non c'è nulla da dire. */
export function phrase(tool: string | null | undefined, l: Labels): string | null {
  const grezzo = tool?.trim()
  if (!grezzo) return null
  const nome = before(before(grezzo, ' '), '(').trim()
  const arg = breve(removeSuffix(removePrefix(removePrefix(grezzo, nome).trim(), '('), ')').trim())
  const withArg = (label: string) => arg === '' ? fmt(l.other, nome) : fmt(label, arg)
  switch (nome.toLowerCase()) {
    case 'bash': case 'shell': case 'run': return withArg(l.run)
    case 'read': case 'notebookread': case 'view': return withArg(l.read)
    case 'edit': case 'multiedit': case 'notebookedit': case 'update': return withArg(l.edit)
    case 'write': case 'create': return withArg(l.write)
    case 'grep': case 'glob': case 'search': case 'find': return withArg(l.search)
    case 'webfetch': case 'websearch': case 'fetch': return l.web
    case 'sendmessage': case 'listagents': case 'senduserfile': return l.message
    case 'task': case 'agent': case 'workflow': return l.delegate
    case 'todowrite': case 'exitplanmode': case 'enterplanmode': return l.plan
    default: return fmt(l.other, nome)
  }
}

/** La description di Claude accanto al comando, se c'è; senza, la frase dallo strumento. Senza strumento in corso, nulla. */
export function describe(note: string | null | undefined, tool: string | null | undefined, l: Labels): string | null {
  if (!tool || !tool.trim()) return null
  return note?.trim() || phrase(tool, l)
}
