// Template de repli pour toute catégorie sans template dédié.
export const hooks = [
  (nom) => `Découvre "${nom}", le livre qui va t'aider à passer à l'action.`,
  (nom) => `Tu cherches des réponses concrètes ? "${nom}" est fait pour toi.`,
];

export function caption(product, hookIndex = 0) {
  const hook = hooks[hookIndex % hooks.length](product.nom);
  return [
    hook,
    '',
    `📖 ${product.nom} — ${product.prix} $`,
    `🔗 Lien en bio / ${product.lien_chariow}`,
    '',
    '#ebook #shukranibusiness',
  ].join('\n');
}

export function videoScript(product) {
  return [
    `[0-3s] ACCROCHE : "Ce livre peut vraiment t'aider."`,
    `[3-12s] Présente "${product.nom}" et ce qu'il apporte concrètement.`,
    `[12-20s] CTA : "Lien en bio — ${product.prix}$."`,
  ].join('\n');
}
