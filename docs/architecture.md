# Architektura

## Přehled

AI Shop Assistant je **modulární monolit**: jedno nasaditelné jádro (Spring Boot),
uvnitř jasně oddělené moduly. Frontend je samostatná Vue 3 SPA, která komunikuje
s backendem přes REST API. Data jsou v MongoDB, soubory v konfigurovatelném úložišti.

~~~mermaid
flowchart TB
    subgraph Klient
        B[Prohlížeč - Vue 3 SPA]
    end

    subgraph Server[Docker Compose]
        RP[Caddy - HTTPS a reverzní proxy]
        API[Spring Boot - modulární monolit]
        DB[(MongoDB)]
        FS[(Úložiště souborů)]
    end

    subgraph Externi[Externí služby]
        G[Gemini API]
        O[OpenAI API]
    end

    B -->|HTTPS| RP
    RP -->|/api| API
    RP -->|ostatní| B
    API --> DB
    API --> FS
    API --> G
    API -. volitelně .-> O
~~~

## Moduly backendu

| Modul | Odpovědnost | Veřejné rozhraní |
|-------|-------------|------------------|
| <code>auth</code> | přihlášení, JWT, refresh tokeny, role | <code>AuthApi</code> |
| <code>users</code> | účty, role, limity | <code>UsersApi</code> |
| <code>requests</code> | požadavky uživatele a jejich stav | <code>RequestsApi</code> |
| <code>media</code> | upload, validace, úložiště, náhledy | <code>MediaApi</code> |
| <code>ai</code> | klienti Gemini/OpenAI, prompty, validace výstupu | <code>AiApi</code> |
| <code>pipeline</code> | orchestrace kroků a worker | – |
| <code>export</code> | mapování na WooCommerce JSON a ZIP | <code>ExportApi</code> |
| <code>shopqueue</code> | fronta požadavků pro e-shop | <code>ShopQueueApi</code> |
| <code>audit</code> | auditní log, statistiky | <code>AuditApi</code> |
| <code>common</code> | konfigurace, chyby, validace, utility | – |

### Pravidla

1. Moduly komunikují pouze přes veřejné rozhraní (<code>*Api</code>) nebo aplikační události.
2. Žádný modul nesahá přímo do kolekcí jiného modulu.
3. <code>common</code> neobsahuje business logiku.
4. Hranice hlídá <code>ModularityTests</code> (Spring Modulith).

## Vrstvy v modulu

~~~text
web (controller, DTO)
  -> application (service, use-case)
  -> domain (entity, pravidla)
  -> infrastructure (repository, klienti, úložiště)
~~~

Závislosti směřují dovnitř. Doména nezná Spring ani MongoDB.

## Tok zpracování požadavku

~~~mermaid
sequenceDiagram
    actor U as Uživatel
    participant FE as Vue frontend
    participant API as Backend
    participant W as Worker
    participant AI as AI služba
    participant DB as MongoDB
    participant FS as Úložiště

    U->>FE: fotka + popis + volby
    FE->>FE: komprese fotky v prohlížeči
    FE->>API: POST /api/v1/requests
    API->>API: auth, limity, validace
    API->>FS: uložení fotky
    API->>DB: požadavek (CREATED)
    API-->>FE: 202 Accepted + requestId
    FE->>API: polling nebo SSE stavu

    W->>DB: převzetí úlohy
    W->>AI: popis produktu
    W->>AI: příspěvek na sítě
    opt foto s modelem
        W->>AI: generování obrázku
        W->>FS: uložení obrázku
    end
    W->>W: validace výstupu
    W->>DB: výsledek + fronta e-shopu
    FE->>API: GET výsledek
    API-->>FE: texty, náhledy, JSON
~~~

## Stavy požadavku

~~~mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> VALIDATING
    VALIDATING --> FAILED: neplatný vstup
    VALIDATING --> PROCESSING
    PROCESSING --> FAILED: chyba AI nebo timeout
    PROCESSING --> REVIEW: výstup připraven
    REVIEW --> QUEUED_FOR_SHOP: zařazeno do fronty
    QUEUED_FOR_SHOP --> IMPORTED: admin potvrdil import
    FAILED --> CREATED: opakovat
    IMPORTED --> [*]
~~~

## Data

- Fotky se neukládají do dokumentů, pouze <code>storageKey</code>.
- Kolekce: <code>users</code>, <code>requests</code>, <code>media</code>, <code>results</code>,
  <code>shop_queue</code>, <code>ai_calls</code>, <code>audit_log</code>.
- Indexy vytváří inicializační skript <code>infra/mongo/init/01-init.js</code>.
- Podrobný model je v README, kapitola 7.

## Rozhodnutí

- [ADR-0001](adr/0001-modularni-monolit.md) – modulární monolit.
- [ADR-0002](adr/0002-mongodb.md) – MongoDB.
- [ADR-0003](adr/0003-ai-poskytovatel.md) – abstrakce AI poskytovatele.
- [ADR-0004](adr/0004-uloziste-souboru.md) – úložiště souborů.

## Odkazy

- [Bezpečnost](security.md)
- [API](api.md)
- [Nasazení](deployment.md)
- [Prompty](prompts.md)
