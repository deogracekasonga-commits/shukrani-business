// Orchestrateur IA — reçoit les demandes de contenu et les distribue aux
// sous-agents.
import { generateWeeklyDrafts } from './content-agent.js';
import { publishApprovedDraft } from './planning-agent.js';
import { generateWeeklyReport } from './analytics-agent.js';

export function requestWeeklyContent(options) {
  return generateWeeklyDrafts(options);
}

export function requestPublish(draftId) {
  return publishApprovedDraft(draftId);
}

export function requestWeeklyReport(referenceDate) {
  return generateWeeklyReport(referenceDate);
}
