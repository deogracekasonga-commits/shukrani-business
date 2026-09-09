import Link from 'next/link';
import { query, initDb } from '../db/client.js';
import { config } from '../lib/config.js';
import { listContentDrafts, listRecentSales, getSetting } from '../db/repository.js';
import { syncProducts, generateDrafts } from './actions.js';

// Toujours rendu à la demande (jamais prérendu au build) — les données
// viennent d'une base Postgres externe, pas d'un fichier local.
export const dynamic = 'force-dynamic';

function parseStatus(raw) {
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export default async function HomePage() {
  await initDb();
  const [{ n: productCount }] = await query('SELECT COUNT(*)::int AS n FROM products');
  const pendingCount = (await listContentDrafts({ statut: 'brouillon', limit: 1000 })).length;
  const approvedCount = (await listContentDrafts({ statut: 'valide', limit: 1000 })).length;
  const salesCount = (await listRecentSales(1000)).length;
  const syncStatus = parseStatus(await getSetting('last_sync_status'));
  const draftsStatus = parseStatus(await getSetting('last_drafts_status'));

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

      <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', margin: '1.5rem 0' }}>
        <form action={syncProducts}>
          <button type="submit" style={buttonStyle}>
            🔄 Synchroniser les produits Chariow
          </button>
        </form>
        <form action={generateDrafts}>
          <button type="submit" style={buttonStyle}>
            ✨ Générer de nouveaux brouillons
          </button>
        </form>
      </div>

      <StatusBanner label="Dernière synchronisation Chariow" status={syncStatus} />
      <StatusBanner label="Dernière génération de brouillons" status={draftsStatus} />

      <p style={{ color: '#666' }}>
        Prochaines étapes : publication Instagram (Étape 5), agent analytics et
        rapport hebdomadaire (Étape 6).
      </p>
    </main>
  );
}

function StatusBanner({ label, status }) {
  if (!status) return null;
  const when = new Date(status.at).toLocaleString('fr-FR');
  return (
    <div
      style={{
        border: `1px solid ${status.ok ? '#1a7f37' : '#c92a2a'}`,
        background: status.ok ? '#f0fdf4' : '#fff5f5',
        borderRadius: 8,
        padding: '0.75rem 1rem',
        margin: '0.75rem 0',
        fontSize: '0.9rem',
      }}
    >
      <strong>{label}</strong> — {when}
      <br />
      {status.ok ? (
        <span>✅ OK{status.count !== undefined ? ` (${status.count} produit(s))` : ''}</span>
      ) : (
        <span>❌ Erreur : {status.error}</span>
      )}
      {status.diagnostic && (
        <div style={{ marginTop: '0.5rem', color: '#555' }}>
          ℹ️ {status.diagnostic.totalProduitsChariow} produit(s) au total chez Chariow.
          {status.diagnostic.categoriesVues.length > 0 ? (
            <> Catégories vues : {status.diagnostic.categoriesVues.map((c) => `"${c}"`).join(', ')}.</>
          ) : (
            <> Aucune catégorie détectée sur ces produits.</>
          )}
        </div>
      )}
    </div>
  );
}

const buttonStyle = {
  background: '#0a66c2',
  color: 'white',
  border: 'none',
  borderRadius: 6,
  padding: '0.6rem 1.1rem',
  cursor: 'pointer',
};

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
