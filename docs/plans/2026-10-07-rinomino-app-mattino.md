# Rinomino dell'app in Team Supervisor: cosa resta per il mattino (07/10/2026)

Passo 5 del piano `personali/claude-master/docs/plans/2026-10-07-rinomino-team-supervisor.md`, chiesto dalla master
alle 01:08 con la delega di Franz delle 00:33.

## Fatto stanotte

- Repo GitHub `frsorrentino/claude-master-watch` → `team-supervisor-app` e archivio → `team-supervisor-app-archivio`
  (01:25). Il vecchio indirizzo rimanda al nuovo; i remoti locali sono aggiornati e valgono per tutti i worktree.
- Nome mostrato «Team Supervisor»: icona e titolo dell'app, tile e complicazione dell'orologio, marchio del tablet,
  titolo e manifest della web app. I testi citano il comando `team-supervisor relay pair`. Privacy e README col nome
  e l'indirizzo nuovi (commit `2d33f5c`).
- Invariati apposta: id del pacchetto `com.francescosorrentino.cmaster` (un id nuovo vorrebbe dire reinstallare e
  riaccoppiare ogni dispositivo), progetto Firebase `claude-master-relay-3761`, etichette crypto
  `claude-master-relay-v1` (AAD e HKDF, identiche nel relay: cambiarle rompe la cifratura), link `cmwatch://pair`.

## Resta per il mattino, con Franz

1. Cartelle: `personali/claude-master-watch` (repo principale) e `personali/claude-master-phone` (worktree) →
   `personali/team-supervisor-app`, più il worktree `personali/claude-master-phone-notte`. Solo dopo il turno di notte
   (`~/.claude/night.log` col giro finito). Nello stesso momento:
   - `relay.web.dir` in `~/.config/claude-master/config.json` punta a `.../claude-master-phone/web/dist`: senza
     aggiornarlo il relay non trova più la web app;
   - la memoria di sessione sta in `~/.claude/projects/-home-franz-Desktop-workspaces-personali-claude-master-watch/`,
     legata al percorso: va spostata con la cartella.
2. Nomi interni: `rootProject.name` in `settings.gradle.kts` e `name` in `web/package.json` (con il lock).
3. CLAUDE.md del progetto: cita la sessione `claude-master` e le cartelle; si aggiorna con la migrazione delle sessioni
   (passo 8 del piano).
4. README: i riferimenti al plugin (`frsorrentino/claude-master`, `claude-master init`, `claude-master relay …`)
   dopo la release 0.7.0 del plugin.
5. Installazione dell'APK col nome nuovo su telefono, tablet e orologio.
6. Da decidere: film promo (`tools/promo/`) e scheda del Play Store, oggi col nome vecchio.
