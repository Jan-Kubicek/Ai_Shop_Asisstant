# Import do WordPressu / WooCommerce

Cílem je připravit JSON a obrázky tak, aby je šlo co nejjednodušeji dostat
do WooCommerce. Import zůstává **ruční a kontrolovaný** – nic se nepublikuje samo.

## Výstupní ZIP

Po stažení obsahuje ZIP:

~~~text
export-<requestId>/
  product.json
  photo-original.jpg
  photo-model.jpg
  README.txt
~~~

## Struktura product.json

~~~json
{
  "schemaVersion": "1.0",
  "generatedAt": "2026-11-25T10:20:00Z",
  "requestId": "6740f1c2a1b2c3d4e5f60789",
  "product": {
    "name": "Ručně pletená čepice – šedá",
    "type": "simple",
    "status": "draft",
    "short_description": "Teplá čepice z jemné vlny.",
    "description": "<p>Delší popis produktu...</p>",
    "categories": [{ "name": "Čepice" }],
    "tags": [{ "name": "ruční práce" }, { "name": "zima" }],
    "images": [
      { "src": "photo-original.jpg", "alt": "Šedá pletená čepice" },
      { "src": "photo-model.jpg", "alt": "Čepice na modelce" }
    ],
    "meta_data": [
      { "key": "_ai_generated", "value": "true" }
    ]
  },
  "social": {
    "instagram": { "caption": "...", "hashtags": ["#handmade", "#zima"] },
    "facebook": { "text": "..." }
  }
}
~~~

## Mapování na WooCommerce

| JSON | WooCommerce | Poznámka |
|------|-------------|----------|
| <code>product.name</code> | <code>name</code> | název produktu |
| <code>product.type</code> | <code>type</code> | vždy <code>simple</code> |
| <code>product.status</code> | <code>status</code> | vždy <code>draft</code> |
| <code>short_description</code> | <code>short_description</code> | krátký popis |
| <code>description</code> | <code>description</code> | HTML z AI, sanitizované |
| <code>categories</code> | <code>categories</code> | podle názvu |
| <code>tags</code> | <code>tags</code> | podle názvu |
| <code>images</code> | <code>images</code> | po nahrání do médií |
| <code>meta_data</code> | <code>meta_data</code> | označení AI původu |

## Postup importu

1. V administraci aplikace otevřete **Fronta e-shopu**.
2. Stáhněte ZIP a rozbalte ho.
3. V WordPressu nahrajte obrázky do knihovny médií.
4. Vytvořte produkt nebo použijte importér, který umí JSON/CSV.
5. Zkontrolujte texty, ceny, dostupnost a kategorie.
6. Publikujte až po kontrole.
7. V aplikaci označte požadavek jako **importovaný**.

## Obrázky

- JSON obsahuje relativní názvy souborů, ne URL.
- Při importu je potřeba obrázky nejprve nahrát do WordPressu.
- U vygenerované fotky s modelem uveďte, že je ilustrační a vytvořená AI.
- Doporučený formát: JPEG, šířka 1200–1600 px, kvalita 85.

## Ověření verze

Přesná pole se mohou lišit podle verze WooCommerce a použitého importéru.
Před prvním ostrým importem:

1. Ověřte verzi WooCommerce.
2. Otestujte import na staging e-shopu.
3. Zkontrolujte, zda importér očekává JSON, CSV nebo XML.
4. Podle potřeby upravte <code>WooJsonBuilder</code>.

## Časté problémy

| Problém | Řešení |
|---------|--------|
| Obrázky se nenaimportují | nahrajte je do médií a upravte cesty |
| Kategorie se vytvoří duplicitně | použijte existující ID nebo přesný název |
| HTML se zobrazí jako text | zkontrolujte, zda importér neošetřuje escapování |
| Chybí diakritika | ověřte UTF-8 v JSON i v databázi |
