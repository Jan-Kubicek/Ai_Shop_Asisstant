# Zálohy

## Co se zálohuje

- MongoDB – skript <code>scripts/backup-mongo.sh</code>.
- Nahrané soubory v <code>data/uploads</code> – doporučeno tar nebo rsync.
- Konfigurace <code>.env</code> – uložit mimo server, nikdy do gitu.

## Instalace cronu

~~~bash
sudo cp infra/backup/mongo-backup.cron /etc/cron.d/mongo-backup
sudo chmod 644 /etc/cron.d/mongo-backup
sudo touch /var/log/mongo-backup.log
~~~

Upravte cestu k projektu a uživatele podle serveru.

## Retence

Výchozí retence je 14 dní. Změňte ji parametrem skriptu nebo v cronu.

## Obnova

~~~bash
bash scripts/restore-mongo.sh backups/mongo-20261125-030000.archive.gz
~~~

Obnovu testujte na stagingu alespoň jednou za čtvrt roku.

## Doporučení

- Kopírovat zálohy na jiný stroj nebo do objektového úložiště.
- Šifrovat zálohy, pokud obsahují osobní údaje.
- Sledovat volné místo na disku.
- Před každou aktualizací vytvořit zálohu.
