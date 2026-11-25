# Bezpečnost

Tento dokument rozvádí kapitolu 12 v README. Cílem je OWASP ASVS Level 1
a bezpečný provoz pro malý e-shop.

## Aktiva

- účty a přihlašovací údaje,
- nahrané fotky a vygenerované texty,
- API klíče (Gemini, OpenAI),
- JWT secret a přístupy k MongoDB,
- auditní logy.

## Model hrozeb

| Hrozba | Dopad | Opatření |
|--------|-------|----------|
| Brute-force a credential stuffing | převzetí účtu | rate limit, lockout, silná hesla, refresh rotace |
| NoSQL injection | únik nebo změna dat | typované DTO, zákaz <code>$</code> operátorů, Bean Validation |
| XSS | krádež session, škodlivý obsah | escapování Vue, zákaz <code>v-html</code>, CSP |
| Škodlivý upload | RCE, malware | magic bytes, limity, re-enkódování, mimo web root |
| Prompt injection | nežádoucí výstup, zneužití kvóty | oddělení dat a instrukcí, JSON schéma, limity |
| IDOR | přístup k cizím datům | kontrola vlastníka na každém endpointu |
| Únik tajemství | zneužití API a dat | env proměnné, secret scanning, žádné klíče v gitu |
| Vyčerpání tokenů | náklady | denní limity, timeouty, monitoring spotřeby |

## Autentizace a autorizace

- Hesla hashovaná **Argon2id** (nebo bcrypt s cost 12+).
- Access token JWT s platností 15 minut.
- Refresh token v <code>HttpOnly</code>, <code>Secure</code>, <code>SameSite=Strict</code>
  cookie, s rotací při každém obnovení.
- Role <code>USER</code> a <code>ADMIN</code>, metodová autorizace.
- Každý endpoint ověřuje vlastnictví zdroje (IDOR).
- Rate limiting na <code>/auth/login</code> a AI endpointy.
- Volitelně TOTP 2FA pro admina.

## Vstupy a NoSQL injection

- Veškeré vstupy přes DTO s Bean Validation.
- Nikdy neskládat dotazy z řetězců a nepředávat surový JSON do <code>Query</code>.
- Odmítat klíče začínající <code>$</code> a klíče obsahující tečku.
- Globální filtr požadavků kontroluje i vnořené objekty a pole.
- Číselné a datumové vstupy parsovat přes typované konvertory.

## Uploady

- Povolené typy: JPEG, PNG, WebP – kontrola magic bytes, ne přípony.
- Maximální velikost 8 MB a maximální rozlišení (ochrana proti decompression bomb).
- Obrázek se znovu zakóduje, tím se odstraní EXIF (včetně GPS) a případný škodlivý obsah.
- Ukládat pod náhodným názvem mimo veřejný adresář.
- Stahování pouze přes autorizovaný endpoint s kontrolou vlastníka.
- Náhledy generovat na backendu, ne z původního souboru.

## AI specifická rizika

- Text z fotky i popis uživatele jsou **data**, ne instrukce. Systémový prompt to výslovně uvádí.
- Výstup se validuje proti JSON schématu, kontrolují se délky a zakázaný obsah.
- AI výstup se nikdy nevykresluje jako HTML bez sanitizace.
- Každé volání se zapisuje do <code>ai_calls</code> (provider, model, tokeny, trvání, úspěch).
- Denní limity na požadavky a zvlášť na generování fotek s modelem.
- Do AI se neposílají zbytečné osobní údaje; EXIF je odstraněn před voláním.

## Hlavičky a CSP

Produkční Nginx nastavuje:

- <code>Content-Security-Policy</code> s <code>default-src 'self'</code>,
- <code>X-Content-Type-Options: nosniff</code>,
- <code>Referrer-Policy: strict-origin-when-cross-origin</code>,
- <code>Permissions-Policy</code> bez kamery, mikrofonu a geolokace,
- HSTS na úrovni Caddy.

CORS povoluje pouze vlastní doménu z <code>CORS_ALLOWED_ORIGINS</code>.

## Tajemství

- Pouze v <code>.env</code> nebo Docker secrets, <code>.env</code> je v <code>.gitignore</code>.
- V repu je jen <code>.env.example</code> s placeholdery.
- GitHub secret scanning a push protection.
- Klíče pravidelně rotovat, zejména po jakémkoli podezření na únik.

## Logování a soukromí

- Strukturované logy s <code>traceId</code>, bez hesel, tokenů a osobních údajů.
- Auditní log má TTL 365 dní.
- Uživatel má právo na výmaz účtu a dat.
- V UI je informace, že fotky zpracovává externí AI služba.

## Checklist před nasazením

- [ ] Všechny endpointy mají autorizaci a kontrolu vlastníka.
- [ ] Rate limiting je zapnutý a otestovaný.
- [ ] Upload validace a stripping EXIF fungují.
- [ ] Žádné secrety v gitu (secret scanning prošel).
- [ ] CSP a bezpečnostní hlavičky jsou aktivní.
- [ ] Swagger UI je v produkci vypnuté.
- [ ] Zálohy běží a obnova byla otestovaná.
- [ ] OWASP ZAP základní sken bez kritických nálezů.
- [ ] Dependency scanning (Dependabot, CodeQL) bez kritických nálezů.

## Incident

1. Zneplatnit napadené účty a tokeny (rotace <code>JWT_SECRET</code>).
2. Zablokovat přístup k API klíčům a rotovat je.
3. Zjistit rozsah z auditního logu.
4. Obnovit data ze zálohy, pokud byla změněna.
5. Zdokumentovat incident a doplnit opatření.
