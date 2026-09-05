// Configuration centralisée — jamais de clé en dur, tout vient de l'environnement.
// Ce module est le seul point que agents/ et integrations/ doivent lire pour
// la config ; ça évite de disperser process.env.* dans tout le code.

export const config = {
  chariow: {
    apiBaseUrl: process.env.CHARIOW_API_BASE_URL || 'https://api.chariow.com/v1',
    apiKey: process.env.CHARIOW_API_KEY || '', // Bearer token — jamais exposé côté client
    webhookSecret: process.env.CHARIOW_WEBHOOK_SECRET || '',
  },
  meta: {
    appId: process.env.META_APP_ID || '',
    appSecret: process.env.META_APP_SECRET || '',
    pageAccessToken: process.env.META_PAGE_ACCESS_TOKEN || '',
    igUserId: process.env.META_IG_USER_ID || '',
    // Image utilisée quand un produit n'a pas encore de visuel propre
    // (products.image_url) — Instagram exige une image pour tout post.
    defaultImageUrl:
      process.env.META_DEFAULT_IMAGE_URL ||
      'https://placehold.co/1080x1080/1a2b4c/ffffff.png?text=Shukrani+Business',
  },
  anthropic: {
    apiKey: process.env.ANTHROPIC_API_KEY || '',
  },
  activeCategory: process.env.ACTIVE_CATEGORY || 'developpement-personnel',
  adBudgetWeeklyCap: Number(process.env.AD_BUDGET_WEEKLY_CAP || 0),
  autoPublishInstagram: process.env.AUTO_PUBLISH_INSTAGRAM === 'true',
};

export function assertServerOnly() {
  if (typeof window !== 'undefined') {
    throw new Error('config.js contient des secrets serveur : ne jamais importer côté client.');
  }
}
