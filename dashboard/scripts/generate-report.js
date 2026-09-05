import './env.js';
import { requestWeeklyReport } from '../src/agents/orchestrator.js';
import { buildRecommendations } from '../src/agents/analytics-agent.js';

const report = await requestWeeklyReport();

console.log(`Rapport ${report.semaine} :`);
console.log(`- Ventes totales : ${report.ventes_totales} $`);
console.log('- CA par catégorie :', JSON.parse(report.ca_par_categorie));
console.log('- Top posts :', JSON.parse(report.top_posts));
console.log('- Recommandations :');
for (const reco of buildRecommendations(report)) {
  console.log(`  · ${reco}`);
}
