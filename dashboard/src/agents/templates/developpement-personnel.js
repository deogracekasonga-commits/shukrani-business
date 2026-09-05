// Template éditorial pour la catégorie "développement personnel".
// Ton : bienveillant, direct, orienté action — pas de survente.
export const hooks = [
  (nom) => `Et si le déclic pour changer venait d'un seul livre ?`,
  (nom) => `On ne te dira jamais ça à l'école : ${nom.toLowerCase()} s'apprend.`,
  (nom) => `Petit pas aujourd'hui, grand changement dans 30 jours.`,
];

export function caption(product, hookIndex = 0) {
  const hook = hooks[hookIndex % hooks.length](product.nom);
  return [
    hook,
    '',
    `« ${product.nom} » t'accompagne pas à pas, avec des conseils concrets à`,
    `appliquer dès aujourd'hui — pas de théorie, que du pratique.`,
    '',
    `📖 Disponible maintenant : ${product.prix} $`,
    `🔗 Lien en bio / ${product.lien_chariow}`,
    '',
    '#développementpersonnel #ebook #shukranibusiness #croissancepersonnelle #motivation',
  ].join('\n');
}

export function videoScript(product) {
  return [
    `[0-3s] ACCROCHE (face caméra) : "Tu te sens bloqué(e) ? Ce livre a changé ma façon de voir les choses."`,
    `[3-8s] PROBLÈME : montrer une situation reconnaissable (procrastination, manque de confiance...).`,
    `[8-15s] SOLUTION : montrer la couverture de "${product.nom}", citer 1 conseil concret du livre.`,
    `[15-20s] PREUVE : "Déjà des lecteurs qui appliquent ces principes au quotidien."`,
    `[20-25s] CTA : "Lien en bio pour te le procurer — ${product.prix}$."`,
  ].join('\n');
}
