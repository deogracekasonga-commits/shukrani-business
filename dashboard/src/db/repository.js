// Petites requêtes réutilisables — évite de dupliquer du SQL brut dans les
// routes API et les scripts. Toutes les fonctions sont asynchrones
// (connexion Postgres/Supabase via `query()`).
import { nanoid } from 'nanoid';
import { query } from './client.js';

const nowIso = () => new Date().toISOString();

export async function upsertProductFromChariow(product) {
  const existing = product.chariow_product_id
    ? (await query('SELECT id FROM products WHERE chariow_product_id = $1', [product.chariow_product_id]))[0]
    : null;

  if (existing) {
    await query(
      'UPDATE products SET nom = $1, categorie = $2, prix = $3, lien_chariow = $4, image_url = $5 WHERE id = $6',
      [product.nom, product.categorie, product.prix, product.lien_chariow, product.image_url ?? null, existing.id]
    );
    return existing.id;
  }

  const id = nanoid();
  await query(
    `INSERT INTO products (id, nom, categorie, prix, lien_chariow, chariow_product_id, image_url, created_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
    [
      id,
      product.nom,
      product.categorie,
      product.prix,
      product.lien_chariow,
      product.chariow_product_id ?? product.id ?? null,
      product.image_url ?? null,
      nowIso(),
    ]
  );
  return id;
}

export async function findProductByChariowRef({ chariowProductId, productUrl }) {
  if (chariowProductId) {
    const [byId] = await query('SELECT * FROM products WHERE chariow_product_id = $1', [chariowProductId]);
    if (byId) return byId;
  }
  if (productUrl) {
    // le lien de vente peut porter des paramètres (?ref=..., ?utm_...) : on
    // compare sur la base de l'URL sans query string.
    const baseUrl = productUrl.split('?')[0];
    const [byUrl] = await query('SELECT * FROM products WHERE lien_chariow LIKE $1', [`${baseUrl}%`]);
    if (byUrl) return byUrl;
  }
  return null;
}

/** Retrouve le post publié correspondant à un lien UTM cliqué, pour attribution. */
export async function findPublishedPostByUtmLink(productUrl) {
  if (!productUrl) return null;
  const [post] = await query('SELECT * FROM published_posts WHERE utm_link = $1', [productUrl]);
  return post ?? null;
}

/**
 * Enregistre une vente reçue par webhook, de façon idempotente
 * (chariow_event_id est UNIQUE : un même événement rejoué est ignoré).
 * @returns {Promise<{ inserted: boolean, sale: object }>}
 */
export async function recordSaleFromWebhook(sale) {
  if (sale.chariow_event_id) {
    const [existing] = await query('SELECT * FROM sales WHERE chariow_event_id = $1', [sale.chariow_event_id]);
    if (existing) return { inserted: false, sale: existing };
  }

  const product = await findProductByChariowRef({
    chariowProductId: sale.chariow_product_id,
    productUrl: sale.product_url,
  });
  const publishedPost = await findPublishedPostByUtmLink(sale.product_url);

  const id = nanoid();
  await query(
    `INSERT INTO sales (id, product_id, montant, date, source_webhook, chariow_event_id, attributed_post_id)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [
      id,
      product?.id ?? null,
      sale.montant,
      sale.date,
      JSON.stringify(sale.raw ?? sale),
      sale.chariow_event_id ?? null,
      publishedPost?.id ?? null,
    ]
  );

  const [stored] = await query('SELECT * FROM sales WHERE id = $1', [id]);
  return { inserted: true, sale: stored };
}

export async function listProductsByCategoryLocal(categorie) {
  return query('SELECT * FROM products WHERE categorie = $1 ORDER BY created_at', [categorie]);
}

export async function listAllProductsLocal() {
  return query('SELECT * FROM products ORDER BY created_at');
}

export async function insertContentDraft({ productId, texte, format = 'post' }) {
  const id = nanoid();
  await query(
    `INSERT INTO content_drafts (id, product_id, texte, format, statut, date_creation)
     VALUES ($1, $2, $3, $4, 'brouillon', $5)`,
    [id, productId, texte, format, nowIso()]
  );
  const [draft] = await query('SELECT * FROM content_drafts WHERE id = $1', [id]);
  return draft;
}

export async function listContentDrafts({ statut, limit = 50 } = {}) {
  const where = statut ? 'WHERE content_drafts.statut = $1' : '';
  const params = statut ? [statut, limit] : [limit];
  return query(
    `SELECT content_drafts.*, products.nom AS produit_nom, products.categorie
     FROM content_drafts LEFT JOIN products ON products.id = content_drafts.product_id
     ${where}
     ORDER BY content_drafts.date_creation DESC LIMIT $${params.length}`,
    params
  );
}

export async function getContentDraft(id) {
  const [draft] = await query(
    `SELECT content_drafts.*, products.nom AS produit_nom, products.categorie, products.lien_chariow, products.image_url
     FROM content_drafts LEFT JOIN products ON products.id = content_drafts.product_id
     WHERE content_drafts.id = $1`,
    [id]
  );
  return draft ?? null;
}

/** Approuve ou rejette un brouillon (statut : valide | rejete). */
export async function reviewContentDraft(id, statut, reviewedBy = 'Deograce') {
  await query(
    `UPDATE content_drafts SET statut = $1, date_validation = $2, valide_par = $3 WHERE id = $4`,
    [statut, nowIso(), reviewedBy, id]
  );
  return getContentDraft(id);
}

/** Modifie le texte d'un brouillon (repasse en statut brouillon s'il avait déjà été traité). */
export async function updateContentDraftText(id, texte) {
  await query(
    `UPDATE content_drafts SET texte = $1, statut = 'brouillon', date_validation = NULL, valide_par = NULL
     WHERE id = $2`,
    [texte, id]
  );
  return getContentDraft(id);
}

/**
 * Enregistre une publication Instagram et marque le brouillon comme publié.
 * `urlInstagram` peut être `null` en dry-run (aucun token Meta configuré) —
 * le lien tracké (`utmLink`), lui, existe toujours.
 */
export async function insertPublishedPost({ draftId, urlInstagram, utmLink, externalPostId }) {
  const id = nanoid();
  await query(
    `INSERT INTO published_posts (id, draft_id, url_instagram, date_publication, utm_link, external_post_id)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [id, draftId, urlInstagram, nowIso(), utmLink, externalPostId ?? null]
  );
  await query(`UPDATE content_drafts SET statut = 'publie' WHERE id = $1`, [draftId]);
  const [post] = await query('SELECT * FROM published_posts WHERE id = $1', [id]);
  return post;
}

export async function getPublishedPostForDraft(draftId) {
  const [post] = await query('SELECT * FROM published_posts WHERE draft_id = $1', [draftId]);
  return post ?? null;
}

export async function listPublishedPosts(limit = 50) {
  return query(
    `SELECT published_posts.*, content_drafts.format, products.nom AS produit_nom, products.categorie
     FROM published_posts
     LEFT JOIN content_drafts ON content_drafts.id = published_posts.draft_id
     LEFT JOIN products ON products.id = content_drafts.product_id
     ORDER BY published_posts.date_publication DESC LIMIT $1`,
    [limit]
  );
}

export async function listRecentSales(limit = 50) {
  return query(
    `SELECT sales.*, products.nom AS produit_nom, products.categorie
     FROM sales LEFT JOIN products ON products.id = sales.product_id
     ORDER BY sales.date DESC LIMIT $1`,
    [limit]
  );
}

/** Ventes dont la date tombe dans [sinceIso, untilIso), produit + catégorie inclus. */
export async function listSalesBetween(sinceIso, untilIso) {
  return query(
    `SELECT sales.*, products.nom AS produit_nom, products.categorie
     FROM sales LEFT JOIN products ON products.id = sales.product_id
     WHERE sales.date >= $1 AND sales.date < $2
     ORDER BY sales.date`,
    [sinceIso, untilIso]
  );
}

/** Posts publiés dans [sinceIso, untilIso), avec produit/catégorie associés. */
export async function listPublishedPostsBetween(sinceIso, untilIso) {
  return query(
    `SELECT published_posts.*, products.nom AS produit_nom, products.categorie
     FROM published_posts
     LEFT JOIN content_drafts ON content_drafts.id = published_posts.draft_id
     LEFT JOIN products ON products.id = content_drafts.product_id
     WHERE published_posts.date_publication >= $1 AND published_posts.date_publication < $2
     ORDER BY published_posts.date_publication`,
    [sinceIso, untilIso]
  );
}

export async function insertWeeklyReport({ semaine, ventesTotales, caParCategorie, topPosts }) {
  const id = nanoid();
  await query(
    `INSERT INTO weekly_reports (id, semaine, ventes_totales, ca_par_categorie, top_posts, created_at)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [id, semaine, ventesTotales, JSON.stringify(caParCategorie), JSON.stringify(topPosts), nowIso()]
  );
  const [report] = await query('SELECT * FROM weekly_reports WHERE id = $1', [id]);
  return report;
}

export async function listWeeklyReports(limit = 20) {
  return query('SELECT * FROM weekly_reports ORDER BY semaine DESC LIMIT $1', [limit]);
}

export async function getWeeklyReport(semaine) {
  const [report] = await query('SELECT * FROM weekly_reports WHERE semaine = $1', [semaine]);
  return report ?? null;
}

export async function getSetting(key) {
  const [row] = await query('SELECT value FROM settings WHERE key = $1', [key]);
  return row?.value ?? null;
}

export async function setSetting(key, value) {
  await query(
    `INSERT INTO settings (key, value) VALUES ($1, $2)
     ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value`,
    [key, value]
  );
}
