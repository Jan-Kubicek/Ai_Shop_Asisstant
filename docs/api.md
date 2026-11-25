# API

Základní cesta: <code>/api/v1</code>. Komunikace je JSON, výjimkou je upload
(<code>multipart/form-data</code>) a stahování souborů.

## Autentizace

1. <code>POST /auth/login</code> vrátí access token a nastaví refresh cookie.
2. Access token se posílá v hlavičce <code>Authorization: Bearer &lt;token&gt;</code>.
3. Po expiraci klient zavolá <code>POST /auth/refresh</code> a dostane nový token.
4. <code>POST /auth/logout</code> zneplatní refresh token.

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | <code>/auth/login</code> | přihlášení |
| POST | <code>/auth/refresh</code> | obnovení access tokenu |
| POST | <code>/auth/logout</code> | odhlášení |
| GET | <code>/auth/me</code> | přihlášený uživatel |

Přihlášení je omezeno rate limitorem: výchozí 10 pokusů za 5 minut z jedné
IP adresy a e-mailu. Po překročení vrací <code>429 RATE_LIMITED</code>.

## Požadavky (role USER)

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| POST | <code>/requests</code> | nový požadavek (multipart: photo, description, options) |
| GET | <code>/requests</code> | seznam vlastních požadavků, stránkování |
| GET | <code>/requests/{id}</code> | detail a stav |
| GET | <code>/requests/{id}/events</code> | SSE stream stavu |
| GET | <code>/requests/{id}/result</code> | výsledek |
| PATCH | <code>/requests/{id}/result</code> | ruční úprava textů |
| POST | <code>/requests/{id}/regenerate</code> | znovu vygenerovat část |
| GET | <code>/requests/{id}/download</code> | ZIP se všemi výstupy |
| GET | <code>/media/{id}</code> | obrázek s kontrolou vlastníka |

### Vytvoření požadavku

~~~http
POST /api/v1/requests
Content-Type: multipart/form-data
Authorization: Bearer <token>

photo=<soubor>
description=Ručně pletená čepice, šedá, 100% vlna
options={"generateWebText":true,"generateSocialPost":true,"generateModelPhoto":false}
~~~

Odpověď <code>202 Accepted</code>:

~~~json
{
  "id": "6740f1c2a1b2c3d4e5f60789",
  "status": "CREATED",
  "createdAt": "2026-11-25T10:15:30Z"
}
~~~

### SSE

<code>GET /requests/{id}/events</code> posílá události <code>status</code> s JSON
obsahujícím aktuální stav. Klient může použít i polling <code>GET /requests/{id}</code>.

Stream vyžaduje <code>Authorization: Bearer</code> (proto frontend čte SSE přes
<code>fetch</code> a ne přes <code>EventSource</code>). První událost je aktuální
stav, další se odesílají při každé změně dokud požadavek nedosáhne koncového
stavu.

## Admin

| Metoda | Endpoint | Popis |
|--------|----------|-------|
| GET | <code>/admin/shop-queue</code> | fronta pro e-shop |
| GET | <code>/admin/shop-queue/{id}/export</code> | JSON pro import |
| POST | <code>/admin/shop-queue/{id}/mark-imported</code> | označit jako importované |
| GET | <code>/admin/users</code> | seznam uživatelů |
| POST | <code>/admin/users</code> | vytvoření uživatele |
| PATCH | <code>/admin/users/{id}</code> | role, limity, zablokování |
| GET | <code>/admin/audit</code> | auditní log s filtry |
| GET | <code>/admin/stats</code> | statistiky a spotřeba |

## Chybový formát

Všechny chyby vrací stejnou strukturu:

~~~json
{
  "timestamp": "2026-11-25T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Fotka je příliš velká (max. 8 MB).",
  "traceId": "a1b2c3d4"
}
~~~

| HTTP | Kód | Význam |
|------|-----|--------|
| 400 | <code>VALIDATION_ERROR</code> | neplatný vstup |
| 401 | <code>UNAUTHORIZED</code> | chybějící nebo neplatný token |
| 403 | <code>FORBIDDEN</code> | nedostatečná role nebo cizí zdroj |
| 404 | <code>NOT_FOUND</code> | zdroj neexistuje |
| 409 | <code>CONFLICT</code> | kolize stavu |
| 413 | <code>PAYLOAD_TOO_LARGE</code> | příliš velký soubor |
| 429 | <code>RATE_LIMITED</code> | překročen limit |
| 500 | <code>INTERNAL_ERROR</code> | neočekávaná chyba |

## Stránkování

Parametry <code>page</code> (od 0) a <code>size</code> (výchozí 20, max 100).

~~~json
{
  "items": [],
  "total": 0,
  "page": 0,
  "size": 20
}
~~~

## OpenAPI

Swagger UI je dostupné v dev profilu na <code>/swagger-ui.html</code>,
JSON na <code>/v3/api-docs</code>. V produkci je vypnuté.

Export dokumentace:

~~~bash
curl -s http://localhost:8080/v3/api-docs > docs/openapi.json
~~~

Při změně API aktualizujte tento dokument a vyexportované schéma.
