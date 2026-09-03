// Template éditorial pour la catégorie "Business & Entrepreneuriat".
// Chaque catégorie a son propre fichier de template : angle, ton et
// structure de contenu différents (cf. ARCHITECTURE.md §7 et
// consigne "un angle marketing propre à chaque catégorie").

export const hooks = [
  {
    group: 'A',
    text: (p) => `Et si ${p.title.toLowerCase().startsWith('50') ? 'ces' : 'ce'} 50 idées changeaient ta façon de voir le business ?`,
  },
  {
    group: 'B',
    text: () => `Tu cherches une activité qui rapporte, sans attendre 10 ans ? Commence ici.`,
  },
  {
    group: 'C',
    text: () => `L'IA n'est pas réservée aux experts. Voici 50 façons concrètes de t'en servir pour gagner ta vie.`,
  },
];

export function buildCaption({ product, category, hook }) {
  return [
    hook,
    '',
    `📖 ${product.title}`,
    product.description,
    '',
    `💡 Pensé pour les entrepreneurs de Kinshasa et d'Afrique qui veulent agir maintenant, avec les moyens du bord.`,
    '',
    `👉 Disponible dès maintenant en version numérique — lien en bio / commentaire.`,
    `💵 ${product.price_usd} $`,
  ].join('\n');
}

export const hashtags = [
  '#ShukraniBusiness',
  '#EntrepreneuriatAfricain',
  '#BusinessRDC',
  '#Kinshasa',
  '#IntelligenceArtificielle',
  '#Ebook',
  '#ReussiteEntrepreneuriale',
];

export function buildVideoScript({ product, hook }) {
  return [
    `[0-3s] Accroche (face caméra ou texte à l'écran) : "${hook}"`,
    `[3-10s] Problème : "Beaucoup pensent qu'il faut du capital ou un diplôme pour démarrer un business avec l'IA. Faux."`,
    `[10-20s] Solution : montrer l'ebook "${product.title}" — feuilleter 2-3 pages, citer 2 exemples concrets d'idées.`,
    `[20-25s] Preuve/bénéfice : "50 idées testées, applicables avec un simple smartphone."`,
    `[25-30s] Appel à l'action : "Lien en bio pour te le procurer dès maintenant, à ${product.price_usd}$."`,
  ].join('\n');
}
