# ADR-0004: Úložiště souborů

- **Stav:** Přijato
- **Datum:** 2026-11-25

## Kontext

Ukládáme nahrané fotky a volitelně vygenerované obrázky. Objem je malý,
ale soubory nesmí být veřejně dostupné bez autorizace. Provoz může běžet
na VPS nebo domácím serveru, později případně v cloudu.

## Rozhodnutí

Zavedeme rozhraní <code>Storage</code> s implementacemi:

- **LocalStorage** – výchozí, soubory v <code>data/uploads</code>,
- **GridFsStorage** – soubory v MongoDB, vhodné pro menší instalace,
- **S3Storage** – pro budoucí cloudové nasazení.

Typ se vybírá přes <code>STORAGE_TYPE</code>. Soubory se ukládají pod náhodným
klíčem, přístup je pouze přes autorizovaný endpoint.

## Důsledky

### Pozitivní

- Jednoduchý start bez další služby.
- Snadná migrace mezi variantami díky jednotnému rozhraní.
- Soubory jsou mimo veřejný web root.

### Negativní

- Lokální úložiště komplikuje škálování na více instancí.
- Zálohování musí pokrývat i soubory, ne jen databázi.
- GridFS zvyšuje zátěž databáze.

## Alternativy

- **Pouze S3** – závislost na cloudu a náklady.
- **Pouze lokální disk** – jednoduché, ale nepřenosné.
- **Ukládání přímo do dokumentů** – nevhodné pro velké soubory.
