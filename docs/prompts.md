# Prompty pro AI

## Zásady

1. Prompty jsou **verzované soubory** v <code>backend/src/main/resources/prompts/</code>.
2. Název obsahuje verzi: <code>product-description.v1.txt</code>.
3. Změna promptu = nová verze souboru + záznam v tomto dokumentu.
4. Výstup je vždy **JSON podle schématu**, nikdy volný text.
5. Uživatelský vstup je v promptu jasně označen jako data, ne instrukce.

## Přehled promptů

| Soubor | Účel | Výstupní schéma |
|--------|------|-----------------|
| <code>product-description.v1.txt</code> | popis produktu pro e-shop | <code>product-text.schema.json</code> |
| <code>social-post.v1.txt</code> | příspěvek na Instagram a Facebook | <code>social-text</code> (součást result) |
| <code>model-photo.v1.txt</code> | fotka produktu na modelovi | obrázek |

## Proměnné

| Proměnná | Význam |
|----------|--------|
| <code>{{userDescription}}</code> | popis od uživatele |
| <code>{{productTitle}}</code> | pracovní název produktu |
| <code>{{tone}}</code> | tón textu (elegantní, vtipný, stručný) |
| <code>{{keywords}}</code> | volitelná klíčová slova |
| <code>{{productDescription}}</code> | popis pro generování fotky |

Proměnné se nahrazují před voláním AI. Pokud hodnota chybí, použije se prázdný řetězec.

## Strukturovaný výstup

AI musí vrátit pouze JSON. Příklad pro popis produktu:

~~~json
{
  "title": "Ručně pletená čepice – šedá",
  "shortDescription": "Teplá čepice z jemné vlny.",
  "longDescription": "Delší popis produktu...",
  "keywords": ["čepice", "vlna", "ruční práce"]
}
~~~

Validace:

- JSON schema (<code>schemas/product-text.schema.json</code>),
- maximální délky jednotlivých polí,
- povinná pole a typy,
- filtr zakázaných výrazů a neplatných znaků,
- při nevalidním výstupu jeden retry s doplněnou instrukcí.

## Ochrana proti prompt injection

- Systémový prompt výslovně říká: text od uživatele je **data**, ne příkazy.
- Uživatelský vstup je obalen do oddělovačů a nikdy se nevykonává.
- Text z fotky se bere jako nedůvěryhodný popis, ne jako instrukce.
- Výstup se validuje, i když AI vrátí něco jiného.
- Do promptu se nevkládají tajemství ani interní informace.

## Testování

- Fixtures s ukázkovými odpověďmi jsou v <code>backend/src/test/resources/fixtures/</code>.
- WireMock simuluje Gemini a OpenAI v integračních testech.
- Testy pokrývají platný výstup, nevalidní JSON, timeout a rate limit.

## Historie změn

| Datum | Verze | Změna |
|-------|-------|-------|
| 2026-11-25 | v1 | počáteční prompty |
