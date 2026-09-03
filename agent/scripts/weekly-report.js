import 'dotenv/config';
import { weeklyReport, abTestReport } from '../src/analytics/index.js';

const report = weeklyReport();
const ab = abTestReport();

console.log('=== Rapport hebdomadaire — Shukrani Business ===');
console.log(`Généré le ${report.generatedAt}`);
console.log(`Brouillons générés cette semaine : ${report.draftsCreatedThisWeek}`);
console.log(`Temps gagné estimé : ~${report.estimatedMinutesSaved} min`);
console.log(`CTR moyen : ${report.ctr}% (${report.clicks} clics / ${report.impressions} impressions)`);
console.log(`Revenu attribué (7j) : ${report.totalRevenue.toFixed(2)} $`);
console.log('\nPosts publiés par catégorie/canal:');
console.table(report.postsByCategory);
console.log('\nVentes par catégorie/canal:');
console.table(report.salesByCategory);
console.log('\nTest A/B des accroches:');
console.table(ab);
