// Petites requêtes réutilisables — évite de dupliquer du SQL brut dans les
// routes API et les scripts.
import { nanoid } from 'nanoid';
import { getDb } from './client.js';

export function upsertProductFromChariow(product) {
  const db = getDb();
  const existing = product.chariow_product_id
    ? db
        .prepare('SELECT id FROM products WHERE chariow_product_id = ?')
        .get(product.chariow_product_id)
    : null;

  if (existing) {
    db.prepare(
      'UPDATE products SET nom = ?, categorie = ?, prix = ?, lien_chariow = ? WHERE id = ?'
    ).run(product.nom, product.categorie, product.prix, product.lien_chariow, existing.id);
    return existing.id;
  }

  const id = nanoid();
  db.prepare(
    `INSERT INTO products (id, nom, categorie, prix, lien_chariow, chariow_product_id)
     VALUES (?, ?, ?, ?, ?, ?)`
  ).run(id, product.nom, product.categorie, product.prix, product.lien_chariow, product.chariow_product_id ?? product.id ?? null);
  return id;
}

export function findProductByChariowRef({ chariowProductId, productUrl }) {
  const db = getDb();
  if (chariowProductId) {
    const byId = db
      .prepare('SELECT * FROM products WHERE chariow_product_id = ?')
      .get(chariowProductId);
    if (byId) return byId;
  }
  if (productUrl) {
    // le lien de vente peut porter des paramètres (?ref=..., ?utm_...) : on
    // compare sur la base de l'URL sans query string.
    const baseUrl = productUrl.split('?')[0];
    const byUrl = db
      .prepare('SELECT * FROM products WHERE lien_chariow LIKE ?')
      .get(`${baseUrl}%`);
    if (byUrl) return byUrl;
  }
  return null;
}

/** Retrouve le post publié correspondant à un lien UTM cliqué, pour attribution. */
export function findPublishedPostByUtmLink(productUrl) {
  if (!productUrl) return null;
  const db = getDb();
  return db
    .prepare('SELECT * FROM published_posts WHERE utm_link = ?')
    .get(productUrl);
}

/**
 * Enregistre une vente reçue par webhook, de façon idempotente
 * (chariow_event_id est UNIQUE : un même événement rejoué est ignoré).
 * @returns {{ inserted: boolean, sale: object }}
 */
export function recordSaleFromWebhook(sale) {
  const db = getDb();

  if (sale.chariow_event_id) {
    const existing = db
      .prepare('SELECT * FROM sales WHERE chariow_event_id = ?')
      .get(sale.chariow_event_id);
    if (existing) return { inserted: false, sale: existing };
  }

  const product = findProductByChariowRef({
    chariowProductId: sale.chariow_product_id,
    productUrl: sale.product_url,
  });
  const publishedPost = findPublishedPostByUtmLink(sale.product_url);

  const id = nanoid();
  db.prepare(
    `INSERT INTO sales (id, product_id, montant, date, source_webhook, chariow_event_id, attributed_post_id)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).run(
    id,
    product?.id ?? null,
    sale.montant,
    sale.date,
    JSON.stringify(sale.raw ?? sale),
    sale.chariow_event_id ?? null,
    publishedPost?.id ?? null
  );

  return { inserted: true, sale: db.prepare('SELECT * FROM sales WHERE id = ?').get(id) };
}

export function listRecentSales(limit = 50) {
  const db = getDb();
  return db
    .prepare(
      `SELECT sales.*, products.nom AS produit_nom, products.categorie
       FROM sales LEFT JOIN products ON products.id = sales.product_id
       ORDER BY sales.date DESC LIMIT ?`
    )
    .all(limit);
}
