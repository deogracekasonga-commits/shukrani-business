// Orchestrateur IA — reçoit les demandes de contenu et les distribue aux
// sous-agents. L'analytics (Étape 6) viendra s'y brancher sans changer
// cette interface publique.
import { generateWeeklyDrafts } from './content-agent.js';
import { publishApprovedDraft } from './planning-agent.js';

export function requestWeeklyContent(options) {
  return generateWeeklyDrafts(options);
}

export function requestPublish(draftId) {
  return publishApprovedDraft(draftId);
}
