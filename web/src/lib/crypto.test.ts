import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import {
  checkCode, fromHex, generatePair, openBlob, openBlobBytes, privateFromScalar, publicB64, sealBlob, sharedKey, toHex,
} from './crypto'

const fixture = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))
const range = (a: number, b: number) => Uint8Array.from({ length: b - a }, (_, i) => a + i)

describe('cifratura: vettori di cm-relay-crypto.py', () => {
  it('X25519 + HKDF danno la stessa chiave dai due lati, e il check HMAC', async () => {
    const a = await privateFromScalar(range(0, 32))
    const b = await privateFromScalar(range(32, 64))
    const expected = 'dd9f775d5fbdd918e727cb41c05452189759ccc0d87798791eff22474e278b5c'
    expect(toHex(await sharedKey(a, 'NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ='))).toBe(expected)
    expect(toHex(await sharedKey(b, 'j0DFrbaPJWJK5bIU6nZ6bslNgp09e14a0bpvPiE4KF8='))).toBe(expected)
    expect(await checkCode(fromHex(expected), '123456')).toBe('efcd032555a52bcf')
    expect(await checkCode(fromHex(expected), '123456:pc')).toBe('140090a8c7a1c707')
  })

  it('info separa gli usi', async () => {
    const a = await privateFromScalar(range(0, 32))
    const pub = 'NYBy1jZYgNGu6jKa35EhODhR7SGijjt16WXQ0s0WYlQ='
    expect(toHex(await sharedKey(a, pub))).not.toBe(toHex(await sharedKey(a, pub, 'cmwatch-handoff-v1')))
  })

  it('la chiave pubblica è di 32 byte grezzi, uguale alla fixture', async () => {
    const kp = await generatePair()
    expect(atob(await publicB64(kp.publicKey)).length).toBe(32)
    const fromScalar = await privateFromScalar(range(64, 96))
    expect(await publicB64(fromScalar.publicKey)).toBe(fixture('pair-add.json').watch.watch_pub)
  })
})

describe('accoppiamento in più (1.30): pair-add.json', () => {
  it('risposta, conferma del PC e chiave del relay', async () => {
    const f = fixture('pair-add.json')
    const dev = await privateFromScalar(range(64, 96))
    const k = await sharedKey(dev, f.qr.c)
    expect(await checkCode(k, f.qr.i)).toBe(f.watch.check)
    expect(await checkCode(k, `${f.qr.i}:pc`)).toBe(f.ok.check)
    expect(JSON.parse(await openBlob(JSON.stringify(f.ok.key), k))).toEqual({ key: f.relay_key })
  })

  it('accoppiamento della 1.15: pair-response.json', async () => {
    const f = fixture('pair-response.json')
    const qr = fixture('pair-qr.json')
    const phone = await privateFromScalar(range(32, 64))
    const k = await sharedKey(phone, qr.c)
    expect(await checkCode(k, qr.i)).toBe(f.watch.check)
    expect(await checkCode(k, `${qr.i}:pc`)).toBe(f.ok.check)
  })
})

describe('busta {v, enc}', () => {
  it('apre i pezzi di file della 1.34 e la meta', async () => {
    const f = fixture('file-parts.json')
    const key = fromHex(f.key)
    expect(JSON.parse(await openBlob(JSON.stringify(f.meta), key))).toEqual(f.meta_plain)
    const parts = await Promise.all(f.parts.map((p: unknown) => openBlobBytes(JSON.stringify(p), key)))
    const all = new Uint8Array(parts.reduce((n, p) => n + p.length, 0))
    let o = 0
    for (const p of parts) { all.set(p, o); o += p.length }
    expect(new TextDecoder().decode(all)).toBe(f.file)
    const digest = new Uint8Array(await crypto.subtle.digest('SHA-256', all))
    expect(toHex(digest)).toBe(f.meta_plain.sha256)
  })

  it('chiude e riapre, con nonce sempre nuovo', async () => {
    const key = range(96, 128)
    const a = await sealBlob('{"op":"prompt","text":"è così"}', key)
    const b = await sealBlob('{"op":"prompt","text":"è così"}', key)
    expect(JSON.parse(a).v).toBe(1)
    expect(a).not.toBe(b)
    expect(await openBlob(a, key)).toBe('{"op":"prompt","text":"è così"}')
  })

  it('rifiuta versione, base64, lunghezza e chiave sbagliate', async () => {
    const key = range(96, 128)
    const doc = await sealBlob('x', key)
    await expect(openBlob('{"v":2,"enc":"AAAA"}', key)).rejects.toThrow('unsupported version')
    await expect(openBlob('{"v":1,"enc":"%%"}', key)).rejects.toThrow('bad base64')
    await expect(openBlob('{"v":1,"enc":"AAAA"}', key)).rejects.toThrow('too short')
    await expect(openBlob('nope', key)).rejects.toThrow('not a document')
    await expect(openBlob(doc, range(0, 32))).rejects.toThrow('cannot open')
  })
})
