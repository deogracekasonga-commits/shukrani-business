'use server';

import { revalidatePath } from 'next/cache';
import { listProductsByCategory } from '../integrations/chariow.js';
import { upsertProductFromChariow } from '../db/repository.js';
import { requestWeeklyContent } from '../agents/orchestrator.js';
import { config } from '../lib/config.js';

/** Synchronise les produits Chariow de la catégorie active (dry-run sans CHARIOW_API_KEY). */
export async function syncProducts() {
  const products = await listProductsByCategory(config.activeCategory);
  for (const product of products) {
    await upsertProductFromChariow(product);
  }
  revalidatePath('/');
  revalidatePath('/drafts');
  revalidatePath('/calendar');
}

/** Génère 3-5 nouveaux brouillons pour la catégorie active. */
export async function generateDrafts() {
  await requestWeeklyContent({ targetCount: 4 });
  revalidatePath('/');
  revalidatePath('/drafts');
  revalidatePath('/calendar');
}
