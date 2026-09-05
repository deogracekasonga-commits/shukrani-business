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

## Étape 2 — Intégration Chariow

- `src/integrations/chariow.js` : client API (`listProductsByCategory`,
  `getSalesHistory`) + vérification de signature webhook HMAC-SHA256.
  ⚠️ chariow.dev est inaccessible depuis cet environnement (bloqué par le
  proxy réseau) : les noms de champs/endpoints n'ont pas pu être vérifiés
  contre la doc réelle et sont ceux de la spec du projet, à confirmer avec
  Deograce (voir commentaires en tête de fichier).
- `src/app/api/webhooks/chariow/route.js` : endpoint qui écoute
  `sale.completed` (alias `order.completed` accepté par précaution). Refuse
  (401) toute requête sans signature valide — jamais de vente non
  authentifiée enregistrée. Idempotent via `sales.chariow_event_id` (UNIQUE) :
  un même événement rejoué est ignoré sans dupliquer la vente.
- Sans `CHARIOW_API_KEY`, tout tourne en dry-run avec 2 produits de démo.

Test de bout en bout :

```bash
npm run sync:products     # charge les produits (démo si pas de clé API) en base
npm run dev                # démarre le serveur (autre terminal)
npm run test:fake-sale     # construit + signe + envoie une vente factice au webhook
```

La page d'accueil (`/`) affiche les ventes récentes reçues.

## Étape 3 — Agent contenu

- `src/agents/content-agent.js` : génère légendes (`format: 'post'`) et
  scripts vidéo courts (`format: 'reel_script'`) à partir de templates par
  catégorie (`src/agents/templates/`). MVP sans coût API — aucune clé
  requise. `generateCaptionText`/`generateVideoScriptText` sont isolées pour
  brancher l'API Claude plus tard (`ANTHROPIC_API_KEY`) sans changer
  l'appelant.
- `src/agents/orchestrator.js` : point d'entrée unique (`requestWeeklyContent`),
  futur point de distribution vers planification (Étape 5) et analytics
  (Étape 6).
- `npm run generate:drafts` : génère 3-5 brouillons (mix légendes + script
  vidéo) pour la catégorie active et les enregistre en base (`brouillon`).
  Testé avec les 2 produits de démo → 4 brouillons cohérents.

## Étape 4 — Dashboard de validation

- `/drafts` : relire, modifier (textarea + « Enregistrer ») puis
  **Approuver** ou **Rejeter** chaque brouillon. Rien n'est publié sans ce
  passage humain. Historique des brouillons déjà traités affiché en dessous.
- `/calendar` : calendrier éditorial (contenu par jour, statut de chacun).
  La programmation d'un créneau de publication précis arrivera avec
  l'Étape 5.
- `/sales` : ventes en temps réel (alimentées par le webhook Chariow).
- `/` : vue d'ensemble avec compteurs et liens vers les 3 pages ci-dessus.

Testé de bout en bout avec Playwright : édition → sauvegarde, approbation,
rejet — chaque action se reflète immédiatement (Server Actions Next.js,
`revalidatePath`).

## Étape 5 — Publication Instagram

- `src/integrations/meta.js` : publication Instagram (Meta Graph API,
  2 étapes : créer le conteneur média puis le publier). Sans
  `META_PAGE_ACCESS_TOKEN`/`META_IG_USER_ID`, dry-run complet (rien n'est
  envoyé à Meta, tout est loggé en console).
- `src/agents/planning-agent.js` : génère le lien Chariow tracké (UTM :
  `utm_source=instagram&utm_medium=social&utm_campaign=<categorie>&utm_content=<draft_id>`)
  et remplace le lien brut par ce lien tracké dans la légende avant
  publication. Refuse de publier un brouillon qui n'est pas `valide`.
- Dashboard `/drafts` : nouvelle section **« Approuvés — prêts à publier »**
  avec bouton **📤 Publier sur Instagram**. Une fois publié, affiche le
  lien Instagram (ou la mention dry-run) et le lien tracké.
- `AUTO_PUBLISH_INSTAGRAM=true` publie automatiquement dès l'approbation
  (toujours après validation humaine explicite) — `false` par défaut,
  publication manuelle via le bouton.
- `products.image_url` (+ `META_DEFAULT_IMAGE_URL` en repli) : Instagram
  exige une image pour tout post.
- Testé de bout en bout (`npm run test:publish` + Playwright sur le
  dashboard) : approbation → publication → lien tracké visible.

⚠️ Comme pour Chariow, `developers.facebook.com`/`graph.facebook.com` n'ont
pas pu être vérifiés depuis cet environnement réseau restreint — l'appel
réel (2 étapes + lecture du permalink) suit la documentation Graph API
standard, à tester avec un vrai token dès qu'il sera disponible.

## Roadmap (voir la demande initiale pour le détail de chaque étape)

- [x] Étape 1 — Setup projet, base de données, variables d'environnement
- [x] Étape 2 — Intégration Chariow (API + webhook, test vente factice)
- [x] Étape 3 — Agent contenu (génération de brouillons)
- [x] Étape 4 — Dashboard de validation
- [x] Étape 5 — Intégration Instagram (publication test)
- [ ] Étape 6 — Agent analytics + rapport hebdomadaire
