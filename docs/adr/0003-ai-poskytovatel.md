# ADR-0003: AI poskytovatel

- **Stav:** Přijato
- **Datum:** 2026-11-25

## Kontext

Potřebujeme generovat české texty z fotky a popisu a volitelně upravovat
fotky. Poskytovatelé mají odlišné ceny, limity a kvalitu. Free tier se mění,
takže na jednoho poskytovatele se nelze plně spolehnout.

## Rozhodnutí

Zavedeme rozhraní <code>AiTextClient</code> a <code>AiImageClient</code>
s implementacemi pro **Gemini** a **OpenAI**. Poskytovatel se vybírá konfigurací
(<code>AI_PROVIDER_TEXT</code>, <code>AI_PROVIDER_IMAGE</code>). Primární je Gemini,
OpenAI je záložní.

## Důsledky

### Pozitivní

- Snadná výměna poskytovatele bez zásahu do business logiky.
- Možnost fallbacku při výpadku nebo vyčerpání kvóty.
- Testy mohou používat WireMock a nevolat reálné API.

### Negativní

- Je potřeba udržovat dvě implementace a sladit výstupy.
- Odlišné formáty odpovědí a chybování.
- Vyšší nároky na testování.

## Alternativy

- **Jeden poskytovatel** – jednodušší, ale rizikový výpadek a horší ceny.
- **Lokální model** – vyšší nároky na hardware a kvalita češtiny.
- **Vlastní fine-tuning** – zbytečné pro MVP.
