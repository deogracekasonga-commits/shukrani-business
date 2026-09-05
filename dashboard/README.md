# Agent IA Marketing — Shukrani Business (MVP)

Boutique : livres/ebooks vendus sur Chariow. CEO : Deograce.
Périmètre MVP : 1 catégorie de produits paramétrable, 1 réseau (Instagram
Business via Meta Graph API), 1 intégration ventes (Chariow, API + webhooks
officiels).

## Installation

```bash
cd dashboard
npm install
cp .env.example .env.local   # remplir les clés disponibles (voir commentaires)
npm run db:init               # crée data/agent.db et charge le schéma
npm run dev                    # http://localhost:3000
```

L'agent démarre sans aucune clé API : `CHARIOW_API_KEY`/`META_PAGE_ACCESS_TOKEN`
vides désactivent simplement les appels externes réels (à brancher à l'Étape 2
et à l'Étape 5). Aucune clé secrète n'est jamais exposée côté client — voir
`src/lib/config.js`.

## Structure du projet

```
dashboard/
  src/
    app/            Next.js App Router — dashboard + routes API (webhooks, actions)
    agents/          orchestrateur + 3 sous-agents (contenu, planification, analytics)
    integrations/    clients Chariow (API + webhook) et Meta Graph API (Instagram)
    db/              schéma SQLite (schema.sql) + connexion (client.js)
    lib/             config centralisée (lecture des variables d'environnement)
  scripts/           scripts utilitaires (init-db.js)
  data/              base SQLite locale (gitignored)
```

## Base de données (schéma de départ)

Voir `src/db/schema.sql`. Tables : `products`, `content_drafts`,
`published_posts`, `sales`, `weekly_reports`, `settings` (réglages
modifiables depuis le dashboard : catégorie active, budget pub plafonné,
activation de la publication automatique).

Point clé pour l'attribution des ventes : chaque `published_posts.utm_link`
est le lien Chariow avec des paramètres UTM propres au post ; `sales` peut
être relié à `published_posts` via `attributed_post_id` une fois le webhook
Chariow branché (Étape 2).

## Variables d'environnement

Voir `.env.example`. Résumé :

| Variable | Usage |
|---|---|
| `CHARIOW_API_BASE_URL` | Base URL API Chariow (`https://api.chariow.com/v1`) |
| `CHARIOW_API_KEY` | Bearer token Chariow — secret, jamais côté client |
| `CHARIOW_WEBHOOK_SECRET` | Vérifie l'authenticité du webhook `sale.completed` |
| `META_APP_ID` / `META_APP_SECRET` | App Meta pour Instagram Business |
| `META_PAGE_ACCESS_TOKEN` / `META_IG_USER_ID` | Publication + lecture insights Instagram |
| `ANTHROPIC_API_KEY` | Orchestrateur IA (agent principal, génération de contenu) |
| `ACTIVE_CATEGORY` | Catégorie de produits ciblée par le MVP |
| `AD_BUDGET_WEEKLY_CAP` | Budget publicitaire hebdomadaire plafonné (0 = pas de pub payante) |
| `AUTO_PUBLISH_INSTAGRAM` | `false` par défaut — mode brouillon obligatoire |

## Roadmap (voir la demande initiale pour le détail de chaque étape)

- [x] Étape 1 — Setup projet, base de données, variables d'environnement
- [ ] Étape 2 — Intégration Chariow (API + webhook, test vente factice)
- [ ] Étape 3 — Agent contenu (génération de brouillons)
- [ ] Étape 4 — Dashboard de validation
- [ ] Étape 5 — Intégration Instagram (publication test)
- [ ] Étape 6 — Agent analytics + rapport hebdomadaire
