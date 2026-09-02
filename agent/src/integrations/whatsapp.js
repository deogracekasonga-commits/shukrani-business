// Intégration WhatsApp Business Cloud API — Phase 2, non activée dans le
// MVP. Le sous-agent service client (src/customer-service/) fonctionne
// déjà en mode "log + réponse suggérée" ; il suffira de brancher l'envoi
// réel ici une fois WHATSAPP_ACCESS_TOKEN et WHATSAPP_PHONE_NUMBER_ID
// configurés.

export async function sendWhatsAppMessage() {
  throw new Error(
    'Intégration WhatsApp Business non activée (phase 2). Configurer WHATSAPP_ACCESS_TOKEN avant utilisation.'
  );
}
