import Link from 'next/link';

export const metadata = {
  title: 'Shukrani Business — Agent Marketing',
  description: 'Dashboard de validation de l\'agent IA marketing (Chariow + Instagram)',
};

const NAV_LINKS = [
  { href: '/', label: 'Vue d\'ensemble' },
  { href: '/drafts', label: 'Brouillons à valider' },
  { href: '/calendar', label: 'Calendrier éditorial' },
  { href: '/sales', label: 'Ventes' },
  { href: '/reports', label: 'Rapport hebdomadaire' },
];

export default function RootLayout({ children }) {
  return (
    <html lang="fr">
      <body style={{ fontFamily: 'system-ui, sans-serif', margin: 0, color: '#1a1a1a' }}>
        <nav
          style={{
            display: 'flex',
            gap: '1.25rem',
            padding: '1rem 1.5rem',
            borderBottom: '1px solid #e2e2e2',
          }}
        >
          <strong>Shukrani Business</strong>
          {NAV_LINKS.map((link) => (
            <Link key={link.href} href={link.href} style={{ color: '#1a1a1a' }}>
              {link.label}
            </Link>
          ))}
        </nav>
        {children}
      </body>
    </html>
  );
}
