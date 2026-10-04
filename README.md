# MedFind Maroc — V1.0.0

> **Trouvez le médecin qu'il vous faut.**

Annuaire Android géolocalisé des médecins généralistes et spécialistes au Maroc :
recherche par nom, spécialité, ville et quartier, médecins à proximité, carte
OpenStreetMap, fiches détaillées (Appeler, WhatsApp, Itinéraire) et favoris hors ligne.

| Élément | Choix |
|---|---|
| Langage / UI | Kotlin 2.2, Jetpack Compose, Material 3 |
| Architecture | MVVM (ViewModel partagé + Repository), injection manuelle |
| Données locales | Room (SQLite) alimenté par des fichiers JSON embarqués |
| Carte | osmdroid + tuiles OpenStreetMap (aucune clé, aucun service payant) |
| Localisation | `LocationManager` Android (pas de Google Play Services) |
| Publicité | Google Mobile Ads (AdMob), identifiants de **test** par défaut |
| SDK | minSdk 26 (Android 8.0) · targetSdk / compileSdk 36 |
| Build | Gradle 8.14.3 (wrapper) · Android Gradle Plugin 8.11.1 · KSP |

Hors périmètre V1 (volontairement absent) : dentistes, pharmacies, laboratoires,
rendez-vous, téléconsultation, paiement, chat, IA, avis.

---

## 1. Ouvrir le projet

1. Installer une version récente d'**Android Studio** (JDK 17 ou 21 intégré).
2. *File → Open…* → sélectionner le dossier **`MedFindMaroc/`** (celui qui contient `settings.gradle.kts`).
3. Accepter « Trust project ».

## 2. Synchroniser Gradle

Android Studio lance la synchronisation automatiquement (sinon : *File → Sync Project with Gradle Files*).
Le premier lancement télécharge Gradle 8.14.3 et les dépendances (connexion Internet requise).
Si Android Studio propose de mettre à jour l'AGP ou des bibliothèques, ce n'est pas obligatoire.

En ligne de commande :

```bash
./gradlew assembleDebug        # APK de debug
./gradlew testDebugUnitTest    # tests unitaires (recherche, distances)
```

## 3. Lancer l'application

Choisir un émulateur ou un téléphone (débogage USB activé) puis **Run ▶**.
Pour tester la localisation sur émulateur : *Extended controls (…) → Location*, saisir par
exemple 35.5785, -5.3684 (Tétouan) puis « Set location ».

## 4. Modifier les médecins

Fichier : **`app/src/main/assets/data/doctors.json`**

```json
{
  "version": 2,
  "medecins": [
    {
      "id": "tet-0001",
      "nom": "Nom",
      "prenom": "Prénom",
      "specialite": "cardiologie",
      "typeMedecin": "SPECIALIST",
      "ville": "tetouan",
      "quartier": "Centre-ville",
      "adresse": "…",
      "telephone": "05 39 00 00 00",
      "telephoneSecondaire": null,
      "latitude": 35.5785,
      "longitude": -5.3684,
      "horaires": "Lundi – Vendredi : 09:00 – 17:00\nSamedi : 09:00 – 13:00",
      "description": null,
      "clinique": null,
      "siteWeb": null,
      "whatsapp": "+212 6 00 00 00 00",
      "source": "Nom de la source ouverte / saisie administrateur",
      "dateVerification": "2026-10-01",
      "profilVerifie": true
    }
  ]
}
```

Règles :

- `specialite` = l'`id` d'une spécialité de `specialties.json` ; `ville` = l'`id` d'une ville de `cities.json`.
  La **région** est déduite automatiquement de la ville.
- `typeMedecin` : `GENERALIST` ou `SPECIALIST` uniquement (déduit de la spécialité s'il est absent).
- Tous les champs sauf `id` et `nom` sont facultatifs ; l'application gère les valeurs manquantes
  (pas de téléphone → bouton désactivé, pas de coordonnées → absent de la carte mais itinéraire par adresse…).
- `profilVerifie: true` affiche le badge vert « Informations vérifiées ». Ne l'activer que pour des fiches réellement vérifiées.
- **Important : incrémentez `"version"`** à chaque modification. Au démarrage suivant, la base locale
  est remplacée par le nouveau contenu (les favoris des médecins toujours présents sont conservés).

### Données de démonstration

Le fichier livré contient **50 profils fictifs** (`"source": "DEMO"`, noms « Exemple 01 »…,
numéros « 05 00 00 00 xx »). Ils sont signalés partout par le bandeau et le badge
**« DONNÉES DE DÉMONSTRATION »**. Quelques fiches n'ont volontairement pas de téléphone, de
coordonnées ou d'adresse, pour tester la robustesse. Le bandeau disparaît automatiquement dès
qu'aucune fiche `"source": "DEMO"` ne reste dans le fichier.

N'intégrez de vrais médecins que si leur source et le droit de réutilisation des données sont établis
(Open Data marocain, OpenStreetMap — licence ODbL, attribution requise —, données publiques réutilisables,
saisie manuelle par l'administrateur).

## 5. Modifier les spécialités

Fichier : **`app/src/main/assets/data/specialties.json`** — champs `id`, `nom`, `nomPraticien`
(« Cardiologue »), `type`, `motsCles` (synonymes utilisés par la recherche, sans accents),
`populaire` (affichée sur l'accueil), `icone`, `ordre`. Incrémentez `version`.
Clés d'icônes disponibles : voir `specialtyIcon()` dans `ui/components/DirectoryCards.kt`.
Toute spécialité contenant « dentiste », « dentaire » ou « odonto » est ignorée.

## 6. Modifier les villes

Fichier : **`app/src/main/assets/data/cities.json`** — liste de régions, chacune avec ses `villes`
(`id`, `nom`, `latitude`, `longitude`). 69 villes dans les 12 régions sont fournies ; ajoutez-en
librement puis incrémentez `version`.

## 7. Configurer OpenStreetMap

Fichier : **`app/src/main/java/com/medfind/maroc/config/MapConfig.kt`** (source des tuiles, zooms,
centre par défaut, nombre maximal de marqueurs). Le User-Agent exigé par OSM est défini dans `MedFindApp.kt`.

Les serveurs `tile.openstreetmap.org` sont gratuits mais soumis à une
[politique d'usage](https://operations.osmfoundation.org/policies/tiles/) : ils ne sont pas prévus pour
une application à forte audience. Avant une diffusion large, passez à un fournisseur de tuiles OSM
(offre gratuite ou payante) ou à votre propre serveur en remplaçant `tileSource` par un `XYTileSource`.
L'attribution « © les contributeurs d'OpenStreetMap » est affichée sur la carte et doit le rester.

## 8. Configurer AdMob

- Activer / désactiver toutes les publicités : `medfind.ads.enabled=true|false` dans `gradle.properties`.
- Emplacements : bas de l'accueil, et une bannière après le 3ᵉ résultat de la liste
  (`ads/AdsConfig.kt`, `ads/AdContainer.kt`). Aucune publicité sur la fiche médecin, la carte,
  les paramètres ou la barre de navigation. Chaque bannière est précédée du libellé « PUBLICITÉ »
  et disparaît si elle ne se charge pas.
- Les builds **Debug utilisent toujours les identifiants de test Google**.
- Pour la production, ajoutez dans **`~/.gradle/gradle.properties`** (fichier personnel, jamais dans le dépôt) :

  ```properties
  medfind.admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
  medfind.admob.bannerId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
  ```

  Sans ces lignes, la version Release utilise aussi les identifiants de test.
- Sur la Play Console : déclarer « contient des annonces », la permission `AD_ID` ajoutée par le SDK,
  et remplir la section Sécurité des données en conséquence. Si vous visez des utilisateurs de l'UE/EEE,
  ajoutez le consentement UMP de Google.

## 9. Générer l'APK

*Build → Generate Signed Bundle / APK… → APK* → créer ou choisir un keystore → variante **release**.
Le fichier se trouve dans `app/release/`. (APK non signé de test : `./gradlew assembleRelease`.)

**Conservez le keystore et ses mots de passe en lieu sûr** : sans eux, plus aucune mise à jour possible.

## 10. Générer l'AAB pour Google Play

*Build → Generate Signed Bundle / APK… → Android App Bundle* → keystore → **release**.
Résultat : `app/release/app-release.aab`, à envoyer dans la Play Console
(Play App Signing recommandé). Icône 512 × 512 pour la fiche Play Store : `store/play_store_icon_512.png`.
Avant chaque nouvelle version : incrémenter `versionCode` et `versionName` dans `app/build.gradle.kts`.

---

## Structure du code

```text
app/src/main/
├── assets/data/            doctors.json · specialties.json · cities.json
├── java/com/medfind/maroc/
│   ├── MedFindApp.kt        Application (osmdroid, AdMob)
│   ├── AppContainer.kt      Injection de dépendances manuelle
│   ├── MainActivity.kt      Edge-to-edge + thème
│   ├── ads/                 AdsConfig, AdContainer
│   ├── config/              MapConfig
│   ├── data/local/          Entités Room, DAO, base
│   ├── data/seed/           Lecture des JSON
│   ├── data/repository/     DirectoryRepository (interface) + implémentation locale, favoris, réglages
│   ├── domain/model/        Doctor, Specialty, City, DoctorType
│   ├── domain/search/       Recherche (sans accents, synonymes), distances
│   ├── location/            LocationProvider
│   ├── util/                ExternalActions (appel, WhatsApp, itinéraire… protégés contre les crashs)
│   └── ui/                  theme, components, navigation, home, search, specialties,
│                            cities, map, profile, favorites, settings
└── res/                     icône adaptative (épingle + croix médicale), marqueurs de carte
```

**Passer à une base distante** : implémenter `DirectoryRepository` (par exemple une synchronisation
API → Room) et la brancher dans `AppContainer.kt`. Les écrans n'ont rien à changer.

## Notes de conception

- La permission de localisation n'est demandée qu'à l'appui sur « Médecins près de moi », sur le filtre
  Distance ou sur le bouton de position de la carte. En cas de refus, la recherche par ville et spécialité
  reste pleinement utilisable.
- Les boutons Appeler / WhatsApp / Itinéraire ouvrent des applications externes ; s'il n'y en a aucune,
  un message s'affiche (et, pour l'itinéraire, l'OSM web ou la copie des coordonnées prend le relais).
- Les paramètres ne comportent pas d'entrée « Notifications » : la V1 n'envoie aucune notification,
  et le cahier des charges interdit d'afficher une fonction non implémentée.
- Le ViewModel partagé conserve recherche, filtres, position et caméra de carte lors des rotations
  et des retours d'arrière-plan.


## Synchronisation distante de la base des médecins

La V2 peut vérifier automatiquement une base JSON publique hébergée sur GitHub Raw, sans utiliser l'API GitHub.

Fichiers distants :
- `app/src/main/assets/data/version.json`
- `app/src/main/assets/data/doctors.json`
- `app/src/main/assets/data/cities.json`
- `app/src/main/assets/data/specialties.json`

L'application conserve toujours une copie locale dans Room. Si Internet est indisponible, les données locales restent utilisables. Une erreur de téléchargement ne remplace jamais une base locale valide.

### Mettre à jour les médecins sans publier une nouvelle APK

1. Modifier `doctors.json`.
2. Incrémenter la valeur `version` dans `version.json`.
3. Si `cities.json` ou `specialties.json` change, modifier également leur `version`.
4. Faire `git add`, `git commit` puis `git push`.
5. Les applications installées détecteront la nouvelle version lors de leur prochaine initialisation avec Internet.

La version distante doit être un entier positif et doit augmenter à chaque publication de données.

