#!/usr/bin/env python3
"""Offline tests of play.py: key guard, JWT signature, request plan in --dry-run. Run: python3 scripts/test_play.py"""
import base64, contextlib, io, json, os, sys, tempfile, unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import play  # noqa: E402

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa


def fake_key():
    k = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pem = k.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()).decode()
    return k, {"client_email": "ci@example.iam.gserviceaccount.com", "private_key": pem, "token_uri": "https://oauth2.googleapis.com/token"}


def unb64(s):
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


class KeyGuard(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.mkdtemp()
        self.path = os.path.join(self.dir, "key.json")
        with open(self.path, "w") as f:
            json.dump(fake_key()[1], f)

    def test_refuses_key_readable_by_others(self):
        os.chmod(self.path, 0o644)
        with self.assertRaisesRegex(play.PlayError, "chmod 600"):
            play.load_key(self.path)

    def test_refuses_key_inside_git_checkout(self):
        os.mkdir(os.path.join(self.dir, ".git"))
        os.chmod(self.path, 0o600)
        with self.assertRaisesRegex(play.PlayError, "git checkout"):
            play.load_key(self.path)

    def test_accepts_private_key_outside_git(self):
        os.chmod(self.path, 0o600)
        self.assertEqual("ci@example.iam.gserviceaccount.com", play.load_key(self.path)["client_email"])

    def test_missing_key_says_what_to_do(self):
        with self.assertRaisesRegex(play.PlayError, "chmod 600"):
            play.load_key(os.path.join(self.dir, "none.json"))


class Jwt(unittest.TestCase):
    def test_signed_with_the_key_and_scoped_to_androidpublisher(self):
        private, key = fake_key()
        head, claims, sig = play.signed_jwt(key, 1_790_000_000).split(".")
        private.public_key().verify(unb64(sig), f"{head}.{claims}".encode(), padding.PKCS1v15(), hashes.SHA256())
        c = json.loads(unb64(claims))
        self.assertEqual(play.SCOPE, c["scope"])
        self.assertEqual(key["token_uri"], c["aud"])
        self.assertEqual(3600, c["exp"] - c["iat"])


class DryRun(unittest.TestCase):
    def run_cli(self, *args):
        out = io.StringIO()
        with contextlib.redirect_stdout(out):
            code = play.main(["--dry-run", *args])
        return code, out.getvalue()

    def test_upload_puts_both_bundles_in_one_release_and_commits(self):
        d = tempfile.mkdtemp()
        aabs = []
        for n in ("mobile-release.aab", "wear-release.aab"):
            p = os.path.join(d, n)
            with open(p, "wb") as f:
                f.write(b"x" * 10)
            aabs.append(p)
        code, out = self.run_cli("upload", "--track", "internal", *aabs, "--notes", "first")
        self.assertEqual(0, code)
        lines = out.splitlines()
        self.assertTrue(lines[0].startswith(f"[dry-run] POST {play.API}/edits"))
        self.assertEqual(2, sum("bundles?uploadType=media <10 bytes>" in l for l in lines))
        put = next(l for l in lines if " PUT " in l)
        self.assertIn("/tracks/internal", put)
        self.assertIn('"status": "completed"', put)
        self.assertIn('"releaseNotes": [{"language": "en-US", "text": "first"}]', put)
        self.assertTrue(lines[-2].endswith(":commit"))

    def test_testers_sets_google_groups(self):
        code, out = self.run_cli("testers", "--track", "alpha", "--group", "claude-master-testers@googlegroups.com")
        self.assertEqual(0, code)
        self.assertIn('/testers/alpha {"googleGroups": ["claude-master-testers@googlegroups.com"]}', out)

    def test_rollout_fraction_must_be_below_one(self):
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            play.main(["--dry-run", "rollout", "--track", "production", "--fraction", "20"])


if __name__ == "__main__":
    unittest.main()
