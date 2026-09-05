'use server';

import { revalidatePath } from 'next/cache';
import { reviewContentDraft, updateContentDraftText } from '../../db/repository.js';
import { requestPublish } from '../../agents/orchestrator.js';
import { config } from '../../lib/config.js';

export async function approveDraft(id) {
  reviewContentDraft(id, 'valide');

  // Publication automatique optionnelle (désactivée par défaut) — activable
  // uniquement via AUTO_PUBLISH_INSTAGRAM=true, jamais sans l'approbation
  // explicite ci-dessus.
  if (config.autoPublishInstagram) {
    try {
      await requestPublish(id);
    } catch (error) {
      console.error(`[planning-agent] échec de publication auto du brouillon ${id} :`, error);
    }
  }

  revalidatePath('/drafts');
  revalidatePath('/calendar');
  revalidatePath('/');
}

export async function rejectDraft(id) {
  reviewContentDraft(id, 'rejete');
  revalidatePath('/drafts');
  revalidatePath('/calendar');
  revalidatePath('/');
}

export async function saveDraftText(id, formData) {
  const texte = formData.get('texte');
  if (typeof texte === 'string' && texte.trim().length > 0) {
    updateContentDraftText(id, texte);
  }
  revalidatePath('/drafts');
  revalidatePath('/calendar');
  revalidatePath('/');
}

export async function publishDraft(id) {
  try {
    await requestPublish(id);
  } catch (error) {
    // On ne bloque pas le dashboard : l'erreur est loggée côté serveur et le
    // brouillon reste "valide" (prêt à réessayer) plutôt que de faire
    // planter la page de validation de Deograce.
    console.error(`[planning-agent] échec de publication du brouillon ${id} :`, error);
  }
  revalidatePath('/drafts');
  revalidatePath('/calendar');
  revalidatePath('/');
}
