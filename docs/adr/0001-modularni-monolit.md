# ADR-0001: Modulární monolit

- **Stav:** Přijato
- **Datum:** 2026-11-25

## Kontext

Aplikaci bude používat malý počet uživatelů (řádově jednotky až desítky).
Tým je malý a potřebujeme rychlý vývoj, jednoduché nasazení a nízké nároky
na provoz. Zároveň chceme jasné hranice, aby se aplikace dala později rozdělit.

## Rozhodnutí

Backend bude **modulární monolit** postavený na Spring Bootu a Spring Modulith.
Moduly: auth, users, requests, media, ai, pipeline, export, shopqueue, audit, common.
Komunikace probíhá přes veřejná rozhraní <code>*Api</code> nebo aplikační události.

## Důsledky

### Pozitivní

- Jedno nasazení, jedna databáze, jednoduchý provoz a zálohování.
- Rychlý vývoj a ladění, žádná síťová latence mezi moduly.
- Hranice modulů hlídá test, takže se kód nerozpadne.
- Pozdější rozdělení je možné po jednotlivých modulech.

### Negativní

- Všechny moduly se nasazují společně.
- Chyba v jednom modulu může ovlivnit celou aplikaci.
- Je potřeba disciplína při dodržování hranic.

## Alternativy

- **Mikroslužby** – zbytečná složitost pro tento rozsah.
- **Monolit bez modulů** – rychlý start, ale špatná udržovatelnost.
- **Serverless** – nevhodné pro dlouhé AI úlohy a stabilní frontu.
