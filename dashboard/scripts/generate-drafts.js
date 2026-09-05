import './env.js';
import { requestWeeklyContent } from '../src/agents/orchestrator.js';

const drafts = requestWeeklyContent({ targetCount: 4 });

console.log(`${drafts.length} brouillon(s) généré(s) :\n`);
for (const draft of drafts) {
  console.log(`— [${draft.format}] (${draft.id})`);
  console.log(draft.texte);
  console.log('---');
}
