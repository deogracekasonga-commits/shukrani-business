// Simule une vente Chariow de bout en bout : construit un payload
// "sale.completed" plausible, le signe en HMAC-SHA256 comme le ferait
// Chariow, puis l'envoie au webhook local (npm run dev doit tourner).
// Usage : npm run test:fake-sale [-- --url http://localhost:3000]
import './env.js';
import crypto from 'node:crypto';
import { config } from '../src/lib/config.js';
import { getDb } from '../src/db/client.js';

const baseUrl = process.argv.includes('--url')
  ? process.argv[process.argv.indexOf('--url') + 1]
  : `http://localhost:${process.env.PORT || 3000}`;

if (!config.chariow.webhookSecret) {
  console.error(
    'CHARIOW_WEBHOOK_SECRET absente de .env.local — le webhook refusera toute requête ' +
      'non signée. Définis-en une (même une valeur de test) avant de lancer ce script.'
  );
  process.exit(1);
}

const db = getDb();

// S'il existe un post déjà publié, on simule l'achat via SON lien tracké
// (comme le ferait un vrai acheteur cliquant depuis Instagram) pour tester
// l'attribution vente ↔ post en plus de l'enregistrement de la vente.
const publishedPost = db
  .prepare(
    `SELECT published_posts.*, products.* FROM published_posts
     JOIN content_drafts ON content_drafts.id = published_posts.draft_id
     JOIN products ON products.id = content_drafts.product_id
     ORDER BY published_posts.date_publication DESC LIMIT 1`
  )
  .get();

const product = publishedPost || db.prepare('SELECT * FROM products LIMIT 1').get();
if (!product) {
  console.error('Aucun produit en base. Lance d\'abord `npm run sync:products`.');
  process.exit(1);
}
const productUrl = publishedPost ? publishedPost.utm_link : product.lien_chariow;

const payload = {
  event: 'sale.completed',
  id: `evt_test_${Date.now()}`,
  data: {
    id: `order_test_${Date.now()}`,
    product_id: product.chariow_product_id || product.id,
    product_name: product.nom,
    category: product.categorie,
    amount: product.prix,
    currency: 'USD',
    created_at: new Date().toISOString(),
    product_url: productUrl,
  },
};

const rawBody = JSON.stringify(payload);
const signature = crypto
  .createHmac('sha256', config.chariow.webhookSecret)
  .update(rawBody, 'utf8')
  .digest('hex');

const res = await fetch(`${baseUrl}/api/webhooks/chariow`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Chariow-Signature': `sha256=${signature}`,
  },
  body: rawBody,
});

const body = await res.json().catch(() => ({}));
console.log(`POST /api/webhooks/chariow → ${res.status}`, body);
if (res.ok) {
  console.log(`\nVente factice envoyée pour "${product.nom}" (${product.prix}$).`);
  console.log('Vérifie la table `sales` (ou le dashboard une fois l\'Étape 4 en place).');
}
