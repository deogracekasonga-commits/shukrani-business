import { listContentDrafts, getPublishedPostForDraft } from '../../db/repository.js';
import { approveDraft, rejectDraft, saveDraftText, publishDraft } from './actions.js';

const STATUT_LABEL = {
  brouillon: 'À valider',
  valide: 'Approuvé',
  rejete: 'Rejeté',
  publie: 'Publié',
};

export default function DraftsPage() {
  const pending = listContentDrafts({ statut: 'brouillon', limit: 50 });
  const approved = listContentDrafts({ statut: 'valide', limit: 50 });
  const done = listContentDrafts({ limit: 50 }).filter(
    (d) => d.statut === 'rejete' || d.statut === 'publie'
  );

  return (
    <main style={{ maxWidth: 760, margin: '2.5rem auto', padding: '0 1.5rem' }}>
      <h1>Brouillons à valider</h1>
      <p style={{ color: '#666' }}>
        Relis, modifie si besoin puis approuve ou rejette. Rien n&apos;est publié sans
        validation explicite ici.
      </p>

      {pending.length === 0 ? (
        <p>Aucun brouillon en attente. Lance <code>npm run generate:drafts</code>.</p>
      ) : (
        pending.map((draft) => (
          <div key={draft.id} style={cardStyle}>
            <div style={metaStyle}>
              {draft.produit_nom} · {draft.format} · {draft.date_creation}
            </div>
            <form action={saveDraftText.bind(null, draft.id)}>
              <textarea
                name="texte"
                defaultValue={draft.texte}
                rows={draft.format === 'reel_script' ? 8 : 6}
                style={textareaStyle}
              />
              <button type="submit" style={secondaryButtonStyle}>
                Enregistrer les modifications
              </button>
            </form>
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
              <form action={approveDraft.bind(null, draft.id)}>
                <button type="submit" style={approveButtonStyle}>
                  ✅ Approuver
                </button>
              </form>
              <form action={rejectDraft.bind(null, draft.id)}>
                <button type="submit" style={rejectButtonStyle}>
                  ✕ Rejeter
                </button>
              </form>
            </div>
          </div>
        ))
      )}

      {approved.length > 0 && (
        <>
          <h2 style={{ marginTop: '2.5rem' }}>Approuvés — prêts à publier</h2>
          {approved.map((draft) => (
            <div key={draft.id} style={cardStyle}>
              <div style={metaStyle}>
                {draft.produit_nom} · {draft.format} · approuvé par {draft.valide_par} le{' '}
                {draft.date_validation}
              </div>
              <pre style={{ whiteSpace: 'pre-wrap', margin: '0 0 0.5rem' }}>{draft.texte}</pre>
              <form action={publishDraft.bind(null, draft.id)}>
                <button type="submit" style={publishButtonStyle}>
                  📤 Publier sur Instagram
                </button>
              </form>
            </div>
          ))}
        </>
      )}

      {done.length > 0 && (
        <>
          <h2 style={{ marginTop: '2.5rem' }}>Déjà traités</h2>
          {done.map((draft) => {
            const post = draft.statut === 'publie' ? getPublishedPostForDraft(draft.id) : null;
            return (
              <div key={draft.id} style={{ ...cardStyle, opacity: 0.75 }}>
                <div style={metaStyle}>
                  {draft.produit_nom} · {draft.format} · {STATUT_LABEL[draft.statut] ?? draft.statut}
                  {draft.valide_par ? ` par ${draft.valide_par}` : ''} le {draft.date_validation}
                </div>
                <pre style={{ whiteSpace: 'pre-wrap', margin: 0, fontFamily: 'inherit' }}>{draft.texte}</pre>
                {post && (
                  <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                    {post.url_instagram ? (
                      <a href={post.url_instagram} target="_blank" rel="noreferrer">
                        Voir sur Instagram ↗
                      </a>
                    ) : (
                      <span style={{ color: '#a15c00' }}>
                        (dry-run — aucune vraie publication, configure META_PAGE_ACCESS_TOKEN)
                      </span>
                    )}
                    {' · '}Lien tracké : <code>{post.utm_link}</code>
                  </div>
                )}
              </div>
            );
          })}
        </>
      )}
    </main>
  );
}

const cardStyle = {
  border: '1px solid #ddd',
  borderRadius: 8,
  padding: '1rem',
  marginBottom: '1rem',
};
const metaStyle = { fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' };
const textareaStyle = { width: '100%', boxSizing: 'border-box', fontFamily: 'inherit', padding: '0.5rem' };
const secondaryButtonStyle = { marginTop: '0.5rem' };
const approveButtonStyle = { background: '#1a7f37', color: 'white', border: 'none', borderRadius: 6, padding: '0.5rem 1rem', cursor: 'pointer' };
const rejectButtonStyle = { background: '#b3261e', color: 'white', border: 'none', borderRadius: 6, padding: '0.5rem 1rem', cursor: 'pointer' };
const publishButtonStyle = { background: '#0a66c2', color: 'white', border: 'none', borderRadius: 6, padding: '0.5rem 1rem', cursor: 'pointer' };
