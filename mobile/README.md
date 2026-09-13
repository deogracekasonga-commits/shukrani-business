# Shukrani Business — Mobile (Android)

Application Android destinée à être publiée sur **Google Play Console**
(compte développeur : Deogracio Apps). Projet Kotlin/Gradle standard,
généré comme point de départ — à ouvrir dans Android Studio pour continuer
le développement.

## Structure

```
mobile/
  app/
    src/main/java/com/shukranibusiness/app/MainActivity.kt
    src/main/res/            layouts, strings, thème, icône adaptative
    build.gradle.kts         config du module app (applicationId, SDK, deps)
  build.gradle.kts           config du projet (plugins AGP/Kotlin)
  settings.gradle.kts        déclare le module :app
```

- `applicationId` : `com.shukranibusiness.app`
- `minSdk` 26 (Android 8.0+) / `compileSdk` & `targetSdk` 34
- Kotlin 1.9.24, Android Gradle Plugin 8.5.2

## Ouvrir le projet

1. Installer [Android Studio](https://developer.android.com/studio).
2. `File > Open` puis sélectionner le dossier `mobile/`.
3. Android Studio génère automatiquement le Gradle wrapper manquant
   (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) au premier sync — ces
   fichiers ne sont pas versionnés ici pour éviter de committer un binaire.
4. Laisser Gradle synchroniser, puis lancer sur un émulateur ou un appareil
   physique (`Run > Run 'app'`).

## Publier sur Google Play Console

1. Générer un App Bundle signé : `Build > Generate Signed Bundle / APK`
   (créer/utiliser un keystore de release — à conserver précieusement,
   il ne peut pas être remplacé une fois l'app publiée).
2. Dans la [Google Play Console](https://play.google.com/console),
   compte **Deogracio Apps**, créer l'application et suivre les étapes de
   fiche Store (description, captures d'écran, politique de
   confidentialité, classification du contenu).
3. Uploader le fichier `.aab` généré dans une piste de test interne
   (recommandé avant une release publique).

## Prochaines étapes suggérées

- Décider si l'app affiche le dashboard existant (`dashboard/`, Next.js)
  via WebView, ou si c'est une app native indépendante.
- Ajouter le vrai logo (`dashboard/public/logo.png`) comme icône au lieu
  du placeholder vectoriel fourni ici.
- Configurer la signature de release et les variantes de build
  (debug/release).
