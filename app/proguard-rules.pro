# Règles R8 / ProGuard pour MedFind Maroc.
# Room, Compose, Navigation et Google Mobile Ads fournissent leurs propres
# règles "consumer". Seules quelques précautions sont ajoutées ici.

# osmdroid : la bibliothèque référence des classes optionnelles absentes.
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }

# Conserver les numéros de ligne pour des rapports de crash lisibles.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
