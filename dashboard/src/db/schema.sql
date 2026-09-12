-- Schéma de départ — Agent IA marketing Shukrani Business
-- PostgreSQL (Supabase). Reprend telles quelles les tables demandées dans
-- la spec ; seuls des champs techniques strictement nécessaires (id, clés
-- étrangères, dédoublonnage webhook) ont été ajoutés. Toutes les colonnes
-- de date/heure sont du texte ISO 8601 fourni par l'application (jamais un
-- DEFAULT SQL) pour rester comparables telles quelles entre elles.

CREATE TABLE IF NOT EXISTS products (
  id TEXT PRIMARY KEY,
  nom TEXT NOT NULL,
  categorie TEXT NOT NULL,          -- catégorie paramétrable (1 seule active au MVP, voir settings.active_category)
  prix DOUBLE PRECISION NOT NULL,
  lien_chariow TEXT NOT NULL,       -- URL produit publique sur Chariow
  chariow_product_id TEXT,          -- id produit côté API Chariow (GET /v1/products)
  image_url TEXT,                   -- visuel de couverture, requis par Instagram pour publier
  created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS content_drafts (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL REFERENCES products(id),
  texte TEXT NOT NULL,              -- légende / accroche générée par l'agent contenu
  format TEXT NOT NULL DEFAULT 'post', -- post | reel_script | story
  plateforme TEXT NOT NULL DEFAULT 'instagram_facebook', -- instagram_facebook | linkedin (réseau visé, ton/lien adaptés)
  statut TEXT NOT NULL DEFAULT 'brouillon', -- brouillon | valide | rejete | publie
  date_creation TEXT NOT NULL,
  date_validation TEXT,
  valide_par TEXT
);

CREATE TABLE IF NOT EXISTS published_posts (
  id TEXT PRIMARY KEY,
  draft_id TEXT NOT NULL REFERENCES content_drafts(id),
  url_instagram TEXT,                -- permalink renvoyé par Meta Graph API après publication
  date_publication TEXT NOT NULL,
  utm_link TEXT NOT NULL,            -- lien Chariow + paramètres UTM propres à ce post
  external_post_id TEXT              -- id média Meta, requis pour lire les insights (Étape 6)
);

CREATE TABLE IF NOT EXISTS sales (
  id TEXT PRIMARY KEY,
  product_id TEXT REFERENCES products(id),
  montant DOUBLE PRECISION NOT NULL,
  date TEXT NOT NULL,
  source_webhook TEXT,               -- payload brut reçu (JSON), conservé pour audit/débogage
  chariow_event_id TEXT UNIQUE,       -- id d'événement Chariow, pour ignorer les doublons de webhook
  attributed_post_id TEXT REFERENCES published_posts(id) -- attribution via le paramètre UTM du lien cliqué
);

CREATE TABLE IF NOT EXISTS weekly_reports (
  id TEXT PRIMARY KEY,
  semaine TEXT NOT NULL,             -- ex: "2026-W36"
  ventes_totales DOUBLE PRECISION NOT NULL,
  ca_par_categorie TEXT NOT NULL,     -- JSON: {"categorie": montant}
  top_posts TEXT NOT NULL,            -- JSON: [{post_id, ventes, clics, roi}]
  created_at TEXT NOT NULL
);

-- Réglages modifiables depuis le dashboard (catégorie active, budget pub
-- plafonné, activation de la publication automatique). Jamais codés en dur.
CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
