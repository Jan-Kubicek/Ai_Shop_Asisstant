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
healthcheck v Dockeru. Ostatní endpointy vyžadují autentizaci; modul
<code>auth</code> zatím není implementovaný, takže přihlášení ve Fázi 0
vrací chybu. To je očekávané.

## 3. Frontend

~~~bash
cd frontend
npm install
npm run dev
~~~

Frontend běží na <code>http://localhost:5173</code>. Neočekávaný uživatel
je přesměrován na <code>/login</code>, kde se zobrazí přihlašovací formulář.

## 4. Testy a CI

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
