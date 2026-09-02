import crypto from 'node:crypto';
import { nanoid } from 'nanoid';
import { db } from '../db/index.js';

/**
 * Contrat webhook Chariow attendu (à ajuster dès que le format exact
 * exposé par Chariow est confirmé — voir ARCHITECTURE.md §4).
 *
 * POST /webhooks/chariow
 * Header: X-Chariow-Signature: sha256=<hmac hex>
 * Body JSON:
 * {
 *   "order_id": "abc123",
 *   "product_url": "https://chariow.com/shukrani-business/50-opportunites-business-ia?ref=<draft_id>",
 *   "amount": 9,
 *   "currency": "USD"
 * }
 */

export function verifySignature(rawBody, signatureHeader) {
  const secret = process.env.CHARIOW_WEBHOOK_SECRET;
  if (!secret) {
    // Aucun secret configuré = on ne peut pas vérifier : on refuse par
    // défaut plutôt que d'accepter des paiements non authentifiés.
    return false;
  }
  if (!signatureHeader) return false;

  const expected = crypto.createHmac('sha256', secret).update(rawBody).digest('hex');
  const provided = signatureHeader.replace(/^sha256=/, '');

  try {
    return crypto.timingSafeEqual(Buffer.from(expected, 'hex'), Buffer.from(provided, 'hex'));
  } catch {
    return false;
  }
}

function extractRef(url) {
  try {
    return new URL(url).searchParams.get('ref');
  } catch {
    return null;
  }
}

const insertSale = db.prepare(`
  INSERT INTO sales (id, chariow_order_id, product_id, attributed_draft_id, channel, amount, currency, raw_payload)
  VALUES (@id, @chariow_order_id, @product_id, @attributed_draft_id, @channel, @amount, @currency, @raw_payload)
  ON CONFLICT(chariow_order_id) DO NOTHING
`);

export function recordSale(payload) {
  const draftId = extractRef(payload.product_url || '');
  let productId = null;
  let channel = null;

  if (draftId) {
    const draft = db
      .prepare('SELECT product_id, channel FROM content_drafts WHERE id = ?')
      .get(draftId);
    if (draft) {
      productId = draft.product_id;
      channel = draft.channel;
    }
  }

  if (!productId) {
    // Pas de ref exploitable : on rattache au moins au produit via l'URL
    // de base (sans query string), pour ne pas perdre la vente.
    const baseUrl = (payload.product_url || '').split('?')[0];
    const product = db.prepare('SELECT id FROM products WHERE chariow_url = ?').get(baseUrl);
    productId = product?.id || null;
  }

  const record = {
    id: nanoid(),
    chariow_order_id: payload.order_id,
    product_id: productId,
    attributed_draft_id: draftId,
    channel,
    amount: Number(payload.amount) || 0,
    currency: payload.currency || 'USD',
    raw_payload: JSON.stringify(payload),
  };

  insertSale.run(record);
  return record;
}
