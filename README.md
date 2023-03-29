# Simulator for køalgoritmer

En enkel simulator for køalgoritmer. 

## Input / variabler

Som bruker kan du justere følgende

For fordeling av triage hastegrader
- `oransjePerDag`, `gulePerDag`, `gronnePerDag` - andel oransje, grønne og gule pasienter. Antallet her angir andelen av pasientene som får hver av prioritetene, ikke totalt antall pasienter

For flyten av pasienter
- `gjennomsnittligIntervallAnkomst` - antall minutter (i gjennomsnitt) mellom hver ankomst.
- `gjennomsnittForLedigLege` - antall minutter (i gjennomsnitt) mellom hver gang en lege tar inn en ny pasient
- `variansAnkomst` - varians på intervallet mellom to pasienter. Ankomsttidspunktene er poisson-distribuert. Høyere varians gir mindre forutsigbar pasientstrøm. Verdien 0 gir fast intervall. 
- `variansBehandling` - varians på intervallet mellom to innkallinger av pasienter. Ankomsttidspunktene er poisson-distribuert. Høyere varians gir mindre forutsigbar pasientstrøm. Verdien 0 gir fast intervall.
- `antallPasienter` - hvor mange pasienter man ønsker å simulere. NB: Testen slutter å behandle folk ved midnatt, så det er ikke å sette denne altfor høyt

Algoritme
- `algoritme` - hvilken algoritme som skal brukes. "FIFO" er uten prioritet, "TODAY" er en forenklet variant av dagens, og "FOUR_STATUSES" er den foreslåtte algoritmen i det nye systemet.
- `status2Capacity` og `status3Capacity` - kapasitet til status 2 og 3 i den nye algoritmen. Fungerer kun sammen med "FOUR_STATUSES"

Tilfeldighet og reproduksjon
- `randomSeed` - input for tilfeldighetsalgoritmen. Endres denne, får man nye, tilfeldige tall, mens to kjøringer med samme randomSeed gir samme resultat, gitt at alle parameterne og koden er lik.  Brukes for å kunne gjenskape en simulering

## Organisering av kode
For å kjøre en simulering, kjører man SimulationTest. Dette kan gjøres ved hjelp av maven: `mvn test -Dtest=SimulationTest`. Husk å endre parametre dersom ønskelig. 
 
Koden er delt inn i følgende filer: 
`DataClasses` - hjelpeklasser for å holde på data underveis
`Simulation` - klassen som kjører simuleringen. Her ligger implementasjonen av simuleringsalgoritmene. I tillegg visualisering av køen underveis og utskrift av statistikk. 
`Generation` - klassen som genererer "pasienter" (entries) og tidspunkt for innkalling av pasienter. 
