import { listWeeklyReports } from '../../db/repository.js';
import { buildRecommendations, getIsoWeekInfo } from '../../agents/analytics-agent.js';
import { generateReport } from './actions.js';

export default function ReportsPage() {
  const reports = listWeeklyReports(20);
  const currentWeek = getIsoWeekInfo().label;
  const latest = reports[0];

  return (
    <main style={{ maxWidth: 760, margin: '2.5rem auto', padding: '0 1.5rem' }}>
      <h1>Rapport hebdomadaire</h1>
      <p style={{ color: '#666' }}>
        Ventes, ROI par post et recommandations — calculés à partir des ventes Chariow
        et des posts publiés de la semaine.
      </p>

      <form action={generateReport}>
        <button type="submit" style={generateButtonStyle}>
          📊 Générer le rapport de la semaine {currentWeek}
        </button>
      </form>

      {!latest ? (
        <p style={{ marginTop: '1.5rem' }}>Aucun rapport généré pour l&apos;instant.</p>
      ) : (
        <div style={{ marginTop: '2rem' }}>
          <ReportCard report={latest} highlight />
        </div>
      )}

      {reports.length > 1 && (
        <>
          <h2 style={{ marginTop: '2.5rem' }}>Rapports précédents</h2>
          {reports.slice(1).map((report) => (
            <ReportCard key={report.id} report={report} />
          ))}
        </>
      )}
    </main>
  );
}

function ReportCard({ report, highlight }) {
  const topPosts = JSON.parse(report.top_posts);
  const caParCategorie = JSON.parse(report.ca_par_categorie);
  const recommandations = buildRecommendations(report);

  return (
    <div style={{ ...cardStyle, borderColor: highlight ? '#0a66c2' : '#ddd' }}>
      <h3 style={{ marginTop: 0 }}>Semaine {report.semaine}</h3>
      <p>
        <strong>{report.ventes_totales} $</strong> de ventes totales
      </p>

      <h4 style={{ marginBottom: '0.4rem' }}>CA par catégorie</h4>
      <ul style={{ marginTop: 0 }}>
        {Object.entries(caParCategorie).map(([categorie, montant]) => (
          <li key={categorie}>
            {categorie} : {montant} $
          </li>
        ))}
      </ul>

      <h4 style={{ marginBottom: '0.4rem' }}>Top posts</h4>
      {topPosts.length === 0 ? (
        <p style={{ color: '#666' }}>Aucun post publié cette semaine.</p>
      ) : (
        <table style={{ borderCollapse: 'collapse', width: '100%', marginBottom: '1rem' }}>
          <thead>
            <tr>
              <th style={cellStyle}>Produit</th>
              <th style={cellStyle}>Ventes</th>
              <th style={cellStyle}>ROI</th>
              <th style={cellStyle}>Impressions</th>
            </tr>
          </thead>
          <tbody>
            {topPosts.map((post) => (
              <tr key={post.post_id}>
                <td style={cellStyle}>
                  {post.url_instagram ? (
                    <a href={post.url_instagram} target="_blank" rel="noreferrer">
                      {post.produit_nom}
                    </a>
                  ) : (
                    post.produit_nom
                  )}
                </td>
                <td style={cellStyle}>
                  {post.ventes} $ ({post.nombre_ventes})
                </td>
                <td style={cellStyle}>{post.roi !== null ? `${post.roi}x` : '—'}</td>
                <td style={cellStyle}>{post.insights?.impressions ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h4 style={{ marginBottom: '0.4rem' }}>Recommandations</h4>
      <ul style={{ marginTop: 0 }}>
        {recommandations.map((reco, i) => (
          <li key={i}>{reco}</li>
        ))}
      </ul>
    </div>
  );
}

const cardStyle = {
  border: '1px solid #ddd',
  borderRadius: 8,
  padding: '1.25rem',
  marginBottom: '1.5rem',
};
const cellStyle = { textAlign: 'left', borderBottom: '1px solid #ddd', padding: '0.4rem 0.6rem' };
const generateButtonStyle = {
  background: '#0a66c2',
  color: 'white',
  border: 'none',
  borderRadius: 6,
  padding: '0.6rem 1.1rem',
  cursor: 'pointer',
};
