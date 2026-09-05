import Link from 'next/link';
import { getDb } from '../db/client.js';
import { config } from '../lib/config.js';
import { listContentDrafts, listRecentSales } from '../db/repository.js';

export default function HomePage() {
  const db = getDb();
  const productCount = db.prepare('SELECT COUNT(*) AS n FROM products').get().n;
  const pendingCount = listContentDrafts({ statut: 'brouillon', limit: 1000 }).length;
  const approvedCount = listContentDrafts({ statut: 'valide', limit: 1000 }).length;
  const salesCount = listRecentSales(1000).length;

  return (
    <main style={{ maxWidth: 720, margin: '2.5rem auto', padding: '0 1.5rem' }}>
      <h1>Agent IA Marketing — Shukrani Business</h1>
      <p>
        Étape 4 (dashboard de validation) en place. Catégorie active :{' '}
        <strong>{config.activeCategory}</strong>.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem', margin: '1.5rem 0' }}>
        <StatCard label="Produits en base" value={productCount} />
        <StatCard label="Brouillons à valider" value={pendingCount} href="/drafts" highlight={pendingCount > 0} />
        <StatCard label="Brouillons approuvés" value={approvedCount} href="/calendar" />
        <StatCard label="Ventes enregistrées" value={salesCount} href="/sales" />
      </div>

      <p style={{ color: '#666' }}>
        Prochaines étapes : publication Instagram (Étape 5), agent analytics et
        rapport hebdomadaire (Étape 6).
      </p>
    </main>
  );
}

function StatCard({ label, value, href, highlight }) {
  const content = (
    <div
      style={{
        border: `1px solid ${highlight ? '#1a7f37' : '#ddd'}`,
        borderRadius: 8,
        padding: '1rem',
        color: 'inherit',
        textDecoration: 'none',
      }}
    >
      <div style={{ fontSize: '1.75rem', fontWeight: 600 }}>{value}</div>
      <div style={{ color: '#666' }}>{label}</div>
    </div>
  );
  return href ? <Link href={href}>{content}</Link> : content;
}
