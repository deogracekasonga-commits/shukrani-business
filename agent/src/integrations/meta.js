import { nanoid } from 'nanoid';
import { db, getSetting } from '../db/index.js';

const GRAPH_API_VERSION = 'v19.0';

function isAutoPublishEnabled(channel) {
  return getSetting(`auto_publish_${channel}`, 'false') === 'true';
}

function hasCredentials() {
  return Boolean(process.env.META_PAGE_ACCESS_TOKEN && process.env.META_PAGE_ID);
}

const insertPublication = db.prepare(`
  INSERT INTO publications (id, draft_id, channel, external_post_id, dry_run)
  VALUES (@id, @draft_id, @channel, @external_post_id, @dry_run)
`);

/**
 * Publie un brouillon APPROUVÉ sur Facebook (Meta Graph API).
 *
 * Garde-fous (non contournables par l'agent) :
 *  - le brouillon doit avoir le statut "approved" (validation humaine)
 *  - si le token Meta n'est pas configuré, OU si le réglage
 *    `auto_publish_<channel>` est désactivé (par défaut), on simule
 *    (dry-run) : rien n'est envoyé à Meta, on log ce qui aurait été publié.
 */
export async function publishToFacebook(draft) {
  if (draft.status !== 'approved') {
    throw new Error(`Le brouillon ${draft.id} doit être approuvé avant publication (statut actuel: ${draft.status}).`);
  }

  const message = `${draft.caption}\n\n${draft.hashtags}\n\n${draft.cta_link}`;
  const dryRun = !hasCredentials() || !isAutoPublishEnabled('facebook');

  if (dryRun) {
    console.log('[meta:facebook][DRY-RUN] Publication simulée :\n---\n%s\n---', message);
    const record = {
      id: nanoid(),
      draft_id: draft.id,
      channel: 'facebook',
      external_post_id: null,
      dry_run: 1,
    };
    insertPublication.run(record);
    db.prepare(`UPDATE content_drafts SET status = 'scheduled' WHERE id = ?`).run(draft.id);
    return record;
  }

  const url = `https://graph.facebook.com/${GRAPH_API_VERSION}/${process.env.META_PAGE_ID}/feed`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message,
      access_token: process.env.META_PAGE_ACCESS_TOKEN,
    }),
  });

  if (!res.ok) {
    const errorBody = await res.text();
    throw new Error(`Échec publication Meta Graph API (${res.status}): ${errorBody}`);
  }

  const body = await res.json();
  const record = {
    id: nanoid(),
    draft_id: draft.id,
    channel: 'facebook',
    external_post_id: body.id || null,
    dry_run: 0,
  };
  insertPublication.run(record);
  db.prepare(`UPDATE content_drafts SET status = 'published' WHERE id = ?`).run(draft.id);
  return record;
}

/**
 * Récupère les insights (impressions, clics) d'une publication via Graph
 * API. Nécessite un token valide — retourne null en dry-run.
 * Point d'extension phase 3 : brancher ceci sur un cron régulier qui
 * remplit la table `metrics`.
 */
export async function fetchFacebookInsights(externalPostId) {
  if (!hasCredentials() || !externalPostId) return null;
  const url = `https://graph.facebook.com/${GRAPH_API_VERSION}/${externalPostId}/insights?metric=post_impressions,post_clicks&access_token=${process.env.META_PAGE_ACCESS_TOKEN}`;
  const res = await fetch(url);
  if (!res.ok) return null;
  return res.json();
}
