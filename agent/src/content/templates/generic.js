// Template générique utilisé pour toute catégorie qui n'a pas encore son
// propre fichier dans src/content/templates/. Sert de filet de sécurité
// pour la phase 2 (nouvelles catégories) tant qu'un angle marketing dédié
// n'a pas été écrit.

export const hooks = [
  { group: 'A', text: (p) => `Découvre "${p.title}", disponible dès maintenant.` },
  { group: 'B', text: (p) => `Ce que tu cherchais est peut-être dans "${p.title}".` },
  { group: 'C', text: () => `Nouveau chez Shukrani Business 👇` },
];

export function buildCaption({ product, category, hook }) {
  return [
    hook,
    '',
    `📖 ${product.title}`,
    product.description,
    '',
    `👉 Disponible dès maintenant — lien en bio / commentaire.`,
    `💵 ${product.price_usd} $`,
  ].join('\n');
}

export const hashtags = ['#ShukraniBusiness', '#Ebook', '#Kinshasa'];

export function buildVideoScript({ product, hook }) {
  return [
    `[0-3s] Accroche : "${hook}"`,
    `[3-15s] Présentation rapide de "${product.title}" et de ce que ça apporte.`,
    `[15-25s] Preuve / bénéfice concret pour le lecteur.`,
    `[25-30s] Appel à l'action : "Lien en bio, à ${product.price_usd}$."`,
  ].join('\n');
}
