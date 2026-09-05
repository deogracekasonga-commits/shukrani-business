// Orchestrateur IA — reçoit les demandes de contenu et les distribue aux
// sous-agents. Pour le MVP (Étape 3), seul l'agent contenu existe ; la
// planification (Étape 5) et l'analytics (Étape 6) viendront s'y brancher
// sans changer cette interface publique.
import { generateWeeklyDrafts } from './content-agent.js';

export function requestWeeklyContent(options) {
  return generateWeeklyDrafts(options);
}
