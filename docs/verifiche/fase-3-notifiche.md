# Fase 3 — notifiche native (specifica di Franz, 12/09 15:25 e 15:26)

Una riga per capacità, «vista sul polso» da aggiornare alla prova dal vivo.

| Capacità | Dove | Vista sul polso |
|---|---|---|
| Conversazione per sessione (`MessagingStyle`, `Person` = «🔴 nome» con badge, `setShortcutId` + shortcut dinamico long-lived, `CATEGORY_MESSAGE`) | `Notifier.question` | no (in attesa FCM) |
| Storico nel thread («❓ domanda → 1 yes») | `Notifier.remember` (ultime 3, in memoria) | no |
| Anteprima al polso: titolo «❓ nome», prima riga della domanda; espansa: testo intero + azioni | `NotificationPlan.question` | no |
| Azioni dirette «1 …» «2 …» (`showsUserInterface=false`, BroadcastReceiver) | `ReplyReceiver.ACTION_OPTION` | notifica vista il 13/09 senza i tasti: le azioni erano senza icona e Wear OS le scarta. Icone aggiunte, da riprovare |
| «Rispondi» con `RemoteInput.setChoices` (tutte le opzioni numerate) + testo libero/dettatura + `setAllowGeneratedReplies` | `Notifier.question` | no |
| `tier=high`: solo «Apri» (niente azioni dirette, niente chip, niente testo libero) | `NotificationPlan.question` (test) | n/a |
| Aggiornamento in place: «✓ 1 · yes inviato» → «✓ confermato» (chiusa dopo 5 s) / «✗ non consegnato · Riprova» | `Notifier.sent/confirmed/failed`, `CmApp` | no |
| `setWhen(asked_at)` + `setUsesChronometer` sulla domanda; `setShowWhen` sugli esiti | `Notifier.base` | no |
| Icone: piccola = glifo app, grande = badge della sessione, `setColor` cobalto, `setSubText` = account | `Notifier.base`, `BadgeBitmap` | no |
| Semantica: `SEMANTIC_ACTION_REPLY` su Rispondi, `MARK_AS_READ` su Apri | `Notifier.question` | no |
| Gruppo «cm» con summary `InboxStyle` (una riga per sessione), `GROUP_ALERT_CHILDREN`, `setLocalOnly` | `Notifier.summary` | no |
| Esito seguito: `BigTextStyle`, azioni Leggi (TTS in `SpeakService`, stop al secondo tocco) · Scrivi (`RemoteInput` → prompt) · Apri; `setTimeoutAfter` 12 h; `autoCancel` | `Notifier.outcome` | no |
| Sparita: «✗ nome» + Riprendi (`/cmd resume`) | `Notifier.gone` | no |
| Quota a soglia: `setProgress(100, pct)`, «⚠ 95 % personale» | `Notifier.quota` | no |
| Canali: domande HIGH 2×60 ms · esiti DEFAULT 40 ms · sparite DEFAULT 200 ms · quota LOW, tutti senza suono | `Notifier.ensureChannels` | no |
| Dismiss = «visto» (`setDeleteIntent`): tile e complication smettono di evidenziare la domanda | `ReplyReceiver.ACTION_SEEN`, `Prefs.seenQuestions` | no |
| Sorgente: FCM data message → `WorkManager` expedited → GET /state → diff → notifiche solo per il nuovo; app in primo piano: niente notifica | `WakeWorker`, `CmApp.react` | ✅ 13/09 01:41: risveglio FCM e notifica «❓ claude-master» a app chiusa (il diff ora gira sullo stream, non solo sul risveglio) |
| Segui: live update promosso con `ProgressStyle` indeterminata su API 36+, altrimenti `OngoingActivity` | `FollowOngoing.promoted` | da provare (il Pixel Watch 5 è API 37) |
| Impostazioni: notifiche spente → pulsante alle impostazioni di sistema | `SettingsScreen` | no |

Unit test: `NotificationPlanTest` (titolo, righe, azioni per tier, chip, cronometro, canale, summary), `BadgeTest` (forma × colore × contrasto).
