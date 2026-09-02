# Agent IA Marketing — Shukrani Business

Boutique : vente de livres/ebooks (Chariow) — CEO : Deograce
Marché cible : RDC (Kinshasa) et Afrique francophone
Objectif : ventes régulières et prévisibles, sans faux engagement, avec
validation humaine avant toute publication.

## 1. Principes non négociables

- **API officielles uniquement** : Meta Graph API (Facebook/Instagram),
  TikTok for Developers API, WhatsApp Business Platform (Cloud API).
  Aucun scraping, aucun bot d'engagement, aucune fausse interaction.
- **Brouillon par défaut** : tout contenu généré a le statut `draft` et ne
  part jamais tout seul. La publication automatique (`AUTO_PUBLISH=true`)
  est un choix explicite, par canal, réversible à tout moment.
- **Budget plafonné** : le budget pub (par canal, par semaine) est un
  réglage stocké en base, jamais codé en dur ; l'agent ne peut pas le
  dépasser ni le modifier lui-même.
- **RGPD / CGU plateformes** : on ne stocke que ce qui est nécessaire au
  service (catalogue, calendrier, métriques agrégées, conversations liées
  au service client). Les tokens d'API vivent uniquement dans les
  variables d'environnement, jamais commités.

## 2. Architecture : orchestrateur + sous-agents

```
                         ┌──────────────────────────┐
                         │   Dashboard (Deograce)    │  ← validation humaine
                         │  Express + EJS            │
                         └────────────┬──────────────┘
                                      │ approuve / rejette / édite
                                      ▼
┌───────────────┐   catalogue   ┌───────────────────────┐
│   Catalogue    │──────────────▶│      Orchestrateur     │
│ (catégories +  │               │  src/orchestrator.js   │
│  produits)     │◀──────────────│  - planifie le contenu │
└───────────────┘   angle/ton   │  - déclenche les sous- │
                                 │    agents              │
                                 └──────┬───────┬─────────┘
                    ┌────────────────────┘       └───────────────────┐
                    ▼                                                 ▼
        ┌────────────────────────┐                     ┌──────────────────────────┐
        │ Sous-agent Contenu      │                     │ Sous-agent Service Client │
        │ src/content/            │                     │ src/customer-service/     │
        │ - hooks A/B par         │                     │ - FAQ (prix, lien, achat) │
        │   catégorie             │                     │ - ton chaleureux          │
        │ - caption + hashtags    │                     │ - log conversations       │
        │ - script vidéo courte   │                     └──────────────────────────┘
        └───────────┬─────────────┘
                    ▼
        ┌────────────────────────┐         ┌──────────────────────────┐
        │ Sous-agent Planification│────────▶│ Intégrations plateformes │
        │ src/scheduler/          │         │ src/integrations/        │
        │ - calendrier éditorial  │         │ - meta.js (Graph API)    │
        │ - respecte budget/canal │         │ - tiktok.js (phase 2)    │
        └────────────────────────┘         │ - whatsapp.js (phase 2)  │
                                            │ - chariow.js (webhook +  │
                                            │   attribution ventes)    │
                                            └─────────────┬────────────┘
                                                           ▼
                                            ┌──────────────────────────┐
                                            │ Sous-agent Analytics      │
                                            │ src/analytics/            │
                                            │ - CTR, ventes attribuées  │
                                            │ - rapport hebdomadaire    │
                                            │ - comparaison A/B         │
                                            └──────────────────────────┘
```

Chaque sous-agent est un module Node.js indépendant, appelable en CLI
(`npm run ...`) ou par le dashboard. Aucun sous-agent n'appelle une API
externe en écriture sans passer par `integrations/`, qui centralise les
garde-fous (dry-run, budget, tokens).

## 3. Modèle de données (SQLite → migration Postgres/Supabase si besoin)

Voir `src/db/schema.sql`. Tables : `categories`, `products`,
`content_drafts`, `publications`, `metrics`, `sales`, `conversations`,
`settings`.

Point clé : chaque lien Chariow généré dans un draft porte un paramètre
`?ref=<draft_id>` pour permettre l'attribution des ventes par canal et par
catégorie une fois le webhook Chariow branché.

## 4. Intégrations

| Plateforme | API officielle | Usage MVP | Phase |
|---|---|---|---|
| Facebook/Instagram | Meta Graph API (Pages, IG Business) | publication posts, lecture insights | **MVP (phase 1)** |
| Chariow | Webhook commandes (+ API si disponible) | attribution des ventes | **MVP (phase 1)** |
| TikTok | TikTok for Developers (Content Posting API) | scripts vidéo + publication | Phase 2 |
| WhatsApp Business | Cloud API | FAQ auto + diffusion catalogue | Phase 2 |

Le webhook Chariow n'a pas de spécification publique fixe connue ici : le
module `src/integrations/chariow.js` définit un format d'entrée
raisonnable (id de commande, produit, montant, `ref`) et vérifie une
signature HMAC partagée. À ajuster dès que Deograce a le format exact
exposé par Chariow (webhook sortant ou export CSV en attendant).

## 5. Roadmap en étapes

**Phase 1 — MVP (ce commit)** : 1 catégorie (« Business & Entrepreneuriat »,
produit *50 opportunités business IA pour entrepreneurs africains »), 1
réseau (Facebook/Instagram via Meta Graph API, en mode dry-run tant
qu'aucun token n'est configuré).
- Catalogue + calendrier éditorial en base
- Génération de drafts avec 3 accroches A/B par produit
- Dashboard de validation humaine (approuver / rejeter / éditer)
- Webhook Chariow → table `sales` + attribution best-effort
- Rapport hebdomadaire (posts publiés, CTR, ventes, temps gagné estimé)

**Phase 2** : ajout des catégories restantes (développement personnel,
informatique/bureautique...), chacune avec son propre angle marketing et
ses templates ; ajout Instagram Reels natif + TikTok (scripts courts déjà
prévus dans le modèle de données) ; auto-réponse WhatsApp Business pour
les FAQ.

**Phase 3** : automatisation de la publication planifiée (cron +
`AUTO_PUBLISH=true` par canal, toujours plafonné par budget) ; insights
Meta réels branchés dans `metrics` ; sélection automatique de l'accroche
gagnante après un seuil d'impressions.

**Phase 4** : optimisation budgétaire multi-canal (arbitrage des
dépenses pub selon le ROI par catégorie), recommandations proactives de
l'agent (« cette catégorie sous-performe, voici 3 angles à tester »).

## 6. Mesure du succès (branché sur `src/analytics/`)

- Posts publiés / semaine, par catégorie
- Taux de clic vers les liens Chariow (par variante A/B)
- Ventes attribuées par canal et par catégorie
- Temps gagné estimé pour Deograce (nb de drafts générés × temps moyen de
  création manuelle défini en config)

## 7. Ce que ce commit livre concrètement

Une application Node.js (`agent/`) exécutable localement (`npm start`) :
base SQLite auto-créée, catalogue pré-rempli avec les 3 ebooks déjà en
vente, génération de drafts pour la catégorie Business, dashboard de
validation sur `http://localhost:3000`, webhook Chariow, rapport
hebdomadaire en CLI. Toutes les clés d'API externes sont optionnelles :
sans elles, l'agent tourne entièrement en simulation (dry-run) et affiche
ce qu'il aurait publié.
