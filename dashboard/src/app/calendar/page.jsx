import { listContentDrafts } from '../../db/repository.js';

const STATUT_LABEL = {
  brouillon: 'À valider',
  valide: 'Approuvé — prêt à programmer',
  rejete: 'Rejeté',
  publie: 'Publié',
};

function dayKey(dateCreation) {
  return dateCreation.slice(0, 10); // "2026-09-05 10:12:00" → "2026-09-05"
}

export default function CalendarPage() {
  const drafts = listContentDrafts({ limit: 200 }).filter((d) => d.statut !== 'rejete');

  const byDay = new Map();
  for (const draft of drafts) {
    const key = dayKey(draft.date_creation);
    if (!byDay.has(key)) byDay.set(key, []);
    byDay.get(key).push(draft);
  }
  const days = [...byDay.entries()].sort((a, b) => (a[0] < b[0] ? 1 : -1));

  return (
    <main style={{ maxWidth: 760, margin: '2.5rem auto', padding: '0 1.5rem' }}>
      <h1>Calendrier éditorial</h1>
      <p style={{ color: '#666' }}>
        Contenu généré par jour de création. La programmation d&apos;un créneau de
        publication précis arrive avec l&apos;intégration Instagram (Étape 5) —
        pour l&apos;instant, ceci montre ce qui est prêt (approuvé) ou en attente
        de validation.
      </p>

      {days.length === 0 && <p>Aucun contenu généré pour l&apos;instant.</p>}

      {days.map(([day, items]) => (
        <div key={day} style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ borderBottom: '1px solid #ddd', paddingBottom: '0.25rem' }}>{day}</h3>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {items.map((draft) => (
              <li
                key={draft.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  padding: '0.4rem 0',
                  borderBottom: '1px solid #f0f0f0',
                }}
              >
                <span>
                  {draft.produit_nom} · {draft.format}
                </span>
                <span style={{ color: '#666' }}>{STATUT_LABEL[draft.statut] ?? draft.statut}</span>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </main>
  );
}
