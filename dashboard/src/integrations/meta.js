// Client Meta Graph API — publication Instagram (compte Business).
//
// Respecte les CGU Meta : aucune automatisation non officielle, aucune
// fausse interaction (on n'appelle jamais les endpoints like/comment/follow
// pour gonfler l'engagement). Rate limit standard de la plateforme
// (~200 appels/heure/utilisateur) — largement suffisant pour ce volume de
// publication (quelques posts approuvés par semaine).
import { config } from '../lib/config.js';

const GRAPH_API_BASE = 'https://graph.facebook.com/v21.0';

function isConfigured() {
  return Boolean(config.meta.pageAccessToken && config.meta.igUserId);
}

async function graphPost(path, body) {
  const res = await fetch(`${GRAPH_API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...body, access_token: config.meta.pageAccessToken }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(`Meta Graph API ${path} → ${res.status}: ${JSON.stringify(data)}`);
  }
  return data;
}

/**
 * Publie une image + légende sur le compte Instagram Business configuré.
 * Sans META_PAGE_ACCESS_TOKEN/META_IG_USER_ID, simule la publication
 * (dry-run) : aucun appel réseau, tout est loggé en console.
 * @returns {{ externalPostId: string, permalink: string|null, dryRun: boolean }}
 */
export async function publishToInstagram({ imageUrl, caption }) {
  if (!isConfigured()) {
    console.warn(
      '[meta] META_PAGE_ACCESS_TOKEN/META_IG_USER_ID absents → dry-run (aucune publication réelle)'
    );
    console.log('[meta] dry-run — image :', imageUrl);
    console.log('[meta] dry-run — légende :\n', caption);
    return { externalPostId: `dryrun_${Date.now()}`, permalink: null, dryRun: true };
  }

  // 1. Créer le conteneur média (image + légende).
  const created = await graphPost(`/${config.meta.igUserId}/media`, {
    image_url: imageUrl,
    caption,
  });

  // 2. Publier le conteneur créé.
  const published = await graphPost(`/${config.meta.igUserId}/media_publish`, {
    creation_id: created.id,
  });

  // 3. Récupérer le permalink pour l'afficher dans le dashboard.
  const infoRes = await fetch(
    `${GRAPH_API_BASE}/${published.id}?fields=permalink&access_token=${config.meta.pageAccessToken}`
  );
  const info = await infoRes.json();

  return { externalPostId: published.id, permalink: info.permalink ?? null, dryRun: false };
}

/**
 * Lit les métriques (« insights ») d'un média Instagram déjà publié —
 * utilisé par l'agent analytics pour le rapport hebdomadaire.
 * Sans token configuré, ou pour un id de dry-run (`dryrun_...`), renvoie
 * `null` plutôt que d'appeler l'API.
 * @returns {{ impressions: number, reach: number, engagement: number }|null}
 */
export async function getMediaInsights(mediaId) {
  if (!isConfigured() || !mediaId || mediaId.startsWith('dryrun_')) return null;

  const res = await fetch(
    `${GRAPH_API_BASE}/${mediaId}/insights?metric=impressions,reach,engagement&access_token=${config.meta.pageAccessToken}`
  );
  const data = await res.json();
  if (!res.ok) {
    console.warn(`[meta] insights indisponibles pour ${mediaId} :`, JSON.stringify(data));
    return null;
  }

  const byName = Object.fromEntries((data.data || []).map((m) => [m.name, m.values?.[0]?.value ?? 0]));
  return {
    impressions: byName.impressions ?? 0,
    reach: byName.reach ?? 0,
    engagement: byName.engagement ?? 0,
  };
}
