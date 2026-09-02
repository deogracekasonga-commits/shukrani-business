import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { listAllProducts } from '../catalog/index.js';

function normalize(text) {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, ''); // retire les accents pour un matching plus robuste
}

const INTENT_KEYWORDS = {
  prix: ['prix', 'combien', 'coute', 'tarif', 'ca coute'],
  lien_achat: ['lien', 'acheter', 'commander', 'ou trouver', 'ou je peux', 'commande'],
  paiement: ['payer', 'paiement', 'mobile money', 'orange money', 'airtel money', 'mpesa'],
  disponibilite: ['disponible', 'en stock', 'toujours dispo'],
};

function detectIntent(message) {
  const text = normalize(message);
  for (const [intent, keywords] of Object.entries(INTENT_KEYWORDS)) {
    if (keywords.some((k) => text.includes(k))) return intent;
  }
  return 'autre';
}

function findMentionedProduct(message) {
  const text = normalize(message);
  return listAllProducts().find((p) => text.includes(normalize(p.title).split(' ').slice(0, 3).join(' ')));
}

const TONE_INTRO = 'Bonjour et merci de nous écrire 🙏';

function buildReply(intent, product) {
  const productLine = product
    ? `"${product.title}" est disponible à ${product.price_usd} $ ici : ${product.chariow_url}`
    : `Nos ebooks sont disponibles sur notre boutique Chariow. Dites-moi lequel vous intéresse et je vous envoie le lien direct.`;

  switch (intent) {
    case 'prix':
      return `${TONE_INTRO} 😊 ${productLine}`;
    case 'lien_achat':
      return `${TONE_INTRO} Avec plaisir ! ${productLine}`;
    case 'paiement':
      return `${TONE_INTRO} Le paiement se fait directement sur la page Chariow (mobile money et carte acceptés selon les options affichées) : ${
        product ? product.chariow_url : 'lien disponible dès que vous me précisez le titre qui vous intéresse'
      }.`;
    case 'disponibilite':
      return `${TONE_INTRO} Oui, ${
        product ? `"${product.title}" est` : 'nos ebooks sont'
      } disponible(s) 24h/24 en version numérique, aucune rupture de stock possible 📚.`;
    default:
      return `${TONE_INTRO} Un membre de l'équipe Shukrani Business va vous répondre avec plaisir. En attendant, voici notre catalogue : demandez-moi "prix" ou le titre qui vous intéresse et je vous envoie le lien direct.`;
  }
}

const insertConversation = db.prepare(`
  INSERT INTO conversations (id, channel, contact_ref, message_in, intent, message_out, handled_by)
  VALUES (@id, @channel, @contact_ref, @message_in, @intent, @message_out, @handled_by)
`);

/**
 * Traite un message entrant (commentaire ou DM) et propose une réponse.
 * N'envoie rien à la plateforme : l'envoi réel passe par
 * src/integrations/whatsapp.js ou meta.js (phase 2), sur la même logique
 * "brouillon par défaut" que pour les publications.
 */
export function handleIncomingMessage({ channel, contactRef, message }) {
  const intent = detectIntent(message);
  const product = findMentionedProduct(message);
  const reply = buildReply(intent, product);

  const record = {
    id: nanoid(),
    channel,
    contact_ref: contactRef || null,
    message_in: message,
    intent,
    message_out: reply,
    handled_by: 'agent',
  };
  insertConversation.run(record);
  return record;
}

export function listRecentConversations(limit = 50) {
  return db
    .prepare('SELECT * FROM conversations ORDER BY created_at DESC LIMIT ?')
    .all(limit);
}
