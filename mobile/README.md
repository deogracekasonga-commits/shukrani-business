# Shukra POS (Android)

**Shukra POS** — *Simplifiez vos ventes, maîtrisez vos stocks.*

Application Android de **caisse (POS) et gestion de stock en temps réel**
pour la boutique physique Shukrani Business (biscuits, boudin, pampers,
bonbons, etc.), destinée à être publiée sur **Google Play Console** (compte
développeur : Deogracio Apps).

Conçue pour fonctionner **hors-ligne** sur un seul appareil (toutes les
données sont stockées localement en base SQLite via Room), avec des comptes
employés à accès différencié (gérant / vendeur), gestion des deux devises
courantes en RDC (CDF et USD), export des données et impression de reçu sur
imprimante Bluetooth.

## Fonctionnalités

- **Connexion par PIN** : chaque employé a son propre code, avec un rôle
  Gérant (accès complet) ou Vendeur (accès à la vente uniquement).
- **Accueil** (gérant) : ventes du jour (total CDF/USD, nombre de ventes),
  alertes stock bas, et — si la synchronisation à distance est activée en
  tant que gérant — les ventes des autres appareils/boutiques connectés.
- **Vente (POS)** : grille de produits par catégorie, panier avec quantités,
  choix de la devise (CDF ou USD), décrément du stock à la validation.
- **Stock** (gérant) : liste des produits avec alerte stock bas, ajout/
  modification/**suppression** de produit, réapprovisionnement journalisé.
- **Rapports** : historique des ventes filtrable par période (jour/semaine/
  mois/tout), totaux CDF et USD, export **CSV** (Excel) et **PDF**. Toucher
  une vente ouvre son détail ; le **gérant** peut l'**annuler** (note de
  crédit) — les articles sont remis en stock, la vente reste visible dans
  l'historique mais marquée « annulée » et exclue des totaux.
- **Employés** (gérant) : création/modification/désactivation des comptes,
  attribution du rôle.
- **Réglages** : nom de la boutique, taux de change CDF ↔ USD, sélection de
  l'imprimante Bluetooth.
- **Reçu Bluetooth** : impression automatique après chaque vente si une
  imprimante thermique (ESC/POS, profil SPP) est configurée.
- **Synchronisation à distance** (optionnelle) : chaque appareil peut envoyer
  ses ventes vers une base Supabase partagée, pour qu'un gérant à distance
  les consulte depuis son propre téléphone — voir la section dédiée
  plus bas.

## Architecture

```
mobile/app/src/main/java/com/shukranibusiness/app/
  data/
    entities/        Employee, Product, Sale, SaleItem, StockMovement
    dao/              interfaces Room (une par entité)
    AppDatabase.kt    base Room (SQLite local, hors-ligne)
    ShopRepository.kt logique métier (vente transactionnelle, stock, auth PIN)
    Prefs.kt          réglages (SharedPreferences) : taux de change, session…
    CartLine.kt       ligne de panier (produit + quantité)
  ui/
    login/            écran de connexion PIN + création du 1er compte gérant
    main/              MainActivity : navigation par onglets selon le rôle
    pos/                écran de vente (grille produits, panier, checkout)
    stock/              écran stock (liste, édition, réapprovisionnement)
    reports/            historique des ventes + export
    employees/          gestion des employés (gérant)
    settings/            taux de change, boutique, imprimante Bluetooth,
                         activation de la synchronisation à distance
    dashboard/           écran Accueil (résumé + ventes distantes)
  sync/
    SupabaseConfig.kt        URL + clé publique du projet Supabase
    SupabaseSyncClient.kt    appels REST (enregistrement, push, lecture)
    SyncWorker.kt             pousse les ventes en attente (WorkManager)
    SyncScheduler.kt          déclenche la sync (immédiat + filet périodique)
    RemoteSale.kt             modèle de lecture des ventes distantes
  util/
    CurrencyFormatter, CsvExporter, PdfExporter, FileSharer,
    EscPosPrinter, ReceiptPrinter, PinHasher
```

Chaque vente est enregistrée dans **une seule transaction** (vérification du
stock, insertion de la vente et de ses lignes, décrément du stock, journal de
mouvement de stock) : soit tout est appliqué, soit rien ne l'est en cas
d'erreur — le stock ne peut donc jamais se désynchroniser d'une vente
enregistrée.

- `applicationId` : `com.shukranibusiness.app`
- `minSdk` 26 (Android 8.0+) / `compileSdk` & `targetSdk` 34
- Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Room 2.6.1

## ⚠️ Non testé par compilation

Ce code a été écrit dans un environnement sans SDK Android ni accès réseau
vers les serveurs Google — **il n'a donc pas pu être compilé ni exécuté ici**.
La structure, les types et les références de ressources ont été vérifiés
manuellement avec soin, mais la **première compilation dans Android Studio**
est nécessaire pour confirmer que tout s'assemble correctement (Room génère
du code au moment de la compilation, les ViewBinding aussi).

## Ouvrir le projet

1. Installer [Android Studio](https://developer.android.com/studio).
2. `File > Open` puis sélectionner le dossier `mobile/`.
3. Android Studio génère automatiquement le Gradle wrapper manquant
   (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) au premier sync — ces
   fichiers ne sont pas versionnés ici pour éviter de committer un binaire.
4. Laisser Gradle synchroniser (Room et ViewBinding génèrent du code à ce
   moment-là), puis lancer sur un émulateur ou un appareil physique
   (`Run > Run 'app'`).
5. Au premier lancement, l'app demande de créer le compte du **gérant**
   (nom + PIN) : c'est le tout premier compte, avant toute vente.

## Réapprovisionner / ajouter des produits

Depuis l'onglet **Stock** (visible uniquement pour le gérant) : bouton
« Ajouter un produit » pour créer un article (nom, catégorie, prix en CDF
**et** en USD, quantité, seuil d'alerte), ou « Réapprovisionner » sur un
produit existant pour ajouter des quantités reçues.

## Imprimante de reçu Bluetooth

1. Associer l'imprimante thermique dans les réglages Bluetooth du téléphone
   (comme n'importe quel appareil Bluetooth).
2. Dans l'app, onglet **Réglages** → « Choisir l'imprimante Bluetooth » →
   sélectionner l'appareil dans la liste des appareils appairés.
3. Chaque vente validée envoie ensuite automatiquement un ticket ESC/POS à
   cette imprimante (protocole SPP, standard des imprimantes de reçus bon
   marché). Si aucune imprimante n'est configurée, l'impression est
   simplement ignorée.

## Publier sur Google Play Console

1. Générer un App Bundle signé : `Build > Generate Signed Bundle / APK`
   (créer/utiliser un keystore de release — à conserver précieusement,
   il ne peut pas être remplacé une fois l'app publiée).
2. Dans la [Google Play Console](https://play.google.com/console),
   compte **Deogracio Apps**, créer l'application et suivre les étapes de
   fiche Store (description, captures d'écran, politique de
   confidentialité, classification du contenu — l'app utilise le Bluetooth,
   à déclarer dans le questionnaire de sécurité des données).
3. Uploader le fichier `.aab` généré dans une piste de test interne
   (recommandé avant une release publique).

## Synchronisation multi-appareils (cloud)

Chaque appareil reste **hors-ligne d'abord** : une vente est toujours
enregistrée localement, jamais bloquée par le réseau. Si la synchronisation
est activée, l'app tente ensuite d'envoyer la vente vers Supabase dès qu'il y
a du réseau (immédiatement après la vente, puis réessais automatiques ; un
filet de sécurité relance une synchronisation toutes les 30 minutes au cas
où l'app aurait été fermée).

Projet Supabase dédié : **`shukra-pos`** (séparé du projet marketing/ebooks
existant, pour ne pas mélanger les données). Schéma : `devices` (un jeton
secret par appareil), `sales` / `sale_items` (miroir cloud des ventes),
protégés par des politiques RLS strictes :
- un appareil ne peut envoyer/relire **que ses propres ventes** ;
- seul un appareil enregistré avec le rôle **« gérant »** peut lire les
  ventes de **tous** les appareils.

### Activer la synchronisation sur un appareil

1. Onglet **Réglages** → section « Synchronisation à distance ».
2. Entrer le **code de configuration** (secret, communiqué séparément —
   nécessaire pour empêcher n'importe qui d'enregistrer un faux appareil
   "gérant" et de tout lire).
3. Choisir le rôle :
   - **Vendeur** — cet appareil envoie ses propres ventes.
   - **Gérant à distance** — cet appareil consulte les ventes de tous les
     appareils enregistrés (visible dans l'onglet **Accueil**).
4. « Activer la synchronisation ».

⚠️ Le code de configuration ne doit être donné qu'aux personnes de
confiance : régénérable via `update app_config set value = ... where key =
'setup_key'` côté Supabase si besoin (ce qui invalide l'ancien code, sans
affecter les appareils déjà enregistrés).

- Pas de calcul de marge/bénéfice (prix d'achat non demandé) — à ajouter si
  besoin en ajoutant un champ `purchasePriceCdf`/`purchasePriceUsd` au
  produit.
- Le taux de change CDF/USD est saisi manuellement dans Réglages (pas de
  mise à jour automatique en ligne, cohérent avec le fonctionnement
  hors-ligne).
- Les icônes de navigation (`ic_pos`, `ic_stock`, `ic_reports`,
  `ic_settings`) restent des vecteurs placeholder simples ; à harmoniser
  avec le style du logo si besoin.

## Identité visuelle

Logo officiel conçu avec Canva (design : https://www.canva.com/d/agCwOqCegcpt50Z) :
badge circulaire bleu (globe + document coché) et wordmark « Shukra ».

- **Icône de l'application** : `mipmap-xxxhdpi/ic_launcher_foreground.png`
  (juste le badge, recadré et centré) + `@color/ic_launcher_background`
  (bleu marine `#030E24`), assemblés via l'icône adaptative
  `mipmap-anydpi-v26/ic_launcher.xml`. Une seule densité est fournie —
  Android Studio peut régénérer les autres tailles depuis ce fichier
  (clic droit sur `res` → New → Image Asset).
- **Écran de connexion** : bannière `drawable-nodpi/logo_shukra_pos.png`
  (badge + « Shukra ») suivie du texte « POS » (le mot « POS » n'était pas
  inclus dans l'export Canva, il est donc ajouté par l'app en texte stylé).
- **Couleurs de marque** (`values/colors.xml`) : `brand_navy` (#030E24),
  `brand_blue` (#2E6FF2), `brand_blue_dark` (#1B4FC4) — appliquées au thème
  (`Theme.ShukraPOS`) comme couleurs primaire/secondaire de l'app.
