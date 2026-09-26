# claude-master-watch

![Status: beta](https://img.shields.io/badge/status-beta-orange) ![Wear OS 4+](https://img.shields.io/badge/Wear%20OS-4%2B-3ddc84) ![Needs claude-master](https://img.shields.io/badge/needs-claude--master-8A5CF6)

![claude-master on your wrist: on the left the three steps — the claude-master plugin on the PC, the relay paired with your Firebase, the 6-digit code on the watch; on the right three real screens of the app on the demo set: the sessions, a question, a session's card.](docs/readme/card-hero.png)

**The Claude Code sessions of your computer, on your Wear OS watch.** The one waiting
for an answer comes first: read its question, tap an option or dictate a reply, and the
session goes on. See what the others are doing, follow one, launch a project, check the
quota, all without going back to the desk.

> **Just want to try it?** Start from [«Try it» in the claude-master README](https://github.com/frsorrentino/claude-master#try-it):
> it has everything for a first run. This page is for building the phone and watch apps from source.

## It needs claude-master on your PC

This app is the wrist of [**claude-master**](https://github.com/frsorrentino/claude-master),
the Claude Code plugin that runs many sessions on one machine: one tab per project, one list
of what is running, prompts from one session to another, restore after a reboot. The app
shows what the plugin knows and sends back what you decide; without the plugin there is
nothing to show.

```bash
claude plugin marketplace add frsorrentino/claude-master
claude plugin install claude-master@claude-master-dev --scope user
claude-master init --yes --shim --shell
```

The full quickstart, and everything the plugin does from the terminal, is in
[its README](https://github.com/frsorrentino/claude-master#try-it).

**No Wear OS watch?** claude-master works without this app, from the terminal. watchOS is
not supported yet. Telegram, if you set it up in the plugin, only sends notifications: long
texts, and a fallback when the watch is unreachable.

## Screens

<table>
  <tr>
    <td align="center"><img src="docs/readme/sessions.png" width="220" alt="Sessions list: the session waiting for an answer first, with its question in full"><br>Sessions</td>
    <td align="center"><img src="docs/readme/question.png" width="220" alt="A question full screen, ▶ next to the session's name to read it aloud, the options as wide buttons"><br>Question</td>
    <td align="center"><img src="docs/readme/card.png" width="220" alt="A working session's card: what it is doing and what comes next; a tap opens the whole answer"><br>Working session</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/readme/card-idle.png" width="220" alt="A stopped session's card with its outcome in full, no extra tap"><br>Outcome</td>
    <td align="center"><img src="docs/readme/timeline.png" width="220" alt="The timeline: questions, answers and outcomes of every session, newest first"><br>Timeline</td>
    <td align="center"><img src="docs/readme/quota.png" width="220" alt="Quota in the style of the Wear OS morning brief: 5-hour window, week, resets"><br>Quota</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/readme/pairing.png" width="220" alt="Pairing: run claude-master relay pair on the PC, then enter the 6-digit code on the watch"><br>Pairing</td>
    <td align="center"><img src="docs/readme/stale.png" width="220" alt="A list that is no longer fresh says how old it is and never looks live"><br>Stale data</td>
    <td align="center"><img src="docs/readme/complication.png" width="220" alt="The quota ring complication on a watch face: 13 % of the 5-hour window, the window's tag 5h under the number"><br>Complication</td>
  </tr>
</table>

App screens are rendered from the app's code by the Paparazzi snapshot tests, on the demo set
of `contract/` (no real project, path or account). The complication is a screenshot from a
Pixel Watch 5. Texts grow with the system font size and are read by TalkBack:

<p align="center"><img src="docs/readme/question-large-font.png" width="220" alt="The same question with the largest system font: whole lines, nothing cut"></p>

## What it does

- **Sessions**: every session of every account, the one waiting for you first, each with the
  badge of its terminal tab. A closed session reopens in its conversation from its row.
- **Questions**: full screen, options as wide buttons; «Write» for a free-text answer,
  «Chat about this» to talk it over first. Questions of high importance get a red border.
- **Session card**: what it is doing and the next step, or its outcome in full; the whole
  answer in paragraphs above the terminal tail; «Write» sends it a new prompt.
- **Read aloud**: ▶ next to every long text reads it with the system voice, block by block,
  and keeps going with the screen off.
- **Follow**: a long press on a session buzzes you when it finishes.
- **Launch**: start a project from the list the PC publishes; the terminal tab opens on the
  desktop too.
- **Tile**: the session in focus with its latest step, and the quota bars when there is room.
- **Complication**: the quota ring with the percentage and its window, 5h or 7d; when the
  5-hour window is empty the ring shows the week.
- **Notifications**: one conversation per session, the first options as direct actions, reply
  with choices or dictation; a question answered elsewhere closes on the wrist.
- **Also**: the timeline of the day, the 20:00 recap (read aloud too), the night queue.

## How it connects

![Your PC, your Firebase, your wrist: the claude-master plugin and its relay on the PC push the state and run the commands; your Firebase Realtime Database and Cloud Messaging hold only AES-256-GCM encrypted blobs; the watch keeps the key in its Keystore, paired over X25519 with a 6-digit code.](docs/readme/card-bus.png)

There is no server of ours in between. The relay (`claude-master relay`, part of the plugin)
publishes the state of your sessions to **your own** Firebase project and runs the commands
the watch sends: answers, prompts, launches, reopenings. Every document is encrypted end to
end with AES-256-GCM; the key is agreed at pairing over X25519 and lives in the Keystore of
the phone and of the watch. Firebase sees blobs, never your prompts or your project names.

## Requirements

- **PC**: Linux or macOS, always on and awake (the relay covers «sessions closed», not
  «machine off»); `bash`, `tmux`, `python3` ≥ 3.8 with the `cryptography` module, `crontab`;
  Claude Code ≥ 2.1.263 with the [claude-master](https://github.com/frsorrentino/claude-master)
  plugin; the Firebase CLI (`npm install -g firebase-tools`, `firebase login`) for `relay setup`.
- **Firebase**: a project of your own. `claude-master relay setup` creates or picks it and sets
  up Realtime Database, the rules, anonymous sign-in, the Android app and the service account.
- **Phone**: Android 13 or newer (minSdk 33), paired with the watch as Wear OS requires. It
  scans the pairing QR and hands the key to the watch.
- **Watch**: Wear OS 4 or newer (minSdk 33) with Google Play services; tried only on a Pixel
  Watch 5 (Wear OS 7).
- **Build**: JDK 17, Android SDK 36, `adb` (USB for the phone, wireless debugging for the watch).

## Build from source

The repository has four Gradle modules: `wear` (the watch app), `mobile` (the phone app),
`core` (contract, crypto and rules shared by both) and `ui-tokens`. Phone and watch apps share
the package `it.pixelbox.cmwatch`, as one Play listing will.

1. **The plugin** on your PC: see above.
2. **Firebase, guided**: `claude-master relay setup` (`--dry-run` first to see the steps). It
   saves the Android app's `google-services.json` in the relay's folder; the apps do not need
   it, the QR carries the same data. Then `claude-master relay install` for the relay's
   crontab. Details: [Relay for the Wear OS app](https://github.com/frsorrentino/claude-master#relay-for-the-wear-os-app).
3. **Build and sign** both apps with the same key. Release builds read it from the environment:

   ```bash
   export KEYSTORE_PATH=/path/to/release.jks KEYSTORE_PASS=… KEY_ALIAS=… KEY_PASS=…
   ./gradlew :mobile:assembleRelease :wear:assembleRelease
   adb -s <phone> install -r mobile/build/outputs/apk/release/mobile-release.apk
   adb connect <watch-ip>:<port>        # Wear OS: Developer options → Wireless debugging
   adb -s <watch-ip>:<port> install -r wear/build/outputs/apk/release/wear-release.apk
   ```

   Keep one key for good: an app signed with another key cannot be updated in place, and
   uninstalling it wipes the pairing. Debug builds work too, with the same caveat.
4. **Pair**: on the PC `claude-master relay pair` shows a QR. On the phone tap «Pair» and scan
   it (or «Paste the code» with `relay pair --text`). The phone joins your Firebase project,
   agrees the AES key with the PC over X25519, then passes it to the watch, which keeps it in
   its Keystore and works on its own over Wi-Fi or LTE. The screen shows the three steps,
   phone, watch, PC. With no watch connected the phone pairs alone; pair again to add it.
   A watch build with `google-services.json` in `wear/` can still pair by itself with the
   6-digit code that `relay pair` prints under the QR.
5. **Try without a PC**: on the watch's pairing screen tap «Try the demo»: sessions, questions
   and quota from the demo set, no PC needed. Settings → «Demo mode» turns it off.

Tests: `./gradlew :core:testDebugUnitTest :mobile:testDebugUnitTest`; screens are Paparazzi
snapshots (`:wear:verifyPaparazziDebug`, `:mobile:verifyPaparazziDebug`), also run by the
GitHub Actions workflow.

## Status

Beta, in daily use on a Pixel Watch 5 (45 mm, Wear OS 7) and a Pixel phone; minSdk 33.
There is no build on the Play Store yet: you build and sign the apps yourself. A Play testing
track for phone and watch is being prepared. Built with Kotlin, Compose for Wear OS Material 3,
ProtoLayout for the tile, Room, Firebase.

Design: [`docs/plans/2026-09-12-app-polso-design.md`](docs/plans/2026-09-12-app-polso-design.md)
(Italian). Contract with the PC: [`contract/`](contract/): the JSON fixtures the app's tests
read and the relay produces, byte for byte.
