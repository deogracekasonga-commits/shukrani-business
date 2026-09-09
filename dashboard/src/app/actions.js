'use server';

import { revalidatePath } from 'next/cache';
import { listProductsByCategory, fetchAllProductsRaw } from '../integrations/chariow.js';
import { upsertProductFromChariow, setSetting } from '../db/repository.js';
import { requestWeeklyContent } from '../agents/orchestrator.js';
import { config } from '../lib/config.js';

// Les erreurs des Server Actions ne s'affichent pas clairement dans le
// navigateur (surtout en production, sur mobile). On enregistre donc le
// résultat de chaque action dans `settings` pour l'afficher directement sur
// le dashboard, sans avoir besoin de consulter les logs Vercel.
async function recordActionResult(key, result) {
  await setSetting(key, JSON.stringify({ ...result, at: new Date().toISOString() }));
}

/** Synchronise les produits Chariow de la catégorie active (dry-run sans CHARIOW_API_KEY). */
export async function syncProducts() {
  try {
    const products = await listProductsByCategory(config.activeCategory);
    for (const product of products) {
      await upsertProductFromChariow(product);
    }

    let diagnostic;
    if (products.length === 0) {
      // Aucun produit dans la catégorie active : on remonte le nombre total
      // de produits vus côté Chariow, les catégories qu'ils portent, et un
      // échantillon brut du 1er produit — pour repérer un écart de nommage
      // ou un champ mal deviné sans aller-retour supplémentaire.
      const { rawItems, normalized } = await fetchAllProductsRaw();
      diagnostic = {
        totalProduitsChariow: rawItems.length,
        categoriesVues: [...new Set(normalized.map((p) => p.categorie).filter(Boolean))],
        premierProduitBrut: rawItems[0] ? JSON.stringify(rawItems[0]).slice(0, 600) : null,
      };
    }

    await recordActionResult('last_sync_status', { ok: true, count: products.length, diagnostic });
  } catch (err) {
    await recordActionResult('last_sync_status', { ok: false, error: String(err?.message || err) });
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
