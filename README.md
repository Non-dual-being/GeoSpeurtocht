# GeoSpeurtocht

Een Paper/Spigot-plugin voor Minecraft-speurtochten op het GeoFort.

Met deze plugin kunnen begeleiders speurtochten starten vanuit ingestelde startpunten. Spelers worden naar het startpunt gebracht, vrijgegeven voor de speurtocht en na afloop teruggezet en bevroren. De plugin ondersteunt zowel normale speurtochten met een aflopende timer als time-trials met een oplopende timer en score-opslag.

De plugin is ontworpen voor gebruik met meerdere Minecraft-werelden via Multiverse-Core. Meerdere fysieke werelden kunnen samen één logische speurtocht vormen, bijvoorbeeld alle ClimateCrafter-werelden samen onder één sessie-key.

---

## Functies

* Startpunt instellen per logische speurtocht
* Meerdere actieve speurtochten naast elkaar
* Meerdere fysieke werelden groeperen tot één logische speurtocht
* Countdown-timer voor normale workshops
* Countup-timer voor time-trials
* Optionele maximale tijd bij countup
* Teamnaam koppelen aan time-trial
* Bossbar met actuele timer
* Timer pauzeren en hervatten
* Pauzetijd telt niet mee in de eindscore
* Time-trial finishen en score opslaan
* Scores tonen per logische speurtocht
* Scores resetten met confirm
* Scoreboard/sidebar tonen met actuele status
* Top 3 scores tonen op het scoreboard
* Huidige plaats tonen tijdens een time-trial
* Tijd toevoegen aan een lopende timer
* Spelers toevoegen aan een actieve speurtocht
* Spelers verwijderen uit een actieve speurtocht
* Verwijderde spelers terugzetten naar start en bevriezen
* Verwijderde spelers vrijgeven op hun huidige plek
* Spelers freezen en unfreezen
* Reset naar de Multiverse-gamemode van de startwereld
* Broadcast-commando voor berichten
* Teleporteer alle spelers naar begeleider

---

## Benodigdheden

### Verplicht

* Java 21
* Paper of Spigot 1.21.x
* Multiverse-Core 5.5.2 of compatibele versie

### Build tools

* Maven
* IntelliJ IDEA of een andere Java IDE

### Let op

De plugin draait prima op Paper. De huidige build gebruikt de Spigot API als compile-dependency, zodat de build stabiel blijft.

De scoreboard/sidebar gebruikt de standaard Bukkit/Spigot-scoreboardfunctionaliteit. Daardoor kunnen de rode score-nummers rechts naast de sidebar zichtbaar blijven. Dat is cosmetisch. De functionaliteit werkt gewoon.

---

## Projectstructuur

De plugin verwacht ongeveer deze structuur:

```text
GeoSpeurtocht/
├─ pom.xml
├─ lib/
│  └─ multiverse-core-5.5.2.jar
├─ src/
│  └─ main/
│     ├─ java/
│     │  └─ io/
│     │     └─ github/
│     │        └─ BrianVanB/
│     │           ├─ FreezeModule/
│     │           ├─ GeoSpeurtocht/
│     │           ├─ SpeurtochtModule/
│     │           └─ Utilities/
│     └─ resources/
│        ├─ config.yml
│        └─ plugin.yml
```

Belangrijk: `plugin.yml` moet in deze map staan:

```text
src/main/resources/plugin.yml
```

Als `plugin.yml` niet in de jar zit, kan Paper/Spigot de plugin niet laden.

---

## Maven build

De plugin kan gebouwd worden met Maven:

```bash
mvn clean package
```

De jar komt daarna in:

```text
target/
```

Bijvoorbeeld:

```text
target/GeoSpeurtocht-1.21.1.jar
```

---

## Installatie op de server

1. Stop de Minecraft-server.
2. Bouw de jar met Maven.
3. Kopieer de jar uit `target/` naar de servermap:

```text
plugins/
```

4. Zorg dat Multiverse-Core ook in de `plugins/` map staat.
5. Start de server opnieuw.
6. Controleer de console op een melding zoals:

```text
[GeoSpeurtocht] Finished loading
```

Controleer ook of de sessiegroepen geladen zijn:

```text
[GeoSpeurtocht] Loaded 10 grouped speurtocht worlds.
```

---

## Configuratie

De plugin gebruikt logische speurtocht-sessies. Een logische sessie kan bestaan uit één wereld of uit meerdere fysieke werelden.

Voorbeeldconfiguratie:

```yaml
speurtocht-session-groups:
  climatecrafter:
    display-name: "ClimateCrafter"
    type: "MULTI_WORLD"
    worlds:
      - climatecrafter_diamant
      - climatecrafter_goud
      - climatecrafter_ijzer
      - climatecrafter_koper
      - climatecrafter_steenkool

  klimaatspeurtocht:
    display-name: "Klimaatspeurtocht"
    type: "MULTI_WORLD"
    worlds:
      - klimaatspeurtocht_oceanen
      - klimaatspeurtocht_natuur
      - klimaatspeurtocht_afval
      - klimaatspeurtocht_lucht
      - klimaatspeurtocht_klimaat
```

### Belangrijk

`type: MULTI_WORLD` zegt alleen dat meerdere fysieke werelden samen één logische speurtocht vormen.

De timer-richting staat bewust niet in de config. De keuze tussen countdown en countup wordt gemaakt via command flags.

---

## Startpunten

Startpunten worden opgeslagen onder `Startpunten:` per logische sessie-key.

Voorbeeld:

```yaml
Startpunten:
  climatecrafter:
    X: 121722.5
    Y: 14.0
    Z: -487815.5
    Yaw: -134.55005
    Pitch: 0.59999865
    World: climatecrafter_diamant

  klimaatspeurtocht:
    X: 120001.1604012322
    Y: 11.0
    Z: -483437.90868288587
    Yaw: 174.60127
    Pitch: -13.499966
    World: klimaatspeurtocht_klimaat

  GeoFort_Heat:
    X: 136886.3947121695
    Y: 16.0
    Z: -430775.16739866143
    Yaw: -118.35014
    Pitch: 10.349974
    World: GeoFort_Heat
```

De key is de logische sessie:

```text
climatecrafter
klimaatspeurtocht
GeoFort_Heat
```

De `World:` blijft de fysieke Minecraft-wereld waar het startpunt echt staat.

---

## Werking

### Startpunt instellen

Een begeleider zet een startpunt met:

```text
/setstart
```

De huidige locatie van de begeleider wordt opgeslagen als startpunt voor de logische speurtocht waarin de begeleider staat.

Voorbeeld:

Als de begeleider in `climatecrafter_diamant` staat, wordt het startpunt opgeslagen onder de sessie-key:

```text
climatecrafter
```

Spelers kunnen naar het startpunt teleporteren met:

```text
/startpunt
```

---

## Normale speurtocht met countdown

Met dit commando wordt een normale speurtocht gestart:

```text
/startall <tijd in minuten>
```

Voorbeeld:

```text
/startall 30
```

Dit gebruikt standaard een aflopende timer.

Expliciet countdown gebruiken kan ook:

```text
/startall 30 --countdown
```

Bij het starten gebeurt dit:

* De plugin bepaalt de logische sessie via de wereld van de begeleider.
* Alle spelers binnen dezelfde logische sessie worden meegenomen.
* Begeleiders worden niet als gewone deelnemers behandeld.
* Spelers worden naar het startpunt geteleporteerd.
* Spelers worden vrijgegeven/unfreezed.
* De bossbar-timer wordt gestart.
* De deelnemers worden bijgehouden als actieve spelers.

---

## Time-trial met countup

Voor een time-trial gebruik je een oplopende timer:

```text
/startall --countup --team "Team Blauw"
```

Dit start een time-trial zonder maximale tijd.

Een countup met maximale tijd kan ook:

```text
/startall 45 --countup --team "Team Blauw"
```

Dit betekent:

* De timer loopt op vanaf `0:00`.
* Het team heet `Team Blauw`.
* De maximale tijd is 45 minuten.
* Bij finish wordt de verstreken tijd opgeslagen als score.

### Teamnaam

Bij `--countup` is een teamnaam verplicht:

```text
/startall --countup --team "Team Rood"
```

Zonder teamnaam wordt de time-trial niet gestart.

---

## Timer pauzeren en hervatten

### Pauzeren

```text
/pausetimer
```

Pauzeert de actieve timer van de logische speurtocht waarin de begeleider staat.

### Hervatten

```text
/resumetimer
```

Hervat de gepauzeerde timer.

Bij time-trials telt de pauzetijd niet mee in de eindscore.

---

## Timer aanpassen

Met dit commando kan een begeleider extra tijd toevoegen aan de lopende timer:

```text
/addtime <seconden>
```

Voorbeeld:

```text
/addtime 60
```

Bij een countdown wordt de resterende tijd verlengd.

Bij een countup is dit alleen relevant als er een maximale tijd is ingesteld.

---

## Time-trial finishen

Een actieve time-trial finish je met:

```text
/finishtimer
```

De plugin slaat dan de verstreken tijd op als score voor het team waarmee de time-trial gestart is.

Voorbeeld:

```text
/startall --countup --team "Team Blauw"
/finishtimer
```

### Teamnaam overschrijven bij finish

Je kunt bij het finishen ook een teamnaam meegeven:

```text
/finishtimer --team "Team Rood"
```

Dan wordt de score opgeslagen onder `Team Rood`.

### Belangrijk

`/finishtimer` werkt alleen bij een actieve countup/time-trial.

Bij een normale countdown gebruik je:

```text
/stopall
```

of laat je de timer aflopen.

---

## Scores

Scores worden opgeslagen per logische sessie-key.

Voorbeelden:

```text
climatecrafter
klimaatspeurtocht
GeoFort_Heat
```

De scores worden opgeslagen in:

```text
plugins/GeoSpeurtocht/scores.yml
```

### Scores tonen

```text
/scores
```

Toont de top scores van de logische speurtocht waarin de begeleider staat.

### Scores resetten

```text
/scores reset --confirm
```

Reset de scores van de huidige logische speurtocht.

Zonder `--confirm` wordt er niets verwijderd:

```text
/scores reset
```

---

## Scoreboard/sidebar

De sidebar kan worden aangezet met:

```text
/scoreboardtoggle
```

Nogmaals uitvoeren zet de sidebar weer uit:

```text
/scoreboardtoggle
```

De sidebar toont onder andere:

* Naam van de speurtocht
* Status: actief, gepauzeerd of gestopt
* Teamnaam
* Huidige tijd
* Beste tijd/record
* Aantal actieve spelers
* Top 3 scores
* Huidige plaats tijdens een time-trial

### Let op

De sidebar gebruikt de standaard Bukkit/Spigot-scoreboard API. Daardoor kunnen rechts rode score-nummers zichtbaar zijn. Die zijn cosmetisch en hebben geen invloed op de werking.

---

## Speler toevoegen aan actieve speurtocht

Met dit commando kan een speler alsnog worden toegevoegd aan de actieve speurtocht:

```text
/addspeler <speler>
```

Voorbeeld:

```text
/addspeler Kevin
```

De speler wordt dan:

* toegevoegd aan de actieve deelnemers
* naar het startpunt geteleporteerd
* vrijgegeven/unfreezed
* toegevoegd aan de bossbar

Een speler kan niet tegelijk in twee actieve speurtochten zitten.

---

## Speler verwijderen uit actieve speurtocht

Er zijn twee manieren om een speler te verwijderen.

### Verwijderen, naar start zetten en bevriezen

```text
/removespeler <speler> freeze
```

Voorbeeld:

```text
/removespeler Kevin freeze
```

De speler wordt dan:

* uit de actieve deelnemers gehaald
* van de bossbar verwijderd
* naar het startpunt geteleporteerd
* bevroren

### Verwijderen en vrijgeven op huidige plek

```text
/removespeler <speler> release
```

Voorbeeld:

```text
/removespeler Kevin release
```

De speler wordt dan:

* uit de actieve deelnemers gehaald
* van de bossbar verwijderd
* op zijn huidige plek gelaten
* vrijgegeven/unfreezed

Een speler die met `release` verwijderd is, wordt bij het einde van de speurtocht niet alsnog teruggezet.

---

## Stoppen van de speurtocht

### `/stopall`

```text
/stopall
```

Dit doet het volgende:

* Stopt de timer.
* Zet actieve deelnemers terug naar het startpunt.
* Bevriest deze spelers.
* Zet deze spelers in de Multiverse-gamemode van de startwereld.
* Leegt standaard de inventory van gewone spelers.

### `/stopall --keepinventory`

```text
/stopall --keepinventory
```

Zelfde als `/stopall`, maar de inventory blijft behouden.

### `/stopall --force`

```text
/stopall --force
```

Dit stopt de actieve logische speurtocht geforceerd.

Bij multi-world sessies geldt dit voor alle online spelers binnen dezelfde logische sessie, niet voor de hele server.

Voorbeeld:

Als de begeleider in `climatecrafter_diamant` staat, geldt force voor:

```text
climatecrafter_diamant
climatecrafter_goud
climatecrafter_ijzer
climatecrafter_koper
climatecrafter_steenkool
```

### `/stopall --force --keepinventory`

```text
/stopall --force --keepinventory
```

Zelfde als `/stopall --force`, maar de inventory blijft behouden.

### `/stoptimers`

```text
/stoptimers
```

Stopt alleen de timer en bossbar, maar reset spelers niet.

Gebruik daarna eventueel `/stopall` om spelers alsnog terug te zetten.

---

## Freeze-systeem

Spelers kunnen worden bevroren of vrijgegeven.

Een bevroren speler kan niet bewegen. Als hij probeert te bewegen, wordt hij teruggezet naar zijn vorige locatie.

Begeleiders zijn standaard beschermd tegen speurtocht-resets en freeze-effecten waar dat in de code zo is ingericht.

---

## Permissies

### `begeleider`

Voor begeleiders/admins.

Standaard voor operators.

Begeleiders kunnen:

* speurtocht starten en stoppen
* time-trials starten en finishen
* scores bekijken en resetten
* scoreboard/sidebar toggelen
* timers pauzeren en hervatten
* spelers freezen/unfreezen
* spelers toevoegen/verwijderen
* tijd toevoegen
* broadcasts sturen
* spelers teleporteren

### `speler`

Voor gewone spelers.

Standaard voor iedereen.

---

## Commando's

| Commando                                          | Beschrijving                                                               | Permissie  |
| ------------------------------------------------- | -------------------------------------------------------------------------- | ---------- |
| `/setstart`                                       | Zet jouw huidige locatie als startpunt voor de huidige logische speurtocht | begeleider |
| `/startpunt`                                      | Teleporteert naar het startpunt van de huidige logische speurtocht         | speler     |
| `/startall <minuten>`                             | Start een normale speurtocht met countdown                                 | begeleider |
| `/startall <minuten> --countdown`                 | Start expliciet een countdown                                              | begeleider |
| `/startall --countup --team "Teamnaam"`           | Start een time-trial zonder maximale tijd                                  | begeleider |
| `/startall <minuten> --countup --team "Teamnaam"` | Start een time-trial met maximale tijd                                     | begeleider |
| `/pausetimer`                                     | Pauzeert de actieve timer                                                  | begeleider |
| `/resumetimer`                                    | Hervat de actieve timer                                                    | begeleider |
| `/finishtimer`                                    | Finisht een actieve time-trial en slaat de score op                        | begeleider |
| `/finishtimer --team "Teamnaam"`                  | Finisht een time-trial en overschrijft de teamnaam                         | begeleider |
| `/scores`                                         | Toont de scores van de huidige logische speurtocht                         | begeleider |
| `/scores reset --confirm`                         | Reset scores van de huidige logische speurtocht                            | begeleider |
| `/scoreboardtoggle`                               | Zet de sidebar aan of uit                                                  | begeleider |
| `/addtime <seconden>`                             | Voegt seconden toe aan de actieve timer                                    | begeleider |
| `/addspeler <speler>`                             | Voegt een speler toe aan de actieve speurtocht                             | begeleider |
| `/removespeler <speler> freeze`                   | Verwijdert speler, teleporteert naar start en bevriest                     | begeleider |
| `/removespeler <speler> release`                  | Verwijdert speler en geeft hem vrij op huidige plek                        | begeleider |
| `/stopall`                                        | Stopt de speurtocht en reset actieve spelers                               | begeleider |
| `/stopall --keepinventory`                        | Zelfde als stopall, maar inventory blijft behouden                         | begeleider |
| `/stopall --force`                                | Stopt de volledige logische sessie geforceerd                              | begeleider |
| `/stopall --force --keepinventory`                | Zelfde als force, maar inventory blijft behouden                           | begeleider |
| `/stoptimers`                                     | Stopt alleen de timer, zonder spelers te resetten                          | begeleider |
| `/freezeall`                                      | Bevriest alle spelers                                                      | begeleider |
| `/unfreezeall`                                    | Laat alle bevroren spelers weer vrij                                       | begeleider |
| `/freeze <speler>`                                | Bevriest één speler                                                        | begeleider |
| `/unfreeze <speler>`                              | Laat één speler weer vrij                                                  | begeleider |
| `/tpall`                                          | Teleporteert alle spelers naar jouw locatie                                | begeleider |
| `/broadcast <bericht>`                            | Stuurt een bericht naar alle spelers                                       | begeleider |
| `/kinderkiller`                                   | Geeft de begeleider de Kinder Killer stick                                 | begeleider |

---

## Testvolgorde

Na installatie kun je eerst de normale flow testen:

```text
/setstart
/startall 1
/pausetimer
/resumetimer
/stopall
```

Daarna de time-trial flow:

```text
/startall --countup --team "Team Blauw"
/pausetimer
/resumetimer
/finishtimer
/scores
```

Daarna scores resetten:

```text
/scores reset
/scores reset --confirm
/scores
```

Daarna scoreboard/sidebar testen:

```text
/scoreboardtoggle
/startall --countup --team "Team Rood"
/finishtimer
/scoreboardtoggle
```

---

## Opmerkingen

* De plugin maakt zelf geen speurtochtlocaties of opdrachten.
* De opdrachten bouw je zelf in de Minecraft-wereld.
* De plugin koppelt spelers aan logische speurtocht-sessies via de wereld waarin zij staan.
* Gewone spelers kunnen bij `/stopall` standaard hun inventory kwijtraken.
* Gebruik `/stopall --keepinventory` als inventory behouden moet blijven.
* Scores worden opgeslagen in `plugins/GeoSpeurtocht/scores.yml`.
* Startpunten worden opgeslagen in `plugins/GeoSpeurtocht/config.yml`.
* De sidebar is bedoeld als begeleidershulpmiddel, niet als verplichte speler-UI.

---

## Auteur

Originele plugin:

* Brian van Beusekom

Aangepaste versie:

* Brian van Beusekom ft. Kevin de Schepper
