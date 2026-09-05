'use server';

import { revalidatePath } from 'next/cache';
import { requestWeeklyReport } from '../../agents/orchestrator.js';

export async function generateReport() {
  await requestWeeklyReport();
  revalidatePath('/reports');
}
