import { listRecentSales } from '../../db/repository.js';

export default function SalesPage() {
  const sales = listRecentSales(100);

  return (
    <main style={{ maxWidth: 760, margin: '2.5rem auto', padding: '0 1.5rem' }}>
      <h1>Ventes</h1>
      <p style={{ color: '#666' }}>Alimenté en temps réel par le webhook Chariow.</p>

      {sales.length === 0 ? (
        <p>
          Aucune vente pour l&apos;instant. Lance <code>npm run test:fake-sale</code> pour
          tester le pipeline webhook.
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
    </main>
  );
}

const cellStyle = { textAlign: 'left', borderBottom: '1px solid #ddd', padding: '0.4rem 0.6rem' };
