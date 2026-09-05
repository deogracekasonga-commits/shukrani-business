// Charge les variables d'environnement comme le fait Next.js pour l'app
// (.env puis .env.local par-dessus), pour que les scripts CLI (seed, sync,
// tests) lisent la même config que `npm run dev`. `dotenv/config` seul ne
// lit que `.env`.
import fs from 'node:fs';
import { config as loadEnv } from 'dotenv';

loadEnv({ path: '.env' });
if (fs.existsSync('.env.local')) {
  loadEnv({ path: '.env.local', override: true });
}
