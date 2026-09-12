import Link from 'next/link';
import './globals.css';

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
      <body>
        <header className="sb-header">
          <nav className="sb-header-inner">
            <Link href="/" className="sb-brand">
              <img src="/logo-icon.png" alt="Shukrani Business" className="sb-logo-badge" />
              <span className="sb-brand-text">Shukrani Business</span>
            </Link>
            <div className="sb-nav-links">
              {NAV_LINKS.map((link) => (
                <Link key={link.href} href={link.href} className="sb-nav-link">
                  {link.label}
                </Link>
              ))}
            </div>
          </nav>
        </header>
        {children}
      </body>
    </html>
  );
}
