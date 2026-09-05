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
import { listProductsByCategoryLocal, insertContentDraft } from '../db/repository.js';
import { config } from '../lib/config.js';

const TEMPLATES_BY_CATEGORY = {
  'developpement-personnel': developpementPersonnel,
};

function templatesFor(categorie) {
  return TEMPLATES_BY_CATEGORY[categorie] || generic;
}

export function generateCaptionText(product, hookIndex = 0) {
  return templatesFor(product.categorie).caption(product, hookIndex);
}

export function generateVideoScriptText(product) {
  return templatesFor(product.categorie).videoScript(product);
}

/**
 * Génère un lot de brouillons (3 à 5, cible par défaut) pour la catégorie
 * active et les enregistre en base avec le statut `brouillon`.
 * Répartit les variantes entre les produits disponibles : 1 légende par
 * produit au minimum, puis des variantes d'accroche et un script vidéo pour
 * compléter jusqu'à `targetCount`.
 */
export function generateWeeklyDrafts({
  categorie = config.activeCategory,
  targetCount = 4,
} = {}) {
  const products = listProductsByCategoryLocal(categorie);
  if (products.length === 0) {
    throw new Error(
      `Aucun produit en base pour la catégorie "${categorie}". Lance d'abord ` +
        `npm run sync:products.`
    );
  }

  const drafts = [];

  // 1 légende par produit (couvre tout le catalogue de la catégorie).
  for (const product of products) {
    drafts.push(
      insertContentDraft({
        productId: product.id,
        texte: generateCaptionText(product, 0),
        format: 'post',
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
        ? insertContentDraft({
            productId: product.id,
            texte: generateVideoScriptText(product),
            format: 'reel_script',
          })
        : insertContentDraft({
            productId: product.id,
            texte: generateCaptionText(product, hookVariant++),
            format: 'post',
          })
    );
    productIndex++;
  }

  return drafts;
}
