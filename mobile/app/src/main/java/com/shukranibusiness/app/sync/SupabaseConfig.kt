package com.shukranibusiness.app.sync

/**
 * Coordonnées du projet Supabase "shukra-pos" dédié à la synchronisation des ventes entre
 * appareils. La clé publique ci-dessous n'est pas un secret : la sécurité repose sur les
 * politiques RLS côté base de données (voir migration `shukra_pos_remote_sync_schema`), pas sur
 * la confidentialité de cette clé.
 */
object SupabaseConfig {
    const val BASE_URL = "https://aopncbykyxmvrjcuxutu.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_NZNTMyNy6gMfSoSA7FmAIg_Jh9YR1Fx"
}
