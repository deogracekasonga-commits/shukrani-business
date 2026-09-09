// Connexion PostgreSQL (Supabase). Le schéma est créé par migration
// (voir schema.sql, appliqué via Supabase) — ce module se contente de la
// connexion et de l'amorçage des réglages par défaut.
import pg from 'pg';

const { Pool } = pg;

let pool;

function getPool() {
  if (pool) return pool;
  const connectionString = process.env.DATABASE_URL;
  if (!connectionString) {
    throw new Error(
      'DATABASE_URL manquante — voir .env.example (chaîne de connexion Postgres Supabase, ' +
        'idéalement le pooler port 6543 pour un environnement serverless).'
    );
  }
  pool = new Pool({ connectionString, ssl: { rejectUnauthorized: false } });
  return pool;
}

/** Exécute une requête paramétrée ($1, $2, ...) et renvoie les lignes. */
export async function query(text, params = []) {
  const result = await getPool().query(text, params);
  return result.rows;
}

/** Ferme le pool de connexions — utile en fin de script CLI pour que le process se termine. */
export async function closePool() {
  if (pool) await pool.end();
}

const DEFAULT_SETTINGS = {
  active_category: process.env.ACTIVE_CATEGORY || 'personal_development',
  ad_budget_weekly_cap: process.env.AD_BUDGET_WEEKLY_CAP || '0',
  auto_publish_instagram: process.env.AUTO_PUBLISH_INSTAGRAM || 'false',
};

let seeded = false;

/** Amorce les réglages par défaut (idempotent, une seule fois par process). */
export async function initDb() {
  if (seeded) return;
  for (const [key, value] of Object.entries(DEFAULT_SETTINGS)) {
    await query('INSERT INTO settings (key, value) VALUES ($1, $2) ON CONFLICT (key) DO NOTHING', [
      key,
      value,
    ]);
  }
  seeded = true;
}
