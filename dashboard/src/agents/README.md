# Agents

- `orchestrator.js` — point d'entrée unique, distribue aux sous-agents.
- `content-agent.js` — génère légendes, accroches, scripts vidéo courts par
  templates (catégorie `templates/<categorie>.js`, repli sur `generic.js`).
- `planning-agent.js` — programme les publications validées, génère les
  liens UTM (Étape 5, pas encore implémenté).
- `analytics-agent.js` — traite les webhooks Chariow + métriques Instagram,
  calcule le ROI, génère le rapport hebdomadaire (Étape 6, pas encore
  implémenté).
