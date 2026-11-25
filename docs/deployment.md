# Nasazení

## Požadavky na server

- Linux VPS nebo domácí server, 2 vCPU, 4 GB RAM, 20 GB disku (minimum).
- Docker Engine a Docker Compose v2.
- Veřejná IP a doména s DNS záznamem typu A na server.
- Otevřené porty 80 a 443.

## První nasazení

### 1. Příprava serveru

~~~bash
sudo apt update
sudo apt install -y git
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
~~~

Po přihlášení znovu se ověřte, že <code>docker compose version</code> funguje.

### 2. Klonování a konfigurace

~~~bash
git clone https://github.com/<uzivatel>/ai-shop-assistant.git
cd ai-shop-assistant
cp .env.example .env
nano .env
~~~

V <code>.env</code> nastavte zejména:

- <code>APP_DOMAIN</code> – doména bez <code>https://</code>,
- <code>ACME_EMAIL</code> – e-mail pro Let's Encrypt,
- <code>JWT_SECRET</code> – náhodný řetězec alespoň 32 znaků,
- <code>MONGO_INITDB_ROOT_PASSWORD</code> a <code>MONGO_APP_PASSWORD</code>,
- <code>GEMINI_API_KEY</code>, případně <code>OPENAI_API_KEY</code>.

Hesla generujte například přes <code>openssl rand -base64 48</code>.
Soubor <code>.env</code> nikdy necommitujte.

### 3. Práva k datům

Kontejner backendu běží jako neprivilegovaný uživatel. Na serveru připravte adresáře:

~~~bash
mkdir -p data/mongo data/uploads
sudo chown -R 1000:1000 data
~~~

Pokud se uploady neukládají, zkontrolujte práva a vlastníka <code>data/uploads</code>.

### 4. Spuštění

~~~bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
~~~

Caddy automaticky získá HTTPS certifikát. Aplikace běží na <code>https://&lt;APP_DOMAIN&gt;</code>.

### 5. První admin

~~~bash
docker compose exec backend java -jar /app/app.jar --seed-admin
~~~

Heslo zadávejte interaktivně, nikdy ho neposílejte v příkazové řádce.
Funkce musí být v backendu implementovaná jako <code>CommandLineRunner</code>
nebo <code>ApplicationRunner</code>, který se spustí pouze při prázdné kolekci uživatelů.

## Aktualizace

~~~bash
git pull
docker compose build
docker compose up -d
docker image prune -f
~~~

Před aktualizací vždy vytvořte zálohu:

~~~bash
bash scripts/backup-mongo.sh
~~~

## Zálohy

- Skript: <code>scripts/backup-mongo.sh</code> nebo <code>.ps1</code>.
- Zálohy se ukládají do <code>backups/</code>, retence 14 dní.
- Cron pro každodenní zálohu je v <code>infra/backup/mongo-backup.cron</code>.
- Zálohujte i <code>data/uploads</code> (např. rsync nebo tar).
- Obnovu pravidelně testujte na stagingu.

Obnova:

~~~bash
bash scripts/restore-mongo.sh backups/mongo-20261125-030000.archive.gz
~~~

## Monitoring

- <code>GET /actuator/health</code> – health check backendu (jen interně).
- <code>docker compose ps</code> – stav kontejnerů.
- <code>docker compose logs -f backend</code> – logy.
- Caddy logy: <code>docker compose logs -f caddy</code>.
- Sledujte spotřebu tokenů v admin sekci a nastavte limity.

Doporučené doplňky:

- Uptime monitoring (Uptime Kuma, Healthchecks.io).
- Alarm na zaplnění disku a na chybovost pipeline.
- Pravidelná kontrola záložního úložiště.

## Rollback

~~~bash
git checkout <predchozi-tag>
docker compose up -d --build
~~~

Databázi obnovte ze zálohy, pokud nová verze změnila data.

## Bezpečnostní minimum

- Firewall: povolené pouze 22 (nebo jiný SSH port), 80 a 443.
- SSH pouze s klíčem, bez přihlášení heslem.
- MongoDB není vystavená do internetu.
- Pravidelné aktualizace obrazů a závislostí.
- Silná hesla a 2FA pro admina, pokud je dostupné.
- Zálohy mimo server (jiný stroj nebo objektové úložiště).

## Řešení problémů

| Problém | Kontrola |
|---------|----------|
| 502 Bad Gateway | <code>docker compose ps</code>, logy backendu a frontendu |
| HTTPS nefunguje | DNS, porty 80/443, logy Caddy |
| Backend se restartuje | <code>docker compose logs backend</code>, env proměnné |
| Upload selže | práva <code>data/uploads</code>, <code>UPLOAD_MAX_MB</code> |
| AI neodpovídá | API klíč, kvóta, timeout, logy <code>ai_calls</code> |
| Mongo se nepřipojí | hesla v <code>.env</code>, <code>MONGO_URI</code>, healthcheck |
