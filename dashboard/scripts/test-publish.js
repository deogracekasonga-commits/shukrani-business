// Publie de bout en bout le premier brouillon approuvé trouvé (ou approuve
// automatiquement le premier brouillon en attente s'il n'y en a aucun) —
// pour tester le pipeline sans passer par le dashboard.
// Usage : npm run test:publish
import './env.js';
import { getDb } from '../src/db/client.js';
import { reviewContentDraft } from '../src/db/repository.js';
import { requestPublish } from '../src/agents/orchestrator.js';
import { config } from '../src/lib/config.js';

const db = getDb();

let draft = db.prepare(`SELECT * FROM content_drafts WHERE statut = 'valide' LIMIT 1`).get();

if (!draft) {
  const pending = db.prepare(`SELECT * FROM content_drafts WHERE statut = 'brouillon' LIMIT 1`).get();
  if (!pending) {
    console.error('Aucun brouillon disponible. Lance d\'abord `npm run generate:drafts`.');
    process.exit(1);
  }
  draft = reviewContentDraft(pending.id, 'valide', 'test-publish.js');
  console.log(`Brouillon ${draft.id} approuvé automatiquement pour ce test.`);
}

const post = await requestPublish(draft.id);

console.log('\nPublication enregistrée :');
console.log('- url_instagram :', post.url_instagram ?? '(dry-run — aucune vraie publication)');
console.log('- utm_link      :', post.utm_link);
if (!config.meta.pageAccessToken) {
  console.log(
    '\n(mode dry-run — configure META_PAGE_ACCESS_TOKEN et META_IG_USER_ID pour publier réellement)'
  );
}
