// Client API Chariow (REST) + vérification de signature webhook.
//
// Confirmé via chariow.dev/api-reference (capture d'écran Deograce, 09/2026) :
// - Base URL : https://api.chariow.com/v1, auth Bearer token
// - Enveloppe de réponse : { message, data, errors }
// - `price` est un objet imbriqué { value, ... }, pas un nombre brut
// - Pagination par curseur (cursor/per_page → { next_cursor, prev_cursor,
//   has_more }) — non géré ici (MVP : un seul appel, catalogue restreint) ;
//   à ajouter si le nombre de produits dépasse une page.
// Le nom exact de l'événement webhook de vente ("sale.completed") et le
// header de signature restent à confirmer depuis le dashboard Chariow.
// Toutes les lectures de champs passent par de petites fonctions
// `extractX()`/`normalizeX()` isolées pour limiter l'impact d'un ajustement.
import crypto from 'node:crypto';
import { config } from '../lib/config.js';

const DRY_RUN_PRODUCTS = [
  {
    id: 'sample-confiance-solide',
    nom: 'Construire une confiance solide',
    categorie: 'personal_development',
    prix: 9,
    lien_chariow: 'https://chariow.com/shukrani-business/construire-une-confiance-solide',
    image_url: 'https://placehold.co/1080x1080/1a2b4c/ffffff.png?text=Confiance+Solide',
  },
  {
    id: 'sample-discipline-quotidienne',
    nom: 'La discipline au quotidien',
    categorie: 'personal_development',
    prix: 7,
    lien_chariow: 'https://chariow.com/shukrani-business/discipline-quotidienne',
    image_url: 'https://placehold.co/1080x1080/1a2b4c/ffffff.png?text=Discipline+Quotidienne',
  },
];

function isConfigured() {
  return Boolean(config.chariow.apiKey);
}

async function chariowFetch(path, { searchParams } = {}) {
  // ⚠️ path ne doit JAMAIS commencer par "/" : avec new URL(path, base), un
  // chemin absolu (commençant par "/") remplace tout le chemin de la base
  // (donc "/products" + base ".../v1/" → ".../products", en perdant le
  // "/v1" — c'était le bug à l'origine du 404 "route products could not be
  // found"). On force donc un chemin relatif ici.
  const url = new URL(path.replace(/^\/+/, ''), config.chariow.apiBaseUrl.replace(/\/?$/, '/'));
  if (searchParams) {
    for (const [key, value] of Object.entries(searchParams)) {
      if (value !== undefined && value !== null) url.searchParams.set(key, value);
    }
  }
  const res = await fetch(url, {
    headers: {
      Authorization: `Bearer ${config.chariow.apiKey}`,
      Accept: 'application/json',
    },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(`Chariow API ${path} → ${res.status}: ${body.slice(0, 300)}`);
  }
  return res.json();
}

/**
 * Liste les produits d'une catégorie via GET /v1/products.
 * Sans CHARIOW_API_KEY configurée, renvoie un jeu de données de démo
 * (dry-run) pour permettre de développer/tester le reste du pipeline.
 *
 * ⚠️ On ne filtre PAS côté serveur avec ?category=... : le nom exact du
 * paramètre attendu par Chariow (et s'il prend un slug ou un ID) n'a pas pu
 * être confirmé. On récupère donc tous les produits (première page, 100 max
 * — le catalogue MVP est petit) et on filtre nous-mêmes sur `categorie`.
 */
export async function listProductsByCategory(categorie = config.activeCategory) {
  if (!isConfigured()) {
    console.warn('[chariow] CHARIOW_API_KEY absente → dry-run (produits de démo)');
    return DRY_RUN_PRODUCTS.filter((p) => p.categorie === categorie);
  }
  const { normalized } = await fetchAllProductsRaw();
  return normalized.filter((p) => p.categorie === categorie);
}

/**
 * Récupère tous les produits (sans filtre de catégorie), pour lister le
 * catalogue et diagnostiquer les écarts de nommage de catégorie côté
 * Chariow (utilisé par le bandeau de diagnostic du dashboard).
 */
export async function fetchAllProductsRaw() {
  const data = await chariowFetch('products', { searchParams: { per_page: 100 } });
  const rawItems = data.data || data.products || data.results || [];
  const normalized = rawItems.map(normalizeProduct);
  return { rawItems, normalized };
}

/**
 * Historique des ventes via GET /v1/sales (nom d'endpoint à confirmer —
 * peut être /v1/orders selon la version de l'API).
 */
export async function getSalesHistory({ since } = {}) {
  if (!isConfigured()) {
    console.warn('[chariow] CHARIOW_API_KEY absente → dry-run (historique vide)');
    return [];
  }
  const data = await chariowFetch('/sales', { searchParams: since ? { since } : undefined });
  return data.data || data.sales || data.results || [];
}

function normalizeProduct(raw) {
  // Structure réelle confirmée via un produit synchronisé (09/2026) :
  // price sous pricing.current_price.value, category sous category.value,
  // et AUCUN champ URL — le lien de vente se reconstruit à partir de l'id
  // (voir lien_chariow ci-dessous).
  const prix = Number(raw.pricing?.current_price?.value ?? raw.price?.value ?? raw.price ?? 0);
  const categorie = raw.category?.value ?? raw.category?.slug ?? raw.category ?? null;

  const productId = raw.id || raw.product_id;
  const lienChariow =
    raw.url ||
    raw.lien_chariow ||
    raw.product_url ||
    (config.chariow.storeSubdomain && productId
      ? `https://${config.chariow.storeSubdomain}.mychariow.com/${productId}`
      : null);

  return {
    id: productId,
    nom: raw.name || raw.nom || raw.title,
    categorie,
    prix,
    lien_chariow: lienChariow,
    image_url: raw.pictures?.thumbnail || raw.image_url || raw.cover_image_url || raw.image || null,
  };
}

/**
 * Vérifie la signature HMAC-SHA256 du webhook Chariow.
 * En-tête attendu : `X-Chariow-Signature: sha256=<hex>` (à confirmer —
 * certains providers utilisent un header différent ou un timestamp signé).
 * @param {string} rawBody corps brut de la requête (avant JSON.parse)
 * @param {string|null} signatureHeader valeur du header de signature
 */
export function verifyWebhookSignature(rawBody, signatureHeader) {
  if (!config.chariow.webhookSecret) return false; // jamais accepter sans secret configuré
  if (!signatureHeader) return false;

  const expected = crypto
    .createHmac('sha256', config.chariow.webhookSecret)
    .update(rawBody, 'utf8')
    .digest('hex');
  const provided = signatureHeader.replace(/^sha256=/, '').trim();

  const expectedBuf = Buffer.from(expected, 'hex');
  const providedBuf = Buffer.from(provided, 'hex');
  if (expectedBuf.length !== providedBuf.length) return false;
  return crypto.timingSafeEqual(expectedBuf, providedBuf);
}

/**
 * Normalise le payload de l'événement "sale.completed" (ou équivalent) en
 * un objet plat prêt à insérer dans la table `sales`.
 */
export function extractSaleFromWebhookPayload(payload) {
  const event = payload.event || payload.type;
  const sale = payload.data || payload.sale || payload;

  return {
    event,
    chariow_event_id: payload.id || payload.event_id || sale.id || sale.order_id,
    chariow_product_id: sale.product_id || sale.product?.id,
    nom_produit: sale.product_name || sale.product?.name,
    montant: Number(sale.amount ?? sale.montant ?? sale.total ?? 0),
    date: sale.created_at || sale.paid_at || sale.date || new Date().toISOString(),
    categorie: sale.category || sale.product?.category,
    // lien cliqué par l'acheteur, porte le paramètre UTM du post d'origine
    product_url: sale.product_url || sale.url,
    raw: payload,
  };
}
