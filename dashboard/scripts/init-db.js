import 'dotenv/config';
import { getDb } from '../src/db/client.js';

const db = getDb();

const counts = {
  products: db.prepare('SELECT COUNT(*) AS n FROM products').get().n,
  content_drafts: db.prepare('SELECT COUNT(*) AS n FROM content_drafts').get().n,
  sales: db.prepare('SELECT COUNT(*) AS n FROM sales').get().n,
};

console.log('Base de données initialisée:', process.env.DATABASE_PATH || './data/agent.db');
console.log('Tables prêtes:', Object.keys(counts).join(', '), '...');
console.log('Réglages actifs:', db.prepare('SELECT key, value FROM settings').all());
