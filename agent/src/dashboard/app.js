import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { db, getSetting, setSetting } from '../db/index.js';
import { listActiveCategories } from '../catalog/index.js';
import { generateWeeklyDrafts, listDraftsByStatus, getDraft, updateDraftStatus } from '../content/index.js';
import { publishToFacebook } from '../integrations/meta.js';
import { verifySignature, recordSale } from '../integrations/chariow.js';
import { handleIncomingMessage, listRecentConversations } from '../customer-service/index.js';
import { weeklyReport, abTestReport } from '../analytics/index.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export function createApp() {
  const app = express();
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));

  // Capture le corps brut pour la vérification de signature du webhook
  // Chariow, tout en gardant express.json() utilisable partout ailleurs.
  app.use(express.json({ verify: (req, res, buf) => { req.rawBody = buf; } }));
  app.use(express.urlencoded({ extended: true }));

  app.get('/', (req, res) => res.redirect('/drafts'));

  // ---- File de validation ----------------------------------------------
  app.get('/drafts', (req, res) => {
    const drafts = listDraftsByStatus('draft');
    const activeCategoryName = listActiveCategories().map((c) => c.name).join(', ') || 'aucune';
    res.render('drafts', { drafts, activeCategoryName, flash: req.query.flash || null });
  });

  app.post('/drafts/generate', (req, res) => {
    const created = generateWeeklyDrafts({ channel: 'facebook' });
    res.redirect(`/drafts?flash=${encodeURIComponent(`${created.length} nouveau(x) brouillon(s) généré(s).`)}`);
  });

  app.post('/drafts/:id/approve', async (req, res) => {
    updateDraftStatus(req.params.id, 'approved');
    const draft = getDraft(req.params.id);
    try {
      await publishToFacebook(draft);
    } catch (err) {
      console.error('[dashboard] Erreur publication:', err.message);
    }
    res.redirect('/drafts');
  });

  app.post('/drafts/:id/reject', (req, res) => {
    updateDraftStatus(req.params.id, 'rejected');
    res.redirect('/drafts');
  });

  // ---- Calendrier éditorial ---------------------------------------------
  app.get('/calendar', (req, res) => {
    const grouped = {};
    for (const status of ['approved', 'scheduled', 'published', 'rejected']) {
      grouped[status] = listDraftsByStatus(status);
    }
    res.render('calendar', { grouped });
  });

  // ---- Service client -----------------------------------------------------
  app.get('/conversations', (req, res) => {
    res.render('conversations', { conversations: listRecentConversations() });
  });

  app.post('/conversations/simulate', (req, res) => {
    handleIncomingMessage({
      channel: req.body.channel || 'whatsapp',
      contactRef: 'test-dashboard',
      message: req.body.message || '',
    });
    res.redirect('/conversations');
  });

  // ---- Rapports -----------------------------------------------------------
  app.get('/reports/weekly', (req, res) => {
    res.render('weekly-report', { report: weeklyReport() });
  });

  app.get('/reports/ab', (req, res) => {
    res.render('ab-report', { rows: abTestReport() });
  });

  // ---- Réglages -------------------------------------------------------------
  app.get('/settings', (req, res) => {
    res.render('settings', {
      settings: {
        weekly_ad_budget_usd: getSetting('weekly_ad_budget_usd', '0'),
        auto_publish_facebook: getSetting('auto_publish_facebook', 'false'),
        minutes_saved_per_draft: getSetting('minutes_saved_per_draft', '25'),
      },
      metaConfigured: Boolean(process.env.META_PAGE_ACCESS_TOKEN),
      chariowConfigured: Boolean(process.env.CHARIOW_WEBHOOK_SECRET),
      flash: req.query.flash || null,
    });
  });

  app.post('/settings', (req, res) => {
    setSetting('weekly_ad_budget_usd', Number(req.body.weekly_ad_budget_usd || 0));
    setSetting('auto_publish_facebook', req.body.auto_publish_facebook === 'on' ? 'true' : 'false');
    setSetting('minutes_saved_per_draft', Number(req.body.minutes_saved_per_draft || 25));
    res.redirect('/settings?flash=' + encodeURIComponent('Réglages enregistrés.'));
  });

  // ---- Webhook Chariow (ventes) ------------------------------------------
  app.post('/webhooks/chariow', (req, res) => {
    const signature = req.header('X-Chariow-Signature');
    if (!verifySignature(req.rawBody, signature)) {
      return res.status(401).json({ error: 'signature invalide ou secret non configuré' });
    }
    const record = recordSale(req.body);
    res.json({ ok: true, sale_id: record.id });
  });

  app.get('/health', (req, res) => res.json({ ok: true }));

  return app;
}
