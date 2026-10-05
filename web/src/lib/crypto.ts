// Cifratura del bus con WebCrypto, come crypto/Blob.kt e crypto/Pairing.kt del core Android.
// Busta: {"v":1,"enc":"<base64>"}, enc = nonce (12 byte) ‖ AES-256-GCM(ciphertext ‖ tag), AAD "claude-master-relay-v1".
// Accoppiamento: X25519, chiave = HKDF-SHA256(shared, salt vuoto, info "claude-master-relay-v1", 32 byte);
// chiavi pubbliche grezze (32 byte) in base64; check = primi 16 hex di HMAC-SHA256(chiave, nodo).

const INFO = 'claude-master-relay-v1'
const AAD = new TextEncoder().encode(INFO)
const NONCE = 12
const subtle = globalThis.crypto.subtle

export class BlobError extends Error {}

type Bytes = Uint8Array<ArrayBuffer>

export const toHex = (b: Bytes) => Array.from(b, x => x.toString(16).padStart(2, '0')).join('')
export const fromHex = (h: string) => Uint8Array.from(h.match(/../g) ?? [], x => parseInt(x, 16))
export const toB64 = (b: Bytes) => btoa(Array.from(b, x => String.fromCharCode(x)).join(''))
export const fromB64 = (s: string) => Uint8Array.from(atob(s), c => c.charCodeAt(0))

const aesKey = (key: Bytes, use: KeyUsage) => subtle.importKey('raw', key, 'AES-GCM', false, [use])

export async function sealBytes(plain: Bytes, key: Bytes): Promise<string> {
  const nonce = globalThis.crypto.getRandomValues(new Uint8Array(NONCE))
  const ct = new Uint8Array(await subtle.encrypt({ name: 'AES-GCM', iv: nonce, additionalData: AAD }, await aesKey(key, 'encrypt'), plain))
  const raw = new Uint8Array(NONCE + ct.length)
  raw.set(nonce)
  raw.set(ct, NONCE)
  return JSON.stringify({ v: 1, enc: toB64(raw) })
}

export const sealBlob = (plain: string, key: Bytes) => sealBytes(new TextEncoder().encode(plain), key)

/** Contratto 1.34: un pezzo di file è una busta dei byte grezzi, senza JSON in chiaro. */
export async function openBlobBytes(doc: string, key: Bytes): Promise<Bytes> {
  let o: { v?: unknown; enc?: unknown }
  try { o = JSON.parse(doc) } catch { throw new BlobError('not a document') }
  if (o === null || typeof o !== 'object') throw new BlobError('not a document')
  if (String(o.v) !== '1') throw new BlobError('unsupported version')
  let raw: Bytes
  try { raw = fromB64(String(o.enc)) } catch { throw new BlobError('bad base64') }
  if (raw.length < NONCE + 16) throw new BlobError('too short')
  try {
    return new Uint8Array(await subtle.decrypt(
      { name: 'AES-GCM', iv: raw.subarray(0, NONCE), additionalData: AAD }, await aesKey(key, 'decrypt'), raw.subarray(NONCE)))
  } catch { throw new BlobError('cannot open') }
}

export const openBlob = async (doc: string, key: Bytes) => new TextDecoder().decode(await openBlobBytes(doc, key))

export const generatePair = () => subtle.generateKey({ name: 'X25519' }, true, ['deriveBits']) as Promise<CryptoKeyPair>

export async function publicB64(pub: CryptoKey): Promise<string> {
  return toB64(new Uint8Array(await subtle.exportKey('raw', pub)))
}

/** PKCS#8 di X25519 (RFC 8410): prefisso fisso + 32 byte dello scalare. Per i vettori di prova condivisi con il relay. */
const PKCS8_PREFIX = fromHex('302e020100300506032b656e04220420')

export async function privateFromScalar(scalar: Bytes): Promise<CryptoKeyPair> {
  const der = new Uint8Array(PKCS8_PREFIX.length + 32)
  der.set(PKCS8_PREFIX)
  der.set(scalar, PKCS8_PREFIX.length)
  const privateKey = await subtle.importKey('pkcs8', der, { name: 'X25519' }, true, ['deriveBits'])
  const { x } = await subtle.exportKey('jwk', privateKey)
  const publicKey = await subtle.importKey('jwk', { kty: 'OKP', crv: 'X25519', x }, { name: 'X25519' }, true, [])
  return { privateKey, publicKey }
}

/** `info` separa gli usi: il relay (default) e il passaggio telefono → orologio. */
export async function sharedKey(own: CryptoKeyPair, peerPubB64: string, info = INFO): Promise<Bytes> {
  const raw = fromB64(peerPubB64)
  if (raw.length !== 32) throw new Error('peer public key must be 32 bytes')
  const peer = await subtle.importKey('raw', raw, { name: 'X25519' }, false, [])
  const shared = await subtle.deriveBits({ name: 'X25519', public: peer }, own.privateKey, 256)
  const ikm = await subtle.importKey('raw', shared, 'HKDF', false, ['deriveBits'])
  return new Uint8Array(await subtle.deriveBits(
    { name: 'HKDF', hash: 'SHA-256', salt: new Uint8Array(0), info: new TextEncoder().encode(info) }, ikm, 256))
}

export async function checkCode(key: Bytes, code: string): Promise<string> {
  const k = await subtle.importKey('raw', key, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign'])
  return toHex(new Uint8Array(await subtle.sign('HMAC', k, new TextEncoder().encode(code)))).slice(0, 16)
}
