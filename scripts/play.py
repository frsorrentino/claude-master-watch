#!/usr/bin/env python3
"""Google Play Developer API for Claude Master App: uploads, test tracks, testers, rollout. No Play Console, no captcha.

The service account key lives OUTSIDE the repository, mode 0600 (default ~/.config/claude-master-watch/play-service-account.json,
or --key / PLAY_KEY). Only the standard library and `cryptography` (already required by the claude-master relay).

  play.py status                                    tracks and their releases
  play.py upload --track internal mobile.aab wear.aab [--notes "…"] [--status completed|draft]
  play.py promote --from internal --to alpha        same version codes on another track
  play.py testers --track alpha [--group g@googlegroups.com …]   read, or set, the Google Groups of a track
  play.py rollout --track production --fraction 0.2 (or --complete)

--dry-run prints each request and sends nothing (no key needed). Limits of the API (verified 26/09/2026):
- the app is created in the Console, and its FIRST release too: on a draft app the API accepts only releases in «draft»;
- testers: the API sets Google Groups only; the email lists of the internal track stay in the Console.
"""
import argparse, base64, json, os, stat, sys, time, urllib.error, urllib.parse, urllib.request

PACKAGE = "it.pixelbox.cmwatch"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
DEFAULT_KEY = os.path.expanduser("~/.config/claude-master-watch/play-service-account.json")


class PlayError(Exception):
    pass


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def load_key(path: str) -> dict:
    """The key must be a file of its owner only: 0600 or stricter, never inside a git checkout."""
    if not os.path.isfile(path):
        raise PlayError(f"no service account key at {path}: create it in Google Cloud, then chmod 600")
    mode = stat.S_IMODE(os.stat(path).st_mode)
    if mode & 0o077:
        raise PlayError(f"{path} is readable by others (mode {mode:o}): chmod 600 {path}")
    d = os.path.dirname(os.path.abspath(path))
    while d != os.path.dirname(d):
        if os.path.exists(os.path.join(d, ".git")):
            raise PlayError(f"{path} is inside a git checkout ({d}): move it out of the repository")
        d = os.path.dirname(d)
    with open(path) as f:
        key = json.load(f)
    for field in ("client_email", "private_key", "token_uri"):
        if field not in key:
            raise PlayError(f"{path} is not a service account key: «{field}» missing")
    return key


def signed_jwt(key: dict, now: int) -> str:
    from cryptography.hazmat.primitives import hashes, serialization
    from cryptography.hazmat.primitives.asymmetric import padding
    header = {"alg": "RS256", "typ": "JWT"}
    claims = {"iss": key["client_email"], "scope": SCOPE, "aud": key["token_uri"], "iat": now, "exp": now + 3600}
    signing_input = f"{b64url(json.dumps(header).encode())}.{b64url(json.dumps(claims).encode())}"
    private = serialization.load_pem_private_key(key["private_key"].encode(), password=None)
    signature = private.sign(signing_input.encode(), padding.PKCS1v15(), hashes.SHA256())
    return f"{signing_input}.{b64url(signature)}"


class Client:
    def __init__(self, key_path: str, dry_run: bool):
        self.dry_run = dry_run
        self.token = None
        if not dry_run:
            key = load_key(key_path)
            body = urllib.parse.urlencode({
                "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
                "assertion": signed_jwt(key, int(time.time())),
            }).encode()
            self.token = self._send(urllib.request.Request(key["token_uri"], data=body, method="POST"))["access_token"]

    def _send(self, req: urllib.request.Request) -> dict:
        try:
            with urllib.request.urlopen(req, timeout=300) as r:
                raw = r.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")
            try:
                detail = json.loads(detail)["error"]["message"]
            except (ValueError, KeyError, TypeError):
                pass
            raise PlayError(f"{req.get_method()} {req.full_url}: HTTP {e.code}: {detail}") from None

    def call(self, method: str, url: str, body=None, data: bytes = None, content_type="application/json") -> dict:
        if self.dry_run:
            size = f" <{len(data)} bytes>" if data is not None else ""
            print(f"[dry-run] {method} {url}{size}" + (f" {json.dumps(body)}" if body is not None else ""))
            return {"id": "DRY-EDIT", "versionCode": 0, "releases": [], "googleGroups": []}
        payload = data if data is not None else (json.dumps(body).encode() if body is not None else None)
        req = urllib.request.Request(url, data=payload, method=method)
        req.add_header("Authorization", f"Bearer {self.token}")
        if payload is not None:
            req.add_header("Content-Type", content_type)
        return self._send(req)


class Edit:
    """One edit: everything inside it becomes visible together at commit, or not at all."""
    def __init__(self, client: Client):
        self.c = client
        self.id = client.call("POST", f"{API}/edits", body={})["id"]

    def url(self, tail: str) -> str:
        return f"{API}/edits/{self.id}{tail}"

    def track(self, name: str) -> dict:
        return self.c.call("GET", self.url(f"/tracks/{name}"))

    def set_track(self, name: str, releases: list) -> dict:
        return self.c.call("PUT", self.url(f"/tracks/{name}"), body={"track": name, "releases": releases})

    def commit(self):
        self.c.call("POST", self.url(":commit"))

    def abandon(self):
        self.c.call("DELETE", self.url(""))


def release(codes, status: str, name: str = None, notes: str = None, fraction: float = None) -> dict:
    r = {"versionCodes": [str(c) for c in codes], "status": status}
    if name:
        r["name"] = name
    if notes:
        r["releaseNotes"] = [{"language": "en-US", "text": notes}]
    if fraction is not None:
        r["userFraction"] = fraction
    return r


def cmd_status(c: Client, a):
    e = Edit(c)
    try:
        tracks = c.call("GET", e.url("/tracks")).get("tracks", [])
        for t in tracks:
            for r in t.get("releases", []) or [{}]:
                frac = f" {r['userFraction']:.0%}" if "userFraction" in r else ""
                print(f"{t['track']:<12} {r.get('status', '-'):<11}{frac} {','.join(r.get('versionCodes', []))} {r.get('name', '')}")
    finally:
        e.abandon()


def cmd_upload(c: Client, a):
    e = Edit(c)
    try:
        codes = []
        for path in a.aab:
            with open(path, "rb") as f:
                data = f.read()
            got = c.call("POST", f"{UPLOAD}/edits/{e.id}/bundles?uploadType=media", data=data, content_type="application/octet-stream")
            print(f"uploaded {os.path.basename(path)}: versionCode {got.get('versionCode')}")
            codes.append(got.get("versionCode"))
        e.set_track(a.track, [release(codes, a.status, a.name, a.notes)])
        e.commit()
        print(f"{a.track}: release {a.status} with {codes}")
    except BaseException:
        e.abandon()
        raise


def cmd_promote(c: Client, a):
    e = Edit(c)
    try:
        src = [r for r in e.track(a.src).get("releases", []) if r.get("status") in ("completed", "inProgress")]
        if not src:
            raise PlayError(f"{a.src}: no completed or rolling release to promote")
        r = src[0]
        e.set_track(a.dst, [release(r["versionCodes"], a.status, r.get("name"), None)])
        e.commit()
        print(f"{a.src} → {a.dst}: {r['versionCodes']} ({a.status})")
    except BaseException:
        e.abandon()
        raise


def cmd_testers(c: Client, a):
    e = Edit(c)
    try:
        if a.group:
            c.call("PUT", e.url(f"/testers/{a.track}"), body={"googleGroups": a.group})
            e.commit()
            print(f"{a.track}: Google Groups {a.group}")
        else:
            print(json.dumps(c.call("GET", e.url(f"/testers/{a.track}")), indent=2))
            e.abandon()
    except BaseException:
        e.abandon()
        raise


def cmd_rollout(c: Client, a):
    e = Edit(c)
    try:
        live = [r for r in e.track(a.track).get("releases", []) if r.get("status") in ("inProgress", "halted", "draft")]
        if not live:
            raise PlayError(f"{a.track}: no release in progress, halted or draft")
        r = live[0]
        new = release(r["versionCodes"], "completed" if a.complete else "inProgress", r.get("name"), None,
                      None if a.complete else a.fraction)
        e.set_track(a.track, [new])
        e.commit()
        print(f"{a.track}: {new['status']} {new.get('userFraction', 1.0):.0%} of users")
    except BaseException:
        e.abandon()
        raise


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--key", default=os.environ.get("PLAY_KEY", DEFAULT_KEY))
    p.add_argument("--dry-run", action="store_true")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status")
    u = sub.add_parser("upload")
    u.add_argument("aab", nargs="+")
    u.add_argument("--track", required=True)
    u.add_argument("--status", default="completed", choices=["completed", "draft"])
    u.add_argument("--name")
    u.add_argument("--notes")
    pr = sub.add_parser("promote")
    pr.add_argument("--from", dest="src", required=True)
    pr.add_argument("--to", dest="dst", required=True)
    pr.add_argument("--status", default="completed", choices=["completed", "draft"])
    t = sub.add_parser("testers")
    t.add_argument("--track", required=True)
    t.add_argument("--group", action="append")
    r = sub.add_parser("rollout")
    r.add_argument("--track", required=True)
    g = r.add_mutually_exclusive_group(required=True)
    g.add_argument("--fraction", type=float)
    g.add_argument("--complete", action="store_true")
    a = p.parse_args(argv)
    if a.cmd == "rollout" and a.fraction is not None and not 0 < a.fraction < 1:
        p.error("--fraction is between 0 and 1, for example 0.2")
    try:
        client = Client(a.key, a.dry_run)
        {"status": cmd_status, "upload": cmd_upload, "promote": cmd_promote, "testers": cmd_testers, "rollout": cmd_rollout}[a.cmd](client, a)
    except PlayError as e:
        print(f"play: {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
