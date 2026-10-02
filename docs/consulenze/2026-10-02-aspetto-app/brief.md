# Consulenza sull'aspetto di claude-master-watch (telefono e orologio)

Ti chiediamo un parere da designer di prodotto mobile. Rispondi in italiano.

## Che cos'è

claude-master è un plugin di Claude Code che fa girare più sessioni di Claude Code sullo stesso PC, una per progetto, ognuna nella sua finestra di terminale. Una sessione «master» le lancia, le sorveglia e le chiude. Un relay sul PC pubblica lo stato delle sessioni, cifrato, su Firebase Realtime Database; le app lo leggono e mandano comandi indietro.

L'app è in due parti, native:
- **Telefono Android** (Kotlin, Jetpack Compose, Material 3 Expressive, tema scuro): la casa della master con l'ultimo esito e «Per te», la lista delle sessioni, la chat di ogni sessione (conversazione vera, passaggi degli strumenti, file, risposte alle domande, comandi slash), il quadro della quota, il registro della giornata e della notte, «Lancia» per aprire sessioni nuove, la condivisione verso una sessione, il terminale dal vivo, le impostazioni.
- **Orologio Wear OS** (Pixel Watch, Compose for Wear OS): sessioni, scheda di una sessione, risposta alle domande con un tocco, quota, terminale, tile e complicazioni.

## Chi la usa e come

Un solo utente, sviluppatore, che tiene 5-6 sessioni aperte su due account (personale e lavoro) e si allontana dal PC. Dal telefono e dal polso vuole sapere chi lo aspetta, rispondere, dare il passo successivo, sentire gli esiti a voce, controllare quota e contesto. Le domande delle sessioni bloccano il lavoro finché non risponde.

## Regole di forma già decise (vincoli)

- Tema scuro, Material 3 Expressive; liste che si deformano allo scroll come nelle app Google (sull'orologio).
- Una riga logica occupa la riga fisica: mai colonne di frammenti, mai «…» nei testi (si taglia senza puntini).
- Un solo bottone pieno per schermata.
- Stati come icone: mano = ti aspetta, bandierina = ha finito, fulmine = al lavoro, cerchio con pausa = ferma; forma del badge = account (cerchio personale, quadrato lavoro).
- Testi in italiano; tasto ▶ per la lettura vocale accanto ai testi lunghi, alle risposte e alle domande.
- Niente microfono nell'app.
- Lo stack (Kotlin, Compose) non cambia.

## Schermate allegate (dati della modalità Demo)

Telefono:
- t01 casa della master: ultimo esito, «Per te» (chi ti aspetta, chi ha finito, chi è al lavoro), quota
- t02 «Per te» con due righe aperte: domanda con le opzioni, turno finito con i consigli
- t03 lista delle sessioni
- t04 chat con la conversazione vera (passaggi degli strumenti raggruppati, esito)
- t05 chat con i messaggi mandati dal telefono e i loro stati
- t06 chat con una domanda in attesa
- t07 avviso in cima alla chat: un'altra sessione ti aspetta
- t08 risposta con tabelle (stretta come griglia, larga come schede)
- t09 sessione al lavoro con il tasto Stop
- t10 quadro: quota e lavoro
- t11 registro: notte e giorni
- t12 «Lancia»: apri una sessione su un progetto
- t13 fila «Ti aspettano»: domande da rispondere in sequenza
- t14 terminale dal vivo
- t15 impostazioni
- t16 condividi un'immagine verso una sessione
- t17 ricerca nelle conversazioni

Orologio:
- o01 sessioni, o02 scheda di una sessione, o03 domanda, o04 quota, o05 terminale

## Cosa ti chiediamo

1. **Per ogni schermata**, da 1 a 3 ottimizzazioni concrete: gerarchia, spaziature, densità, leggibilità, tocchi, accessibilità. Cita il nome del file (t01, o03…).
2. **Incoerenze** fra le schermate: stili di tasti, intestazioni, icone, colori, ritmi diversi per la stessa cosa.
3. **Da 5 a 8 funzionalità nuove** utili per questo uso (sviluppatore lontano dal PC con più sessioni), ognuna con il perché e dove la metteresti.
4. **Le 5 modifiche a più alto impatto e basso costo**, in ordine.

Sii specifico e sintetico: niente consigli generici («migliora il contrasto») senza dire dove e come. Rispetta i vincoli sopra; se pensi che uno vada rivisto, dillo in una riga separata.
