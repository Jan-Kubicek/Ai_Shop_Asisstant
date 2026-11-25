# Lokální vývoj a ověření kostry

Tento dokument popisuje, jak spustit vývojové prostředí a jak ověřit,
že kostra aplikace funguje (Fáze 0).

## Požadavky

- Docker Desktop (běžící Docker Engine)
- JDK 21
- Node.js 20+ a npm
- Git

Maven není nutné instalovat, v repozitáři je Maven Wrapper
(<code>backend/mvnw</code>, na Windows <code>backend\mvnw.cmd</code>).

## 1. MongoDB

~~~bash
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml ps
~~~

Kontejner <code>mongo</code> musí být ve stavu <code>healthy</code>.
Pokud v projektu není soubor <code>.env</code>, použijí se vývojové
výchozí hodnoty (<code>root/root</code> pro správu, <code>app/app</code>
pro aplikaci). Tyto hodnoty nikdy nepoužívejte v produkci.

Ověření přístupu aplikačního uživatele:

~~~bash
docker exec ai-shop-assistant-dev-mongo-1 \
  mongosh --quiet --host 0.0.0.0 -u app -p app \
  --authenticationDatabase shopassistant \
  --eval "db.runCommand({ ping: 1 }).ok"
~~~

Očekávaný výstup je <code>1</code>.

## 2. Backend

~~~bash
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
~~~

Backend poslouchá na <code>http://localhost:8080</code>. Ověření:

~~~bash
curl http://localhost:8080/actuator/health
~~~

Očekávaná odpověď:

~~~json
{"status":"UP","groups":["liveness","readiness"]}
~~~

Endpoint <code>/actuator/health</code> je veřejný, protože ho používá
healthcheck v Dockeru. Ostatní endpointy vyžadují přihlášení.

## 3. Vytvoření admina

Admin se zakládá pouze při spuštění s parametrem <code>--seed-admin</code>
a jen tehdy, když je kolekce uživatelů prázdná. Údaje se předávají
proměnnými prostředí, nikdy se necommitují:

~~~bash
cd backend
./mvnw package -DskipTests

SEED_ADMIN_EMAIL="admin@example.com" \
SEED_ADMIN_PASSWORD="zvolte-silne-heslo" \
java -jar target/shop-assistant-0.1.0-SNAPSHOT.jar --seed-admin
~~~

Windows PowerShell:

~~~powershell
$env:SEED_ADMIN_EMAIL = "admin@example.com"
$env:SEED_ADMIN_PASSWORD = "zvolte-silne-heslo"
java -jar target/shop-assistant-0.1.0-SNAPSHOT.jar --seed-admin
~~~

Po vytvoření uživatele už parametr <code>--seed-admin</code> není potřeba;
přihlaste se na <code>http://localhost:5173</code>. Případné další účty
přidá administrace v dalších fázích.

## 4. Frontend

~~~bash
cd frontend
npm install
npm run dev
~~~

Frontend běží na <code>http://localhost:5173</code>. Neočekávaný uživatel
je přesměrován na <code>/login</code>, kde se zobrazí přihlašovací formulář.

## 5. Testy a CI

Lokálně spusťte stejné příkazy jako CI:

~~~bash
# Backend (build, testy, Modulith hranice modulů)
cd backend
./mvnw -B -ntp verify

# Frontend (lint, unit testy, produkční build)
cd frontend
npm ci
npm run lint
npm run test:run
npm run build
~~~

Workflow:

- <code>.github/workflows/backend-ci.yml</code> – JDK 21, <code>mvn verify</code>,
- <code>.github/workflows/frontend-ci.yml</code> – Node.js 22, <code>npm ci</code>,
  lint, testy a build.

Test <code>ModularityTests</code> hlídá hranice modulů Spring Modulith.
Test <code>ShopAssistantApplicationTests</code> ověřuje, že se Spring
kontext sestaví i bez běžící databáze.

## 6. AI (Fáze 3)

Backend používá rozhraní `AiTextClient`. Ve výchozím nastavení běží
`GeminiTextClient` s klíčem z `GEMINI_API_KEY` a modelem z `AI_MODEL_TEXT`.

Poskytovatele textu vybírá `AI_PROVIDER_TEXT`:

- `gemini` (výchozí) – `GEMINI_API_KEY`, model `AI_MODEL_TEXT`,
- `openai` – `OPENAI_API_KEY`, model `OPENAI_MODEL` (výchozí `gpt-4.1-mini`),
- `fake` – žádné API volání, fixní JSON.

Pro vývoj bez spotřeby tokenů:

~~~bash
AI_PROVIDER_TEXT=fake java -jar target/shop-assistant-0.1.0-SNAPSHOT.jar
~~~

Výstup se validuje proti schématu v `schemas/product-text.schema.json`
a prochází sanitizací (odstranění markdown bloků, kontrola povinných polí).

Ruční ověření kvality textů ve vývoji (endpoint není v produkci dostupný):

~~~bash
curl -X POST http://localhost:8080/api/v1/ai/product-text \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"description":"Ručně pletená čepice z vlny","tone":"věcný"}'
~~~

## 7. Pipeline, export a fronta (Fáze 4)

Worker v modulu `pipeline` běží přes `@Scheduled` a zpracovává požadavky
v pořadí `CREATED → PROCESSING → REVIEW`. Každý běh zapíše záznam
do kolekce `ai_calls` (provider, latence, stav).

Admin po kontrole schválí výsledek přes `POST /api/v1/requests/{id}/approve`
– požadavek se přesune do `QUEUED_FOR_SHOP` a přidá se do fronty e-shopu.
Odtud lze stáhnout WooCommerce JSON nebo označit položku jako importovanou.

Stav požadavku lze sledovat přes SSE: `GET /api/v1/requests/{id}/events`
vyžaduje Bearer token a posílá události `status` (CREATED → PROCESSING →
REVIEW). Frontend preferuje SSE a pokud není dostupný, přepne se na polling.

Výsledek lze upravit přes `PATCH /api/v1/requests/{id}/result` a stáhnout
celý balík přes `GET /api/v1/requests/{id}/download` (ZIP s texty, exportem
a fotkou).

E2E s mockovanou AI (vyžaduje `AI_PROVIDER_TEXT=fake`):

~~~bash
cd backend
AI_PROVIDER_TEXT=fake java -jar target/shop-assistant-0.1.0-SNAPSHOT.jar

cd ../e2e
E2E_USER_EMAIL=admin@example.com E2E_USER_PASSWORD=tajne-heslo npx playwright test
~~~

## Časté problémy

- **Port 8080 nebo 5173 je obsazený** – ukončete předchozí proces nebo
  změňte port (<code>server.port</code>, respektive
  <code>server.port</code> ve <code>vite.config.ts</code>).
- **Mongo se nespustí** – ověřte, že běží Docker Engine, a podívejte se do
  <code>docker compose -f docker-compose.dev.yml logs mongo</code>.
- **Backend hlásí chybu připojení k MongoDB** – zkontrolujte, že kontejner
  je <code>healthy</code>, a že <code>MONGO_URI</code> odpovídá údajům
  v <code>.env</code>, případně výchozím hodnotám.
- **Změnili jste hesla v <code>.env</code> po prvním spuštění** – Mongo si
  inicializační skript spouští jen nad prázdným datovým adresářem. Smažte
  <code>data/mongo-dev</code> a spusťte kontejner znovu.
