# Agent IA Marketing — Shukrani Business

MVP de l'agent marketing décrit dans `ARCHITECTURE.md` : périmètre actuel
= catégorie **Business & Entrepreneuriat**, canal **Facebook**. Toutes
les autres catégories/canaux sont déjà modélisés en base mais désactivés
(`active = 0`) jusqu'à la phase 2.

## Installation

```bash
cd agent
npm install
cp .env.example .env   # facultatif — l'agent tourne sans aucune clé, en dry-run
npm run seed            # crée la base SQLite et charge le catalogue
npm start                # lance le dashboard sur http://localhost:3000
```

## Utilisation

1. Ouvrir `http://localhost:3000/drafts` et cliquer sur **« Générer les
   brouillons de la semaine »** : le sous-agent de contenu crée 3
   variantes d'accroche (A/B/C) pour chaque produit de la catégorie
   active.
2. Relire chaque brouillon, puis **Approuver & publier** ou **Rejeter**.
   - Sans `META_PAGE_ACCESS_TOKEN` configuré (ou avec la publication auto
     désactivée dans `/settings`), l'approbation passe le brouillon en
     statut `scheduled` et log dans la console ce qui aurait été publié
     (mode simulation — aucune requête n'est envoyée à Meta).
   - Avec un token valide et `auto_publish_facebook` activé dans
     `/settings`, l'approbation publie réellement via Meta Graph API.
3. `/calendar` : vue d'ensemble du calendrier éditorial par statut.
4. `/conversations` : test du sous-agent service client (répondeur FAQ :
   prix, lien d'achat, paiement, disponibilité) — champ de test manuel
   inclus.
5. `/reports/weekly` et `/reports/ab` : rapport hebdomadaire (posts,
   CTR, ventes attribuées, temps gagné estimé) et comparaison des
   accroches A/B. `npm run report` donne la même chose en CLI.
6. `/settings` : budget publicitaire hebdomadaire plafonné, activation
   de la publication automatique par canal, état des connexions API.

## Webhook Chariow (suivi des ventes)

`POST /webhooks/chariow`, signé en HMAC-SHA256 avec `CHARIOW_WEBHOOK_SECRET`
dans le header `X-Chariow-Signature: sha256=<hex>`. Sans secret configuré,
le endpoint refuse toute requête (401) — comportement volontaire pour ne
jamais accepter des données de vente non authentifiées.

Format de payload attendu (à ajuster une fois le format exact exposé par
Chariow confirmé — voir `src/integrations/chariow.js`) :

```json
{
  "order_id": "abc123",
  "product_url": "https://chariow.com/shukrani-business/50-opportunites-business-ia?ref=<draft_id>",
  "amount": 9,
  "currency": "USD"
}
```

Le paramètre `?ref=<draft_id>` (ajouté automatiquement par l'agent sur
chaque lien publié) permet l'attribution de la vente au brouillon, donc
au canal et à la catégorie exacts.

## Structure du projet

```
agent/
  src/
    db/            schéma SQLite + connexion
    catalog/        catégories, produits, seed
    content/         générateur de drafts (templates par catégorie + A/B)
    scheduler/       (réservé phase 3 — cron de publication planifiée)
    integrations/   meta.js (actif), chariow.js (actif), tiktok.js / whatsapp.js (phase 2)
    customer-service/  répondeur FAQ
    analytics/       rapport hebdomadaire + rapport A/B
    dashboard/       app Express + vues EJS (validation humaine)
  scripts/           seed.js, weekly-report.js
  server.js          point d'entrée
```

## Prochaines étapes (voir ARCHITECTURE.md §5)

- Activer une 2e catégorie (`active = 1` sur son slug dans
  `src/catalog/seed-data.js`) + écrire son template dédié dans
  `src/content/templates/`.
- Brancher Instagram (déjà prévu dans le modèle, `channel = 'instagram'`).
- Activer TikTok et WhatsApp Business (stubs prêts dans `src/integrations/`).
- Cron de publication planifiée + remplissage automatique de `metrics`
  via les insights Meta.
