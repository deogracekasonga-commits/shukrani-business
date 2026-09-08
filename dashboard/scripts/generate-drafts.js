import './env.js';
import { requestWeeklyContent } from '../src/agents/orchestrator.js';
import { closePool } from '../src/db/client.js';

const drafts = await requestWeeklyContent({ targetCount: 4 });

console.log(`${drafts.length} brouillon(s) généré(s) :\n`);
for (const draft of drafts) {
  console.log(`— [${draft.format}] (${draft.id})`);
  console.log(draft.texte);
  console.log('---');
}

await closePool();
