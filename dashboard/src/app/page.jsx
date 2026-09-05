import { getDb } from '../db/client.js';
import { config } from '../lib/config.js';
import { listRecentSales } from '../db/repository.js';

export default function HomePage() {
  const db = getDb();
  const productCount = db.prepare('SELECT COUNT(*) AS n FROM products').get().n;
  const draftCount = db.prepare('SELECT COUNT(*) AS n FROM content_drafts').get().n;
  const sales = listRecentSales(10);

  return (
    <main style={{ maxWidth: 720, margin: '4rem auto', padding: '0 1.5rem' }}>
      <h1>Agent IA Marketing — Shukrani Business</h1>
      <p>
        Étape 2 (intégration Chariow) en place. Catégorie active :{' '}
        <strong>{config.activeCategory}</strong>.
      </p>
      <ul>
        <li>Produits en base : {productCount}</li>
        <li>Brouillons de contenu : {draftCount}</li>
        <li>Ventes enregistrées : {sales.length}</li>
      </ul>

      <h2>Ventes récentes</h2>
      {sales.length === 0 ? (
        <p style={{ color: '#666' }}>
          Aucune vente pour l&apos;instant. Lance <code>npm run sync:products</code> puis{' '}
          <code>npm run test:fake-sale</code> pour tester le pipeline webhook.
        </p>
      ) : (
        <table style={{ borderCollapse: 'collapse', width: '100%' }}>
          <thead>
            <tr>
              <th style={cellStyle}>Produit</th>
              <th style={cellStyle}>Catégorie</th>
              <th style={cellStyle}>Montant</th>
              <th style={cellStyle}>Date</th>
            </tr>
          </thead>
          <tbody>
            {sales.map((sale) => (
              <tr key={sale.id}>
                <td style={cellStyle}>{sale.produit_nom ?? '(produit inconnu)'}</td>
                <td style={cellStyle}>{sale.categorie ?? '—'}</td>
                <td style={cellStyle}>{sale.montant} $</td>
                <td style={cellStyle}>{sale.date}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <p style={{ color: '#666', marginTop: '2rem' }}>
        Prochaines étapes : agent contenu, dashboard de validation, publication
        Instagram, agent analytics.
      </p>
    </main>
  );
}

const cellStyle = { textAlign: 'left', borderBottom: '1px solid #ddd', padding: '0.4rem 0.6rem' };
