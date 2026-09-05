'use server';

import { revalidatePath } from 'next/cache';
import { reviewContentDraft, updateContentDraftText } from '../../db/repository.js';

export async function approveDraft(id) {
  reviewContentDraft(id, 'valide');
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
