import { getDb } from '../db/client.js';
import { config } from '../lib/config.js';

export default function HomePage() {
  const db = getDb();
  const productCount = db.prepare('SELECT COUNT(*) AS n FROM products').get().n;
  const draftCount = db.prepare('SELECT COUNT(*) AS n FROM content_drafts').get().n;
  const salesCount = db.prepare('SELECT COUNT(*) AS n FROM sales').get().n;

  return (
    <main style={{ maxWidth: 720, margin: '4rem auto', padding: '0 1.5rem' }}>
      <h1>Agent IA Marketing — Shukrani Business</h1>
      <p>
        Étape 1 (setup projet + base de données) en place. Catégorie active :{' '}
        <strong>{config.activeCategory}</strong>.
      </p>
      <ul>
        <li>Produits en base : {productCount}</li>
        <li>Brouillons de contenu : {draftCount}</li>
        <li>Ventes enregistrées : {salesCount}</li>
      </ul>
      <p style={{ color: '#666' }}>
        Prochaines étapes : intégration Chariow (API + webhook), agent contenu,
        dashboard de validation, publication Instagram, agent analytics.
      </p>
    </main>
  );
}
