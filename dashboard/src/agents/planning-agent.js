// Sous-agent Planification — programme les publications validées via Meta
// Graph API et génère des liens trackés (UTM) par post.
//
// Ne publie jamais un brouillon qui n'a pas été explicitement approuvé
// (statut 'valide') dans le dashboard — la validation humaine reste
// obligatoire avant toute publication, dry-run ou réelle.
import { config } from '../lib/config.js';
import { getContentDraft, insertPublishedPost } from '../db/repository.js';
import { publishToInstagram } from '../integrations/meta.js';

/** Génère le lien Chariow du produit avec des paramètres UTM propres à ce brouillon. */
export function buildTrackedLink(draft) {
  const url = new URL(draft.lien_chariow);
  url.searchParams.set('utm_source', 'instagram');
  url.searchParams.set('utm_medium', 'social');
  url.searchParams.set('utm_campaign', draft.categorie || 'general');
  url.searchParams.set('utm_content', draft.id);
  return url.toString();
}

/** Remplace le lien brut par le lien tracké dans le texte, ou l'ajoute si absent. */
function captionWithTrackedLink(draft, trackedLink) {
  if (draft.lien_chariow && draft.texte.includes(draft.lien_chariow)) {
    return draft.texte.replaceAll(draft.lien_chariow, trackedLink);
  }
  return `${draft.texte}\n\n🔗 ${trackedLink}`;
}

/**
 * Publie un brouillon déjà approuvé (statut 'valide'). Lève une erreur si le
 * brouillon n'existe pas ou n'a pas été approuvé — jamais de publication
 * sans validation humaine préalable.
 */
export async function publishApprovedDraft(draftId) {
  const draft = getContentDraft(draftId);
  if (!draft) throw new Error(`Brouillon introuvable : ${draftId}`);
  if (draft.statut !== 'valide') {
    throw new Error('Seul un brouillon approuvé (statut "valide") peut être publié.');
  }

  const utmLink = buildTrackedLink(draft);
  const caption = captionWithTrackedLink(draft, utmLink);
  const imageUrl = draft.image_url || config.meta.defaultImageUrl;

  const result = await publishToInstagram({ imageUrl, caption });

  return insertPublishedPost({
    draftId: draft.id,
    urlInstagram: result.permalink,
    utmLink,
  });
}
