// Il trasporto della web app, come l'interfaccia `Transport` dell'app Android. Strada locale (contratto 1.35): il relay
// serve la pagina e l'API su 127.0.0.1, JSON in chiaro con il token Bearer; le GET accettano anche `?t=`.

import { decodeState, type Cmd, type CmdOp, type CmdResult, type Event, type State } from './contract'
import { toB64 } from './crypto'

export type FileBlob = { data: Uint8Array; mime: string; name: string | null }

export interface Transport {
  fetchState(): Promise<State>
  /** Ogni cambiamento di stato; restituisce la funzione che chiude. */
  subscribe(onState: (s: State) => void, onDown?: () => void): () => void
  /** Eventi con ts > since, dal più recente. */
  fetchEvents(since?: number): Promise<Event[]>
  send(cmd: Cmd): Promise<CmdResult>
  share(id: string, mime: string, data: Uint8Array, name?: string | null): Promise<void>
  fetchFile(id: string): Promise<FileBlob | null>
}

export class TransportError extends Error {
  constructor(readonly status: number, readonly reason: string) { super(`${status} ${reason}`) }
}

const RESULT_TIMEOUT_MS = 20_000
/** Un comando slash che apre un pannello ci mette più di 20 s, come sull'app Android. */
const SLASH_RESULT_TIMEOUT_MS = 60_000
export const resultTimeoutMs = (op: CmdOp) => op === 'slash' ? SLASH_RESULT_TIMEOUT_MS : RESULT_TIMEOUT_MS

export class LocalTransport implements Transport {
  constructor(
    private readonly base: string,
    private readonly token: string,
    private readonly http: typeof fetch = (...a) => fetch(...a),
    private readonly Source: typeof EventSource = globalThis.EventSource,
  ) {}

  private async call(path: string, init: RequestInit = {}, timeoutMs = RESULT_TIMEOUT_MS): Promise<Response> {
    const headers = new Headers(init.headers)
    if (this.token) headers.set('Authorization', `Bearer ${this.token}`)
    const r = await this.http(this.base + path, { ...init, headers, signal: AbortSignal.timeout(timeoutMs) })
    if (!r.ok) {
      const reason = await r.json().then(j => String(j?.error ?? ''), () => '')
      throw new TransportError(r.status, reason)
    }
    return r
  }

  private post(path: string, body: unknown, timeoutMs?: number) {
    return this.call(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }, timeoutMs)
  }

  async fetchState() { return decodeState(await (await this.call('/api/state')).text()) }

  async fetchEvents(since = 0): Promise<Event[]> { return (await this.call(`/api/events?since=${since}`)).json() }

  async send(cmd: Cmd): Promise<CmdResult> { return (await this.post('/api/cmd', cmd, resultTimeoutMs(cmd.op))).json() }

  async share(id: string, mime: string, data: Uint8Array, name?: string | null) {
    const body: Record<string, string> = { mime, data: toB64(data as Uint8Array<ArrayBuffer>) }
    if (name) body.name = name
    await this.post(`/api/share/${encodeURIComponent(id)}`, body)
  }

  async fetchFile(id: string): Promise<FileBlob | null> {
    try {
      const r = await this.call(`/api/file/${encodeURIComponent(id)}`)
      const cd = r.headers.get('Content-Disposition') ?? ''
      const star = /filename\*=UTF-8''([^;]+)/i.exec(cd)
      return { data: new Uint8Array(await r.arrayBuffer()), mime: r.headers.get('Content-Type') ?? 'application/octet-stream', name: star ? decodeURIComponent(star[1]) : null }
    } catch (e) {
      if (e instanceof TransportError && e.status === 404) return null
      throw e
    }
  }

  /** SSE: `event: state` con lo stato intero all'apertura e a ogni push; EventSource si riconnette da sé. */
  subscribe(onState: (s: State) => void, onDown?: () => void): () => void {
    const es = new this.Source(`${this.base}/api/stream?t=${encodeURIComponent(this.token)}`)
    es.addEventListener('state', (m: MessageEvent) => { try { onState(decodeState(m.data)) } catch { /* uno stato rotto si salta */ } })
    es.onerror = () => onDown?.()
    return () => es.close()
  }
}

type Store = { get(k: string): string | null | undefined; set(k: string, v: string): void }
const TOKEN = 'cm.token'

/**
 * La strada locale vale solo sulla pagina servita dal relay (127.0.0.1 o localhost, non il server di Vite).
 * Il token arriva una volta nell'URL di `claude-master relay web`, si ricorda e si toglie dall'indirizzo (`clean`).
 */
export function localAccess(url: URL, store: Store): { base: string; token: string; clean: string | null } | null {
  if (url.hostname !== '127.0.0.1' && url.hostname !== 'localhost') return null
  const fromUrl = url.searchParams.get('t')
  if (fromUrl) store.set(TOKEN, fromUrl)
  const token = fromUrl ?? store.get(TOKEN)
  if (!token) return null
  let clean: string | null = null
  if (fromUrl) {
    const q = new URLSearchParams(url.search)
    q.delete('t')
    clean = url.pathname + (q.size ? `?${q}` : '') + url.hash
  }
  return { base: url.origin, token, clean }
}

// Contratto 1.37: anche approve e decision dicono da dove arrivano.
const DEVICE_OPS = new Set<CmdOp>(['prompt', 'resume', 'launch', 'report', 'approve', 'decision'])

/** Un comando nuovo dalla web app: `by` = "web" come mette il relay di default. */
export function newCmd(op: CmdOp, session: string | null, arg?: string | null, text?: string): Cmd {
  const c: Cmd = { id: crypto.randomUUID(), op, session, arg: arg ?? null, issued: Math.round(Date.now() / 1000), by: 'web' }
  if (text !== undefined) c.text = text
  // Contratto 1.36: il relay scrive «via web app» e la trascrizione dà origin "web".
  if (DEVICE_OPS.has(op)) c.device = 'web'
  return c
}
