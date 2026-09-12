// Template LinkedIn — ton professionnel, orienté valeur/retour d'expérience.
// Contrairement à Instagram, LinkedIn affiche les liens de façon cliquable
// directement dans le post : pas de "lien en bio", le lien Chariow est donc
// inclus tel quel dans le texte.
export function caption(product) {
  return [
    `📚 Nouvelle ressource pour les entrepreneurs et professionnels francophones : « ${product.nom} ».`,
    '',
    `Ce guide propose des conseils concrets, applicables dès aujourd'hui, sans détour théorique.`,
    '',
    `👉 Disponible ici : ${product.lien_chariow} (${product.prix} $)`,
    '',
    '#entrepreneuriat #développementpersonnel #ebook #afrique #shukranibusiness',
  ].join('\n');
}
