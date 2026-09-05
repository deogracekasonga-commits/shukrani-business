import { NextResponse } from 'next/server';
import {
  verifyWebhookSignature,
  extractSaleFromWebhookPayload,
} from '../../../../integrations/chariow.js';
import { recordSaleFromWebhook } from '../../../../db/repository.js';

// Événement écouté : "sale.completed" (nom à confirmer dans le dashboard
// Chariow — accepté avec un alias "order.completed" par précaution, sans
// certitude que ce soit le nom réel).
const HANDLED_EVENTS = new Set(['sale.completed', 'order.completed']);

export async function POST(request) {
  const rawBody = await request.text();
  const signature = request.headers.get('x-chariow-signature');

  if (!verifyWebhookSignature(rawBody, signature)) {
    return NextResponse.json({ error: 'signature invalide ou absente' }, { status: 401 });
  }

  let payload;
  try {
    payload = JSON.parse(rawBody);
  } catch {
    return NextResponse.json({ error: 'JSON invalide' }, { status: 400 });
  }

  const sale = extractSaleFromWebhookPayload(payload);

  if (!HANDLED_EVENTS.has(sale.event)) {
    // On reconnaît la requête (signature valide) mais on ignore l'événement.
    return NextResponse.json({ ignored: true, event: sale.event ?? null });
  }

  const { inserted, sale: stored } = recordSaleFromWebhook(sale);

  return NextResponse.json({ ok: true, inserted, sale_id: stored.id });
}
