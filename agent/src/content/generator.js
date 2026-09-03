import * as businessTemplate from './templates/business-entrepreneuriat.js';
import * as genericTemplate from './templates/generic.js';

const TEMPLATES_BY_CATEGORY_SLUG = {
  'business-entrepreneuriat': businessTemplate,
};

function templateFor(categorySlug) {
  return TEMPLATES_BY_CATEGORY_SLUG[categorySlug] || genericTemplate;
}

/**
 * Génère un ou plusieurs brouillons de contenu pour un produit donné, sur
 * un canal donné. Une variante par accroche du template (test A/B) —
 * cf. consigne "mécanisme de test A/B sur les accroches marketing".
 *
 * Ne touche PAS la base de données : retourne des objets simples que
 * l'orchestrateur décide (ou non) d'enregistrer en `content_drafts`.
 */
export function generateDraftVariants({ product, category, channel = 'facebook' }) {
  const template = templateFor(category.slug);
  const format = channel === 'tiktok' ? 'reel_script' : 'post';

  return template.hooks.map(({ group, text }) => {
    const hook = text(product);
    return {
      ab_group: group,
      channel,
      format,
      hook,
      caption: template.buildCaption({ product, category, hook }),
      hashtags: template.hashtags.join(' '),
      video_script:
        channel === 'tiktok' || channel === 'instagram'
          ? template.buildVideoScript({ product, hook })
          : null,
    };
  });
}
