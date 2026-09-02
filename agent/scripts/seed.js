import 'dotenv/config';
import { seedCatalog } from '../src/catalog/index.js';
import { setSetting, getSetting } from '../src/db/index.js';

seedCatalog();

// Réglages par défaut : brouillon obligatoire, budget à 0 (le client doit
// le fixer explicitement avant toute campagne payante), auto-publish OFF.
if (getSetting('auto_publish_facebook') === null) setSetting('auto_publish_facebook', 'false');
if (getSetting('weekly_ad_budget_usd') === null) setSetting('weekly_ad_budget_usd', '0');
if (getSetting('minutes_saved_per_draft') === null) setSetting('minutes_saved_per_draft', '25');

console.log('Catalogue initialisé et réglages par défaut appliqués.');
