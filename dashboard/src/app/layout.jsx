export const metadata = {
  title: 'Shukrani Business — Agent Marketing',
  description: 'Dashboard de validation de l\'agent IA marketing (Chariow + Instagram)',
};

export default function RootLayout({ children }) {
  return (
    <html lang="fr">
      <body style={{ fontFamily: 'system-ui, sans-serif', margin: 0 }}>{children}</body>
    </html>
  );
}
