# 🛍️ AI Shop Assistant (pracovní název)

> Webová aplikace, která z **fotky a krátkého popisu produktu** vygeneruje popis pro web, příspěvky na **Instagram / Facebook**, volitelně **fotku produktu na modelovi** a připraví **JSON pro import do WordPress e-shopu**.

![status](https://img.shields.io/badge/status-planning-orange)
![backend](https://img.shields.io/badge/backend-Java%2021%20%7C%20Spring%20Boot-green)
![frontend](https://img.shields.io/badge/frontend-Vue%203%20%7C%20Vite-42b883)
![db](https://img.shields.io/badge/db-MongoDB-47A248)

---

## 📑 Obsah

1. [Cíl a motivace](#1-cíl-a-motivace)
2. [Uživatelské role a scénáře](#2-uživatelské-role-a-scénáře)
3. [Funkční požadavky](#3-funkční-požadavky)
4. [Nefunkční požadavky](#4-nefunkční-požadavky)
5. [Architektura](#5-architektura)
6. [Struktura repozitáře](#6-struktura-repozitáře)
7. [Datový model (MongoDB)](#7-datový-model-mongodb)
8. [Pipeline zpracování požadavku](#8-pipeline-zpracování-požadavku)
9. [REST API (návrh)](#9-rest-api-návrh)
10. [Formát JSON pro WordPress import](#10-formát-json-pro-wordpress-import)
11. [AI integrace](#11-ai-integrace)
12. [Bezpečnost](#12-bezpečnost)
13. [Frontend / UX](#13-frontend--ux)
14. [Logování, audit a limity](#14-logování-audit-a-limity)
15. [Lokální spuštění](#15-lokální-spuštění)
16. [Konfigurace (env proměnné)](#16-konfigurace-env-proměnné)
17. [Git workflow a CI/CD](#17-git-workflow-a-cicd)
18. [Testování](#18-testování)
19. [Roadmapa](#19-roadmapa)
20. [Rizika a otevřené otázky](#20-rizika-a-otevřené-otázky)

---

## 1. Cíl a motivace

Ruční tvorba popisků, příspěvků na sociální sítě a produktových fotek je pro malého prodejce časově náročná. Aplikace má celý proces zkrátit na:

1. přihlásit se,
2. nahrát fotku + napsat pár slov,
3. zaškrtnout, co všechno chci vygenerovat,
4. zkontrolovat výsledek a stáhnout / odeslat k importu.

**Principy návrhu**

- **Jednoduchost pro koncového uživatele** – minimum kliků, srozumitelné texty, žádný technický žargon.
- **Člověk ve smyčce** – nic se samo nepublikuje; import do e-shopu provádí správce ručně po kontrole.
- **Lehkost** – frontend musí být svižný i na slabším notebooku.
- **Bezpečnost od začátku** – validace vstupů, autentizace, auditní stopa, ochrana API klíčů.
- **Modularita** – modulární monolit, který lze později případně rozdělit.

---

## 2. Uživatelské role a scénáře

| Role | Popis | Práva |
|------|-------|-------|
| **USER** (maminka) | Běžná uživatelka, vytváří požadavky | vytvořit požadavek, vidět vlastní požadavky a výsledky, stáhnout výstupy |
| **ADMIN** (správce) | Správce e-shopu / aplikace | vše výše + fronta požadavků na e-shop, správa uživatelů, logy, statistiky, limity |

```mermaid
flowchart LR
    U([👩 USER]) --> A1[Přihlášení]
    A1 --> A2[Nový požadavek: fotka + popis]
    A2 --> A3[Volby: popis webu / IG+FB / foto s modelem]
    A3 --> A4[Náhled výsledků a úpravy]
    A4 --> A5[Stažení výstupů]

    AD([🧑‍💻 ADMIN]) --> B1[Fronta požadavků na e-shop]
    B1 --> B2[Kontrola a stažení JSON]
    B2 --> B3[Ruční import do WordPressu]
    AD --> B4[Logy a statistiky uživatelů]
    AD --> B5[Správa uživatelů a limitů]
```

---

## 3. Funkční požadavky

### Must have (MVP)

- [ ] **FR-1** Registrace/přihlášení (účty zakládá ADMIN, uživatelé se nemohou sami registrovat).
- [ ] **FR-2** Formulář nového požadavku: nahrání fotky + textový popis.
- [ ] **FR-3** AI vygeneruje **popis produktu pro web** (český text, titulek, krátký a dlouhý popis, klíčová slova).
- [ ] **FR-4** AI vygeneruje **příspěvek pro IG / FB** (text + hashtagy).
- [ ] **FR-5** Výsledky se zobrazí v **GUI** a jsou dostupné jako **JSON** (formát pro WordPress import).
- [ ] **FR-6** Pipeline po zadání požadavku: validace → AI → kontrola výstupu → uložení → **založení požadavku do fronty pro e-shop**.
- [ ] **FR-7** Stažení všech výstupů (JSON, texty, obrázky) do počítače.
- [ ] **FR-8** Logy aktivit a počet požadavků na uživatele (viditelné pro ADMINa).

### Should have

- [ ] **FR-9** Volitelné **vygenerování fotky s modelem/modelkou** (pouze při zaškrtnutí checkboxu).
- [ ] **FR-10** Možnost ručně upravit vygenerovaný text před uložením.
- [ ] **FR-11** Opakované vygenerování (regenerate) jednotlivé části.
- [ ] **FR-12** Denní / měsíční limity požadavků na uživatele.

### Nice to have

- [ ] Přímé napojení na WooCommerce REST API (místo ručního importu).
- [ ] Šablony tónu (vtipný / elegantní / stručný).
- [ ] Více jazykových verzí popisu.
- [ ] Plánování příspěvků na sociální sítě.

---

## 4. Nefunkční požadavky

| Oblast | Požadavek |
|--------|-----------|
| **Výkon FE** | Počáteční načtení < 150 kB gzip JS, lazy loading rout, komprese fotek v prohlížeči před uploadem |
| **Výkon BE** | Zpracování AI běží **asynchronně** (fronta), API odpovídá okamžitě |
| **Použitelnost** | Jednotný design, velká tlačítka, srozumitelné chyby česky, responzivita |
| **Bezpečnost** | OWASP ASVS L1 jako cíl, viz [kapitola 12](#12-bezpečnost) |
| **Auditovatelnost** | Každá akce uživatele a každé volání AI je zalogováno |
| **Přenositelnost** | Vše v Docker Compose, jedna příkazová řádka pro spuštění |
| **Udržitelnost** | Modulární struktura, testy, CI, konvenční commity |

---

## 5. Architektura

### 5.1 Kontext systému

```mermaid
flowchart TB
    subgraph Klient
        B[🌐 Prohlížeč<br/>Vue 3 SPA]
    end

    subgraph Server["Server (Docker Compose)"]
        RP[Reverse proxy<br/>Nginx / Caddy + HTTPS]
        API[Java backend<br/>Spring Boot<br/>modulární monolit]
        DB[(MongoDB)]
        FS[(Úložiště souborů<br/>volume / GridFS / S3)]
    end

    subgraph Externi["Externí služby"]
        G[Gemini API]
        O[OpenAI API<br/>volitelně]
    end

    WP[WordPress e-shop<br/>ruční import JSON]

    B -->|HTTPS| RP --> API
    API --> DB
    API --> FS
    API -->|text + obraz| G
    API -.->|alternativa| O
    API -. stažený JSON .-> WP
```

### 5.2 Modulární monolit (backend)

Jeden nasaditelný celek, uvnitř jasně oddělené moduly s **veřejným API modulu** a skrytou implementací. Doporučení: **Spring Modulith** (hlídá hranice modulů testem, podporuje události mezi moduly).

```mermaid
flowchart LR
    subgraph Backend["Backend – modulární monolit"]
        direction TB
        AUTH[auth<br/>přihlášení, JWT, role]
        USERS[users<br/>správa uživatelů]
        REQ[requests<br/>požadavky a stav]
        MEDIA[media<br/>upload, validace, úložiště]
        AIM[ai<br/>klienti Gemini/OpenAI, prompty]
        PIPE[pipeline<br/>orchestrace a worker]
        EXP[export<br/>JSON pro WordPress]
        QUEUE[shopqueue<br/>fronta pro e-shop]
        AUD[audit<br/>logy a statistiky]
        COMMON[common<br/>chyby, validace, konfigurace]
    end

    AUTH --> USERS
    REQ --> MEDIA
    REQ --> PIPE
    PIPE --> AIM
    PIPE --> EXP
    PIPE --> QUEUE
    PIPE --> MEDIA
    AUTH -.-> AUD
    REQ -.-> AUD
    AIM -.-> AUD
```

**Pravidla modulů**

- Moduly spolu komunikují jen přes veřejné rozhraní (`api` balíček) nebo **aplikační události**.
- Žádný modul nesahá do kolekcí jiného modulu přímo.
- Modul `common` neobsahuje business logiku.

### 5.3 Vrstvy uvnitř modulu

```
web (controller, DTO)  →  application (service, use-case)  →  domain (entity, pravidla)  →  infrastructure (Mongo repo, klienti)
```

---

## 6. Struktura repozitáře

Monorepo, aby se dokumentace, FE i BE verzovaly společně.

```
ai-shop-assistant/
├── README.md
├── LICENSE
├── .gitignore
├── .env.example
├── docker-compose.yml
├── docker-compose.dev.yml
├── .github/
│   ├── workflows/
│   │   ├── backend-ci.yml
│   │   ├── frontend-ci.yml
│   │   └── codeql.yml
│   ├── ISSUE_TEMPLATE/
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── dependabot.yml
├── docs/
│   ├── architecture.md
│   ├── api.md                  # export OpenAPI
│   ├── security.md
│   ├── prompts.md              # verzované prompty pro AI
│   ├── adr/                    # Architecture Decision Records
│   │   └── 0001-modularni-monolit.md
│   └── wordpress-import.md
├── backend/
│   ├── pom.xml                 # nebo build.gradle
│   ├── Dockerfile
│   └── src/
│       ├── main/java/cz/example/shopassistant/
│       │   ├── ShopAssistantApplication.java
│       │   ├── auth/
│       │   ├── users/
│       │   ├── requests/
│       │   ├── media/
│       │   ├── ai/
│       │   ├── pipeline/
│       │   ├── export/
│       │   ├── shopqueue/
│       │   ├── audit/
│       │   └── common/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── prompts/
│       └── test/java/...
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── Dockerfile
│   └── src/
│       ├── main.ts
│       ├── router/
│       ├── stores/             # Pinia
│       ├── api/                # klient k backendu
│       ├── components/         # sdílené UI komponenty
│       ├── views/
│       │   ├── LoginView.vue
│       │   ├── NewRequestView.vue
│       │   ├── RequestDetailView.vue
│       │   ├── HistoryView.vue
│       │   └── admin/
│       │       ├── ShopQueueView.vue
│       │       ├── UsersView.vue
│       │       └── LogsView.vue
│       ├── styles/             # design tokeny, globální CSS
│       └── utils/
└── scripts/
    ├── backup-mongo.sh
    └── seed-admin.sh
```

> Pozn.: na Windows / PowerShellu lze `.sh` skripty nahradit `.ps1` variantami nebo je spouštět přes WSL / Git Bash.

---

## 7. Datový model (MongoDB)

Fotky se **neukládají do dokumentů** – v databázi je jen odkaz (`storageKey`), soubor leží v úložišti.

```mermaid
erDiagram
    USERS ||--o{ REQUESTS : vytvari
    REQUESTS ||--|| MEDIA : obsahuje_vstupni_foto
    REQUESTS ||--o| RESULTS : ma_vysledek
    RESULTS ||--o{ MEDIA : generovane_obrazky
    REQUESTS ||--o| SHOP_QUEUE : zaklada
    USERS ||--o{ AUDIT_LOG : generuje
    REQUESTS ||--o{ AI_CALLS : vola

    USERS {
        ObjectId _id
        string email
        string passwordHash
        string role
        boolean enabled
        int dailyLimit
        date createdAt
        date lastLoginAt
    }
    REQUESTS {
        ObjectId _id
        ObjectId userId
        string description
        object options
        string status
        string errorMessage
        date createdAt
        date finishedAt
    }
    MEDIA {
        ObjectId _id
        ObjectId ownerId
        string storageKey
        string mimeType
        long sizeBytes
        string sha256
        string kind
    }
    RESULTS {
        ObjectId _id
        ObjectId requestId
        object webText
        object socialText
        ObjectId modelImageId
        object wooJson
        date createdAt
    }
    SHOP_QUEUE {
        ObjectId _id
        ObjectId requestId
        string status
        string note
        date createdAt
        date processedAt
    }
    AI_CALLS {
        ObjectId _id
        ObjectId requestId
        string provider
        string model
        string purpose
        int inputTokens
        int outputTokens
        long durationMs
        boolean success
    }
    AUDIT_LOG {
        ObjectId _id
        ObjectId userId
        string action
        string ip
        object details
        date timestamp
    }
```

### Kolekce a indexy

| Kolekce | Účel | Důležité indexy |
|---------|------|-----------------|
| `users` | účty | unique `email` |
| `requests` | požadavky uživatelů | `userId + createdAt`, `status` |
| `media` | metadata souborů | `ownerId`, unique `sha256 + ownerId` |
| `results` | AI výstupy | unique `requestId` |
| `shop_queue` | fronta pro e-shop | `status + createdAt` |
| `ai_calls` | spotřeba tokenů a audit AI | `requestId`, `timestamp` |
| `audit_log` | akce uživatelů | `userId + timestamp`, TTL index (např. 365 dní) |

### Stavy požadavku

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> VALIDATING
    VALIDATING --> FAILED: neplatný vstup
    VALIDATING --> PROCESSING
    PROCESSING --> FAILED: chyba AI / timeout
    PROCESSING --> REVIEW: výstup vygenerován
    REVIEW --> QUEUED_FOR_SHOP: schváleno / kontrola OK
    QUEUED_FOR_SHOP --> IMPORTED: admin potvrdil import
    FAILED --> CREATED: opakovat
    IMPORTED --> [*]
```

---

## 8. Pipeline zpracování požadavku

```mermaid
sequenceDiagram
    actor U as Uživatel
    participant FE as Vue frontend
    participant API as Backend API
    participant Q as Fronta úloh (Mongo)
    participant W as Worker (pipeline)
    participant AI as Gemini / OpenAI
    participant FS as Úložiště
    participant SQ as Fronta pro e-shop
    actor A as Admin

    U->>FE: Nahraje fotku + popis + zaškrtne volby
    FE->>FE: Zmenšení fotky, základní validace
    FE->>API: POST /api/requests (multipart)
    API->>API: Autentizace, limity, validace souboru
    API->>FS: Uloží fotku
    API->>Q: Vytvoří úlohu (CREATED)
    API-->>FE: 202 Accepted + requestId
    FE->>API: Polling / SSE stav požadavku

    W->>Q: Převezme úlohu
    W->>AI: Popis produktu z fotky + popisu
    AI-->>W: Text pro web
    W->>AI: Příspěvek IG/FB
    AI-->>W: Text pro sítě
    opt Zaškrtnuto "foto s modelem"
        W->>AI: Generování obrázku s modelem
        AI-->>W: Obrázek
        W->>FS: Uloží obrázek
    end
    W->>W: Kontrola výstupu (schéma, délka, zakázaná slova)
    W->>W: Sestaví JSON pro WordPress
    W->>SQ: Založí požadavek na vložení do e-shopu
    W->>Q: Stav REVIEW / QUEUED_FOR_SHOP
    FE->>API: Načte výsledek
    API-->>FE: Texty, náhledy, JSON
    U->>FE: Upraví / stáhne výstupy

    A->>FE: Otevře frontu e-shopu
    FE->>API: GET /api/admin/shop-queue
    A->>FE: Stáhne JSON a ručně importuje do WordPressu
    A->>API: Označí jako IMPORTED
```

**Kroky pipeline (kontrola dat)**

1. **Validace vstupu** – typ souboru (kontrola magic bytes), velikost, rozlišení, délka popisu.
2. **Předzpracování** – odstranění EXIF (včetně GPS), změna velikosti, vytvoření náhledu.
3. **AI volání** – každé s timeoutem, retry s exponenciálním backoffem, zápis do `ai_calls`.
4. **Post-validace výstupu** – JSON schéma, limity délek, kontrola jazyka, filtr nevhodného obsahu.
5. **Sestavení exportu** – mapování na strukturu produktu pro WordPress.
6. **Založení požadavku do fronty e-shopu** + uložení vše ke stažení.

---

## 9. REST API (návrh)

Prefix `/api/v1`. Dokumentace přes **OpenAPI / Swagger UI** (v produkci chráněná nebo vypnutá).

### Autentizace

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | `/auth/login` | přihlášení, vrací access token (krátká platnost) + refresh cookie |
| POST | `/auth/refresh` | obnovení tokenu |
| POST | `/auth/logout` | odhlášení, zneplatnění refresh tokenu |
| GET | `/auth/me` | informace o přihlášeném uživateli |

### Požadavky (USER)

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | `/requests` | nový požadavek (multipart: `photo`, `description`, `options`) |
| GET | `/requests` | seznam vlastních požadavků (stránkování) |
| GET | `/requests/{id}` | detail a stav |
| GET | `/requests/{id}/events` | SSE stream stavu (alternativa k pollingu) |
| GET | `/requests/{id}/result` | výsledek (texty, odkazy na obrázky) |
| PATCH | `/requests/{id}/result` | ruční úprava textů |
| POST | `/requests/{id}/regenerate` | znovu vygenerovat vybranou část |
| GET | `/requests/{id}/download` | ZIP se všemi výstupy |
| GET | `/media/{id}` | stažení obrázku (kontrola vlastníka) |

### Admin

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| GET | `/admin/shop-queue` | fronta požadavků na e-shop |
| GET | `/admin/shop-queue/{id}/export` | stažení JSON pro import |
| POST | `/admin/shop-queue/{id}/mark-imported` | označit jako importováno |
| GET | `/admin/users` | seznam uživatelů |
| POST | `/admin/users` | vytvoření uživatele |
| PATCH | `/admin/users/{id}` | změna role, limitů, zablokování |
| GET | `/admin/audit` | audit log (filtry: uživatel, akce, datum) |
| GET | `/admin/stats` | počty požadavků, spotřeba tokenů, chybovost |

### Jednotný formát chyby

```json
{
  "timestamp": "2026-11-25T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Fotka je příliš velká (max. 8 MB).",
  "traceId": "a1b2c3d4"
}
```

---

## 10. Formát JSON pro WordPress import

Předpoklad: e-shop běží na **WooCommerce**. Výstup je navržen tak, aby šel použít pro import produktů (přes REST API `POST /wp-json/wc/v3/products`, nebo po převodu na CSV importér). Přesné pole je potřeba ověřit proti verzi WooCommerce, kterou e-shop používá.

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "2026-11-25T10:20:00Z",
  "requestId": "6740f1c2a1b2c3d4e5f60789",
  "product": {
    "name": "Ručně pletená čepice – šedá",
    "type": "simple",
    "status": "draft",
    "short_description": "Teplá čepice z jemné vlny.",
    "description": "<p>Delší popis produktu ...</p>",
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
```

- `status` je vždy **`draft`** – publikuje až člověk po kontrole.
- Obrázky jsou v ZIPu vedle JSONu (relativní názvy); při importu je potřeba je nahrát do WordPress knihovny médií.

---

## 11. AI integrace

### 11.1 Poskytovatelé a abstrakce

Backend má rozhraní `AiTextClient` a `AiImageClient` s více implementacemi (Gemini, OpenAI), které se přepínají konfigurací. Díky tomu se dá snadno změnit poskytovatel nebo přidat záložní.

```mermaid
classDiagram
    class AiTextClient {
        <<interface>>
        +describeProduct(image, text) ProductText
        +writeSocialPost(product) SocialText
    }
    class AiImageClient {
        <<interface>>
        +generateModelPhoto(image, prompt) Image
    }
    class GeminiTextClient
    class OpenAiTextClient
    class GeminiImageClient
    class OpenAiImageClient
    AiTextClient <|.. GeminiTextClient
    AiTextClient <|.. OpenAiTextClient
    AiImageClient <|.. GeminiImageClient
    AiImageClient <|.. OpenAiImageClient
```

### 11.2 Free API klíče – realita

- **Google Gemini API** – má bezplatnou vrstvu přes Google AI Studio, vhodnou pro vývoj a malý provoz; limity (požadavky/min, /den, tokeny) se ale často mění, proto je **vždy ověř v aktuální dokumentaci**. Pro text i vstup z obrázku je to nejschůdnější start.
- **OpenAI API** – klasicky placené (předplatné ChatGPT API nezahrnuje). Počítej s malou platbou za použití, případně jen jako záložní poskytovatel.
- **Generování obrázků (foto s modelem)** – bývá nejdražší a nejvíce omezené; dostupnost ve free vrstvě je potřeba ověřit. Proto je to **volitelná funkce za checkboxem** a limitovaná počtem za den.
- Doporučení: začít s Gemini free tier, počítat spotřebu v `ai_calls`, nastavit denní limity na uživatele a mít přepínač poskytovatele.

### 11.3 Prompty

- Uložené v `backend/src/main/resources/prompts/` a verzované v gitu (`docs/prompts.md` vysvětluje účel).
- Strukturovaný výstup: AI musí vracet **JSON podle schématu**; výstup se vždy validuje.
- Fotka obsahuje potenciálně nedůvěryhodný text (např. nápis na obrázku) → **ochrana proti prompt injection** (viz bezpečnost).

### 11.4 Foto s modelem – etika a kvalita

- Generovaný obrázek označit v UI jako „vytvořeno AI“.
- U e-shopu zvážit transparentní označení, že fotka s modelem je ilustrační a nemusí přesně odpovídat realitě (střih, velikost, barva).
- Vždy ukázat uživateli náhled a nechat ho schválit.

---

## 12. Bezpečnost

Bezpečnost je v zadání prioritou, proto ji řešíme od začátku.

### 12.1 Model hrozeb (zjednodušený)

```mermaid
flowchart TB
    A[Útočník] --> T1[Brute-force / credential stuffing]
    A --> T2[Injection - NoSQL / XSS]
    A --> T3[Škodlivý upload]
    A --> T4[Zneužití AI - prompt injection, vyčerpání tokenů]
    A --> T5[Únik API klíčů a secretů]
    A --> T6[Přístup k cizím datům - IDOR]
    T1 --> M1[Rate limit, silná hesla, lockout, refresh rotace]
    T2 --> M2[Validace, parametrizované dotazy, CSP, escapování]
    T3 --> M3[Kontrola magic bytes, limity, re-enkódování, mimo web root]
    T4 --> M4[Strukturovaný výstup, sanitizace, limity na uživatele]
    T5 --> M5[Env proměnné / secrets, nikdy do gitu, skenování]
    T6 --> M6[Kontrola vlastníka na každém endpointu]
```

### 12.2 Konkrétní opatření

**Autentizace a autorizace**
- Hesla: **Argon2id** nebo bcrypt (cost ≥ 12), nikdy v čistém textu.
- JWT s krátkou platností (např. 15 min) + **refresh token v HttpOnly, Secure, SameSite cookie** s rotací.
- Spring Security, metodová autorizace (`@PreAuthorize`), role USER / ADMIN.
- Každý endpoint kontroluje **vlastnictví zdroje** (ochrana před IDOR).
- Rate limiting na `/auth/login` a na AI endpointy (např. Bucket4j), zpomalení / dočasný zámek po opakovaných chybách.
- Volitelně 2FA (TOTP) pro ADMINa.

**Injection (SQLi / NoSQLi)**
- Aplikace používá MongoDB, takže „klasické“ SQL injection se netýká, ale hrozí **NoSQL injection** (operátory typu `$ne`, `$gt` v JSON vstupu).
- Používat **Spring Data repozitáře a typované DTO**, nikdy skládat dotazy z řetězců ani předávat surový JSON z requestu do `Query`.
- Validovat vstupy přes Bean Validation (`@NotBlank`, `@Size`, `@Pattern`).
- Vypnout parsování `$` operátorů ve vstupních objektech, odmítat klíče začínající `$` a obsahující `.`.
- Pokud by se někdy přidala SQL databáze: pouze **prepared statements**.

**XSS / CSRF / hlavičky**
- Vue escapuje výstup; **nepoužívat `v-html`** pro AI texty (nebo jen přes sanitizér, např. DOMPurify).
- **CSP**, `X-Content-Type-Options`, `X-Frame-Options`/`frame-ancestors`, `Referrer-Policy`, HSTS.
- CSRF: při cookie-based refresh použít SameSite + CSRF token nebo double-submit cookie.
- CORS: povolit jen vlastní doménu.

**Uploady**
- Povolit jen JPEG / PNG / WebP, kontrola **magic bytes**, ne jen přípony.
- Limit velikosti a rozměrů, ochrana proti „decompression bomb“.
- **Re-enkódování** obrázku (zahodí škodlivý obsah) a **odstranění EXIF**.
- Ukládat pod **náhodným jménem** mimo veřejný adresář; nikdy nepoužít původní jméno souboru v cestě.
- Stahování souborů jen přes autorizovaný endpoint.

**AI specifická rizika**
- **Prompt injection**: text z obrázku či popisu je data, ne instrukce; systémový prompt jasně odděluje vstup; výstup se validuje podle schématu.
- AI výstup se nikdy nespouští ani nevkládá jako HTML bez sanitizace.
- Limity tokenů a požadavků na uživatele (ochrana před vyčerpáním kvóty i peněz).
- Neposílat do AI osobní údaje navíc, než je nutné (EXIF pryč).

**Secrets a provoz**
- API klíče **jen v env proměnných / Docker secrets**, `.env` v `.gitignore`, v repu pouze `.env.example`.
- GitHub **secret scanning** + push protection, Dependabot, CodeQL.
- Pravidelné aktualizace závislostí, `mvn dependency-check` / OWASP Dependency-Check, `npm audit`.
- HTTPS všude (Let’s Encrypt přes Caddy / Nginx), Mongo není vystavené do internetu, zapnutá autentizace Mongo.
- Zálohy databáze a úložiště, otestované obnovení.
- Minimální oprávnění kontejnerů (non-root uživatel).

**GDPR a soukromí**
- Uchovávat jen nutné údaje, definovat dobu uchování (TTL na logy), možnost smazat účet a data.
- Informovat uživatele o tom, že fotky se zpracovávají externí AI službou.

### 12.3 Bezpečnostní checklist před spuštěním

- [ ] Všechny endpointy mají autorizaci a kontrolu vlastníka
- [ ] Rate limiting zapnutý
- [ ] Upload validace + EXIF stripping
- [ ] Žádné secrety v gitu (ověřeno skenerem)
- [ ] CSP a bezpečnostní hlavičky
- [ ] Swagger UI v produkci vypnuté nebo chráněné
- [ ] Zálohy + test obnovy
- [ ] Penetrační test základních scénářů (OWASP ZAP)

---

## 13. Frontend / UX

### 13.1 Technologie

- **Vue 3** (Composition API, `<script setup>`), **TypeScript**, **Vite**
- **Pinia** (stav), **Vue Router** (lazy loading rout)
- UI knihovna: doporučení **PrimeVue** nebo **Naive UI** (lehčí) / alternativně vlastní CSS s design tokeny; **vyhnout se těžkým knihovnám**
- **VueUse**, **Axios / ofetch**, **vee-validate / zod** pro formuláře

### 13.2 Obrazovky

```mermaid
flowchart LR
    L[Přihlášení] --> D[Přehled / Historie]
    D --> N[Nový požadavek]
    N --> S[Stav zpracování]
    S --> R[Detail výsledku<br/>texty, náhledy, stažení]
    D --> R
    D -.admin.-> Q[Fronta e-shopu]
    D -.admin.-> LG[Logy a statistiky]
    D -.admin.-> US[Uživatelé]
```

### 13.3 Zásady GUI

- **Jednotný design** – design tokeny (barvy, mezery, typografie) v jednom souboru, sdílené komponenty (`AppButton`, `AppCard`, `AppUpload`, `StatusBadge`).
- **Intuitivní** – jeden hlavní krok na obrazovku, velké ovládací prvky, srozumitelné popisky v češtině, žádné zbytečné volby.
- **Zpětná vazba** – průběh zpracování (kroky: „Nahrávám → Píšu popis → Tvořím příspěvek → Hotovo“), jasné chybové hlášky.
- **Přístupnost** – kontrast, klávesnice, `aria` atributy, čitelná velikost písma.
- **Světlý a tmavý režim** (nice to have).

### 13.4 Odlehčené načítání (slabší notebook)

- Lazy loading rout a těžkých komponent, code splitting.
- **Komprese a zmenšení fotky v prohlížeči** před uploadem (Canvas API).
- Náhledy (thumbnails) generované backendem, `loading="lazy"`.
- Stránkování místo načítání všeho; žádné velké animace.
- Cíl: JS bundle úvodní stránky pod ~150 kB gzip, kontrola přes Lighthouse.
- Stav zpracování přes SSE / lehký polling (např. 3 s s postupným prodlužováním).

---

## 14. Logování, audit a limity

**Co se loguje (kolekce `audit_log`)**

| Akce | Příklad |
|------|---------|
| `LOGIN_SUCCESS` / `LOGIN_FAILED` | čas, IP, user agent |
| `REQUEST_CREATED` | requestId, zvolené možnosti |
| `REQUEST_COMPLETED` / `REQUEST_FAILED` | doba, chyba |
| `RESULT_EDITED` / `RESULT_DOWNLOADED` | kdo a co |
| `AI_CALL` | poskytovatel, model, tokeny, trvání |
| `ADMIN_ACTION` | změna uživatele, označení importu |

**Přehled pro ADMINa**
- Počet požadavků na uživatele (den / týden / měsíc).
- Seznam posledních akcí uživatele (co dělal).
- Spotřeba tokenů a odhad nákladů, chybovost pipeline.
- Export logů do CSV.

**Limity**
- Denní limit požadavků na uživatele (výchozí např. 20).
- Samostatný, přísnější limit na **generování fotek s modelem**.
- Při překročení srozumitelná hláška v GUI.

**Technicky**
- Strukturované JSON logy (Logback) s `traceId`, bez hesel, tokenů a osobních údajů.
- Actuator (`/actuator/health`) jen interně.

---

## 15. Lokální spuštění

### Požadavky

- Docker Desktop (Windows / macOS / Linux)
- JDK 21 a Maven / Gradle (pro vývoj mimo Docker)
- Node.js 20+ a npm / pnpm (pro vývoj FE)
- Git

### Rychlý start (Docker Compose)

```bash
git clone https://github.com/<uzivatel>/ai-shop-assistant.git
cd ai-shop-assistant
cp .env.example .env          # v PowerShellu: Copy-Item .env.example .env
# vyplň GEMINI_API_KEY, JWT_SECRET, MONGO_PASSWORD ...
docker compose up --build
```

- Frontend: http://localhost:5173 (dev) / http://localhost (prod)
- Backend: http://localhost:8080
- Swagger UI (jen dev): http://localhost:8080/swagger-ui.html

### Vývoj bez Dockeru pro aplikace

```bash
# Mongo v Dockeru
docker compose up mongo -d

# Backend
cd backend
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# Frontend
cd frontend
npm install
npm run dev
```

### Vytvoření prvního admina

```bash
# spustí se jen při prázdné databázi, heslo se vypíše / zadá jednou
docker compose exec backend java -jar app.jar --seed-admin
```

---

## 16. Konfigurace (env proměnné)

`.env.example`:

```dotenv
# --- Obecné ---
SPRING_PROFILES_ACTIVE=dev
APP_BASE_URL=http://localhost:5173

# --- MongoDB ---
MONGO_URI=mongodb://app:CHANGE_ME@mongo:27017/shopassistant?authSource=admin
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=CHANGE_ME

# --- Bezpečnost ---
JWT_SECRET=CHANGE_ME_LONG_RANDOM_256_BIT
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=14
CORS_ALLOWED_ORIGINS=http://localhost:5173

# --- Úložiště souborů ---
STORAGE_TYPE=local            # local | gridfs | s3
STORAGE_LOCAL_PATH=/data/uploads
UPLOAD_MAX_MB=8

# --- AI ---
AI_PROVIDER_TEXT=gemini       # gemini | openai
AI_PROVIDER_IMAGE=gemini
GEMINI_API_KEY=
OPENAI_API_KEY=
AI_REQUEST_TIMEOUT_SECONDS=60

# --- Limity ---
LIMIT_REQUESTS_PER_DAY=20
LIMIT_MODEL_PHOTOS_PER_DAY=3
```

> ⚠️ Soubor `.env` **nikdy necommitovat**.

---

## 17. Git workflow a CI/CD

### 17.1 Větvení

- `main` – vždy funkční, chráněná větev (PR + zelené CI).
- `develop` – volitelně integrační větev.
- `feature/<popis>`, `fix/<popis>`, `docs/<popis>`, `chore/<popis>`.

```mermaid
gitGraph
    commit id: "init"
    branch develop
    commit id: "skeleton"
    branch feature/auth
    commit id: "login"
    commit id: "jwt"
    checkout develop
    merge feature/auth
    branch feature/upload
    commit id: "upload"
    checkout develop
    merge feature/upload
    checkout main
    merge develop tag: "v0.1.0"
```

### 17.2 Commity

Pravidelně a po malých krocích, ideálně alespoň jednou denně při práci na projektu. Formát **Conventional Commits**:

```
feat(auth): přidáno přihlášení přes JWT
fix(upload): oprava kontroly velikosti souboru
docs(readme): doplněna kapitola o bezpečnosti
test(pipeline): testy validace výstupu AI
chore(deps): aktualizace Spring Boot
```

### 17.3 GitHub nastavení (checklist)

- [ ] Chráněná větev `main` (PR vyžadovány, povinná CI)
- [ ] **Secret scanning** + push protection
- [ ] **Dependabot** (Maven / npm / GitHub Actions)
- [ ] **CodeQL** skenování (Java, JavaScript)
- [ ] Šablony issue a pull requestů
- [ ] `CODEOWNERS`, `SECURITY.md`, `CONTRIBUTING.md`
- [ ] Verzování přes tagy (`v0.1.0`, …) a `CHANGELOG.md`
- [ ] GitHub Projects / Issues jako kanban pro roadmapu

### 17.4 CI pipeline

```mermaid
flowchart LR
    P[Push / Pull Request] --> L[Lint + formát]
    L --> T[Unit + integrační testy<br/>Testcontainers Mongo]
    T --> S[Bezpečnostní skeny<br/>CodeQL, dependency-check, npm audit]
    S --> B[Build Docker images]
    B --> R{main?}
    R -- ano --> REL[Publikace image / nasazení]
    R -- ne --> END[Hotovo]
```

---

## 18. Testování

| Úroveň | Nástroje | Co testovat |
|--------|----------|-------------|
| Unit BE | JUnit 5, Mockito | služby, validace, mapování na JSON |
| Integrační BE | Spring Boot Test, **Testcontainers (MongoDB)** | repozitáře, API, bezpečnost endpointů |
| Moduly | **Spring Modulith** testy | dodržení hranic modulů |
| AI klienti | WireMock / mock klienti | parsování odpovědí, timeouty, retry |
| Unit FE | Vitest, Vue Test Utils | komponenty, store |
| E2E | Playwright | přihlášení → nový požadavek → výsledek → stažení |
| Bezpečnost | OWASP ZAP, vlastní testy IDOR / injection | autorizace, `$ne` payloady, upload škodlivých souborů |
| Výkon FE | Lighthouse CI | velikost bundlu, rychlost načtení |

Důležité bezpečnostní testy: přístup k cizímu požadavku → 403/404, login s payloadem `{"email":{"$ne":null}}` → odmítnout, upload `.php` přejmenovaného na `.jpg` → odmítnout, překročení limitů → 429.

---

## 19. Roadmapa

```mermaid
gantt
    title Plán vývoje
    dateFormat  YYYY-MM-DD
    axisFormat  %d.%m.
    section Základ
    Repo, README, CI, Docker Compose        :a1, 2026-12-01, 5d
    Backend skeleton + moduly + Mongo       :a2, after a1, 7d
    Autentizace a role                      :a3, after a2, 7d
    section MVP
    Upload a správa médií                   :b1, after a3, 5d
    AI klient (Gemini) + prompty            :b2, after b1, 7d
    Pipeline + fronta + stavy               :b3, after b2, 7d
    Frontend: login, nový požadavek, detail :b4, after a3, 14d
    Export JSON pro WordPress               :b5, after b3, 4d
    section Rozšíření
    Fronta e-shopu a admin sekce            :c1, after b5, 7d
    Audit, logy, statistiky, limity         :c2, after c1, 5d
    Foto s modelem (volitelné)              :c3, after c2, 7d
    section Dokončení
    Bezpečnostní audit a testy              :d1, after c3, 7d
    Nasazení na server + zálohy             :d2, after d1, 4d
```

### Milníky

| Verze | Obsah |
|-------|-------|
| **v0.1** | Kostra projektu, CI, přihlášení, základní UI |
| **v0.2** | Upload + AI popis produktu |
| **v0.3** | Pipeline, fronta, export JSON, příspěvek IG/FB |
| **v0.4** | Admin: fronta e-shopu, logy, limity |
| **v0.5** | Foto s modelem (volitelné) |
| **v1.0** | Bezpečnostní audit, nasazení, dokumentace pro uživatele |

---

## 20. Rizika a otevřené otázky

| Riziko / otázka | Dopad | Řešení |
|-----------------|-------|--------|
| Změna limitů / podmínek free API | AI přestane fungovat nebo zdraží | Abstrakce poskytovatelů, limity, monitoring spotřeby |
| Kvalita generovaných fotek s modelem | Nerealistické, zavádějící | Volitelná funkce, náhled, schválení člověkem, označení „AI“ |
| Prompt injection přes obrázek / popis | Nežádoucí výstup | Validace schématu, sanitizace, oddělení instrukcí a dat |
| Zneužití účtu | Vyčerpání kvóty | Rate limit, denní limity, 2FA pro admina |
| Slabší notebook uživatele | Pomalé GUI | Lehký FE, lazy loading, komprese fotek |
| Přesný formát importu do WordPressu | Nutné úpravy JSON | Ověřit WooCommerce verzi, případně doplnit konverzi na CSV |
| Hosting a náklady serveru | Nasazení | Malý VPS / domácí server + Docker Compose, zálohy |
| Autorská práva a označení AI obsahu | Právní rizika | Označení AI obsahu, vlastní fotky uživatele |

**Otevřené otázky k rozhodnutí**

1. Jaká verze WordPressu / WooCommerce a jaký importní plugin se bude používat?
2. Kde bude aplikace hostovaná (VPS, domácí server, cloud)?
3. Jak se mají fotky ukládat (lokální volume vs. GridFS vs. S3)?
4. Má se výsledek před zařazením do fronty e-shopu vždy schvalovat uživatelem?
5. Kolik uživatelů se očekává (jen maminka, nebo více)?
6. Jaký jazykový tón mají texty mít (osobní, elegantní, vtipný)?

---

## 📄 Licence

Doporučení: pro osobní / komerční projekt vybrat licenci podle záměru (MIT pro open source, nebo „All rights reserved“ pokud kód nemá být veřejně použitelný).

## 🤝 Přispívání

Viz `CONTRIBUTING.md` (bude doplněno) – Conventional Commits, PR do `main`, povinná zelená CI.
