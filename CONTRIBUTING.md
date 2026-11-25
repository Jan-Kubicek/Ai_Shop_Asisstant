# Přispívání do projektu

Díky, že chcete projektu pomoci. Tento dokument popisuje, jak projekt spustit,
jakým stylem psát kód a jak posílat změny.

## Požadavky

- JDK 21 a Maven 3.9+
- Node.js 20+ a npm
- Docker Desktop
- Git

## Lokální vývoj

~~~bash
# 1. Závislosti (MongoDB)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend
cd backend
mvn spring-boot:run

# 3. Frontend (v dalším terminálu)
cd frontend
npm install
npm run dev
~~~

Podrobnosti najdete v [docs/deployment.md](docs/deployment.md).

## Větve

- <code>main</code> – vždy funkční, chráněná, změny pouze přes pull request.
- <code>feature/&lt;popis&gt;</code> – nová funkce.
- <code>fix/&lt;popis&gt;</code> – oprava chyby.
- <code>docs/&lt;popis&gt;</code> – dokumentace.
- <code>chore/&lt;popis&gt;</code> – údržba, závislosti, CI.

## Commity

Používáme [Conventional Commits](https://www.conventionalcommits.org/):

~~~text
feat(auth): přidáno přihlášení přes JWT
fix(upload): oprava kontroly velikosti souboru
docs(readme): doplněna kapitola o bezpečnosti
test(pipeline): testy validace výstupu AI
chore(deps): aktualizace Spring Boot
~~~

Commitujte po malých krocích a srozumitelně. Jeden commit = jedna logická změna.

## Pull request

Před odesláním PR zkontrolujte:

- [ ] Kód se lokálně zkompiluje a testy procházejí.
- [ ] Nová funkce má testy (unit nebo integrační).
- [ ] Změny v API jsou popsané v <code>docs/api.md</code>.
- [ ] Změny konfigurace jsou v <code>.env.example</code>.
- [ ] Nejsou v repu žádné secrety, hesla ani reálná data.
- [ ] Dokumentace odpovídá skutečnosti.
- [ ] PR má krátký popis a odkaz na issue.

## Styl kódu

### Backend

- Java 21, 4 mezery, balíčky po modulech.
- Dodržujte hranice modulů: komunikace pouze přes <code>*Api</code> rozhraní nebo aplikační události.
- Validace vstupů přes Bean Validation, žádné skládání dotazů z řetězců.
- Veřejné metody mají Javadoc, pokud jejich účel není zřejmý.

### Frontend

- Vue 3 Composition API a <code>&lt;script setup lang="ts"&gt;</code>.
- TypeScript ve strict režimu, žádné <code>any</code> bez vážného důvodu.
- Komponenty v PascalCase, soubory s příponou <code>.vue</code>.
- Texty v UI česky, srozumitelně a bez technického žargonu.

### Obecně

- Žádné mrtvé soubory ani zakomentovaný kód.
- Názvy anglicky, uživatelské texty česky.
- Tajemství pouze v env proměnných.

## Testy

~~~bash
# Backend
cd backend
mvn verify

# Frontend
cd frontend
npm run test:run
npm run lint

# E2E (vyžaduje běžící aplikaci)
cd e2e
npm install
npm test
~~~

## Bezpečnost

Zranitelnosti nehlaste veřejně. Postupujte podle [SECURITY.md](SECURITY.md).
