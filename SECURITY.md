# Bezpečnostní politika

## Podporované verze

| Verze | Podpora |
|-------|---------|
| 0.1.x | ano |
| starší | ne |

## Hlášení zranitelnosti

Prosím **nezakládejte veřejné issue** pro bezpečnostní problémy.

Napište na **security@example.com** (placeholder – před nasazením nahraďte)
a uveďte:

- popis zranitelnosti a její dopad,
- kroky k reprodukci nebo proof of concept,
- verzi aplikace a prostředí,
- případný návrh opravy.

Potvrzení přijetí do 3 pracovních dnů, první vyjádření do 10 pracovních dnů.
Po opravě zveřejníme poděkování, pokud si to budete přát.

## Rozsah

Vítáme zejména hlášení těchto problémů:

- autentizace a autorizace, IDOR (přístup k cizím datům),
- NoSQL injection a XSS,
- škodlivé uploady a obcházení validace,
- únik tajemství nebo osobních údajů,
- prompt injection a zneužití AI endpointů,
- chyby v konfiguraci Dockeru a reverzní proxy.

## Co nedělat

- Netestujte na produkčních datech a cizích účtech.
- Neprovádějte DoS útoky ani hromadné skenování.
- Nezveřejňujte zranitelnost před opravou.

## Bezpečnostní opatření

Přehled opatření a model hrozeb najdete v [docs/security.md](docs/security.md).
