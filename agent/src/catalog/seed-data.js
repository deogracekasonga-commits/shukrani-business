// Données de départ du catalogue. `active: true` = catégorie incluse dans
// le périmètre courant de génération de contenu (MVP = 1 seule catégorie,
// cf. ARCHITECTURE.md). Les autres catégories sont déjà modélisées pour
// la phase 2, mais aucun contenu n'est généré pour elles tant qu'elles ne
// sont pas activées.

export const categories = [
  {
    slug: 'business-entrepreneuriat',
    name: 'Business & Entrepreneuriat',
    marketing_angle:
      "Des opportunités concrètes pour démarrer ou faire grandir une activité en Afrique, avec l'IA comme levier accessible à tous.",
    tone: 'direct, motivant, orienté action et résultats concrets',
    target_audience:
      'Entrepreneurs et futurs entrepreneurs à Kinshasa et en Afrique francophone, 20-40 ans, actifs sur mobile',
    active: true,
  },
  {
    slug: 'developpement-personnel',
    name: 'Développement personnel',
    marketing_angle:
      'Construire des bases intérieures solides (confiance, discipline, clarté) pour avancer dans la vie et les affaires.',
    tone: 'chaleureux, inspirant, proche, ton de mentor bienveillant',
    target_audience:
      'Jeunes adultes en recherche de croissance personnelle, forte présence sur Instagram/TikTok',
    active: false,
  },
  {
    slug: 'informatique-bureautique',
    name: 'Informatique & Bureautique',
    marketing_angle:
      "Des compétences pratiques et immédiatement utiles (Word, outils numériques) pour l'école, le travail ou une activité indépendante.",
    tone: 'pédagogue, rassurant, simple, orienté "je peux le faire"',
    target_audience:
      "Étudiants, jeunes diplômés et travailleurs cherchant à se former rapidement à moindre coût",
    active: false,
  },
];

export const products = [
  {
    categorySlug: 'business-entrepreneuriat',
    title: '50 opportunités business IA pour entrepreneurs africains',
    description:
      "Un guide pratique de 50 idées de business exploitant l'intelligence artificielle, adaptées au contexte et aux ressources disponibles en Afrique.",
    price_usd: 9,
    chariow_url: 'https://chariow.com/shukrani-business/50-opportunites-business-ia',
    cover_image_url: null,
    file_path: '50-opportunites-business-ia-entrepreneurs-africains.pdf',
  },
  {
    categorySlug: 'developpement-personnel',
    title: 'Construire une confiance solide',
    description:
      "Un ebook pour identifier ce qui freine la confiance en soi et bâtir, étape par étape, une confiance durable.",
    price_usd: 6,
    chariow_url: 'https://chariow.com/shukrani-business/construire-une-confiance-solide',
    cover_image_url: null,
    file_path: 'construire-une-confiance-solide.pdf',
  },
  {
    categorySlug: 'informatique-bureautique',
    title: 'Initiation informatique — Word',
    description:
      'Un guide simple et progressif pour apprendre à utiliser Microsoft Word, de la prise en main aux mises en page professionnelles.',
    price_usd: 5,
    chariow_url: 'https://chariow.com/shukrani-business/initiation-informatique-word',
    cover_image_url: null,
    file_path: 'initiation-informatique-word.pdf',
  },
];
