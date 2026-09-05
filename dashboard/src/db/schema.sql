-- Schéma de départ — Agent IA marketing Shukrani Business
-- SQLite (fichier unique, data/agent.db). Reprend telles quelles les tables
-- demandées dans la spec ; seuls des champs techniques strictement
-- nécessaires (id, clés étrangères, valeurs par défaut, dédoublonnage
-- webhook) ont été ajoutés.

CREATE TABLE IF NOT EXISTS products (
  id TEXT PRIMARY KEY,
  nom TEXT NOT NULL,
  categorie TEXT NOT NULL,          -- catégorie paramétrable (1 seule active au MVP, voir settings.active_category)
  prix REAL NOT NULL,
  lien_chariow TEXT NOT NULL,       -- URL produit publique sur Chariow
  chariow_product_id TEXT,          -- id produit côté API Chariow (GET /v1/products)
  image_url TEXT,                   -- visuel de couverture, requis par Instagram pour publier
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS content_drafts (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL REFERENCES products(id),
  texte TEXT NOT NULL,              -- légende / accroche générée par l'agent contenu
  format TEXT NOT NULL DEFAULT 'post', -- post | reel_script | story
  statut TEXT NOT NULL DEFAULT 'brouillon', -- brouillon | valide | rejete | publie
  date_creation TEXT NOT NULL DEFAULT (datetime('now')),
  date_validation TEXT,
  valide_par TEXT
);

CREATE TABLE IF NOT EXISTS published_posts (
  id TEXT PRIMARY KEY,
  draft_id TEXT NOT NULL REFERENCES content_drafts(id),
  url_instagram TEXT,                -- permalink renvoyé par Meta Graph API après publication
  date_publication TEXT NOT NULL DEFAULT (datetime('now')),
  utm_link TEXT NOT NULL             -- lien Chariow + paramètres UTM propres à ce post
);

CREATE TABLE IF NOT EXISTS sales (
  id TEXT PRIMARY KEY,
  product_id TEXT REFERENCES products(id),
  montant REAL NOT NULL,
  date TEXT NOT NULL DEFAULT (datetime('now')),
  source_webhook TEXT,               -- payload brut reçu (JSON), conservé pour audit/débogage
  chariow_event_id TEXT UNIQUE,       -- id d'événement Chariow, pour ignorer les doublons de webhook
  attributed_post_id TEXT REFERENCES published_posts(id) -- attribution via le paramètre UTM du lien cliqué
);

CREATE TABLE IF NOT EXISTS weekly_reports (
  id TEXT PRIMARY KEY,
  semaine TEXT NOT NULL,             -- ex: "2026-W36"
  ventes_totales REAL NOT NULL,
  ca_par_categorie TEXT NOT NULL,     -- JSON: {"categorie": montant}
  top_posts TEXT NOT NULL,            -- JSON: [{post_id, ventes, clics, roi}]
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Réglages modifiables depuis le dashboard (catégorie active, budget pub
-- plafonné, activation de la publication automatique). Jamais codés en dur.
CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
