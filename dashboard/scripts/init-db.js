import './env.js';
import { query, initDb, closePool } from '../src/db/client.js';

await initDb();

const [{ n: products }] = await query('SELECT COUNT(*)::int AS n FROM products');
const [{ n: contentDrafts }] = await query('SELECT COUNT(*)::int AS n FROM content_drafts');
const [{ n: sales }] = await query('SELECT COUNT(*)::int AS n FROM sales');

console.log('Connexion Postgres (Supabase) OK.');
console.log('Lignes actuelles — products:', products, '· content_drafts:', contentDrafts, '· sales:', sales);
console.log('Réglages actifs:', await query('SELECT key, value FROM settings'));

await closePool();
