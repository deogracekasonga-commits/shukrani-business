// Sous-agent Analytics — reçoit les ventes remontées par le webhook Chariow
// et les métriques Instagram (quand un token Meta est configuré), calcule
// le ROI par post/catégorie et génère le rapport hebdomadaire.
import { config } from '../lib/config.js';
import {
  listSalesBetween,
  listPublishedPostsBetween,
  insertWeeklyReport,
} from '../db/repository.js';
import { getMediaInsights } from '../integrations/meta.js';

/** Semaine ISO (lundi-dimanche, UTC) contenant `date`. */
export function getIsoWeekInfo(date = new Date()) {
  const monday = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNum = (monday.getUTCDay() + 6) % 7; // lundi = 0
  monday.setUTCDate(monday.getUTCDate() - dayNum);

  const nextMonday = new Date(monday);
  nextMonday.setUTCDate(monday.getUTCDate() + 7);

  const thursday = new Date(monday);
  thursday.setUTCDate(monday.getUTCDate() + 3);
  const isoYear = thursday.getUTCFullYear();
  const jan1 = new Date(Date.UTC(isoYear, 0, 1));
  const weekNum = Math.ceil((((thursday - jan1) / 86400000) + 1) / 7);

  return {
    label: `${isoYear}-W${String(weekNum).padStart(2, '0')}`,
    startIso: monday.toISOString(),
    endIso: nextMonday.toISOString(),
  };
}

/**
 * Calcule et enregistre le rapport de la semaine ISO contenant `referenceDate`
 * (par défaut : maintenant). Peut être régénéré plusieurs fois pour la même
 * semaine (par ex. après de nouvelles ventes) — insère une nouvelle ligne à
 * chaque appel, `listWeeklyReports` renvoie la plus récente en premier.
 */
export async function generateWeeklyReport(referenceDate = new Date()) {
  const { label, startIso, endIso } = getIsoWeekInfo(referenceDate);

  const sales = listSalesBetween(startIso, endIso);
  const posts = listPublishedPostsBetween(startIso, endIso);

  const ventesTotales = round2(sales.reduce((sum, s) => sum + s.montant, 0));

  const caParCategorie = {};
  for (const sale of sales) {
    const cat = sale.categorie || 'non-categorise';
    caParCategorie[cat] = round2((caParCategorie[cat] || 0) + sale.montant);
  }

  const ventesParPost = new Map();
  for (const sale of sales) {
    if (!sale.attributed_post_id) continue;
    const entry = ventesParPost.get(sale.attributed_post_id) || { ventes: 0, nombre: 0 };
    entry.ventes += sale.montant;
    entry.nombre += 1;
    ventesParPost.set(sale.attributed_post_id, entry);
  }

  const budgetParPost =
    config.adBudgetWeeklyCap > 0 && posts.length > 0 ? config.adBudgetWeeklyCap / posts.length : 0;

  const topPosts = [];
  for (const post of posts) {
    const attribue = ventesParPost.get(post.id) || { ventes: 0, nombre: 0 };
    let insights = null;
    if (config.meta.pageAccessToken) {
      try {
        insights = await getMediaInsights(post.external_post_id);
      } catch (error) {
        console.warn(`[analytics] insights indisponibles pour le post ${post.id} :`, error.message);
      }
    }
    topPosts.push({
      post_id: post.id,
      produit_nom: post.produit_nom,
      categorie: post.categorie,
      url_instagram: post.url_instagram,
      utm_link: post.utm_link,
      ventes: round2(attribue.ventes),
      nombre_ventes: attribue.nombre,
      roi: budgetParPost > 0 ? round2(attribue.ventes / budgetParPost) : null,
      insights,
    });
  }
  topPosts.sort((a, b) => b.ventes - a.ventes);

  return insertWeeklyReport({
    semaine: label,
    ventesTotales,
    caParCategorie,
    topPosts,
  });
}

/** Recommandations simples, calculées à l'affichage à partir d'un rapport stocké. */
export function buildRecommendations(report) {
  const topPosts = JSON.parse(report.top_posts);
  const recommandations = [];

  if (report.ventes_totales === 0) {
    recommandations.push(
      "Aucune vente cette semaine — teste une nouvelle accroche ou vérifie que le lien en bio est bien à jour."
    );
  }

  const best = topPosts.find((p) => p.ventes > 0);
  if (best) {
    recommandations.push(
      `« ${best.produit_nom} » a généré le plus de ventes (${best.ventes} $) cette semaine — réutilise cet angle pour de prochains posts.`
    );
  }

  const withRoi = topPosts.filter((p) => p.roi !== null);
  const roiFaible = withRoi.filter((p) => p.roi < 1);
  if (withRoi.length > 0 && roiFaible.length === withRoi.length) {
    recommandations.push(
      'Le ROI est inférieur à 1 sur tous les posts sponsorisés cette semaine — réévalue le budget publicitaire ou les visuels/accroches utilisés.'
    );
  }

  if (recommandations.length === 0) {
    recommandations.push('Rien d\'anormal à signaler cette semaine — continue sur cette lancée.');
  }

  return recommandations;
}

function round2(n) {
  return Math.round(n * 100) / 100;
}
