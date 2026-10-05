# Web app, strada remota (Firebase): piano di implementazione

> **Per chi esegue:** sotto-skill richiesta: superpowers:subagent-driven-development oppure superpowers:executing-plans,
> un task alla volta. I passi usano le caselle (`- [ ]`) per segnare l'avanzamento.

**Obiettivo:** la stessa web app di `web/` usata da un Mac, un PC, un iPad o un iPhone lontani dal Chromebook: legge e
scrive il bus RTDB come il telefono, cifrato, dopo un accoppiamento con il codice a 6 cifre o con un link.

**Architettura:** tre moduli senza SDK, come `Rtdb.kt` e `FirebaseTransport.kt` del core Android:
- `auth.ts`: Firebase Auth anonimo via REST, un uid per browser;
- `rtdb.ts`: REST `<path>.json?auth=` e SSE con `EventSource`;
- `firebaseTransport.ts`: la stessa interfaccia `Transport` della strada locale, con le buste `{v, enc}`.

L'accoppiamento (`pairing.ts`) porta `PhonePairer.kt` e accetta solo gli inviti `--add`. La chiave del relay sta in
IndexedDB come `CryptoKey` non estraibile. La pagina è servita da Firebase Hosting del progetto dell'utente, che le dà
anche la configurazione (`/__/firebase/init.json`).

**Tecnologia:** TypeScript, Svelte 5, Vite, vitest; WebCrypto (`crypto.ts`, già fatto e verificato sui vettori, `b6ad535`);
Firebase Auth REST, RTDB REST + SSE, Firebase Hosting (CLI `firebase` in `~/.local/bin`).

**Spec:** `docs/plans/2026-10-05-web-app-piano.md` (fase 3), `contract/README.md` (1.15, 1.19, 1.20, 1.28, 1.30, 1.31,
1.32, 1.34), e come riferimento di comportamento `core/src/main/kotlin/it/pixelbox/cmwatch/transport/FirebaseTransport.kt`,
`transport/Rtdb.kt`, `pairing/PhonePairer.kt`.

## Vincoli globali

- Nessuna dipendenza nuova a runtime (niente SDK Firebase), salvo il ripiego del Task 1 se Safari non ha X25519.
- Testi visibili in italiano in `web/src/lib/t.ts`, mai cablati nei componenti; una riga logica su una riga fisica; mai «…».
- La chiave del relay non esce mai dal browser e non finisce in log, URL o localStorage: solo IndexedDB, non estraibile.
- L'invito viaggia nel frammento (`#pair=…`), che non arriva a nessun server.
- Il browser accetta solo inviti `--add` (`mode`/`m` = "add"): un invito di primo accoppiamento cambierebbe la chiave
  di tutti gli altri dispositivi.
- Busta: `{"v":1,"enc":"<base64>"}`, AES-256-GCM, AAD `claude-master-relay-v1` (contratto, sezione iniziale).
- Attese come l'app: risultato di un comando 20 s, `slash` 60 s; poll 1 s; riconnessione 1-2-5-15-30 s; 90 s di silenzio
  dello stream = connessione morta.
- `share`: il limite `state.share.max_bytes` vale sulla lunghezza della stringa `enc` (1.19); file a pezzi fino a 25 MB (1.34).
- `/seen/<uid>` = `{".sv":"timestamp"}` dopo il primo stato letto (1.20); un rifiuto non blocca la lettura.
- Nessun id del progetto Firebase nel repo (origin è pubblico): il deploy passa `--project` da riga di comando.
- Commit in inglese, file per nome, mai `git add -A`.

## Prerequisiti fuori da questo repo

- **Relay, contratto 1.39** (Task 0; la 1.38 è dei «Prossimi che sbloccano», il numero lo conferma claude-master): massimo dispositivi da 4 a 8, `kind` "web" fra i tipi accettati, op `unpair` per
  togliere un dispositivo. Oggi sono accoppiati telefono, orologio, tablet e Chromebook: un browser verrebbe rifiutato
  con «full». Si chiede a claude-master solo a release 0.6.8 finita.
- **Franz:** `firebase login` se la CLI non è autenticata; una prova di X25519 su Safari dell'iPhone (Task 1).

## Mappa dei file

- Nuovi in `web/src/lib/`:
  - `auth.ts`, `auth.test.ts`: sessione anonima, rinnovo del token;
  - `rtdb.ts`, `rtdb.test.ts`: REST e stream;
  - `firebaseTransport.ts`, `firebaseTransport.test.ts`: il `Transport` remoto;
  - `pairing.ts`, `pairing.test.ts`: inviti, accoppiamento, nome del dispositivo;
  - `vault.ts`, `vault.test.ts`: dove sta l'accoppiamento;
  - `Pair.svelte`: la pagina «Collega questo browser».
- Nuovi altrove: `web/firebase.json`, `docs/verifiche/web-strada-remota.md`, `docs/richieste/2026-10-0X-dispositivi-web.md`.
- Modificati:
  - `web/src/lib/crypto.ts`: chiave anche come `CryptoKey`, `Bytes` esportato;
  - `web/src/lib/transport.ts`: `TooLargeError`;
  - `web/src/App.svelte`: scelta del trasporto, pagina di accoppiamento;
  - `web/src/lib/Settings.svelte`: «Scollega questo browser»;
  - `web/src/lib/t.ts`;
  - Android: `core/.../pairing/PairLink.kt` (`webUrl`), `mobile/.../ui/AddDeviceSheet.kt` e stringhe.

## Da verificare in revisione

1. **Token scaduto a stream aperto** (dopo un'ora RTDB manda `auth_revoked`): lo stato deve tornare da solo con un token
   nuovo, senza ricaricare. Test: Task 4 (`auth_revoked` chiude e chiama `onDown`), Task 5 (riconnessione dopo il backoff),
   Task 3 (rinnovo a 5 minuti dalla scadenza).
2. **Connessione morta senza errori** (portatile in sospensione, cambio di rete): dopo 90 s senza eventi né keep-alive lo
   stream si chiude e riparte. Test: Task 4, timer finti.
3. **Invito di primo accoppiamento aperto sul browser**: rifiuto chiaro («usa Aggiungi un dispositivo»), mai una risposta
   in `/pair/<id>/watch`. Test: Task 6, `not-add`, nessuna `put`.
4. **Già 4 dispositivi** (o 8 dopo la 1.39): errore «full» appena il PC lo scrive, non dopo 30 s. Test: Task 6.
5. **Allegato oltre `share.max_bytes`**: rifiuto prima di scrivere, con le misure, e messaggio in italiano. Test: Task 5
   (`TooLargeError`, nessuna `put`), Task 8 (il testo).

---

### Task 0: richiesta al relay (contratto 1.39)

**File:**
- Creare: `docs/richieste/2026-10-0X-dispositivi-web.md` (X = giorno dell'invio)

- [ ] **Passo 1: controllare che non ci sia una release del plugin in corso**

Eseguire: `cd ~/Desktop/workspaces/personali/claude-master && git log --oneline -3`
Atteso: in cima `release: 0.6.8` (finita). Se la release non è finita, aspettare: durante una release non si mandano richieste.

- [ ] **Passo 2: scrivere la richiesta**

```markdown
# Richiesta al relay: i browser come dispositivi (contratto 1.39)

Franz, 05/10: la web app remota (Mac, PC, iPad, iPhone) si accoppia come un dispositivo in più, con `pair --add`.

1. `PAIR_MAX_DEVICES` da 4 a 8: oggi telefono, orologio, tablet e Chromebook riempiono i posti e un browser riceve «full».
2. `kind` "web" fra `DEVICE_KINDS`: la risposta di accoppiamento del browser porta `kind: "web"` e `kinds: {<uid>: "web"}`;
   oggi un tipo sconosciuto si scarta e `state.devices[].kind` resta null.
3. Op `unpair`, `arg` = uid: lo toglie da `/allowed` e da `devices.json`; rifiuti «unknown device <uid>», e per l'ultimo
   dispositivo rimasto «cannot remove the last device». Il browser lo usa da Impostazioni, «Scollega questo browser».
   `ops` comprende `unpair`.

Fixture: un dispositivo `kind` "web" negli stati, un `unpair` riuscito e uno rifiutato in `cmd-result-sample.json`.
`v` resta 1.
```

- [ ] **Passo 3: mandarla e committarla**

```bash
claude-master talk claude-master "Richiesta dell'app, contratto 1.39: /home/franz/Desktop/workspaces/personali/claude-master-phone/docs/richieste/2026-10-0X-dispositivi-web.md (8 dispositivi, kind web, op unpair)."
git add docs/richieste/2026-10-0X-dispositivi-web.md
git commit -m "docs: relay request for browsers as devices (contract 1.39)"
```

Quando claude-master consegna la 1.39: copiare le fixture in `contract/` identiche byte per byte, il paragrafo nel
README, aggiornare i conteggi dei test Kotlin come per la 1.37 (`ContractTest`), e portarle anche su `origin/master`
(R0 del relay controlla lì).

---

### Task 1: Hosting, app web registrata e prove di fattibilità

**File:**
- Creare: `web/firebase.json`
- Creare: `docs/verifiche/web-strada-remota.md` (sezione «Prove di fattibilità»)

**Interfacce:**
- Produce: `https://<progetto>.web.app/` che serve `web/dist`; `GET /__/firebase/init.json` →
  `{apiKey, projectId, databaseURL, …}` usato dal Task 8 per l'accoppiamento col codice.

- [ ] **Passo 1: la CLI è autenticata e l'app web c'è**

Eseguire: `firebase login:list` e `firebase apps:list WEB --project claude-master-relay-3761`
Atteso: un account; se non c'è, Franz esegue `! firebase login --no-localhost`. Se la lista WEB è vuota:
`firebase apps:create WEB claude-master-web --project claude-master-relay-3761` (crea anche la chiave «Browser key»).

- [ ] **Passo 2: scrivere `web/firebase.json`**

```json
{
  "hosting": {
    "public": "dist",
    "ignore": ["firebase.json", "**/.*"],
    "rewrites": [{ "source": "**", "destination": "/index.html" }],
    "headers": [
      { "source": "/sw.js", "headers": [{ "key": "Cache-Control", "value": "no-cache" }] },
      { "source": "/index.html", "headers": [{ "key": "Cache-Control", "value": "no-cache" }] },
      { "source": "/assets/**", "headers": [{ "key": "Cache-Control", "value": "public, max-age=31536000, immutable" }] }
    ]
  }
}
```

- [ ] **Passo 3: primo deploy della build di oggi**

```bash
cd web && npx vite build && firebase deploy --only hosting --project claude-master-relay-3761
```
Atteso: «Deploy complete!» e l'URL `https://claude-master-relay-3761.web.app`.

- [ ] **Passo 4: la configurazione arriva dall'hosting**

Eseguire: `curl -s https://claude-master-relay-3761.web.app/__/firebase/init.json | python3 -m json.tool`
Atteso: `apiKey`, `projectId` = claude-master-relay-3761, `databaseURL` che finisce con `firebasedatabase.app`.

- [ ] **Passo 5: auth anonima con la chiave web, e RTDB raggiungibile**

```bash
K=$(curl -s https://claude-master-relay-3761.web.app/__/firebase/init.json | python3 -c "import json,sys;print(json.load(sys.stdin)['apiKey'])")
curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$K" -H 'Content-Type: application/json' -d '{"returnSecureToken":true}' | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('localId'), d.get('expiresIn'), d.get('error'))"
```
Atteso: un uid e `3600`, nessun errore. Con un errore `API_KEY_*_BLOCKED` la chiave ha restrizioni: annotarlo e usare la
«Browser key» del Passo 1. Poi, con l'idToken, `GET <databaseURL>/state.json?auth=<idToken>` deve dare 401 (uid non
ancora in `/allowed`): le regole funzionano.

- [ ] **Passo 6: X25519 su Safari dell'iPhone (Franz)**

Pubblicare in `web/public/x25519.html` una pagina che prova `crypto.subtle.generateKey({name:'X25519'}, true, ['deriveBits'])`
e scrive «X25519 sì» o l'errore; deploy; Franz la apre sull'iPhone e sull'iPad. Poi togliere la pagina.

```html
<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<p id="o">prova…</p>
<script type="module">
  try { await crypto.subtle.generateKey({ name: 'X25519' }, true, ['deriveBits']); o.textContent = 'X25519 sì' }
  catch (e) { o.textContent = 'X25519 no: ' + e }
</script>
```
Se l'esito è «no»: unica dipendenza ammessa `@noble/curves` (`npm i @noble/curves@1 --prefix web`), usata in
`crypto.ts` solo quando `subtle.importKey('raw', …, {name:'X25519'}, …)` lancia `NotSupportedError`:
`x25519.getSharedSecret(privRaw, peerRaw)` al posto di `deriveBits`, con le stesse prove sui vettori.

- [ ] **Passo 7: annotare gli esiti e committare**

```bash
git add web/firebase.json docs/verifiche/web-strada-remota.md
git commit -m "feat(web): Firebase Hosting for the remote road, feasibility checks (init.json, anonymous auth, X25519 on Safari)"
```

---

### Task 2: la chiave anche come `CryptoKey`

**File:**
- Modificare: `web/src/lib/crypto.ts`
- Test: `web/src/lib/crypto.test.ts`

**Interfacce:**
- Produce: `export type Bytes = Uint8Array<ArrayBuffer>`; `export type AesKey = Bytes | CryptoKey`;
  `importAesKey(raw: Bytes): Promise<CryptoKey>` (non estraibile); `sealBlob`, `sealBytes`, `openBlob`, `openBlobBytes`
  accettano `AesKey`.

- [ ] **Passo 1: test che falliscono**

In fondo a `crypto.test.ts`:
```ts
describe('chiave non estraibile', () => {
  it('apre i pezzi della 1.34 con una CryptoKey importata, che non si può esportare', async () => {
    const f = fixture('file-parts.json')
    const key = await importAesKey(fromHex(f.key))
    expect(key.extractable).toBe(false)
    await expect(crypto.subtle.exportKey('raw', key)).rejects.toThrow()
    expect(JSON.parse(await openBlob(JSON.stringify(f.meta), key))).toEqual(f.meta_plain)
    expect(await openBlob(await sealBlob('ciao', key), fromHex(f.key))).toBe('ciao')
  })
})
```
e `importAesKey` nell'import in cima.

- [ ] **Passo 2: eseguirli**

Eseguire: `cd web && npx vitest run src/lib/crypto.test.ts`
Atteso: FAIL, `importAesKey` non esiste.

- [ ] **Passo 3: implementazione**

In `crypto.ts`: `type Bytes` diventa `export type Bytes`; sotto:
```ts
/** La chiave del relay: byte grezzi (vettori, accoppiamento) o CryptoKey non estraibile (quella salvata nel browser). */
export type AesKey = Bytes | CryptoKey

export const importAesKey = (raw: Bytes) => subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt', 'decrypt'])
```
`aesKey` diventa:
```ts
const aesKey = (key: AesKey, use: KeyUsage) =>
  key instanceof CryptoKey ? Promise.resolve(key) : subtle.importKey('raw', key, 'AES-GCM', false, [use])
```
e nelle firme di `sealBytes`, `sealBlob`, `openBlobBytes`, `openBlob` il parametro `key: Bytes` diventa `key: AesKey`.

- [ ] **Passo 4: test verdi e tipi puliti**

Eseguire: `npx vitest run && npm run check`
Atteso: tutti verdi, 0 errori.

- [ ] **Passo 5: commit**

```bash
git add web/src/lib/crypto.ts web/src/lib/crypto.test.ts
git commit -m "feat(web): the relay key as a non-extractable CryptoKey"
```

---

### Task 3: Firebase Auth anonimo via REST

**File:**
- Creare: `web/src/lib/auth.ts`
- Test: `web/src/lib/auth.test.ts`

**Interfacce:**
- Produce:
  ```ts
  export type AuthSession = { uid: string; idToken: string; refreshToken: string; expiresAt: number } // ms
  export interface AuthStore { load(): AuthSession | null; save(s: AuthSession): void; clear(): void }
  export class AuthError extends Error { readonly reason: 'signed-out' | 'network' }
  export class AnonAuth {
    constructor(apiKey: string, store: AuthStore, http?: typeof fetch, now?: () => number)
    signUp(): Promise<AuthSession>
    uid(): string | null
    token(force?: boolean): Promise<string>
  }
  ```

- [ ] **Passo 1: test che falliscono**

```ts
import { describe, expect, it } from 'vitest'
import { AnonAuth, AuthError, type AuthSession } from './auth'

const mem = () => {
  let s: AuthSession | null = null
  return { load: () => s, save: (x: AuthSession) => { s = x }, clear: () => { s = null } }
}
const reply = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('Firebase Auth anonimo via REST', () => {
  it('signUp salva uid, token e scadenza', async () => {
    const calls: string[] = []
    const http = (async (url: string, init: RequestInit) => {
      calls.push(`${init.method} ${url}`)
      expect(JSON.parse(String(init.body))).toEqual({ returnSecureToken: true })
      return reply(200, { localId: 'u1', idToken: 't1', refreshToken: 'r1', expiresIn: '3600' })
    }) as unknown as typeof fetch
    const a = new AnonAuth('KEY', mem(), http, () => 1000)
    expect(await a.signUp()).toEqual({ uid: 'u1', idToken: 't1', refreshToken: 'r1', expiresAt: 1000 + 3_600_000 })
    expect(calls).toEqual(['POST https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=KEY'])
    expect(await a.token()).toBe('t1')
    expect(a.uid()).toBe('u1')
  })

  it('rinnova a 5 minuti dalla scadenza, una volta sola per più richieste insieme', async () => {
    const store = mem()
    store.save({ uid: 'u1', idToken: 'old', refreshToken: 'r1', expiresAt: 10 * 60_000 })
    let refreshes = 0
    const http = (async (url: string, init: RequestInit) => {
      refreshes++
      expect(url).toBe('https://securetoken.googleapis.com/v1/token?key=KEY')
      expect(String(init.body)).toBe('grant_type=refresh_token&refresh_token=r1')
      return reply(200, { user_id: 'u1', id_token: 'new', refresh_token: 'r2', expires_in: '3600' })
    }) as unknown as typeof fetch
    const a = new AnonAuth('KEY', store, http, () => 4 * 60_000)
    expect(await a.token()).toBe('old')
    expect(refreshes).toBe(0)
    const b = new AnonAuth('KEY', store, http, () => 6 * 60_000)
    expect(await Promise.all([b.token(), b.token()])).toEqual(['new', 'new'])
    expect(refreshes).toBe(1)
    expect(store.load()?.refreshToken).toBe('r2')
  })

  it('un refresh rifiutato cancella la sessione: il browser va ricollegato', async () => {
    const store = mem()
    store.save({ uid: 'u1', idToken: 'old', refreshToken: 'bad', expiresAt: 0 })
    const http = (async () => reply(400, { error: { message: 'INVALID_REFRESH_TOKEN' } })) as unknown as typeof fetch
    const a = new AnonAuth('KEY', store, http, () => 1)
    await expect(a.token()).rejects.toMatchObject({ reason: 'signed-out' })
    expect(store.load()).toBeNull()
  })

  it('senza sessione: signed-out; con la rete giù: network', async () => {
    await expect(new AnonAuth('KEY', mem()).token()).rejects.toBeInstanceOf(AuthError)
    const store = mem()
    store.save({ uid: 'u1', idToken: 'old', refreshToken: 'r1', expiresAt: 0 })
    const down = (async () => { throw new TypeError('Failed to fetch') }) as unknown as typeof fetch
    await expect(new AnonAuth('KEY', store, down, () => 1).token()).rejects.toMatchObject({ reason: 'network' })
    expect(store.load()).not.toBeNull()
  })
})
```

- [ ] **Passo 2: eseguirli**

Eseguire: `npx vitest run src/lib/auth.test.ts`
Atteso: FAIL, modulo `./auth` mancante.

- [ ] **Passo 3: implementazione**

```ts
// Firebase Auth anonimo via REST, come l'app Android ma senza SDK: un uid per browser, il token (un'ora) si rinnova da solo.
export type AuthSession = { uid: string; idToken: string; refreshToken: string; expiresAt: number }
export interface AuthStore { load(): AuthSession | null; save(s: AuthSession): void; clear(): void }

export class AuthError extends Error {
  constructor(readonly reason: 'signed-out' | 'network', detail = '') { super(detail ? `${reason}: ${detail}` : reason) }
}

const SIGN_UP = 'https://identitytoolkit.googleapis.com/v1/accounts:signUp'
const REFRESH = 'https://securetoken.googleapis.com/v1/token'
/** Si rinnova 5 minuti prima della scadenza. */
const EARLY_MS = 5 * 60_000
/** Il refresh token non vale più: l'uid è perso e va rifatto l'accoppiamento. */
const GONE = /TOKEN_EXPIRED|INVALID_REFRESH_TOKEN|USER_NOT_FOUND|USER_DISABLED/

export class AnonAuth {
  private inflight: Promise<string> | null = null

  constructor(
    private readonly apiKey: string,
    private readonly store: AuthStore,
    private readonly http: typeof fetch = (...a) => fetch(...a),
    private readonly now: () => number = () => Date.now(),
  ) {}

  /** Il primo accesso di questo browser, solo durante l'accoppiamento: un uid nuovo. */
  async signUp(): Promise<AuthSession> {
    const r = await this.post(`${SIGN_UP}?key=${encodeURIComponent(this.apiKey)}`, JSON.stringify({ returnSecureToken: true }), 'application/json')
    const s = { uid: r.localId, idToken: r.idToken, refreshToken: r.refreshToken, expiresAt: this.now() + Number(r.expiresIn) * 1000 }
    this.store.save(s)
    return s
  }

  uid(): string | null { return this.store.load()?.uid ?? null }

  /** Un token valido; `force` dopo un 401. Più richieste insieme rinnovano una volta sola. */
  token(force = false): Promise<string> {
    const s = this.store.load()
    if (!s) return Promise.reject(new AuthError('signed-out'))
    if (!force && s.expiresAt - EARLY_MS > this.now()) return Promise.resolve(s.idToken)
    this.inflight ??= this.refresh(s).finally(() => { this.inflight = null })
    return this.inflight
  }

  private async refresh(s: AuthSession): Promise<string> {
    const body = new URLSearchParams({ grant_type: 'refresh_token', refresh_token: s.refreshToken }).toString()
    const r = await this.post(`${REFRESH}?key=${encodeURIComponent(this.apiKey)}`, body, 'application/x-www-form-urlencoded')
    this.store.save({ uid: r.user_id, idToken: r.id_token, refreshToken: r.refresh_token, expiresAt: this.now() + Number(r.expires_in) * 1000 })
    return r.id_token
  }

  private async post(url: string, body: string, type: string) {
    let res: Response
    try { res = await this.http(url, { method: 'POST', headers: { 'Content-Type': type }, body }) }
    catch (e) { throw new AuthError('network', String(e)) }
    const j = await res.json().catch(() => ({}))
    if (res.ok) return j
    const msg = String(j?.error?.message ?? res.status)
    if (res.status === 400 && GONE.test(msg)) { this.store.clear(); throw new AuthError('signed-out', msg) }
    throw new AuthError('network', msg)
  }
}
```
Nel secondo test il token scade a 10 minuti: a 4 minuti vale ancora, a 6 minuti è dentro i 5 minuti prima della
scadenza e si rinnova.

- [ ] **Passo 4: test verdi**

Eseguire: `npx vitest run src/lib/auth.test.ts`
Atteso: 4 passed.

- [ ] **Passo 5: commit**

```bash
git add web/src/lib/auth.ts web/src/lib/auth.test.ts
git commit -m "feat(web): anonymous Firebase Auth over REST, token refreshed 5 minutes early, signed-out when the refresh token is gone"
```

---

### Task 4: RTDB via REST e stream SSE

**File:**
- Creare: `web/src/lib/rtdb.ts`
- Test: `web/src/lib/rtdb.test.ts`

**Interfacce:**
- Consuma: `AnonAuth.token(force?)` (Task 3), passato come funzione.
- Produce:
  ```ts
  export type SseEvent = { event: string; data: string }
  export class RtdbError extends Error { readonly status: number }
  export class Rtdb {
    constructor(base: string, token: (force?: boolean) => Promise<string>, http?: typeof fetch, Source?: typeof EventSource, silenceMs?: number)
    get(path: string, query?: Record<string, string>): Promise<string | null>
    put(path: string, body: string): Promise<void>
    del(path: string): Promise<void>
    stream(path: string, onEvent: (e: SseEvent) => void, onDown: (why: string) => void): () => void
  }
  ```

- [ ] **Passo 1: test che falliscono**

```ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { Rtdb, RtdbError } from './rtdb'

class FakeSource {
  static all: FakeSource[] = []
  listeners: Record<string, ((m: MessageEvent) => void)[]> = {}
  onerror: (() => void) | null = null
  closed = false
  constructor(readonly url: string) { FakeSource.all.push(this) }
  addEventListener(n: string, f: (m: MessageEvent) => void) { (this.listeners[n] ??= []).push(f) }
  emit(n: string, data = '') { for (const f of this.listeners[n] ?? []) f({ data } as MessageEvent) }
  close() { this.closed = true }
}
const BASE = 'https://demo-default-rtdb.europe-west1.firebasedatabase.app'
const tok = (force?: boolean) => Promise.resolve(force ? 'T2' : 'T1')

describe('RTDB via REST', () => {
  it('GET con auth nella query; «null» vuol dire nodo assente', async () => {
    const urls: string[] = []
    const http = (async (u: string) => { urls.push(u); return new Response(urls.length === 1 ? '{"a":1}' : 'null') }) as unknown as typeof fetch
    const db = new Rtdb(BASE, tok, http)
    expect(await db.get('events', { orderBy: '"$key"', limitToLast: '200' })).toBe('{"a":1}')
    expect(await db.get('nope')).toBeNull()
    expect(urls[0]).toBe(`${BASE}/events.json?orderBy=%22%24key%22&limitToLast=200&auth=T1`)
  })

  it('un 401 riprova una volta con un token nuovo', async () => {
    const seen: string[] = []
    const http = (async (u: string) => { seen.push(new URL(u).searchParams.get('auth')!); return new Response('1', { status: seen.length === 1 ? 401 : 200 }) }) as unknown as typeof fetch
    expect(await new Rtdb(BASE, tok, http).get('state')).toBe('1')
    expect(seen).toEqual(['T1', 'T2'])
  })

  it('un errore HTTP diventa RtdbError con lo stato', async () => {
    const http = (async () => new Response('denied', { status: 403 })) as unknown as typeof fetch
    await expect(new Rtdb(BASE, tok, http).put('cmd/x', '{}')).rejects.toMatchObject({ status: 403 })
    await expect(new Rtdb(BASE, tok, http).put('cmd/x', '{}')).rejects.toBeInstanceOf(RtdbError)
  })
})

describe('stream SSE', () => {
  beforeEach(() => { vi.useFakeTimers(); FakeSource.all = [] })
  afterEach(() => vi.useRealTimers())
  const open = async (onEvent = vi.fn(), onDown = vi.fn()) => {
    const stop = new Rtdb(BASE, tok, fetch, FakeSource as unknown as typeof EventSource).stream('state', onEvent, onDown)
    await vi.advanceTimersByTimeAsync(0)
    return { s: FakeSource.all.at(-1)!, onEvent, onDown, stop }
  }

  it('apre con il token nella query e passa put e patch', async () => {
    const { s, onEvent } = await open()
    expect(s.url).toBe(`${BASE}/state.json?auth=T1`)
    s.emit('put', '{"path":"/","data":1}')
    s.emit('patch', '{"path":"/x","data":2}')
    expect(onEvent.mock.calls.map(c => c[0].event)).toEqual(['put', 'patch'])
  })

  it('90 s di silenzio chiudono e chiamano onDown una volta; il keep-alive li rimanda', async () => {
    const { s, onDown } = await open()
    await vi.advanceTimersByTimeAsync(60_000)
    s.emit('keep-alive')
    await vi.advanceTimersByTimeAsync(60_000)
    expect(onDown).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(31_000)
    expect(onDown).toHaveBeenCalledTimes(1)
    expect(onDown).toHaveBeenCalledWith('silence')
    expect(s.closed).toBe(true)
  })

  it('auth_revoked, cancel ed errore chiudono', async () => {
    for (const why of ['auth_revoked', 'cancel']) {
      const { s, onDown } = await open()
      s.emit(why)
      expect(onDown).toHaveBeenCalledWith(why)
      expect(s.closed).toBe(true)
    }
    const { s, onDown } = await open()
    s.onerror?.()
    expect(onDown).toHaveBeenCalledWith('error')
  })

  it('chiuso da chi l\'ha aperto non chiama onDown', async () => {
    const { s, onDown, stop } = await open()
    stop()
    s.onerror?.()
    await vi.advanceTimersByTimeAsync(100_000)
    expect(onDown).not.toHaveBeenCalled()
  })
})
```

- [ ] **Passo 2: eseguirli**

Eseguire: `npx vitest run src/lib/rtdb.test.ts`
Atteso: FAIL, modulo `./rtdb` mancante.

- [ ] **Passo 3: implementazione**

```ts
// Firebase Realtime Database via REST (`<path>.json?auth=<token>`) e streaming SSE, come Rtdb.kt: nessun SDK.
export type SseEvent = { event: string; data: string }

export class RtdbError extends Error {
  constructor(readonly status: number, what: string) { super(`${what}: HTTP ${status}`) }
}

export class Rtdb {
  constructor(
    private readonly base: string,
    private readonly token: (force?: boolean) => Promise<string>,
    private readonly http: typeof fetch = (...a) => fetch(...a),
    private readonly Source: typeof EventSource = globalThis.EventSource,
    /** RTDB manda un keep-alive ogni 30 s circa: 90 s di silenzio = connessione morta (N1 del 16/09). */
    private readonly silenceMs = 90_000,
  ) {}

  private async url(path: string, query: Record<string, string> = {}, force = false) {
    return `${this.base}/${path}.json?${new URLSearchParams({ ...query, auth: await this.token(force) })}`
  }

  /** Un 401 riprova una volta con un token nuovo: scaduto fra due rinnovi. */
  private async call(method: string, path: string, init: RequestInit = {}, query?: Record<string, string>): Promise<Response> {
    let r = await this.http(await this.url(path, query), { ...init, method })
    if (r.status === 401) r = await this.http(await this.url(path, query, true), { ...init, method })
    if (!r.ok) throw new RtdbError(r.status, `${method} ${path}`)
    return r
  }

  async get(path: string, query?: Record<string, string>): Promise<string | null> {
    const body = await (await this.call('GET', path, {}, query)).text()
    return body === 'null' || body.trim() === '' ? null : body
  }

  async put(path: string, body: string): Promise<void> {
    await this.call('PUT', path, { body, headers: { 'Content-Type': 'application/json' } })
  }

  async del(path: string): Promise<void> { await this.call('DELETE', path) }

  /**
   * Gli eventi `put` e `patch` di un nodo. `cancel`, `auth_revoked`, un errore o il silenzio chiudono e chiamano `onDown`
   * una volta: chi riapre, con il backoff e un token fresco, è il trasporto. EventSource non riapre da solo.
   */
  stream(path: string, onEvent: (e: SseEvent) => void, onDown: (why: string) => void): () => void {
    let es: EventSource | null = null
    let timer: ReturnType<typeof setTimeout> | undefined
    let closed = false
    const close = () => { closed = true; clearTimeout(timer); es?.close() }
    const down = (why: string) => { if (closed) return; close(); onDown(why) }
    const alive = () => { clearTimeout(timer); timer = setTimeout(() => down('silence'), this.silenceMs) }
    this.url(path).then(u => {
      if (closed) return
      es = new this.Source(u)
      alive()
      for (const name of ['put', 'patch']) es.addEventListener(name, (m) => { alive(); onEvent({ event: name, data: (m as MessageEvent).data }) })
      es.addEventListener('keep-alive', alive)
      es.addEventListener('cancel', () => down('cancel'))
      es.addEventListener('auth_revoked', () => down('auth_revoked'))
      es.onerror = () => down('error')
    }, (e) => down(String(e)))
    return close
  }
}
```

- [ ] **Passo 4: test verdi e tipi**

Eseguire: `npx vitest run src/lib/rtdb.test.ts && npm run check`
Atteso: 7 passed, 0 errori.

- [ ] **Passo 5: commit**

```bash
git add web/src/lib/rtdb.ts web/src/lib/rtdb.test.ts
git commit -m "feat(web): RTDB over REST and SSE — auth in the query, one retry on 401, stream closed on cancel, auth_revoked, error or 90 s of silence"
```

---

### Task 5: il trasporto remoto

**File:**
- Creare: `web/src/lib/firebaseTransport.ts`
- Modificare: `web/src/lib/transport.ts` (aggiungere `TooLargeError`)
- Test: `web/src/lib/firebaseTransport.test.ts`

**Interfacce:**
- Consuma: `Rtdb` (Task 4), `AesKey`, `sealBlob`, `openBlob`, `openBlobBytes`, `toB64`, `fromB64`, `toHex` (Task 2),
  `Transport`, `FileBlob`, `TransportError`, `resultTimeoutMs` (già in `transport.ts`).
- Produce:
  ```ts
  export class TooLargeError extends Error { readonly size: number; readonly max: number }   // in transport.ts
  export type Db = Pick<Rtdb, 'get' | 'put' | 'del' | 'stream'>
  export class FirebaseTransport implements Transport {
    constructor(db: Db, key: AesKey | Promise<AesKey>, uid: string, opts?: { pollMs?: number; backoffMs?: number[] })
  }
  ```

- [ ] **Passo 1: test che falliscono**

```ts
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { readFileSync } from 'node:fs'
import { FirebaseTransport, type Db } from './firebaseTransport'
import { fromHex, openBlob, sealBlob } from './crypto'
import { TooLargeError } from './transport'
import type { Cmd } from './contract'

const raw = (n: string) => readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8')
const parts = JSON.parse(raw('file-parts.json'))
const KEY = fromHex(parts.key)

/** Il bus finto: un dizionario di nodi; lo stream lo apre il test e lo comanda con emit/down. */
function bus() {
  const nodes = new Map<string, string>()
  const puts: string[] = []
  const streams: { onEvent: (e: { event: string; data: string }) => void; onDown: (w: string) => void }[] = []
  const db: Db = {
    get: async (p) => nodes.get(p) ?? null,
    put: async (p, b) => { puts.push(p); nodes.set(p, b) },
    del: async (p) => { for (const k of [...nodes.keys()]) if (k === p || k.startsWith(`${p}/`)) nodes.delete(k) },
    stream: (_p, onEvent, onDown) => { streams.push({ onEvent, onDown }); return () => {} },
  }
  return { db, nodes, puts, streams }
}

describe('trasporto remoto', () => {
  it('stato dal bus, e «ho ricevuto» una volta sola', async () => {
    const b = bus()
    b.nodes.set('state', await sealBlob(raw('state-2-idle.json'), KEY))
    const t = new FirebaseTransport(b.db, KEY, 'uid1')
    expect((await t.fetchState()).host).toBe(JSON.parse(raw('state-2-idle.json')).host)
    await t.fetchState()
    expect(b.puts.filter(p => p === 'seen/uid1')).toHaveLength(1)
    expect(b.nodes.get('seen/uid1')).toBe('{".sv":"timestamp"}')
  })

  describe('stream', () => {
    beforeEach(() => vi.useFakeTimers())
    afterEach(() => vi.useRealTimers())
    it('passa lo stato e riapre dopo il backoff quando cade', async () => {
      const b = bus()
      const got: string[] = []
      const down = vi.fn()
      new FirebaseTransport(b.db, KEY, 'uid1', { backoffMs: [1000, 2000] }).subscribe(s => got.push(s.host), down)
      const data = JSON.parse(await sealBlob(raw('state-2-idle.json'), KEY))
      b.streams[0].onEvent({ event: 'put', data: JSON.stringify({ path: '/', data }) })
      await vi.advanceTimersByTimeAsync(0)
      expect(got).toHaveLength(1)
      b.streams[0].onDown('auth_revoked')
      expect(down).toHaveBeenCalledTimes(1)
      await vi.advanceTimersByTimeAsync(999)
      expect(b.streams).toHaveLength(1)
      await vi.advanceTimersByTimeAsync(1)
      expect(b.streams).toHaveLength(2)
    })
  })

  it('comando cifrato in /cmd, risultato da /result; `file` chiede i pezzi', async () => {
    const b = bus()
    const t = new FirebaseTransport(b.db, KEY, 'uid1', { pollMs: 1 })
    const cmd: Cmd = { id: 'c1', op: 'file', session: 'field-notes', arg: '/x/cover.png', issued: 1, by: 'web', device: 'web' }
    b.nodes.set('result/c1', await sealBlob(JSON.stringify({ id: 'c1', ok: true, text: 'file ready', at: 2 }), KEY))
    expect((await t.send(cmd)).ok).toBe(true)
    expect(JSON.parse(await openBlob(b.nodes.get('cmd/c1')!, KEY))).toMatchObject({ op: 'file', parts: true, device: 'web' })
  })

  it('senza risposta in 20 s: errore di tempo', async () => {
    vi.useFakeTimers()
    try {
      const t = new FirebaseTransport(bus().db, KEY, 'uid1', { pollMs: 1000 })
      const p = t.send({ id: 'c2', op: 'prompt', session: 'x', arg: 'ciao', issued: 1, by: 'web' })
      const check = expect(p).rejects.toMatchObject({ status: 408 })
      await vi.advanceTimersByTimeAsync(21_000)
      await check
    } finally { vi.useRealTimers() }
  })

  it('un allegato oltre share.max_bytes si rifiuta prima di scrivere', async () => {
    const b = bus()
    const st = { ...JSON.parse(raw('state-2-idle.json')), share: { max_bytes: 400, any: true } }
    b.nodes.set('state', await sealBlob(JSON.stringify(st), KEY))
    const t = new FirebaseTransport(b.db, KEY, 'uid1')
    await t.fetchState()
    // 400 byte diventano più di 600 caratteri di busta; 10 byte ne fanno circa 120.
    await expect(t.share('s1', 'text/plain', new Uint8Array(400), 'a.txt')).rejects.toBeInstanceOf(TooLargeError)
    expect(b.nodes.has('share/s1')).toBe(false)
    await t.share('s2', 'text/plain', new Uint8Array(10), 'b.txt')
    expect(JSON.parse(await openBlob(b.nodes.get('share/s2')!, KEY))).toMatchObject({ mime: 'text/plain', name: 'b.txt' })
  })

  it('file a pezzi della 1.34: ricompone, controlla sha256 e cancella il nodo', async () => {
    const b = bus()
    b.nodes.set('file/f1/meta', JSON.stringify(parts.meta))
    parts.parts.forEach((p: unknown, k: number) => b.nodes.set(`file/f1/parts/${k}`, JSON.stringify(p)))
    const f = await new FirebaseTransport(b.db, KEY, 'uid1').fetchFile('f1')
    expect(new TextDecoder().decode(f!.data)).toBe(parts.file)
    expect(f).toMatchObject({ mime: 'text/plain', name: 'hello.txt' })
    expect([...b.nodes.keys()].some(k => k.startsWith('file/f1'))).toBe(false)
  })

  it('pezzi scambiati: sha256 diverso, errore', async () => {
    const b = bus()
    b.nodes.set('file/f2/meta', JSON.stringify(parts.meta))
    b.nodes.set('file/f2/parts/0', JSON.stringify(parts.parts[1]))
    b.nodes.set('file/f2/parts/1', JSON.stringify(parts.parts[0]))
    await expect(new FirebaseTransport(b.db, KEY, 'uid1').fetchFile('f2')).rejects.toThrow('sha256')
  })

  it('eventi da un ts, dal più recente, illeggibili saltati', async () => {
    const b = bus()
    const evs = JSON.parse(raw('events-sample.json')) as { key: string; ts: number }[]
    const node: Record<string, unknown> = { junk: { v: 1, enc: 'AAAA' } }
    for (const e of evs) node[e.key] = JSON.parse(await sealBlob(JSON.stringify(e), KEY))
    b.nodes.set('events', JSON.stringify(node))
    const since = Math.min(...evs.map(e => e.ts))
    const got = await new FirebaseTransport(b.db, KEY, 'uid1').fetchEvents(since)
    expect(got.every(e => e.ts > since)).toBe(true)
    expect(got.map(e => e.ts)).toEqual([...got.map(e => e.ts)].sort((a, b) => b - a))
  })
})
```
Il fake `get` ignora la query: `fetchEvents` chiama `get('events', {orderBy, limitToLast})` e trova il nodo `events`.

- [ ] **Passo 2: eseguirli**

Eseguire: `npx vitest run src/lib/firebaseTransport.test.ts`
Atteso: FAIL, modulo mancante.

- [ ] **Passo 3: `TooLargeError` in `transport.ts`**

Sotto `TransportError`:
```ts
/** Contratto 1.19: sulla strada remota la busta di un allegato ha un tetto (`state.share.max_bytes`). */
export class TooLargeError extends Error {
  constructor(readonly size: number, readonly max: number) { super(`too large: ${size} max ${max}`) }
}
```

- [ ] **Passo 4: implementazione di `firebaseTransport.ts`**

```ts
// La strada remota: il bus RTDB come il telefono (FirebaseTransport.kt), ogni documento una busta {v, enc} con la chiave
// del relay. Stessa interfaccia della strada locale, così App non sa da dove arrivano i dati.
import { decodeState, type Cmd, type CmdResult, type Event, type State } from './contract'
import { fromB64, openBlob, openBlobBytes, sealBlob, toB64, toHex, type AesKey, type Bytes } from './crypto'
import type { Rtdb, SseEvent } from './rtdb'
import { resultTimeoutMs, TooLargeError, TransportError, type FileBlob, type Transport } from './transport'

export type Db = Pick<Rtdb, 'get' | 'put' | 'del' | 'stream'>
const BACKOFF_MS = [1000, 2000, 5000, 15000, 30000]
const isBlob = (x: unknown): x is { enc: string } => !!x && typeof x === 'object' && 'enc' in x

export class FirebaseTransport implements Transport {
  private maxShare: number | null = null
  private seen = false

  constructor(
    private readonly db: Db,
    private readonly key: AesKey | Promise<AesKey>,
    private readonly uid: string,
    private readonly opts: { pollMs?: number; backoffMs?: number[] } = {},
  ) {}

  private async open(doc: string): Promise<string> {
    try { return await openBlob(doc, await this.key) } catch (e) { throw new TransportError(0, `cannot decrypt: ${(e as Error).message}`) }
  }

  private took(s: State): State {
    this.maxShare = s.share?.max_bytes ?? null
    // Contratto 1.20: «ho ricevuto», con l'ora del server; un rifiuto delle regole non blocca la lettura.
    if (!this.seen) { this.seen = true; this.db.put(`seen/${this.uid}`, '{".sv":"timestamp"}').catch(() => {}) }
    return s
  }

  async fetchState(): Promise<State> {
    const body = await this.db.get('state')
    if (!body) throw new TransportError(404, 'no state on the bus')
    return this.took(decodeState(await this.open(body)))
  }

  subscribe(onState: (s: State) => void, onDown?: () => void): () => void {
    const backoff = this.opts.backoffMs ?? BACKOFF_MS
    let attempt = 0
    let stop: (() => void) | null = null
    let timer: ReturnType<typeof setTimeout> | undefined
    let closed = false
    const connect = () => {
      stop = this.db.stream('state', (e: SseEvent) => {
        attempt = 0
        let o: { path?: unknown; data?: unknown }
        try { o = JSON.parse(e.data) } catch { return }
        if (o.path !== '/' || !isBlob(o.data)) return
        this.open(JSON.stringify(o.data)).then(t => onState(this.took(decodeState(t))), () => { /* uno stato rotto si salta */ })
      }, () => {
        if (closed) return
        onDown?.()
        timer = setTimeout(connect, backoff[Math.min(attempt++, backoff.length - 1)])
      })
    }
    connect()
    return () => { closed = true; clearTimeout(timer); stop?.() }
  }

  async fetchEvents(since = 0): Promise<Event[]> {
    const body = await this.db.get('events', { orderBy: '"$key"', limitToLast: '200' })
    if (!body) return []
    const out: Event[] = []
    for (const v of Object.values(JSON.parse(body) as Record<string, unknown>)) {
      if (!isBlob(v)) continue
      try {
        const e = JSON.parse(await this.open(JSON.stringify(v))) as Event
        if (e.ts > since) out.push(e)
      } catch { /* un evento illeggibile si salta */ }
    }
    return out.sort((a, b) => b.ts - a.ts)
  }

  async send(cmd: Cmd): Promise<CmdResult> {
    // Contratto 1.34: sulla strada remota un file arriva a pezzi, fino a 25 MB.
    const c = cmd.op === 'file' ? { ...cmd, parts: true } : cmd
    await this.db.put(`cmd/${c.id}`, await sealBlob(JSON.stringify(c), await this.key))
    const until = Date.now() + resultTimeoutMs(c.op)
    const poll = this.opts.pollMs ?? 1000
    while (Date.now() < until) {
      const r = await this.db.get(`result/${c.id}`)
      if (r) return JSON.parse(await this.open(r)) as CmdResult
      await new Promise(res => setTimeout(res, poll))
    }
    throw new TransportError(408, 'timeout')
  }

  async share(id: string, mime: string, data: Uint8Array, name?: string | null): Promise<void> {
    const plain: Record<string, string> = { mime, data: toB64(data as Bytes) }
    if (name) plain.name = name
    const doc = await sealBlob(JSON.stringify(plain), await this.key)
    const enc = (JSON.parse(doc) as { enc: string }).enc
    if (this.maxShare != null && enc.length > this.maxShare) throw new TooLargeError(enc.length, this.maxShare)
    await this.db.put(`share/${id}`, doc)
  }

  async fetchFile(id: string): Promise<FileBlob | null> {
    const m = await this.db.get(`file/${id}/meta`)
    if (m) {
      const meta = JSON.parse(await this.open(m)) as { n: number; size: number; sha256: string; mime: string; name?: string | null }
      const chunks: Uint8Array[] = []
      try {
        for (let k = 0; k < meta.n; k++) {
          const p = await this.db.get(`file/${id}/parts/${k}`)
          if (!p) throw new TransportError(404, `file ${id}: part ${k} missing`)
          chunks.push(await openBlobBytes(p, await this.key))
        }
      } finally { this.db.del(`file/${id}`).catch(() => {}) }
      const data = new Uint8Array(chunks.reduce((n, c) => n + c.length, 0))
      let o = 0
      for (const c of chunks) { data.set(c, o); o += c.length }
      const sha = toHex(new Uint8Array(await crypto.subtle.digest('SHA-256', data)))
      if (data.length !== meta.size || sha !== meta.sha256) throw new TransportError(0, `file ${id}: size or sha256 mismatch`)
      return { data, mime: meta.mime, name: meta.name ?? null }
    }
    const body = await this.db.get(`file/${id}`)
    if (!body) return null
    const plain = JSON.parse(await this.open(body)) as { mime: string; data: string; name?: string | null }
    this.db.del(`file/${id}`).catch(() => {})
    return { data: fromB64(plain.data), mime: plain.mime, name: plain.name ?? null }
  }
}
```

- [ ] **Passo 5: test verdi, suite intera, tipi**

Eseguire: `npx vitest run && npm run check`
Atteso: tutto verde, 0 errori.

- [ ] **Passo 6: commit**

```bash
git add web/src/lib/firebaseTransport.ts web/src/lib/firebaseTransport.test.ts web/src/lib/transport.ts
git commit -m "feat(web): remote transport over RTDB — sealed state, events, commands with results, shares under max_bytes, files in parts with sha256, seen marker, reconnect with backoff"
```

---

### Task 6: l'accoppiamento del browser

**File:**
- Creare: `web/src/lib/pairing.ts`
- Test: `web/src/lib/pairing.test.ts`

**Interfacce:**
- Consuma: `Rtdb.get`/`put` (Task 4), `generatePair`, `privateFromScalar`, `publicB64`, `sharedKey`, `checkCode`,
  `openBlob`, `fromB64`, `fromHex`, `Bytes` (`crypto.ts`).
- Produce:
  ```ts
  export type QrFirebase = { k: string; p: string; a: string; d: string; t: string }
  export type PairQr = { v: number; i: string; c: string; h: string; e: number; f: QrFirebase; m?: string | null }
  export function parseQr(text: string): PairQr | null
  export function inviteFromHash(hash: string): PairQr | null
  export function webLink(origin: string, line: string): string
  export function parseCode(text: string): string | null
  export function deviceName(ua: string): string
  export type PairReason = 'expired' | 'unknown' | 'not-add' | 'no-confirm' | 'bad-confirm' | 'full' | 'network'
  export class PairError extends Error { readonly reason: PairReason }
  export type PairInput = { node: string; pcPub?: string; host?: string; exp?: number; add?: boolean }
  export function pair(db: Pick<Rtdb, 'get' | 'put'>, inv: PairInput, me: { uid: string; name: string },
    opts?: { now?: () => number; pollMs?: number; timeoutMs?: number; keyPair?: CryptoKeyPair }): Promise<{ key: Bytes; host: string }>
  ```

- [ ] **Passo 1: test che falliscono**

```ts
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { deviceName, inviteFromHash, pair, PairError, parseCode, parseQr, webLink } from './pairing'
import { privateFromScalar, toHex } from './crypto'

const fx = (n: string) => JSON.parse(readFileSync(new URL(`../../../contract/${n}`, import.meta.url), 'utf8'))
const add = fx('pair-add.json')
const link = fx('pair-link.json')
const range = (a: number, b: number) => Uint8Array.from({ length: b - a }, (_, i) => a + i)

/** Il PC finto: il documento del nodo, la conferma scritta dopo la risposta del browser. */
function pc(doc: unknown, ok: unknown, error?: unknown) {
  const puts: [string, unknown][] = []
  let answered = false
  return {
    puts,
    db: {
      get: async (p: string) => {
        if (p === `pair/${add.qr.i}`) return doc == null ? null : JSON.stringify(doc)
        if (p.endsWith('/ok')) return answered && ok != null ? JSON.stringify(ok) : null
        if (p.endsWith('/error')) return answered && error != null ? JSON.stringify(error) : null
        return null
      },
      put: async (p: string, b: string) => { puts.push([p, JSON.parse(b)]); answered = true },
    },
  }
}
const doc = { pc_pub: add.qr.c, host: 'penguin', exp: add.qr.e, mode: 'add' }
const me = { uid: add.watch.uid, name: add.watch.name }
const opts = async () => ({ now: () => add.qr.e - 60, pollMs: 1, timeoutMs: 200, keyPair: await privateFromScalar(range(64, 96)) })

describe('inviti', () => {
  it('legge il QR della 1.15 e quello --add della 1.30, rifiuta il resto', () => {
    expect(parseQr(JSON.stringify(fx('pair-qr.json')))?.h).toBe('penguin')
    expect(parseQr(link.line)?.m).toBe('add')
    expect(parseQr('{"v":2}')).toBeNull()
    expect(parseQr('ciao')).toBeNull()
  })
  it('il link per il browser porta la stessa riga del link dell\'app, nel frammento', () => {
    const l = webLink('https://demo.web.app', link.line)
    expect(l.split('#pair=')[1]).toBe(link.uri.split('q=')[1])
    expect(inviteFromHash(new URL(l).hash)?.i).toBe(add.qr.i)
    expect(inviteFromHash('#altro')).toBeNull()
  })
  it('il codice a 6 cifre, anche con lo spazio', () => {
    expect(parseCode('482 913')).toBe('482913')
    expect(parseCode('48291')).toBeNull()
  })
  it('il nome del dispositivo dal browser', () => {
    expect(deviceName('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15')).toBe('Safari su Mac')
    expect(deviceName('Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/130.0 Mobile/15E148 Safari/604.1')).toBe('Chrome su iPhone')
    expect(deviceName('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36 Edg/130.0')).toBe('Edge su Windows')
  })
})

describe('accoppiamento (pair-add.json)', () => {
  it('risponde con chiave pubblica, check e tipo «web», verifica il PC e prende la chiave del relay', async () => {
    const p = pc(doc, add.ok)
    const r = await pair(p.db, { node: add.qr.i, add: true }, me, await opts())
    expect(toHex(r.key)).toBe(add.relay_key)
    expect(r.host).toBe('penguin')
    const [path, body] = p.puts[0]
    expect(path).toBe(`pair/${add.qr.i}/watch`)
    expect(body).toMatchObject({ watch_pub: add.watch.watch_pub, uid: me.uid, check: add.watch.check, kind: 'web', uids: [me.uid] })
  })
  it('invito di primo accoppiamento: rifiutato senza rispondere', async () => {
    const p = pc({ ...doc, mode: undefined }, add.ok)
    await expect(pair(p.db, { node: add.qr.i }, me, await opts())).rejects.toMatchObject({ reason: 'not-add' })
    expect(p.puts).toEqual([])
  })
  it('scaduto, sconosciuto, pieno, senza conferma, conferma sbagliata', async () => {
    const o = await opts()
    await expect(pair(pc(doc, add.ok).db, { node: add.qr.i, exp: add.qr.e }, me, { ...o, now: () => add.qr.e + 1 })).rejects.toMatchObject({ reason: 'expired' })
    await expect(pair(pc(null, null).db, { node: add.qr.i }, me, o)).rejects.toMatchObject({ reason: 'unknown' })
    await expect(pair(pc(doc, null, 'full').db, { node: add.qr.i }, me, o)).rejects.toMatchObject({ reason: 'full' })
    await expect(pair(pc(doc, null).db, { node: add.qr.i }, me, o)).rejects.toMatchObject({ reason: 'no-confirm' })
    await expect(pair(pc(doc, { ...add.ok, check: '0000000000000000' }).db, { node: add.qr.i }, me, o)).rejects.toBeInstanceOf(PairError)
  })
})
```

- [ ] **Passo 2: eseguirli**

Eseguire: `npx vitest run src/lib/pairing.test.ts`
Atteso: FAIL, modulo mancante.

- [ ] **Passo 3: implementazione**

```ts
// L'accoppiamento del browser, come PhonePairer.kt: solo come dispositivo in più (`pair --add`), con l'invito del QR,
// il link `#pair=` o il codice a 6 cifre. Nessuna chiave nuova per il relay: quella che ha arriva cifrata nella conferma.
import { checkCode, fromB64, fromHex, generatePair, openBlob, publicB64, sharedKey, type Bytes } from './crypto'
import type { Rtdb } from './rtdb'

export type QrFirebase = { k: string; p: string; a: string; d: string; t: string }
export type PairQr = { v: number; i: string; c: string; h: string; e: number; f: QrFirebase; m?: string | null }
export type PairReason = 'expired' | 'unknown' | 'not-add' | 'no-confirm' | 'bad-confirm' | 'full' | 'network'
export class PairError extends Error {
  constructor(readonly reason: PairReason, detail = '') { super(detail || reason) }
}
export type PairInput = { node: string; pcPub?: string; host?: string; exp?: number; add?: boolean }

const ID = /^[A-Za-z0-9_-]{22}$/
const filled = (x: unknown) => typeof x === 'string' && x.trim() !== ''

/** Il testo del QR o della riga di `relay pair --add --text`; null se non è un invito di claude-master. */
export function parseQr(text: string): PairQr | null {
  let q: PairQr
  try { q = JSON.parse(text.trim()) } catch { return null }
  if (!q || typeof q !== 'object' || q.v !== 1 || !ID.test(String(q.i)) || !filled(q.h) || !q.f) return null
  let pubOk = false
  try { pubOk = fromB64(q.c).length === 32 } catch { /* resta falso */ }
  const fbOk = [q.f.k, q.f.p, q.f.a, q.f.t].every(filled) && String(q.f.d).startsWith('https://')
  return pubOk && fbOk ? q : null
}

const b64url = (s: string) => btoa(Array.from(new TextEncoder().encode(s), b => String.fromCharCode(b)).join('')).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
const unb64url = (s: string) => new TextDecoder().decode(Uint8Array.from(atob(s.replace(/-/g, '+').replace(/_/g, '/')), c => c.charCodeAt(0)))

/** Il link per un browser: la riga dell'invito nel frammento, che non arriva al server. */
export const webLink = (origin: string, line: string) => `${origin}/#pair=${b64url(line)}`

export function inviteFromHash(hash: string): PairQr | null {
  const m = /^#pair=([A-Za-z0-9_-]+)$/.exec(hash)
  if (!m) return null
  try { return parseQr(unb64url(m[1])) } catch { return null }
}

/** Il codice a 6 cifre mostrato dal telefono, con o senza lo spazio in mezzo. */
export const parseCode = (text: string) => { const c = text.replace(/\s/g, ''); return /^\d{6}$/.test(c) ? c : null }

/** «Safari su Mac»: il nome che il telefono e il PC mostrano fra i dispositivi. */
export function deviceName(ua: string): string {
  const os = /iPhone/.test(ua) ? 'iPhone' : /iPad/.test(ua) ? 'iPad' : /Android/.test(ua) ? 'Android' : /CrOS/.test(ua) ? 'Chromebook'
    : /Macintosh|Mac OS X/.test(ua) ? 'Mac' : /Windows/.test(ua) ? 'Windows' : /Linux/.test(ua) ? 'Linux' : 'browser'
  const br = /Edg\//.test(ua) ? 'Edge' : /Firefox\/|FxiOS/.test(ua) ? 'Firefox' : /CriOS|Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : 'Browser'
  return `${br} su ${os}`
}

type Db = Pick<Rtdb, 'get' | 'put'>
type Confirm = { check?: unknown; host?: unknown; key?: unknown }

export async function pair(db: Db, inv: PairInput, me: { uid: string; name: string },
  opts: { now?: () => number; pollMs?: number; timeoutMs?: number; keyPair?: CryptoKeyPair } = {}): Promise<{ key: Bytes; host: string }> {
  const now = opts.now ?? (() => Math.floor(Date.now() / 1000))
  if (inv.exp != null && now() > inv.exp) throw new PairError('expired')
  try {
    const raw = await db.get(`pair/${inv.node}`)
    if (!raw) throw new PairError('unknown')
    const doc = JSON.parse(raw) as { pc_pub?: string; host?: string; exp?: number; mode?: string }
    if (doc.exp != null && now() > doc.exp) throw new PairError('expired')
    // Mai un primo accoppiamento dal browser: il relay cambierebbe la chiave di tutti gli altri dispositivi.
    if (!(inv.add ?? false) && doc.mode !== 'add') throw new PairError('not-add')
    const pcPub = inv.pcPub ?? doc.pc_pub
    if (!pcPub) throw new PairError('unknown')
    const kp = opts.keyPair ?? await generatePair()
    const shared = await sharedKey(kp, pcPub)
    await db.put(`pair/${inv.node}/watch`, JSON.stringify({
      watch_pub: await publicB64(kp.publicKey), uid: me.uid, name: me.name, check: await checkCode(shared, inv.node),
      uids: [me.uid], names: { [me.uid]: me.name }, kind: 'web', kinds: { [me.uid]: 'web' },
    }))
    const ok = await confirm(db, inv.node, opts.pollMs ?? 1000, opts.timeoutMs ?? 30_000)
    if (ok.check !== await checkCode(shared, `${inv.node}:pc`)) throw new PairError('bad-confirm')
    return { key: await relayKey(ok.key, shared), host: String(ok.host ?? inv.host ?? doc.host ?? '') }
  } catch (e) {
    if (e instanceof PairError) throw e
    throw new PairError('network', String((e as Error)?.message ?? e))
  }
}

async function confirm(db: Db, node: string, pollMs: number, timeoutMs: number): Promise<Confirm> {
  const until = Date.now() + timeoutMs
  while (Date.now() < until) {
    const ok = await db.get(`pair/${node}/ok`)
    if (ok) {
      let o: unknown
      try { o = JSON.parse(ok) } catch { throw new PairError('bad-confirm') }
      if (!o || typeof o !== 'object') throw new PairError('bad-confirm')
      return o as Confirm
    }
    // Contratto 1.30: con i posti finiti il PC scrive {"error": "full"} al posto della conferma.
    const err = await db.get(`pair/${node}/error`)
    if (err && err.replace(/"/g, '') === 'full') throw new PairError('full')
    await new Promise(r => setTimeout(r, pollMs))
  }
  throw new PairError('no-confirm')
}

/** `ok.key` = busta {v, enc} con dentro {"key": "<64 cifre hex>"}, cifrata con la chiave del giro. */
async function relayKey(env: unknown, shared: Bytes): Promise<Bytes> {
  if (!env || typeof env !== 'object') throw new PairError('bad-confirm')
  let hex: unknown
  try { hex = (JSON.parse(await openBlob(JSON.stringify(env), shared)) as { key?: unknown }).key } catch { throw new PairError('bad-confirm') }
  if (typeof hex !== 'string' || !/^[0-9a-fA-F]{64}$/.test(hex)) throw new PairError('bad-confirm')
  return fromHex(hex)
}
```

- [ ] **Passo 4: test verdi e tipi**

Eseguire: `npx vitest run src/lib/pairing.test.ts && npm run check`
Atteso: 7 passed, 0 errori.

- [ ] **Passo 5: commit**

```bash
git add web/src/lib/pairing.ts web/src/lib/pairing.test.ts
git commit -m "feat(web): browser pairing as an extra device only (pair --add) — QR line, #pair= link and 6-digit code, kind web, full and expired refused, verified on pair-add.json"
```

---

### Task 7: dove sta l'accoppiamento

**File:**
- Creare: `web/src/lib/vault.ts`
- Test: `web/src/lib/vault.test.ts`

**Interfacce:**
- Consuma: `AuthSession`, `AuthStore` (Task 3).
- Produce:
  ```ts
  export type Remote = { apiKey: string; projectId: string; databaseUrl: string; host: string }
  export type Kv = { get(k: string): string | null; set(k: string, v: string): void; del(k: string): void }
  export function loadRemote(kv: Kv): Remote | null
  export function saveRemote(kv: Kv, r: Remote): void
  export function authStore(kv: Kv): AuthStore
  export interface KeyStore { load(): Promise<CryptoKey | null>; save(k: CryptoKey): Promise<void>; clear(): Promise<void> }
  export function idbKeyStore(): KeyStore
  export function forget(kv: Kv, keys: KeyStore): Promise<void>
  ```

- [ ] **Passo 1: test che falliscono**

```ts
import { describe, expect, it } from 'vitest'
import { authStore, forget, loadRemote, saveRemote, type KeyStore, type Kv } from './vault'

const kv = (): Kv & { m: Map<string, string> } => {
  const m = new Map<string, string>()
  return { m, get: k => m.get(k) ?? null, set: (k, v) => { m.set(k, v) }, del: k => { m.delete(k) } }
}

describe('accoppiamento salvato', () => {
  it('configurazione e sessione in localStorage, mai la chiave', () => {
    const s = kv()
    const r = { apiKey: 'K', projectId: 'p', databaseUrl: 'https://p.firebasedatabase.app', host: 'penguin' }
    saveRemote(s, r)
    expect(loadRemote(s)).toEqual(r)
    authStore(s).save({ uid: 'u', idToken: 't', refreshToken: 'r', expiresAt: 1 })
    expect(authStore(s).load()?.uid).toBe('u')
    expect([...s.m.values()].join()).not.toMatch(/[0-9a-f]{64}/)
  })
  it('valori rotti valgono «non accoppiato»', () => {
    const s = kv()
    s.set('cm.remote', '{"apiKey":"K"}')
    s.set('cm.auth', 'rotto')
    expect(loadRemote(s)).toBeNull()
    expect(authStore(s).load()).toBeNull()
  })
  it('scollegare toglie tutto', async () => {
    const s = kv()
    saveRemote(s, { apiKey: 'K', projectId: 'p', databaseUrl: 'https://x', host: 'h' })
    authStore(s).save({ uid: 'u', idToken: 't', refreshToken: 'r', expiresAt: 1 })
    let cleared = false
    const keys: KeyStore = { load: async () => null, save: async () => {}, clear: async () => { cleared = true } }
    await forget(s, keys)
    expect(s.m.size).toBe(0)
    expect(cleared).toBe(true)
  })
})
```

- [ ] **Passo 2: eseguirli**

Eseguire: `npx vitest run src/lib/vault.test.ts`
Atteso: FAIL, modulo mancante.

- [ ] **Passo 3: implementazione**

```ts
// L'accoppiamento di questo browser: configurazione Firebase, host e sessione anonima in localStorage; la chiave del relay
// in IndexedDB come CryptoKey non estraibile (la pagina la usa, nessuno script la può leggere o esportare).
import type { AuthSession, AuthStore } from './auth'

export type Remote = { apiKey: string; projectId: string; databaseUrl: string; host: string }
export type Kv = { get(k: string): string | null; set(k: string, v: string): void; del(k: string): void }
const REMOTE = 'cm.remote'
const AUTH = 'cm.auth'

const parse = <T,>(s: string | null): T | null => { try { return s ? JSON.parse(s) as T : null } catch { return null } }

export function loadRemote(kv: Kv): Remote | null {
  const r = parse<Remote>(kv.get(REMOTE))
  return r && r.apiKey && r.databaseUrl?.startsWith('https://') && r.projectId && r.host != null ? r : null
}
export const saveRemote = (kv: Kv, r: Remote) => kv.set(REMOTE, JSON.stringify(r))

export function authStore(kv: Kv): AuthStore {
  return {
    load: () => { const s = parse<AuthSession>(kv.get(AUTH)); return s?.uid && s.refreshToken ? s : null },
    save: (s) => kv.set(AUTH, JSON.stringify(s)),
    clear: () => kv.del(AUTH),
  }
}

export interface KeyStore { load(): Promise<CryptoKey | null>; save(k: CryptoKey): Promise<void>; clear(): Promise<void> }

/** IndexedDB `cm`, archivio `keys`, voce `relay`: la CryptoKey si salva così com'è (clone strutturato). */
export function idbKeyStore(): KeyStore {
  const db = () => new Promise<IDBDatabase>((ok, ko) => {
    const r = indexedDB.open('cm', 1)
    r.onupgradeneeded = () => r.result.createObjectStore('keys')
    r.onsuccess = () => ok(r.result)
    r.onerror = () => ko(r.error)
  })
  const run = async <T,>(mode: IDBTransactionMode, f: (s: IDBObjectStore) => IDBRequest<T>) => {
    const d = await db()
    try {
      return await new Promise<T>((ok, ko) => { const q = f(d.transaction('keys', mode).objectStore('keys')); q.onsuccess = () => ok(q.result); q.onerror = () => ko(q.error) })
    } finally { d.close() }
  }
  return {
    load: async () => (await run<CryptoKey | undefined>('readonly', s => s.get('relay'))) ?? null,
    save: async (k) => { await run('readwrite', s => s.put(k, 'relay')) },
    clear: async () => { await run('readwrite', s => s.delete('relay')) },
  }
}

export async function forget(kv: Kv, keys: KeyStore) {
  kv.del(REMOTE)
  kv.del(AUTH)
  await keys.clear()
}
```

- [ ] **Passo 4: test verdi e tipi**

Eseguire: `npx vitest run src/lib/vault.test.ts && npm run check`
Atteso: 3 passed, 0 errori.

- [ ] **Passo 5: commit**

```bash
git add web/src/lib/vault.ts web/src/lib/vault.test.ts
git commit -m "feat(web): where the browser keeps its pairing — config and anonymous session in localStorage, the relay key as a non-extractable CryptoKey in IndexedDB"
```

---

### Task 8: «Collega questo browser» e la scelta della strada

**File:**
- Creare: `web/src/lib/Pair.svelte`
- Modificare: `web/src/App.svelte` (blocco della strada, righe dell'avvio del trasporto), `web/src/lib/Settings.svelte`,
  `web/src/lib/t.ts`

**Interfacce:**
- Consuma: `AnonAuth` (3), `Rtdb` (4), `FirebaseTransport`, `TooLargeError` (5), `pair`, `parseQr`, `inviteFromHash`,
  `parseCode`, `deviceName`, `PairError` (6), `loadRemote`, `saveRemote`, `authStore`, `idbKeyStore`, `forget` (7),
  `importAesKey` (2).
- Produce: la pagina servita da Firebase Hosting funziona da sola: accoppia, poi mostra i dati veri.

- [ ] **Passo 1: i testi in `t.ts`**

Accanto a `channelFirebase`:
```ts
  pairTitle: 'Collega questo browser',
  pairHow: 'Sul telefono: ≡, Dispositivi, Aggiungi un dispositivo. Scrivi qui il codice a 6 cifre.',
  pairCode: 'Codice a 6 cifre', pairPaste: "Oppure incolla l'invito di relay pair --add --text", pairGo: 'Collega',
  pairConfirm: (h: string) => `Collegare questo browser a ${h}?`, pairWorking: 'Collego: conferma sul PC',
  pairErr: {
    expired: 'Codice scaduto: chiedine uno nuovo dal telefono.', unknown: 'Codice non valido o già usato.',
    'not-add': "Questo è l'invito del primo accoppiamento: dal telefono usa «Aggiungi un dispositivo».",
    'no-confirm': 'Il PC non ha confermato: riprova.', 'bad-confirm': 'La conferma del PC non torna: riprova con un codice nuovo.',
    full: 'Il PC ha già il massimo dei dispositivi: scollegane uno dal telefono.', network: 'Firebase non risponde: controlla la rete e riprova.',
  } as Record<string, string>,
  pairSignedOut: 'Questo browser va ricollegato: il suo accesso a Firebase non vale più.',
  forgetBrowser: 'Scollega questo browser', forgetBrowserSub: 'Toglie la chiave e il collegamento da questo browser',
  tooLarge: (mb: string, max: string) => `File troppo grande per la strada remota: ${mb} MB, al massimo ${max} MB`,
```

- [ ] **Passo 2: `Pair.svelte`**

```svelte
<script lang="ts">
  import { AnonAuth } from './auth'
  import { importAesKey } from './crypto'
  import { deviceName, inviteFromHash, pair, PairError, parseCode, parseQr, type PairQr } from './pairing'
  import { Rtdb } from './rtdb'
  import { authStore, idbKeyStore, saveRemote, type Kv, type Remote } from './vault'
  import { t } from './t'

  // La pagina servita da Firebase Hosting quando il browser non è collegato: invito dal link `#pair=`, dal testo
  // incollato o dal codice a 6 cifre (la configurazione Firebase allora la dà l'hosting, /__/firebase/init.json).
  let { kv, signedOut = false }: { kv: Kv; signedOut?: boolean } = $props()
  const fromLink = inviteFromHash(location.hash)
  let text = $state('')
  let busy = $state(false)
  let error = $state<string | null>(signedOut ? t.pairSignedOut : null)

  async function hostingConfig(): Promise<Omit<Remote, 'host'>> {
    const c = await (await fetch('/__/firebase/init.json')).json()
    return { apiKey: c.apiKey, projectId: c.projectId, databaseUrl: c.databaseURL }
  }

  async function go(qr: PairQr | null, code: string | null) {
    busy = true; error = null
    try {
      const cfg = qr ? { apiKey: qr.f.k, projectId: qr.f.p, databaseUrl: qr.f.d } : await hostingConfig()
      const auth = new AnonAuth(cfg.apiKey, authStore(kv))
      const me = await auth.signUp()
      const db = new Rtdb(cfg.databaseUrl, (f) => auth.token(f))
      const input = qr ? { node: qr.i, pcPub: qr.c, host: qr.h, exp: qr.e, add: qr.m === 'add' } : { node: code! }
      const r = await pair(db, input, { uid: me.uid, name: deviceName(navigator.userAgent) })
      await idbKeyStore().save(await importAesKey(r.key))
      saveRemote(kv, { ...cfg, host: r.host })
      location.replace(location.pathname)
    } catch (e) {
      error = e instanceof PairError ? t.pairErr[e.reason] : t.pairErr.network
    } finally { busy = false }
  }

  function submit(e: SubmitEvent) {
    e.preventDefault()
    const qr = parseQr(text)
    const code = qr ? null : parseCode(text)
    if (!qr && !code) { error = t.pairErr.unknown; return }
    go(qr, code)
  }
</script>

<main class="pair">
  <h1>{t.pairTitle}</h1>
  {#if fromLink}
    <p>{t.pairConfirm(fromLink.h)}</p>
    <button class="filled" disabled={busy} onclick={() => go(fromLink, null)}>{busy ? t.pairWorking : t.pairGo}</button>
  {:else}
    <p class="sub">{t.pairHow}</p>
    <form onsubmit={submit}>
      <label>{t.pairCode}<textarea rows="2" bind:value={text} placeholder="482 913" autocomplete="one-time-code"></textarea></label>
      <p class="sub">{t.pairPaste}</p>
      <button class="filled" disabled={busy || !text.trim()}>{busy ? t.pairWorking : t.pairGo}</button>
    </form>
  {/if}
  {#if error}<p class="err" role="alert">{error}</p>{/if}
</main>

<style>
  .pair { max-width: 520px; margin: 0 auto; padding: 48px 20px; display: flex; flex-direction: column; gap: 16px; }
  h1 { font-size: 26px; font-weight: 500; }
  .sub { color: var(--text2); font-size: 14px; }
  form { display: flex; flex-direction: column; gap: 12px; }
  label { display: flex; flex-direction: column; gap: 8px; color: var(--text2); font-size: 13px; }
  textarea { background: var(--high); color: var(--text); border: 0; border-radius: 16px; padding: 12px 14px; font: 18px var(--mono); resize: vertical; }
  .filled { background: var(--primary); color: var(--on-primary); border-radius: 999px; padding: 12px 20px; font-weight: 500; }
  .filled:disabled { opacity: .5; cursor: default; }
  .err { color: var(--b-alert); }
</style>
```

- [ ] **Passo 3: la scelta della strada in `App.svelte`**

Al posto delle righe
```ts
  const access = localAccess(new URL(location.href), { get: load, set: save })
  if (access?.clean != null) history.replaceState(null, '', access.clean)
  const tr = access ? new LocalTransport(access.base, access.token) : null
```
scrivere:
```ts
  const kv: Kv = { get: load, set: save, del: (k) => { try { localStorage.removeItem(k) } catch { /* niente */ } } }
  const access = localAccess(new URL(location.href), { get: load, set: save })
  if (access?.clean != null) history.replaceState(null, '', access.clean)
  // Tre strade: il relay sulla stessa macchina (1.35), Firebase da lontano (browser collegato), la demo in sviluppo.
  const remote = access ? null : loadRemote(kv)
  const rauth = remote ? new AnonAuth(remote.apiKey, authStore(kv)) : null
  const tr: Transport | null = access ? new LocalTransport(access.base, access.token)
    : remote && rauth?.uid() ? new FirebaseTransport(new Rtdb(remote.databaseUrl, (f) => rauth.token(f)),
        // Dati del sito cancellati: senza la chiave si torna all'accoppiamento.
        idbKeyStore().load().then(k => { if (!k) needsPair = true; return k ?? Promise.reject(new Error('no key')) }), rauth.uid()!)
    : null
  // Servita da un hosting e non collegata: la pagina di accoppiamento al posto della demo.
  const hosted = !['localhost', '127.0.0.1'].includes(location.hostname) && !new URLSearchParams(location.search).has('demo')
  let needsPair = $state(!tr && hosted)
```
con gli import `AnonAuth`, `Rtdb`, `FirebaseTransport`, `authStore`, `idbKeyStore`, `loadRemote`, `Kv`, `Transport`,
`TooLargeError`, `Pair`. Nel `subscribe` esistente, il secondo argomento diventa:
```ts
    }, () => { down = true; if (rauth && !rauth.uid()) needsPair = true })
```
(un refresh rifiutato cancella la sessione: la pagina torna all'accoppiamento con `t.pairSignedOut`). In `attach`, nel
`catch` del caricamento:
```ts
      } catch (e) {
        const reason = e instanceof TooLargeError ? t.tooLarge((e.size / 1e6).toFixed(1).replace('.', ','), (e.max / 1e6).toFixed(1).replace('.', ',')) : e instanceof Error ? e.message : String(e)
        uploads = { ...uploads, [m.id]: { type: 'failed', reason } }
        continue
      }
```
In cima al markup, prima di `{#if wide}`:
```svelte
{#if needsPair}<Pair {kv} signedOut={!!remote} />{:else}
```
e `{/if}` dopo il blocco della plancia e del telefono (la `CaptionBar` e il foglio della notte restano dentro).
Il canale in Impostazioni: `channel={access ? t.channelLocal : tr ? t.channelFirebase : t.channelDemo}`.

- [ ] **Passo 4: «Scollega questo browser» in `Settings.svelte`**

Nelle prop: `onForget = undefined` e nel tipo `onForget?: () => void`. Dopo l'ultima sezione `.sec`, prima dei dialoghi:
```svelte
  {#if onForget}
    <div class="sec">
      <button class="srow" onclick={onForget}>
        <span class="sic"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="var(--b-alert)" stroke-width="2" stroke-linecap="round"><path d="M12 2v10M18.4 6.6a9 9 0 1 1-12.8 0" /></svg></span>
        <span class="st"><b>{t.forgetBrowser}</b><small>{t.forgetBrowserSub}</small></span>
      </button>
    </div>
  {/if}
```
In `App.svelte`, sul componente `Settings`:
```svelte
onForget={remote ? async () => { await forget(kv, idbKeyStore()); location.replace('/') } : undefined}
```
Il dispositivo resta in `/allowed` finché il relay non ha `unpair` (1.39): con la 1.39, prima di `forget` mandare
`newCmd('unpair', null, rauth.uid())` e aspettarne l'esito.

- [ ] **Passo 5: build, test, tipi, prova in locale**

Eseguire: `cd web && npx vitest run && npm run check && npx vite build`
Atteso: tutto verde. Poi `npx vite preview --port 4179` e `http://localhost:4179/?demo` mostra la demo come prima;
`http://127.0.0.1:4179/` senza token non è «hosted» e mostra la demo.

- [ ] **Passo 6: deploy e prova dal vivo (Franz)**

```bash
cd web && npx vite build && firebase deploy --only hosting --project claude-master-relay-3761
```
Franz apre `https://claude-master-relay-3761.web.app` sul Mac, sul telefono avvia «Aggiungi un dispositivo», scrive il codice.
Atteso: la pagina dice «Collego: conferma sul PC», poi mostra le sessioni vere; `state.devices` ha «Safari su Mac»
(`kind` "web" con la 1.39).

- [ ] **Passo 7: commit**

```bash
git add web/src/lib/Pair.svelte web/src/App.svelte web/src/lib/Settings.svelte web/src/lib/t.ts
git commit -m "feat(web): the remote road in the app — pairing page on the hosted site, Firebase transport when paired, too-large attachments explained, forget this browser"
```

---

### Task 9: il link per un browser dal telefono

**File:**
- Modificare: `core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairLink.kt`
- Test: `core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairLinkTest.kt`
- Modificare: `mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AddDeviceSheet.kt`,
  `mobile/src/main/res/values/strings.xml`, `mobile/src/main/res/values-en/strings.xml`

**Interfacce:**
- Produce: `PairLink.webUrl(projectId: String, line: String): String` =
  `https://<projectId>.web.app/#pair=<base64url della riga, senza padding>`; stessa forma di `webLink` (Task 6).

- [ ] **Passo 1: test che fallisce**

In `PairLinkTest.kt`:
```kotlin
    // Web app remota: lo stesso invito nel frammento di un link https, per la fotocamera di iPhone e iPad.
    @Test fun webUrlCarriesTheSameLineAsTheAppLink() {
        val f = Json.parseToJsonElement(Fixtures.read("pair-link.json")).jsonObject
        val line = f.getValue("line").jsonPrimitive.content
        val q = f.getValue("uri").jsonPrimitive.content.substringAfter("q=")
        assertEquals("https://cmwatch-demo.web.app/#pair=$q", PairLink.webUrl("cmwatch-demo", line))
    }
```

- [ ] **Passo 2: implementazione**

In `PairLink`:
```kotlin
    /** Il link per un browser: la stessa riga nel frammento, che non arriva al server (web app, strada remota). */
    fun webUrl(projectId: String, line: String): String =
        "https://$projectId.web.app/#pair=" + Base64.getUrlEncoder().withoutPadding().encodeToString(line.toByteArray(Charsets.UTF_8))
```

- [ ] **Passo 3: il foglio Aggiungi dispositivo**

In `AddDeviceSheet`, nel ramo `is AddDeviceUi.Offer`, la riga `if (ui.qr != null) QrImage(…)` diventa:
```kotlin
                // Web app remota: lo stesso invito come link https, per la fotocamera di iPhone e iPad o da copiare.
                var browser by rememberSaveable { mutableStateOf(false) }
                val web = ui.qr?.let { q -> PairQr.parse(q)?.let { PairLink.webUrl(it.f.p, q) } }
                if (web != null) SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(R.string.add_device_tab_app, R.string.add_device_tab_browser).forEachIndexed { i, label ->
                        SegmentedButton(selected = browser == (i == 1), onClick = { browser = i == 1 }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                            Text(stringResource(label))
                        }
                    }
                }
                val shown = if (browser && web != null) web else ui.qr
                if (shown != null) QrImage(shown, dim = expired, modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f))
                if (browser && web != null) {
                    Text(stringResource(R.string.add_device_browser_how), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.fillMaxWidth())
                    val clip = LocalClipboardManager.current
                    OutlinedButton({ clip.setText(AnnotatedString(web)) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_device_copy_link)) }
                }
```
con gli import `rememberSaveable`, `mutableStateOf`, `getValue`/`setValue`, `SingleChoiceSegmentedButtonRow`,
`SegmentedButton`, `SegmentedButtonDefaults`, `LocalClipboardManager`, `AnnotatedString`, `PairQr`, `PairLink`, e
`@OptIn(ExperimentalMaterial3Api::class)` sulla funzione se la versione di Material 3 lo chiede.
Stringhe: `add_device_tab_app` «App», `add_device_tab_browser` «Browser», `add_device_copy_link` «Copia il link»,
`add_device_browser_how` «Inquadra con la fotocamera di iPhone o iPad, o apri il link sul computer.»; in inglese
«App», «Browser», «Copy the link», «Scan with the iPhone or iPad camera, or open the link on the computer.».

- [ ] **Passo 4: CI verde e commit**

```bash
git add core/src/main/kotlin/it/pixelbox/cmwatch/pairing/PairLink.kt core/src/test/kotlin/it/pixelbox/cmwatch/pairing/PairLinkTest.kt mobile/src/main/kotlin/it/pixelbox/cmwatch/mobile/ui/AddDeviceSheet.kt mobile/src/main/res/values/strings.xml mobile/src/main/res/values-en/strings.xml
git commit -m "feat(phone): add a browser from the phone — the invite as an https link with the line in the fragment, QR and copy"
git push origin feature/tablet
```
Atteso: CI «Build Android APKs» verde; APK installato solo su Pixel 11 Pro XL, Pixel Tablet e strongbad.

---

### Task 10: verifica dal vivo

**File:**
- Modificare: `docs/verifiche/web-strada-remota.md`

- [ ] **Passo 1: la checklist**

```markdown
# Web app, strada remota: verifica dal vivo

- [ ] Mac, Safari: collegato col codice a 6 cifre; il telefono mostra «Safari su Mac» fra i dispositivi.
- [ ] iPhone: inquadrato il QR «Browser» del telefono, conferma, collegato senza scrivere nulla.
- [ ] Stato dal vivo: una sessione cambia stato sul PC e la pagina lo mostra entro pochi secondi.
- [ ] Dopo un'ora con la pagina aperta lo stato arriva ancora (token rinnovato, stream riaperto).
- [ ] Portatile chiuso 10 minuti e riaperto: lo stato torna da solo.
- [ ] Prompt a una sessione: «consegnato alla sessione», etichetta «dalla web app».
- [ ] Allegato piccolo: arriva; allegato da 3 MB: rifiutato con le misure, prima dell'invio.
- [ ] File dalla chat: si apre (a pezzi, 1.34).
- [ ] Impostazioni, «Scollega questo browser»: torna la pagina di accoppiamento; con la 1.39 il dispositivo sparisce dal telefono.
- [ ] Un invito di primo accoppiamento incollato: rifiutato, gli altri dispositivi continuano a funzionare.
```

- [ ] **Passo 2: eseguirla con Franz, annotare gli esiti come sono, committare**

```bash
git add docs/verifiche/web-strada-remota.md
git commit -m "docs: live check of the web app's remote road"
```
