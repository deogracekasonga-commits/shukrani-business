// Client API Chariow (REST) + vérification de signature webhook.
//
// ⚠️ Le format exact des réponses API et du payload webhook n'a pas pu être
// vérifié depuis cet environnement (chariow.dev est bloqué par le proxy
// réseau de la session). Les noms de champs ci-dessous sont ceux annoncés
// dans la spec du projet ; à ajuster dès que Deograce confirme le format
// exact renvoyé par /v1/products, /v1/sales et le webhook depuis son
// dashboard Chariow. Toutes les lectures de champs passent par de petites
// fonctions `extractX()` isolées pour limiter l'impact d'un ajustement.
import crypto from 'node:crypto';
import { config } from '../lib/config.js';

const DRY_RUN_PRODUCTS = [
  {
    id: 'sample-confiance-solide',
    nom: 'Construire une confiance solide',
    categorie: 'developpement-personnel',
    prix: 9,
    lien_chariow: 'https://chariow.com/shukrani-business/construire-une-confiance-solide',
    image_url: 'https://placehold.co/1080x1080/1a2b4c/ffffff.png?text=Confiance+Solide',
  },
  {
    id: 'sample-discipline-quotidienne',
    nom: 'La discipline au quotidien',
    categorie: 'developpement-personnel',
    prix: 7,
    lien_chariow: 'https://chariow.com/shukrani-business/discipline-quotidienne',
    image_url: 'https://placehold.co/1080x1080/1a2b4c/ffffff.png?text=Discipline+Quotidienne',
  },
];

function isConfigured() {
  return Boolean(config.chariow.apiKey);
}

async function chariowFetch(path, { searchParams } = {}) {
  const url = new URL(path, config.chariow.apiBaseUrl.replace(/\/?$/, '/'));
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
 * Liste les produits d'une catégorie via GET /v1/products?category=...
 * Sans CHARIOW_API_KEY configurée, renvoie un jeu de données de démo
 * (dry-run) pour permettre de développer/tester le reste du pipeline.
 */
export async function listProductsByCategory(categorie = config.activeCategory) {
  if (!isConfigured()) {
    console.warn('[chariow] CHARIOW_API_KEY absente → dry-run (produits de démo)');
    return DRY_RUN_PRODUCTS.filter((p) => p.categorie === categorie);
  }
  const data = await chariowFetch('/products', { searchParams: { category: categorie } });
  const items = data.data || data.products || data.results || [];
  return items.map(normalizeProduct);
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
  return {
    id: raw.id || raw.product_id,
    nom: raw.name || raw.nom || raw.title,
    categorie: raw.category || raw.categorie,
    prix: Number(raw.price ?? raw.prix ?? 0),
    lien_chariow: raw.url || raw.lien_chariow || raw.product_url,
    image_url: raw.image_url || raw.cover_image_url || raw.image || raw.thumbnail_url || null,
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
