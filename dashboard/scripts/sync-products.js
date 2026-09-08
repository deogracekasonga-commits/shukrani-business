import './env.js';
import { listProductsByCategory } from '../src/integrations/chariow.js';
import { upsertProductFromChariow } from '../src/db/repository.js';
import { closePool } from '../src/db/client.js';
import { config } from '../src/lib/config.js';

const products = await listProductsByCategory(config.activeCategory);

for (const product of products) {
  const id = await upsertProductFromChariow(product);
  console.log(`✔ ${product.nom} (${product.prix}$) → id local ${id}`);
}

console.log(`\n${products.length} produit(s) synchronisé(s) pour la catégorie "${config.activeCategory}".`);
if (!config.chariow.apiKey) {
  console.log('(mode dry-run — CHARIOW_API_KEY absente, données de démo utilisées)');
}

await closePool();
