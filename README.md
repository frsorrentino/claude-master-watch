# claude-master-watch

Wear OS companion for [claude-master](https://github.com/frsorrentino/claude-master): the
Claude Code sessions of your machine on your wrist — the one waiting for an answer first.

- Complication and tile: how many sessions, which one is stuck, the question on one line.
- Notifications with the first two options and dictated replies.
- App: sessions of both accounts, the card with outcome and «→ next», the question full
  screen with wide buttons, the outcome read aloud on demand, the terminal tail, the timeline,
  launch, follow, quota rings, the day's recap, the night queue.
- Transport: Firebase RTDB + FCM behind a `Transport` interface, every document an
  end-to-end encrypted blob; the PC side is `cm-relay.py` in the claude-master plugin.

Design: `docs/plans/2026-09-12-app-polso-design.md`. Contract with the PC: `contract/`.
Target: Pixel Watch 5 (45 mm), Wear OS 7, minSdk 33.
