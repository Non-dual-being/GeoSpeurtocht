# GeoSpeurtocht

Een plugin voor de Minecraft-speurtocht op het GeoFort.

Met deze plugin kunnen begeleiders een speurtocht starten vanuit een ingesteld
startpunt. Spelers worden eerst bevroren op het startpunt, daarna vrijgelaten
voor een ingestelde tijd, en aan het einde weer teruggezet en bevroren.

Deze versie ondersteunt ook wereldspecifiek gedrag met Multiverse en extra
beheerfuncties voor lopende speurtochten.

## Functies

- Startpunt instellen
- Speurtocht starten met timer
- Bossbar met resterende tijd
- Tijd toevoegen aan een lopende timer
- Spelers toevoegen aan een actieve speurtocht
- Spelers verwijderen uit een actieve speurtocht
- Verwijderde spelers terugzetten naar start en bevriezen
- Verwijderde spelers vrijgeven op hun huidige plek
- Spelers freezen en unfreezen
- Alleen effect in de actieve speurtochtwereld
- Force-stop voor de hele server
- Optioneel inventory behouden
- Reset naar de Multiverse-gamemode van de startwereld
- Broadcast-commando voor berichten
- Teleporteer alle spelers naar begeleider

## Multiverse-ondersteuning

De plugin gebruikt Multiverse-Core om de ingestelde gamemode van de actieve
wereld op te halen.

Bij een reset worden spelers teruggezet naar de gamemode die in Multiverse voor
de startwereld is ingesteld.

Deze versie is bedoeld voor Multiverse-Core 5.5.2 of een compatibele versie.

## Werking

### Startpunt instellen

Een begeleider zet eerst een startpunt met:

```text
/setstart
```

De huidige locatie van de begeleider wordt dan opgeslagen als startpunt.

Spelers kunnen naar het startpunt teleporteren met:

```text
/startpunt
```

### Start van de speurtocht

Met dit commando wordt de speurtocht gestart:

```text
/startall <tijd in minuten>
```

Voorbeeld:

```text
/startall 30
```

Bij het starten gebeurt dit:

- De wereld van het startpunt wordt de actieve speurtochtwereld.
- Alleen spelers in die wereld worden meegenomen.
- Operators worden niet meegenomen.
- Spelers worden naar het startpunt geteleporteerd.
- Spelers worden vrijgegeven/unfreezed.
- De bossbar-timer wordt gestart.
- De deelnemers worden bijgehouden als actieve spelers.

### Timer tijdens de speurtocht aanpassen

Met dit commando kan een begeleider extra tijd toevoegen aan de lopende timer:

```text
/addtime <seconden>
```

Voorbeeld:

```text
/addtime 60
```

Dit voegt 60 seconden toe aan de actieve timer.

### Speler toevoegen aan actieve speurtocht

Met dit commando kan een speler alsnog worden toegevoegd aan de actieve
speurtocht:

```text
/addspeler <speler>
```

Voorbeeld:

```text
/addspeler Kevin
```

De speler wordt dan:

- toegevoegd aan de actieve deelnemers
- naar het startpunt geteleporteerd
- vrijgegeven/unfreezed
- toegevoegd aan de bossbar

### Speler verwijderen uit actieve speurtocht

Er zijn twee manieren om een speler te verwijderen.

#### Verwijderen, naar start zetten en bevriezen

```text
/removespeler <speler> freeze
```

Voorbeeld:

```text
/removespeler Kevin freeze
```

De speler wordt dan:

- uit de actieve deelnemers gehaald
- van de bossbar verwijderd
- naar het startpunt geteleporteerd
- bevroren

#### Verwijderen en vrijgeven op huidige plek

```text
/removespeler <speler> release
```

Voorbeeld:

```text
/removespeler Kevin release
```

De speler wordt dan:

- uit de actieve deelnemers gehaald
- van de bossbar verwijderd
- op zijn huidige plek gelaten
- vrijgegeven/unfreezed

Een speler die met `release` verwijderd is, wordt bij het einde van de
speurtocht niet alsnog teruggezet.

## Stoppen van de speurtocht

### `/stopall`

```text
/stopall
```

Dit doet het volgende:

- Stopt de timer.
- Zet actieve deelnemers terug naar het startpunt.
- Bevriest deze spelers.
- Zet deze spelers in de Multiverse-gamemode van de startwereld.
- Leegt standaard de inventory van gewone spelers.

### `/stopall --keepinventory`

```text
/stopall --keepinventory
```

Zelfde als `/stopall`, maar de inventory blijft behouden.

### `/stopall --force`

```text
/stopall --force
```

Dit doet het volgende:

- Stopt de timer.
- Zet alle online spelers op de server terug naar het startpunt.
- Bevriest deze spelers.
- Zet deze spelers in de Multiverse-gamemode van de startwereld.
- Leegt standaard de inventory van gewone spelers.

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

## Freeze-systeem

Spelers kunnen worden bevroren of vrijgegeven.

Een bevroren speler kan niet bewegen. Als hij probeert te bewegen, wordt hij
teruggezet naar zijn vorige locatie.

Operators zijn standaard immuun voor freeze.

## Permissies

### `begeleider`

Voor begeleiders/admins.

Standaard voor operators.

Begeleiders kunnen:

- speurtocht starten en stoppen
- spelers freezen/unfreezen
- spelers toevoegen/verwijderen
- tijd toevoegen
- broadcasts sturen
- spelers teleporteren

### `speler`

Voor gewone spelers.

Standaard voor iedereen.

## Commando's

| Commando | Beschrijving | Permissie |
|----------|--------------|-----------|
| `/setstart` | Zet jouw huidige locatie als startpunt | begeleider |
| `/startpunt` | Teleporteert naar het startpunt | speler |
| `/startall <tijd>` | Start de speurtocht voor het opgegeven aantal minuten | begeleider |
| `/addtime <seconden>` | Voegt seconden toe aan de actieve timer | begeleider |
| `/addspeler <speler>` | Voegt een speler toe aan de actieve speurtocht | begeleider |
| `/removespeler <speler> freeze` | Verwijdert speler, teleporteert naar start en bevriest | begeleider |
| `/removespeler <speler> release` | Verwijdert speler en geeft hem vrij op huidige plek | begeleider |
| `/stopall` | Stopt de speurtocht en reset actieve spelers | begeleider |
| `/stopall --keepinventory` | Zelfde als stopall, maar inventory blijft behouden | begeleider |
| `/stopall --force` | Stopt de speurtocht en reset alle spelers op de server | begeleider |
| `/stopall --force --keepinventory` | Zelfde als force, maar inventory blijft behouden | begeleider |
| `/stoptimers` | Stopt alleen de timer, zonder spelers te resetten | begeleider |
| `/freezeall` | Bevriest alle spelers | begeleider |
| `/unfreezeall` | Laat alle bevroren spelers weer vrij | begeleider |
| `/freeze <speler>` | Bevriest één speler | begeleider |
| `/unfreeze <speler>` | Laat één speler weer vrij | begeleider |
| `/tpall` | Teleporteert alle spelers naar jouw locatie | begeleider |
| `/broadcast <bericht>` | Stuurt een bericht naar alle spelers | begeleider |
| `/kinderkiller` | Geeft de begeleider de Kinder Killer stick | begeleider |

## Benodigdheden

### Verplicht

- Java 21
- Spigot/Paper 1.21.x
- Multiverse-Core 5.5.2 of compatibele versie

### Build tools

- Maven
- IntelliJ IDEA of een andere Java IDE

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
│        └─ plugin.yml
```

Belangrijk: `plugin.yml` moet in deze map staan:

```text
src/main/resources/plugin.yml
```

Als `plugin.yml` niet in de jar zit, kan Spigot/Paper de plugin niet laden.

## Builden

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

## Installatie op de server

1. Stop de Minecraft-server.
2. Bouw de jar met Maven.
3. Kopieer de jar uit `target/` naar de servermap:

```text
plugins/
```

4. Zorg dat Multiverse-Core ook in de `plugins/` map staat.
5. Start de server opnieuw.
6. Controleer de console op:

```text
[GeoSpeurtocht] Finished loading
```

## Testvolgorde

Na installatie kun je testen met:

```text
/setstart
/startall 5
/addtime 60
/addspeler <naam>
/removespeler <naam> release
/removespeler <naam> freeze
/stopall
```

## Opmerkingen

- De plugin maakt zelf geen speurtochtlocaties of opdrachten.
- De opdrachten bouw je zelf in de Minecraft-wereld.
- Operators zijn standaard immuun voor freeze en resets.
- Gewone spelers kunnen bij `/stopall` standaard hun inventory kwijtraken.
- Gebruik `/stopall --keepinventory` als inventory behouden moet blijven.
- De speurtocht is gekoppeld aan de wereld van het ingestelde startpunt.

## Auteur

Originele plugin:

- Brian van Beusekom

Aangepaste versie:

- Brian van Beusekom ft. Kevin de Schepper