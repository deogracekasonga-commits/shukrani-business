// Sous-agent Contenu — génère légendes, accroches et scripts vidéo courts
// adaptés à la catégorie de produit ciblée.
//
// MVP : génération par templates (aucun coût API, résultat déterministe et
// facile à relire). Le point d'extension `generateCaptionText` /
// `generateVideoScriptText` est isolé pour brancher l'API Claude plus tard
// (ANTHROPIC_API_KEY, voir src/lib/config.js) sans changer l'appelant
// (orchestrateur, dashboard) ni le format stocké en base.
import * as developpementPersonnel from './templates/developpement-personnel.js';
import * as generic from './templates/generic.js';
import * as linkedin from './templates/linkedin.js';
import { listAllProductsLocal, insertContentDraft } from '../db/repository.js';

const TEMPLATES_BY_CATEGORY = {
  'developpement-personnel': developpementPersonnel,
  // Slug réel côté Chariow pour cette catégorie (voir ACTIVE_CATEGORY).
  personal_development: developpementPersonnel,
};

function templatesFor(categorie) {
  return TEMPLATES_BY_CATEGORY[categorie] || generic;
}

// Ajouté à la fin de chaque légende (pas des scripts vidéo) — demande de
// Deograce pour développer la chaîne WhatsApp et la page Facebook depuis
// chaque post produit.
const COMMUNITY_FOOTER = [
  '👉 Rejoins la chaîne WhatsApp pour les recevoir : https://whatsapp.com/channel/0029VbDdAe2JuyA4SYd9vs1m',
  '👉 Aime la page Facebook : https://www.facebook.com/profile.php?id=61590639694843',
  '',
  '#Productivité #Entrepreneuriat #DéveloppementPersonnel',
].join('\n');

export function generateCaptionText(product, hookIndex = 0) {
  const caption = templatesFor(product.categorie).caption(product, hookIndex);
  return `${caption}\n\n${COMMUNITY_FOOTER}`;
}

export function generateVideoScriptText(product) {
  return templatesFor(product.categorie).videoScript(product);
}

// LinkedIn : un seul ton (professionnel), pas de variante par catégorie —
// le lien Chariow est cliquable directement dans le texte sur ce réseau.
export function generateLinkedInCaptionText(product) {
  return `${linkedin.caption(product)}\n\n${COMMUNITY_FOOTER}`;
}

/**
 * Génère un lot de brouillons (3 à 5, cible par défaut) pour TOUT le
 * catalogue (tous produits, toutes catégories confondues) et les enregistre
 * en base avec le statut `brouillon`.
 * Répartit les variantes entre les produits disponibles : 1 légende par
 * produit au minimum, puis des variantes d'accroche et un script vidéo pour
 * compléter jusqu'à `targetCount`.
 */
export async function generateWeeklyDrafts({ targetCount = 4 } = {}) {
  const products = await listAllProductsLocal();
  if (products.length === 0) {
    throw new Error('Aucun produit en base. Lance d\'abord la synchronisation Chariow.');
  }

  const drafts = [];

  // 1 légende par produit (couvre tout le catalogue de la catégorie).
  for (const product of products) {
    drafts.push(
      await insertContentDraft({
        productId: product.id,
        texte: generateCaptionText(product, 0),
        format: 'post',
        plateforme: 'instagram_facebook',
      })
    );
  }

  // 1 légende LinkedIn par produit — réseau séparé, ton et lien différents,
  // donc pas comptée dans targetCount (cadence propre à ce réseau).
  for (const product of products) {
    drafts.push(
      await insertContentDraft({
        productId: product.id,
        texte: generateLinkedInCaptionText(product),
        format: 'post',
        plateforme: 'linkedin',
      })
    );
  }

  // Complète jusqu'à targetCount en alternant : variante d'accroche (post),
  // puis script vidéo courte, en tournant sur les produits disponibles.
  let productIndex = 0;
  let hookVariant = 1;
  while (drafts.length < targetCount) {
    const product = products[productIndex % products.length];
    const useVideo = drafts.length % 2 === 1; // alterne post/reel_script

    drafts.push(
      useVideo
        ? await insertContentDraft({
            productId: product.id,
            texte: generateVideoScriptText(product),
            format: 'reel_script',
          })
        : await insertContentDraft({
            productId: product.id,
            texte: generateCaptionText(product, hookVariant++),
            format: 'post',
          })
    );
    productIndex++;
  }

  return drafts;
}
