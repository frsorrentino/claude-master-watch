# Privacy — Claude Master App and Claude Master Watch

_Last updated: 26 September 2026._

Claude Master App (phone) and Claude Master Watch (Wear OS) are an unofficial companion for Claude Code, not
affiliated with Anthropic. They show and steer the Claude Code sessions running on your own computer through the
claude-master plugin. They have no account of their own, no analytics, no advertising and no server run by the author.

## Where your data goes

- **Your own Firebase project.** When you pair, the phone reads a QR code shown by your computer. It holds the
  configuration of a Firebase project that you created and own. The app talks only to that project: its Realtime
  Database and Firebase Cloud Messaging. The author never receives anything.
- **End-to-end encrypted.** Session states, questions, answers, commands, terminal text, diaries and anything you
  share with a session travel as AES-256-GCM ciphertext. The key is agreed with X25519 between your computer and your
  phone at pairing, then passed to your watch encrypted for it alone. Firebase stores only ciphertext; neither Google
  nor the author can read it.
- **Identifiers.** The Firebase SDK signs the app in anonymously and gives it a user id, an installation id and a
  messaging token. They let your computer allow your devices and wake them up. The device names (for example «Pixel
  Watch 5») are stored by your computer to list the paired devices.

## What stays on the device

- The encryption key, in the Android Keystore.
- The Firebase configuration, the name of your computer, the paired devices, and a local copy of recent states and
  diaries.
- Settings such as who rings on a question and text-to-speech.

Reading aloud uses the text-to-speech engine installed on your device. The QR code is read by the Google Play services
code scanner: the app has no camera permission and never sees the camera image.

## Demo mode

Demo mode runs on sample data inside the app. Nothing leaves the device.

## Deleting your data

- «Pair again» or uninstalling removes the key and the local data from the device.
- Removing a device on your computer (a new pairing) revokes its access to your database.
- Everything else lives in your own Firebase project, which you can delete from the Firebase console at any time.

## Changes and contact

Changes to this page are versioned in the
[GitHub repository](https://github.com/frsorrentino/claude-master-watch). Questions: an issue at
<https://github.com/frsorrentino/claude-master-watch/issues>. The plugin on the computer has its own page:
[claude-master privacy](https://github.com/frsorrentino/claude-master/blob/main/PRIVACY.md).
