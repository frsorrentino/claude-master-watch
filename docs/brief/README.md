# Stile «brief mattutino» di Wear OS

Misure e colori letti dai fotogrammi della schermata Oggi di Fitbit sul Pixel Watch 5
(`riferimento-*.png`, catturati via adb il 13/09/2026). Densità 320 dpi, schermo 480 px = 240 dp.
Franz vuole questo stile identico nella schermata Quota (13/09 16:16).

## Geometria (dp)

| pezzo | valore |
|---|---|
| margini laterali della lista | 12,5 (5,2 % della larghezza, come il design) |
| card | 215 di larghezza; 98 con tre righe, 119 con quattro |
| raggio degli angoli | 26 |
| padding interno | 15 a sinistra, 13 sopra e sotto, 8 a destra |
| spazio fra card | 6 |
| numero grande | maiuscole alte 18 → 26 sp, a 38,5 dal bordo alto |
| etichetta | 14 sp, a 15,5 dal bordo alto |
| pillolina | alta 20,5, angoli pieni, padding 12 × 3 |
| anello | 48 di diametro, tratto 8, a 8,5 dal bordo destro, centrato in verticale |

## Colori

| ruolo | valore | dove |
|---|---|---|
| fondo card | `#292F3A` | tutte le card |
| etichetta | `#BCE4C7` | nome del dato |
| numero | `#F4F4F4` | valore grande |
| unità e riga secondaria | `#BCC0CB` | «cal», «140 di 170» |
| pillolina neutra | `#7DCCFB` su inchiostro `#001C33` | «3400 restanti» |
| pillolina buona | `#65C581` su inchiostro `#072510` | «1 sopra l'obiettivo» |
| anello | `#8BB4F7` su traccia `#455165` | progresso del dato |

## Perché così

L'anello grande (72 dp) strappava la colonna dei testi e tagliava etichetta e riga secondaria: con i 48 dp
del brief la colonna resta larga una quindicina di caratteri e i testi stanno su una riga. I token `numeral*`
di Wear M3 sono molto più grandi dei 26 sp misurati e sfondavano la card.
