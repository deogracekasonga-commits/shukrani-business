import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { listActiveCategories, listProductsByCategory } from '../catalog/index.js';
import { generateDraftVariants } from './generator.js';

function withRefParam(url, draftId) {
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}ref=${draftId}`;
}

function alreadyGeneratedThisWeek(productId, channel, abGroup) {
  const row = db
    .prepare(
      `SELECT id FROM content_drafts
       WHERE product_id = ? AND channel = ? AND ab_group = ?
         AND created_at >= datetime('now', '-7 days')`
    )
    .get(productId, channel, abGroup);
  return Boolean(row);
}

const insertDraft = db.prepare(`
  INSERT INTO content_drafts
    (id, product_id, category_id, channel, format, ab_group, hook, caption, hashtags, video_script, cta_link, status)
  VALUES
    (@id, @product_id, @category_id, @channel, @format, @ab_group, @hook, @caption, @hashtags, @video_script, @cta_link, 'draft')
`);

/**
 * Génère les brouillons de la semaine pour les catégories actives
 * (MVP = 1 catégorie). N'écrase jamais un brouillon existant, ne republie
 * pas deux fois la même variante dans la même semaine.
 */
export function generateWeeklyDrafts({ channel = 'facebook' } = {}) {
  const created = [];

  for (const category of listActiveCategories()) {
    const products = listProductsByCategory(category.id);

    for (const product of products) {
      const variants = generateDraftVariants({ product, category, channel });

      for (const variant of variants) {
        if (alreadyGeneratedThisWeek(product.id, channel, variant.ab_group)) continue;

        const id = nanoid();
        const record = {
          id,
          product_id: product.id,
          category_id: category.id,
          channel: variant.channel,
          format: variant.format,
          ab_group: variant.ab_group,
          hook: variant.hook,
          caption: variant.caption,
          hashtags: variant.hashtags,
          video_script: variant.video_script,
          cta_link: withRefParam(product.chariow_url, id),
        };
        insertDraft.run(record);
        created.push(record);
      }
    }
  }

  return created;
}

export function listDraftsByStatus(status) {
  return db
    .prepare(
      `SELECT content_drafts.*, products.title AS product_title, categories.name AS category_name
       FROM content_drafts
       JOIN products ON products.id = content_drafts.product_id
       JOIN categories ON categories.id = content_drafts.category_id
       WHERE content_drafts.status = ?
       ORDER BY content_drafts.created_at DESC`
    )
    .all(status);
}

export function getDraft(id) {
  return db
    .prepare(
      `SELECT content_drafts.*, products.title AS product_title, products.chariow_url
       FROM content_drafts JOIN products ON products.id = content_drafts.product_id
       WHERE content_drafts.id = ?`
    )
    .get(id);
}

export function updateDraftStatus(id, status, { reviewedBy = 'deograce', caption, scheduledAt } = {}) {
  db.prepare(
    `UPDATE content_drafts
     SET status = ?, reviewed_by = ?, reviewed_at = datetime('now'),
         caption = COALESCE(?, caption),
         scheduled_at = COALESCE(?, scheduled_at)
     WHERE id = ?`
  ).run(status, reviewedBy, caption ?? null, scheduledAt ?? null, id);
}
