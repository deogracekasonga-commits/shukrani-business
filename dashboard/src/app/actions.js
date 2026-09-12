'use server';

import { revalidatePath } from 'next/cache';
import { listAllProducts, fetchAllProductsRaw } from '../integrations/chariow.js';
import { upsertProductFromChariow, setSetting } from '../db/repository.js';
import { requestWeeklyContent } from '../agents/orchestrator.js';

// Les erreurs des Server Actions ne s'affichent pas clairement dans le
// navigateur (surtout en production, sur mobile). On enregistre donc le
// résultat de chaque action dans `settings` pour l'afficher directement sur
// le dashboard, sans avoir besoin de consulter les logs Vercel.
async function recordActionResult(key, result) {
  await setSetting(key, JSON.stringify({ ...result, at: new Date().toISOString() }));
}

/** Repère les champs dont le nom évoque une URL, pour deviner le bon champ sans aller-retour. */
function urlishFields(obj) {
  if (!obj) return 'aucun';
  const matches = Object.entries(obj).filter(([k]) => /url|link|href|slug/i.test(k));
  return matches.length ? matches.map(([k, v]) => `${k}=${JSON.stringify(v)}`).join(', ') : 'aucun';
}

// Aperçu ligne par ligne de chaque champ (valeur tronquée) — un long champ
// comme `description` (HTML) ne doit pas manger tout le budget d'affichage
// et masquer les champs utiles (url, slug, price...) qui le suivent.
function summarizeProduct(raw, maxFieldLen = 50) {
  if (!raw) return null;
  return Object.entries(raw)
    .map(([k, v]) => {
      const asString = typeof v === 'string' ? v : JSON.stringify(v);
      const truncated = asString && asString.length > maxFieldLen ? `${asString.slice(0, maxFieldLen)}…` : asString;
      return `${k}: ${truncated}`;
    })
    .join('\n');
}

async function buildRawDiagnostic() {
  const { rawItems, normalized } = await fetchAllProductsRaw();
  // La "Pack Complet" (1er produit vu jusqu'ici) a un slug null — c'est
  // peut-être spécifique aux bundles. On montre aussi un 2e produit pour
  // comparer.
  return {
    totalProduitsChariow: rawItems.length,
    categoriesVues: [...new Set(normalized.map((p) => p.categorie).filter(Boolean))],
    champsUrlDetectes: urlishFields(rawItems[0]),
    premierProduitResume: summarizeProduct(rawItems[0]),
    deuxiemeProduitResume: summarizeProduct(rawItems[1]),
  };
}

/** Synchronise TOUS les produits Chariow, peu importe leur catégorie (dry-run sans CHARIOW_API_KEY). */
export async function syncProducts() {
  try {
    const products = await listAllProducts();
    for (const product of products) {
      await upsertProductFromChariow(product);
    }

    // Diagnostic si 0 produit (boutique vide ou souci d'accès) — pour
    // ajuster sans aller-retour supplémentaire.
    const diagnostic = products.length === 0 ? await buildRawDiagnostic() : undefined;
    await recordActionResult('last_sync_status', { ok: true, count: products.length, diagnostic });
  } catch (err) {
    // Diagnostic aussi en cas d'erreur (ex. champ mal deviné qui viole une
    // contrainte NOT NULL en base) — même logique.
    const diagnostic = await buildRawDiagnostic().catch(() => undefined);
    await recordActionResult('last_sync_status', { ok: false, error: String(err?.message || err), diagnostic });
  }
  revalidatePath('/');
  revalidatePath('/drafts');
  revalidatePath('/calendar');
}

/** Génère 3-5 nouveaux brouillons pour la catégorie active. */
export async function generateDrafts() {
  try {
    await requestWeeklyContent({ targetCount: 4 });
    await recordActionResult('last_drafts_status', { ok: true });
  } catch (err) {
    await recordActionResult('last_drafts_status', { ok: false, error: String(err?.message || err) });
  }
  revalidatePath('/');
  revalidatePath('/drafts');
  revalidatePath('/calendar');
}
