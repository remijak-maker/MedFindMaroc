package com.medfind.maroc.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.medfind.maroc.BuildConfig
import com.medfind.maroc.R
import com.medfind.maroc.ui.components.BackTopBar
import com.medfind.maroc.ui.components.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("À propos", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo()
            Spacer(Modifier.height(16.dp))
            Text("MedFind Maroc", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Trouvez le médecin qu'il vous faut.",
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(24.dp))
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "MedFind Maroc facilite la recherche de médecins généralistes et spécialistes au Maroc " +
                            "grâce à une recherche par ville, spécialité et proximité.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "MedFind Maroc est un annuaire. L'application ne propose ni diagnostic, ni conseil médical, " +
                            "ni prise de rendez-vous. Vérifiez toujours les informations directement auprès du cabinet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Cartographie : © les contributeurs d'OpenStreetMap (licence ODbL).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLogo() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1F6FE0)),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Logo MedFind Maroc",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Politique de confidentialité", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Dernière mise à jour : version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PolicySection(
                "Données personnelles",
                "MedFind Maroc ne demande pas de création de compte et ne collecte ni votre nom, ni votre " +
                    "adresse e-mail, ni votre numéro de téléphone. Vos favoris et vos préférences sont " +
                    "enregistrés uniquement sur votre appareil.",
            )
            PolicySection(
                "Localisation",
                "La localisation n'est demandée que lorsque vous utilisez « Médecins près de moi » ou le bouton " +
                    "de position sur la carte. Elle sert uniquement à calculer, sur votre appareil, la distance " +
                    "jusqu'aux médecins. Elle n'est ni enregistrée ni transmise à nos serveurs. Vous pouvez retirer " +
                    "cette autorisation à tout moment dans les paramètres d'Android.",
            )
            PolicySection(
                "Carte",
                "Le fond de carte est fourni par OpenStreetMap. Pour afficher la carte, votre appareil télécharge " +
                    "des images (tuiles) depuis les serveurs de tuiles, qui reçoivent votre adresse IP comme pour " +
                    "toute page web. Votre position n'est pas envoyée pour cela.",
            )
            PolicySection(
                "Publicité",
                "L'application peut afficher des bannières publicitaires fournies par Google AdMob. Google peut " +
                    "utiliser l'identifiant publicitaire de l'appareil et des informations techniques pour diffuser " +
                    "et mesurer les annonces. Vous pouvez réinitialiser ou supprimer cet identifiant dans les " +
                    "paramètres Google de votre appareil. En savoir plus : policies.google.com/technologies/ads",
            )
            PolicySection(
                "Applications externes",
                "Les boutons Appeler, WhatsApp et Itinéraire ouvrent les applications correspondantes de votre " +
                    "téléphone. MedFind Maroc n'a pas accès au contenu de vos appels ou messages.",
            )
            PolicySection(
                "Informations sur les médecins",
                "Les fiches proviennent de sources ouvertes ou publiques dont la réutilisation est autorisée, ou " +
                    "sont ajoutées par l'éditeur. La source et la date de vérification sont indiquées sur chaque " +
                    "fiche lorsqu'elles sont connues. Les profils marqués « Données de démonstration » sont fictifs.",
            )
            PolicySection(
                "Contact",
                "Pour toute question ou demande de correction d'une fiche, contactez l'éditeur via la page de " +
                    "l'application sur Google Play.",
            )
            Spacer(Modifier.fillMaxWidth().height(16.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
