import { db, getSetting } from '../db/index.js';

function safeCtr(clicks, impressions) {
  if (!impressions) return 0;
  return Number(((clicks / impressions) * 100).toFixed(2));
}

/**
 * Rapport hebdomadaire : posts publiés/semaine par catégorie, CTR,
 * ventes attribuées par canal/catégorie, temps gagné estimé.
 * cf. ARCHITECTURE.md §6 "Mesure du succès".
 */
export function weeklyReport() {
  const postsByCategory = db
    .prepare(
      `SELECT categories.name AS category, content_drafts.channel, COUNT(*) AS count
       FROM publications
       JOIN content_drafts ON content_drafts.id = publications.draft_id
       JOIN categories ON categories.id = content_drafts.category_id
       WHERE publications.published_at >= datetime('now', '-7 days')
       GROUP BY categories.name, content_drafts.channel`
    )
    .all();

  const clicksAndImpressions = db
    .prepare(
      `SELECT COALESCE(SUM(clicks), 0) AS clicks, COALESCE(SUM(impressions), 0) AS impressions
       FROM metrics
       JOIN publications ON publications.id = metrics.publication_id
       WHERE publications.published_at >= datetime('now', '-7 days')`
    )
    .get();

  const salesByCategory = db
    .prepare(
      `SELECT categories.name AS category, sales.channel, COUNT(*) AS orders, SUM(sales.amount) AS revenue
       FROM sales
       LEFT JOIN products ON products.id = sales.product_id
       LEFT JOIN categories ON categories.id = products.category_id
       WHERE sales.occurred_at >= datetime('now', '-7 days')
       GROUP BY categories.name, sales.channel`
    )
    .all();

  const draftsCreatedThisWeek = db
    .prepare(`SELECT COUNT(*) AS count FROM content_drafts WHERE created_at >= datetime('now', '-7 days')`)
    .get().count;

  const minutesSavedPerDraft = Number(getSetting('minutes_saved_per_draft', '25'));
  const totalRevenue = salesByCategory.reduce((sum, s) => sum + (s.revenue || 0), 0);

  return {
    generatedAt: new Date().toISOString(),
    postsByCategory,
    ctr: safeCtr(clicksAndImpressions.clicks, clicksAndImpressions.impressions),
    clicks: clicksAndImpressions.clicks,
    impressions: clicksAndImpressions.impressions,
    salesByCategory,
    totalRevenue,
    draftsCreatedThisWeek,
    estimatedMinutesSaved: draftsCreatedThisWeek * minutesSavedPerDraft,
  };
}

/**
 * Comparaison des accroches A/B/C : clics et CTR par variante, toutes
 * catégories confondues. Sert à identifier l'accroche gagnante.
 */
export function abTestReport() {
  const rows = db
    .prepare(
      `SELECT content_drafts.ab_group,
              categories.name AS category,
              COUNT(DISTINCT publications.id) AS publications_count,
              COALESCE(SUM(metrics.impressions), 0) AS impressions,
              COALESCE(SUM(metrics.clicks), 0) AS clicks
       FROM content_drafts
       JOIN categories ON categories.id = content_drafts.category_id
       LEFT JOIN publications ON publications.draft_id = content_drafts.id
       LEFT JOIN metrics ON metrics.publication_id = publications.id
       GROUP BY content_drafts.ab_group, categories.name
       ORDER BY categories.name, content_drafts.ab_group`
    )
    .all();

  return rows.map((r) => ({ ...r, ctr: safeCtr(r.clicks, r.impressions) }));
}
