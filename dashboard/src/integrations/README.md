# Intégrations (à implémenter aux étapes suivantes)

- `chariow.js` — client API REST (`GET /v1/products`, historique des ventes) +
  vérification de signature du webhook `sale.completed` (Étape 2).
- `meta.js` — Meta Graph API (publication Instagram, insights) (Étape 5).

Toute clé API secrète vit uniquement dans `process.env` (voir `src/lib/config.js`
et `.env.example`) — jamais en dur, jamais exposée au frontend Next.js
(pas de préfixe `NEXT_PUBLIC_`).
