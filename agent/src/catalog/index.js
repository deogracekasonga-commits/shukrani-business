import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { categories as seedCategories, products as seedProducts } from './seed-data.js';

export function seedCatalog() {
  const insertCategory = db.prepare(`
    INSERT INTO categories (id, slug, name, marketing_angle, tone, target_audience, active)
    VALUES (@id, @slug, @name, @marketing_angle, @tone, @target_audience, @active)
    ON CONFLICT(slug) DO UPDATE SET
      name = excluded.name,
      marketing_angle = excluded.marketing_angle,
      tone = excluded.tone,
      target_audience = excluded.target_audience,
      active = excluded.active
  `);

  for (const cat of seedCategories) {
    const existing = db.prepare('SELECT id FROM categories WHERE slug = ?').get(cat.slug);
    insertCategory.run({
      id: existing?.id || nanoid(),
      slug: cat.slug,
      name: cat.name,
      marketing_angle: cat.marketing_angle,
      tone: cat.tone,
      target_audience: cat.target_audience,
      active: cat.active ? 1 : 0,
    });
  }

  const insertProduct = db.prepare(`
    INSERT INTO products (id, category_id, title, description, price_usd, chariow_url, cover_image_url, file_path)
    SELECT @id, categories.id, @title, @description, @price_usd, @chariow_url, @cover_image_url, @file_path
    FROM categories WHERE categories.slug = @categorySlug
    ON CONFLICT(id) DO NOTHING
  `);

  for (const product of seedProducts) {
    const existing = db
      .prepare('SELECT id FROM products WHERE chariow_url = ?')
      .get(product.chariow_url);
    if (existing) continue;
    insertProduct.run({ id: nanoid(), ...product });
  }
}

export function listActiveCategories() {
  return db.prepare('SELECT * FROM categories WHERE active = 1').all();
}

export function listCategories() {
  return db.prepare('SELECT * FROM categories ORDER BY name').all();
}

export function getCategoryBySlug(slug) {
  return db.prepare('SELECT * FROM categories WHERE slug = ?').get(slug);
}

export function listProductsByCategory(categoryId) {
  return db.prepare('SELECT * FROM products WHERE category_id = ?').all(categoryId);
}

export function listAllProducts() {
  return db
    .prepare(
      `SELECT products.*, categories.name AS category_name, categories.slug AS category_slug
       FROM products JOIN categories ON categories.id = products.category_id
       ORDER BY products.created_at`
    )
    .all();
}
