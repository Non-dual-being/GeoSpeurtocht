# GeoSpeurtocht

Een plugin voor de Minecraft-speurtocht op het GeoFort.

Met deze plugin kunnen begeleiders een speurtocht starten vanuit een ingesteld startpunt. Spelers worden eerst bevroren op het startpunt, daarna vrijgelaten voor een ingestelde tijd, en aan het einde weer teruggezet en bevroren.

Deze aangepaste versie ondersteunt ook wereldspecifiek gedrag met Multiverse:
- de speurtocht is alleen actief in de wereld waarin hij gestart wordt
- `/stopall` werkt alleen op spelers in de actieve speurtochtwereld
- `/stopall --force` werkt op alle spelers op de server
- spelers kunnen optioneel hun inventory behouden met `--keepinventory`
- bij reset worden spelers in de gamemode gezet die in Multiverse voor die wereld is ingesteld

## Functies

- Startpunt instellen
- Speurtocht starten met timer
- Bossbar met resterende tijd
- Spelers freezen en unfreezen
- Alleen effect in de actieve speurtochtwereld
- Force-stop voor de hele server
- Optioneel inventory behouden
- Reset naar de Multiverse-gamemode van de startwereld

## Werking

### Start van de speurtocht
- De begeleider zet eerst een startpunt met `/setstart`
- Met `/startall <tijd in minuten>` wordt de speurtocht gestart
- De wereld van het startpunt wordt opgeslagen als actieve speurtochtwereld
- Alleen spelers in die wereld worden meegenomen
- Zij worden naar het startpunt geteleporteerd en daarna vrijgelaten

### Stoppen van de speurtocht
#### `/stopall`
- Stopt de timer
- Zet alleen spelers in de actieve speurtochtwereld terug naar het startpunt
- Bevriest deze spelers
- Zet deze spelers in de gamemode die voor die wereld in Multiverse is ingesteld
- Leegt standaard de inventory van gewone spelers

#### `/stopall --keepinventory`
- Zelfde als `/stopall`
- Maar inventory blijft behouden

#### `/stopall --force`
- Stopt de timer
- Zet alle spelers op de hele server terug naar het startpunt
- Bevriest deze spelers
- Zet deze spelers in de gamemode die voor de startpuntwereld in Multiverse is ingesteld
- Leegt standaard de inventory van gewone spelers

#### `/stopall --force --keepinventory`
- Zelfde als `/stopall --force`
- Maar inventory blijft behouden

## Permissies

### begeleider
Heeft controle over de spelers en is zelf immuun voor freeze en resets.  
Standaard voor operators.

### speler
Voor gewone spelers.  
Standaard aan voor alle spelers.

## Commando's

| Commando | Beschrijving | Permissie |
|----------|--------------|-----------|
| `/setstart` | Zet jouw huidige locatie als startpunt | begeleider |
| `/startpunt` | Teleporteert naar het startpunt | speler |
| `/startall <tijd>` | Start de speurtocht voor het opgegeven aantal minuten | begeleider |
| `/stopall` | Stopt de speurtocht en reset spelers in de actieve wereld | begeleider |
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

## Benodigdheden

### Verplicht
- Spigot/Paper 1.21.x
- Multiverse-Core 5.5.2 of compatibele versie

## Opmerkingen

- De plugin maakt zelf geen speurtochtlocaties of opdrachten. Die bouw je zelf in de wereld.
- Alleen spelers zonder begeleider-permissie worden standaard gereset en krijgen hun inventory leeggemaakt.
- De speurtocht is gekoppeld aan de wereld van het ingestelde startpunt.

## Auteur

Originele plugin:
- Brian van Beusekom

Aangepaste versie:
- Brian van Beusekom ft. Kevin de Schepper