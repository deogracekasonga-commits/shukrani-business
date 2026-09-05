import Database from 'better-sqlite3';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const DB_PATH = process.env.DATABASE_PATH || path.join(process.cwd(), 'data', 'agent.db');

let dbInstance;

function loadSchema(db) {
  const schema = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf8');
  db.exec(schema);
}

const DEFAULT_SETTINGS = {
  active_category: process.env.ACTIVE_CATEGORY || 'developpement-personnel',
  ad_budget_weekly_cap: process.env.AD_BUDGET_WEEKLY_CAP || '0',
  auto_publish_instagram: process.env.AUTO_PUBLISH_INSTAGRAM || 'false',
};

function seedDefaultSettings(db) {
  const insert = db.prepare(
    'INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)'
  );
  for (const [key, value] of Object.entries(DEFAULT_SETTINGS)) {
    insert.run(key, value);
  }
}

export function getDb() {
  if (dbInstance) return dbInstance;
  fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });
  dbInstance = new Database(DB_PATH);
  dbInstance.pragma('journal_mode = WAL');
  dbInstance.pragma('foreign_keys = ON');
  loadSchema(dbInstance);
  seedDefaultSettings(dbInstance);
  return dbInstance;
}
