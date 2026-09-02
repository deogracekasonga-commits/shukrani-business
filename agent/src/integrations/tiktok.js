// Intégration TikTok (Content Posting API) — Phase 2, non activée dans le
// MVP (cf. ARCHITECTURE.md §5). Le modèle de données et le générateur de
// contenu produisent déjà des `video_script` pour ce canal ; il ne
// manque que le branchement à l'API officielle une fois les accès
// développeur TikTok obtenus par Deograce.

export async function publishToTikTok() {
  throw new Error(
    "Intégration TikTok non activée (phase 2). Configurer TIKTOK_ACCESS_TOKEN et implémenter l'appel à l'API officielle avant utilisation."
  );
}
