// Vytvoří aplikačního uživatele a indexy.
// Skript se spustí pouze při první inicializaci datového adresáře.

const appDbName = _getEnv('MONGO_DATABASE') || "shopassistant";
const appUser = _getEnv('MONGO_APP_USER') || "app";
const appPassword = _getEnv('MONGO_APP_PASSWORD');

if (!appPassword) {
  print("MONGO_APP_PASSWORD není nastaveno, přeskakuji vytvoření aplikačního uživatele.");
} else {
  const appDb = db.getSiblingDB(appDbName);

  appDb.createUser({
    user: appUser,
    pwd: appPassword,
    roles: [{ role: "readWrite", db: appDbName }],
  });

  appDb.users.createIndex({ email: 1 }, { unique: true });
  appDb.requests.createIndex({ userId: 1, createdAt: -1 });
  appDb.requests.createIndex({ status: 1 });
  appDb.media.createIndex({ ownerId: 1 });
  appDb.media.createIndex({ ownerId: 1, sha256: 1 }, { unique: true });
  appDb.results.createIndex({ requestId: 1 }, { unique: true });
  appDb.shop_queue.createIndex({ status: 1, createdAt: 1 });
  appDb.ai_calls.createIndex({ requestId: 1 });
  appDb.ai_calls.createIndex({ timestamp: -1 });
  appDb.audit_log.createIndex({ userId: 1, timestamp: -1 });
  appDb.audit_log.createIndex({ timestamp: 1 }, { expireAfterSeconds: 31536000 });

  print("MongoDB inicializace dokončena pro databázi " + appDbName + ".");
}
