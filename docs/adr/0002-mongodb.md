# ADR-0002: MongoDB

- **Stav:** Přijato
- **Datum:** 2026-11-25

## Kontext

Data mají přirozeně dokumentový charakter: požadavek obsahuje volby, výsledek
obsahuje strukturované texty a JSON pro WordPress, auditní záznamy mají
proměnlivá metadata. Neočekáváme složité relační dotazy ani silné transakce
napříč mnoha tabulkami.

## Rozhodnutí

Použijeme **MongoDB 4.4** se Spring Data MongoDB. Fotky se ukládají mimo databázi,
v dokumentech je pouze odkaz. Indexy vytváří inicializační skript.

## Důsledky

### Pozitivní

- Přirozené ukládání JSON struktury a flexibilní schéma.
- Snadné horizontální škálování a jednoduchý vývoj.
- Dobrá podpora v Spring Bootu a Testcontainers.

### Negativní

- Chybí silná schémata na úrovni databáze – je potřeba validace v aplikaci.
- Riziko NoSQL injection – řešíme typovanými DTO a filtrem operátorů.
- Zálohy a obnova vyžadují samostatný postup.

## Alternativy

- **PostgreSQL** – silnější konzistence, ale složitější mapování JSON.
- **SQLite** – jednoduché, ale nevhodné pro souběh a růst.
- **Souborové úložiště** – nedostatečné pro dotazy a audit.
