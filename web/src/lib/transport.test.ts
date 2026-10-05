import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { LocalTransport, localAccess, resultTimeoutMs, TransportError } from './transport'
import type { Cmd } from './contract'

const raw = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const api = JSON.parse(raw('local-api.json'))
type Exchange = { name: string; request: { method: string; path: string; headers: Record<string, string>; body?: unknown }; response: { status: number; content_type: string; body?: unknown; body_ref?: string; body_base64?: string; content_disposition?: string } }
const ex = (name: string): Exchange => api.exchanges.find((e: Exchange) => e.name === name)

function bodyOf(r: Exchange['response']): BodyInit {
  if (r.body_ref) return raw(r.body_ref)
  if (r.body_base64) return Uint8Array.from(atob(r.body_base64), c => c.charCodeAt(0))
  return JSON.stringify(r.body)
}

/** Il relay finto: risponde solo se la richiesta è quella della fixture, intestazioni e corpo compresi. */
function relay(names: string[], seen: string[] = []) {
  return async (url: string | URL | Request, init: RequestInit = {}) => {
    const u = new URL(String(url))
    const e = names.map(ex).find(e => e.request.method === (init.method ?? 'GET') && e.request.path === u.pathname + u.search)
    if (!e) throw new Error(`unexpected ${init.method ?? 'GET'} ${u.pathname}${u.search}`)
    expect(u.origin).toBe(api.base)
    const h = new Headers(init.headers)
    for (const [k, v] of Object.entries(e.request.headers)) expect(h.get(k)).toBe(v)
    if (e.request.body !== undefined) expect(JSON.parse(String(init.body))).toEqual(e.request.body)
    seen.push(e.name)
    const headers: Record<string, string> = { 'Content-Type': e.response.content_type }
    if (e.response.content_disposition) headers['Content-Disposition'] = e.response.content_disposition
    return new Response(bodyOf(e.response), { status: e.response.status, headers })
  }
}

const transport = (names: string[], seen?: string[]) => new LocalTransport(api.base, api.token, relay(names, seen) as typeof fetch)

describe('strada locale (contratto 1.35): local-api.json', () => {
  it('stato', async () => {
    const s = await transport(['state']).fetchState()
    expect(s.host).toBe(JSON.parse(raw('state-2-idle.json')).host)
    expect(s.sessions.length).toBeGreaterThan(0)
  })

  it('eventi da un ts, dal più recente', async () => {
    const e = await transport(['events']).fetchEvents(1789200000)
    expect(e.length).toBe(ex('events').response.body instanceof Array ? (ex('events').response.body as unknown[]).length : -1)
    expect(e[0].ts).toBeGreaterThanOrEqual(e[e.length - 1].ts)
  })

  it('comando file e poi i byte del file, col nome', async () => {
    const t = transport(['cmd-file', 'file'])
    const cmd = ex('cmd-file').request.body as Cmd
    const r = await t.send(cmd)
    expect(r).toMatchObject({ id: cmd.id, ok: true })
    const f = await t.fetchFile(cmd.id)
    expect(f?.mime).toBe('image/png')
    expect(f?.name).toBe('cover.png')
    expect(f?.data.length).toBe(69)
  })

  it('condivisione e poi il report', async () => {
    const seen: string[] = []
    const t = transport(['share', 'cmd-report'], seen)
    const b = ex('share').request.body as { mime: string; data: string; name: string }
    const id = ex('share').request.path.split('/').pop()!
    await t.share(id, b.mime, Uint8Array.from(atob(b.data), c => c.charCodeAt(0)), b.name)
    const r = await t.send(ex('cmd-report').request.body as Cmd)
    expect(r.ok).toBe(true)
    expect(seen).toEqual(['share', 'cmd-report'])
  })

  it('lo stream porta il token nell\'URL, come la fixture', () => {
    const urls: string[] = []
    class FakeSource { onerror = null; constructor(u: string) { urls.push(u) } addEventListener() {} close() {} }
    const stop = new LocalTransport(api.base, api.token, fetch, FakeSource as unknown as typeof EventSource).subscribe(() => {})
    expect(urls).toEqual([api.base + ex('stream').request.path])
    stop()
  })

  it('senza token il relay dice 401: errore «token»', async () => {
    const t = new LocalTransport(api.base, '', relay(['state-no-token']) as typeof fetch)
    await expect(t.fetchState()).rejects.toThrow(TransportError)
    await expect(t.fetchState()).rejects.toMatchObject({ status: 401, reason: 'token' })
  })
})

describe('accesso locale', () => {
  it('prende il token dall\'URL e lo ricorda, poi lo legge da solo', () => {
    const store = new Map<string, string>()
    const a = localAccess(new URL('http://127.0.0.1:8765/?t=abc&page=diary#x'), store)
    expect(a).toEqual({ base: 'http://127.0.0.1:8765', token: 'abc', clean: '/?page=diary#x' })
    expect(localAccess(new URL('http://127.0.0.1:8765/'), store)).toEqual({ base: 'http://127.0.0.1:8765', token: 'abc', clean: null })
  })

  it('fuori da 127.0.0.1 e localhost niente strada locale', () => {
    expect(localAccess(new URL('https://cmwatch-demo.web.app/?t=abc'), new Map())).toBeNull()
    expect(localAccess(new URL('http://localhost:5173/'), new Map())).toBeNull()
  })

  it('attese come l\'app Android: slash 60 s, gli altri 20 s', () => {
    expect(resultTimeoutMs('slash')).toBe(60_000)
    expect(resultTimeoutMs('prompt')).toBe(20_000)
  })
})
