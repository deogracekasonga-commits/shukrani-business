-- Schéma SQLite de l'agent marketing Shukrani Business
-- Un fichier unique (data/agent.db) : suffisant pour le MVP, migration facile
-- vers Postgres/Supabase en phase 2 si le volume de données augmente.

CREATE TABLE IF NOT EXISTS categories (
  id TEXT PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  marketing_angle TEXT NOT NULL,   -- promesse/angle marketing propre à la catégorie
  tone TEXT NOT NULL,              -- ton éditorial (ex: "inspirant, direct, preuve sociale")
  target_audience TEXT NOT NULL,
  active INTEGER NOT NULL DEFAULT 0  -- 1 = incluse dans le périmètre courant (MVP = 1 seule catégorie)
);

CREATE TABLE IF NOT EXISTS products (
  id TEXT PRIMARY KEY,
  category_id TEXT NOT NULL REFERENCES categories(id),
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  price_usd REAL,
  chariow_url TEXT NOT NULL,
  cover_image_url TEXT,
  file_path TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS content_drafts (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL REFERENCES products(id),
  category_id TEXT NOT NULL REFERENCES categories(id),
  channel TEXT NOT NULL,             -- facebook | instagram | tiktok | whatsapp
  format TEXT NOT NULL,              -- post | reel_script | story | whatsapp_broadcast
  ab_group TEXT NOT NULL DEFAULT 'A',-- variante de test A/B sur l'accroche
  hook TEXT NOT NULL,
  caption TEXT NOT NULL,
  hashtags TEXT NOT NULL DEFAULT '', -- liste séparée par des espaces
  video_script TEXT,                 -- rempli pour les formats vidéo courtes
  cta_link TEXT NOT NULL,            -- lien Chariow trackable (avec ?ref=<draft_id>)
  status TEXT NOT NULL DEFAULT 'draft', -- draft | approved | rejected | scheduled | published
  scheduled_at TEXT,
  reviewed_by TEXT,
  reviewed_at TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS publications (
  id TEXT PRIMARY KEY,
  draft_id TEXT NOT NULL REFERENCES content_drafts(id),
  channel TEXT NOT NULL,
  external_post_id TEXT,       -- id renvoyé par l'API (Meta Graph, TikTok...)
  dry_run INTEGER NOT NULL DEFAULT 1, -- 1 = simulation (pas de token / auto_publish off)
  published_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS metrics (
  id TEXT PRIMARY KEY,
  publication_id TEXT NOT NULL REFERENCES publications(id),
  captured_at TEXT NOT NULL DEFAULT (datetime('now')),
  impressions INTEGER NOT NULL DEFAULT 0,
  clicks INTEGER NOT NULL DEFAULT 0,
  likes INTEGER NOT NULL DEFAULT 0,
  comments INTEGER NOT NULL DEFAULT 0,
  shares INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sales (
  id TEXT PRIMARY KEY,
  chariow_order_id TEXT UNIQUE,
  product_id TEXT REFERENCES products(id),
  attributed_draft_id TEXT REFERENCES content_drafts(id),
  channel TEXT,
  amount REAL NOT NULL,
  currency TEXT NOT NULL DEFAULT 'USD',
  occurred_at TEXT NOT NULL DEFAULT (datetime('now')),
  raw_payload TEXT
);

CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  channel TEXT NOT NULL,
  contact_ref TEXT,
  message_in TEXT NOT NULL,
  intent TEXT,               -- prix | lien_achat | infos | autre
  message_out TEXT,
  handled_by TEXT NOT NULL DEFAULT 'agent', -- agent | human
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
